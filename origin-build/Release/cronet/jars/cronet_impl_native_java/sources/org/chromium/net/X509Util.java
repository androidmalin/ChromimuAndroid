package org.chromium.net;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.http.X509TrustManagerExtensions;
import android.os.Build.VERSION;
import android.util.Log;
import android.util.Pair;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import javax.security.auth.x500.X500Principal;
import org.chromium.base.ContextUtils;
import org.chromium.base.annotations.JNINamespace;
import org.chromium.base.metrics.RecordHistogram;

@JNINamespace("net")
public class X509Util {
    static final /* synthetic */ boolean $assertionsDisabled;
    private static final char[] HEX_DIGITS = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
    private static final String OID_ANY_EKU = "2.5.29.37.0";
    private static final String OID_SERVER_GATED_MICROSOFT = "1.3.6.1.4.1.311.10.3.3";
    private static final String OID_SERVER_GATED_NETSCAPE = "2.16.840.1.113730.4.1";
    private static final String OID_TLS_SERVER_AUTH = "1.3.6.1.5.5.7.3.1";
    private static final String TAG = "X509Util";
    private static CertificateFactory sCertificateFactory;
    private static X509TrustManagerImplementation sDefaultTrustManager;
    private static boolean sDisableNativeCodeForTest;
    private static boolean sLoadedSystemKeyStore;
    private static final Object sLock = new Object();
    private static File sSystemCertificateDirectory;
    private static KeyStore sSystemKeyStore;
    private static Set<Pair<X500Principal, PublicKey>> sSystemTrustAnchorCache;
    private static KeyStore sTestKeyStore;
    private static X509TrustManagerImplementation sTestTrustManager;
    private static TrustStorageListener sTrustStorageListener;

    private static final class TrustStorageListener extends BroadcastReceiver {
        private TrustStorageListener() {
        }

        public void onReceive(Context context, Intent intent) {
            boolean shouldReloadTrustManager = X509Util.$assertionsDisabled;
            if (VERSION.SDK_INT < 26) {
                shouldReloadTrustManager = "android.security.STORAGE_CHANGED".equals(intent.getAction());
            } else if ("android.security.action.KEYCHAIN_CHANGED".equals(intent.getAction()) || "android.security.action.TRUST_STORE_CHANGED".equals(intent.getAction())) {
                shouldReloadTrustManager = true;
            } else if ("android.security.action.KEY_ACCESS_CHANGED".equals(intent.getAction()) && !intent.getBooleanExtra("android.security.extra.KEY_ACCESSIBLE", X509Util.$assertionsDisabled)) {
                shouldReloadTrustManager = true;
            }
            if (shouldReloadTrustManager) {
                try {
                    X509Util.reloadDefaultTrustManager();
                } catch (CertificateException e) {
                    Log.e(X509Util.TAG, "Unable to reload the default TrustManager", e);
                } catch (KeyStoreException e2) {
                    Log.e(X509Util.TAG, "Unable to reload the default TrustManager", e2);
                } catch (NoSuchAlgorithmException e3) {
                    Log.e(X509Util.TAG, "Unable to reload the default TrustManager", e3);
                }
            }
        }
    }

    private interface X509TrustManagerImplementation {
        List<X509Certificate> checkServerTrusted(X509Certificate[] x509CertificateArr, String str, String str2) throws CertificateException;
    }

    private static final class X509TrustManagerIceCreamSandwich implements X509TrustManagerImplementation {
        private final X509TrustManager mTrustManager;

        public X509TrustManagerIceCreamSandwich(X509TrustManager trustManager) {
            this.mTrustManager = trustManager;
        }

        public List<X509Certificate> checkServerTrusted(X509Certificate[] chain, String authType, String host) throws CertificateException {
            this.mTrustManager.checkServerTrusted(chain, authType);
            return Collections.emptyList();
        }
    }

    private static final class X509TrustManagerJellyBean implements X509TrustManagerImplementation {
        private final X509TrustManagerExtensions mTrustManagerExtensions;

        @SuppressLint({"NewApi"})
        public X509TrustManagerJellyBean(X509TrustManager trustManager) {
            this.mTrustManagerExtensions = new X509TrustManagerExtensions(trustManager);
        }

        @SuppressLint({"NewApi"})
        public List<X509Certificate> checkServerTrusted(X509Certificate[] chain, String authType, String host) throws CertificateException {
            return this.mTrustManagerExtensions.checkServerTrusted(chain, authType, host);
        }
    }

    private static native void nativeNotifyKeyChainChanged();

    static {
        boolean z;
        if (X509Util.class.desiredAssertionStatus()) {
            z = $assertionsDisabled;
        } else {
            z = true;
        }
        $assertionsDisabled = z;
    }

    private static void ensureInitialized() throws CertificateException, KeyStoreException, NoSuchAlgorithmException {
        synchronized (sLock) {
            ensureInitializedLocked();
        }
    }

    private static void ensureInitializedLocked() throws CertificateException, KeyStoreException, NoSuchAlgorithmException {
        if ($assertionsDisabled || Thread.holdsLock(sLock)) {
            if (sCertificateFactory == null) {
                sCertificateFactory = CertificateFactory.getInstance("X.509");
            }
            if (sDefaultTrustManager == null) {
                sDefaultTrustManager = createTrustManager(null);
            }
            if (!sLoadedSystemKeyStore) {
                try {
                    sSystemKeyStore = KeyStore.getInstance("AndroidCAStore");
                    try {
                        sSystemKeyStore.load(null);
                    } catch (IOException e) {
                    }
                    sSystemCertificateDirectory = new File(System.getenv("ANDROID_ROOT") + "/etc/security/cacerts");
                } catch (KeyStoreException e2) {
                }
                if (!sDisableNativeCodeForTest && VERSION.SDK_INT >= 17) {
                    RecordHistogram.recordBooleanHistogram("Net.FoundSystemTrustRootsAndroid", sSystemKeyStore != null ? true : $assertionsDisabled);
                }
                sLoadedSystemKeyStore = true;
            }
            if (sSystemTrustAnchorCache == null) {
                sSystemTrustAnchorCache = new HashSet();
            }
            if (sTestKeyStore == null) {
                sTestKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                try {
                    sTestKeyStore.load(null);
                } catch (IOException e3) {
                }
            }
            if (sTestTrustManager == null) {
                sTestTrustManager = createTrustManager(sTestKeyStore);
            }
            if (!sDisableNativeCodeForTest && sTrustStorageListener == null) {
                sTrustStorageListener = new TrustStorageListener();
                IntentFilter filter = new IntentFilter();
                if (VERSION.SDK_INT >= 26) {
                    filter.addAction("android.security.action.KEYCHAIN_CHANGED");
                    filter.addAction("android.security.action.KEY_ACCESS_CHANGED");
                    filter.addAction("android.security.action.TRUST_STORE_CHANGED");
                } else {
                    filter.addAction("android.security.STORAGE_CHANGED");
                }
                ContextUtils.getApplicationContext().registerReceiver(sTrustStorageListener, filter);
                return;
            }
            return;
        }
        throw new AssertionError();
    }

    private static X509TrustManagerImplementation createTrustManager(KeyStore keyStore) throws KeyStoreException, NoSuchAlgorithmException {
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(keyStore);
        TrustManager[] trustManagers = tmf.getTrustManagers();
        int length = trustManagers.length;
        int i = 0;
        while (i < length) {
            TrustManager tm = trustManagers[i];
            if (tm instanceof X509TrustManager) {
                try {
                    if (VERSION.SDK_INT >= 17) {
                        return new X509TrustManagerJellyBean((X509TrustManager) tm);
                    }
                    return new X509TrustManagerIceCreamSandwich((X509TrustManager) tm);
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "Error creating trust manager (" + tm.getClass().getName() + "): " + e);
                }
            } else {
                i++;
            }
        }
        Log.e(TAG, "Could not find suitable trust manager");
        return null;
    }

    private static void reloadTestTrustManager() throws KeyStoreException, NoSuchAlgorithmException {
        if ($assertionsDisabled || Thread.holdsLock(sLock)) {
            sTestTrustManager = createTrustManager(sTestKeyStore);
            return;
        }
        throw new AssertionError();
    }

    private static void reloadDefaultTrustManager() throws KeyStoreException, NoSuchAlgorithmException, CertificateException {
        synchronized (sLock) {
            sDefaultTrustManager = null;
            sSystemTrustAnchorCache = null;
            ensureInitializedLocked();
        }
        nativeNotifyKeyChainChanged();
    }

    public static X509Certificate createCertificateFromBytes(byte[] derBytes) throws CertificateException, KeyStoreException, NoSuchAlgorithmException {
        ensureInitialized();
        return (X509Certificate) sCertificateFactory.generateCertificate(new ByteArrayInputStream(derBytes));
    }

    public static void addTestRootCertificate(byte[] rootCertBytes) throws CertificateException, KeyStoreException, NoSuchAlgorithmException {
        ensureInitialized();
        X509Certificate rootCert = createCertificateFromBytes(rootCertBytes);
        synchronized (sLock) {
            sTestKeyStore.setCertificateEntry("root_cert_" + Integer.toString(sTestKeyStore.size()), rootCert);
            reloadTestTrustManager();
        }
    }

    public static void clearTestRootCertificates() throws NoSuchAlgorithmException, CertificateException, KeyStoreException {
        ensureInitialized();
        synchronized (sLock) {
            try {
                sTestKeyStore.load(null);
                reloadTestTrustManager();
            } catch (IOException e) {
            }
        }
    }

    private static String hashPrincipal(X500Principal principal) throws NoSuchAlgorithmException {
        byte[] digest = MessageDigest.getInstance("MD5").digest(principal.getEncoded());
        char[] hexChars = new char[8];
        for (int i = 0; i < 4; i++) {
            hexChars[i * 2] = HEX_DIGITS[(digest[3 - i] >> 4) & 15];
            hexChars[(i * 2) + 1] = HEX_DIGITS[digest[3 - i] & 15];
        }
        return new String(hexChars);
    }

    private static boolean isKnownRoot(X509Certificate root) throws NoSuchAlgorithmException, KeyStoreException {
        if (!$assertionsDisabled && !Thread.holdsLock(sLock)) {
            throw new AssertionError();
        } else if (sSystemKeyStore == null) {
            return $assertionsDisabled;
        } else {
            Pair<X500Principal, PublicKey> key = new Pair(root.getSubjectX500Principal(), root.getPublicKey());
            if (sSystemTrustAnchorCache.contains(key)) {
                return true;
            }
            String hash = hashPrincipal(root.getSubjectX500Principal());
            int i = 0;
            while (true) {
                String alias = hash + '.' + i;
                if (!new File(sSystemCertificateDirectory, alias).exists()) {
                    return $assertionsDisabled;
                }
                Certificate anchor = sSystemKeyStore.getCertificate("system:" + alias);
                if (anchor != null) {
                    if (anchor instanceof X509Certificate) {
                        X509Certificate anchorX509 = (X509Certificate) anchor;
                        if (root.getSubjectX500Principal().equals(anchorX509.getSubjectX500Principal()) && root.getPublicKey().equals(anchorX509.getPublicKey())) {
                            sSystemTrustAnchorCache.add(key);
                            return true;
                        }
                    }
                    Log.e(TAG, "Anchor " + alias + " not an X509Certificate: " + anchor.getClass().getName());
                }
                i++;
            }
        }
    }

    static boolean verifyKeyUsage(X509Certificate certificate) throws CertificateException {
        try {
            List<String> ekuOids = certificate.getExtendedKeyUsage();
            if (ekuOids == null) {
                return true;
            }
            for (String ekuOid : ekuOids) {
                if (ekuOid.equals(OID_TLS_SERVER_AUTH) || ekuOid.equals(OID_ANY_EKU) || ekuOid.equals(OID_SERVER_GATED_NETSCAPE)) {
                    return true;
                }
                if (ekuOid.equals(OID_SERVER_GATED_MICROSOFT)) {
                    return true;
                }
            }
            return $assertionsDisabled;
        } catch (NullPointerException e) {
            return $assertionsDisabled;
        }
    }

    public static AndroidCertVerifyResult verifyServerCertificates(byte[][] certChain, String authType, String host) throws KeyStoreException, NoSuchAlgorithmException {
        if (certChain == null || certChain.length == 0 || certChain[0] == null) {
            throw new IllegalArgumentException("Expected non-null and non-empty certificate chain passed as |certChain|. |certChain|=" + Arrays.deepToString(certChain));
        }
        try {
            ensureInitialized();
            List<X509Certificate> serverCertificatesList = new ArrayList();
            try {
                serverCertificatesList.add(createCertificateFromBytes(certChain[0]));
                for (int i = 1; i < certChain.length; i++) {
                    try {
                        serverCertificatesList.add(createCertificateFromBytes(certChain[i]));
                    } catch (CertificateException e) {
                        Log.w(TAG, "intermediate " + i + " failed parsing");
                    }
                }
                X509Certificate[] serverCertificates = (X509Certificate[]) serverCertificatesList.toArray(new X509Certificate[serverCertificatesList.size()]);
                try {
                    serverCertificates[0].checkValidity();
                    if (!verifyKeyUsage(serverCertificates[0])) {
                        return new AndroidCertVerifyResult(-6);
                    }
                    synchronized (sLock) {
                        AndroidCertVerifyResult androidCertVerifyResult;
                        if (sDefaultTrustManager == null) {
                            androidCertVerifyResult = new AndroidCertVerifyResult(-1);
                            return androidCertVerifyResult;
                        }
                        List<X509Certificate> verifiedChain;
                        try {
                            verifiedChain = sDefaultTrustManager.checkServerTrusted(serverCertificates, authType, host);
                        } catch (CertificateException eDefaultManager) {
                            try {
                                verifiedChain = sTestTrustManager.checkServerTrusted(serverCertificates, authType, host);
                            } catch (CertificateException e2) {
                                Log.i(TAG, "Failed to validate the certificate chain, error: " + eDefaultManager.getMessage());
                                return new AndroidCertVerifyResult(-2);
                            }
                        }
                        boolean isIssuedByKnownRoot = $assertionsDisabled;
                        if (verifiedChain.size() > 0) {
                            isIssuedByKnownRoot = isKnownRoot((X509Certificate) verifiedChain.get(verifiedChain.size() - 1));
                        }
                        androidCertVerifyResult = new AndroidCertVerifyResult(0, isIssuedByKnownRoot, verifiedChain);
                        return androidCertVerifyResult;
                    }
                } catch (CertificateExpiredException e3) {
                    return new AndroidCertVerifyResult(-3);
                } catch (CertificateNotYetValidException e4) {
                    return new AndroidCertVerifyResult(-4);
                } catch (CertificateException e5) {
                    return new AndroidCertVerifyResult(-1);
                }
            } catch (CertificateException e6) {
                return new AndroidCertVerifyResult(-5);
            }
        } catch (CertificateException e7) {
            return new AndroidCertVerifyResult(-1);
        }
    }

    public static void setDisableNativeCodeForTest(boolean disabled) {
        sDisableNativeCodeForTest = disabled;
    }
}
