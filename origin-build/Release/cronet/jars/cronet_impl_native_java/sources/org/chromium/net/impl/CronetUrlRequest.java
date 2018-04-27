package org.chromium.net.impl;

import java.nio.ByteBuffer;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import javax.annotation.concurrent.GuardedBy;
import org.chromium.base.Log;
import org.chromium.base.VisibleForTesting;
import org.chromium.base.annotations.CalledByNative;
import org.chromium.base.annotations.JNIAdditionalImport;
import org.chromium.base.annotations.JNINamespace;
import org.chromium.base.annotations.NativeClassQualifiedName;
import org.chromium.net.CallbackException;
import org.chromium.net.CronetException;
import org.chromium.net.InlineExecutionProhibitedException;
import org.chromium.net.UploadDataProvider;
import org.chromium.net.UrlRequest.Callback;
import org.chromium.net.UrlRequest.StatusListener;
import org.chromium.net.impl.VersionSafeCallbacks.UrlRequestCallback;
import org.chromium.net.impl.VersionSafeCallbacks.UrlRequestStatusListener;

@VisibleForTesting
@JNIAdditionalImport({VersionSafeCallbacks.class})
@JNINamespace("cronet")
public final class CronetUrlRequest extends UrlRequestBase {
    static final /* synthetic */ boolean $assertionsDisabled = (!CronetUrlRequest.class.desiredAssertionStatus());
    private final boolean mAllowDirectExecutor;
    private final UrlRequestCallback mCallback;
    private final boolean mDisableCache;
    private final boolean mDisableConnectionMigration;
    private CronetException mException;
    private final Executor mExecutor;
    private int mFinishedReason;
    private String mInitialMethod;
    private final String mInitialUrl;
    private CronetMetrics mMetrics;
    @GuardedBy("mUrlRequestAdapterLock")
    private Runnable mOnDestroyedCallbackForTesting;
    private OnReadCompletedRunnable mOnReadCompletedTask;
    private final int mPriority;
    private long mReceivedByteCountFromRedirects;
    private final Collection<Object> mRequestAnnotations;
    private final CronetUrlRequestContext mRequestContext;
    private final HeadersList mRequestHeaders = new HeadersList();
    private UrlResponseInfoImpl mResponseInfo;
    @GuardedBy("mUrlRequestAdapterLock")
    private boolean mStarted;
    private CronetUploadDataStream mUploadDataStream;
    private final List<String> mUrlChain = new ArrayList();
    @GuardedBy("mUrlRequestAdapterLock")
    private long mUrlRequestAdapter;
    private final Object mUrlRequestAdapterLock = new Object();
    @GuardedBy("mUrlRequestAdapterLock")
    private boolean mWaitingOnRead;
    @GuardedBy("mUrlRequestAdapterLock")
    private boolean mWaitingOnRedirect;

    private static final class HeadersList extends ArrayList<Entry<String, String>> {
        private HeadersList() {
        }
    }

    private final class OnReadCompletedRunnable implements Runnable {
        ByteBuffer mByteBuffer;

        private OnReadCompletedRunnable() {
        }

        public void run() {
            CronetUrlRequest.this.checkCallingThread();
            ByteBuffer buffer = this.mByteBuffer;
            this.mByteBuffer = null;
            try {
                synchronized (CronetUrlRequest.this.mUrlRequestAdapterLock) {
                    if (CronetUrlRequest.this.isDoneLocked()) {
                        return;
                    }
                    CronetUrlRequest.this.mWaitingOnRead = true;
                    CronetUrlRequest.this.mCallback.onReadCompleted(CronetUrlRequest.this, CronetUrlRequest.this.mResponseInfo, buffer);
                }
            } catch (Exception e) {
                CronetUrlRequest.this.onCallbackException(e);
            }
        }
    }

    @NativeClassQualifiedName("CronetURLRequestAdapter")
    private native boolean nativeAddRequestHeader(long j, String str, String str2);

    private native long nativeCreateRequestAdapter(long j, String str, int i, boolean z, boolean z2, boolean z3);

    @NativeClassQualifiedName("CronetURLRequestAdapter")
    private native void nativeDestroy(long j, boolean z);

    @NativeClassQualifiedName("CronetURLRequestAdapter")
    private native void nativeFollowDeferredRedirect(long j);

    @NativeClassQualifiedName("CronetURLRequestAdapter")
    private native void nativeGetStatus(long j, UrlRequestStatusListener urlRequestStatusListener);

    @NativeClassQualifiedName("CronetURLRequestAdapter")
    private native boolean nativeReadData(long j, ByteBuffer byteBuffer, int i, int i2);

    @NativeClassQualifiedName("CronetURLRequestAdapter")
    private native boolean nativeSetHttpMethod(long j, String str);

    @NativeClassQualifiedName("CronetURLRequestAdapter")
    private native void nativeStart(long j);

    CronetUrlRequest(CronetUrlRequestContext requestContext, String url, int priority, Callback callback, Executor executor, Collection<Object> requestAnnotations, boolean disableCache, boolean disableConnectionMigration, boolean allowDirectExecutor) {
        if (url == null) {
            throw new NullPointerException("URL is required");
        } else if (callback == null) {
            throw new NullPointerException("Listener is required");
        } else if (executor == null) {
            throw new NullPointerException("Executor is required");
        } else {
            this.mAllowDirectExecutor = allowDirectExecutor;
            this.mRequestContext = requestContext;
            this.mInitialUrl = url;
            this.mUrlChain.add(url);
            this.mPriority = convertRequestPriority(priority);
            this.mCallback = new UrlRequestCallback(callback);
            this.mExecutor = executor;
            this.mRequestAnnotations = requestAnnotations;
            this.mDisableCache = disableCache;
            this.mDisableConnectionMigration = disableConnectionMigration;
        }
    }

    public void setHttpMethod(String method) {
        checkNotStarted();
        if (method == null) {
            throw new NullPointerException("Method is required.");
        }
        this.mInitialMethod = method;
    }

    public void addHeader(String header, String value) {
        checkNotStarted();
        if (header == null) {
            throw new NullPointerException("Invalid header name.");
        } else if (value == null) {
            throw new NullPointerException("Invalid header value.");
        } else {
            this.mRequestHeaders.add(new SimpleImmutableEntry(header, value));
        }
    }

    public void setUploadDataProvider(UploadDataProvider uploadDataProvider, Executor executor) {
        if (uploadDataProvider == null) {
            throw new NullPointerException("Invalid UploadDataProvider.");
        }
        if (this.mInitialMethod == null) {
            this.mInitialMethod = "POST";
        }
        this.mUploadDataStream = new CronetUploadDataStream(uploadDataProvider, executor, this);
    }

    public void start() {
        synchronized (this.mUrlRequestAdapterLock) {
            checkNotStarted();
            try {
                this.mUrlRequestAdapter = nativeCreateRequestAdapter(this.mRequestContext.getUrlRequestContextAdapter(), this.mInitialUrl, this.mPriority, this.mDisableCache, this.mDisableConnectionMigration, this.mRequestContext.hasRequestFinishedListener());
                this.mRequestContext.onRequestStarted();
                if (this.mInitialMethod == null || nativeSetHttpMethod(this.mUrlRequestAdapter, this.mInitialMethod)) {
                    boolean hasContentType = false;
                    Iterator it = this.mRequestHeaders.iterator();
                    while (it.hasNext()) {
                        Entry<String, String> header = (Entry) it.next();
                        if (((String) header.getKey()).equalsIgnoreCase("Content-Type") && !((String) header.getValue()).isEmpty()) {
                            hasContentType = true;
                        }
                        if (!nativeAddRequestHeader(this.mUrlRequestAdapter, (String) header.getKey(), (String) header.getValue())) {
                            throw new IllegalArgumentException("Invalid header " + ((String) header.getKey()) + "=" + ((String) header.getValue()));
                        }
                    }
                    if (this.mUploadDataStream == null) {
                        this.mStarted = true;
                        startInternalLocked();
                        return;
                    } else if (hasContentType) {
                        this.mStarted = true;
                        this.mUploadDataStream.postTaskToExecutor(new Runnable() {
                            public void run() {
                                CronetUrlRequest.this.mUploadDataStream.initializeWithRequest();
                                synchronized (CronetUrlRequest.this.mUrlRequestAdapterLock) {
                                    if (CronetUrlRequest.this.isDoneLocked()) {
                                        return;
                                    }
                                    CronetUrlRequest.this.mUploadDataStream.attachNativeAdapterToRequest(CronetUrlRequest.this.mUrlRequestAdapter);
                                    CronetUrlRequest.this.startInternalLocked();
                                }
                            }
                        });
                        return;
                    } else {
                        throw new IllegalArgumentException("Requests with upload data must have a Content-Type.");
                    }
                }
                throw new IllegalArgumentException("Invalid http method " + this.mInitialMethod);
            } catch (RuntimeException e) {
                destroyRequestAdapterLocked(1);
                throw e;
            }
        }
    }

    @GuardedBy("mUrlRequestAdapterLock")
    private void startInternalLocked() {
        nativeStart(this.mUrlRequestAdapter);
    }

    public void followRedirect() {
        synchronized (this.mUrlRequestAdapterLock) {
            if (this.mWaitingOnRedirect) {
                this.mWaitingOnRedirect = false;
                if (isDoneLocked()) {
                    return;
                }
                nativeFollowDeferredRedirect(this.mUrlRequestAdapter);
                return;
            }
            throw new IllegalStateException("No redirect to follow.");
        }
    }

    public void read(ByteBuffer buffer) {
        Preconditions.checkHasRemaining(buffer);
        Preconditions.checkDirect(buffer);
        synchronized (this.mUrlRequestAdapterLock) {
            if (this.mWaitingOnRead) {
                this.mWaitingOnRead = false;
                if (isDoneLocked()) {
                    return;
                }
                if (nativeReadData(this.mUrlRequestAdapter, buffer, buffer.position(), buffer.limit())) {
                    return;
                } else {
                    this.mWaitingOnRead = true;
                    throw new IllegalArgumentException("Unable to call native read");
                }
            }
            throw new IllegalStateException("Unexpected read attempt.");
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void cancel() {
        /*
        r2 = this;
        r1 = r2.mUrlRequestAdapterLock;
        monitor-enter(r1);
        r0 = r2.isDoneLocked();	 Catch:{ all -> 0x0015 }
        if (r0 != 0) goto L_0x000d;
    L_0x0009:
        r0 = r2.mStarted;	 Catch:{ all -> 0x0015 }
        if (r0 != 0) goto L_0x000f;
    L_0x000d:
        monitor-exit(r1);	 Catch:{ all -> 0x0015 }
    L_0x000e:
        return;
    L_0x000f:
        r0 = 2;
        r2.destroyRequestAdapterLocked(r0);	 Catch:{ all -> 0x0015 }
        monitor-exit(r1);	 Catch:{ all -> 0x0015 }
        goto L_0x000e;
    L_0x0015:
        r0 = move-exception;
        monitor-exit(r1);	 Catch:{ all -> 0x0015 }
        throw r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.chromium.net.impl.CronetUrlRequest.cancel():void");
    }

    public boolean isDone() {
        boolean isDoneLocked;
        synchronized (this.mUrlRequestAdapterLock) {
            isDoneLocked = isDoneLocked();
        }
        return isDoneLocked;
    }

    @GuardedBy("mUrlRequestAdapterLock")
    private boolean isDoneLocked() {
        return this.mStarted && this.mUrlRequestAdapter == 0;
    }

    public void getStatus(StatusListener unsafeListener) {
        final UrlRequestStatusListener listener = new UrlRequestStatusListener(unsafeListener);
        synchronized (this.mUrlRequestAdapterLock) {
            if (this.mUrlRequestAdapter != 0) {
                nativeGetStatus(this.mUrlRequestAdapter, listener);
                return;
            }
            postTaskToExecutor(new Runnable() {
                public void run() {
                    listener.onStatus(-1);
                }
            });
        }
    }

    @VisibleForTesting
    public void setOnDestroyedCallbackForTesting(Runnable onDestroyedCallbackForTesting) {
        synchronized (this.mUrlRequestAdapterLock) {
            this.mOnDestroyedCallbackForTesting = onDestroyedCallbackForTesting;
        }
    }

    @VisibleForTesting
    public void setOnDestroyedUploadCallbackForTesting(Runnable onDestroyedUploadCallbackForTesting) {
        this.mUploadDataStream.setOnDestroyedCallbackForTesting(onDestroyedUploadCallbackForTesting);
    }

    @VisibleForTesting
    public long getUrlRequestAdapterForTesting() {
        long j;
        synchronized (this.mUrlRequestAdapterLock) {
            j = this.mUrlRequestAdapter;
        }
        return j;
    }

    private void postTaskToExecutor(Runnable task) {
        try {
            this.mExecutor.execute(task);
        } catch (RejectedExecutionException failException) {
            Log.e(CronetUrlRequestContext.LOG_TAG, "Exception posting task to executor", failException);
            failWithException(new CronetExceptionImpl("Exception posting task to executor", failException));
        }
    }

    private static int convertRequestPriority(int priority) {
        switch (priority) {
            case 0:
                return 1;
            case 1:
                return 2;
            case 2:
                return 3;
            case 4:
                return 5;
            default:
                return 4;
        }
    }

    private UrlResponseInfoImpl prepareResponseInfoOnNetworkThread(int httpStatusCode, String httpStatusText, String[] headers, boolean wasCached, String negotiatedProtocol, String proxyServer) {
        HeadersList headersList = new HeadersList();
        for (int i = 0; i < headers.length; i += 2) {
            headersList.add(new SimpleImmutableEntry(headers[i], headers[i + 1]));
        }
        return new UrlResponseInfoImpl(new ArrayList(this.mUrlChain), httpStatusCode, httpStatusText, headersList, wasCached, negotiatedProtocol, proxyServer);
    }

    private void checkNotStarted() {
        synchronized (this.mUrlRequestAdapterLock) {
            if (this.mStarted || isDoneLocked()) {
                throw new IllegalStateException("Request is already started.");
            }
        }
    }

    @GuardedBy("mUrlRequestAdapterLock")
    private void destroyRequestAdapterLocked(int finishedReason) {
        boolean z = true;
        if ($assertionsDisabled || this.mException == null || finishedReason == 1) {
            this.mFinishedReason = finishedReason;
            if (this.mUrlRequestAdapter != 0) {
                this.mRequestContext.onRequestDestroyed();
                long j = this.mUrlRequestAdapter;
                if (finishedReason != 2) {
                    z = false;
                }
                nativeDestroy(j, z);
                this.mUrlRequestAdapter = 0;
                return;
            }
            return;
        }
        throw new AssertionError();
    }

    private void onCallbackException(Exception e) {
        CallbackException requestError = new CallbackExceptionImpl("Exception received from UrlRequest.Callback", e);
        Log.e(CronetUrlRequestContext.LOG_TAG, "Exception in CalledByNative method", e);
        failWithException(requestError);
    }

    void onUploadException(Throwable e) {
        CallbackException uploadError = new CallbackExceptionImpl("Exception received from UploadDataProvider", e);
        Log.e(CronetUrlRequestContext.LOG_TAG, "Exception in upload method", e);
        failWithException(uploadError);
    }

    private void failWithException(CronetException exception) {
        synchronized (this.mUrlRequestAdapterLock) {
            if (isDoneLocked()) {
            } else if ($assertionsDisabled || this.mException == null) {
                this.mException = exception;
                destroyRequestAdapterLocked(1);
            } else {
                throw new AssertionError();
            }
        }
    }

    @CalledByNative
    private void onRedirectReceived(final String newLocation, int httpStatusCode, String httpStatusText, String[] headers, boolean wasCached, String negotiatedProtocol, String proxyServer, long receivedByteCount) {
        final UrlResponseInfoImpl responseInfo = prepareResponseInfoOnNetworkThread(httpStatusCode, httpStatusText, headers, wasCached, negotiatedProtocol, proxyServer);
        this.mReceivedByteCountFromRedirects += receivedByteCount;
        responseInfo.setReceivedByteCount(this.mReceivedByteCountFromRedirects);
        this.mUrlChain.add(newLocation);
        postTaskToExecutor(new Runnable() {
            public void run() {
                CronetUrlRequest.this.checkCallingThread();
                synchronized (CronetUrlRequest.this.mUrlRequestAdapterLock) {
                    if (CronetUrlRequest.this.isDoneLocked()) {
                        return;
                    }
                    CronetUrlRequest.this.mWaitingOnRedirect = true;
                    try {
                        CronetUrlRequest.this.mCallback.onRedirectReceived(CronetUrlRequest.this, responseInfo, newLocation);
                    } catch (Exception e) {
                        CronetUrlRequest.this.onCallbackException(e);
                    }
                }
            }
        });
    }

    @CalledByNative
    private void onResponseStarted(int httpStatusCode, String httpStatusText, String[] headers, boolean wasCached, String negotiatedProtocol, String proxyServer) {
        this.mResponseInfo = prepareResponseInfoOnNetworkThread(httpStatusCode, httpStatusText, headers, wasCached, negotiatedProtocol, proxyServer);
        postTaskToExecutor(new Runnable() {
            public void run() {
                CronetUrlRequest.this.checkCallingThread();
                synchronized (CronetUrlRequest.this.mUrlRequestAdapterLock) {
                    if (CronetUrlRequest.this.isDoneLocked()) {
                        return;
                    }
                    CronetUrlRequest.this.mWaitingOnRead = true;
                    try {
                        CronetUrlRequest.this.mCallback.onResponseStarted(CronetUrlRequest.this, CronetUrlRequest.this.mResponseInfo);
                    } catch (Exception e) {
                        CronetUrlRequest.this.onCallbackException(e);
                    }
                }
            }
        });
    }

    @CalledByNative
    private void onReadCompleted(ByteBuffer byteBuffer, int bytesRead, int initialPosition, int initialLimit, long receivedByteCount) {
        this.mResponseInfo.setReceivedByteCount(this.mReceivedByteCountFromRedirects + receivedByteCount);
        if (byteBuffer.position() == initialPosition && byteBuffer.limit() == initialLimit) {
            if (this.mOnReadCompletedTask == null) {
                this.mOnReadCompletedTask = new OnReadCompletedRunnable();
            }
            byteBuffer.position(initialPosition + bytesRead);
            this.mOnReadCompletedTask.mByteBuffer = byteBuffer;
            postTaskToExecutor(this.mOnReadCompletedTask);
            return;
        }
        failWithException(new CronetExceptionImpl("ByteBuffer modified externally during read", null));
    }

    @CalledByNative
    private void onSucceeded(long receivedByteCount) {
        this.mResponseInfo.setReceivedByteCount(this.mReceivedByteCountFromRedirects + receivedByteCount);
        postTaskToExecutor(new Runnable() {
            public void run() {
                synchronized (CronetUrlRequest.this.mUrlRequestAdapterLock) {
                    if (CronetUrlRequest.this.isDoneLocked()) {
                        return;
                    }
                    CronetUrlRequest.this.destroyRequestAdapterLocked(0);
                    try {
                        CronetUrlRequest.this.mCallback.onSucceeded(CronetUrlRequest.this, CronetUrlRequest.this.mResponseInfo);
                        CronetUrlRequest.this.maybeReportMetrics();
                    } catch (Exception e) {
                        Log.e(CronetUrlRequestContext.LOG_TAG, "Exception in onSucceeded method", e);
                    }
                }
            }
        });
    }

    @CalledByNative
    private void onError(int errorCode, int nativeError, int nativeQuicError, String errorString, long receivedByteCount) {
        if (this.mResponseInfo != null) {
            this.mResponseInfo.setReceivedByteCount(this.mReceivedByteCountFromRedirects + receivedByteCount);
        }
        if (errorCode == 10) {
            failWithException(new QuicExceptionImpl("Exception in CronetUrlRequest: " + errorString, nativeError, nativeQuicError));
            return;
        }
        failWithException(new NetworkExceptionImpl("Exception in CronetUrlRequest: " + errorString, mapUrlRequestErrorToApiErrorCode(errorCode), nativeError));
    }

    @CalledByNative
    private void onCanceled() {
        postTaskToExecutor(new Runnable() {
            public void run() {
                try {
                    CronetUrlRequest.this.mCallback.onCanceled(CronetUrlRequest.this, CronetUrlRequest.this.mResponseInfo);
                    CronetUrlRequest.this.maybeReportMetrics();
                } catch (Exception e) {
                    Log.e(CronetUrlRequestContext.LOG_TAG, "Exception in onCanceled method", e);
                }
            }
        });
    }

    @CalledByNative
    private void onStatus(final UrlRequestStatusListener listener, final int loadState) {
        postTaskToExecutor(new Runnable() {
            public void run() {
                listener.onStatus(UrlRequestBase.convertLoadState(loadState));
            }
        });
    }

    @CalledByNative
    private void onMetricsCollected(long requestStartMs, long dnsStartMs, long dnsEndMs, long connectStartMs, long connectEndMs, long sslStartMs, long sslEndMs, long sendingStartMs, long sendingEndMs, long pushStartMs, long pushEndMs, long responseStartMs, long requestEndMs, boolean socketReused, long sentByteCount, long receivedByteCount) {
        synchronized (this.mUrlRequestAdapterLock) {
            if (this.mMetrics != null) {
                throw new IllegalStateException("Metrics collection should only happen once.");
            }
            this.mMetrics = new CronetMetrics(requestStartMs, dnsStartMs, dnsEndMs, connectStartMs, connectEndMs, sslStartMs, sslEndMs, sendingStartMs, sendingEndMs, pushStartMs, pushEndMs, responseStartMs, requestEndMs, socketReused, sentByteCount, receivedByteCount);
        }
    }

    @CalledByNative
    private void onNativeAdapterDestroyed() {
        synchronized (this.mUrlRequestAdapterLock) {
            if (this.mOnDestroyedCallbackForTesting != null) {
                this.mOnDestroyedCallbackForTesting.run();
            }
            if (this.mException == null) {
                return;
            }
            try {
                this.mExecutor.execute(new Runnable() {
                    public void run() {
                        try {
                            CronetUrlRequest.this.mCallback.onFailed(CronetUrlRequest.this, CronetUrlRequest.this.mResponseInfo, CronetUrlRequest.this.mException);
                            CronetUrlRequest.this.maybeReportMetrics();
                        } catch (Exception e) {
                            Log.e(CronetUrlRequestContext.LOG_TAG, "Exception in onFailed method", e);
                        }
                    }
                });
            } catch (RejectedExecutionException e) {
                Log.e(CronetUrlRequestContext.LOG_TAG, "Exception posting task to executor", e);
            }
        }
    }

    void checkCallingThread() {
        if (!this.mAllowDirectExecutor && this.mRequestContext.isNetworkThread(Thread.currentThread())) {
            throw new InlineExecutionProhibitedException();
        }
    }

    private int mapUrlRequestErrorToApiErrorCode(int errorCode) {
        switch (errorCode) {
            case 1:
                return 1;
            case 2:
                return 2;
            case 3:
                return 3;
            case 4:
                return 4;
            case 5:
                return 5;
            case 6:
                return 6;
            case 7:
                return 7;
            case 8:
                return 8;
            case 9:
                return 9;
            case 10:
                return 10;
            case 11:
                return 11;
            default:
                Log.e(CronetUrlRequestContext.LOG_TAG, "Unknown error code: " + errorCode, new Object[0]);
                return errorCode;
        }
    }

    private void maybeReportMetrics() {
        if (this.mMetrics != null) {
            this.mRequestContext.reportFinished(new RequestFinishedInfoImpl(this.mInitialUrl, this.mRequestAnnotations, this.mMetrics, this.mFinishedReason, this.mResponseInfo, this.mException));
        }
    }
}
