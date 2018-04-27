package org.chromium.base.library_loader;

import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import org.chromium.base.ContextUtils;
import org.chromium.base.Log;
import org.chromium.base.annotations.AccessedByNative;

public abstract class Linker {
    protected static final int ADDRESS_SPACE_RESERVATION = 201326592;
    protected static final int BREAKPAD_GUARD_REGION_BYTES = 16777216;
    public static final int BROWSER_SHARED_RELRO_CONFIG = 1;
    public static final int BROWSER_SHARED_RELRO_CONFIG_ALWAYS = 2;
    public static final int BROWSER_SHARED_RELRO_CONFIG_LOW_RAM_ONLY = 1;
    public static final int BROWSER_SHARED_RELRO_CONFIG_NEVER = 0;
    protected static final boolean DEBUG = false;
    public static final String EXTRA_LINKER_SHARED_RELROS = "org.chromium.base.android.linker.shared_relros";
    public static final int LINKER_IMPLEMENTATION_LEGACY = 1;
    public static final int LINKER_IMPLEMENTATION_MODERN = 2;
    private static final String LINKER_JNI_LIBRARY = "chromium_android_linker";
    public static final int MEMORY_DEVICE_CONFIG_INIT = 0;
    public static final int MEMORY_DEVICE_CONFIG_LOW = 1;
    public static final int MEMORY_DEVICE_CONFIG_NORMAL = 2;
    private static final String TAG = "LibraryLoader";
    private static Linker sSingleton;
    private static Object sSingletonLock = new Object();
    protected final Object mLock = new Object();
    protected int mMemoryDeviceConfig = 0;
    private String mTestRunnerClassName;

    public static class LibInfo implements Parcelable {
        public static final Creator<LibInfo> CREATOR = new Creator<LibInfo>() {
            public LibInfo createFromParcel(Parcel in) {
                return new LibInfo(in);
            }

            public LibInfo[] newArray(int size) {
                return new LibInfo[size];
            }
        };
        @AccessedByNative
        public long mLoadAddress;
        @AccessedByNative
        public long mLoadSize;
        @AccessedByNative
        public int mRelroFd;
        @AccessedByNative
        public long mRelroSize;
        @AccessedByNative
        public long mRelroStart;

        public LibInfo() {
            this.mLoadAddress = 0;
            this.mLoadSize = 0;
            this.mRelroStart = 0;
            this.mRelroSize = 0;
            this.mRelroFd = -1;
        }

        public void close() {
            if (this.mRelroFd >= 0) {
                try {
                    ParcelFileDescriptor.adoptFd(this.mRelroFd).close();
                } catch (IOException e) {
                }
                this.mRelroFd = -1;
            }
        }

        public LibInfo(Parcel in) {
            this.mLoadAddress = in.readLong();
            this.mLoadSize = in.readLong();
            this.mRelroStart = in.readLong();
            this.mRelroSize = in.readLong();
            ParcelFileDescriptor fd = (ParcelFileDescriptor) ParcelFileDescriptor.CREATOR.createFromParcel(in);
            this.mRelroFd = fd == null ? -1 : fd.detachFd();
        }

        public void writeToParcel(Parcel out, int flags) {
            if (this.mRelroFd >= 0) {
                out.writeLong(this.mLoadAddress);
                out.writeLong(this.mLoadSize);
                out.writeLong(this.mRelroStart);
                out.writeLong(this.mRelroSize);
                try {
                    ParcelFileDescriptor fd = ParcelFileDescriptor.fromFd(this.mRelroFd);
                    fd.writeToParcel(out, 0);
                    fd.close();
                } catch (IOException e) {
                    Log.e(Linker.TAG, "Can't write LibInfo file descriptor to parcel", e);
                }
            }
        }

        public int describeContents() {
            return 1;
        }

        public String toString() {
            return String.format(Locale.US, "[load=0x%x-0x%x relro=0x%x-0x%x fd=%d]", new Object[]{Long.valueOf(this.mLoadAddress), Long.valueOf(this.mLoadAddress + this.mLoadSize), Long.valueOf(this.mRelroStart), Long.valueOf(this.mRelroStart + this.mRelroSize), Integer.valueOf(this.mRelroFd)});
        }
    }

    public interface TestRunner {
        boolean runChecks(int i, boolean z);
    }

    private static native long nativeGetRandomBaseLoadAddress();

    public abstract void disableSharedRelros();

    public abstract void finishLibraryLoad();

    public abstract long getBaseLoadAddress();

    public abstract Bundle getSharedRelros();

    public abstract void initServiceProcess(long j);

    public abstract boolean isUsingBrowserSharedRelros();

    abstract void loadLibraryImpl(@Nullable String str, String str2, boolean z);

    public abstract void prepareLibraryLoad();

    public abstract void useSharedRelros(Bundle bundle);

    protected Linker() {
    }

    public static final Linker getInstance() {
        Linker linker;
        boolean isIncrementalInstall = DEBUG;
        synchronized (sSingletonLock) {
            if (sSingleton == null) {
                String appClass = ContextUtils.getApplicationContext().getApplicationInfo().className;
                if (appClass != null && appClass.contains("incrementalinstall")) {
                    isIncrementalInstall = true;
                }
                if (VERSION.SDK_INT < 23 || isIncrementalInstall) {
                    sSingleton = LegacyLinker.create();
                } else {
                    sSingleton = ModernLinker.create();
                }
                Log.i(TAG, "Using linker: " + sSingleton.getClass().getName(), new Object[0]);
            }
            linker = sSingleton;
        }
        return linker;
    }

    public static boolean areTestsEnabled() {
        return NativeLibraries.sEnableLinkerTests;
    }

    private static void assertForTesting(boolean flag) {
        if (!flag) {
            throw new AssertionError();
        }
    }

    private static void assertLinkerTestsAreEnabled() {
        if (!NativeLibraries.sEnableLinkerTests) {
            throw new AssertionError("Testing method called in non-testing context");
        }
    }

    public static final void setImplementationForTesting(int type) {
        boolean z = DEBUG;
        assertLinkerTestsAreEnabled();
        boolean z2 = (type == 1 || type == 2) ? true : DEBUG;
        assertForTesting(z2);
        synchronized (sSingletonLock) {
            if (sSingleton == null) {
                z = true;
            }
            assertForTesting(z);
            if (type == 2) {
                sSingleton = ModernLinker.create();
            } else if (type == 1) {
                sSingleton = LegacyLinker.create();
            }
            Log.i(TAG, "Forced linker: " + sSingleton.getClass().getName(), new Object[0]);
        }
    }

    public final int getImplementationForTesting() {
        assertLinkerTestsAreEnabled();
        synchronized (sSingletonLock) {
            assertForTesting(sSingleton == this ? true : DEBUG);
            if (sSingleton instanceof ModernLinker) {
                return 2;
            } else if (sSingleton instanceof LegacyLinker) {
                return 1;
            } else {
                Log.wtf(TAG, "Invalid linker: " + sSingleton.getClass().getName(), new Object[0]);
                assertForTesting(DEBUG);
                return 0;
            }
        }
    }

    public final void setTestRunnerClassNameForTesting(String testRunnerClassName) {
        assertLinkerTestsAreEnabled();
        synchronized (this.mLock) {
            assertForTesting(this.mTestRunnerClassName == null ? true : DEBUG);
            this.mTestRunnerClassName = testRunnerClassName;
        }
    }

    public final String getTestRunnerClassNameForTesting() {
        String str;
        assertLinkerTestsAreEnabled();
        synchronized (this.mLock) {
            str = this.mTestRunnerClassName;
        }
        return str;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static final void setupForTesting(int r5, java.lang.String r6) {
        /*
        r1 = 1;
        r2 = 0;
        assertLinkerTestsAreEnabled();
        r4 = sSingletonLock;
        monitor-enter(r4);
        r3 = sSingleton;	 Catch:{ all -> 0x0031 }
        if (r3 != 0) goto L_0x0016;
    L_0x000c:
        setImplementationForTesting(r5);	 Catch:{ all -> 0x0031 }
        r1 = sSingleton;	 Catch:{ all -> 0x0031 }
        r1.setTestRunnerClassNameForTesting(r6);	 Catch:{ all -> 0x0031 }
        monitor-exit(r4);	 Catch:{ all -> 0x0031 }
    L_0x0015:
        return;
    L_0x0016:
        r3 = sSingleton;	 Catch:{ all -> 0x0031 }
        r3 = r3.getImplementationForTesting();	 Catch:{ all -> 0x0031 }
        if (r3 != r5) goto L_0x0034;
    L_0x001e:
        r3 = r1;
    L_0x001f:
        assertForTesting(r3);	 Catch:{ all -> 0x0031 }
        r3 = sSingleton;	 Catch:{ all -> 0x0031 }
        r0 = r3.getTestRunnerClassNameForTesting();	 Catch:{ all -> 0x0031 }
        if (r6 != 0) goto L_0x0038;
    L_0x002a:
        if (r0 != 0) goto L_0x0036;
    L_0x002c:
        assertForTesting(r1);	 Catch:{ all -> 0x0031 }
    L_0x002f:
        monitor-exit(r4);	 Catch:{ all -> 0x0031 }
        goto L_0x0015;
    L_0x0031:
        r1 = move-exception;
        monitor-exit(r4);	 Catch:{ all -> 0x0031 }
        throw r1;
    L_0x0034:
        r3 = r2;
        goto L_0x001f;
    L_0x0036:
        r1 = r2;
        goto L_0x002c;
    L_0x0038:
        r1 = r0.equals(r6);	 Catch:{ all -> 0x0031 }
        assertForTesting(r1);	 Catch:{ all -> 0x0031 }
        goto L_0x002f;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.chromium.base.library_loader.Linker.setupForTesting(int, java.lang.String):void");
    }

    protected final void runTestRunnerClassForTesting(int memoryDeviceConfig, boolean inBrowserProcess) {
        assertLinkerTestsAreEnabled();
        synchronized (this.mLock) {
            if (this.mTestRunnerClassName == null) {
                Log.wtf(TAG, "Linker runtime tests not set up for this process", new Object[0]);
                assertForTesting(DEBUG);
            }
            TestRunner testRunner = null;
            try {
                testRunner = (TestRunner) Class.forName(this.mTestRunnerClassName).getDeclaredConstructor(new Class[0]).newInstance(new Object[0]);
            } catch (Exception e) {
                Log.wtf(TAG, "Could not instantiate test runner class by name", e);
                assertForTesting(DEBUG);
            }
            if (!testRunner.runChecks(memoryDeviceConfig, inBrowserProcess)) {
                Log.wtf(TAG, "Linker runtime tests failed in this process", new Object[0]);
                assertForTesting(DEBUG);
            }
            Log.i(TAG, "All linker tests passed", new Object[0]);
        }
    }

    public final void setMemoryDeviceConfigForTesting(int memoryDeviceConfig) {
        boolean z = true;
        assertLinkerTestsAreEnabled();
        boolean z2 = (memoryDeviceConfig == 1 || memoryDeviceConfig == 2) ? true : DEBUG;
        assertForTesting(z2);
        synchronized (this.mLock) {
            if (this.mMemoryDeviceConfig != 0) {
                z = DEBUG;
            }
            assertForTesting(z);
            this.mMemoryDeviceConfig = memoryDeviceConfig;
        }
    }

    public boolean isChromiumLinkerLibrary(String library) {
        return library.equals(LINKER_JNI_LIBRARY);
    }

    protected static void loadLinkerJniLibrary() {
        String libName = "libchromium_android_linker.so";
        System.loadLibrary(LINKER_JNI_LIBRARY);
    }

    protected long getRandomBaseLoadAddress() {
        return nativeGetRandomBaseLoadAddress();
    }

    public void loadLibrary(@Nullable String zipFilePath, String libFilePath) {
        loadLibraryImpl(zipFilePath, libFilePath, true);
    }

    public void loadLibraryNoFixedAddress(@Nullable String zipFilePath, String libFilePath) {
        loadLibraryImpl(zipFilePath, libFilePath, DEBUG);
    }

    public static boolean isInZipFile() {
        return NativeLibraries.sUseLibraryInZipFile;
    }

    public static boolean isUsed() {
        return NativeLibraries.sUseLinker;
    }

    protected Bundle createBundleFromLibInfoMap(HashMap<String, LibInfo> map) {
        Bundle bundle = new Bundle(map.size());
        for (Entry<String, LibInfo> entry : map.entrySet()) {
            bundle.putParcelable((String) entry.getKey(), (Parcelable) entry.getValue());
        }
        return bundle;
    }

    protected HashMap<String, LibInfo> createLibInfoMapFromBundle(Bundle bundle) {
        HashMap<String, LibInfo> map = new HashMap();
        for (String library : bundle.keySet()) {
            map.put(library, (LibInfo) bundle.getParcelable(library));
        }
        return map;
    }

    protected void closeLibInfoMap(HashMap<String, LibInfo> map) {
        for (Entry<String, LibInfo> entry : map.entrySet()) {
            ((LibInfo) entry.getValue()).close();
        }
    }
}
