package org.chromium.net.impl;

import android.os.ConditionVariable;
import android.os.Process;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandlerFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.concurrent.GuardedBy;
import org.chromium.base.Log;
import org.chromium.base.ObserverList;
import org.chromium.base.VisibleForTesting;
import org.chromium.base.annotations.CalledByNative;
import org.chromium.base.annotations.JNINamespace;
import org.chromium.base.annotations.NativeClassQualifiedName;
import org.chromium.base.annotations.UsedByReflection;
import org.chromium.net.BidirectionalStream.Callback;
import org.chromium.net.ExperimentalBidirectionalStream;
import org.chromium.net.ExperimentalBidirectionalStream.Builder;
import org.chromium.net.NetworkQualityRttListener;
import org.chromium.net.NetworkQualityThroughputListener;
import org.chromium.net.RequestFinishedInfo;
import org.chromium.net.RequestFinishedInfo.Listener;
import org.chromium.net.UrlRequest;
import org.chromium.net.impl.CronetEngineBuilderImpl.Pkp;
import org.chromium.net.impl.CronetEngineBuilderImpl.QuicHint;
import org.chromium.net.impl.VersionSafeCallbacks.NetworkQualityRttListenerWrapper;
import org.chromium.net.impl.VersionSafeCallbacks.NetworkQualityThroughputListenerWrapper;
import org.chromium.net.impl.VersionSafeCallbacks.RequestFinishedInfoListener;
import org.chromium.net.urlconnection.CronetHttpURLConnection;
import org.chromium.net.urlconnection.CronetURLStreamHandlerFactory;

@UsedByReflection("CronetEngine.java")
@VisibleForTesting
@JNINamespace("cronet")
public class CronetUrlRequestContext extends CronetEngineBase {
    private static final int LOG_DEBUG = -1;
    private static final int LOG_NONE = 3;
    static final String LOG_TAG = CronetUrlRequestContext.class.getSimpleName();
    private static final int LOG_VERBOSE = -2;
    @GuardedBy("sInUseStoragePaths")
    private static final HashSet<String> sInUseStoragePaths = new HashSet();
    private final AtomicInteger mActiveRequestCount = new AtomicInteger(0);
    private String mCertVerifierData;
    @GuardedBy("mNetworkQualityLock")
    private int mDownstreamThroughputKbps = -1;
    @GuardedBy("mNetworkQualityLock")
    private int mEffectiveConnectionType = 0;
    private final Object mFinishedListenerLock = new Object();
    @GuardedBy("mFinishedListenerLock")
    private final Map<Listener, RequestFinishedInfoListener> mFinishedListenerMap = new HashMap();
    @GuardedBy("mNetworkQualityLock")
    private int mHttpRttMs = -1;
    private final String mInUseStoragePath;
    private final ConditionVariable mInitCompleted = new ConditionVariable(false);
    @GuardedBy("mLock")
    private boolean mIsLogging;
    private final Object mLock = new Object();
    private final boolean mNetworkQualityEstimatorEnabled;
    private final Object mNetworkQualityLock = new Object();
    private Thread mNetworkThread;
    private final int mNetworkThreadPriority;
    @GuardedBy("mNetworkQualityLock")
    private final ObserverList<NetworkQualityRttListenerWrapper> mRttListenerList = new ObserverList();
    private volatile ConditionVariable mStopNetLogCompleted;
    @GuardedBy("mNetworkQualityLock")
    private final ObserverList<NetworkQualityThroughputListenerWrapper> mThroughputListenerList = new ObserverList();
    @GuardedBy("mNetworkQualityLock")
    private int mTransportRttMs = -1;
    @GuardedBy("mLock")
    private long mUrlRequestContextAdapter = 0;
    private ConditionVariable mWaitGetCertVerifierDataComplete = new ConditionVariable();

    private static native void nativeAddPkp(long j, String str, byte[][] bArr, boolean z, long j2);

    private static native void nativeAddQuicHint(long j, String str, int i, int i2);

    @NativeClassQualifiedName("CronetURLRequestContextAdapter")
    private native void nativeConfigureNetworkQualityEstimatorForTesting(long j, boolean z, boolean z2, boolean z3);

    private static native long nativeCreateRequestContextAdapter(long j);

    private static native long nativeCreateRequestContextConfig(String str, String str2, boolean z, String str3, boolean z2, boolean z3, boolean z4, int i, long j, String str4, long j2, boolean z5, boolean z6, String str5);

    @NativeClassQualifiedName("CronetURLRequestContextAdapter")
    private native void nativeDestroy(long j);

    @NativeClassQualifiedName("CronetURLRequestContextAdapter")
    private native void nativeGetCertVerifierData(long j);

    private static native byte[] nativeGetHistogramDeltas();

    @NativeClassQualifiedName("CronetURLRequestContextAdapter")
    private native void nativeInitRequestContextOnInitThread(long j);

    @NativeClassQualifiedName("CronetURLRequestContextAdapter")
    private native void nativeProvideRTTObservations(long j, boolean z);

    @NativeClassQualifiedName("CronetURLRequestContextAdapter")
    private native void nativeProvideThroughputObservations(long j, boolean z);

    private static native int nativeSetMinLogLevel(int i);

    @NativeClassQualifiedName("CronetURLRequestContextAdapter")
    private native void nativeStartNetLogToDisk(long j, String str, boolean z, int i);

    @NativeClassQualifiedName("CronetURLRequestContextAdapter")
    private native boolean nativeStartNetLogToFile(long j, String str, boolean z);

    @NativeClassQualifiedName("CronetURLRequestContextAdapter")
    private native void nativeStopNetLog(long j);

    @UsedByReflection("CronetEngine.java")
    public CronetUrlRequestContext(CronetEngineBuilderImpl builder) {
        this.mNetworkQualityEstimatorEnabled = builder.networkQualityEstimatorEnabled();
        this.mNetworkThreadPriority = builder.threadPriority(10);
        CronetLibraryLoader.ensureInitialized(builder.getContext(), builder);
        nativeSetMinLogLevel(getLoggingLevel());
        if (builder.httpCacheMode() == 1) {
            this.mInUseStoragePath = builder.storagePath();
            synchronized (sInUseStoragePaths) {
                if (sInUseStoragePaths.add(this.mInUseStoragePath)) {
                } else {
                    throw new IllegalStateException("Disk cache storage path already in use");
                }
            }
        }
        this.mInUseStoragePath = null;
        synchronized (this.mLock) {
            this.mUrlRequestContextAdapter = nativeCreateRequestContextAdapter(createNativeUrlRequestContextConfig(builder));
            if (this.mUrlRequestContextAdapter == 0) {
                throw new NullPointerException("Context Adapter creation failed.");
            }
        }
        CronetLibraryLoader.postToInitThread(new Runnable() {
            public void run() {
                CronetLibraryLoader.ensureInitializedOnInitThread();
                synchronized (CronetUrlRequestContext.this.mLock) {
                    CronetUrlRequestContext.this.nativeInitRequestContextOnInitThread(CronetUrlRequestContext.this.mUrlRequestContextAdapter);
                }
            }
        });
    }

    @VisibleForTesting
    public static long createNativeUrlRequestContextConfig(CronetEngineBuilderImpl builder) {
        long urlRequestContextConfig = nativeCreateRequestContextConfig(builder.getUserAgent(), builder.storagePath(), builder.quicEnabled(), builder.getDefaultQuicUserAgentId(), builder.http2Enabled(), builder.brotliEnabled(), builder.cacheDisabled(), builder.httpCacheMode(), builder.httpCacheMaxSize(), builder.experimentalOptions(), builder.mockCertVerifier(), builder.networkQualityEstimatorEnabled(), builder.publicKeyPinningBypassForLocalTrustAnchorsEnabled(), builder.certVerifierData());
        for (QuicHint quicHint : builder.quicHints()) {
            nativeAddQuicHint(urlRequestContextConfig, quicHint.mHost, quicHint.mPort, quicHint.mAlternatePort);
        }
        for (Pkp pkp : builder.publicKeyPins()) {
            nativeAddPkp(urlRequestContextConfig, pkp.mHost, pkp.mHashes, pkp.mIncludeSubdomains, pkp.mExpirationDate.getTime());
        }
        return urlRequestContextConfig;
    }

    public Builder newBidirectionalStreamBuilder(String url, Callback callback, Executor executor) {
        return new BidirectionalStreamBuilderImpl(url, callback, executor, this);
    }

    public UrlRequestBase createRequest(String url, UrlRequest.Callback callback, Executor executor, int priority, Collection<Object> requestAnnotations, boolean disableCache, boolean disableConnectionMigration, boolean allowDirectExecutor) {
        CronetUrlRequest cronetUrlRequest;
        synchronized (this.mLock) {
            checkHaveAdapter();
            cronetUrlRequest = new CronetUrlRequest(this, url, priority, callback, executor, requestAnnotations, disableCache, disableConnectionMigration, allowDirectExecutor);
        }
        return cronetUrlRequest;
    }

    protected ExperimentalBidirectionalStream createBidirectionalStream(String url, Callback callback, Executor executor, String httpMethod, List<Entry<String, String>> requestHeaders, int priority, boolean delayRequestHeadersUntilFirstFlush, Collection<Object> requestAnnotations) {
        CronetBidirectionalStream cronetBidirectionalStream;
        synchronized (this.mLock) {
            checkHaveAdapter();
            cronetBidirectionalStream = new CronetBidirectionalStream(this, url, priority, callback, executor, httpMethod, requestHeaders, delayRequestHeadersUntilFirstFlush, requestAnnotations);
        }
        return cronetBidirectionalStream;
    }

    public String getVersionString() {
        return "Cronet/" + ImplVersion.getCronetVersionWithLastChange();
    }

    public void shutdown() {
        if (this.mInUseStoragePath != null) {
            synchronized (sInUseStoragePaths) {
                sInUseStoragePaths.remove(this.mInUseStoragePath);
            }
        }
        synchronized (this.mLock) {
            checkHaveAdapter();
            if (this.mActiveRequestCount.get() != 0) {
                throw new IllegalStateException("Cannot shutdown with active requests.");
            } else if (Thread.currentThread() == this.mNetworkThread) {
                throw new IllegalThreadStateException("Cannot shutdown from network thread.");
            }
        }
        this.mInitCompleted.block();
        stopNetLog();
        synchronized (this.mLock) {
            if (haveRequestContextAdapter()) {
                nativeDestroy(this.mUrlRequestContextAdapter);
                this.mUrlRequestContextAdapter = 0;
                return;
            }
        }
    }

    public void startNetLogToFile(String fileName, boolean logAll) {
        synchronized (this.mLock) {
            checkHaveAdapter();
            if (nativeStartNetLogToFile(this.mUrlRequestContextAdapter, fileName, logAll)) {
                this.mIsLogging = true;
            } else {
                throw new RuntimeException("Unable to start NetLog");
            }
        }
    }

    public void startNetLogToDisk(String dirPath, boolean logAll, int maxSize) {
        synchronized (this.mLock) {
            checkHaveAdapter();
            nativeStartNetLogToDisk(this.mUrlRequestContextAdapter, dirPath, logAll, maxSize);
            this.mIsLogging = true;
        }
    }

    public void stopNetLog() {
        synchronized (this.mLock) {
            if (this.mIsLogging) {
                checkHaveAdapter();
                this.mStopNetLogCompleted = new ConditionVariable();
                nativeStopNetLog(this.mUrlRequestContextAdapter);
                this.mIsLogging = false;
                this.mStopNetLogCompleted.block();
                return;
            }
        }
    }

    @CalledByNative
    public void stopNetLogCompleted() {
        this.mStopNetLogCompleted.open();
    }

    public String getCertVerifierData(long timeout) {
        if (timeout < 0) {
            throw new IllegalArgumentException("timeout must be a positive value");
        }
        if (timeout == 0) {
            timeout = 100;
        }
        this.mWaitGetCertVerifierDataComplete.close();
        synchronized (this.mLock) {
            checkHaveAdapter();
            nativeGetCertVerifierData(this.mUrlRequestContextAdapter);
        }
        this.mWaitGetCertVerifierDataComplete.block(timeout);
        return this.mCertVerifierData;
    }

    public byte[] getGlobalMetricsDeltas() {
        return nativeGetHistogramDeltas();
    }

    public int getEffectiveConnectionType() {
        if (this.mNetworkQualityEstimatorEnabled) {
            int convertConnectionTypeToApiValue;
            synchronized (this.mNetworkQualityLock) {
                convertConnectionTypeToApiValue = convertConnectionTypeToApiValue(this.mEffectiveConnectionType);
            }
            return convertConnectionTypeToApiValue;
        }
        throw new IllegalStateException("Network quality estimator must be enabled");
    }

    public int getHttpRttMs() {
        int i = -1;
        if (this.mNetworkQualityEstimatorEnabled) {
            synchronized (this.mNetworkQualityLock) {
                if (this.mHttpRttMs != -1) {
                    i = this.mHttpRttMs;
                }
            }
            return i;
        }
        throw new IllegalStateException("Network quality estimator must be enabled");
    }

    public int getTransportRttMs() {
        int i = -1;
        if (this.mNetworkQualityEstimatorEnabled) {
            synchronized (this.mNetworkQualityLock) {
                if (this.mTransportRttMs != -1) {
                    i = this.mTransportRttMs;
                }
            }
            return i;
        }
        throw new IllegalStateException("Network quality estimator must be enabled");
    }

    public int getDownstreamThroughputKbps() {
        int i = -1;
        if (this.mNetworkQualityEstimatorEnabled) {
            synchronized (this.mNetworkQualityLock) {
                if (this.mDownstreamThroughputKbps != -1) {
                    i = this.mDownstreamThroughputKbps;
                }
            }
            return i;
        }
        throw new IllegalStateException("Network quality estimator must be enabled");
    }

    @VisibleForTesting
    public void configureNetworkQualityEstimatorForTesting(boolean useLocalHostRequests, boolean useSmallerResponses, boolean disableOfflineCheck) {
        if (this.mNetworkQualityEstimatorEnabled) {
            synchronized (this.mLock) {
                checkHaveAdapter();
                nativeConfigureNetworkQualityEstimatorForTesting(this.mUrlRequestContextAdapter, useLocalHostRequests, useSmallerResponses, disableOfflineCheck);
            }
            return;
        }
        throw new IllegalStateException("Network quality estimator must be enabled");
    }

    public void addRttListener(NetworkQualityRttListener listener) {
        if (this.mNetworkQualityEstimatorEnabled) {
            synchronized (this.mNetworkQualityLock) {
                if (this.mRttListenerList.isEmpty()) {
                    synchronized (this.mLock) {
                        checkHaveAdapter();
                        nativeProvideRTTObservations(this.mUrlRequestContextAdapter, true);
                    }
                }
                this.mRttListenerList.addObserver(new NetworkQualityRttListenerWrapper(listener));
            }
            return;
        }
        throw new IllegalStateException("Network quality estimator must be enabled");
    }

    public void removeRttListener(NetworkQualityRttListener listener) {
        if (this.mNetworkQualityEstimatorEnabled) {
            synchronized (this.mNetworkQualityLock) {
                if (this.mRttListenerList.removeObserver(new NetworkQualityRttListenerWrapper(listener)) && this.mRttListenerList.isEmpty()) {
                    synchronized (this.mLock) {
                        checkHaveAdapter();
                        nativeProvideRTTObservations(this.mUrlRequestContextAdapter, false);
                    }
                }
            }
            return;
        }
        throw new IllegalStateException("Network quality estimator must be enabled");
    }

    public void addThroughputListener(NetworkQualityThroughputListener listener) {
        if (this.mNetworkQualityEstimatorEnabled) {
            synchronized (this.mNetworkQualityLock) {
                if (this.mThroughputListenerList.isEmpty()) {
                    synchronized (this.mLock) {
                        checkHaveAdapter();
                        nativeProvideThroughputObservations(this.mUrlRequestContextAdapter, true);
                    }
                }
                this.mThroughputListenerList.addObserver(new NetworkQualityThroughputListenerWrapper(listener));
            }
            return;
        }
        throw new IllegalStateException("Network quality estimator must be enabled");
    }

    public void removeThroughputListener(NetworkQualityThroughputListener listener) {
        if (this.mNetworkQualityEstimatorEnabled) {
            synchronized (this.mNetworkQualityLock) {
                if (this.mThroughputListenerList.removeObserver(new NetworkQualityThroughputListenerWrapper(listener)) && this.mThroughputListenerList.isEmpty()) {
                    synchronized (this.mLock) {
                        checkHaveAdapter();
                        nativeProvideThroughputObservations(this.mUrlRequestContextAdapter, false);
                    }
                }
            }
            return;
        }
        throw new IllegalStateException("Network quality estimator must be enabled");
    }

    public void addRequestFinishedListener(Listener listener) {
        synchronized (this.mFinishedListenerLock) {
            this.mFinishedListenerMap.put(listener, new RequestFinishedInfoListener(listener));
        }
    }

    public void removeRequestFinishedListener(Listener listener) {
        synchronized (this.mFinishedListenerLock) {
            this.mFinishedListenerMap.remove(listener);
        }
    }

    boolean hasRequestFinishedListener() {
        boolean z;
        synchronized (this.mFinishedListenerLock) {
            z = !this.mFinishedListenerMap.isEmpty();
        }
        return z;
    }

    public URLConnection openConnection(URL url) {
        return openConnection(url, Proxy.NO_PROXY);
    }

    public URLConnection openConnection(URL url, Proxy proxy) {
        if (proxy.type() != Type.DIRECT) {
            throw new UnsupportedOperationException();
        }
        String protocol = url.getProtocol();
        if ("http".equals(protocol) || "https".equals(protocol)) {
            return new CronetHttpURLConnection(url, this);
        }
        throw new UnsupportedOperationException("Unexpected protocol:" + protocol);
    }

    public URLStreamHandlerFactory createURLStreamHandlerFactory() {
        return new CronetURLStreamHandlerFactory(this);
    }

    void onRequestStarted() {
        this.mActiveRequestCount.incrementAndGet();
    }

    void onRequestDestroyed() {
        this.mActiveRequestCount.decrementAndGet();
    }

    @VisibleForTesting
    public long getUrlRequestContextAdapter() {
        long j;
        synchronized (this.mLock) {
            checkHaveAdapter();
            j = this.mUrlRequestContextAdapter;
        }
        return j;
    }

    @GuardedBy("mLock")
    private void checkHaveAdapter() throws IllegalStateException {
        if (!haveRequestContextAdapter()) {
            throw new IllegalStateException("Engine is shut down.");
        }
    }

    @GuardedBy("mLock")
    private boolean haveRequestContextAdapter() {
        return this.mUrlRequestContextAdapter != 0;
    }

    private int getLoggingLevel() {
        if (Log.isLoggable(LOG_TAG, 2)) {
            return -2;
        }
        if (Log.isLoggable(LOG_TAG, 3)) {
            return -1;
        }
        return 3;
    }

    private static int convertConnectionTypeToApiValue(int type) {
        switch (type) {
            case 0:
                return 0;
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
            default:
                throw new RuntimeException("Internal Error: Illegal EffectiveConnectionType value " + type);
        }
    }

    @CalledByNative
    private void initNetworkThread() {
        this.mNetworkThread = Thread.currentThread();
        this.mInitCompleted.open();
        Thread.currentThread().setName("ChromiumNet");
        Process.setThreadPriority(this.mNetworkThreadPriority);
    }

    @CalledByNative
    private void onEffectiveConnectionTypeChanged(int effectiveConnectionType) {
        synchronized (this.mNetworkQualityLock) {
            this.mEffectiveConnectionType = effectiveConnectionType;
        }
    }

    @CalledByNative
    private void onRTTOrThroughputEstimatesComputed(int httpRttMs, int transportRttMs, int downstreamThroughputKbps) {
        synchronized (this.mNetworkQualityLock) {
            this.mHttpRttMs = httpRttMs;
            this.mTransportRttMs = transportRttMs;
            this.mDownstreamThroughputKbps = downstreamThroughputKbps;
        }
    }

    @CalledByNative
    private void onRttObservation(int rttMs, long whenMs, int source) {
        synchronized (this.mNetworkQualityLock) {
            Iterator it = this.mRttListenerList.iterator();
            while (it.hasNext()) {
                final NetworkQualityRttListenerWrapper listener = (NetworkQualityRttListenerWrapper) it.next();
                final int i = rttMs;
                final long j = whenMs;
                final int i2 = source;
                postObservationTaskToExecutor(listener.getExecutor(), new Runnable() {
                    public void run() {
                        listener.onRttObservation(i, j, i2);
                    }
                });
            }
        }
    }

    @CalledByNative
    private void onThroughputObservation(int throughputKbps, long whenMs, int source) {
        synchronized (this.mNetworkQualityLock) {
            Iterator it = this.mThroughputListenerList.iterator();
            while (it.hasNext()) {
                final NetworkQualityThroughputListenerWrapper listener = (NetworkQualityThroughputListenerWrapper) it.next();
                final int i = throughputKbps;
                final long j = whenMs;
                final int i2 = source;
                postObservationTaskToExecutor(listener.getExecutor(), new Runnable() {
                    public void run() {
                        listener.onThroughputObservation(i, j, i2);
                    }
                });
            }
        }
    }

    @CalledByNative
    private void onGetCertVerifierData(String certVerifierData) {
        this.mCertVerifierData = certVerifierData;
        this.mWaitGetCertVerifierDataComplete.open();
    }

    void reportFinished(final RequestFinishedInfo requestInfo) {
        ArrayList<RequestFinishedInfoListener> currentListeners;
        synchronized (this.mFinishedListenerLock) {
            currentListeners = new ArrayList(this.mFinishedListenerMap.values());
        }
        Iterator it = currentListeners.iterator();
        while (it.hasNext()) {
            final RequestFinishedInfoListener listener = (RequestFinishedInfoListener) it.next();
            postObservationTaskToExecutor(listener.getExecutor(), new Runnable() {
                public void run() {
                    listener.onRequestFinished(requestInfo);
                }
            });
        }
    }

    private static void postObservationTaskToExecutor(Executor executor, Runnable task) {
        try {
            executor.execute(task);
        } catch (RejectedExecutionException failException) {
            Log.e(LOG_TAG, "Exception posting task to executor", failException);
        }
    }

    public boolean isNetworkThread(Thread thread) {
        return thread == this.mNetworkThread;
    }
}
