package org.chromium.base.library_loader;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.os.StrictMode;
import android.os.StrictMode.ThreadPolicy;
import android.os.SystemClock;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nullable;
import org.chromium.base.CommandLine;
import org.chromium.base.ContextUtils;
import org.chromium.base.Log;
import org.chromium.base.TraceEvent;
import org.chromium.base.VisibleForTesting;
import org.chromium.base.annotations.CalledByNative;
import org.chromium.base.annotations.JNINamespace;
import org.chromium.base.annotations.MainDex;
import org.chromium.base.metrics.RecordHistogram;

@MainDex
@JNINamespace("base::android")
public class LibraryLoader {
    static final /* synthetic */ boolean $assertionsDisabled = (!LibraryLoader.class.desiredAssertionStatus());
    private static final boolean DEBUG = false;
    private static final String DONT_PREFETCH_LIBRARIES_KEY = "dont_prefetch_libraries";
    private static final String TAG = "LibraryLoader";
    private static volatile LibraryLoader sInstance;
    private static NativeLibraryPreloader sLibraryPreloader;
    private static boolean sLibraryPreloaderCalled;
    private static final Object sLock = new Object();
    private boolean mCommandLineSwitched;
    private volatile boolean mInitialized;
    private boolean mIsUsingBrowserSharedRelros;
    private long mLibraryLoadTimeMs;
    private int mLibraryPreloaderStatus = -1;
    private final int mLibraryProcessType;
    private boolean mLibraryWasLoadedFromApk;
    private boolean mLoadAtFixedAddressFailed;
    private boolean mLoaded;
    private final AtomicBoolean mPrefetchLibraryHasBeenCalled;

    /* renamed from: org.chromium.base.library_loader.LibraryLoader$1 */
    class AnonymousClass1 extends AsyncTask<Void, Void, Void> {
        final /* synthetic */ boolean val$coldStart;

        AnonymousClass1(boolean z) {
            this.val$coldStart = z;
        }

        protected Void doInBackground(Void... params) {
            Throwable th;
            Throwable th2 = null;
            boolean prefetch = false;
            TraceEvent e = TraceEvent.scoped("LibraryLoader.asyncPrefetchLibrariesToMemory");
            try {
                int percentage = LibraryLoader.nativePercentageOfResidentNativeLibraryCode();
                boolean success = false;
                if (this.val$coldStart && percentage < 90) {
                    prefetch = true;
                }
                if (prefetch) {
                    success = LibraryLoader.nativeForkAndPrefetchNativeLibrary();
                    if (!success) {
                        Log.w(LibraryLoader.TAG, "Forking a process to prefetch the native library failed.", new Object[0]);
                    }
                }
                RecordHistogram.initialize();
                if (prefetch) {
                    RecordHistogram.recordBooleanHistogram("LibraryLoader.PrefetchStatus", success);
                }
                if (percentage != -1) {
                    RecordHistogram.recordPercentageHistogram("LibraryLoader.PercentageOfResidentCodeBeforePrefetch" + (this.val$coldStart ? ".ColdStartup" : ".WarmStartup"), percentage);
                }
                if (e != null) {
                    if (th2 != null) {
                        try {
                            e.close();
                        } catch (Throwable th3) {
                            th2.addSuppressed(th3);
                        }
                    } else {
                        e.close();
                    }
                }
                return th2;
            } catch (Throwable th22) {
                Throwable th4 = th22;
                th22 = th3;
                th3 = th4;
            }
            throw th3;
            if (e != null) {
                if (th22 != null) {
                    try {
                        e.close();
                    } catch (Throwable th5) {
                        th22.addSuppressed(th5);
                    }
                } else {
                    e.close();
                }
            }
            throw th3;
        }
    }

    private static native boolean nativeForkAndPrefetchNativeLibrary();

    private native String nativeGetVersionNumber();

    private native boolean nativeLibraryLoaded();

    private static native int nativePercentageOfResidentNativeLibraryCode();

    private static native void nativePeriodicallyCollectResidency();

    private native void nativeRecordChromiumAndroidLinkerBrowserHistogram(boolean z, boolean z2, int i, long j);

    private native void nativeRecordLibraryPreloaderBrowserHistogram(int i);

    private native void nativeRegisterChromiumAndroidLinkerRendererHistogram(boolean z, boolean z2, long j);

    private native void nativeRegisterLibraryPreloaderRendererHistogram(int i);

    public void asyncPrefetchLibrariesToMemory() {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception: Unknown instruction: 'invoke-custom' in method: org.chromium.base.library_loader.LibraryLoader.asyncPrefetchLibrariesToMemory():void, dex: 
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:116)
	at jadx.core.dex.nodes.ClassNode.load(ClassNode.java:249)
	at jadx.core.ProcessClass.process(ProcessClass.java:31)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:282)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
Caused by: jadx.core.utils.exceptions.DecodeException: Unknown instruction: 'invoke-custom'
	at jadx.core.dex.instructions.InsnDecoder.decode(InsnDecoder.java:590)
	at jadx.core.dex.instructions.InsnDecoder.process(InsnDecoder.java:74)
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:102)
	... 5 more
*/
        /*
        // Can't load method instructions.
        */
        throw new UnsupportedOperationException("Method not decompiled: org.chromium.base.library_loader.LibraryLoader.asyncPrefetchLibrariesToMemory():void");
    }

    public static void setNativeLibraryPreloader(NativeLibraryPreloader loader) {
        synchronized (sLock) {
            if ($assertionsDisabled || (sLibraryPreloader == null && (sInstance == null || !sInstance.mLoaded))) {
                sLibraryPreloader = loader;
            } else {
                throw new AssertionError();
            }
        }
    }

    public static LibraryLoader get(int libraryProcessType) throws ProcessInitException {
        LibraryLoader libraryLoader;
        synchronized (sLock) {
            if (sInstance == null) {
                sInstance = new LibraryLoader(libraryProcessType);
                libraryLoader = sInstance;
            } else if (sInstance.mLibraryProcessType == libraryProcessType) {
                libraryLoader = sInstance;
            } else {
                throw new ProcessInitException(2);
            }
        }
        return libraryLoader;
    }

    private LibraryLoader(int libraryProcessType) {
        this.mLibraryProcessType = libraryProcessType;
        this.mPrefetchLibraryHasBeenCalled = new AtomicBoolean();
    }

    public void ensureInitialized() throws ProcessInitException {
        synchronized (sLock) {
            if (this.mInitialized) {
                return;
            }
            loadAlreadyLocked(ContextUtils.getApplicationContext());
            initializeAlreadyLocked();
        }
    }

    public void preloadNow() {
        preloadNowOverrideApplicationContext(ContextUtils.getApplicationContext());
    }

    public void preloadNowOverrideApplicationContext(Context appContext) {
        synchronized (sLock) {
            if (!Linker.isUsed()) {
                preloadAlreadyLocked(appContext);
            }
        }
    }

    private void preloadAlreadyLocked(Context appContext) {
        Throwable th;
        TraceEvent te = TraceEvent.scoped("LibraryLoader.preloadAlreadyLocked");
        Throwable th2 = null;
        try {
            if ($assertionsDisabled || !Linker.isUsed()) {
                if (!(sLibraryPreloader == null || sLibraryPreloaderCalled)) {
                    this.mLibraryPreloaderStatus = sLibraryPreloader.loadLibrary(appContext);
                    sLibraryPreloaderCalled = true;
                }
                if (te != null) {
                    $closeResource(null, te);
                    return;
                }
                return;
            }
            throw new AssertionError();
        } catch (Throwable th22) {
            Throwable th3 = th22;
            th22 = th;
            th = th3;
        }
        if (te != null) {
            $closeResource(th22, te);
        }
        throw th;
    }

    private static /* synthetic */ void $closeResource(Throwable x0, AutoCloseable x1) {
        if (x0 != null) {
            try {
                x1.close();
                return;
            } catch (Throwable th) {
                x0.addSuppressed(th);
                return;
            }
        }
        x1.close();
    }

    public static boolean isInitialized() {
        return sInstance != null && sInstance.mInitialized;
    }

    public void loadNow() throws ProcessInitException {
        loadNowOverrideApplicationContext(ContextUtils.getApplicationContext());
    }

    public void loadNowOverrideApplicationContext(Context appContext) throws ProcessInitException {
        synchronized (sLock) {
            if (!this.mLoaded || appContext == ContextUtils.getApplicationContext()) {
                loadAlreadyLocked(appContext);
            } else {
                throw new IllegalStateException("Attempt to load again from alternate context.");
            }
        }
    }

    public void initialize() throws ProcessInitException {
        synchronized (sLock) {
            initializeAlreadyLocked();
        }
    }

    public static void setDontPrefetchLibrariesOnNextRuns(boolean dontPrefetch) {
        ContextUtils.getAppSharedPreferences().edit().putBoolean(DONT_PREFETCH_LIBRARIES_KEY, dontPrefetch).apply();
    }

    private static boolean isNotPrefetchingLibraries() {
        ThreadPolicy oldPolicy = StrictMode.allowThreadDiskReads();
        try {
            boolean z = ContextUtils.getAppSharedPreferences().getBoolean(DONT_PREFETCH_LIBRARIES_KEY, false);
            return z;
        } finally {
            StrictMode.setThreadPolicy(oldPolicy);
        }
    }

    private void loadLibrary(Linker linker, @Nullable String zipFilePath, String libFilePath) {
        if (linker.isUsingBrowserSharedRelros()) {
            this.mIsUsingBrowserSharedRelros = true;
            try {
                linker.loadLibrary(zipFilePath, libFilePath);
            } catch (UnsatisfiedLinkError e) {
                Log.w(TAG, "Failed to load native library with shared RELRO, retrying without", new Object[0]);
                this.mLoadAtFixedAddressFailed = true;
                linker.loadLibraryNoFixedAddress(zipFilePath, libFilePath);
            }
        } else {
            linker.loadLibrary(zipFilePath, libFilePath);
        }
        if (zipFilePath != null) {
            this.mLibraryWasLoadedFromApk = true;
        }
    }

    @SuppressLint({"DefaultLocale"})
    private void loadAlreadyLocked(Context appContext) throws ProcessInitException {
        Throwable th;
        try {
            TraceEvent te = TraceEvent.scoped("LibraryLoader.loadAlreadyLocked");
            Throwable th2 = null;
            try {
                if (!this.mLoaded) {
                    if ($assertionsDisabled || !this.mInitialized) {
                        long startTime = SystemClock.uptimeMillis();
                        if (Linker.isUsed()) {
                            Linker linker = Linker.getInstance();
                            linker.prepareLibraryLoad();
                            for (String library : NativeLibraries.LIBRARIES) {
                                if (!linker.isChromiumLinkerLibrary(library)) {
                                    String zipFilePath = null;
                                    String libFilePath = System.mapLibraryName(library);
                                    if (Linker.isInZipFile()) {
                                        zipFilePath = appContext.getApplicationInfo().sourceDir;
                                        Log.i(TAG, "Loading " + library + " from within " + zipFilePath, new Object[0]);
                                    } else {
                                        Log.i(TAG, "Loading " + library, new Object[0]);
                                    }
                                    loadLibrary(linker, zipFilePath, libFilePath);
                                }
                            }
                            linker.finishLibraryLoad();
                        } else {
                            preloadAlreadyLocked(appContext);
                            for (String library2 : NativeLibraries.LIBRARIES) {
                                System.loadLibrary(library2);
                            }
                        }
                        this.mLibraryLoadTimeMs = SystemClock.uptimeMillis() - startTime;
                        Log.i(TAG, String.format("Time to load native libraries: %d ms (timestamps %d-%d)", new Object[]{Long.valueOf(this.mLibraryLoadTimeMs), Long.valueOf(startTime % 10000), Long.valueOf(stopTime % 10000)}), new Object[0]);
                        this.mLoaded = true;
                    } else {
                        throw new AssertionError();
                    }
                }
                if (te != null) {
                    $closeResource(null, te);
                    return;
                }
                return;
            } catch (UnsatisfiedLinkError e) {
                Log.e(TAG, "Unable to load library: " + library2, new Object[0]);
                throw e;
            } catch (UnsatisfiedLinkError e2) {
                Log.e(TAG, "Unable to load library: " + library2, new Object[0]);
                throw e2;
            } catch (Throwable th22) {
                Throwable th3 = th22;
                th22 = th;
                th = th3;
            }
            if (te != null) {
                $closeResource(th22, te);
            }
            throw th;
        } catch (UnsatisfiedLinkError e22) {
            throw new ProcessInitException(2, e22);
        }
    }

    public void switchCommandLineForWebView() {
        synchronized (sLock) {
            ensureCommandLineSwitchedAlreadyLocked();
        }
    }

    private void ensureCommandLineSwitchedAlreadyLocked() {
        if (!$assertionsDisabled && !this.mLoaded) {
            throw new AssertionError();
        } else if (!this.mCommandLineSwitched) {
            CommandLine.enableNativeProxy();
            this.mCommandLineSwitched = true;
        }
    }

    private void initializeAlreadyLocked() throws ProcessInitException {
        if (!this.mInitialized) {
            ensureCommandLineSwitchedAlreadyLocked();
            if (nativeLibraryLoaded()) {
                Log.i(TAG, String.format("Expected native library version number \"%s\", actual native library version number \"%s\"", new Object[]{NativeLibraries.sVersionNumber, nativeGetVersionNumber()}), new Object[0]);
                if (NativeLibraries.sVersionNumber.equals(nativeGetVersionNumber())) {
                    TraceEvent.registerNativeEnabledObserver();
                    this.mInitialized = true;
                    return;
                }
                throw new ProcessInitException(3);
            }
            Log.e(TAG, "error calling nativeLibraryLoaded", new Object[0]);
            throw new ProcessInitException(1);
        }
    }

    public void onNativeInitializationComplete() {
        recordBrowserProcessHistogram();
    }

    private void recordBrowserProcessHistogram() {
        Linker.getInstance();
        if (Linker.isUsed()) {
            nativeRecordChromiumAndroidLinkerBrowserHistogram(this.mIsUsingBrowserSharedRelros, this.mLoadAtFixedAddressFailed, getLibraryLoadFromApkStatus(), this.mLibraryLoadTimeMs);
        }
        if (sLibraryPreloader != null) {
            nativeRecordLibraryPreloaderBrowserHistogram(this.mLibraryPreloaderStatus);
        }
    }

    private int getLibraryLoadFromApkStatus() {
        if (!$assertionsDisabled) {
            Linker.getInstance();
            if (!Linker.isUsed()) {
                throw new AssertionError();
            }
        }
        if (this.mLibraryWasLoadedFromApk) {
            return 3;
        }
        return 0;
    }

    public void registerRendererProcessHistogram(boolean requestedSharedRelro, boolean loadAtFixedAddressFailed) {
        Linker.getInstance();
        if (Linker.isUsed()) {
            nativeRegisterChromiumAndroidLinkerRendererHistogram(requestedSharedRelro, loadAtFixedAddressFailed, this.mLibraryLoadTimeMs);
        }
        if (sLibraryPreloader != null) {
            nativeRegisterLibraryPreloaderRendererHistogram(this.mLibraryPreloaderStatus);
        }
    }

    @CalledByNative
    public static int getLibraryProcessType() {
        if (sInstance == null) {
            return 0;
        }
        return sInstance.mLibraryProcessType;
    }

    @VisibleForTesting
    public static void setLibraryLoaderForTesting(LibraryLoader loader) {
        sInstance = loader;
    }
}
