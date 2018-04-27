package org.chromium.base.process_launcher;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import org.chromium.base.Log;
import org.chromium.base.ObserverList;
import org.chromium.base.VisibleForTesting;
import org.chromium.base.process_launcher.ChildProcessConnection.ServiceCallback;

public class ChildConnectionAllocator {
    static final /* synthetic */ boolean $assertionsDisabled = (!ChildConnectionAllocator.class.desiredAssertionStatus() ? true : $assertionsDisabled);
    private static final long FREE_CONNECTION_DELAY_MILLIS = 1;
    private static final String TAG = "ChildConnAllocator";
    private final boolean mBindAsExternalService;
    private final boolean mBindToCaller;
    private final ChildProcessConnection[] mChildProcessConnections;
    private ConnectionFactory mConnectionFactory = new ConnectionFactoryImpl();
    private final ArrayList<Integer> mFreeConnectionIndices;
    private final Handler mLauncherHandler;
    private final ObserverList<Listener> mListeners = new ObserverList();
    private final String mPackageName;
    private final String mServiceClassName;
    private final boolean mUseStrongBinding;

    @VisibleForTesting
    public interface ConnectionFactory {
        ChildProcessConnection createConnection(Context context, ComponentName componentName, boolean z, boolean z2, Bundle bundle);
    }

    private static class ConnectionFactoryImpl implements ConnectionFactory {
        private ConnectionFactoryImpl() {
        }

        public ChildProcessConnection createConnection(Context context, ComponentName serviceName, boolean bindToCaller, boolean bindAsExternalService, Bundle serviceBundle) {
            return new ChildProcessConnection(context, serviceName, bindToCaller, bindAsExternalService, serviceBundle);
        }
    }

    public static abstract class Listener {
        public void onConnectionAllocated(ChildConnectionAllocator allocator, ChildProcessConnection connection) {
        }

        public void onConnectionFreed(ChildConnectionAllocator allocator, ChildProcessConnection connection) {
        }
    }

    public static ChildConnectionAllocator create(Context context, Handler launcherHandler, String packageName, String serviceClassNameManifestKey, String numChildServicesManifestKey, boolean bindToCaller, boolean bindAsExternalService, boolean useStrongBinding) {
        String serviceClassName = null;
        int numServices = -1;
        PackageManager packageManager = context.getPackageManager();
        try {
            ApplicationInfo appInfo = packageManager.getApplicationInfo(packageName, 128);
            if (appInfo.metaData != null) {
                serviceClassName = appInfo.metaData.getString(serviceClassNameManifestKey);
                numServices = appInfo.metaData.getInt(numChildServicesManifestKey, -1);
            }
            if (numServices < 0) {
                throw new RuntimeException("Illegal meta data value for number of child services");
            }
            try {
                packageManager.getServiceInfo(new ComponentName(packageName, serviceClassName + "0"), 0);
                return new ChildConnectionAllocator(launcherHandler, packageName, serviceClassName, bindToCaller, bindAsExternalService, useStrongBinding, numServices);
            } catch (NameNotFoundException e) {
                throw new RuntimeException("Illegal meta data value: the child service doesn't exist");
            }
        } catch (NameNotFoundException e2) {
            throw new RuntimeException("Could not get application info.");
        }
    }

    public static int getNumberOfServices(Context context, String packageName, String numChildServicesManifestKey) {
        int numServices = -1;
        try {
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(packageName, 128);
            if (appInfo.metaData != null) {
                numServices = appInfo.metaData.getInt(numChildServicesManifestKey, -1);
            }
            if (numServices >= 0) {
                return numServices;
            }
            throw new RuntimeException("Illegal meta data value for number of child services");
        } catch (NameNotFoundException e) {
            throw new RuntimeException("Could not get application info", e);
        }
    }

    @VisibleForTesting
    public static ChildConnectionAllocator createForTest(String packageName, String serviceClassName, int serviceCount, boolean bindToCaller, boolean bindAsExternalService, boolean useStrongBinding) {
        return new ChildConnectionAllocator(new Handler(), packageName, serviceClassName, bindToCaller, bindAsExternalService, useStrongBinding, serviceCount);
    }

    private ChildConnectionAllocator(Handler launcherHandler, String packageName, String serviceClassName, boolean bindToCaller, boolean bindAsExternalService, boolean useStrongBinding, int numChildServices) {
        this.mLauncherHandler = launcherHandler;
        if ($assertionsDisabled || isRunningOnLauncherThread()) {
            this.mPackageName = packageName;
            this.mServiceClassName = serviceClassName;
            this.mBindToCaller = bindToCaller;
            this.mBindAsExternalService = bindAsExternalService;
            this.mUseStrongBinding = useStrongBinding;
            this.mChildProcessConnections = new ChildProcessConnection[numChildServices];
            this.mFreeConnectionIndices = new ArrayList(numChildServices);
            for (int i = 0; i < numChildServices; i++) {
                this.mFreeConnectionIndices.add(Integer.valueOf(i));
            }
            return;
        }
        throw new AssertionError();
    }

    public ChildProcessConnection allocate(Context context, Bundle serviceBundle, final ServiceCallback serviceCallback) {
        if (!$assertionsDisabled && !isRunningOnLauncherThread()) {
            throw new AssertionError();
        } else if (this.mFreeConnectionIndices.isEmpty()) {
            Log.d(TAG, "Ran out of services to allocate.");
            return null;
        } else {
            int slot = ((Integer) this.mFreeConnectionIndices.remove(0)).intValue();
            if ($assertionsDisabled || this.mChildProcessConnections[slot] == null) {
                ComponentName serviceName = new ComponentName(this.mPackageName, this.mServiceClassName + slot);
                ServiceCallback serviceCallbackWrapper = new ServiceCallback() {
                    static final /* synthetic */ boolean $assertionsDisabled = (!ChildConnectionAllocator.class.desiredAssertionStatus() ? true : ChildConnectionAllocator.$assertionsDisabled);

                    public void onChildStarted() {
                        if (!$assertionsDisabled && !ChildConnectionAllocator.this.isRunningOnLauncherThread()) {
                            throw new AssertionError();
                        } else if (serviceCallback != null) {
                            ChildConnectionAllocator.this.mLauncherHandler.post(new Runnable() {
                                public void run() {
                                    serviceCallback.onChildStarted();
                                }
                            });
                        }
                    }

                    public void onChildStartFailed(final ChildProcessConnection connection) {
                        if ($assertionsDisabled || ChildConnectionAllocator.this.isRunningOnLauncherThread()) {
                            if (serviceCallback != null) {
                                ChildConnectionAllocator.this.mLauncherHandler.post(new Runnable() {
                                    public void run() {
                                        serviceCallback.onChildStartFailed(connection);
                                    }
                                });
                            }
                            freeConnectionWithDelay(connection);
                            return;
                        }
                        throw new AssertionError();
                    }

                    public void onChildProcessDied(final ChildProcessConnection connection) {
                        if ($assertionsDisabled || ChildConnectionAllocator.this.isRunningOnLauncherThread()) {
                            if (serviceCallback != null) {
                                ChildConnectionAllocator.this.mLauncherHandler.post(new Runnable() {
                                    public void run() {
                                        serviceCallback.onChildProcessDied(connection);
                                    }
                                });
                            }
                            freeConnectionWithDelay(connection);
                            return;
                        }
                        throw new AssertionError();
                    }

                    private void freeConnectionWithDelay(final ChildProcessConnection connection) {
                        ChildConnectionAllocator.this.mLauncherHandler.postDelayed(new Runnable() {
                            public void run() {
                                ChildConnectionAllocator.this.free(connection);
                            }
                        }, ChildConnectionAllocator.FREE_CONNECTION_DELAY_MILLIS);
                    }
                };
                ChildProcessConnection connection = this.mConnectionFactory.createConnection(context, serviceName, this.mBindToCaller, this.mBindAsExternalService, serviceBundle);
                this.mChildProcessConnections[slot] = connection;
                Iterator it = this.mListeners.iterator();
                while (it.hasNext()) {
                    ((Listener) it.next()).onConnectionAllocated(this, connection);
                }
                connection.start(this.mUseStrongBinding, serviceCallbackWrapper);
                Log.d(TAG, "Allocator allocated and bound a connection, name: %s, slot: %d", this.mServiceClassName, Integer.valueOf(slot));
                return connection;
            }
            throw new AssertionError();
        }
    }

    private void free(ChildProcessConnection connection) {
        if ($assertionsDisabled || isRunningOnLauncherThread()) {
            int slot = Arrays.asList(this.mChildProcessConnections).indexOf(connection);
            if (slot == -1) {
                Log.e(TAG, "Unable to find connection to free.", new Object[0]);
                if (!$assertionsDisabled) {
                    throw new AssertionError();
                }
            }
            this.mChildProcessConnections[slot] = null;
            if ($assertionsDisabled || !this.mFreeConnectionIndices.contains(Integer.valueOf(slot))) {
                this.mFreeConnectionIndices.add(Integer.valueOf(slot));
                Log.d(TAG, "Allocator freed a connection, name: %s, slot: %d", this.mServiceClassName, Integer.valueOf(slot));
            } else {
                throw new AssertionError();
            }
            Iterator it = this.mListeners.iterator();
            while (it.hasNext()) {
                ((Listener) it.next()).onConnectionFreed(this, connection);
            }
            return;
        }
        throw new AssertionError();
    }

    public String getPackageName() {
        return this.mPackageName;
    }

    public boolean anyConnectionAllocated() {
        return this.mFreeConnectionIndices.size() < this.mChildProcessConnections.length ? true : $assertionsDisabled;
    }

    public boolean isFreeConnectionAvailable() {
        if ($assertionsDisabled || isRunningOnLauncherThread()) {
            return !this.mFreeConnectionIndices.isEmpty() ? true : $assertionsDisabled;
        } else {
            throw new AssertionError();
        }
    }

    public int getNumberOfServices() {
        return this.mChildProcessConnections.length;
    }

    public void addListener(Listener listener) {
        if ($assertionsDisabled || !this.mListeners.hasObserver(listener)) {
            this.mListeners.addObserver(listener);
            return;
        }
        throw new AssertionError();
    }

    public void removeListener(Listener listener) {
        boolean removed = this.mListeners.removeObserver(listener);
        if (!$assertionsDisabled && !removed) {
            throw new AssertionError();
        }
    }

    public boolean isConnectionFromAllocator(ChildProcessConnection connection) {
        for (ChildProcessConnection existingConnection : this.mChildProcessConnections) {
            if (existingConnection == connection) {
                return true;
            }
        }
        return $assertionsDisabled;
    }

    @VisibleForTesting
    public void setConnectionFactoryForTesting(ConnectionFactory connectionFactory) {
        this.mConnectionFactory = connectionFactory;
    }

    @VisibleForTesting
    public int allocatedConnectionsCountForTesting() {
        if ($assertionsDisabled || isRunningOnLauncherThread()) {
            return this.mChildProcessConnections.length - this.mFreeConnectionIndices.size();
        }
        throw new AssertionError();
    }

    @VisibleForTesting
    public ChildProcessConnection getChildProcessConnectionAtSlotForTesting(int slotNumber) {
        return this.mChildProcessConnections[slotNumber];
    }

    private boolean isRunningOnLauncherThread() {
        return this.mLauncherHandler.getLooper() == Looper.myLooper() ? true : $assertionsDisabled;
    }
}
