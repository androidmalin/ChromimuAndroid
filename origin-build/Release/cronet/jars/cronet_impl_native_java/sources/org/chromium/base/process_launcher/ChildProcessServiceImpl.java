package org.chromium.base.process_launcher;

import android.content.Context;
import android.os.Binder;
import android.os.Bundle;
import android.os.Debug;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Process;
import android.os.RemoteException;
import android.util.SparseArray;
import java.util.List;
import java.util.concurrent.Semaphore;
import javax.annotation.concurrent.GuardedBy;
import org.chromium.base.BaseSwitches;
import org.chromium.base.CommandLine;
import org.chromium.base.ContextUtils;
import org.chromium.base.Log;
import org.chromium.base.annotations.JNINamespace;
import org.chromium.base.annotations.MainDex;
import org.chromium.base.process_launcher.IChildProcessService.Stub;

@MainDex
@JNINamespace("base::android")
public class ChildProcessServiceImpl {
    static final /* synthetic */ boolean $assertionsDisabled = (!ChildProcessServiceImpl.class.desiredAssertionStatus() ? true : $assertionsDisabled);
    private static final String MAIN_THREAD_NAME = "ChildProcessMain";
    private static final String TAG = "ChildProcessService";
    private static boolean sCreateCalled;
    private final Semaphore mActivitySemaphore = new Semaphore(1);
    private int mAuthorizedCallerUid;
    private boolean mBindToCallerCheck;
    private final Stub mBinder = new Stub() {
        static final /* synthetic */ boolean $assertionsDisabled = (!ChildProcessServiceImpl.class.desiredAssertionStatus() ? true : ChildProcessServiceImpl.$assertionsDisabled);

        /* JADX WARNING: inconsistent code. */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public boolean bindToCaller() {
            /*
            r8 = this;
            r2 = 1;
            r1 = 0;
            r3 = $assertionsDisabled;
            if (r3 != 0) goto L_0x0014;
        L_0x0006:
            r3 = org.chromium.base.process_launcher.ChildProcessServiceImpl.this;
            r3 = r3.mBindToCallerCheck;
            if (r3 != 0) goto L_0x0014;
        L_0x000e:
            r1 = new java.lang.AssertionError;
            r1.<init>();
            throw r1;
        L_0x0014:
            r3 = $assertionsDisabled;
            if (r3 != 0) goto L_0x0026;
        L_0x0018:
            r3 = org.chromium.base.process_launcher.ChildProcessServiceImpl.this;
            r3 = r3.mServiceBound;
            if (r3 != 0) goto L_0x0026;
        L_0x0020:
            r1 = new java.lang.AssertionError;
            r1.<init>();
            throw r1;
        L_0x0026:
            r3 = org.chromium.base.process_launcher.ChildProcessServiceImpl.this;
            r3 = r3.mBinderLock;
            monitor-enter(r3);
            r0 = android.os.Binder.getCallingPid();	 Catch:{ all -> 0x0069 }
            r4 = org.chromium.base.process_launcher.ChildProcessServiceImpl.this;	 Catch:{ all -> 0x0069 }
            r4 = r4.mBoundCallingPid;	 Catch:{ all -> 0x0069 }
            if (r4 != 0) goto L_0x0041;
        L_0x0039:
            r1 = org.chromium.base.process_launcher.ChildProcessServiceImpl.this;	 Catch:{ all -> 0x0069 }
            r1.mBoundCallingPid = r0;	 Catch:{ all -> 0x0069 }
        L_0x003e:
            monitor-exit(r3);	 Catch:{ all -> 0x0069 }
            r1 = r2;
        L_0x0040:
            return r1;
        L_0x0041:
            r4 = org.chromium.base.process_launcher.ChildProcessServiceImpl.this;	 Catch:{ all -> 0x0069 }
            r4 = r4.mBoundCallingPid;	 Catch:{ all -> 0x0069 }
            if (r4 == r0) goto L_0x003e;
        L_0x0049:
            r2 = "ChildProcessService";
            r4 = "Service is already bound by pid %d, cannot bind for pid %d";
            r5 = 2;
            r5 = new java.lang.Object[r5];	 Catch:{ all -> 0x0069 }
            r6 = 0;
            r7 = org.chromium.base.process_launcher.ChildProcessServiceImpl.this;	 Catch:{ all -> 0x0069 }
            r7 = r7.mBoundCallingPid;	 Catch:{ all -> 0x0069 }
            r7 = java.lang.Integer.valueOf(r7);	 Catch:{ all -> 0x0069 }
            r5[r6] = r7;	 Catch:{ all -> 0x0069 }
            r6 = 1;
            r7 = java.lang.Integer.valueOf(r0);	 Catch:{ all -> 0x0069 }
            r5[r6] = r7;	 Catch:{ all -> 0x0069 }
            org.chromium.base.Log.e(r2, r4, r5);	 Catch:{ all -> 0x0069 }
            monitor-exit(r3);	 Catch:{ all -> 0x0069 }
            goto L_0x0040;
        L_0x0069:
            r1 = move-exception;
            monitor-exit(r3);	 Catch:{ all -> 0x0069 }
            throw r1;
            */
            throw new UnsupportedOperationException("Method not decompiled: org.chromium.base.process_launcher.ChildProcessServiceImpl.1.bindToCaller():boolean");
        }

        /* JADX WARNING: inconsistent code. */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void setupConnection(android.os.Bundle r5, org.chromium.base.process_launcher.ICallbackInt r6, java.util.List<android.os.IBinder> r7) throws android.os.RemoteException {
            /*
            r4 = this;
            r0 = $assertionsDisabled;
            if (r0 != 0) goto L_0x0012;
        L_0x0004:
            r0 = org.chromium.base.process_launcher.ChildProcessServiceImpl.this;
            r0 = r0.mServiceBound;
            if (r0 != 0) goto L_0x0012;
        L_0x000c:
            r0 = new java.lang.AssertionError;
            r0.<init>();
            throw r0;
        L_0x0012:
            r0 = org.chromium.base.process_launcher.ChildProcessServiceImpl.this;
            r1 = r0.mBinderLock;
            monitor-enter(r1);
            r0 = org.chromium.base.process_launcher.ChildProcessServiceImpl.this;	 Catch:{ all -> 0x0047 }
            r0 = r0.mBindToCallerCheck;	 Catch:{ all -> 0x0047 }
            if (r0 == 0) goto L_0x0039;
        L_0x0021:
            r0 = org.chromium.base.process_launcher.ChildProcessServiceImpl.this;	 Catch:{ all -> 0x0047 }
            r0 = r0.mBoundCallingPid;	 Catch:{ all -> 0x0047 }
            if (r0 != 0) goto L_0x0039;
        L_0x0029:
            r0 = "ChildProcessService";
            r2 = "Service has not been bound with bindToCaller()";
            r3 = 0;
            r3 = new java.lang.Object[r3];	 Catch:{ all -> 0x0047 }
            org.chromium.base.Log.e(r0, r2, r3);	 Catch:{ all -> 0x0047 }
            r0 = -1;
            r6.call(r0);	 Catch:{ all -> 0x0047 }
            monitor-exit(r1);	 Catch:{ all -> 0x0047 }
        L_0x0038:
            return;
        L_0x0039:
            monitor-exit(r1);	 Catch:{ all -> 0x0047 }
            r0 = android.os.Process.myPid();
            r6.call(r0);
            r0 = org.chromium.base.process_launcher.ChildProcessServiceImpl.this;
            r0.processConnectionBundle(r5, r7);
            goto L_0x0038;
        L_0x0047:
            r0 = move-exception;
            monitor-exit(r1);	 Catch:{ all -> 0x0047 }
            throw r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: org.chromium.base.process_launcher.ChildProcessServiceImpl.1.setupConnection(android.os.Bundle, org.chromium.base.process_launcher.ICallbackInt, java.util.List):void");
        }

        public void crashIntentionallyForTesting() {
            if ($assertionsDisabled || ChildProcessServiceImpl.this.mServiceBound) {
                Process.killProcess(Process.myPid());
                return;
            }
            throw new AssertionError();
        }

        public boolean onTransact(int arg0, Parcel arg1, Parcel arg2, int arg3) throws RemoteException {
            if ($assertionsDisabled || ChildProcessServiceImpl.this.mServiceBound) {
                if (ChildProcessServiceImpl.this.mAuthorizedCallerUid >= 0) {
                    int callingUid = Binder.getCallingUid();
                    if (callingUid != ChildProcessServiceImpl.this.mAuthorizedCallerUid) {
                        throw new RemoteException("Unauthorized caller " + callingUid + "does not match expected host=" + ChildProcessServiceImpl.this.mAuthorizedCallerUid);
                    }
                }
                return super.onTransact(arg0, arg1, arg2, arg3);
            }
            throw new AssertionError();
        }
    };
    private final Object mBinderLock = new Object();
    @GuardedBy("mBinderLock")
    private int mBoundCallingPid;
    private String[] mCommandLineParams;
    private final ChildProcessServiceDelegate mDelegate;
    private FileDescriptorInfo[] mFdInfos;
    private ClassLoader mHostClassLoader;
    private Context mHostContext;
    @GuardedBy("mLibraryInitializedLock")
    private boolean mLibraryInitialized;
    private final Object mLibraryInitializedLock = new Object();
    private Thread mMainThread;
    private boolean mServiceBound;

    private static native void nativeExitChildProcess();

    private static native void nativeRegisterFileDescriptors(String[] strArr, int[] iArr, int[] iArr2, long[] jArr, long[] jArr2);

    public android.os.IBinder bind(android.content.Intent r1, int r2) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception: Unknown instruction: 'invoke-custom' in method: org.chromium.base.process_launcher.ChildProcessServiceImpl.bind(android.content.Intent, int):android.os.IBinder, dex: 
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:116)
	at jadx.core.dex.nodes.ClassNode.load(ClassNode.java:249)
	at jadx.core.ProcessClass.process(ProcessClass.java:31)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:282)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
Caused by: jadx.core.utils.exceptions.DecodeException: Unknown instruction: 'invoke-custom'
	at jadx.core.dex.instructions.InsnDecoder.decode(InsnDecoder.java:590)
	at jadx.core.dex.instructions.InsnDecoder.process(InsnDecoder.java:74)
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:102)
	... 7 more
*/
        /*
        // Can't load method instructions.
        */
        throw new UnsupportedOperationException("Method not decompiled: org.chromium.base.process_launcher.ChildProcessServiceImpl.bind(android.content.Intent, int):android.os.IBinder");
    }

    public ChildProcessServiceImpl(ChildProcessServiceDelegate delegate) {
        this.mDelegate = delegate;
    }

    public void create(Context context, final Context hostContext) {
        this.mHostClassLoader = hostContext.getClassLoader();
        this.mHostContext = hostContext;
        Log.i(TAG, "Creating new ChildProcessService pid=%d", Integer.valueOf(Process.myPid()));
        if (sCreateCalled) {
            throw new RuntimeException("Illegal child process reuse.");
        }
        sCreateCalled = true;
        ContextUtils.initApplicationContext(context);
        this.mDelegate.onServiceCreated();
        this.mMainThread = new Thread(new Runnable() {
            static final /* synthetic */ boolean $assertionsDisabled = (!ChildProcessServiceImpl.class.desiredAssertionStatus() ? true : ChildProcessServiceImpl.$assertionsDisabled);

            public void run() {
                try {
                    synchronized (ChildProcessServiceImpl.this.mMainThread) {
                        while (ChildProcessServiceImpl.this.mCommandLineParams == null) {
                            ChildProcessServiceImpl.this.mMainThread.wait();
                        }
                    }
                    if ($assertionsDisabled || ChildProcessServiceImpl.this.mServiceBound) {
                        CommandLine.init(ChildProcessServiceImpl.this.mCommandLineParams);
                        if (CommandLine.getInstance().hasSwitch(BaseSwitches.RENDERER_WAIT_FOR_JAVA_DEBUGGER)) {
                            Debug.waitForDebugger();
                        }
                        boolean nativeLibraryLoaded = ChildProcessServiceImpl.$assertionsDisabled;
                        try {
                            nativeLibraryLoaded = ChildProcessServiceImpl.this.mDelegate.loadNativeLibrary(hostContext);
                        } catch (Exception e) {
                            Log.e(ChildProcessServiceImpl.TAG, "Failed to load native library.", e);
                        }
                        if (!nativeLibraryLoaded) {
                            System.exit(-1);
                        }
                        synchronized (ChildProcessServiceImpl.this.mLibraryInitializedLock) {
                            ChildProcessServiceImpl.this.mLibraryInitialized = true;
                            ChildProcessServiceImpl.this.mLibraryInitializedLock.notifyAll();
                        }
                        synchronized (ChildProcessServiceImpl.this.mMainThread) {
                            ChildProcessServiceImpl.this.mMainThread.notifyAll();
                            while (ChildProcessServiceImpl.this.mFdInfos == null) {
                                ChildProcessServiceImpl.this.mMainThread.wait();
                            }
                        }
                        SparseArray<String> idsToKeys = ChildProcessServiceImpl.this.mDelegate.getFileDescriptorsIdsToKeys();
                        int[] fileIds = new int[ChildProcessServiceImpl.this.mFdInfos.length];
                        String[] keys = new String[ChildProcessServiceImpl.this.mFdInfos.length];
                        int[] fds = new int[ChildProcessServiceImpl.this.mFdInfos.length];
                        long[] regionOffsets = new long[ChildProcessServiceImpl.this.mFdInfos.length];
                        long[] regionSizes = new long[ChildProcessServiceImpl.this.mFdInfos.length];
                        for (int i = 0; i < ChildProcessServiceImpl.this.mFdInfos.length; i++) {
                            FileDescriptorInfo fdInfo = ChildProcessServiceImpl.this.mFdInfos[i];
                            String key = idsToKeys != null ? (String) idsToKeys.get(fdInfo.id) : null;
                            if (key != null) {
                                keys[i] = key;
                            } else {
                                fileIds[i] = fdInfo.id;
                            }
                            fds[i] = fdInfo.fd.detachFd();
                            regionOffsets[i] = fdInfo.offset;
                            regionSizes[i] = fdInfo.size;
                        }
                        ChildProcessServiceImpl.nativeRegisterFileDescriptors(keys, fileIds, fds, regionOffsets, regionSizes);
                        ChildProcessServiceImpl.this.mDelegate.onBeforeMain();
                        if (ChildProcessServiceImpl.this.mActivitySemaphore.tryAcquire()) {
                            ChildProcessServiceImpl.this.mDelegate.runMain();
                            ChildProcessServiceImpl.nativeExitChildProcess();
                            return;
                        }
                        return;
                    }
                    throw new AssertionError();
                } catch (InterruptedException e2) {
                    Log.w(ChildProcessServiceImpl.TAG, "%s startup failed: %s", ChildProcessServiceImpl.MAIN_THREAD_NAME, e2);
                }
            }
        }, MAIN_THREAD_NAME);
        this.mMainThread.start();
    }

    public void destroy() {
        Log.i(TAG, "Destroying ChildProcessService pid=%d", Integer.valueOf(Process.myPid()));
        if (this.mActivitySemaphore.tryAcquire()) {
            System.exit(0);
            return;
        }
        synchronized (this.mLibraryInitializedLock) {
            while (!this.mLibraryInitialized) {
                try {
                    this.mLibraryInitializedLock.wait();
                } catch (InterruptedException e) {
                }
            }
        }
        this.mDelegate.onDestroy();
    }

    private /* synthetic */ void lambda$bind$0() {
        this.mDelegate.preloadNativeLibrary(this.mHostContext);
    }

    private void processConnectionBundle(Bundle bundle, List<IBinder> clientInterfaces) {
        bundle.setClassLoader(this.mHostClassLoader);
        synchronized (this.mMainThread) {
            if (this.mCommandLineParams == null) {
                this.mCommandLineParams = bundle.getStringArray(ChildProcessConstants.EXTRA_COMMAND_LINE);
                this.mMainThread.notifyAll();
            }
            if ($assertionsDisabled || this.mCommandLineParams != null) {
                Parcelable[] fdInfosAsParcelable = bundle.getParcelableArray(ChildProcessConstants.EXTRA_FILES);
                if (fdInfosAsParcelable != null) {
                    this.mFdInfos = new FileDescriptorInfo[fdInfosAsParcelable.length];
                    System.arraycopy(fdInfosAsParcelable, 0, this.mFdInfos, 0, fdInfosAsParcelable.length);
                }
                this.mDelegate.onConnectionSetup(bundle, clientInterfaces);
                this.mMainThread.notifyAll();
            } else {
                throw new AssertionError();
            }
        }
    }
}
