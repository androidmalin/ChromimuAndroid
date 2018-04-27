package org.chromium.base.process_launcher;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import java.util.List;
import javax.annotation.Nullable;
import org.chromium.base.Log;
import org.chromium.base.TraceEvent;
import org.chromium.base.VisibleForTesting;
import org.chromium.base.process_launcher.IChildProcessService.Stub;
import org.chromium.net.CellularSignalStrengthError;

public class ChildProcessConnection {
    static final /* synthetic */ boolean $assertionsDisabled = (!ChildProcessConnection.class.desiredAssertionStatus() ? true : $assertionsDisabled);
    private static final String TAG = "ChildProcessConn";
    private final boolean mBindToCaller;
    private ConnectionCallback mConnectionCallback;
    private ConnectionParams mConnectionParams;
    private boolean mDidOnServiceConnected;
    private final ChildServiceConnection mInitialBinding;
    private final Handler mLauncherHandler;
    private final ChildServiceConnection mModerateBinding;
    private int mModerateBindingCount;
    private int mPid;
    private IChildProcessService mService;
    private final Bundle mServiceBundle;
    private ServiceCallback mServiceCallback;
    private boolean mServiceConnectComplete;
    private boolean mServiceDisconnected;
    private final ComponentName mServiceName;
    private final ChildServiceConnection mStrongBinding;
    private int mStrongBindingCount;
    private boolean mUnbound;
    private final ChildServiceConnection mWaivedBinding;
    private boolean mWaivedBoundOnly;

    public interface ServiceCallback {
        void onChildProcessDied(ChildProcessConnection childProcessConnection);

        void onChildStartFailed(ChildProcessConnection childProcessConnection);

        void onChildStarted();
    }

    @VisibleForTesting
    protected interface ChildServiceConnectionFactory {
        ChildServiceConnection createConnection(Intent intent, int i, ChildServiceConnectionDelegate childServiceConnectionDelegate);
    }

    @VisibleForTesting
    protected interface ChildServiceConnectionDelegate {
        void onServiceConnected(IBinder iBinder);

        void onServiceDisconnected();
    }

    @VisibleForTesting
    protected interface ChildServiceConnection {
        boolean bind();

        boolean isBound();

        void unbind();
    }

    private static class ChildServiceConnectionImpl implements ChildServiceConnection, ServiceConnection {
        private final int mBindFlags;
        private final Intent mBindIntent;
        private boolean mBound;
        private final Context mContext;
        private final ChildServiceConnectionDelegate mDelegate;

        private ChildServiceConnectionImpl(Context context, Intent bindIntent, int bindFlags, ChildServiceConnectionDelegate delegate) {
            this.mContext = context;
            this.mBindIntent = bindIntent;
            this.mBindFlags = bindFlags;
            this.mDelegate = delegate;
        }

        public boolean bind() {
            if (!this.mBound) {
                try {
                    TraceEvent.begin("ChildProcessConnection.ChildServiceConnectionImpl.bind");
                    this.mBound = this.mContext.bindService(this.mBindIntent, this, this.mBindFlags);
                } finally {
                    TraceEvent.end("ChildProcessConnection.ChildServiceConnectionImpl.bind");
                }
            }
            return this.mBound;
        }

        public void unbind() {
            if (this.mBound) {
                this.mContext.unbindService(this);
                this.mBound = ChildProcessConnection.$assertionsDisabled;
            }
        }

        public boolean isBound() {
            return this.mBound;
        }

        public void onServiceConnected(ComponentName className, IBinder service) {
            this.mDelegate.onServiceConnected(service);
        }

        public void onServiceDisconnected(ComponentName className) {
            this.mDelegate.onServiceDisconnected();
        }
    }

    public interface ConnectionCallback {
        void onConnected(ChildProcessConnection childProcessConnection);
    }

    private static class ConnectionParams {
        final List<IBinder> mClientInterfaces;
        final Bundle mConnectionBundle;

        ConnectionParams(Bundle connectionBundle, List<IBinder> clientInterfaces) {
            this.mConnectionBundle = connectionBundle;
            this.mClientInterfaces = clientInterfaces;
        }
    }

    public ChildProcessConnection(Context context, ComponentName serviceName, boolean bindToCaller, boolean bindAsExternalService, Bundle serviceBundle) {
        this(context, serviceName, bindToCaller, bindAsExternalService, serviceBundle, null);
    }

    @VisibleForTesting
    public ChildProcessConnection(final Context context, ComponentName serviceName, boolean bindToCaller, boolean bindAsExternalService, Bundle serviceBundle, ChildServiceConnectionFactory connectionFactory) {
        this.mLauncherHandler = new Handler();
        if ($assertionsDisabled || isRunningOnLauncherThread()) {
            Bundle bundle;
            this.mServiceName = serviceName;
            if (serviceBundle != null) {
                bundle = serviceBundle;
            } else {
                bundle = new Bundle();
            }
            this.mServiceBundle = bundle;
            this.mServiceBundle.putBoolean(ChildProcessConstants.EXTRA_BIND_TO_CALLER, bindToCaller);
            this.mBindToCaller = bindToCaller;
            if (connectionFactory == null) {
                connectionFactory = new ChildServiceConnectionFactory() {
                    public ChildServiceConnection createConnection(Intent bindIntent, int bindFlags, ChildServiceConnectionDelegate delegate) {
                        return new ChildServiceConnectionImpl(context, bindIntent, bindFlags, delegate);
                    }
                };
            }
            ChildServiceConnectionDelegate delegate = new ChildServiceConnectionDelegate() {
                public void onServiceConnected(final IBinder service) {
                    ChildProcessConnection.this.mLauncherHandler.post(new Runnable() {
                        public void run() {
                            ChildProcessConnection.this.onServiceConnectedOnLauncherThread(service);
                        }
                    });
                }

                public void onServiceDisconnected() {
                    ChildProcessConnection.this.mLauncherHandler.post(new Runnable() {
                        public void run() {
                            ChildProcessConnection.this.onServiceDisconnectedOnLauncherThread();
                        }
                    });
                }
            };
            Intent intent = new Intent();
            intent.setComponent(serviceName);
            if (serviceBundle != null) {
                intent.putExtras(serviceBundle);
            }
            int defaultFlags = (bindAsExternalService ? CellularSignalStrengthError.ERROR_NOT_SUPPORTED : 0) | 1;
            this.mInitialBinding = connectionFactory.createConnection(intent, defaultFlags, delegate);
            this.mModerateBinding = connectionFactory.createConnection(intent, defaultFlags, delegate);
            this.mStrongBinding = connectionFactory.createConnection(intent, defaultFlags | 64, delegate);
            this.mWaivedBinding = connectionFactory.createConnection(intent, defaultFlags | 32, delegate);
            return;
        }
        throw new AssertionError();
    }

    public final IChildProcessService getService() {
        if ($assertionsDisabled || isRunningOnLauncherThread()) {
            return this.mService;
        }
        throw new AssertionError();
    }

    public final ComponentName getServiceName() {
        if ($assertionsDisabled || isRunningOnLauncherThread()) {
            return this.mServiceName;
        }
        throw new AssertionError();
    }

    public boolean isConnected() {
        return this.mService != null ? true : $assertionsDisabled;
    }

    public int getPid() {
        if ($assertionsDisabled || isRunningOnLauncherThread()) {
            return this.mPid;
        }
        throw new AssertionError();
    }

    public void start(boolean useStrongBinding, ServiceCallback serviceCallback) {
        try {
            TraceEvent.begin("ChildProcessConnection.start");
            if (!$assertionsDisabled && !isRunningOnLauncherThread()) {
                throw new AssertionError();
            } else if ($assertionsDisabled || this.mConnectionParams == null) {
                this.mServiceCallback = serviceCallback;
                if (!bind(useStrongBinding)) {
                    Log.e(TAG, "Failed to establish the service connection.", new Object[0]);
                    notifyChildProcessDied();
                }
                TraceEvent.end("ChildProcessConnection.start");
            } else {
                throw new AssertionError("setupConnection() called before start() in ChildProcessConnection.");
            }
        } catch (Throwable th) {
            TraceEvent.end("ChildProcessConnection.start");
        }
    }

    public void setupConnection(Bundle connectionBundle, @Nullable List<IBinder> clientInterfaces, ConnectionCallback connectionCallback) {
        if (!$assertionsDisabled && !isRunningOnLauncherThread()) {
            throw new AssertionError();
        } else if (!$assertionsDisabled && this.mConnectionParams != null) {
            throw new AssertionError();
        } else if (this.mServiceDisconnected) {
            Log.w(TAG, "Tried to setup a connection that already disconnected.", new Object[0]);
            connectionCallback.onConnected(null);
        } else {
            try {
                TraceEvent.begin("ChildProcessConnection.setupConnection");
                this.mConnectionCallback = connectionCallback;
                this.mConnectionParams = new ConnectionParams(connectionBundle, clientInterfaces);
                if (this.mServiceConnectComplete) {
                    doConnectionSetup();
                }
                TraceEvent.end("ChildProcessConnection.setupConnection");
            } catch (Throwable th) {
                TraceEvent.end("ChildProcessConnection.setupConnection");
            }
        }
    }

    public void stop() {
        if ($assertionsDisabled || isRunningOnLauncherThread()) {
            unbind();
            notifyChildProcessDied();
            return;
        }
        throw new AssertionError();
    }

    private void onServiceConnectedOnLauncherThread(IBinder service) {
        if (!$assertionsDisabled && !isRunningOnLauncherThread()) {
            throw new AssertionError();
        } else if (!this.mDidOnServiceConnected) {
            try {
                TraceEvent.begin("ChildProcessConnection.ChildServiceConnection.onServiceConnected");
                this.mDidOnServiceConnected = true;
                this.mService = Stub.asInterface(service);
                if (this.mBindToCaller) {
                    if (!this.mService.bindToCaller()) {
                        if (this.mServiceCallback != null) {
                            this.mServiceCallback.onChildStartFailed(this);
                        }
                        unbind();
                        return;
                    }
                }
                if (this.mServiceCallback != null) {
                    this.mServiceCallback.onChildStarted();
                }
                this.mServiceConnectComplete = true;
                if (this.mConnectionParams != null) {
                    doConnectionSetup();
                }
                TraceEvent.end("ChildProcessConnection.ChildServiceConnection.onServiceConnected");
            } catch (RemoteException ex) {
                Log.e(TAG, "Failed to bind service to connection.", ex);
            } finally {
                TraceEvent.end("ChildProcessConnection.ChildServiceConnection.onServiceConnected");
            }
        }
    }

    private void onServiceDisconnectedOnLauncherThread() {
        if (!$assertionsDisabled && !isRunningOnLauncherThread()) {
            throw new AssertionError();
        } else if (!this.mServiceDisconnected) {
            this.mServiceDisconnected = true;
            Log.w(TAG, "onServiceDisconnected (crash or killed by oom): pid=%d", Integer.valueOf(this.mPid));
            stop();
            if (this.mConnectionCallback != null) {
                this.mConnectionCallback.onConnected(null);
                this.mConnectionCallback = null;
            }
        }
    }

    private void onSetupConnectionResult(int pid) {
        this.mPid = pid;
        if ($assertionsDisabled || this.mPid != 0) {
            if (this.mConnectionCallback != null) {
                this.mConnectionCallback.onConnected(this);
            }
            this.mConnectionCallback = null;
            return;
        }
        throw new AssertionError("Child service claims to be run by a process of pid=0.");
    }

    private void doConnectionSetup() {
        try {
            TraceEvent.begin("ChildProcessConnection.doConnectionSetup");
            if (!$assertionsDisabled && (!this.mServiceConnectComplete || this.mService == null)) {
                throw new AssertionError();
            } else if ($assertionsDisabled || this.mConnectionParams != null) {
                this.mService.setupConnection(this.mConnectionParams.mConnectionBundle, new ICallbackInt.Stub() {
                    public void call(final int pid) {
                        ChildProcessConnection.this.mLauncherHandler.post(new Runnable() {
                            public void run() {
                                ChildProcessConnection.this.onSetupConnectionResult(pid);
                            }
                        });
                    }
                }, this.mConnectionParams.mClientInterfaces);
                this.mConnectionParams = null;
                TraceEvent.end("ChildProcessConnection.doConnectionSetup");
            } else {
                throw new AssertionError();
            }
        } catch (RemoteException re) {
            Log.e(TAG, "Failed to setup connection.", re);
        } catch (Throwable th) {
            TraceEvent.end("ChildProcessConnection.doConnectionSetup");
        }
    }

    private boolean bind(boolean useStrongBinding) {
        if (!$assertionsDisabled && !isRunningOnLauncherThread()) {
            throw new AssertionError();
        } else if ($assertionsDisabled || !this.mUnbound) {
            if (!(useStrongBinding ? this.mStrongBinding.bind() : this.mInitialBinding.bind())) {
                return $assertionsDisabled;
            }
            updateWaivedBoundOnlyState();
            this.mWaivedBinding.bind();
            return true;
        } else {
            throw new AssertionError();
        }
    }

    @VisibleForTesting
    protected void unbind() {
        if ($assertionsDisabled || isRunningOnLauncherThread()) {
            this.mService = null;
            this.mConnectionParams = null;
            this.mUnbound = true;
            this.mStrongBinding.unbind();
            this.mWaivedBinding.unbind();
            this.mModerateBinding.unbind();
            this.mInitialBinding.unbind();
            return;
        }
        throw new AssertionError();
    }

    public boolean isInitialBindingBound() {
        if ($assertionsDisabled || isRunningOnLauncherThread()) {
            return this.mInitialBinding.isBound();
        }
        throw new AssertionError();
    }

    public void addInitialBinding() {
        if ($assertionsDisabled || isRunningOnLauncherThread()) {
            this.mInitialBinding.bind();
            updateWaivedBoundOnlyState();
            return;
        }
        throw new AssertionError();
    }

    public boolean isStrongBindingBound() {
        if ($assertionsDisabled || isRunningOnLauncherThread()) {
            return this.mStrongBinding.isBound();
        }
        throw new AssertionError();
    }

    public void removeInitialBinding() {
        if ($assertionsDisabled || isRunningOnLauncherThread()) {
            this.mInitialBinding.unbind();
            updateWaivedBoundOnlyState();
            return;
        }
        throw new AssertionError();
    }

    public void addStrongBinding() {
        if (!$assertionsDisabled && !isRunningOnLauncherThread()) {
            throw new AssertionError();
        } else if (isConnected()) {
            if (this.mStrongBindingCount == 0) {
                this.mStrongBinding.bind();
                updateWaivedBoundOnlyState();
            }
            this.mStrongBindingCount++;
        } else {
            Log.w(TAG, "The connection is not bound for %d", Integer.valueOf(getPid()));
        }
    }

    public void removeStrongBinding() {
        if (!$assertionsDisabled && !isRunningOnLauncherThread()) {
            throw new AssertionError();
        } else if (!isConnected()) {
            Log.w(TAG, "The connection is not bound for %d", Integer.valueOf(getPid()));
        } else if ($assertionsDisabled || this.mStrongBindingCount > 0) {
            this.mStrongBindingCount--;
            if (this.mStrongBindingCount == 0) {
                this.mStrongBinding.unbind();
                updateWaivedBoundOnlyState();
            }
        } else {
            throw new AssertionError();
        }
    }

    public boolean isModerateBindingBound() {
        if ($assertionsDisabled || isRunningOnLauncherThread()) {
            return this.mModerateBinding.isBound();
        }
        throw new AssertionError();
    }

    public void addModerateBinding() {
        if (!$assertionsDisabled && !isRunningOnLauncherThread()) {
            throw new AssertionError();
        } else if (isConnected()) {
            if (this.mModerateBindingCount == 0) {
                this.mModerateBinding.bind();
                updateWaivedBoundOnlyState();
            }
            this.mModerateBindingCount++;
        } else {
            Log.w(TAG, "The connection is not bound for %d", Integer.valueOf(getPid()));
        }
    }

    public void removeModerateBinding() {
        if (!$assertionsDisabled && !isRunningOnLauncherThread()) {
            throw new AssertionError();
        } else if (!isConnected()) {
            Log.w(TAG, "The connection is not bound for %d", Integer.valueOf(getPid()));
        } else if ($assertionsDisabled || this.mModerateBindingCount > 0) {
            this.mModerateBindingCount--;
            if (this.mModerateBindingCount == 0) {
                this.mModerateBinding.unbind();
                updateWaivedBoundOnlyState();
            }
        } else {
            throw new AssertionError();
        }
    }

    public boolean isWaivedBoundOnlyOrWasWhenDied() {
        return this.mWaivedBoundOnly;
    }

    private void updateWaivedBoundOnlyState() {
        if (!this.mUnbound) {
            boolean z = (this.mInitialBinding.isBound() || this.mStrongBinding.isBound() || this.mModerateBinding.isBound()) ? $assertionsDisabled : true;
            this.mWaivedBoundOnly = z;
        }
    }

    private void notifyChildProcessDied() {
        if (this.mServiceCallback != null) {
            ServiceCallback serviceCallback = this.mServiceCallback;
            this.mServiceCallback = null;
            serviceCallback.onChildProcessDied(this);
        }
    }

    private boolean isRunningOnLauncherThread() {
        return this.mLauncherHandler.getLooper() == Looper.myLooper() ? true : $assertionsDisabled;
    }

    @VisibleForTesting
    public void crashServiceForTesting() throws RemoteException {
        this.mService.crashIntentionallyForTesting();
    }

    @VisibleForTesting
    public boolean didOnServiceConnectedForTesting() {
        return this.mDidOnServiceConnected;
    }

    @VisibleForTesting
    protected Handler getLauncherHandler() {
        return this.mLauncherHandler;
    }
}
