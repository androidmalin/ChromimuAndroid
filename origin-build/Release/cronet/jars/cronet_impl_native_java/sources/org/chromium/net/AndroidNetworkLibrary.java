package org.chromium.net;

import android.annotation.TargetApi;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.net.wifi.WifiInfo;
import android.os.Build.VERSION;
import android.os.ParcelFileDescriptor;
import android.security.KeyChain;
import android.security.NetworkSecurityPolicy;
import android.telephony.TelephonyManager;
import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketImpl;
import java.net.URLConnection;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Enumeration;
import java.util.List;
import org.chromium.base.ContextUtils;
import org.chromium.base.annotations.CalledByNative;
import org.chromium.base.annotations.CalledByNativeUnchecked;

class AndroidNetworkLibrary {
    private static final String TAG = "AndroidNetworkLibrary";

    private static class SocketFd extends Socket {

        private static class SocketImplFd extends SocketImpl {
            private final ParcelFileDescriptor mPfd;

            SocketImplFd(int fd) {
                this.mPfd = ParcelFileDescriptor.adoptFd(fd);
                this.fd = this.mPfd.getFileDescriptor();
            }

            protected void accept(SocketImpl s) {
                throw new RuntimeException("accept not implemented");
            }

            protected int available() {
                throw new RuntimeException("accept not implemented");
            }

            protected void bind(InetAddress host, int port) {
                throw new RuntimeException("accept not implemented");
            }

            protected void close() {
                this.mPfd.detachFd();
            }

            protected void connect(InetAddress address, int port) {
                throw new RuntimeException("connect not implemented");
            }

            protected void connect(SocketAddress address, int timeout) {
                throw new RuntimeException("connect not implemented");
            }

            protected void connect(String host, int port) {
                throw new RuntimeException("connect not implemented");
            }

            protected void create(boolean stream) {
                throw new RuntimeException("create not implemented");
            }

            protected InputStream getInputStream() {
                throw new RuntimeException("getInputStream not implemented");
            }

            protected OutputStream getOutputStream() {
                throw new RuntimeException("getOutputStream not implemented");
            }

            protected void listen(int backlog) {
                throw new RuntimeException("listen not implemented");
            }

            protected void sendUrgentData(int data) {
                throw new RuntimeException("sendUrgentData not implemented");
            }

            public Object getOption(int optID) {
                throw new RuntimeException("getOption not implemented");
            }

            public void setOption(int optID, Object value) {
                throw new RuntimeException("setOption not implemented");
            }
        }

        SocketFd(int fd) throws IOException {
            super(new SocketImplFd(fd));
        }
    }

    private static class ThreadStatsUid {
        private static final Method sClearThreadStatsUid;
        private static final Method sSetThreadStatsUid;

        private ThreadStatsUid() {
        }

        static {
            Exception e;
            try {
                sSetThreadStatsUid = TrafficStats.class.getMethod("setThreadStatsUid", new Class[]{Integer.TYPE});
                sClearThreadStatsUid = TrafficStats.class.getMethod("clearThreadStatsUid", new Class[0]);
            } catch (NoSuchMethodException e2) {
                e = e2;
                throw new RuntimeException("Unable to get TrafficStats methods", e);
            } catch (SecurityException e3) {
                e = e3;
                throw new RuntimeException("Unable to get TrafficStats methods", e);
            }
        }

        public static void set(int uid) throws IllegalAccessException, InvocationTargetException {
            sSetThreadStatsUid.invoke(null, new Object[]{Integer.valueOf(uid)});
        }

        public static void clear() throws IllegalAccessException, InvocationTargetException {
            sClearThreadStatsUid.invoke(null, new Object[0]);
        }
    }

    AndroidNetworkLibrary() {
    }

    @CalledByNative
    public static boolean storeKeyPair(byte[] publicKey, byte[] privateKey) {
        try {
            Intent intent = KeyChain.createInstallIntent();
            intent.putExtra("PKEY", privateKey);
            intent.putExtra("KEY", publicKey);
            intent.addFlags(268435456);
            ContextUtils.getApplicationContext().startActivity(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            Log.w(TAG, "could not store key pair: " + e);
            return false;
        }
    }

    @CalledByNative
    public static String getMimeTypeFromExtension(String extension) {
        return URLConnection.guessContentTypeFromName("foo." + extension);
    }

    @CalledByNative
    public static boolean haveOnlyLoopbackAddresses() {
        try {
            Enumeration<NetworkInterface> list = NetworkInterface.getNetworkInterfaces();
            if (list == null) {
                return false;
            }
            while (list.hasMoreElements()) {
                NetworkInterface netIf = (NetworkInterface) list.nextElement();
                try {
                    if (netIf.isUp() && !netIf.isLoopback()) {
                        return false;
                    }
                } catch (SocketException e) {
                }
            }
            return true;
        } catch (Exception e2) {
            Log.w(TAG, "could not get network interfaces: " + e2);
            return false;
        }
    }

    @CalledByNative
    public static AndroidCertVerifyResult verifyServerCertificates(byte[][] certChain, String authType, String host) {
        try {
            return X509Util.verifyServerCertificates(certChain, authType, host);
        } catch (KeyStoreException e) {
            return new AndroidCertVerifyResult(-1);
        } catch (NoSuchAlgorithmException e2) {
            return new AndroidCertVerifyResult(-1);
        } catch (IllegalArgumentException e3) {
            return new AndroidCertVerifyResult(-1);
        }
    }

    @CalledByNativeUnchecked
    public static void addTestRootCertificate(byte[] rootCert) throws CertificateException, KeyStoreException, NoSuchAlgorithmException {
        X509Util.addTestRootCertificate(rootCert);
    }

    @CalledByNativeUnchecked
    public static void clearTestRootCertificates() throws NoSuchAlgorithmException, CertificateException, KeyStoreException {
        X509Util.clearTestRootCertificates();
    }

    @CalledByNative
    private static String getNetworkCountryIso() {
        TelephonyManager telephonyManager = (TelephonyManager) ContextUtils.getApplicationContext().getSystemService("phone");
        if (telephonyManager == null) {
            return "";
        }
        return telephonyManager.getNetworkCountryIso();
    }

    @CalledByNative
    private static String getNetworkOperator() {
        TelephonyManager telephonyManager = (TelephonyManager) ContextUtils.getApplicationContext().getSystemService("phone");
        if (telephonyManager == null) {
            return "";
        }
        return telephonyManager.getNetworkOperator();
    }

    @CalledByNative
    private static String getSimOperator() {
        TelephonyManager telephonyManager = (TelephonyManager) ContextUtils.getApplicationContext().getSystemService("phone");
        if (telephonyManager == null) {
            return "";
        }
        return telephonyManager.getSimOperator();
    }

    @CalledByNative
    private static boolean getIsRoaming() {
        NetworkInfo networkInfo = ((ConnectivityManager) ContextUtils.getApplicationContext().getSystemService("connectivity")).getActiveNetworkInfo();
        if (networkInfo == null) {
            return false;
        }
        return networkInfo.isRoaming();
    }

    @TargetApi(23)
    @CalledByNative
    private static boolean getIsCaptivePortal() {
        if (VERSION.SDK_INT < 23) {
            return false;
        }
        ConnectivityManager connectivityManager = (ConnectivityManager) ContextUtils.getApplicationContext().getSystemService("connectivity");
        if (connectivityManager == null) {
            return false;
        }
        Network network = connectivityManager.getActiveNetwork();
        if (network == null) {
            return false;
        }
        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
        if (capabilities == null || !capabilities.hasCapability(17)) {
            return false;
        }
        return true;
    }

    @CalledByNative
    public static String getWifiSSID() {
        Intent intent = ContextUtils.getApplicationContext().registerReceiver(null, new IntentFilter("android.net.wifi.STATE_CHANGE"));
        if (intent != null) {
            WifiInfo wifiInfo = (WifiInfo) intent.getParcelableExtra("wifiInfo");
            if (wifiInfo != null) {
                String ssid = wifiInfo.getSSID();
                if (ssid != null) {
                    return ssid;
                }
            }
        }
        return "";
    }

    @TargetApi(24)
    @CalledByNative
    private static boolean isCleartextPermitted(String host) {
        if (VERSION.SDK_INT < 23) {
            return true;
        }
        NetworkSecurityPolicy policy = NetworkSecurityPolicy.getInstance();
        if (VERSION.SDK_INT >= 24) {
            return policy.isCleartextTrafficPermitted(host);
        }
        return policy.isCleartextTrafficPermitted();
    }

    @TargetApi(23)
    @CalledByNative
    private static byte[][] getDnsServers() {
        ConnectivityManager connectivityManager = (ConnectivityManager) ContextUtils.getApplicationContext().getSystemService("connectivity");
        if (connectivityManager == null) {
            return (byte[][]) Array.newInstance(Byte.TYPE, new int[]{0, 0});
        }
        Network network = connectivityManager.getActiveNetwork();
        if (network == null) {
            return (byte[][]) Array.newInstance(Byte.TYPE, new int[]{0, 0});
        }
        LinkProperties linkProperties = connectivityManager.getLinkProperties(network);
        if (linkProperties == null) {
            return (byte[][]) Array.newInstance(Byte.TYPE, new int[]{0, 0});
        }
        List<InetAddress> dnsServersList = linkProperties.getDnsServers();
        byte[][] dnsServers = new byte[dnsServersList.size()][];
        for (int i = 0; i < dnsServersList.size(); i++) {
            dnsServers[i] = ((InetAddress) dnsServersList.get(i)).getAddress();
        }
        return dnsServers;
    }

    @CalledByNative
    private static void tagSocket(int fd, int uid, int tag) throws IOException, IllegalAccessException, InvocationTargetException {
        int oldTag = TrafficStats.getThreadStatsTag();
        if (tag != oldTag) {
            TrafficStats.setThreadStatsTag(tag);
        }
        if (uid != -1) {
            ThreadStatsUid.set(uid);
        }
        SocketFd s = new SocketFd(fd);
        TrafficStats.tagSocket(s);
        s.close();
        if (tag != oldTag) {
            TrafficStats.setThreadStatsTag(oldTag);
        }
        if (uid != -1) {
            ThreadStatsUid.clear();
        }
    }
}
