package org.chromium.net;

import java.util.Date;
import java.util.Set;
import org.chromium.net.CronetEngine.Builder.LibraryLoader;

public abstract class ICronetEngineBuilder {
    public abstract ICronetEngineBuilder addPublicKeyPins(String str, Set<byte[]> set, boolean z, Date date);

    public abstract ICronetEngineBuilder addQuicHint(String str, int i, int i2);

    public abstract ExperimentalCronetEngine build();

    public abstract ICronetEngineBuilder enableHttp2(boolean z);

    public abstract ICronetEngineBuilder enableHttpCache(int i, long j);

    public abstract ICronetEngineBuilder enablePublicKeyPinningBypassForLocalTrustAnchors(boolean z);

    public abstract ICronetEngineBuilder enableQuic(boolean z);

    public abstract ICronetEngineBuilder enableSdch(boolean z);

    public abstract String getDefaultUserAgent();

    public abstract ICronetEngineBuilder setExperimentalOptions(String str);

    public abstract ICronetEngineBuilder setLibraryLoader(LibraryLoader libraryLoader);

    public abstract ICronetEngineBuilder setStoragePath(String str);

    public abstract ICronetEngineBuilder setUserAgent(String str);

    public ICronetEngineBuilder enableBrotli(boolean value) {
        return this;
    }

    public ICronetEngineBuilder enableNetworkQualityEstimator(boolean value) {
        return this;
    }

    public ICronetEngineBuilder setCertVerifierData(String certVerifierData) {
        return this;
    }

    public ICronetEngineBuilder setThreadPriority(int priority) {
        return this;
    }
}
