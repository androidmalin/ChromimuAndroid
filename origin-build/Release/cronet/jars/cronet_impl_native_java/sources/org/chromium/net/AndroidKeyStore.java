package org.chromium.net;

import java.lang.reflect.Method;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import org.chromium.base.Log;
import org.chromium.base.annotations.CalledByNative;
import org.chromium.base.annotations.JNINamespace;

@JNINamespace("net::android")
public class AndroidKeyStore {
    private static final String TAG = "AndroidKeyStore";

    @CalledByNative
    private static byte[] signWithPrivateKey(PrivateKey privateKey, String algorithm, byte[] message) {
        byte[] bArr = null;
        try {
            Signature signature = Signature.getInstance(algorithm);
            try {
                signature.initSign(privateKey);
                signature.update(message);
                bArr = signature.sign();
            } catch (Exception e) {
                Log.e(TAG, "Exception while signing message with " + algorithm + " and " + privateKey.getAlgorithm() + " private key (" + privateKey.getClass().getName() + "): " + e, new Object[0]);
            }
        } catch (NoSuchAlgorithmException e2) {
            Log.e(TAG, "Signature algorithm " + algorithm + " not supported: " + e2, new Object[0]);
        }
        return bArr;
    }

    private static Object getOpenSSLKeyForPrivateKey(PrivateKey privateKey) {
        Method getKey;
        if (privateKey == null) {
            Log.e(TAG, "privateKey == null", new Object[0]);
            return null;
        } else if (privateKey instanceof RSAPrivateKey) {
            try {
                Class<?> superClass = Class.forName("org.apache.harmony.xnet.provider.jsse.OpenSSLRSAPrivateKey");
                if (superClass.isInstance(privateKey)) {
                    try {
                        getKey = superClass.getDeclaredMethod("getOpenSSLKey", new Class[0]);
                        getKey.setAccessible(true);
                        Object opensslKey = getKey.invoke(privateKey, new Object[0]);
                        getKey.setAccessible(false);
                        if (opensslKey != null) {
                            return opensslKey;
                        }
                        Log.e(TAG, "getOpenSSLKey() returned null", new Object[0]);
                        return null;
                    } catch (Exception e) {
                        Log.e(TAG, "Exception while trying to retrieve system EVP_PKEY handle: " + e, new Object[0]);
                        return null;
                    } catch (Throwable th) {
                        getKey.setAccessible(false);
                    }
                }
                Log.e(TAG, "Private key is not an OpenSSLRSAPrivateKey instance, its class name is:" + privateKey.getClass().getCanonicalName(), new Object[0]);
                return null;
            } catch (Exception e2) {
                Log.e(TAG, "Cannot find system OpenSSLRSAPrivateKey class: " + e2, new Object[0]);
                return null;
            }
        } else {
            Log.e(TAG, "does not implement RSAPrivateKey", new Object[0]);
            return null;
        }
    }

    @CalledByNative
    private static long getOpenSSLHandleForPrivateKey(PrivateKey privateKey) {
        Object opensslKey = getOpenSSLKeyForPrivateKey(privateKey);
        if (opensslKey == null) {
            return 0;
        }
        try {
            Method getPkeyContext = opensslKey.getClass().getDeclaredMethod("getPkeyContext", new Class[0]);
            try {
                getPkeyContext.setAccessible(true);
                long evp_pkey = ((Number) getPkeyContext.invoke(opensslKey, new Object[0])).longValue();
                getPkeyContext.setAccessible(false);
                if (evp_pkey != 0) {
                    return evp_pkey;
                }
                Log.e(TAG, "getPkeyContext() returned null", new Object[0]);
                return evp_pkey;
            } catch (Exception e) {
                Log.e(TAG, "Exception while trying to retrieve system EVP_PKEY handle: " + e, new Object[0]);
                return 0;
            } catch (Throwable th) {
                getPkeyContext.setAccessible(false);
            }
        } catch (Exception e2) {
            Log.e(TAG, "No getPkeyContext() method on OpenSSLKey member:" + e2, new Object[0]);
            return 0;
        }
    }

    @CalledByNative
    private static Object getOpenSSLEngineForPrivateKey(PrivateKey privateKey) {
        try {
            Class<?> engineClass = Class.forName("org.apache.harmony.xnet.provider.jsse.OpenSSLEngine");
            Object opensslKey = getOpenSSLKeyForPrivateKey(privateKey);
            if (opensslKey == null) {
                return null;
            }
            try {
                Method getEngine = opensslKey.getClass().getDeclaredMethod("getEngine", new Class[0]);
                try {
                    getEngine.setAccessible(true);
                    Object engine = getEngine.invoke(opensslKey, new Object[0]);
                    getEngine.setAccessible(false);
                    if (engine == null) {
                        Log.e(TAG, "getEngine() returned null", new Object[0]);
                    }
                    if (engineClass.isInstance(engine)) {
                        return engine;
                    }
                    Log.e(TAG, "Engine is not an OpenSSLEngine instance, its class name is:" + engine.getClass().getCanonicalName(), new Object[0]);
                    return null;
                } catch (Exception e) {
                    Log.e(TAG, "Exception while trying to retrieve OpenSSLEngine object: " + e, new Object[0]);
                    return null;
                } catch (Throwable th) {
                    getEngine.setAccessible(false);
                }
            } catch (Exception e2) {
                Log.e(TAG, "No getEngine() method on OpenSSLKey member:" + e2, new Object[0]);
                return null;
            }
        } catch (Exception e22) {
            Log.e(TAG, "Cannot find system OpenSSLEngine class: " + e22, new Object[0]);
            return null;
        }
    }
}
