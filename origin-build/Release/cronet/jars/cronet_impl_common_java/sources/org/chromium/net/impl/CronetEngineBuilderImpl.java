package org.chromium.net.impl;

import android.content.Context;
import android.support.annotation.VisibleForTesting;
import java.io.File;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.IDN;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.chromium.net.CronetEngine.Builder.LibraryLoader;
import org.chromium.net.ICronetEngineBuilder;

public abstract class CronetEngineBuilderImpl extends ICronetEngineBuilder {
    private static final Pattern INVALID_PKP_HOST_NAME = Pattern.compile("^[0-9\\.]*$");
    private static final int INVALID_THREAD_PRIORITY = 20;
    private final Context mApplicationContext;
    private boolean mBrotiEnabled;
    private String mCertVerifierData;
    private boolean mDisableCache;
    private String mExperimentalOptions;
    private boolean mHttp2Enabled;
    private long mHttpCacheMaxSize;
    private int mHttpCacheMode;
    protected long mMockCertVerifier;
    private boolean mNetworkQualityEstimatorEnabled;
    private final List<Pkp> mPkps = new LinkedList();
    private boolean mPublicKeyPinningBypassForLocalTrustAnchorsEnabled;
    private boolean mQuicEnabled;
    private final List<QuicHint> mQuicHints = new LinkedList();
    private String mStoragePath;
    private int mThreadPriority = INVALID_THREAD_PRIORITY;
    private String mUserAgent;

    @Retention(RetentionPolicy.SOURCE)
    public @interface HttpCacheSetting {
    }

    public static class Pkp {
        final Date mExpirationDate;
        final byte[][] mHashes;
        final String mHost;
        final boolean mIncludeSubdomains;

        Pkp(String host, byte[][] hashes, boolean includeSubdomains, Date expirationDate) {
            this.mHost = host;
            this.mHashes = hashes;
            this.mIncludeSubdomains = includeSubdomains;
            this.mExpirationDate = expirationDate;
        }
    }

    public static class QuicHint {
        final int mAlternatePort;
        final String mHost;
        final int mPort;

        QuicHint(String host, int port, int alternatePort) {
            this.mHost = host;
            this.mPort = port;
            this.mAlternatePort = alternatePort;
        }
    }

    public CronetEngineBuilderImpl(Context context) {
        this.mApplicationContext = context.getApplicationContext();
        enableQuic(false);
        enableHttp2(true);
        enableBrotli(false);
        enableHttpCache(0, 0);
        enableNetworkQualityEstimator(false);
        enablePublicKeyPinningBypassForLocalTrustAnchors(true);
    }

    public String getDefaultUserAgent() {
        return UserAgent.from(this.mApplicationContext);
    }

    public CronetEngineBuilderImpl setUserAgent(String userAgent) {
        this.mUserAgent = userAgent;
        return this;
    }

    String getUserAgent() {
        return this.mUserAgent;
    }

    public CronetEngineBuilderImpl setStoragePath(String value) {
        if (new File(value).isDirectory()) {
            this.mStoragePath = value;
            return this;
        }
        throw new IllegalArgumentException("Storage path must be set to existing directory");
    }

    String storagePath() {
        return this.mStoragePath;
    }

    public CronetEngineBuilderImpl setLibraryLoader(LibraryLoader loader) {
        return this;
    }

    VersionSafeCallbacks.LibraryLoader libraryLoader() {
        return null;
    }

    public CronetEngineBuilderImpl enableQuic(boolean value) {
        this.mQuicEnabled = value;
        return this;
    }

    boolean quicEnabled() {
        return this.mQuicEnabled;
    }

    String getDefaultQuicUserAgentId() {
        return this.mQuicEnabled ? UserAgent.getQuicUserAgentIdFrom(this.mApplicationContext) : "";
    }

    public CronetEngineBuilderImpl enableHttp2(boolean value) {
        this.mHttp2Enabled = value;
        return this;
    }

    boolean http2Enabled() {
        return this.mHttp2Enabled;
    }

    public CronetEngineBuilderImpl enableSdch(boolean value) {
        return this;
    }

    public CronetEngineBuilderImpl enableBrotli(boolean value) {
        this.mBrotiEnabled = value;
        return this;
    }

    boolean brotliEnabled() {
        return this.mBrotiEnabled;
    }

    public CronetEngineBuilderImpl enableHttpCache(int cacheMode, long maxSize) {
        if (cacheMode == 3 || cacheMode == 2) {
            if (storagePath() == null) {
                throw new IllegalArgumentException("Storage path must be set");
            }
        } else if (storagePath() != null) {
            throw new IllegalArgumentException("Storage path must not be set");
        }
        boolean z = cacheMode == 0 || cacheMode == 2;
        this.mDisableCache = z;
        this.mHttpCacheMaxSize = maxSize;
        switch (cacheMode) {
            case 0:
                this.mHttpCacheMode = 0;
                break;
            case 1:
                this.mHttpCacheMode = 2;
                break;
            case 2:
            case 3:
                this.mHttpCacheMode = 1;
                break;
            default:
                throw new IllegalArgumentException("Unknown cache mode");
        }
        return this;
    }

    boolean cacheDisabled() {
        return this.mDisableCache;
    }

    long httpCacheMaxSize() {
        return this.mHttpCacheMaxSize;
    }

    int httpCacheMode() {
        return this.mHttpCacheMode;
    }

    public CronetEngineBuilderImpl addQuicHint(String host, int port, int alternatePort) {
        if (host.contains("/")) {
            throw new IllegalArgumentException("Illegal QUIC Hint Host: " + host);
        }
        this.mQuicHints.add(new QuicHint(host, port, alternatePort));
        return this;
    }

    List<QuicHint> quicHints() {
        return this.mQuicHints;
    }

    public CronetEngineBuilderImpl addPublicKeyPins(String hostName, Set<byte[]> pinsSha256, boolean includeSubdomains, Date expirationDate) {
        if (hostName == null) {
            throw new NullPointerException("The hostname cannot be null");
        } else if (pinsSha256 == null) {
            throw new NullPointerException("The set of SHA256 pins cannot be null");
        } else if (expirationDate == null) {
            throw new NullPointerException("The pin expiration date cannot be null");
        } else {
            String idnHostName = validateHostNameForPinningAndConvert(hostName);
            Set<byte[]> hashes = new HashSet(pinsSha256.size());
            for (byte[] pinSha256 : pinsSha256) {
                if (pinSha256 == null || pinSha256.length != 32) {
                    throw new IllegalArgumentException("Public key pin is invalid");
                }
                hashes.add(pinSha256);
            }
            this.mPkps.add(new Pkp(idnHostName, (byte[][]) hashes.toArray(new byte[hashes.size()][]), includeSubdomains, expirationDate));
            return this;
        }
    }

    List<Pkp> publicKeyPins() {
        return this.mPkps;
    }

    public CronetEngineBuilderImpl enablePublicKeyPinningBypassForLocalTrustAnchors(boolean value) {
        this.mPublicKeyPinningBypassForLocalTrustAnchorsEnabled = value;
        return this;
    }

    boolean publicKeyPinningBypassForLocalTrustAnchorsEnabled() {
        return this.mPublicKeyPinningBypassForLocalTrustAnchorsEnabled;
    }

    private static String validateHostNameForPinningAndConvert(String hostName) throws IllegalArgumentException {
        if (INVALID_PKP_HOST_NAME.matcher(hostName).matches()) {
            throw new IllegalArgumentException("Hostname " + hostName + " is illegal. A hostname should not consist of digits and/or dots only.");
        } else if (hostName.length() > 255) {
            throw new IllegalArgumentException("Hostname " + hostName + " is too long. The name of the host does not comply with RFC 1122 and RFC 1123.");
        } else {
            try {
                return IDN.toASCII(hostName, 2);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Hostname " + hostName + " is illegal. The name of the host does not comply with RFC 1122 and RFC 1123.");
            }
        }
    }

    public CronetEngineBuilderImpl setExperimentalOptions(String options) {
        this.mExperimentalOptions = options;
        return this;
    }

    public String experimentalOptions() {
        return this.mExperimentalOptions;
    }

    @VisibleForTesting
    public CronetEngineBuilderImpl setMockCertVerifierForTesting(long mockCertVerifier) {
        this.mMockCertVerifier = mockCertVerifier;
        return this;
    }

    long mockCertVerifier() {
        return this.mMockCertVerifier;
    }

    boolean networkQualityEstimatorEnabled() {
        return this.mNetworkQualityEstimatorEnabled;
    }

    public CronetEngineBuilderImpl setCertVerifierData(String certVerifierData) {
        this.mCertVerifierData = certVerifierData;
        return this;
    }

    public CronetEngineBuilderImpl enableNetworkQualityEstimator(boolean value) {
        this.mNetworkQualityEstimatorEnabled = value;
        return this;
    }

    String certVerifierData() {
        return this.mCertVerifierData;
    }

    public CronetEngineBuilderImpl setThreadPriority(int priority) {
        if (priority > 19 || priority < -20) {
            throw new IllegalArgumentException("Thread priority invalid");
        }
        this.mThreadPriority = priority;
        return this;
    }

    int threadPriority(int defaultThreadPriority) {
        return this.mThreadPriority == INVALID_THREAD_PRIORITY ? defaultThreadPriority : this.mThreadPriority;
    }

    Context getContext() {
        return this.mApplicationContext;
    }
}
