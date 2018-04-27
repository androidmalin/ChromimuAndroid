package org.chromium.net;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.NetworkRequest;
import android.net.NetworkRequest.Builder;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build.VERSION;
import android.os.Handler;
import android.os.Looper;
import java.io.IOException;
import java.util.Arrays;
import javax.annotation.concurrent.GuardedBy;
import org.chromium.base.ApplicationStatus;
import org.chromium.base.ContextUtils;
import org.chromium.base.VisibleForTesting;

@SuppressLint({"NewApi"})
public class NetworkChangeNotifierAutoDetect extends BroadcastReceiver {
    private static final String TAG = NetworkChangeNotifierAutoDetect.class.getSimpleName();
    private static final int UNKNOWN_LINK_SPEED = -1;
    private ConnectivityManagerDelegate mConnectivityManagerDelegate;
    private final Handler mHandler = new Handler(this.mLooper);
    private boolean mIgnoreNextBroadcast;
    private final NetworkConnectivityIntentFilter mIntentFilter;
    private final Looper mLooper = Looper.myLooper();
    private MyNetworkCallback mNetworkCallback;
    private NetworkRequest mNetworkRequest;
    private NetworkState mNetworkState;
    private final Observer mObserver;
    private boolean mRegisterNetworkCallbackFailed;
    private boolean mRegistered;
    private final RegistrationPolicy mRegistrationPolicy;
    private boolean mShouldSignalObserver;
    private WifiManagerDelegate mWifiManagerDelegate;

    public interface Observer {
        void onConnectionSubtypeChanged(int i);

        void onConnectionTypeChanged(int i);

        void onNetworkConnect(long j, int i);

        void onNetworkDisconnect(long j);

        void onNetworkSoonToDisconnect(long j);

        void purgeActiveNetworkList(long[] jArr);
    }

    static class ConnectivityManagerDelegate {
        static final /* synthetic */ boolean $assertionsDisabled = (!NetworkChangeNotifierAutoDetect.class.desiredAssertionStatus());
        private final ConnectivityManager mConnectivityManager;

        ConnectivityManagerDelegate(Context context) {
            this.mConnectivityManager = (ConnectivityManager) context.getSystemService("connectivity");
        }

        ConnectivityManagerDelegate() {
            this.mConnectivityManager = null;
        }

        @TargetApi(21)
        private NetworkInfo getActiveNetworkInfo() {
            NetworkInfo networkInfo = this.mConnectivityManager.getActiveNetworkInfo();
            if (networkInfo == null) {
                return null;
            }
            if (networkInfo.isConnected()) {
                return networkInfo;
            }
            if (VERSION.SDK_INT < 21) {
                return null;
            }
            if (networkInfo.getDetailedState() != DetailedState.BLOCKED) {
                return null;
            }
            if (ApplicationStatus.getStateForApplication() != 1) {
                return null;
            }
            return networkInfo;
        }

        NetworkState getNetworkState(WifiManagerDelegate wifiManagerDelegate) {
            NetworkInfo networkInfo = getActiveNetworkInfo();
            if (networkInfo == null) {
                return new NetworkState(false, -1, -1, null);
            }
            if (networkInfo.getType() != 1) {
                return new NetworkState(true, networkInfo.getType(), networkInfo.getSubtype(), null);
            }
            if (networkInfo.getExtraInfo() == null || "".equals(networkInfo.getExtraInfo())) {
                return new NetworkState(true, networkInfo.getType(), networkInfo.getSubtype(), wifiManagerDelegate.getWifiSsid());
            }
            return new NetworkState(true, networkInfo.getType(), networkInfo.getSubtype(), networkInfo.getExtraInfo());
        }

        private NetworkInfo getNetworkInfo(Network network) {
            try {
                return this.mConnectivityManager.getNetworkInfo(network);
            } catch (NullPointerException e) {
                try {
                    return this.mConnectivityManager.getNetworkInfo(network);
                } catch (NullPointerException e2) {
                    return null;
                }
            }
        }

        @TargetApi(21)
        int getConnectionType(Network network) {
            NetworkInfo networkInfo = getNetworkInfo(network);
            if (networkInfo != null && networkInfo.getType() == 17) {
                networkInfo = this.mConnectivityManager.getActiveNetworkInfo();
            }
            if (networkInfo == null || !networkInfo.isConnected()) {
                return 6;
            }
            return NetworkChangeNotifierAutoDetect.convertToConnectionType(networkInfo.getType(), networkInfo.getSubtype());
        }

        @TargetApi(21)
        @VisibleForTesting
        protected Network[] getAllNetworksUnfiltered() {
            Network[] networks = this.mConnectivityManager.getAllNetworks();
            return networks == null ? new Network[0] : networks;
        }

        @TargetApi(21)
        @VisibleForTesting
        protected boolean vpnAccessible(Network network) {
            try {
                network.getSocketFactory().createSocket().close();
                return true;
            } catch (IOException e) {
                return false;
            }
        }

        @TargetApi(21)
        @VisibleForTesting
        protected NetworkCapabilities getNetworkCapabilities(Network network) {
            return this.mConnectivityManager.getNetworkCapabilities(network);
        }

        @TargetApi(21)
        void registerNetworkCallback(NetworkRequest networkRequest, NetworkCallback networkCallback) {
            this.mConnectivityManager.registerNetworkCallback(networkRequest, networkCallback);
        }

        @TargetApi(21)
        void unregisterNetworkCallback(NetworkCallback networkCallback) {
            this.mConnectivityManager.unregisterNetworkCallback(networkCallback);
        }

        @TargetApi(21)
        long getDefaultNetId() {
            NetworkInfo defaultNetworkInfo = this.mConnectivityManager.getActiveNetworkInfo();
            if (defaultNetworkInfo == null) {
                return -1;
            }
            long defaultNetId = -1;
            for (Network network : NetworkChangeNotifierAutoDetect.getAllNetworksFiltered(this, null)) {
                NetworkInfo networkInfo = getNetworkInfo(network);
                if (networkInfo != null && (networkInfo.getType() == defaultNetworkInfo.getType() || networkInfo.getType() == 17)) {
                    if ($assertionsDisabled || defaultNetId == -1) {
                        defaultNetId = NetworkChangeNotifierAutoDetect.networkToNetId(network);
                    } else {
                        throw new AssertionError();
                    }
                }
            }
            return defaultNetId;
        }
    }

    @TargetApi(21)
    private class MyNetworkCallback extends NetworkCallback {
        static final /* synthetic */ boolean $assertionsDisabled = (!NetworkChangeNotifierAutoDetect.class.desiredAssertionStatus());
        private Network mVpnInPlace;

        private MyNetworkCallback() {
        }

        void initializeVpnInPlace() {
            Network[] networks = NetworkChangeNotifierAutoDetect.getAllNetworksFiltered(NetworkChangeNotifierAutoDetect.this.mConnectivityManagerDelegate, null);
            this.mVpnInPlace = null;
            if (networks.length == 1) {
                NetworkCapabilities capabilities = NetworkChangeNotifierAutoDetect.this.mConnectivityManagerDelegate.getNetworkCapabilities(networks[0]);
                if (capabilities != null && capabilities.hasTransport(4)) {
                    this.mVpnInPlace = networks[0];
                }
            }
        }

        private boolean ignoreNetworkDueToVpn(Network network) {
            return (this.mVpnInPlace == null || this.mVpnInPlace.equals(network)) ? false : true;
        }

        private boolean ignoreConnectedInaccessibleVpn(Network network, NetworkCapabilities capabilities) {
            if (capabilities == null) {
                capabilities = NetworkChangeNotifierAutoDetect.this.mConnectivityManagerDelegate.getNetworkCapabilities(network);
            }
            return capabilities == null || (capabilities.hasTransport(4) && !NetworkChangeNotifierAutoDetect.this.mConnectivityManagerDelegate.vpnAccessible(network));
        }

        private boolean ignoreConnectedNetwork(Network network, NetworkCapabilities capabilities) {
            return ignoreNetworkDueToVpn(network) || ignoreConnectedInaccessibleVpn(network, capabilities);
        }

        public void onAvailable(Network network) {
            NetworkCapabilities capabilities = NetworkChangeNotifierAutoDetect.this.mConnectivityManagerDelegate.getNetworkCapabilities(network);
            if (!ignoreConnectedNetwork(network, capabilities)) {
                final boolean makeVpnDefault = capabilities.hasTransport(4);
                if (makeVpnDefault) {
                    this.mVpnInPlace = network;
                }
                final long netId = NetworkChangeNotifierAutoDetect.networkToNetId(network);
                final int connectionType = NetworkChangeNotifierAutoDetect.this.mConnectivityManagerDelegate.getConnectionType(network);
                NetworkChangeNotifierAutoDetect.this.runOnThread(new Runnable() {
                    public void run() {
                        NetworkChangeNotifierAutoDetect.this.mObserver.onNetworkConnect(netId, connectionType);
                        if (makeVpnDefault) {
                            NetworkChangeNotifierAutoDetect.this.mObserver.onConnectionTypeChanged(connectionType);
                            NetworkChangeNotifierAutoDetect.this.mObserver.purgeActiveNetworkList(new long[]{netId});
                        }
                    }
                });
            }
        }

        public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
            if (!ignoreConnectedNetwork(network, networkCapabilities)) {
                final long netId = NetworkChangeNotifierAutoDetect.networkToNetId(network);
                final int connectionType = NetworkChangeNotifierAutoDetect.this.mConnectivityManagerDelegate.getConnectionType(network);
                NetworkChangeNotifierAutoDetect.this.runOnThread(new Runnable() {
                    public void run() {
                        NetworkChangeNotifierAutoDetect.this.mObserver.onNetworkConnect(netId, connectionType);
                    }
                });
            }
        }

        public void onLosing(Network network, int maxMsToLive) {
            if (!ignoreConnectedNetwork(network, null)) {
                final long netId = NetworkChangeNotifierAutoDetect.networkToNetId(network);
                NetworkChangeNotifierAutoDetect.this.runOnThread(new Runnable() {
                    public void run() {
                        NetworkChangeNotifierAutoDetect.this.mObserver.onNetworkSoonToDisconnect(netId);
                    }
                });
            }
        }

        public void onLost(final Network network) {
            if (!ignoreNetworkDueToVpn(network)) {
                NetworkChangeNotifierAutoDetect.this.runOnThread(new Runnable() {
                    public void run() {
                        NetworkChangeNotifierAutoDetect.this.mObserver.onNetworkDisconnect(NetworkChangeNotifierAutoDetect.networkToNetId(network));
                    }
                });
                if (this.mVpnInPlace == null) {
                    return;
                }
                if ($assertionsDisabled || network.equals(this.mVpnInPlace)) {
                    this.mVpnInPlace = null;
                    for (Network newNetwork : NetworkChangeNotifierAutoDetect.getAllNetworksFiltered(NetworkChangeNotifierAutoDetect.this.mConnectivityManagerDelegate, network)) {
                        onAvailable(newNetwork);
                    }
                    final int newConnectionType = NetworkChangeNotifierAutoDetect.this.getCurrentNetworkState().getConnectionType();
                    NetworkChangeNotifierAutoDetect.this.runOnThread(new Runnable() {
                        public void run() {
                            NetworkChangeNotifierAutoDetect.this.mObserver.onConnectionTypeChanged(newConnectionType);
                        }
                    });
                    return;
                }
                throw new AssertionError();
            }
        }
    }

    @SuppressLint({"NewApi", "ParcelCreator"})
    private static class NetworkConnectivityIntentFilter extends IntentFilter {
        NetworkConnectivityIntentFilter() {
            addAction("android.net.conn.CONNECTIVITY_CHANGE");
        }
    }

    public static class NetworkState {
        static final /* synthetic */ boolean $assertionsDisabled = (!NetworkChangeNotifierAutoDetect.class.desiredAssertionStatus());
        private final boolean mConnected;
        private final int mSubtype;
        private final int mType;
        private final String mWifiSsid;

        public NetworkState(boolean connected, int type, int subtype, String wifiSsid) {
            this.mConnected = connected;
            this.mType = type;
            this.mSubtype = subtype;
            if ($assertionsDisabled || this.mType == 1 || wifiSsid == null) {
                if (wifiSsid == null) {
                    wifiSsid = "";
                }
                this.mWifiSsid = wifiSsid;
                return;
            }
            throw new AssertionError();
        }

        public boolean isConnected() {
            return this.mConnected;
        }

        public int getNetworkType() {
            return this.mType;
        }

        public int getNetworkSubType() {
            return this.mSubtype;
        }

        public String getWifiSsid() {
            return this.mWifiSsid;
        }

        public int getConnectionType() {
            if (isConnected()) {
                return NetworkChangeNotifierAutoDetect.convertToConnectionType(getNetworkType(), getNetworkSubType());
            }
            return 6;
        }

        public int getConnectionSubtype() {
            if (!isConnected()) {
                return 1;
            }
            switch (getNetworkType()) {
                case 0:
                    switch (getNetworkSubType()) {
                        case 1:
                            return 7;
                        case 2:
                            return 8;
                        case 3:
                            return 9;
                        case 4:
                            return 5;
                        case 5:
                            return 10;
                        case 6:
                            return 11;
                        case 7:
                            return 6;
                        case 8:
                            return 14;
                        case 9:
                            return 15;
                        case 10:
                            return 12;
                        case 11:
                            return 4;
                        case 12:
                            return 13;
                        case 13:
                            return 18;
                        case 14:
                            return 16;
                        case ConnectionSubtype.SUBTYPE_HSUPA /*15*/:
                            return 17;
                        default:
                            return 0;
                    }
                case 1:
                case 6:
                case 7:
                case 9:
                    return 0;
                default:
                    return 0;
            }
        }
    }

    public static abstract class RegistrationPolicy {
        static final /* synthetic */ boolean $assertionsDisabled = (!NetworkChangeNotifierAutoDetect.class.desiredAssertionStatus());
        private NetworkChangeNotifierAutoDetect mNotifier;

        protected abstract void destroy();

        protected final void register() {
            if ($assertionsDisabled || this.mNotifier != null) {
                this.mNotifier.register();
                return;
            }
            throw new AssertionError();
        }

        protected final void unregister() {
            if ($assertionsDisabled || this.mNotifier != null) {
                this.mNotifier.unregister();
                return;
            }
            throw new AssertionError();
        }

        protected void init(NetworkChangeNotifierAutoDetect notifier) {
            this.mNotifier = notifier;
        }
    }

    static class WifiManagerDelegate {
        private final Context mContext;
        @GuardedBy("mLock")
        private boolean mHasWifiPermission;
        @GuardedBy("mLock")
        private boolean mHasWifiPermissionComputed;
        private final Object mLock;
        @GuardedBy("mLock")
        private WifiManager mWifiManager;

        WifiManagerDelegate(Context context) {
            this.mLock = new Object();
            this.mContext = context;
        }

        WifiManagerDelegate() {
            this.mLock = new Object();
            this.mContext = null;
        }

        @GuardedBy("mLock")
        @SuppressLint({"WifiManagerPotentialLeak"})
        private boolean hasPermissionLocked() {
            if (this.mHasWifiPermissionComputed) {
                return this.mHasWifiPermission;
            }
            WifiManager wifiManager;
            this.mHasWifiPermission = this.mContext.getPackageManager().checkPermission("android.permission.ACCESS_WIFI_STATE", this.mContext.getPackageName()) == 0;
            if (this.mHasWifiPermission) {
                wifiManager = (WifiManager) this.mContext.getSystemService("wifi");
            } else {
                wifiManager = null;
            }
            this.mWifiManager = wifiManager;
            this.mHasWifiPermissionComputed = true;
            return this.mHasWifiPermission;
        }

        String getWifiSsid() {
            synchronized (this.mLock) {
                if (hasPermissionLocked()) {
                    WifiInfo wifiInfo = getWifiInfoLocked();
                    String ssid;
                    if (wifiInfo != null) {
                        ssid = wifiInfo.getSSID();
                        return ssid;
                    }
                    ssid = "";
                    return ssid;
                }
                return AndroidNetworkLibrary.getWifiSSID();
            }
        }

        @GuardedBy("mLock")
        private WifiInfo getWifiInfoLocked() {
            try {
                return this.mWifiManager.getConnectionInfo();
            } catch (NullPointerException e) {
                try {
                    return this.mWifiManager.getConnectionInfo();
                } catch (NullPointerException e2) {
                    return null;
                }
            }
        }
    }

    @TargetApi(21)
    public NetworkChangeNotifierAutoDetect(Observer observer, RegistrationPolicy policy) {
        this.mObserver = observer;
        this.mConnectivityManagerDelegate = new ConnectivityManagerDelegate(ContextUtils.getApplicationContext());
        this.mWifiManagerDelegate = new WifiManagerDelegate(ContextUtils.getApplicationContext());
        if (VERSION.SDK_INT >= 21) {
            this.mNetworkCallback = new MyNetworkCallback();
            this.mNetworkRequest = new Builder().addCapability(12).removeCapability(15).build();
        } else {
            this.mNetworkCallback = null;
            this.mNetworkRequest = null;
        }
        this.mNetworkState = getCurrentNetworkState();
        this.mIntentFilter = new NetworkConnectivityIntentFilter();
        this.mIgnoreNextBroadcast = false;
        this.mShouldSignalObserver = false;
        this.mRegistrationPolicy = policy;
        this.mRegistrationPolicy.init(this);
        this.mShouldSignalObserver = true;
    }

    private boolean onThread() {
        return this.mLooper == Looper.myLooper();
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

    void setConnectivityManagerDelegateForTests(ConnectivityManagerDelegate delegate) {
        this.mConnectivityManagerDelegate = delegate;
    }

    void setWifiManagerDelegateForTests(WifiManagerDelegate delegate) {
        this.mWifiManagerDelegate = delegate;
    }

    @VisibleForTesting
    RegistrationPolicy getRegistrationPolicy() {
        return this.mRegistrationPolicy;
    }

    @VisibleForTesting
    boolean isReceiverRegisteredForTesting() {
        return this.mRegistered;
    }

    public void destroy() {
        assertOnThread();
        this.mRegistrationPolicy.destroy();
        unregister();
    }

    public void register() {
        assertOnThread();
        if (!this.mRegistered) {
            if (this.mShouldSignalObserver) {
                connectionTypeChanged();
            }
            this.mIgnoreNextBroadcast = ContextUtils.getApplicationContext().registerReceiver(this, this.mIntentFilter) != null;
            this.mRegistered = true;
            if (this.mNetworkCallback != null) {
                this.mNetworkCallback.initializeVpnInPlace();
                try {
                    this.mConnectivityManagerDelegate.registerNetworkCallback(this.mNetworkRequest, this.mNetworkCallback);
                } catch (IllegalArgumentException e) {
                    this.mRegisterNetworkCallbackFailed = true;
                    this.mNetworkCallback = null;
                }
                if (!this.mRegisterNetworkCallbackFailed && this.mShouldSignalObserver) {
                    Network[] networks = getAllNetworksFiltered(this.mConnectivityManagerDelegate, null);
                    long[] netIds = new long[networks.length];
                    for (int i = 0; i < networks.length; i++) {
                        netIds[i] = networkToNetId(networks[i]);
                    }
                    this.mObserver.purgeActiveNetworkList(netIds);
                }
            }
        }
    }

    public void unregister() {
        assertOnThread();
        if (this.mRegistered) {
            ContextUtils.getApplicationContext().unregisterReceiver(this);
            this.mRegistered = false;
            if (this.mNetworkCallback != null) {
                this.mConnectivityManagerDelegate.unregisterNetworkCallback(this.mNetworkCallback);
            }
        }
    }

    public NetworkState getCurrentNetworkState() {
        return this.mConnectivityManagerDelegate.getNetworkState(this.mWifiManagerDelegate);
    }

    @TargetApi(21)
    private static Network[] getAllNetworksFiltered(ConnectivityManagerDelegate connectivityManagerDelegate, Network ignoreNetwork) {
        Network[] networks = connectivityManagerDelegate.getAllNetworksUnfiltered();
        int length = networks.length;
        int i = 0;
        int filteredIndex = 0;
        while (i < length) {
            int filteredIndex2;
            Network network = networks[i];
            if (network.equals(ignoreNetwork)) {
                filteredIndex2 = filteredIndex;
            } else {
                NetworkCapabilities capabilities = connectivityManagerDelegate.getNetworkCapabilities(network);
                if (capabilities != null) {
                    if (!capabilities.hasCapability(12)) {
                        filteredIndex2 = filteredIndex;
                    } else if (!capabilities.hasTransport(4)) {
                        filteredIndex2 = filteredIndex + 1;
                        networks[filteredIndex] = network;
                    } else if (connectivityManagerDelegate.vpnAccessible(network)) {
                        return new Network[]{network};
                    }
                }
                filteredIndex2 = filteredIndex;
            }
            i++;
            filteredIndex = filteredIndex2;
        }
        return (Network[]) Arrays.copyOf(networks, filteredIndex);
    }

    public long[] getNetworksAndTypes() {
        int i = 0;
        if (VERSION.SDK_INT < 21) {
            return new long[0];
        }
        Network[] networks = getAllNetworksFiltered(this.mConnectivityManagerDelegate, null);
        long[] networksAndTypes = new long[(networks.length * 2)];
        int length = networks.length;
        int index = 0;
        while (i < length) {
            Network network = networks[i];
            int i2 = index + 1;
            networksAndTypes[index] = networkToNetId(network);
            index = i2 + 1;
            networksAndTypes[i2] = (long) this.mConnectivityManagerDelegate.getConnectionType(network);
            i++;
        }
        return networksAndTypes;
    }

    public long getDefaultNetId() {
        if (VERSION.SDK_INT < 21) {
            return -1;
        }
        return this.mConnectivityManagerDelegate.getDefaultNetId();
    }

    public boolean registerNetworkCallbackFailed() {
        return this.mRegisterNetworkCallbackFailed;
    }

    private static int convertToConnectionType(int type, int subtype) {
        switch (type) {
            case 0:
                switch (subtype) {
                    case 1:
                    case 2:
                    case 4:
                    case 7:
                    case 11:
                        return 3;
                    case 3:
                    case 5:
                    case 6:
                    case 8:
                    case 9:
                    case 10:
                    case 12:
                    case 14:
                    case ConnectionSubtype.SUBTYPE_HSUPA /*15*/:
                        return 4;
                    case 13:
                        return 5;
                    default:
                        return 0;
                }
            case 1:
                return 2;
            case 6:
                return 5;
            case 7:
                return 7;
            case 9:
                return 1;
            default:
                return 0;
        }
    }

    public void onReceive(Context context, Intent intent) {
        runOnThread(new Runnable() {
            public void run() {
                if (!NetworkChangeNotifierAutoDetect.this.mRegistered) {
                    return;
                }
                if (NetworkChangeNotifierAutoDetect.this.mIgnoreNextBroadcast) {
                    NetworkChangeNotifierAutoDetect.this.mIgnoreNextBroadcast = false;
                } else {
                    NetworkChangeNotifierAutoDetect.this.connectionTypeChanged();
                }
            }
        });
    }

    private void connectionTypeChanged() {
        NetworkState networkState = getCurrentNetworkState();
        if (!(networkState.getConnectionType() == this.mNetworkState.getConnectionType() && networkState.getWifiSsid().equals(this.mNetworkState.getWifiSsid()))) {
            this.mObserver.onConnectionTypeChanged(networkState.getConnectionType());
        }
        if (!(networkState.getConnectionType() == this.mNetworkState.getConnectionType() && networkState.getConnectionSubtype() == this.mNetworkState.getConnectionSubtype())) {
            this.mObserver.onConnectionSubtypeChanged(networkState.getConnectionSubtype());
        }
        this.mNetworkState = networkState;
    }

    @TargetApi(21)
    @VisibleForTesting
    static long networkToNetId(Network network) {
        if (VERSION.SDK_INT >= 23) {
            return network.getNetworkHandle();
        }
        return (long) Integer.parseInt(network.toString());
    }
}
