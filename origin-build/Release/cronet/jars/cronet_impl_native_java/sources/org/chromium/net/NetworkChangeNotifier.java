package org.chromium.net;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.net.ConnectivityManager;
import android.os.Build.VERSION;
import java.util.ArrayList;
import java.util.Iterator;
import org.chromium.base.ContextUtils;
import org.chromium.base.ObserverList;
import org.chromium.base.VisibleForTesting;
import org.chromium.base.annotations.CalledByNative;
import org.chromium.base.annotations.JNINamespace;
import org.chromium.base.annotations.NativeClassQualifiedName;
import org.chromium.net.NetworkChangeNotifierAutoDetect.NetworkState;
import org.chromium.net.NetworkChangeNotifierAutoDetect.Observer;
import org.chromium.net.NetworkChangeNotifierAutoDetect.RegistrationPolicy;

@JNINamespace("net")
public class NetworkChangeNotifier {
    static final /* synthetic */ boolean $assertionsDisabled = (!NetworkChangeNotifier.class.desiredAssertionStatus());
    @SuppressLint({"StaticFieldLeak"})
    private static NetworkChangeNotifier sInstance;
    private NetworkChangeNotifierAutoDetect mAutoDetector;
    private final ObserverList<ConnectionTypeObserver> mConnectionTypeObservers = new ObserverList();
    private final ConnectivityManager mConnectivityManager = ((ConnectivityManager) ContextUtils.getApplicationContext().getSystemService("connectivity"));
    private int mCurrentConnectionType = 0;
    private final ArrayList<Long> mNativeChangeNotifiers = new ArrayList();

    public interface ConnectionTypeObserver {
        void onConnectionTypeChanged(int i);
    }

    @NativeClassQualifiedName("NetworkChangeNotifierDelegateAndroid")
    private native void nativeNotifyConnectionTypeChanged(long j, int i, long j2);

    @NativeClassQualifiedName("NetworkChangeNotifierDelegateAndroid")
    private native void nativeNotifyMaxBandwidthChanged(long j, int i);

    @NativeClassQualifiedName("NetworkChangeNotifierDelegateAndroid")
    private native void nativeNotifyOfNetworkConnect(long j, long j2, int i);

    @NativeClassQualifiedName("NetworkChangeNotifierDelegateAndroid")
    private native void nativeNotifyOfNetworkDisconnect(long j, long j2);

    @NativeClassQualifiedName("NetworkChangeNotifierDelegateAndroid")
    private native void nativeNotifyOfNetworkSoonToDisconnect(long j, long j2);

    @NativeClassQualifiedName("NetworkChangeNotifierDelegateAndroid")
    private native void nativeNotifyPurgeActiveNetworkList(long j, long[] jArr);

    @VisibleForTesting
    protected NetworkChangeNotifier() {
    }

    @CalledByNative
    public static NetworkChangeNotifier init() {
        if (sInstance == null) {
            sInstance = new NetworkChangeNotifier();
        }
        return sInstance;
    }

    public static boolean isInitialized() {
        return sInstance != null;
    }

    static void resetInstanceForTests(NetworkChangeNotifier notifier) {
        sInstance = notifier;
    }

    @CalledByNative
    public int getCurrentConnectionType() {
        return this.mCurrentConnectionType;
    }

    @CalledByNative
    public int getCurrentConnectionSubtype() {
        if (this.mAutoDetector == null) {
            return 0;
        }
        return this.mAutoDetector.getCurrentNetworkState().getConnectionSubtype();
    }

    @CalledByNative
    public long getCurrentDefaultNetId() {
        return this.mAutoDetector == null ? -1 : this.mAutoDetector.getDefaultNetId();
    }

    @CalledByNative
    public long[] getCurrentNetworksAndTypes() {
        return this.mAutoDetector == null ? new long[0] : this.mAutoDetector.getNetworksAndTypes();
    }

    @CalledByNative
    public void addNativeObserver(long nativeChangeNotifier) {
        this.mNativeChangeNotifiers.add(Long.valueOf(nativeChangeNotifier));
    }

    @CalledByNative
    public void removeNativeObserver(long nativeChangeNotifier) {
        this.mNativeChangeNotifiers.remove(Long.valueOf(nativeChangeNotifier));
    }

    @CalledByNative
    public boolean registerNetworkCallbackFailed() {
        return this.mAutoDetector == null ? false : this.mAutoDetector.registerNetworkCallbackFailed();
    }

    public static NetworkChangeNotifier getInstance() {
        if ($assertionsDisabled || sInstance != null) {
            return sInstance;
        }
        throw new AssertionError();
    }

    public static void setAutoDetectConnectivityState(boolean shouldAutoDetect) {
        getInstance().setAutoDetectConnectivityStateInternal(shouldAutoDetect, new RegistrationPolicyApplicationStatus());
    }

    public static void registerToReceiveNotificationsAlways() {
        getInstance().setAutoDetectConnectivityStateInternal(true, new RegistrationPolicyAlwaysRegister());
    }

    public static void setAutoDetectConnectivityState(RegistrationPolicy policy) {
        getInstance().setAutoDetectConnectivityStateInternal(true, policy);
    }

    private void destroyAutoDetector() {
        if (this.mAutoDetector != null) {
            this.mAutoDetector.destroy();
            this.mAutoDetector = null;
        }
    }

    private void setAutoDetectConnectivityStateInternal(boolean shouldAutoDetect, RegistrationPolicy policy) {
        if (!shouldAutoDetect) {
            destroyAutoDetector();
        } else if (this.mAutoDetector == null) {
            this.mAutoDetector = new NetworkChangeNotifierAutoDetect(new Observer() {
                public void onConnectionTypeChanged(int newConnectionType) {
                    NetworkChangeNotifier.this.updateCurrentConnectionType(newConnectionType);
                }

                public void onConnectionSubtypeChanged(int newConnectionSubtype) {
                    NetworkChangeNotifier.this.notifyObserversOfConnectionSubtypeChange(newConnectionSubtype);
                }

                public void onNetworkConnect(long netId, int connectionType) {
                    NetworkChangeNotifier.this.notifyObserversOfNetworkConnect(netId, connectionType);
                }

                public void onNetworkSoonToDisconnect(long netId) {
                    NetworkChangeNotifier.this.notifyObserversOfNetworkSoonToDisconnect(netId);
                }

                public void onNetworkDisconnect(long netId) {
                    NetworkChangeNotifier.this.notifyObserversOfNetworkDisconnect(netId);
                }

                public void purgeActiveNetworkList(long[] activeNetIds) {
                    NetworkChangeNotifier.this.notifyObserversToPurgeActiveNetworkList(activeNetIds);
                }
            }, policy);
            NetworkState networkState = this.mAutoDetector.getCurrentNetworkState();
            updateCurrentConnectionType(networkState.getConnectionType());
            notifyObserversOfConnectionSubtypeChange(networkState.getConnectionSubtype());
        }
    }

    @CalledByNative
    public static void forceConnectivityState(boolean networkAvailable) {
        setAutoDetectConnectivityState(false);
        getInstance().forceConnectivityStateInternal(networkAvailable);
    }

    private void forceConnectivityStateInternal(boolean forceOnline) {
        boolean connectionCurrentlyExists;
        int i = 6;
        int i2 = 0;
        if (this.mCurrentConnectionType != 6) {
            connectionCurrentlyExists = true;
        } else {
            connectionCurrentlyExists = false;
        }
        if (connectionCurrentlyExists != forceOnline) {
            if (forceOnline) {
                i = 0;
            }
            updateCurrentConnectionType(i);
            if (!forceOnline) {
                i2 = 1;
            }
            notifyObserversOfConnectionSubtypeChange(i2);
        }
    }

    @CalledByNative
    public static void fakeNetworkConnected(long netId, int connectionType) {
        setAutoDetectConnectivityState(false);
        getInstance().notifyObserversOfNetworkConnect(netId, connectionType);
    }

    @CalledByNative
    public static void fakeNetworkSoonToBeDisconnected(long netId) {
        setAutoDetectConnectivityState(false);
        getInstance().notifyObserversOfNetworkSoonToDisconnect(netId);
    }

    @CalledByNative
    public static void fakeNetworkDisconnected(long netId) {
        setAutoDetectConnectivityState(false);
        getInstance().notifyObserversOfNetworkDisconnect(netId);
    }

    @CalledByNative
    public static void fakePurgeActiveNetworkList(long[] activeNetIds) {
        setAutoDetectConnectivityState(false);
        getInstance().notifyObserversToPurgeActiveNetworkList(activeNetIds);
    }

    @CalledByNative
    public static void fakeDefaultNetwork(long netId, int connectionType) {
        setAutoDetectConnectivityState(false);
        getInstance().notifyObserversOfConnectionTypeChange(connectionType, netId);
    }

    @CalledByNative
    public static void fakeConnectionSubtypeChanged(int connectionSubtype) {
        setAutoDetectConnectivityState(false);
        getInstance().notifyObserversOfConnectionSubtypeChange(connectionSubtype);
    }

    private void updateCurrentConnectionType(int newConnectionType) {
        this.mCurrentConnectionType = newConnectionType;
        notifyObserversOfConnectionTypeChange(newConnectionType);
    }

    void notifyObserversOfConnectionTypeChange(int newConnectionType) {
        notifyObserversOfConnectionTypeChange(newConnectionType, getCurrentDefaultNetId());
    }

    private void notifyObserversOfConnectionTypeChange(int newConnectionType, long defaultNetId) {
        Iterator it = this.mNativeChangeNotifiers.iterator();
        while (it.hasNext()) {
            nativeNotifyConnectionTypeChanged(((Long) it.next()).longValue(), newConnectionType, defaultNetId);
        }
        Iterator it2 = this.mConnectionTypeObservers.iterator();
        while (it2.hasNext()) {
            ((ConnectionTypeObserver) it2.next()).onConnectionTypeChanged(newConnectionType);
        }
    }

    void notifyObserversOfConnectionSubtypeChange(int connectionSubtype) {
        Iterator it = this.mNativeChangeNotifiers.iterator();
        while (it.hasNext()) {
            nativeNotifyMaxBandwidthChanged(((Long) it.next()).longValue(), connectionSubtype);
        }
    }

    void notifyObserversOfNetworkConnect(long netId, int connectionType) {
        Iterator it = this.mNativeChangeNotifiers.iterator();
        while (it.hasNext()) {
            nativeNotifyOfNetworkConnect(((Long) it.next()).longValue(), netId, connectionType);
        }
    }

    void notifyObserversOfNetworkSoonToDisconnect(long netId) {
        Iterator it = this.mNativeChangeNotifiers.iterator();
        while (it.hasNext()) {
            nativeNotifyOfNetworkSoonToDisconnect(((Long) it.next()).longValue(), netId);
        }
    }

    void notifyObserversOfNetworkDisconnect(long netId) {
        Iterator it = this.mNativeChangeNotifiers.iterator();
        while (it.hasNext()) {
            nativeNotifyOfNetworkDisconnect(((Long) it.next()).longValue(), netId);
        }
    }

    void notifyObserversToPurgeActiveNetworkList(long[] activeNetIds) {
        Iterator it = this.mNativeChangeNotifiers.iterator();
        while (it.hasNext()) {
            nativeNotifyPurgeActiveNetworkList(((Long) it.next()).longValue(), activeNetIds);
        }
    }

    public static void addConnectionTypeObserver(ConnectionTypeObserver observer) {
        getInstance().addConnectionTypeObserverInternal(observer);
    }

    private void addConnectionTypeObserverInternal(ConnectionTypeObserver observer) {
        this.mConnectionTypeObservers.addObserver(observer);
    }

    public static void removeConnectionTypeObserver(ConnectionTypeObserver observer) {
        getInstance().removeConnectionTypeObserverInternal(observer);
    }

    private void removeConnectionTypeObserverInternal(ConnectionTypeObserver observer) {
        this.mConnectionTypeObservers.removeObserver(observer);
    }

    @TargetApi(23)
    private boolean isProcessBoundToNetworkInternal() {
        if (VERSION.SDK_INT < 21) {
            return false;
        }
        if (VERSION.SDK_INT < 23) {
            if (ConnectivityManager.getProcessDefaultNetwork() == null) {
                return false;
            }
            return true;
        } else if (this.mConnectivityManager.getBoundNetworkForProcess() == null) {
            return false;
        } else {
            return true;
        }
    }

    @CalledByNative
    public static boolean isProcessBoundToNetwork() {
        return getInstance().isProcessBoundToNetworkInternal();
    }

    public static NetworkChangeNotifierAutoDetect getAutoDetectorForTest() {
        return getInstance().mAutoDetector;
    }

    public static boolean isOnline() {
        return getInstance().getCurrentConnectionType() != 6;
    }
}
