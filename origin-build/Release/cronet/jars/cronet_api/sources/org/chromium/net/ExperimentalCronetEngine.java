package org.chromium.net;

import android.content.Context;
import android.support.annotation.VisibleForTesting;
import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.Executor;
import org.chromium.net.BidirectionalStream.Callback;
import org.chromium.net.CronetEngine.Builder.LibraryLoader;
import org.chromium.net.RequestFinishedInfo.Listener;

public abstract class ExperimentalCronetEngine extends CronetEngine {
    public static final int CONNECTION_METRIC_UNKNOWN = -1;
    public static final int EFFECTIVE_CONNECTION_TYPE_2G = 3;
    public static final int EFFECTIVE_CONNECTION_TYPE_3G = 4;
    public static final int EFFECTIVE_CONNECTION_TYPE_4G = 5;
    public static final int EFFECTIVE_CONNECTION_TYPE_OFFLINE = 1;
    public static final int EFFECTIVE_CONNECTION_TYPE_SLOW_2G = 2;
    public static final int EFFECTIVE_CONNECTION_TYPE_UNKNOWN = 0;

    public static class Builder extends org.chromium.net.CronetEngine.Builder {
        public Builder(Context context) {
            super(context);
        }

        public Builder(ICronetEngineBuilder builderDelegate) {
            super(builderDelegate);
        }

        public Builder enableNetworkQualityEstimator(boolean value) {
            this.mBuilderDelegate.enableNetworkQualityEstimator(value);
            return this;
        }

        public Builder setCertVerifierData(String certVerifierData) {
            this.mBuilderDelegate.setCertVerifierData(certVerifierData);
            return this;
        }

        public Builder setExperimentalOptions(String options) {
            this.mBuilderDelegate.setExperimentalOptions(options);
            return this;
        }

        public Builder setThreadPriority(int priority) {
            this.mBuilderDelegate.setThreadPriority(priority);
            return this;
        }

        @VisibleForTesting
        public ICronetEngineBuilder getBuilderDelegate() {
            return this.mBuilderDelegate;
        }

        public Builder setUserAgent(String userAgent) {
            super.setUserAgent(userAgent);
            return this;
        }

        public Builder setStoragePath(String value) {
            super.setStoragePath(value);
            return this;
        }

        public Builder setLibraryLoader(LibraryLoader loader) {
            super.setLibraryLoader(loader);
            return this;
        }

        public Builder enableQuic(boolean value) {
            super.enableQuic(value);
            return this;
        }

        public Builder enableHttp2(boolean value) {
            super.enableHttp2(value);
            return this;
        }

        public Builder enableSdch(boolean value) {
            return this;
        }

        public Builder enableHttpCache(int cacheMode, long maxSize) {
            super.enableHttpCache(cacheMode, maxSize);
            return this;
        }

        public Builder addQuicHint(String host, int port, int alternatePort) {
            super.addQuicHint(host, port, alternatePort);
            return this;
        }

        public Builder addPublicKeyPins(String hostName, Set<byte[]> pinsSha256, boolean includeSubdomains, Date expirationDate) {
            super.addPublicKeyPins(hostName, pinsSha256, includeSubdomains, expirationDate);
            return this;
        }

        public Builder enablePublicKeyPinningBypassForLocalTrustAnchors(boolean value) {
            super.enablePublicKeyPinningBypassForLocalTrustAnchors(value);
            return this;
        }

        public ExperimentalCronetEngine build() {
            return this.mBuilderDelegate.build();
        }
    }

    public abstract org.chromium.net.ExperimentalBidirectionalStream.Builder newBidirectionalStreamBuilder(String str, Callback callback, Executor executor);

    public abstract org.chromium.net.ExperimentalUrlRequest.Builder newUrlRequestBuilder(String str, UrlRequest.Callback callback, Executor executor);

    public void startNetLogToDisk(String dirPath, boolean logAll, int maxSize) {
    }

    public int getEffectiveConnectionType() {
        return 0;
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

    public URLConnection openConnection(URL url, Proxy proxy) throws IOException {
        return url.openConnection(proxy);
    }

    public void addRequestFinishedListener(Listener listener) {
    }

    public void removeRequestFinishedListener(Listener listener) {
    }

    public String getCertVerifierData(long timeout) {
        return "";
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
}
