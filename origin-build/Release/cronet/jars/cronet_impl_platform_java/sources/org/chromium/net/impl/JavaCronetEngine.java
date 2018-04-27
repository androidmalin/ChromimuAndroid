package org.chromium.net.impl;

import android.os.Process;
import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import org.chromium.net.BidirectionalStream;
import org.chromium.net.ExperimentalBidirectionalStream;
import org.chromium.net.ExperimentalBidirectionalStream.Builder;
import org.chromium.net.NetworkQualityRttListener;
import org.chromium.net.NetworkQualityThroughputListener;
import org.chromium.net.RequestFinishedInfo.Listener;
import org.chromium.net.UrlRequest.Callback;

public final class JavaCronetEngine extends CronetEngineBase {
    private final ExecutorService mExecutorService;
    private final String mUserAgent;

    public JavaCronetEngine(CronetEngineBuilderImpl builder) {
        final int threadPriority = builder.threadPriority(9);
        this.mUserAgent = builder.getUserAgent();
        this.mExecutorService = Executors.newCachedThreadPool(new ThreadFactory() {
            public Thread newThread(final Runnable r) {
                return Executors.defaultThreadFactory().newThread(new Runnable() {
                    public void run() {
                        Thread.currentThread().setName("JavaCronetEngine");
                        Process.setThreadPriority(threadPriority);
                        r.run();
                    }
                });
            }
        });
    }

    public UrlRequestBase createRequest(String url, Callback callback, Executor executor, int priority, Collection<Object> collection, boolean disableCache, boolean disableConnectionMigration, boolean allowDirectExecutor) {
        return new JavaUrlRequest(callback, this.mExecutorService, executor, url, this.mUserAgent, allowDirectExecutor);
    }

    protected ExperimentalBidirectionalStream createBidirectionalStream(String url, BidirectionalStream.Callback callback, Executor executor, String httpMethod, List<Entry<String, String>> list, int priority, boolean delayRequestHeadersUntilFirstFlush, Collection<Object> collection) {
        throw new UnsupportedOperationException("Can't create a bidi stream - httpurlconnection doesn't have those APIs");
    }

    public Builder newBidirectionalStreamBuilder(String url, BidirectionalStream.Callback callback, Executor executor) {
        throw new UnsupportedOperationException("The bidirectional stream API is not supported by the Java implementation of Cronet Engine");
    }

    public String getVersionString() {
        return "CronetHttpURLConnection/" + ImplVersion.getCronetVersionWithLastChange();
    }

    public void shutdown() {
        this.mExecutorService.shutdown();
    }

    public void startNetLogToFile(String fileName, boolean logAll) {
    }

    public void startNetLogToDisk(String dirPath, boolean logAll, int maxSize) {
    }

    public void stopNetLog() {
    }

    public String getCertVerifierData(long timeout) {
        return "";
    }

    public byte[] getGlobalMetricsDeltas() {
        return new byte[0];
    }

    public int getEffectiveConnectionType() {
        return 0;
    }

    public int getHttpRttMs() {
        return -1;
    }

    public int getTransportRttMs() {
        return -1;
    }

    public int getDownstreamThroughputKbps() {
        return -1;
    }

    public void configureNetworkQualityEstimatorForTesting(boolean useLocalHostRequests, boolean useSmallerResponses, boolean disableOfflineCheck) {
    }

    public void addRttListener(NetworkQualityRttListener listener) {
    }

    public void removeRttListener(NetworkQualityRttListener listener) {
    }

    public void addThroughputListener(NetworkQualityThroughputListener listener) {
    }

    public void removeThroughputListener(NetworkQualityThroughputListener listener) {
    }

    public void addRequestFinishedListener(Listener listener) {
    }

    public void removeRequestFinishedListener(Listener listener) {
    }

    public URLConnection openConnection(URL url) throws IOException {
        return url.openConnection();
    }

    public URLConnection openConnection(URL url, Proxy proxy) throws IOException {
        return url.openConnection(proxy);
    }

    public URLStreamHandlerFactory createURLStreamHandlerFactory() {
        return new URLStreamHandlerFactory() {
            public URLStreamHandler createURLStreamHandler(String protocol) {
                return null;
            }
        };
    }
}
