package org.chromium.net;

import android.content.Context;
import android.support.annotation.VisibleForTesting;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandlerFactory;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import org.chromium.net.UrlRequest.Callback;

public abstract class CronetEngine {

    public static class Builder {
        public static final int HTTP_CACHE_DISABLED = 0;
        public static final int HTTP_CACHE_DISK = 3;
        public static final int HTTP_CACHE_DISK_NO_HTTP = 2;
        public static final int HTTP_CACHE_IN_MEMORY = 1;
        protected final ICronetEngineBuilder mBuilderDelegate;

        public static abstract class LibraryLoader {
            public abstract void loadLibrary(String str);
        }

        public Builder(Context context) {
            this(createBuilderDelegate(context));
        }

        public Builder(ICronetEngineBuilder builderDelegate) {
            this.mBuilderDelegate = builderDelegate;
        }

        public String getDefaultUserAgent() {
            return this.mBuilderDelegate.getDefaultUserAgent();
        }

        public Builder setUserAgent(String userAgent) {
            this.mBuilderDelegate.setUserAgent(userAgent);
            return this;
        }

        public Builder setStoragePath(String value) {
            this.mBuilderDelegate.setStoragePath(value);
            return this;
        }

        public Builder setLibraryLoader(LibraryLoader loader) {
            this.mBuilderDelegate.setLibraryLoader(loader);
            return this;
        }

        public Builder enableQuic(boolean value) {
            this.mBuilderDelegate.enableQuic(value);
            return this;
        }

        public Builder enableHttp2(boolean value) {
            this.mBuilderDelegate.enableHttp2(value);
            return this;
        }

        @Deprecated
        public Builder enableSdch(boolean value) {
            return this;
        }

        public Builder enableBrotli(boolean value) {
            this.mBuilderDelegate.enableBrotli(value);
            return this;
        }

        public Builder enableHttpCache(int cacheMode, long maxSize) {
            this.mBuilderDelegate.enableHttpCache(cacheMode, maxSize);
            return this;
        }

        public Builder addQuicHint(String host, int port, int alternatePort) {
            this.mBuilderDelegate.addQuicHint(host, port, alternatePort);
            return this;
        }

        public Builder addPublicKeyPins(String hostName, Set<byte[]> pinsSha256, boolean includeSubdomains, Date expirationDate) {
            this.mBuilderDelegate.addPublicKeyPins(hostName, pinsSha256, includeSubdomains, expirationDate);
            return this;
        }

        public Builder enablePublicKeyPinningBypassForLocalTrustAnchors(boolean value) {
            this.mBuilderDelegate.enablePublicKeyPinningBypassForLocalTrustAnchors(value);
            return this;
        }

        public CronetEngine build() {
            return this.mBuilderDelegate.build();
        }

        private static ICronetEngineBuilder createBuilderDelegate(Context context) {
            return ((CronetProvider) getEnabledCronetProviders(context, CronetProvider.getAllProviders(context)).get(0)).createBuilder().mBuilderDelegate;
        }

        @VisibleForTesting
        static List<CronetProvider> getEnabledCronetProviders(Context context, List<CronetProvider> providers) {
            if (providers.size() == 0) {
                throw new RuntimeException("Unable to find any Cronet provider. Have you included all necessary jars?");
            }
            Iterator<CronetProvider> i = providers.iterator();
            while (i.hasNext()) {
                if (!((CronetProvider) i.next()).isEnabled()) {
                    i.remove();
                }
            }
            if (providers.size() == 0) {
                throw new RuntimeException("All available Cronet providers are disabled. A provider should be enabled before it can be used.");
            }
            Collections.sort(providers, new Comparator<CronetProvider>() {
                public int compare(CronetProvider p1, CronetProvider p2) {
                    if (CronetProvider.PROVIDER_NAME_FALLBACK.equals(p1.getName())) {
                        return 1;
                    }
                    if (CronetProvider.PROVIDER_NAME_FALLBACK.equals(p2.getName())) {
                        return -1;
                    }
                    return -Builder.compareVersions(p1.getVersion(), p2.getVersion());
                }
            });
            return providers;
        }

        @VisibleForTesting
        static int compareVersions(String s1, String s2) {
            if (s1 == null || s2 == null) {
                throw new IllegalArgumentException("The input values cannot be null");
            }
            String[] s1segments = s1.split("\\.");
            String[] s2segments = s2.split("\\.");
            int i = 0;
            while (i < s1segments.length && i < s2segments.length) {
                try {
                    int s1segment = Integer.parseInt(s1segments[i]);
                    int s2segment = Integer.parseInt(s2segments[i]);
                    if (s1segment != s2segment) {
                        return Integer.signum(s1segment - s2segment);
                    }
                    i++;
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Unable to convert version segments into integers: " + s1segments[i] + " & " + s2segments[i], e);
                }
            }
            return Integer.signum(s1segments.length - s2segments.length);
        }
    }

    public abstract URLStreamHandlerFactory createURLStreamHandlerFactory();

    public abstract byte[] getGlobalMetricsDeltas();

    public abstract String getVersionString();

    public abstract org.chromium.net.UrlRequest.Builder newUrlRequestBuilder(String str, Callback callback, Executor executor);

    public abstract URLConnection openConnection(URL url) throws IOException;

    public abstract void shutdown();

    public abstract void startNetLogToFile(String str, boolean z);

    public abstract void stopNetLog();
}
