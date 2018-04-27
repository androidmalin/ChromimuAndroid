package org.chromium.net.impl;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.net.TrafficStats;
import android.os.Build.VERSION;
import android.support.annotation.Nullable;
import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.concurrent.GuardedBy;
import org.chromium.net.CronetException;
import org.chromium.net.InlineExecutionProhibitedException;
import org.chromium.net.UploadDataProvider;
import org.chromium.net.UploadDataSink;
import org.chromium.net.UrlRequest.Callback;
import org.chromium.net.UrlRequest.StatusListener;
import org.chromium.net.UrlResponseInfo;
import org.chromium.net.impl.VersionSafeCallbacks.UploadDataProviderWrapper;
import org.chromium.net.impl.VersionSafeCallbacks.UrlRequestCallback;
import org.chromium.net.impl.VersionSafeCallbacks.UrlRequestStatusListener;

@TargetApi(14)
final class JavaUrlRequest extends UrlRequestBase {
    private static final int DEFAULT_CHUNK_LENGTH = 8192;
    private static final int DEFAULT_UPLOAD_BUFFER_SIZE = 8192;
    private static final String TAG = JavaUrlRequest.class.getSimpleName();
    private static final String USER_AGENT = "User-Agent";
    private static final String X_ANDROID = "X-Android";
    private static final String X_ANDROID_SELECTED_TRANSPORT = "X-Android-Selected-Transport";
    private volatile int mAdditionalStatusDetails = -1;
    private final boolean mAllowDirectExecutor;
    private final AsyncUrlRequestCallback mCallbackAsync;
    private String mCurrentUrl;
    private HttpURLConnection mCurrentUrlConnection;
    private final Executor mExecutor;
    private String mInitialMethod;
    private OutputStreamDataSink mOutputStreamDataSink;
    private String mPendingRedirectUrl;
    private final Map<String, String> mRequestHeaders = new TreeMap(String.CASE_INSENSITIVE_ORDER);
    @Nullable
    private ReadableByteChannel mResponseChannel;
    private final AtomicReference<State> mState = new AtomicReference(State.NOT_STARTED);
    private final int mTrafficStatsTag;
    private UploadDataProviderWrapper mUploadDataProvider;
    private Executor mUploadExecutor;
    private final AtomicBoolean mUploadProviderClosed = new AtomicBoolean(false);
    private final List<String> mUrlChain = new ArrayList();
    private UrlResponseInfoImpl mUrlResponseInfo;
    private final String mUserAgent;

    private interface CheckedRunnable {
        void run() throws Exception;
    }

    private final class AsyncUrlRequestCallback {
        final UrlRequestCallback mCallback;
        final Executor mFallbackExecutor;
        final Executor mUserExecutor;

        AsyncUrlRequestCallback(Callback callback, Executor userExecutor) {
            this.mCallback = new UrlRequestCallback(callback);
            if (JavaUrlRequest.this.mAllowDirectExecutor) {
                this.mUserExecutor = userExecutor;
                this.mFallbackExecutor = null;
                return;
            }
            this.mUserExecutor = new DirectPreventingExecutor(userExecutor);
            this.mFallbackExecutor = userExecutor;
        }

        void sendStatus(final UrlRequestStatusListener listener, final int status) {
            this.mUserExecutor.execute(new Runnable() {
                public void run() {
                    listener.onStatus(status);
                }
            });
        }

        void execute(CheckedRunnable runnable) {
            try {
                this.mUserExecutor.execute(JavaUrlRequest.this.userErrorSetting(runnable));
            } catch (RejectedExecutionException e) {
                JavaUrlRequest.this.enterErrorState(new CronetExceptionImpl("Exception posting task to executor", e));
            }
        }

        void onRedirectReceived(final UrlResponseInfo info, final String newLocationUrl) {
            execute(new CheckedRunnable() {
                public void run() throws Exception {
                    AsyncUrlRequestCallback.this.mCallback.onRedirectReceived(JavaUrlRequest.this, info, newLocationUrl);
                }
            });
        }

        void onResponseStarted(UrlResponseInfo info) {
            execute(new CheckedRunnable() {
                public void run() throws Exception {
                    if (JavaUrlRequest.this.mState.compareAndSet(State.STARTED, State.AWAITING_READ)) {
                        AsyncUrlRequestCallback.this.mCallback.onResponseStarted(JavaUrlRequest.this, JavaUrlRequest.this.mUrlResponseInfo);
                    }
                }
            });
        }

        void onReadCompleted(final UrlResponseInfo info, final ByteBuffer byteBuffer) {
            execute(new CheckedRunnable() {
                public void run() throws Exception {
                    if (JavaUrlRequest.this.mState.compareAndSet(State.READING, State.AWAITING_READ)) {
                        AsyncUrlRequestCallback.this.mCallback.onReadCompleted(JavaUrlRequest.this, info, byteBuffer);
                    }
                }
            });
        }

        void onCanceled(final UrlResponseInfo info) {
            JavaUrlRequest.this.closeResponseChannel();
            this.mUserExecutor.execute(new Runnable() {
                public void run() {
                    try {
                        AsyncUrlRequestCallback.this.mCallback.onCanceled(JavaUrlRequest.this, info);
                    } catch (Exception exception) {
                        Log.e(JavaUrlRequest.TAG, "Exception in onCanceled method", exception);
                    }
                }
            });
        }

        void onSucceeded(final UrlResponseInfo info) {
            this.mUserExecutor.execute(new Runnable() {
                public void run() {
                    try {
                        AsyncUrlRequestCallback.this.mCallback.onSucceeded(JavaUrlRequest.this, info);
                    } catch (Exception exception) {
                        Log.e(JavaUrlRequest.TAG, "Exception in onSucceeded method", exception);
                    }
                }
            });
        }

        void onFailed(final UrlResponseInfo urlResponseInfo, final CronetException e) {
            JavaUrlRequest.this.closeResponseChannel();
            Runnable runnable = new Runnable() {
                public void run() {
                    try {
                        AsyncUrlRequestCallback.this.mCallback.onFailed(JavaUrlRequest.this, urlResponseInfo, e);
                    } catch (Exception exception) {
                        Log.e(JavaUrlRequest.TAG, "Exception in onFailed method", exception);
                    }
                }
            };
            try {
                this.mUserExecutor.execute(runnable);
            } catch (InlineExecutionProhibitedException e2) {
                if (this.mFallbackExecutor != null) {
                    this.mFallbackExecutor.execute(runnable);
                }
            }
        }
    }

    static final class DirectPreventingExecutor implements Executor {
        private final Executor mDelegate;

        private static final class InlineCheckingRunnable implements Runnable {
            private Thread mCallingThread;
            private final Runnable mCommand;
            private InlineExecutionProhibitedException mExecutedInline;

            private InlineCheckingRunnable(Runnable command, Thread callingThread) {
                this.mExecutedInline = null;
                this.mCommand = command;
                this.mCallingThread = callingThread;
            }

            public void run() {
                if (Thread.currentThread() == this.mCallingThread) {
                    this.mExecutedInline = new InlineExecutionProhibitedException();
                } else {
                    this.mCommand.run();
                }
            }
        }

        DirectPreventingExecutor(Executor delegate) {
            this.mDelegate = delegate;
        }

        public void execute(Runnable command) {
            InlineCheckingRunnable runnable = new InlineCheckingRunnable(command, Thread.currentThread());
            this.mDelegate.execute(runnable);
            if (runnable.mExecutedInline != null) {
                throw runnable.mExecutedInline;
            }
            runnable.mCallingThread = null;
        }
    }

    private final class OutputStreamDataSink extends UploadDataSink {
        ByteBuffer mBuffer;
        final Executor mExecutor;
        WritableByteChannel mOutputChannel;
        final AtomicBoolean mOutputChannelClosed = new AtomicBoolean(false);
        final AtomicReference<SinkState> mSinkState = new AtomicReference(SinkState.NOT_STARTED);
        long mTotalBytes;
        final UploadDataProviderWrapper mUploadProvider;
        final HttpURLConnection mUrlConnection;
        OutputStream mUrlConnectionOutputStream;
        final Executor mUserUploadExecutor;
        long mWrittenBytes = 0;

        OutputStreamDataSink(final Executor userExecutor, Executor executor, HttpURLConnection urlConnection, UploadDataProviderWrapper provider) {
            this.mUserUploadExecutor = new Executor(JavaUrlRequest.this) {
                public void execute(Runnable runnable) {
                    try {
                        userExecutor.execute(runnable);
                    } catch (RejectedExecutionException e) {
                        JavaUrlRequest.this.enterUploadErrorState(e);
                    }
                }
            };
            this.mExecutor = executor;
            this.mUrlConnection = urlConnection;
            this.mUploadProvider = provider;
        }

        @SuppressLint({"DefaultLocale"})
        public void onReadSucceeded(final boolean finalChunk) {
            if (this.mSinkState.compareAndSet(SinkState.AWAITING_READ_RESULT, SinkState.UPLOADING)) {
                this.mExecutor.execute(JavaUrlRequest.this.errorSetting(new CheckedRunnable() {
                    public void run() throws Exception {
                        OutputStreamDataSink.this.mBuffer.flip();
                        if (OutputStreamDataSink.this.mTotalBytes == -1 || OutputStreamDataSink.this.mTotalBytes - OutputStreamDataSink.this.mWrittenBytes >= ((long) OutputStreamDataSink.this.mBuffer.remaining())) {
                            while (OutputStreamDataSink.this.mBuffer.hasRemaining()) {
                                OutputStreamDataSink outputStreamDataSink = OutputStreamDataSink.this;
                                outputStreamDataSink.mWrittenBytes += (long) OutputStreamDataSink.this.mOutputChannel.write(OutputStreamDataSink.this.mBuffer);
                            }
                            OutputStreamDataSink.this.mUrlConnectionOutputStream.flush();
                            if (OutputStreamDataSink.this.mWrittenBytes < OutputStreamDataSink.this.mTotalBytes || (OutputStreamDataSink.this.mTotalBytes == -1 && !finalChunk)) {
                                OutputStreamDataSink.this.mBuffer.clear();
                                OutputStreamDataSink.this.mSinkState.set(SinkState.AWAITING_READ_RESULT);
                                OutputStreamDataSink.this.executeOnUploadExecutor(new CheckedRunnable() {
                                    public void run() throws Exception {
                                        OutputStreamDataSink.this.mUploadProvider.read(OutputStreamDataSink.this, OutputStreamDataSink.this.mBuffer);
                                    }
                                });
                                return;
                            } else if (OutputStreamDataSink.this.mTotalBytes == -1) {
                                OutputStreamDataSink.this.finish();
                                return;
                            } else if (OutputStreamDataSink.this.mTotalBytes == OutputStreamDataSink.this.mWrittenBytes) {
                                OutputStreamDataSink.this.finish();
                                return;
                            } else {
                                JavaUrlRequest.this.enterUploadErrorState(new IllegalArgumentException(String.format("Read upload data length %d exceeds expected length %d", new Object[]{Long.valueOf(OutputStreamDataSink.this.mWrittenBytes), Long.valueOf(OutputStreamDataSink.this.mTotalBytes)})));
                                return;
                            }
                        }
                        JavaUrlRequest.this.enterUploadErrorState(new IllegalArgumentException(String.format("Read upload data length %d exceeds expected length %d", new Object[]{Long.valueOf(OutputStreamDataSink.this.mWrittenBytes + ((long) OutputStreamDataSink.this.mBuffer.remaining())), Long.valueOf(OutputStreamDataSink.this.mTotalBytes)})));
                    }
                }));
                return;
            }
            throw new IllegalStateException("Not expecting a read result, expecting: " + this.mSinkState.get());
        }

        public void onRewindSucceeded() {
            if (this.mSinkState.compareAndSet(SinkState.AWAITING_REWIND_RESULT, SinkState.UPLOADING)) {
                startRead();
                return;
            }
            throw new IllegalStateException("Not expecting a read result");
        }

        public void onReadError(Exception exception) {
            JavaUrlRequest.this.enterUploadErrorState(exception);
        }

        public void onRewindError(Exception exception) {
            JavaUrlRequest.this.enterUploadErrorState(exception);
        }

        void startRead() {
            this.mExecutor.execute(JavaUrlRequest.this.errorSetting(new CheckedRunnable() {
                public void run() throws Exception {
                    if (OutputStreamDataSink.this.mOutputChannel == null) {
                        JavaUrlRequest.this.mAdditionalStatusDetails = 10;
                        OutputStreamDataSink.this.mUrlConnection.connect();
                        JavaUrlRequest.this.mAdditionalStatusDetails = 12;
                        OutputStreamDataSink.this.mUrlConnectionOutputStream = OutputStreamDataSink.this.mUrlConnection.getOutputStream();
                        OutputStreamDataSink.this.mOutputChannel = Channels.newChannel(OutputStreamDataSink.this.mUrlConnectionOutputStream);
                    }
                    OutputStreamDataSink.this.mSinkState.set(SinkState.AWAITING_READ_RESULT);
                    OutputStreamDataSink.this.executeOnUploadExecutor(new CheckedRunnable() {
                        public void run() throws Exception {
                            OutputStreamDataSink.this.mUploadProvider.read(OutputStreamDataSink.this, OutputStreamDataSink.this.mBuffer);
                        }
                    });
                }
            }));
        }

        private void executeOnUploadExecutor(CheckedRunnable runnable) {
            try {
                this.mUserUploadExecutor.execute(JavaUrlRequest.this.uploadErrorSetting(runnable));
            } catch (RejectedExecutionException e) {
                JavaUrlRequest.this.enterUploadErrorState(e);
            }
        }

        void closeOutputChannel() throws IOException {
            if (this.mOutputChannel != null && this.mOutputChannelClosed.compareAndSet(false, true)) {
                this.mOutputChannel.close();
            }
        }

        void finish() throws IOException {
            closeOutputChannel();
            JavaUrlRequest.this.fireGetHeaders();
        }

        void start(final boolean firstTime) {
            executeOnUploadExecutor(new CheckedRunnable() {
                public void run() throws Exception {
                    OutputStreamDataSink.this.mTotalBytes = OutputStreamDataSink.this.mUploadProvider.getLength();
                    if (OutputStreamDataSink.this.mTotalBytes == 0) {
                        OutputStreamDataSink.this.finish();
                        return;
                    }
                    if (OutputStreamDataSink.this.mTotalBytes <= 0 || OutputStreamDataSink.this.mTotalBytes >= 8192) {
                        OutputStreamDataSink.this.mBuffer = ByteBuffer.allocateDirect(8192);
                    } else {
                        OutputStreamDataSink.this.mBuffer = ByteBuffer.allocateDirect(((int) OutputStreamDataSink.this.mTotalBytes) + 1);
                    }
                    if (OutputStreamDataSink.this.mTotalBytes > 0 && OutputStreamDataSink.this.mTotalBytes <= 2147483647L) {
                        OutputStreamDataSink.this.mUrlConnection.setFixedLengthStreamingMode((int) OutputStreamDataSink.this.mTotalBytes);
                    } else if (OutputStreamDataSink.this.mTotalBytes <= 2147483647L || VERSION.SDK_INT < 19) {
                        OutputStreamDataSink.this.mUrlConnection.setChunkedStreamingMode(8192);
                    } else {
                        OutputStreamDataSink.this.mUrlConnection.setFixedLengthStreamingMode(OutputStreamDataSink.this.mTotalBytes);
                    }
                    if (firstTime) {
                        OutputStreamDataSink.this.startRead();
                        return;
                    }
                    OutputStreamDataSink.this.mSinkState.set(SinkState.AWAITING_REWIND_RESULT);
                    OutputStreamDataSink.this.mUploadProvider.rewind(OutputStreamDataSink.this);
                }
            });
        }
    }

    private static final class SerializingExecutor implements Executor {
        private final Runnable mRunTasks = new Runnable() {
            /* JADX WARNING: inconsistent code. */
            /* Code decompiled incorrectly, please refer to instructions dump. */
            public void run() {
                /*
                r9 = this;
                r4 = 1;
                r5 = 0;
                r3 = org.chromium.net.impl.JavaUrlRequest.SerializingExecutor.this;
                r6 = r3.mTaskQueue;
                monitor-enter(r6);
                r3 = org.chromium.net.impl.JavaUrlRequest.SerializingExecutor.this;	 Catch:{ all -> 0x0054 }
                r3 = r3.mRunning;	 Catch:{ all -> 0x0054 }
                if (r3 == 0) goto L_0x0013;
            L_0x0011:
                monitor-exit(r6);	 Catch:{ all -> 0x0054 }
            L_0x0012:
                return;
            L_0x0013:
                r3 = org.chromium.net.impl.JavaUrlRequest.SerializingExecutor.this;	 Catch:{ all -> 0x0054 }
                r3 = r3.mTaskQueue;	 Catch:{ all -> 0x0054 }
                r1 = r3.pollFirst();	 Catch:{ all -> 0x0054 }
                r1 = (java.lang.Runnable) r1;	 Catch:{ all -> 0x0054 }
                r7 = org.chromium.net.impl.JavaUrlRequest.SerializingExecutor.this;	 Catch:{ all -> 0x0054 }
                if (r1 == 0) goto L_0x0052;
            L_0x0023:
                r3 = r4;
            L_0x0024:
                r7.mRunning = r3;	 Catch:{ all -> 0x0054 }
                monitor-exit(r6);	 Catch:{ all -> 0x0054 }
            L_0x0028:
                if (r1 == 0) goto L_0x0012;
            L_0x002a:
                r2 = 1;
                r1.run();	 Catch:{ all -> 0x0070 }
                r2 = 0;
                r3 = org.chromium.net.impl.JavaUrlRequest.SerializingExecutor.this;
                r6 = r3.mTaskQueue;
                monitor-enter(r6);
                if (r2 == 0) goto L_0x0057;
            L_0x0038:
                r3 = org.chromium.net.impl.JavaUrlRequest.SerializingExecutor.this;	 Catch:{ all -> 0x004f }
                r7 = 0;
                r3.mRunning = r7;	 Catch:{ all -> 0x004f }
                r3 = org.chromium.net.impl.JavaUrlRequest.SerializingExecutor.this;	 Catch:{ RejectedExecutionException -> 0x00af }
                r3 = r3.mUnderlyingExecutor;	 Catch:{ RejectedExecutionException -> 0x00af }
                r7 = org.chromium.net.impl.JavaUrlRequest.SerializingExecutor.this;	 Catch:{ RejectedExecutionException -> 0x00af }
                r7 = r7.mRunTasks;	 Catch:{ RejectedExecutionException -> 0x00af }
                r3.execute(r7);	 Catch:{ RejectedExecutionException -> 0x00af }
            L_0x004d:
                monitor-exit(r6);	 Catch:{ all -> 0x004f }
                goto L_0x0028;
            L_0x004f:
                r3 = move-exception;
                monitor-exit(r6);	 Catch:{ all -> 0x004f }
                throw r3;
            L_0x0052:
                r3 = r5;
                goto L_0x0024;
            L_0x0054:
                r3 = move-exception;
                monitor-exit(r6);	 Catch:{ all -> 0x0054 }
                throw r3;
            L_0x0057:
                r3 = org.chromium.net.impl.JavaUrlRequest.SerializingExecutor.this;	 Catch:{ all -> 0x004f }
                r3 = r3.mTaskQueue;	 Catch:{ all -> 0x004f }
                r3 = r3.pollFirst();	 Catch:{ all -> 0x004f }
                r0 = r3;
                r0 = (java.lang.Runnable) r0;	 Catch:{ all -> 0x004f }
                r1 = r0;
                r7 = org.chromium.net.impl.JavaUrlRequest.SerializingExecutor.this;	 Catch:{ all -> 0x004f }
                if (r1 == 0) goto L_0x006e;
            L_0x0069:
                r3 = r4;
            L_0x006a:
                r7.mRunning = r3;	 Catch:{ all -> 0x004f }
                goto L_0x004d;
            L_0x006e:
                r3 = r5;
                goto L_0x006a;
            L_0x0070:
                r6 = move-exception;
                r3 = org.chromium.net.impl.JavaUrlRequest.SerializingExecutor.this;
                r7 = r3.mTaskQueue;
                monitor-enter(r7);
                if (r2 == 0) goto L_0x0091;
            L_0x007a:
                r3 = org.chromium.net.impl.JavaUrlRequest.SerializingExecutor.this;	 Catch:{ all -> 0x00a8 }
                r4 = 0;
                r3.mRunning = r4;	 Catch:{ all -> 0x00a8 }
                r3 = org.chromium.net.impl.JavaUrlRequest.SerializingExecutor.this;	 Catch:{ RejectedExecutionException -> 0x00ad }
                r3 = r3.mUnderlyingExecutor;	 Catch:{ RejectedExecutionException -> 0x00ad }
                r4 = org.chromium.net.impl.JavaUrlRequest.SerializingExecutor.this;	 Catch:{ RejectedExecutionException -> 0x00ad }
                r4 = r4.mRunTasks;	 Catch:{ RejectedExecutionException -> 0x00ad }
                r3.execute(r4);	 Catch:{ RejectedExecutionException -> 0x00ad }
            L_0x008f:
                monitor-exit(r7);	 Catch:{ all -> 0x00a8 }
                throw r6;
            L_0x0091:
                r3 = org.chromium.net.impl.JavaUrlRequest.SerializingExecutor.this;	 Catch:{ all -> 0x00a8 }
                r3 = r3.mTaskQueue;	 Catch:{ all -> 0x00a8 }
                r3 = r3.pollFirst();	 Catch:{ all -> 0x00a8 }
                r0 = r3;
                r0 = (java.lang.Runnable) r0;	 Catch:{ all -> 0x00a8 }
                r1 = r0;
                r8 = org.chromium.net.impl.JavaUrlRequest.SerializingExecutor.this;	 Catch:{ all -> 0x00a8 }
                if (r1 == 0) goto L_0x00ab;
            L_0x00a3:
                r3 = r4;
            L_0x00a4:
                r8.mRunning = r3;	 Catch:{ all -> 0x00a8 }
                goto L_0x008f;
            L_0x00a8:
                r3 = move-exception;
                monitor-exit(r7);	 Catch:{ all -> 0x00a8 }
                throw r3;
            L_0x00ab:
                r3 = r5;
                goto L_0x00a4;
            L_0x00ad:
                r3 = move-exception;
                goto L_0x008f;
            L_0x00af:
                r3 = move-exception;
                goto L_0x004d;
                */
                throw new UnsupportedOperationException("Method not decompiled: org.chromium.net.impl.JavaUrlRequest.SerializingExecutor.1.run():void");
            }
        };
        @GuardedBy("mTaskQueue")
        private boolean mRunning;
        @GuardedBy("mTaskQueue")
        private final ArrayDeque<Runnable> mTaskQueue = new ArrayDeque();
        private final Executor mUnderlyingExecutor;

        SerializingExecutor(Executor underlyingExecutor) {
            this.mUnderlyingExecutor = underlyingExecutor;
        }

        public void execute(Runnable command) {
            synchronized (this.mTaskQueue) {
                this.mTaskQueue.addLast(command);
                try {
                    this.mUnderlyingExecutor.execute(this.mRunTasks);
                } catch (RejectedExecutionException e) {
                    this.mTaskQueue.removeLast();
                }
            }
        }
    }

    private enum SinkState {
        AWAITING_READ_RESULT,
        AWAITING_REWIND_RESULT,
        UPLOADING,
        NOT_STARTED
    }

    private enum State {
        NOT_STARTED,
        STARTED,
        REDIRECT_RECEIVED,
        AWAITING_FOLLOW_REDIRECT,
        AWAITING_READ,
        READING,
        ERROR,
        COMPLETE,
        CANCELLED
    }

    JavaUrlRequest(Callback callback, final Executor executor, Executor userExecutor, String url, String userAgent, boolean allowDirectExecutor) {
        if (url == null) {
            throw new NullPointerException("URL is required");
        } else if (callback == null) {
            throw new NullPointerException("Listener is required");
        } else if (executor == null) {
            throw new NullPointerException("Executor is required");
        } else if (userExecutor == null) {
            throw new NullPointerException("userExecutor is required");
        } else {
            this.mAllowDirectExecutor = allowDirectExecutor;
            this.mCallbackAsync = new AsyncUrlRequestCallback(callback, userExecutor);
            this.mTrafficStatsTag = TrafficStats.getThreadStatsTag();
            this.mExecutor = new SerializingExecutor(new Executor() {
                public void execute(final Runnable command) {
                    executor.execute(new Runnable() {
                        public void run() {
                            int oldTag = TrafficStats.getThreadStatsTag();
                            TrafficStats.setThreadStatsTag(JavaUrlRequest.this.mTrafficStatsTag);
                            try {
                                command.run();
                            } finally {
                                TrafficStats.setThreadStatsTag(oldTag);
                            }
                        }
                    });
                }
            });
            this.mCurrentUrl = url;
            this.mUserAgent = userAgent;
        }
    }

    public void setHttpMethod(String method) {
        checkNotStarted();
        if (method == null) {
            throw new NullPointerException("Method is required.");
        } else if ("OPTIONS".equalsIgnoreCase(method) || "GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method) || "POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) || "DELETE".equalsIgnoreCase(method) || "TRACE".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method)) {
            this.mInitialMethod = method;
        } else {
            throw new IllegalArgumentException("Invalid http method " + method);
        }
    }

    private void checkNotStarted() {
        State state = (State) this.mState.get();
        if (state != State.NOT_STARTED) {
            throw new IllegalStateException("Request is already started. State is: " + state);
        }
    }

    public void addHeader(String header, String value) {
        checkNotStarted();
        if (!isValidHeaderName(header) || value.contains("\r\n")) {
            throw new IllegalArgumentException("Invalid header " + header + "=" + value);
        }
        if (this.mRequestHeaders.containsKey(header)) {
            this.mRequestHeaders.remove(header);
        }
        this.mRequestHeaders.put(header, value);
    }

    private boolean isValidHeaderName(String header) {
        int i = 0;
        while (i < header.length()) {
            char c = header.charAt(i);
            switch (c) {
                case '\'':
                case '(':
                case ')':
                case ',':
                case '/':
                case ':':
                case ';':
                case '<':
                case '=':
                case '>':
                case '?':
                case '@':
                case '[':
                case '\\':
                case ']':
                case '{':
                case '}':
                    return false;
                default:
                    if (Character.isISOControl(c) || Character.isWhitespace(c)) {
                        return false;
                    }
                    i++;
                    break;
            }
        }
        return true;
    }

    public void setUploadDataProvider(UploadDataProvider uploadDataProvider, Executor executor) {
        if (uploadDataProvider == null) {
            throw new NullPointerException("Invalid UploadDataProvider.");
        } else if (this.mRequestHeaders.containsKey("Content-Type")) {
            checkNotStarted();
            if (this.mInitialMethod == null) {
                this.mInitialMethod = "POST";
            }
            this.mUploadDataProvider = new UploadDataProviderWrapper(uploadDataProvider);
            if (this.mAllowDirectExecutor) {
                this.mUploadExecutor = executor;
            } else {
                this.mUploadExecutor = new DirectPreventingExecutor(executor);
            }
        } else {
            throw new IllegalArgumentException("Requests with upload data must have a Content-Type.");
        }
    }

    public void start() {
        this.mAdditionalStatusDetails = 10;
        transitionStates(State.NOT_STARTED, State.STARTED, new Runnable() {
            public void run() {
                JavaUrlRequest.this.mUrlChain.add(JavaUrlRequest.this.mCurrentUrl);
                JavaUrlRequest.this.fireOpenConnection();
            }
        });
    }

    private void enterErrorState(CronetException error) {
        if (setTerminalState(State.ERROR)) {
            fireDisconnect();
            fireCloseUploadDataProvider();
            this.mCallbackAsync.onFailed(this.mUrlResponseInfo, error);
        }
    }

    private boolean setTerminalState(State error) {
        State oldState;
        do {
            oldState = (State) this.mState.get();
            switch (oldState) {
                case NOT_STARTED:
                    throw new IllegalStateException("Can't enter error state before start");
                case ERROR:
                case COMPLETE:
                case CANCELLED:
                    return false;
                default:
                    break;
            }
        } while (!this.mState.compareAndSet(oldState, error));
        return true;
    }

    private void enterUserErrorState(Throwable error) {
        enterErrorState(new CallbackExceptionImpl("Exception received from UrlRequest.Callback", error));
    }

    private void enterUploadErrorState(Throwable error) {
        enterErrorState(new CallbackExceptionImpl("Exception received from UploadDataProvider", error));
    }

    private void enterCronetErrorState(Throwable error) {
        enterErrorState(new CronetExceptionImpl("System error", error));
    }

    private void transitionStates(State expected, State newState, Runnable afterTransition) {
        if (this.mState.compareAndSet(expected, newState)) {
            afterTransition.run();
            return;
        }
        State state = (State) this.mState.get();
        if (state != State.CANCELLED && state != State.ERROR) {
            throw new IllegalStateException("Invalid state transition - expected " + expected + " but was " + state);
        }
    }

    public void followRedirect() {
        transitionStates(State.AWAITING_FOLLOW_REDIRECT, State.STARTED, new Runnable() {
            public void run() {
                JavaUrlRequest.this.mCurrentUrl = JavaUrlRequest.this.mPendingRedirectUrl;
                JavaUrlRequest.this.mPendingRedirectUrl = null;
                JavaUrlRequest.this.fireOpenConnection();
            }
        });
    }

    private void fireGetHeaders() {
        this.mAdditionalStatusDetails = 13;
        this.mExecutor.execute(errorSetting(new CheckedRunnable() {
            public void run() throws Exception {
                if (JavaUrlRequest.this.mCurrentUrlConnection != null) {
                    List<Entry<String, String>> headerList = new ArrayList();
                    String selectedTransport = "http/1.1";
                    int i = 0;
                    while (true) {
                        String headerKey = JavaUrlRequest.this.mCurrentUrlConnection.getHeaderFieldKey(i);
                        if (headerKey == null) {
                            break;
                        }
                        if (JavaUrlRequest.X_ANDROID_SELECTED_TRANSPORT.equalsIgnoreCase(headerKey)) {
                            selectedTransport = JavaUrlRequest.this.mCurrentUrlConnection.getHeaderField(i);
                        }
                        if (!headerKey.startsWith(JavaUrlRequest.X_ANDROID)) {
                            headerList.add(new SimpleEntry(headerKey, JavaUrlRequest.this.mCurrentUrlConnection.getHeaderField(i)));
                        }
                        i++;
                    }
                    int responseCode = JavaUrlRequest.this.mCurrentUrlConnection.getResponseCode();
                    JavaUrlRequest.this.mUrlResponseInfo = new UrlResponseInfoImpl(new ArrayList(JavaUrlRequest.this.mUrlChain), responseCode, JavaUrlRequest.this.mCurrentUrlConnection.getResponseMessage(), Collections.unmodifiableList(headerList), false, selectedTransport, "");
                    if (responseCode < 300 || responseCode >= 400) {
                        JavaUrlRequest.this.fireCloseUploadDataProvider();
                        if (responseCode >= 400) {
                            InputStream inputStream = JavaUrlRequest.this.mCurrentUrlConnection.getErrorStream();
                            JavaUrlRequest.this.mResponseChannel = inputStream == null ? null : InputStreamChannel.wrap(inputStream);
                            JavaUrlRequest.this.mCallbackAsync.onResponseStarted(JavaUrlRequest.this.mUrlResponseInfo);
                            return;
                        }
                        JavaUrlRequest.this.mResponseChannel = InputStreamChannel.wrap(JavaUrlRequest.this.mCurrentUrlConnection.getInputStream());
                        JavaUrlRequest.this.mCallbackAsync.onResponseStarted(JavaUrlRequest.this.mUrlResponseInfo);
                        return;
                    }
                    JavaUrlRequest.this.fireRedirectReceived(JavaUrlRequest.this.mUrlResponseInfo.getAllHeaders());
                }
            }
        }));
    }

    private void fireCloseUploadDataProvider() {
        if (this.mUploadDataProvider != null && this.mUploadProviderClosed.compareAndSet(false, true)) {
            try {
                this.mUploadExecutor.execute(uploadErrorSetting(new CheckedRunnable() {
                    public void run() throws Exception {
                        JavaUrlRequest.this.mUploadDataProvider.close();
                    }
                }));
            } catch (RejectedExecutionException e) {
                Log.e(TAG, "Exception when closing uploadDataProvider", e);
            }
        }
    }

    private void fireRedirectReceived(final Map<String, List<String>> headerFields) {
        transitionStates(State.STARTED, State.REDIRECT_RECEIVED, new Runnable() {
            public void run() {
                JavaUrlRequest.this.mPendingRedirectUrl = URI.create(JavaUrlRequest.this.mCurrentUrl).resolve((String) ((List) headerFields.get("location")).get(0)).toString();
                JavaUrlRequest.this.mUrlChain.add(JavaUrlRequest.this.mPendingRedirectUrl);
                JavaUrlRequest.this.transitionStates(State.REDIRECT_RECEIVED, State.AWAITING_FOLLOW_REDIRECT, new Runnable() {
                    public void run() {
                        JavaUrlRequest.this.mCallbackAsync.onRedirectReceived(JavaUrlRequest.this.mUrlResponseInfo, JavaUrlRequest.this.mPendingRedirectUrl);
                    }
                });
            }
        });
    }

    private void fireOpenConnection() {
        this.mExecutor.execute(errorSetting(new CheckedRunnable() {
            public void run() throws Exception {
                if (JavaUrlRequest.this.mState.get() != State.CANCELLED) {
                    URL url = new URL(JavaUrlRequest.this.mCurrentUrl);
                    if (JavaUrlRequest.this.mCurrentUrlConnection != null) {
                        JavaUrlRequest.this.mCurrentUrlConnection.disconnect();
                        JavaUrlRequest.this.mCurrentUrlConnection = null;
                    }
                    JavaUrlRequest.this.mCurrentUrlConnection = (HttpURLConnection) url.openConnection();
                    JavaUrlRequest.this.mCurrentUrlConnection.setInstanceFollowRedirects(false);
                    if (!JavaUrlRequest.this.mRequestHeaders.containsKey(JavaUrlRequest.USER_AGENT)) {
                        JavaUrlRequest.this.mRequestHeaders.put(JavaUrlRequest.USER_AGENT, JavaUrlRequest.this.mUserAgent);
                    }
                    for (Entry<String, String> entry : JavaUrlRequest.this.mRequestHeaders.entrySet()) {
                        JavaUrlRequest.this.mCurrentUrlConnection.setRequestProperty((String) entry.getKey(), (String) entry.getValue());
                    }
                    if (JavaUrlRequest.this.mInitialMethod == null) {
                        JavaUrlRequest.this.mInitialMethod = "GET";
                    }
                    JavaUrlRequest.this.mCurrentUrlConnection.setRequestMethod(JavaUrlRequest.this.mInitialMethod);
                    if (JavaUrlRequest.this.mUploadDataProvider != null) {
                        JavaUrlRequest.this.mOutputStreamDataSink = new OutputStreamDataSink(JavaUrlRequest.this.mUploadExecutor, JavaUrlRequest.this.mExecutor, JavaUrlRequest.this.mCurrentUrlConnection, JavaUrlRequest.this.mUploadDataProvider);
                        JavaUrlRequest.this.mOutputStreamDataSink.start(JavaUrlRequest.this.mUrlChain.size() == 1);
                        return;
                    }
                    JavaUrlRequest.this.mAdditionalStatusDetails = 10;
                    JavaUrlRequest.this.mCurrentUrlConnection.connect();
                    JavaUrlRequest.this.fireGetHeaders();
                }
            }
        }));
    }

    private Runnable errorSetting(final CheckedRunnable delegate) {
        return new Runnable() {
            public void run() {
                try {
                    delegate.run();
                } catch (Throwable t) {
                    JavaUrlRequest.this.enterCronetErrorState(t);
                }
            }
        };
    }

    private Runnable userErrorSetting(final CheckedRunnable delegate) {
        return new Runnable() {
            public void run() {
                try {
                    delegate.run();
                } catch (Throwable t) {
                    JavaUrlRequest.this.enterUserErrorState(t);
                }
            }
        };
    }

    private Runnable uploadErrorSetting(final CheckedRunnable delegate) {
        return new Runnable() {
            public void run() {
                try {
                    delegate.run();
                } catch (Throwable t) {
                    JavaUrlRequest.this.enterUploadErrorState(t);
                }
            }
        };
    }

    public void read(final ByteBuffer buffer) {
        Preconditions.checkDirect(buffer);
        Preconditions.checkHasRemaining(buffer);
        transitionStates(State.AWAITING_READ, State.READING, new Runnable() {
            public void run() {
                JavaUrlRequest.this.mExecutor.execute(JavaUrlRequest.this.errorSetting(new CheckedRunnable() {
                    public void run() throws Exception {
                        JavaUrlRequest.this.processReadResult(JavaUrlRequest.this.mResponseChannel == null ? -1 : JavaUrlRequest.this.mResponseChannel.read(buffer), buffer);
                    }
                }));
            }
        });
    }

    private void processReadResult(int read, ByteBuffer buffer) throws IOException {
        if (read != -1) {
            this.mCallbackAsync.onReadCompleted(this.mUrlResponseInfo, buffer);
            return;
        }
        if (this.mResponseChannel != null) {
            this.mResponseChannel.close();
        }
        if (this.mState.compareAndSet(State.READING, State.COMPLETE)) {
            fireDisconnect();
            this.mCallbackAsync.onSucceeded(this.mUrlResponseInfo);
        }
    }

    private void fireDisconnect() {
        this.mExecutor.execute(new Runnable() {
            public void run() {
                if (JavaUrlRequest.this.mOutputStreamDataSink != null) {
                    try {
                        JavaUrlRequest.this.mOutputStreamDataSink.closeOutputChannel();
                    } catch (IOException e) {
                        Log.e(JavaUrlRequest.TAG, "Exception when closing OutputChannel", e);
                    }
                }
                if (JavaUrlRequest.this.mCurrentUrlConnection != null) {
                    JavaUrlRequest.this.mCurrentUrlConnection.disconnect();
                    JavaUrlRequest.this.mCurrentUrlConnection = null;
                }
            }
        });
    }

    public void cancel() {
        switch ((State) this.mState.getAndSet(State.CANCELLED)) {
            case REDIRECT_RECEIVED:
            case AWAITING_FOLLOW_REDIRECT:
            case AWAITING_READ:
            case STARTED:
            case READING:
                fireDisconnect();
                fireCloseUploadDataProvider();
                this.mCallbackAsync.onCanceled(this.mUrlResponseInfo);
                return;
            default:
                return;
        }
    }

    public boolean isDone() {
        int i = 1;
        State state = (State) this.mState.get();
        int i2 = (state == State.COMPLETE ? 1 : 0) | (state == State.ERROR ? 1 : 0);
        if (state != State.CANCELLED) {
            i = 0;
        }
        return i2 | i;
    }

    public void getStatus(StatusListener listener) {
        int status;
        State state = (State) this.mState.get();
        int extraStatus = this.mAdditionalStatusDetails;
        switch (state) {
            case NOT_STARTED:
            case ERROR:
            case COMPLETE:
            case CANCELLED:
                status = -1;
                break;
            case REDIRECT_RECEIVED:
            case AWAITING_FOLLOW_REDIRECT:
            case AWAITING_READ:
                status = 0;
                break;
            case STARTED:
                status = extraStatus;
                break;
            case READING:
                status = 14;
                break;
            default:
                throw new IllegalStateException("Switch is exhaustive: " + state);
        }
        this.mCallbackAsync.sendStatus(new UrlRequestStatusListener(listener), status);
    }

    private void closeResponseChannel() {
        this.mExecutor.execute(new Runnable() {
            public void run() {
                if (JavaUrlRequest.this.mResponseChannel != null) {
                    try {
                        JavaUrlRequest.this.mResponseChannel.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    JavaUrlRequest.this.mResponseChannel = null;
                }
            }
        });
    }
}
