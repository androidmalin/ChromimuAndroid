package org.chromium.net.impl;

import android.annotation.SuppressLint;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.Executor;
import javax.annotation.concurrent.GuardedBy;
import org.chromium.base.Log;
import org.chromium.base.VisibleForTesting;
import org.chromium.base.annotations.CalledByNative;
import org.chromium.base.annotations.JNINamespace;
import org.chromium.base.annotations.NativeClassQualifiedName;
import org.chromium.net.UploadDataProvider;
import org.chromium.net.UploadDataSink;
import org.chromium.net.impl.VersionSafeCallbacks.UploadDataProviderWrapper;

@VisibleForTesting
@JNINamespace("cronet")
public final class CronetUploadDataStream extends UploadDataSink {
    private static final String TAG = CronetUploadDataStream.class.getSimpleName();
    private ByteBuffer mByteBuffer = null;
    private long mByteBufferLimit;
    private final UploadDataProviderWrapper mDataProvider;
    @GuardedBy("mLock")
    private boolean mDestroyAdapterPostponed = false;
    private final Executor mExecutor;
    @GuardedBy("mLock")
    private UserCallback mInWhichUserCallback = UserCallback.NOT_IN_CALLBACK;
    private long mLength;
    private final Object mLock = new Object();
    private Runnable mOnDestroyedCallbackForTesting;
    private final Runnable mReadTask = new Runnable() {
        static final /* synthetic */ boolean $assertionsDisabled = (!CronetUploadDataStream.class.desiredAssertionStatus());

        /* JADX WARNING: inconsistent code. */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void run() {
            /*
            r8 = this;
            r1 = org.chromium.net.impl.CronetUploadDataStream.this;
            r2 = r1.mLock;
            monitor-enter(r2);
            r1 = org.chromium.net.impl.CronetUploadDataStream.this;	 Catch:{ all -> 0x002c }
            r4 = r1.mUploadDataStreamAdapter;	 Catch:{ all -> 0x002c }
            r6 = 0;
            r1 = (r4 > r6 ? 1 : (r4 == r6 ? 0 : -1));
            if (r1 != 0) goto L_0x0015;
        L_0x0013:
            monitor-exit(r2);	 Catch:{ all -> 0x002c }
        L_0x0014:
            return;
        L_0x0015:
            r1 = org.chromium.net.impl.CronetUploadDataStream.this;	 Catch:{ all -> 0x002c }
            r3 = org.chromium.net.impl.CronetUploadDataStream.UserCallback.NOT_IN_CALLBACK;	 Catch:{ all -> 0x002c }
            r1.checkState(r3);	 Catch:{ all -> 0x002c }
            r1 = org.chromium.net.impl.CronetUploadDataStream.this;	 Catch:{ all -> 0x002c }
            r1 = r1.mByteBuffer;	 Catch:{ all -> 0x002c }
            if (r1 != 0) goto L_0x002f;
        L_0x0024:
            r1 = new java.lang.IllegalStateException;	 Catch:{ all -> 0x002c }
            r3 = "Unexpected readData call. Buffer is null";
            r1.<init>(r3);	 Catch:{ all -> 0x002c }
            throw r1;	 Catch:{ all -> 0x002c }
        L_0x002c:
            r1 = move-exception;
            monitor-exit(r2);	 Catch:{ all -> 0x002c }
            throw r1;
        L_0x002f:
            r1 = org.chromium.net.impl.CronetUploadDataStream.this;	 Catch:{ all -> 0x002c }
            r3 = org.chromium.net.impl.CronetUploadDataStream.UserCallback.READ;	 Catch:{ all -> 0x002c }
            r1.mInWhichUserCallback = r3;	 Catch:{ all -> 0x002c }
            monitor-exit(r2);	 Catch:{ all -> 0x002c }
            r1 = org.chromium.net.impl.CronetUploadDataStream.this;	 Catch:{ Exception -> 0x0052 }
            r1.checkCallingThread();	 Catch:{ Exception -> 0x0052 }
            r1 = $assertionsDisabled;	 Catch:{ Exception -> 0x0052 }
            if (r1 != 0) goto L_0x0059;
        L_0x0040:
            r1 = org.chromium.net.impl.CronetUploadDataStream.this;	 Catch:{ Exception -> 0x0052 }
            r1 = r1.mByteBuffer;	 Catch:{ Exception -> 0x0052 }
            r1 = r1.position();	 Catch:{ Exception -> 0x0052 }
            if (r1 == 0) goto L_0x0059;
        L_0x004c:
            r1 = new java.lang.AssertionError;	 Catch:{ Exception -> 0x0052 }
            r1.<init>();	 Catch:{ Exception -> 0x0052 }
            throw r1;	 Catch:{ Exception -> 0x0052 }
        L_0x0052:
            r0 = move-exception;
            r1 = org.chromium.net.impl.CronetUploadDataStream.this;
            r1.onError(r0);
            goto L_0x0014;
        L_0x0059:
            r1 = org.chromium.net.impl.CronetUploadDataStream.this;	 Catch:{ Exception -> 0x0052 }
            r1 = r1.mDataProvider;	 Catch:{ Exception -> 0x0052 }
            r2 = org.chromium.net.impl.CronetUploadDataStream.this;	 Catch:{ Exception -> 0x0052 }
            r3 = org.chromium.net.impl.CronetUploadDataStream.this;	 Catch:{ Exception -> 0x0052 }
            r3 = r3.mByteBuffer;	 Catch:{ Exception -> 0x0052 }
            r1.read(r2, r3);	 Catch:{ Exception -> 0x0052 }
            goto L_0x0014;
            */
            throw new UnsupportedOperationException("Method not decompiled: org.chromium.net.impl.CronetUploadDataStream.1.run():void");
        }
    };
    private long mRemainingLength;
    private final CronetUrlRequest mRequest;
    @GuardedBy("mLock")
    private long mUploadDataStreamAdapter = 0;

    enum UserCallback {
        READ,
        REWIND,
        GET_LENGTH,
        NOT_IN_CALLBACK
    }

    private native long nativeAttachUploadDataToRequest(long j, long j2);

    private native long nativeCreateAdapterForTesting();

    private native long nativeCreateUploadDataStreamForTesting(long j, long j2);

    @NativeClassQualifiedName("CronetUploadDataStreamAdapter")
    private static native void nativeDestroy(long j);

    @NativeClassQualifiedName("CronetUploadDataStreamAdapter")
    private native void nativeOnReadSucceeded(long j, int i, boolean z);

    @NativeClassQualifiedName("CronetUploadDataStreamAdapter")
    private native void nativeOnRewindSucceeded(long j);

    public CronetUploadDataStream(UploadDataProvider dataProvider, Executor executor, CronetUrlRequest request) {
        this.mExecutor = executor;
        this.mDataProvider = new UploadDataProviderWrapper(dataProvider);
        this.mRequest = request;
    }

    @CalledByNative
    void readData(ByteBuffer byteBuffer) {
        this.mByteBuffer = byteBuffer;
        this.mByteBufferLimit = (long) byteBuffer.limit();
        postTaskToExecutor(this.mReadTask);
    }

    @CalledByNative
    void rewind() {
        postTaskToExecutor(new Runnable() {
            public void run() {
                synchronized (CronetUploadDataStream.this.mLock) {
                    if (CronetUploadDataStream.this.mUploadDataStreamAdapter == 0) {
                        return;
                    }
                    CronetUploadDataStream.this.checkState(UserCallback.NOT_IN_CALLBACK);
                    CronetUploadDataStream.this.mInWhichUserCallback = UserCallback.REWIND;
                    try {
                        CronetUploadDataStream.this.checkCallingThread();
                        CronetUploadDataStream.this.mDataProvider.rewind(CronetUploadDataStream.this);
                    } catch (Exception exception) {
                        CronetUploadDataStream.this.onError(exception);
                    }
                }
            }
        });
    }

    private void checkCallingThread() {
        this.mRequest.checkCallingThread();
    }

    @GuardedBy("mLock")
    private void checkState(UserCallback mode) {
        if (this.mInWhichUserCallback != mode) {
            throw new IllegalStateException("Expected " + mode + ", but was " + this.mInWhichUserCallback);
        }
    }

    @CalledByNative
    void onUploadDataStreamDestroyed() {
        destroyAdapter();
    }

    private void onError(Throwable exception) {
        boolean sendClose;
        synchronized (this.mLock) {
            if (this.mInWhichUserCallback == UserCallback.NOT_IN_CALLBACK) {
                throw new IllegalStateException("There is no read or rewind or length check in progress.");
            }
            if (this.mInWhichUserCallback == UserCallback.GET_LENGTH) {
                sendClose = true;
            } else {
                sendClose = false;
            }
            this.mInWhichUserCallback = UserCallback.NOT_IN_CALLBACK;
            this.mByteBuffer = null;
            destroyAdapterIfPostponed();
        }
        if (sendClose) {
            try {
                this.mDataProvider.close();
            } catch (Exception e) {
                Log.e(TAG, "Failure closing data provider", e);
            }
        }
        this.mRequest.onUploadException(exception);
    }

    @SuppressLint({"DefaultLocale"})
    public void onReadSucceeded(boolean lastChunk) {
        synchronized (this.mLock) {
            checkState(UserCallback.READ);
            if (this.mByteBufferLimit != ((long) this.mByteBuffer.limit())) {
                throw new IllegalStateException("ByteBuffer limit changed");
            }
            if (lastChunk) {
                if (this.mLength >= 0) {
                    throw new IllegalArgumentException("Non-chunked upload can't have last chunk");
                }
            }
            int bytesRead = this.mByteBuffer.position();
            this.mRemainingLength -= (long) bytesRead;
            if (this.mRemainingLength >= 0 || this.mLength < 0) {
                this.mByteBuffer.position(0);
                this.mByteBuffer = null;
                this.mInWhichUserCallback = UserCallback.NOT_IN_CALLBACK;
                destroyAdapterIfPostponed();
                if (this.mUploadDataStreamAdapter == 0) {
                    return;
                }
                nativeOnReadSucceeded(this.mUploadDataStreamAdapter, bytesRead, lastChunk);
                return;
            }
            throw new IllegalArgumentException(String.format("Read upload data length %d exceeds expected length %d", new Object[]{Long.valueOf(this.mLength - this.mRemainingLength), Long.valueOf(this.mLength)}));
        }
    }

    public void onReadError(Exception exception) {
        synchronized (this.mLock) {
            checkState(UserCallback.READ);
            onError(exception);
        }
    }

    public void onRewindSucceeded() {
        synchronized (this.mLock) {
            checkState(UserCallback.REWIND);
            this.mInWhichUserCallback = UserCallback.NOT_IN_CALLBACK;
            this.mRemainingLength = this.mLength;
            if (this.mUploadDataStreamAdapter == 0) {
                return;
            }
            nativeOnRewindSucceeded(this.mUploadDataStreamAdapter);
        }
    }

    public void onRewindError(Exception exception) {
        synchronized (this.mLock) {
            checkState(UserCallback.REWIND);
            onError(exception);
        }
    }

    void postTaskToExecutor(Runnable task) {
        try {
            this.mExecutor.execute(task);
        } catch (Throwable e) {
            this.mRequest.onUploadException(e);
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void destroyAdapter() {
        /*
        r6 = this;
        r4 = 0;
        r1 = r6.mLock;
        monitor-enter(r1);
        r0 = r6.mInWhichUserCallback;	 Catch:{ all -> 0x0018 }
        r2 = org.chromium.net.impl.CronetUploadDataStream.UserCallback.READ;	 Catch:{ all -> 0x0018 }
        if (r0 != r2) goto L_0x0010;
    L_0x000b:
        r0 = 1;
        r6.mDestroyAdapterPostponed = r0;	 Catch:{ all -> 0x0018 }
        monitor-exit(r1);	 Catch:{ all -> 0x0018 }
    L_0x000f:
        return;
    L_0x0010:
        r2 = r6.mUploadDataStreamAdapter;	 Catch:{ all -> 0x0018 }
        r0 = (r2 > r4 ? 1 : (r2 == r4 ? 0 : -1));
        if (r0 != 0) goto L_0x001b;
    L_0x0016:
        monitor-exit(r1);	 Catch:{ all -> 0x0018 }
        goto L_0x000f;
    L_0x0018:
        r0 = move-exception;
        monitor-exit(r1);	 Catch:{ all -> 0x0018 }
        throw r0;
    L_0x001b:
        r2 = r6.mUploadDataStreamAdapter;	 Catch:{ all -> 0x0018 }
        nativeDestroy(r2);	 Catch:{ all -> 0x0018 }
        r2 = 0;
        r6.mUploadDataStreamAdapter = r2;	 Catch:{ all -> 0x0018 }
        r0 = r6.mOnDestroyedCallbackForTesting;	 Catch:{ all -> 0x0018 }
        if (r0 == 0) goto L_0x002d;
    L_0x0028:
        r0 = r6.mOnDestroyedCallbackForTesting;	 Catch:{ all -> 0x0018 }
        r0.run();	 Catch:{ all -> 0x0018 }
    L_0x002d:
        monitor-exit(r1);	 Catch:{ all -> 0x0018 }
        r0 = new org.chromium.net.impl.CronetUploadDataStream$3;
        r0.<init>();
        r6.postTaskToExecutor(r0);
        goto L_0x000f;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.chromium.net.impl.CronetUploadDataStream.destroyAdapter():void");
    }

    private void destroyAdapterIfPostponed() {
        synchronized (this.mLock) {
            if (this.mInWhichUserCallback == UserCallback.READ) {
                throw new IllegalStateException("Method should not be called when read has not completed.");
            }
            if (this.mDestroyAdapterPostponed) {
                destroyAdapter();
            }
        }
    }

    void initializeWithRequest() {
        synchronized (this.mLock) {
            this.mInWhichUserCallback = UserCallback.GET_LENGTH;
        }
        try {
            this.mRequest.checkCallingThread();
            this.mLength = this.mDataProvider.getLength();
            this.mRemainingLength = this.mLength;
        } catch (Throwable t) {
            onError(t);
        }
        synchronized (this.mLock) {
            this.mInWhichUserCallback = UserCallback.NOT_IN_CALLBACK;
        }
    }

    void attachNativeAdapterToRequest(long requestAdapter) {
        synchronized (this.mLock) {
            this.mUploadDataStreamAdapter = nativeAttachUploadDataToRequest(requestAdapter, this.mLength);
        }
    }

    @VisibleForTesting
    public long createUploadDataStreamForTesting() throws IOException {
        long nativeCreateUploadDataStreamForTesting;
        synchronized (this.mLock) {
            this.mUploadDataStreamAdapter = nativeCreateAdapterForTesting();
            this.mLength = this.mDataProvider.getLength();
            this.mRemainingLength = this.mLength;
            nativeCreateUploadDataStreamForTesting = nativeCreateUploadDataStreamForTesting(this.mLength, this.mUploadDataStreamAdapter);
        }
        return nativeCreateUploadDataStreamForTesting;
    }

    @VisibleForTesting
    void setOnDestroyedCallbackForTesting(Runnable onDestroyedCallbackForTesting) {
        this.mOnDestroyedCallbackForTesting = onDestroyedCallbackForTesting;
    }
}
