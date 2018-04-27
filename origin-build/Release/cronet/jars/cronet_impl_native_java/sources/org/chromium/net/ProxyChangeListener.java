package org.chromium.net;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.chromium.base.ContextUtils;
import org.chromium.base.annotations.CalledByNative;
import org.chromium.base.annotations.JNINamespace;
import org.chromium.base.annotations.NativeClassQualifiedName;

@JNINamespace("net")
public class ProxyChangeListener {
    static final /* synthetic */ boolean $assertionsDisabled = (!ProxyChangeListener.class.desiredAssertionStatus() ? true : $assertionsDisabled);
    private static final String TAG = "ProxyChangeListener";
    private static boolean sEnabled = true;
    private Delegate mDelegate;
    private final Handler mHandler = new Handler(this.mLooper);
    private final Looper mLooper = Looper.myLooper();
    private long mNativePtr;
    private ProxyReceiver mProxyReceiver;

    public interface Delegate {
        void proxySettingsChanged();
    }

    private static class ProxyConfig {
        public final String[] mExclusionList;
        public final String mHost;
        public final String mPacUrl;
        public final int mPort;

        public ProxyConfig(String host, int port, String pacUrl, String[] exclusionList) {
            this.mHost = host;
            this.mPort = port;
            this.mPacUrl = pacUrl;
            this.mExclusionList = exclusionList;
        }
    }

    private class ProxyReceiver extends BroadcastReceiver {
        private ProxyReceiver() {
        }

        public void onReceive(Context context, final Intent intent) {
            if (intent.getAction().equals("android.intent.action.PROXY_CHANGE")) {
                ProxyChangeListener.this.runOnThread(new Runnable() {
                    public void run() {
                        ProxyChangeListener.this.proxySettingsChanged(ProxyReceiver.this, ProxyReceiver.this.extractNewProxy(intent));
                    }
                });
            }
        }

        private ProxyConfig extractNewProxy(Intent intent) {
            try {
                String className;
                String proxyInfo;
                String getHostName = "getHost";
                String getPortName = "getPort";
                String getPacFileUrl = "getPacFileUrl";
                String getExclusionList = "getExclusionList";
                if (VERSION.SDK_INT < 21) {
                    className = "android.net.ProxyProperties";
                    proxyInfo = "proxy";
                } else {
                    className = "android.net.ProxyInfo";
                    proxyInfo = "android.intent.extra.PROXY_INFO";
                }
                Object props = intent.getExtras().get(proxyInfo);
                if (props == null) {
                    return null;
                }
                String[] exclusionList;
                Class<?> cls = Class.forName(className);
                Method getHostMethod = cls.getDeclaredMethod("getHost", new Class[0]);
                Method getPortMethod = cls.getDeclaredMethod("getPort", new Class[0]);
                Method getExclusionListMethod = cls.getDeclaredMethod("getExclusionList", new Class[0]);
                String host = (String) getHostMethod.invoke(props, new Object[0]);
                int port = ((Integer) getPortMethod.invoke(props, new Object[0])).intValue();
                if (VERSION.SDK_INT < 21) {
                    exclusionList = ((String) getExclusionListMethod.invoke(props, new Object[0])).split(",");
                } else {
                    exclusionList = (String[]) getExclusionListMethod.invoke(props, new Object[0]);
                }
                if (VERSION.SDK_INT >= 19 && VERSION.SDK_INT < 21) {
                    String pacFileUrl = (String) cls.getDeclaredMethod("getPacFileUrl", new Class[0]).invoke(props, new Object[0]);
                    if (!TextUtils.isEmpty(pacFileUrl)) {
                        return new ProxyConfig(host, port, pacFileUrl, exclusionList);
                    }
                } else if (VERSION.SDK_INT >= 21) {
                    Uri pacFileUrl2 = (Uri) cls.getDeclaredMethod("getPacFileUrl", new Class[0]).invoke(props, new Object[0]);
                    if (!Uri.EMPTY.equals(pacFileUrl2)) {
                        return new ProxyConfig(host, port, pacFileUrl2.toString(), exclusionList);
                    }
                }
                return new ProxyConfig(host, port, null, exclusionList);
            } catch (ClassNotFoundException ex) {
                Log.e(ProxyChangeListener.TAG, "Using no proxy configuration due to exception:" + ex);
                return null;
            } catch (NoSuchMethodException ex2) {
                Log.e(ProxyChangeListener.TAG, "Using no proxy configuration due to exception:" + ex2);
                return null;
            } catch (IllegalAccessException ex3) {
                Log.e(ProxyChangeListener.TAG, "Using no proxy configuration due to exception:" + ex3);
                return null;
            } catch (InvocationTargetException ex4) {
                Log.e(ProxyChangeListener.TAG, "Using no proxy configuration due to exception:" + ex4);
                return null;
            } catch (NullPointerException ex5) {
                Log.e(ProxyChangeListener.TAG, "Using no proxy configuration due to exception:" + ex5);
                return null;
            }
        }
    }

    @NativeClassQualifiedName("ProxyConfigServiceAndroid::JNIDelegate")
    private native void nativeProxySettingsChanged(long j);

    @NativeClassQualifiedName("ProxyConfigServiceAndroid::JNIDelegate")
    private native void nativeProxySettingsChangedTo(long j, String str, int i, String str2, String[] strArr);

    private ProxyChangeListener() {
    }

    public static void setEnabled(boolean enabled) {
        sEnabled = enabled;
    }

    public void setDelegateForTesting(Delegate delegate) {
        this.mDelegate = delegate;
    }

    @CalledByNative
    public static ProxyChangeListener create() {
        return new ProxyChangeListener();
    }

    @CalledByNative
    public static String getProperty(String property) {
        return System.getProperty(property);
    }

    @CalledByNative
    public void start(long nativePtr) {
        assertOnThread();
        if ($assertionsDisabled || this.mNativePtr == 0) {
            this.mNativePtr = nativePtr;
            registerReceiver();
            return;
        }
        throw new AssertionError();
    }

    @CalledByNative
    public void stop() {
        assertOnThread();
        this.mNativePtr = 0;
        unregisterReceiver();
    }

    private void proxySettingsChanged(ProxyReceiver proxyReceiver, ProxyConfig cfg) {
        if (sEnabled && proxyReceiver == this.mProxyReceiver) {
            if (this.mDelegate != null) {
                this.mDelegate.proxySettingsChanged();
            }
            if (this.mNativePtr == 0) {
                return;
            }
            if (cfg != null) {
                nativeProxySettingsChangedTo(this.mNativePtr, cfg.mHost, cfg.mPort, cfg.mPacUrl, cfg.mExclusionList);
            } else {
                nativeProxySettingsChanged(this.mNativePtr);
            }
        }
    }

    private void registerReceiver() {
        if (this.mProxyReceiver == null) {
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.PROXY_CHANGE");
            this.mProxyReceiver = new ProxyReceiver();
            ContextUtils.getApplicationContext().registerReceiver(this.mProxyReceiver, filter);
        }
    }

    private void unregisterReceiver() {
        if (this.mProxyReceiver != null) {
            ContextUtils.getApplicationContext().unregisterReceiver(this.mProxyReceiver);
            this.mProxyReceiver = null;
        }
    }

    private boolean onThread() {
        return this.mLooper == Looper.myLooper() ? true : $assertionsDisabled;
    }

    private void assertOnThread() {
    }

    private void runOnThread(Runnable r) {
        if (onThread()) {
            r.run();
        } else {
            this.mHandler.post(r);
        }
    }
}
