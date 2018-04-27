package org.chromium.net.impl;

import java.nio.ByteBuffer;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import javax.annotation.concurrent.GuardedBy;
import org.chromium.base.Log;
import org.chromium.base.VisibleForTesting;
import org.chromium.base.annotations.CalledByNative;
import org.chromium.base.annotations.JNINamespace;
import org.chromium.base.annotations.NativeClassQualifiedName;
import org.chromium.net.BidirectionalStream.Callback;
import org.chromium.net.CallbackException;
import org.chromium.net.CronetException;
import org.chromium.net.ExperimentalBidirectionalStream;
import org.chromium.net.RequestFinishedInfo.Metrics;
import org.chromium.net.UrlResponseInfo.HeaderBlock;
import org.chromium.net.impl.UrlResponseInfoImpl.HeaderBlockImpl;
import org.chromium.net.impl.VersionSafeCallbacks.BidirectionalStreamCallback;

@VisibleForTesting
@JNINamespace("cronet")
public class CronetBidirectionalStream extends ExperimentalBidirectionalStream {
    static final /* synthetic */ boolean $assertionsDisabled = (!CronetBidirectionalStream.class.desiredAssertionStatus());
    private final BidirectionalStreamCallback mCallback;
    private final boolean mDelayRequestHeadersUntilFirstFlush;
    @GuardedBy("mNativeStreamLock")
    private boolean mEndOfStreamWritten;
    private CronetException mException;
    private final Executor mExecutor;
    @GuardedBy("mNativeStreamLock")
    private LinkedList<ByteBuffer> mFlushData;
    private final String mInitialMethod;
    private final int mInitialPriority;
    private final String mInitialUrl;
    @GuardedBy("mNativeStreamLock")
    private Metrics mMetrics;
    @GuardedBy("mNativeStreamLock")
    private long mNativeStream;
    private final Object mNativeStreamLock = new Object();
    private Runnable mOnDestroyedCallbackForTesting;
    private OnReadCompletedRunnable mOnReadCompletedTask;
    @GuardedBy("mNativeStreamLock")
    private LinkedList<ByteBuffer> mPendingData;
    @GuardedBy("mNativeStreamLock")
    private State mReadState = State.NOT_STARTED;
    private final Collection<Object> mRequestAnnotations;
    private final CronetUrlRequestContext mRequestContext;
    private final String[] mRequestHeaders;
    @GuardedBy("mNativeStreamLock")
    private boolean mRequestHeadersSent;
    private UrlResponseInfoImpl mResponseInfo;
    @GuardedBy("mNativeStreamLock")
    private State mWriteState = State.NOT_STARTED;

    private final class OnReadCompletedRunnable implements Runnable {
        ByteBuffer mByteBuffer;
        boolean mEndOfStream;

        private OnReadCompletedRunnable() {
        }

        /* JADX WARNING: inconsistent code. */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void run() {
            /*
            r7 = this;
            r0 = r7.mByteBuffer;	 Catch:{ Exception -> 0x0049 }
            r3 = 0;
            r7.mByteBuffer = r3;	 Catch:{ Exception -> 0x0049 }
            r2 = 0;
            r3 = org.chromium.net.impl.CronetBidirectionalStream.this;	 Catch:{ Exception -> 0x0049 }
            r4 = r3.mNativeStreamLock;	 Catch:{ Exception -> 0x0049 }
            monitor-enter(r4);	 Catch:{ Exception -> 0x0049 }
            r3 = org.chromium.net.impl.CronetBidirectionalStream.this;	 Catch:{ all -> 0x005a }
            r3 = r3.isDoneLocked();	 Catch:{ all -> 0x005a }
            if (r3 == 0) goto L_0x0017;
        L_0x0015:
            monitor-exit(r4);	 Catch:{ all -> 0x005a }
        L_0x0016:
            return;
        L_0x0017:
            r3 = r7.mEndOfStream;	 Catch:{ all -> 0x005a }
            if (r3 == 0) goto L_0x0052;
        L_0x001b:
            r3 = org.chromium.net.impl.CronetBidirectionalStream.this;	 Catch:{ all -> 0x005a }
            r5 = org.chromium.net.impl.CronetBidirectionalStream.State.READING_DONE;	 Catch:{ all -> 0x005a }
            r3.mReadState = r5;	 Catch:{ all -> 0x005a }
            r3 = org.chromium.net.impl.CronetBidirectionalStream.this;	 Catch:{ all -> 0x005a }
            r3 = r3.mWriteState;	 Catch:{ all -> 0x005a }
            r5 = org.chromium.net.impl.CronetBidirectionalStream.State.WRITING_DONE;	 Catch:{ all -> 0x005a }
            if (r3 != r5) goto L_0x0050;
        L_0x002c:
            r2 = 1;
        L_0x002d:
            monitor-exit(r4);	 Catch:{ all -> 0x005a }
            r3 = org.chromium.net.impl.CronetBidirectionalStream.this;	 Catch:{ Exception -> 0x0049 }
            r3 = r3.mCallback;	 Catch:{ Exception -> 0x0049 }
            r4 = org.chromium.net.impl.CronetBidirectionalStream.this;	 Catch:{ Exception -> 0x0049 }
            r5 = org.chromium.net.impl.CronetBidirectionalStream.this;	 Catch:{ Exception -> 0x0049 }
            r5 = r5.mResponseInfo;	 Catch:{ Exception -> 0x0049 }
            r6 = r7.mEndOfStream;	 Catch:{ Exception -> 0x0049 }
            r3.onReadCompleted(r4, r5, r0, r6);	 Catch:{ Exception -> 0x0049 }
            if (r2 == 0) goto L_0x0016;
        L_0x0043:
            r3 = org.chromium.net.impl.CronetBidirectionalStream.this;	 Catch:{ Exception -> 0x0049 }
            r3.maybeOnSucceededOnExecutor();	 Catch:{ Exception -> 0x0049 }
            goto L_0x0016;
        L_0x0049:
            r1 = move-exception;
            r3 = org.chromium.net.impl.CronetBidirectionalStream.this;
            r3.onCallbackException(r1);
            goto L_0x0016;
        L_0x0050:
            r2 = 0;
            goto L_0x002d;
        L_0x0052:
            r3 = org.chromium.net.impl.CronetBidirectionalStream.this;	 Catch:{ all -> 0x005a }
            r5 = org.chromium.net.impl.CronetBidirectionalStream.State.WAITING_FOR_READ;	 Catch:{ all -> 0x005a }
            r3.mReadState = r5;	 Catch:{ all -> 0x005a }
            goto L_0x002d;
        L_0x005a:
            r3 = move-exception;
            monitor-exit(r4);	 Catch:{ all -> 0x005a }
            throw r3;	 Catch:{ Exception -> 0x0049 }
            */
            throw new UnsupportedOperationException("Method not decompiled: org.chromium.net.impl.CronetBidirectionalStream.OnReadCompletedRunnable.run():void");
        }
    }

    private final class OnWriteCompletedRunnable implements Runnable {
        private ByteBuffer mByteBuffer;
        private final boolean mEndOfStream;

        OnWriteCompletedRunnable(ByteBuffer buffer, boolean endOfStream) {
            this.mByteBuffer = buffer;
            this.mEndOfStream = endOfStream;
        }

        /* JADX WARNING: inconsistent code. */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void run() {
            /*
            r7 = this;
            r0 = r7.mByteBuffer;	 Catch:{ Exception -> 0x0049 }
            r3 = 0;
            r7.mByteBuffer = r3;	 Catch:{ Exception -> 0x0049 }
            r2 = 0;
            r3 = org.chromium.net.impl.CronetBidirectionalStream.this;	 Catch:{ Exception -> 0x0049 }
            r4 = r3.mNativeStreamLock;	 Catch:{ Exception -> 0x0049 }
            monitor-enter(r4);	 Catch:{ Exception -> 0x0049 }
            r3 = org.chromium.net.impl.CronetBidirectionalStream.this;	 Catch:{ all -> 0x0052 }
            r3 = r3.isDoneLocked();	 Catch:{ all -> 0x0052 }
            if (r3 == 0) goto L_0x0017;
        L_0x0015:
            monitor-exit(r4);	 Catch:{ all -> 0x0052 }
        L_0x0016:
            return;
        L_0x0017:
            r3 = r7.mEndOfStream;	 Catch:{ all -> 0x0052 }
            if (r3 == 0) goto L_0x002d;
        L_0x001b:
            r3 = org.chromium.net.impl.CronetBidirectionalStream.this;	 Catch:{ all -> 0x0052 }
            r5 = org.chromium.net.impl.CronetBidirectionalStream.State.WRITING_DONE;	 Catch:{ all -> 0x0052 }
            r3.mWriteState = r5;	 Catch:{ all -> 0x0052 }
            r3 = org.chromium.net.impl.CronetBidirectionalStream.this;	 Catch:{ all -> 0x0052 }
            r3 = r3.mReadState;	 Catch:{ all -> 0x0052 }
            r5 = org.chromium.net.impl.CronetBidirectionalStream.State.READING_DONE;	 Catch:{ all -> 0x0052 }
            if (r3 != r5) goto L_0x0050;
        L_0x002c:
            r2 = 1;
        L_0x002d:
            monitor-exit(r4);	 Catch:{ all -> 0x0052 }
            r3 = org.chromium.net.impl.CronetBidirectionalStream.this;	 Catch:{ Exception -> 0x0049 }
            r3 = r3.mCallback;	 Catch:{ Exception -> 0x0049 }
            r4 = org.chromium.net.impl.CronetBidirectionalStream.this;	 Catch:{ Exception -> 0x0049 }
            r5 = org.chromium.net.impl.CronetBidirectionalStream.this;	 Catch:{ Exception -> 0x0049 }
            r5 = r5.mResponseInfo;	 Catch:{ Exception -> 0x0049 }
            r6 = r7.mEndOfStream;	 Catch:{ Exception -> 0x0049 }
            r3.onWriteCompleted(r4, r5, r0, r6);	 Catch:{ Exception -> 0x0049 }
            if (r2 == 0) goto L_0x0016;
        L_0x0043:
            r3 = org.chromium.net.impl.CronetBidirectionalStream.this;	 Catch:{ Exception -> 0x0049 }
            r3.maybeOnSucceededOnExecutor();	 Catch:{ Exception -> 0x0049 }
            goto L_0x0016;
        L_0x0049:
            r1 = move-exception;
            r3 = org.chromium.net.impl.CronetBidirectionalStream.this;
            r3.onCallbackException(r1);
            goto L_0x0016;
        L_0x0050:
            r2 = 0;
            goto L_0x002d;
        L_0x0052:
            r3 = move-exception;
            monitor-exit(r4);	 Catch:{ all -> 0x0052 }
            throw r3;	 Catch:{ Exception -> 0x0049 }
            */
            throw new UnsupportedOperationException("Method not decompiled: org.chromium.net.impl.CronetBidirectionalStream.OnWriteCompletedRunnable.run():void");
        }
    }

    private enum State {
        NOT_STARTED,
        STARTED,
        WAITING_FOR_READ,
        READING,
        READING_DONE,
        CANCELED,
        ERROR,
        SUCCESS,
        WAITING_FOR_FLUSH,
        WRITING,
        WRITING_DONE
    }

    private native long nativeCreateBidirectionalStream(long j, boolean z, boolean z2);

    @NativeClassQualifiedName("CronetBidirectionalStreamAdapter")
    private native void nativeDestroy(long j, boolean z);

    @NativeClassQualifiedName("CronetBidirectionalStreamAdapter")
    private native boolean nativeReadData(long j, ByteBuffer byteBuffer, int i, int i2);

    @NativeClassQualifiedName("CronetBidirectionalStreamAdapter")
    private native void nativeSendRequestHeaders(long j);

    @NativeClassQualifiedName("CronetBidirectionalStreamAdapter")
    private native int nativeStart(long j, String str, int i, String str2, String[] strArr, boolean z);

    @NativeClassQualifiedName("CronetBidirectionalStreamAdapter")
    private native boolean nativeWritevData(long j, ByteBuffer[] byteBufferArr, int[] iArr, int[] iArr2, boolean z);

    CronetBidirectionalStream(CronetUrlRequestContext requestContext, String url, int priority, Callback callback, Executor executor, String httpMethod, List<Entry<String, String>> requestHeaders, boolean delayRequestHeadersUntilNextFlush, Collection<Object> requestAnnotations) {
        this.mRequestContext = requestContext;
        this.mInitialUrl = url;
        this.mInitialPriority = convertStreamPriority(priority);
        this.mCallback = new BidirectionalStreamCallback(callback);
        this.mExecutor = executor;
        this.mInitialMethod = httpMethod;
        this.mRequestHeaders = stringsFromHeaderList(requestHeaders);
        this.mDelayRequestHeadersUntilFirstFlush = delayRequestHeadersUntilNextFlush;
        this.mPendingData = new LinkedList();
        this.mFlushData = new LinkedList();
        this.mRequestAnnotations = requestAnnotations;
    }

    public void start() {
        boolean z = true;
        synchronized (this.mNativeStreamLock) {
            if (this.mReadState != State.NOT_STARTED) {
                throw new IllegalStateException("Stream is already started.");
            }
            try {
                boolean z2;
                long urlRequestContextAdapter = this.mRequestContext.getUrlRequestContextAdapter();
                if (this.mDelayRequestHeadersUntilFirstFlush) {
                    z2 = false;
                } else {
                    z2 = true;
                }
                this.mNativeStream = nativeCreateBidirectionalStream(urlRequestContextAdapter, z2, this.mRequestContext.hasRequestFinishedListener());
                this.mRequestContext.onRequestStarted();
                long j = this.mNativeStream;
                String str = this.mInitialUrl;
                int i = this.mInitialPriority;
                String str2 = this.mInitialMethod;
                String[] strArr = this.mRequestHeaders;
                if (doesMethodAllowWriteData(this.mInitialMethod)) {
                    z = false;
                }
                int startResult = nativeStart(j, str, i, str2, strArr, z);
                if (startResult == -1) {
                    throw new IllegalArgumentException("Invalid http method " + this.mInitialMethod);
                } else if (startResult > 0) {
                    int headerPos = startResult - 1;
                    throw new IllegalArgumentException("Invalid header " + this.mRequestHeaders[headerPos] + "=" + this.mRequestHeaders[headerPos + 1]);
                } else {
                    State state = State.STARTED;
                    this.mWriteState = state;
                    this.mReadState = state;
                }
            } catch (RuntimeException e) {
                destroyNativeStreamLocked(false);
                throw e;
            }
        }
    }

    public void read(ByteBuffer buffer) {
        synchronized (this.mNativeStreamLock) {
            Preconditions.checkHasRemaining(buffer);
            Preconditions.checkDirect(buffer);
            if (this.mReadState != State.WAITING_FOR_READ) {
                throw new IllegalStateException("Unexpected read attempt.");
            } else if (isDoneLocked()) {
            } else {
                if (this.mOnReadCompletedTask == null) {
                    this.mOnReadCompletedTask = new OnReadCompletedRunnable();
                }
                this.mReadState = State.READING;
                if (nativeReadData(this.mNativeStream, buffer, buffer.position(), buffer.limit())) {
                } else {
                    this.mReadState = State.WAITING_FOR_READ;
                    throw new IllegalArgumentException("Unable to call native read");
                }
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void write(java.nio.ByteBuffer r4, boolean r5) {
        /*
        r3 = this;
        r1 = r3.mNativeStreamLock;
        monitor-enter(r1);
        org.chromium.net.impl.Preconditions.checkDirect(r4);	 Catch:{ all -> 0x0016 }
        r0 = r4.hasRemaining();	 Catch:{ all -> 0x0016 }
        if (r0 != 0) goto L_0x0019;
    L_0x000c:
        if (r5 != 0) goto L_0x0019;
    L_0x000e:
        r0 = new java.lang.IllegalArgumentException;	 Catch:{ all -> 0x0016 }
        r2 = "Empty buffer before end of stream.";
        r0.<init>(r2);	 Catch:{ all -> 0x0016 }
        throw r0;	 Catch:{ all -> 0x0016 }
    L_0x0016:
        r0 = move-exception;
        monitor-exit(r1);	 Catch:{ all -> 0x0016 }
        throw r0;
    L_0x0019:
        r0 = r3.mEndOfStreamWritten;	 Catch:{ all -> 0x0016 }
        if (r0 == 0) goto L_0x0025;
    L_0x001d:
        r0 = new java.lang.IllegalArgumentException;	 Catch:{ all -> 0x0016 }
        r2 = "Write after writing end of stream.";
        r0.<init>(r2);	 Catch:{ all -> 0x0016 }
        throw r0;	 Catch:{ all -> 0x0016 }
    L_0x0025:
        r0 = r3.isDoneLocked();	 Catch:{ all -> 0x0016 }
        if (r0 == 0) goto L_0x002d;
    L_0x002b:
        monitor-exit(r1);	 Catch:{ all -> 0x0016 }
    L_0x002c:
        return;
    L_0x002d:
        r0 = r3.mPendingData;	 Catch:{ all -> 0x0016 }
        r0.add(r4);	 Catch:{ all -> 0x0016 }
        if (r5 == 0) goto L_0x0037;
    L_0x0034:
        r0 = 1;
        r3.mEndOfStreamWritten = r0;	 Catch:{ all -> 0x0016 }
    L_0x0037:
        monitor-exit(r1);	 Catch:{ all -> 0x0016 }
        goto L_0x002c;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.chromium.net.impl.CronetBidirectionalStream.write(java.nio.ByteBuffer, boolean):void");
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void flush() {
        /*
        r4 = this;
        r1 = r4.mNativeStreamLock;
        monitor-enter(r1);
        r0 = r4.isDoneLocked();	 Catch:{ all -> 0x0041 }
        if (r0 != 0) goto L_0x0015;
    L_0x0009:
        r0 = r4.mWriteState;	 Catch:{ all -> 0x0041 }
        r2 = org.chromium.net.impl.CronetBidirectionalStream.State.WAITING_FOR_FLUSH;	 Catch:{ all -> 0x0041 }
        if (r0 == r2) goto L_0x0017;
    L_0x000f:
        r0 = r4.mWriteState;	 Catch:{ all -> 0x0041 }
        r2 = org.chromium.net.impl.CronetBidirectionalStream.State.WRITING;	 Catch:{ all -> 0x0041 }
        if (r0 == r2) goto L_0x0017;
    L_0x0015:
        monitor-exit(r1);	 Catch:{ all -> 0x0041 }
    L_0x0016:
        return;
    L_0x0017:
        r0 = r4.mPendingData;	 Catch:{ all -> 0x0041 }
        r0 = r0.isEmpty();	 Catch:{ all -> 0x0041 }
        if (r0 == 0) goto L_0x0044;
    L_0x001f:
        r0 = r4.mFlushData;	 Catch:{ all -> 0x0041 }
        r0 = r0.isEmpty();	 Catch:{ all -> 0x0041 }
        if (r0 == 0) goto L_0x0044;
    L_0x0027:
        r0 = r4.mRequestHeadersSent;	 Catch:{ all -> 0x0041 }
        if (r0 != 0) goto L_0x003f;
    L_0x002b:
        r0 = 1;
        r4.mRequestHeadersSent = r0;	 Catch:{ all -> 0x0041 }
        r2 = r4.mNativeStream;	 Catch:{ all -> 0x0041 }
        r4.nativeSendRequestHeaders(r2);	 Catch:{ all -> 0x0041 }
        r0 = r4.mInitialMethod;	 Catch:{ all -> 0x0041 }
        r0 = doesMethodAllowWriteData(r0);	 Catch:{ all -> 0x0041 }
        if (r0 != 0) goto L_0x003f;
    L_0x003b:
        r0 = org.chromium.net.impl.CronetBidirectionalStream.State.WRITING_DONE;	 Catch:{ all -> 0x0041 }
        r4.mWriteState = r0;	 Catch:{ all -> 0x0041 }
    L_0x003f:
        monitor-exit(r1);	 Catch:{ all -> 0x0041 }
        goto L_0x0016;
    L_0x0041:
        r0 = move-exception;
        monitor-exit(r1);	 Catch:{ all -> 0x0041 }
        throw r0;
    L_0x0044:
        r0 = $assertionsDisabled;	 Catch:{ all -> 0x0041 }
        if (r0 != 0) goto L_0x005e;
    L_0x0048:
        r0 = r4.mPendingData;	 Catch:{ all -> 0x0041 }
        r0 = r0.isEmpty();	 Catch:{ all -> 0x0041 }
        if (r0 == 0) goto L_0x005e;
    L_0x0050:
        r0 = r4.mFlushData;	 Catch:{ all -> 0x0041 }
        r0 = r0.isEmpty();	 Catch:{ all -> 0x0041 }
        if (r0 == 0) goto L_0x005e;
    L_0x0058:
        r0 = new java.lang.AssertionError;	 Catch:{ all -> 0x0041 }
        r0.<init>();	 Catch:{ all -> 0x0041 }
        throw r0;	 Catch:{ all -> 0x0041 }
    L_0x005e:
        r0 = r4.mPendingData;	 Catch:{ all -> 0x0041 }
        r0 = r0.isEmpty();	 Catch:{ all -> 0x0041 }
        if (r0 != 0) goto L_0x0072;
    L_0x0066:
        r0 = r4.mFlushData;	 Catch:{ all -> 0x0041 }
        r2 = r4.mPendingData;	 Catch:{ all -> 0x0041 }
        r0.addAll(r2);	 Catch:{ all -> 0x0041 }
        r0 = r4.mPendingData;	 Catch:{ all -> 0x0041 }
        r0.clear();	 Catch:{ all -> 0x0041 }
    L_0x0072:
        r0 = r4.mWriteState;	 Catch:{ all -> 0x0041 }
        r2 = org.chromium.net.impl.CronetBidirectionalStream.State.WRITING;	 Catch:{ all -> 0x0041 }
        if (r0 != r2) goto L_0x007a;
    L_0x0078:
        monitor-exit(r1);	 Catch:{ all -> 0x0041 }
        goto L_0x0016;
    L_0x007a:
        r4.sendFlushDataLocked();	 Catch:{ all -> 0x0041 }
        monitor-exit(r1);	 Catch:{ all -> 0x0041 }
        goto L_0x0016;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.chromium.net.impl.CronetBidirectionalStream.flush():void");
    }

    private void sendFlushDataLocked() {
        boolean z = true;
        if ($assertionsDisabled || this.mWriteState == State.WAITING_FOR_FLUSH) {
            int size = this.mFlushData.size();
            ByteBuffer[] buffers = new ByteBuffer[size];
            int[] positions = new int[size];
            int[] limits = new int[size];
            for (int i = 0; i < size; i++) {
                ByteBuffer buffer = (ByteBuffer) this.mFlushData.poll();
                buffers[i] = buffer;
                positions[i] = buffer.position();
                limits[i] = buffer.limit();
            }
            if (!$assertionsDisabled && !this.mFlushData.isEmpty()) {
                throw new AssertionError();
            } else if ($assertionsDisabled || buffers.length >= 1) {
                this.mWriteState = State.WRITING;
                this.mRequestHeadersSent = true;
                long j = this.mNativeStream;
                if (!(this.mEndOfStreamWritten && this.mPendingData.isEmpty())) {
                    z = false;
                }
                if (!nativeWritevData(j, buffers, positions, limits, z)) {
                    this.mWriteState = State.WAITING_FOR_FLUSH;
                    throw new IllegalArgumentException("Unable to call native writev.");
                }
                return;
            } else {
                throw new AssertionError();
            }
        }
        throw new AssertionError();
    }

    @VisibleForTesting
    public List<ByteBuffer> getPendingDataForTesting() {
        List<ByteBuffer> pendingData;
        synchronized (this.mNativeStreamLock) {
            pendingData = new LinkedList();
            Iterator it = this.mPendingData.iterator();
            while (it.hasNext()) {
                pendingData.add(((ByteBuffer) it.next()).asReadOnlyBuffer());
            }
        }
        return pendingData;
    }

    @VisibleForTesting
    public List<ByteBuffer> getFlushDataForTesting() {
        List<ByteBuffer> flushData;
        synchronized (this.mNativeStreamLock) {
            flushData = new LinkedList();
            Iterator it = this.mFlushData.iterator();
            while (it.hasNext()) {
                flushData.add(((ByteBuffer) it.next()).asReadOnlyBuffer());
            }
        }
        return flushData;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void cancel() {
        /*
        r3 = this;
        r1 = r3.mNativeStreamLock;
        monitor-enter(r1);
        r0 = r3.isDoneLocked();	 Catch:{ all -> 0x001d }
        if (r0 != 0) goto L_0x000f;
    L_0x0009:
        r0 = r3.mReadState;	 Catch:{ all -> 0x001d }
        r2 = org.chromium.net.impl.CronetBidirectionalStream.State.NOT_STARTED;	 Catch:{ all -> 0x001d }
        if (r0 != r2) goto L_0x0011;
    L_0x000f:
        monitor-exit(r1);	 Catch:{ all -> 0x001d }
    L_0x0010:
        return;
    L_0x0011:
        r0 = org.chromium.net.impl.CronetBidirectionalStream.State.CANCELED;	 Catch:{ all -> 0x001d }
        r3.mWriteState = r0;	 Catch:{ all -> 0x001d }
        r3.mReadState = r0;	 Catch:{ all -> 0x001d }
        r0 = 1;
        r3.destroyNativeStreamLocked(r0);	 Catch:{ all -> 0x001d }
        monitor-exit(r1);	 Catch:{ all -> 0x001d }
        goto L_0x0010;
    L_0x001d:
        r0 = move-exception;
        monitor-exit(r1);	 Catch:{ all -> 0x001d }
        throw r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.chromium.net.impl.CronetBidirectionalStream.cancel():void");
    }

    public boolean isDone() {
        boolean isDoneLocked;
        synchronized (this.mNativeStreamLock) {
            isDoneLocked = isDoneLocked();
        }
        return isDoneLocked;
    }

    @GuardedBy("mNativeStreamLock")
    private boolean isDoneLocked() {
        return this.mReadState != State.NOT_STARTED && this.mNativeStream == 0;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void maybeOnSucceededOnExecutor() {
        /*
        r5 = this;
        r4 = 0;
        r2 = r5.mNativeStreamLock;
        monitor-enter(r2);
        r1 = r5.isDoneLocked();	 Catch:{ all -> 0x001a }
        if (r1 == 0) goto L_0x000c;
    L_0x000a:
        monitor-exit(r2);	 Catch:{ all -> 0x001a }
    L_0x000b:
        return;
    L_0x000c:
        r1 = r5.mWriteState;	 Catch:{ all -> 0x001a }
        r3 = org.chromium.net.impl.CronetBidirectionalStream.State.WRITING_DONE;	 Catch:{ all -> 0x001a }
        if (r1 != r3) goto L_0x0018;
    L_0x0012:
        r1 = r5.mReadState;	 Catch:{ all -> 0x001a }
        r3 = org.chromium.net.impl.CronetBidirectionalStream.State.READING_DONE;	 Catch:{ all -> 0x001a }
        if (r1 == r3) goto L_0x001d;
    L_0x0018:
        monitor-exit(r2);	 Catch:{ all -> 0x001a }
        goto L_0x000b;
    L_0x001a:
        r1 = move-exception;
        monitor-exit(r2);	 Catch:{ all -> 0x001a }
        throw r1;
    L_0x001d:
        r1 = org.chromium.net.impl.CronetBidirectionalStream.State.SUCCESS;	 Catch:{ all -> 0x001a }
        r5.mWriteState = r1;	 Catch:{ all -> 0x001a }
        r5.mReadState = r1;	 Catch:{ all -> 0x001a }
        r1 = 0;
        r5.destroyNativeStreamLocked(r1);	 Catch:{ all -> 0x001a }
        monitor-exit(r2);	 Catch:{ all -> 0x001a }
        r1 = r5.mCallback;	 Catch:{ Exception -> 0x0030 }
        r2 = r5.mResponseInfo;	 Catch:{ Exception -> 0x0030 }
        r1.onSucceeded(r5, r2);	 Catch:{ Exception -> 0x0030 }
        goto L_0x000b;
    L_0x0030:
        r0 = move-exception;
        r1 = org.chromium.net.impl.CronetUrlRequestContext.LOG_TAG;
        r2 = "Exception in onSucceeded method";
        r3 = 1;
        r3 = new java.lang.Object[r3];
        r3[r4] = r0;
        org.chromium.base.Log.e(r1, r2, r3);
        goto L_0x000b;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.chromium.net.impl.CronetBidirectionalStream.maybeOnSucceededOnExecutor():void");
    }

    @CalledByNative
    private void onStreamReady(final boolean requestHeadersSent) {
        postTaskToExecutor(new Runnable() {
            /* JADX WARNING: inconsistent code. */
            /* Code decompiled incorrectly, please refer to instructions dump. */
            public void run() {
                /*
                r4 = this;
                r1 = org.chromium.net.impl.CronetBidirectionalStream.this;
                r2 = r1.mNativeStreamLock;
                monitor-enter(r2);
                r1 = org.chromium.net.impl.CronetBidirectionalStream.this;	 Catch:{ all -> 0x0056 }
                r1 = r1.isDoneLocked();	 Catch:{ all -> 0x0056 }
                if (r1 == 0) goto L_0x0011;
            L_0x000f:
                monitor-exit(r2);	 Catch:{ all -> 0x0056 }
            L_0x0010:
                return;
            L_0x0011:
                r1 = org.chromium.net.impl.CronetBidirectionalStream.this;	 Catch:{ all -> 0x0056 }
                r3 = r2;	 Catch:{ all -> 0x0056 }
                r1.mRequestHeadersSent = r3;	 Catch:{ all -> 0x0056 }
                r1 = org.chromium.net.impl.CronetBidirectionalStream.this;	 Catch:{ all -> 0x0056 }
                r3 = org.chromium.net.impl.CronetBidirectionalStream.State.WAITING_FOR_READ;	 Catch:{ all -> 0x0056 }
                r1.mReadState = r3;	 Catch:{ all -> 0x0056 }
                r1 = org.chromium.net.impl.CronetBidirectionalStream.this;	 Catch:{ all -> 0x0056 }
                r1 = r1.mInitialMethod;	 Catch:{ all -> 0x0056 }
                r1 = org.chromium.net.impl.CronetBidirectionalStream.doesMethodAllowWriteData(r1);	 Catch:{ all -> 0x0056 }
                if (r1 != 0) goto L_0x004e;
            L_0x002b:
                r1 = org.chromium.net.impl.CronetBidirectionalStream.this;	 Catch:{ all -> 0x0056 }
                r1 = r1.mRequestHeadersSent;	 Catch:{ all -> 0x0056 }
                if (r1 == 0) goto L_0x004e;
            L_0x0033:
                r1 = org.chromium.net.impl.CronetBidirectionalStream.this;	 Catch:{ all -> 0x0056 }
                r3 = org.chromium.net.impl.CronetBidirectionalStream.State.WRITING_DONE;	 Catch:{ all -> 0x0056 }
                r1.mWriteState = r3;	 Catch:{ all -> 0x0056 }
            L_0x003a:
                monitor-exit(r2);	 Catch:{ all -> 0x0056 }
                r1 = org.chromium.net.impl.CronetBidirectionalStream.this;	 Catch:{ Exception -> 0x0047 }
                r1 = r1.mCallback;	 Catch:{ Exception -> 0x0047 }
                r2 = org.chromium.net.impl.CronetBidirectionalStream.this;	 Catch:{ Exception -> 0x0047 }
                r1.onStreamReady(r2);	 Catch:{ Exception -> 0x0047 }
                goto L_0x0010;
            L_0x0047:
                r0 = move-exception;
                r1 = org.chromium.net.impl.CronetBidirectionalStream.this;
                r1.onCallbackException(r0);
                goto L_0x0010;
            L_0x004e:
                r1 = org.chromium.net.impl.CronetBidirectionalStream.this;	 Catch:{ all -> 0x0056 }
                r3 = org.chromium.net.impl.CronetBidirectionalStream.State.WAITING_FOR_FLUSH;	 Catch:{ all -> 0x0056 }
                r1.mWriteState = r3;	 Catch:{ all -> 0x0056 }
                goto L_0x003a;
            L_0x0056:
                r1 = move-exception;
                monitor-exit(r2);	 Catch:{ all -> 0x0056 }
                throw r1;
                */
                throw new UnsupportedOperationException("Method not decompiled: org.chromium.net.impl.CronetBidirectionalStream.1.run():void");
            }
        });
    }

    @CalledByNative
    private void onResponseHeadersReceived(int httpStatusCode, String negotiatedProtocol, String[] headers, long receivedByteCount) {
        try {
            this.mResponseInfo = prepareResponseInfoOnNetworkThread(httpStatusCode, negotiatedProtocol, headers, receivedByteCount);
            postTaskToExecutor(new Runnable() {
                public void run() {
                    synchronized (CronetBidirectionalStream.this.mNativeStreamLock) {
                        if (CronetBidirectionalStream.this.isDoneLocked()) {
                            return;
                        }
                        CronetBidirectionalStream.this.mReadState = State.WAITING_FOR_READ;
                        try {
                            CronetBidirectionalStream.this.mCallback.onResponseHeadersReceived(CronetBidirectionalStream.this, CronetBidirectionalStream.this.mResponseInfo);
                        } catch (Exception e) {
                            CronetBidirectionalStream.this.onCallbackException(e);
                        }
                    }
                }
            });
        } catch (Exception e) {
            failWithException(new CronetExceptionImpl("Cannot prepare ResponseInfo", null));
        }
    }

    @CalledByNative
    private void onReadCompleted(ByteBuffer byteBuffer, int bytesRead, int initialPosition, int initialLimit, long receivedByteCount) {
        this.mResponseInfo.setReceivedByteCount(receivedByteCount);
        if (byteBuffer.position() != initialPosition || byteBuffer.limit() != initialLimit) {
            failWithException(new CronetExceptionImpl("ByteBuffer modified externally during read", null));
        } else if (bytesRead < 0 || initialPosition + bytesRead > initialLimit) {
            failWithException(new CronetExceptionImpl("Invalid number of bytes read", null));
        } else {
            byteBuffer.position(initialPosition + bytesRead);
            if ($assertionsDisabled || this.mOnReadCompletedTask.mByteBuffer == null) {
                this.mOnReadCompletedTask.mByteBuffer = byteBuffer;
                this.mOnReadCompletedTask.mEndOfStream = bytesRead == 0;
                postTaskToExecutor(this.mOnReadCompletedTask);
                return;
            }
            throw new AssertionError();
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    @org.chromium.base.annotations.CalledByNative
    private void onWritevCompleted(java.nio.ByteBuffer[] r6, int[] r7, int[] r8, boolean r9) {
        /*
        r5 = this;
        r2 = $assertionsDisabled;
        if (r2 != 0) goto L_0x000e;
    L_0x0004:
        r2 = r6.length;
        r3 = r7.length;
        if (r2 == r3) goto L_0x000e;
    L_0x0008:
        r2 = new java.lang.AssertionError;
        r2.<init>();
        throw r2;
    L_0x000e:
        r2 = $assertionsDisabled;
        if (r2 != 0) goto L_0x001c;
    L_0x0012:
        r2 = r6.length;
        r3 = r8.length;
        if (r2 == r3) goto L_0x001c;
    L_0x0016:
        r2 = new java.lang.AssertionError;
        r2.<init>();
        throw r2;
    L_0x001c:
        r3 = r5.mNativeStreamLock;
        monitor-enter(r3);
        r2 = r5.isDoneLocked();	 Catch:{ all -> 0x0059 }
        if (r2 == 0) goto L_0x0027;
    L_0x0025:
        monitor-exit(r3);	 Catch:{ all -> 0x0059 }
    L_0x0026:
        return;
    L_0x0027:
        r2 = org.chromium.net.impl.CronetBidirectionalStream.State.WAITING_FOR_FLUSH;	 Catch:{ all -> 0x0059 }
        r5.mWriteState = r2;	 Catch:{ all -> 0x0059 }
        r2 = r5.mFlushData;	 Catch:{ all -> 0x0059 }
        r2 = r2.isEmpty();	 Catch:{ all -> 0x0059 }
        if (r2 != 0) goto L_0x0036;
    L_0x0033:
        r5.sendFlushDataLocked();	 Catch:{ all -> 0x0059 }
    L_0x0036:
        monitor-exit(r3);	 Catch:{ all -> 0x0059 }
        r1 = 0;
    L_0x0038:
        r2 = r6.length;
        if (r1 >= r2) goto L_0x0026;
    L_0x003b:
        r0 = r6[r1];
        r2 = r0.position();
        r3 = r7[r1];
        if (r2 != r3) goto L_0x004d;
    L_0x0045:
        r2 = r0.limit();
        r3 = r8[r1];
        if (r2 == r3) goto L_0x005c;
    L_0x004d:
        r2 = new org.chromium.net.impl.CronetExceptionImpl;
        r3 = "ByteBuffer modified externally during write";
        r4 = 0;
        r2.<init>(r3, r4);
        r5.failWithException(r2);
        goto L_0x0026;
    L_0x0059:
        r2 = move-exception;
        monitor-exit(r3);	 Catch:{ all -> 0x0059 }
        throw r2;
    L_0x005c:
        r2 = r0.limit();
        r0.position(r2);
        r3 = new org.chromium.net.impl.CronetBidirectionalStream$OnWriteCompletedRunnable;
        if (r9 == 0) goto L_0x0076;
    L_0x0067:
        r2 = r6.length;
        r2 = r2 + -1;
        if (r1 != r2) goto L_0x0076;
    L_0x006c:
        r2 = 1;
    L_0x006d:
        r3.<init>(r0, r2);
        r5.postTaskToExecutor(r3);
        r1 = r1 + 1;
        goto L_0x0038;
    L_0x0076:
        r2 = 0;
        goto L_0x006d;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.chromium.net.impl.CronetBidirectionalStream.onWritevCompleted(java.nio.ByteBuffer[], int[], int[], boolean):void");
    }

    @CalledByNative
    private void onResponseTrailersReceived(String[] trailers) {
        final HeaderBlock trailersBlock = new HeaderBlockImpl(headersListFromStrings(trailers));
        postTaskToExecutor(new Runnable() {
            public void run() {
                synchronized (CronetBidirectionalStream.this.mNativeStreamLock) {
                    if (CronetBidirectionalStream.this.isDoneLocked()) {
                        return;
                    }
                    try {
                        CronetBidirectionalStream.this.mCallback.onResponseTrailersReceived(CronetBidirectionalStream.this, CronetBidirectionalStream.this.mResponseInfo, trailersBlock);
                    } catch (Exception e) {
                        CronetBidirectionalStream.this.onCallbackException(e);
                    }
                }
            }
        });
    }

    @CalledByNative
    private void onError(int errorCode, int nativeError, int nativeQuicError, String errorString, long receivedByteCount) {
        if (this.mResponseInfo != null) {
            this.mResponseInfo.setReceivedByteCount(receivedByteCount);
        }
        if (errorCode == 10) {
            failWithException(new QuicExceptionImpl("Exception in BidirectionalStream: " + errorString, nativeError, nativeQuicError));
        } else {
            failWithException(new BidirectionalStreamNetworkException("Exception in BidirectionalStream: " + errorString, errorCode, nativeError));
        }
    }

    @CalledByNative
    private void onCanceled() {
        postTaskToExecutor(new Runnable() {
            public void run() {
                try {
                    CronetBidirectionalStream.this.mCallback.onCanceled(CronetBidirectionalStream.this, CronetBidirectionalStream.this.mResponseInfo);
                } catch (Exception e) {
                    Log.e(CronetUrlRequestContext.LOG_TAG, "Exception in onCanceled method", e);
                }
            }
        });
    }

    @CalledByNative
    private void onMetricsCollected(long requestStartMs, long dnsStartMs, long dnsEndMs, long connectStartMs, long connectEndMs, long sslStartMs, long sslEndMs, long sendingStartMs, long sendingEndMs, long pushStartMs, long pushEndMs, long responseStartMs, long requestEndMs, boolean socketReused, long sentByteCount, long receivedByteCount) {
        synchronized (this.mNativeStreamLock) {
            if (this.mMetrics != null) {
                throw new IllegalStateException("Metrics collection should only happen once.");
            }
            this.mMetrics = new CronetMetrics(requestStartMs, dnsStartMs, dnsEndMs, connectStartMs, connectEndMs, sslStartMs, sslEndMs, sendingStartMs, sendingEndMs, pushStartMs, pushEndMs, responseStartMs, requestEndMs, socketReused, sentByteCount, receivedByteCount);
            if (!$assertionsDisabled && this.mReadState != this.mWriteState) {
                throw new AssertionError();
            } else if ($assertionsDisabled || this.mReadState == State.SUCCESS || this.mReadState == State.ERROR || this.mReadState == State.CANCELED) {
                int finishedReason;
                if (this.mReadState == State.SUCCESS) {
                    finishedReason = 0;
                } else if (this.mReadState == State.CANCELED) {
                    finishedReason = 2;
                } else {
                    finishedReason = 1;
                }
                this.mRequestContext.reportFinished(new RequestFinishedInfoImpl(this.mInitialUrl, this.mRequestAnnotations, this.mMetrics, finishedReason, this.mResponseInfo, this.mException));
            } else {
                throw new AssertionError();
            }
        }
    }

    @VisibleForTesting
    public void setOnDestroyedCallbackForTesting(Runnable onDestroyedCallbackForTesting) {
        this.mOnDestroyedCallbackForTesting = onDestroyedCallbackForTesting;
    }

    private static boolean doesMethodAllowWriteData(String methodName) {
        return (methodName.equals("GET") || methodName.equals("HEAD")) ? false : true;
    }

    private static ArrayList<Entry<String, String>> headersListFromStrings(String[] headers) {
        ArrayList<Entry<String, String>> headersList = new ArrayList(headers.length / 2);
        for (int i = 0; i < headers.length; i += 2) {
            headersList.add(new SimpleImmutableEntry(headers[i], headers[i + 1]));
        }
        return headersList;
    }

    private static String[] stringsFromHeaderList(List<Entry<String, String>> headersList) {
        String[] headersArray = new String[(headersList.size() * 2)];
        int i = 0;
        for (Entry<String, String> requestHeader : headersList) {
            int i2 = i + 1;
            headersArray[i] = (String) requestHeader.getKey();
            i = i2 + 1;
            headersArray[i2] = (String) requestHeader.getValue();
        }
        return headersArray;
    }

    private static int convertStreamPriority(int priority) {
        switch (priority) {
            case 0:
                return 1;
            case 1:
                return 2;
            case 2:
                return 3;
            case 3:
                return 4;
            case 4:
                return 5;
            default:
                throw new IllegalArgumentException("Invalid stream priority.");
        }
    }

    private void postTaskToExecutor(Runnable task) {
        try {
            this.mExecutor.execute(task);
        } catch (RejectedExecutionException failException) {
            Log.e(CronetUrlRequestContext.LOG_TAG, "Exception posting task to executor", failException);
            synchronized (this.mNativeStreamLock) {
                State state = State.ERROR;
                this.mWriteState = state;
                this.mReadState = state;
                destroyNativeStreamLocked(false);
            }
        }
    }

    private UrlResponseInfoImpl prepareResponseInfoOnNetworkThread(int httpStatusCode, String negotiatedProtocol, String[] headers, long receivedByteCount) {
        int i = httpStatusCode;
        UrlResponseInfoImpl responseInfo = new UrlResponseInfoImpl(Arrays.asList(new String[]{this.mInitialUrl}), i, "", headersListFromStrings(headers), false, negotiatedProtocol, null);
        responseInfo.setReceivedByteCount(receivedByteCount);
        return responseInfo;
    }

    @GuardedBy("mNativeStreamLock")
    private void destroyNativeStreamLocked(boolean sendOnCanceled) {
        Log.i(CronetUrlRequestContext.LOG_TAG, "destroyNativeStreamLocked " + toString(), new Object[0]);
        if (this.mNativeStream != 0) {
            nativeDestroy(this.mNativeStream, sendOnCanceled);
            this.mRequestContext.onRequestDestroyed();
            this.mNativeStream = 0;
            if (this.mOnDestroyedCallbackForTesting != null) {
                this.mOnDestroyedCallbackForTesting.run();
            }
        }
    }

    private void failWithExceptionOnExecutor(CronetException e) {
        this.mException = e;
        synchronized (this.mNativeStreamLock) {
            if (isDoneLocked()) {
                return;
            }
            State state = State.ERROR;
            this.mWriteState = state;
            this.mReadState = state;
            destroyNativeStreamLocked(false);
            try {
                this.mCallback.onFailed(this, this.mResponseInfo, e);
            } catch (Exception failException) {
                Log.e(CronetUrlRequestContext.LOG_TAG, "Exception notifying of failed request", failException);
            }
        }
    }

    private void onCallbackException(Exception e) {
        CallbackException streamError = new CallbackExceptionImpl("CalledByNative method has thrown an exception", e);
        Log.e(CronetUrlRequestContext.LOG_TAG, "Exception in CalledByNative method", e);
        failWithExceptionOnExecutor(streamError);
    }

    private void failWithException(final CronetException exception) {
        postTaskToExecutor(new Runnable() {
            public void run() {
                CronetBidirectionalStream.this.failWithExceptionOnExecutor(exception);
            }
        });
    }
}
