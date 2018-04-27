package org.chromium.base.process_launcher;

import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import java.io.IOException;
import java.util.List;
import org.chromium.base.ContextUtils;
import org.chromium.base.Log;
import org.chromium.base.TraceEvent;
import org.chromium.base.process_launcher.ChildConnectionAllocator.Listener;
import org.chromium.base.process_launcher.ChildProcessConnection.ConnectionCallback;
import org.chromium.base.process_launcher.ChildProcessConnection.ServiceCallback;

public class ChildProcessLauncher {
    static final /* synthetic */ boolean $assertionsDisabled = (!ChildProcessLauncher.class.desiredAssertionStatus() ? true : $assertionsDisabled);
    private static final int NULL_PROCESS_HANDLE = 0;
    private static final String TAG = "ChildProcLauncher";
    private final List<IBinder> mClientInterfaces;
    private final String[] mCommandLine;
    private ChildProcessConnection mConnection;
    private final ChildConnectionAllocator mConnectionAllocator;
    private final Delegate mDelegate;
    private final FileDescriptorInfo[] mFilesToBeMapped;
    private final Handler mLauncherHandler;

    public static abstract class Delegate {
        public ChildProcessConnection getBoundConnection(ChildConnectionAllocator connectionAllocator, ServiceCallback serviceCallback) {
            return null;
        }

        public void onBeforeConnectionAllocated(Bundle serviceBundle) {
        }

        public void onBeforeConnectionSetup(Bundle connectionBundle) {
        }

        public void onConnectionEstablished(ChildProcessConnection connection) {
        }

        public void onConnectionLost(ChildProcessConnection connection) {
        }
    }

    public ChildProcessLauncher(Handler launcherHandler, Delegate delegate, String[] commandLine, FileDescriptorInfo[] filesToBeMapped, ChildConnectionAllocator connectionAllocator, List<IBinder> clientInterfaces) {
        if ($assertionsDisabled || connectionAllocator != null) {
            this.mLauncherHandler = launcherHandler;
            isRunningOnLauncherThread();
            this.mCommandLine = commandLine;
            this.mConnectionAllocator = connectionAllocator;
            this.mDelegate = delegate;
            this.mFilesToBeMapped = filesToBeMapped;
            this.mClientInterfaces = clientInterfaces;
            return;
        }
        throw new AssertionError();
    }

    public boolean start(final boolean setupConnection, final boolean queueIfNoFreeConnection) {
        if ($assertionsDisabled || isRunningOnLauncherThread()) {
            try {
                TraceEvent.begin("ChildProcessLauncher.start");
                ServiceCallback serviceCallback = new ServiceCallback() {
                    static final /* synthetic */ boolean $assertionsDisabled = (!ChildProcessLauncher.class.desiredAssertionStatus() ? true : ChildProcessLauncher.$assertionsDisabled);

                    public void onChildStarted() {
                    }

                    public void onChildStartFailed(ChildProcessConnection connection) {
                        if (!$assertionsDisabled && !ChildProcessLauncher.this.isRunningOnLauncherThread()) {
                            throw new AssertionError();
                        } else if ($assertionsDisabled || ChildProcessLauncher.this.mConnection == connection) {
                            Log.e(ChildProcessLauncher.TAG, "ChildProcessConnection.start failed, trying again", new Object[0]);
                            ChildProcessLauncher.this.mLauncherHandler.post(new Runnable() {
                                public void run() {
                                    ChildProcessLauncher.this.mConnection = null;
                                    ChildProcessLauncher.this.start(setupConnection, queueIfNoFreeConnection);
                                }
                            });
                        } else {
                            throw new AssertionError();
                        }
                    }

                    public void onChildProcessDied(ChildProcessConnection connection) {
                        if (!$assertionsDisabled && !ChildProcessLauncher.this.isRunningOnLauncherThread()) {
                            throw new AssertionError();
                        } else if ($assertionsDisabled || ChildProcessLauncher.this.mConnection == connection) {
                            ChildProcessLauncher.this.onChildProcessDied();
                        } else {
                            throw new AssertionError();
                        }
                    }
                };
                this.mConnection = this.mDelegate.getBoundConnection(this.mConnectionAllocator, serviceCallback);
                if (this.mConnection != null) {
                    if ($assertionsDisabled || this.mConnectionAllocator.isConnectionFromAllocator(this.mConnection)) {
                        setupConnection();
                        return true;
                    }
                    throw new AssertionError();
                } else if (allocateAndSetupConnection(serviceCallback, setupConnection, queueIfNoFreeConnection) || queueIfNoFreeConnection) {
                    TraceEvent.end("ChildProcessLauncher.start");
                    return true;
                } else {
                    TraceEvent.end("ChildProcessLauncher.start");
                    return $assertionsDisabled;
                }
            } finally {
                TraceEvent.end("ChildProcessLauncher.start");
            }
        } else {
            throw new AssertionError();
        }
    }

    public ChildProcessConnection getConnection() {
        return this.mConnection;
    }

    public ChildConnectionAllocator getConnectionAllocator() {
        return this.mConnectionAllocator;
    }

    private boolean allocateAndSetupConnection(final ServiceCallback serviceCallback, final boolean setupConnection, final boolean queueIfNoFreeConnection) {
        if ($assertionsDisabled || this.mConnection == null) {
            Bundle serviceBundle = new Bundle();
            this.mDelegate.onBeforeConnectionAllocated(serviceBundle);
            this.mConnection = this.mConnectionAllocator.allocate(ContextUtils.getApplicationContext(), serviceBundle, serviceCallback);
            if (this.mConnection != null) {
                if (setupConnection) {
                    setupConnection();
                }
                return true;
            } else if (queueIfNoFreeConnection) {
                this.mConnectionAllocator.addListener(new Listener() {
                    static final /* synthetic */ boolean $assertionsDisabled = (!ChildProcessLauncher.class.desiredAssertionStatus() ? true : ChildProcessLauncher.$assertionsDisabled);

                    public void onConnectionFreed(ChildConnectionAllocator allocator, ChildProcessConnection connection) {
                        if (!$assertionsDisabled && allocator != ChildProcessLauncher.this.mConnectionAllocator) {
                            throw new AssertionError();
                        } else if (allocator.isFreeConnectionAvailable()) {
                            allocator.removeListener(this);
                            ChildProcessLauncher.this.allocateAndSetupConnection(serviceCallback, setupConnection, queueIfNoFreeConnection);
                        }
                    }
                });
                return $assertionsDisabled;
            } else {
                Log.d(TAG, "Failed to allocate a child connection (no queuing).");
                return $assertionsDisabled;
            }
        }
        throw new AssertionError();
    }

    private void setupConnection() {
        ConnectionCallback connectionCallback = new ConnectionCallback() {
            static final /* synthetic */ boolean $assertionsDisabled = (!ChildProcessLauncher.class.desiredAssertionStatus() ? true : ChildProcessLauncher.$assertionsDisabled);

            public void onConnected(ChildProcessConnection connection) {
                if ($assertionsDisabled || ChildProcessLauncher.this.mConnection == connection) {
                    ChildProcessLauncher.this.onServiceConnected();
                    return;
                }
                throw new AssertionError();
            }
        };
        Bundle connectionBundle = createConnectionBundle();
        this.mDelegate.onBeforeConnectionSetup(connectionBundle);
        this.mConnection.setupConnection(connectionBundle, getClientInterfaces(), connectionCallback);
    }

    private void onServiceConnected() {
        if ($assertionsDisabled || isRunningOnLauncherThread()) {
            Log.d(TAG, "on connect callback, pid=%d", Integer.valueOf(this.mConnection.getPid()));
            this.mDelegate.onConnectionEstablished(this.mConnection);
            try {
                for (FileDescriptorInfo fileInfo : this.mFilesToBeMapped) {
                    fileInfo.fd.close();
                }
                return;
            } catch (IOException ioe) {
                Log.w(TAG, "Failed to close FD.", ioe);
                return;
            }
        }
        throw new AssertionError();
    }

    public int getPid() {
        if ($assertionsDisabled || isRunningOnLauncherThread()) {
            return this.mConnection == null ? 0 : this.mConnection.getPid();
        } else {
            throw new AssertionError();
        }
    }

    public List<IBinder> getClientInterfaces() {
        return this.mClientInterfaces;
    }

    private boolean isRunningOnLauncherThread() {
        return this.mLauncherHandler.getLooper() == Looper.myLooper() ? true : $assertionsDisabled;
    }

    private Bundle createConnectionBundle() {
        Bundle bundle = new Bundle();
        bundle.putStringArray(ChildProcessConstants.EXTRA_COMMAND_LINE, this.mCommandLine);
        bundle.putParcelableArray(ChildProcessConstants.EXTRA_FILES, this.mFilesToBeMapped);
        return bundle;
    }

    private void onChildProcessDied() {
        if (!$assertionsDisabled && !isRunningOnLauncherThread()) {
            throw new AssertionError();
        } else if (getPid() != 0) {
            this.mDelegate.onConnectionLost(this.mConnection);
        }
    }

    public void stop() {
        if ($assertionsDisabled || isRunningOnLauncherThread()) {
            Log.d(TAG, "stopping child connection: pid=%d", Integer.valueOf(this.mConnection.getPid()));
            this.mConnection.stop();
            return;
        }
        throw new AssertionError();
    }
}
