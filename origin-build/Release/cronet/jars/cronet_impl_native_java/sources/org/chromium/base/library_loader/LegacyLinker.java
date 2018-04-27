package org.chromium.base.library_loader;

import android.os.Bundle;
import android.os.Parcel;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import org.chromium.base.Log;
import org.chromium.base.SysUtils;
import org.chromium.base.ThreadUtils;
import org.chromium.base.annotations.CalledByNative;
import org.chromium.base.annotations.MainDex;
import org.chromium.base.library_loader.Linker.LibInfo;

@MainDex
class LegacyLinker extends Linker {
    static final /* synthetic */ boolean $assertionsDisabled = (!LegacyLinker.class.desiredAssertionStatus() ? true : $assertionsDisabled);
    private static final String TAG = "LibraryLoader";
    private long mBaseLoadAddress = -1;
    private boolean mBrowserUsesSharedRelro;
    private long mCurrentLoadAddress = -1;
    private boolean mInBrowserProcess = true;
    private boolean mInitialized;
    private HashMap<String, LibInfo> mLoadedLibraries;
    private boolean mPrepareLibraryLoadCalled;
    private Bundle mSharedRelros;
    private boolean mWaitForSharedRelros;

    /* renamed from: org.chromium.base.library_loader.LegacyLinker$1 */
    class AnonymousClass1 implements Runnable {
        final /* synthetic */ long val$opaque;

        AnonymousClass1(long j) {
            this.val$opaque = j;
        }

        public void run() {
            LegacyLinker.nativeRunCallbackOnUiThread(this.val$opaque);
        }
    }

    private static native boolean nativeCreateSharedRelro(String str, long j, LibInfo libInfo);

    private static native boolean nativeLoadLibrary(String str, long j, LibInfo libInfo);

    private static native boolean nativeLoadLibraryInZipFile(@Nullable String str, String str2, long j, LibInfo libInfo);

    private static native void nativeRunCallbackOnUiThread(long j);

    private static native boolean nativeUseSharedRelro(String str, LibInfo libInfo);

    private LegacyLinker() {
    }

    static Linker create() {
        return new LegacyLinker();
    }

    private void ensureInitializedLocked() {
        if (!$assertionsDisabled && !Thread.holdsLock(this.mLock)) {
            throw new AssertionError();
        } else if (!this.mInitialized && NativeLibraries.sUseLinker) {
            Linker.loadLinkerJniLibrary();
            if (this.mMemoryDeviceConfig == 0) {
                if (SysUtils.isLowEndDevice()) {
                    this.mMemoryDeviceConfig = 1;
                } else {
                    this.mMemoryDeviceConfig = 2;
                }
            }
            switch (1) {
                case null:
                    this.mBrowserUsesSharedRelro = $assertionsDisabled;
                    break;
                case 1:
                    if (this.mMemoryDeviceConfig != 1) {
                        this.mBrowserUsesSharedRelro = $assertionsDisabled;
                        break;
                    }
                    this.mBrowserUsesSharedRelro = true;
                    Log.w(TAG, "Low-memory device: shared RELROs used in all processes", new Object[0]);
                    break;
                case 2:
                    Log.w(TAG, "Beware: shared RELROs used in all processes!", new Object[0]);
                    this.mBrowserUsesSharedRelro = true;
                    break;
                default:
                    Log.wtf(TAG, "FATAL: illegal shared RELRO config", new Object[0]);
                    throw new AssertionError();
            }
            this.mInitialized = true;
        }
    }

    public boolean isUsingBrowserSharedRelros() {
        boolean z;
        synchronized (this.mLock) {
            ensureInitializedLocked();
            z = (this.mInBrowserProcess && this.mBrowserUsesSharedRelro) ? true : $assertionsDisabled;
        }
        return z;
    }

    public void prepareLibraryLoad() {
        synchronized (this.mLock) {
            ensureInitializedLocked();
            this.mPrepareLibraryLoadCalled = true;
            if (this.mInBrowserProcess) {
                setupBaseLoadAddressLocked();
            }
        }
    }

    public void finishLibraryLoad() {
        synchronized (this.mLock) {
            ensureInitializedLocked();
            if (this.mLoadedLibraries != null) {
                if (this.mInBrowserProcess) {
                    this.mSharedRelros = createBundleFromLibInfoMap(this.mLoadedLibraries);
                    if (this.mBrowserUsesSharedRelro) {
                        useSharedRelrosLocked(this.mSharedRelros);
                    }
                }
                if (this.mWaitForSharedRelros) {
                    if ($assertionsDisabled || !this.mInBrowserProcess) {
                        while (this.mSharedRelros == null) {
                            try {
                                this.mLock.wait();
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        }
                        useSharedRelrosLocked(this.mSharedRelros);
                        this.mSharedRelros.clear();
                        this.mSharedRelros = null;
                    } else {
                        throw new AssertionError();
                    }
                }
            }
            if (NativeLibraries.sEnableLinkerTests) {
                runTestRunnerClassForTesting(this.mMemoryDeviceConfig, this.mInBrowserProcess);
            }
        }
    }

    public void useSharedRelros(Bundle bundle) {
        Bundle clonedBundle = null;
        if (bundle != null) {
            bundle.setClassLoader(LibInfo.class.getClassLoader());
            clonedBundle = new Bundle(LibInfo.class.getClassLoader());
            Parcel parcel = Parcel.obtain();
            bundle.writeToParcel(parcel, 0);
            parcel.setDataPosition(0);
            clonedBundle.readFromParcel(parcel);
            parcel.recycle();
        }
        synchronized (this.mLock) {
            this.mSharedRelros = clonedBundle;
            this.mLock.notifyAll();
        }
    }

    public Bundle getSharedRelros() {
        Bundle bundle;
        synchronized (this.mLock) {
            if (this.mInBrowserProcess) {
                bundle = this.mSharedRelros;
            } else {
                bundle = null;
            }
        }
        return bundle;
    }

    public void disableSharedRelros() {
        synchronized (this.mLock) {
            ensureInitializedLocked();
            this.mInBrowserProcess = $assertionsDisabled;
            this.mWaitForSharedRelros = $assertionsDisabled;
            this.mBrowserUsesSharedRelro = $assertionsDisabled;
        }
    }

    public void initServiceProcess(long baseLoadAddress) {
        synchronized (this.mLock) {
            ensureInitializedLocked();
            this.mInBrowserProcess = $assertionsDisabled;
            this.mBrowserUsesSharedRelro = $assertionsDisabled;
            this.mWaitForSharedRelros = true;
            this.mBaseLoadAddress = baseLoadAddress;
            this.mCurrentLoadAddress = baseLoadAddress;
        }
    }

    public long getBaseLoadAddress() {
        long j;
        synchronized (this.mLock) {
            ensureInitializedLocked();
            if (this.mInBrowserProcess) {
                setupBaseLoadAddressLocked();
                j = this.mBaseLoadAddress;
            } else {
                Log.w(TAG, "Shared RELRO sections are disabled in this process!", new Object[0]);
                j = 0;
            }
        }
        return j;
    }

    private void setupBaseLoadAddressLocked() {
        if (!$assertionsDisabled && !Thread.holdsLock(this.mLock)) {
            throw new AssertionError();
        } else if (this.mBaseLoadAddress == -1) {
            this.mBaseLoadAddress = getRandomBaseLoadAddress();
            this.mCurrentLoadAddress = this.mBaseLoadAddress;
            if (this.mBaseLoadAddress == 0) {
                Log.w(TAG, "Disabling shared RELROs due address space pressure", new Object[0]);
                this.mBrowserUsesSharedRelro = $assertionsDisabled;
                this.mWaitForSharedRelros = $assertionsDisabled;
            }
        }
    }

    private void dumpBundle(Bundle bundle) {
    }

    private void useSharedRelrosLocked(Bundle bundle) {
        if (!$assertionsDisabled && !Thread.holdsLock(this.mLock)) {
            throw new AssertionError();
        } else if (bundle != null && this.mLoadedLibraries != null) {
            HashMap<String, LibInfo> relroMap = createLibInfoMapFromBundle(bundle);
            for (Entry<String, LibInfo> entry : relroMap.entrySet()) {
                String libName = (String) entry.getKey();
                if (!nativeUseSharedRelro(libName, (LibInfo) entry.getValue())) {
                    Log.w(TAG, "Could not use shared RELRO section for " + libName, new Object[0]);
                }
            }
            if (!this.mInBrowserProcess) {
                closeLibInfoMap(relroMap);
            }
        }
    }

    void loadLibraryImpl(@Nullable String zipFilePath, String libFilePath, boolean isFixedAddressPermitted) {
        synchronized (this.mLock) {
            ensureInitializedLocked();
            if ($assertionsDisabled || this.mPrepareLibraryLoadCalled) {
                if (this.mLoadedLibraries == null) {
                    this.mLoadedLibraries = new HashMap();
                }
                if (this.mLoadedLibraries.containsKey(libFilePath)) {
                    return;
                }
                String errorMessage;
                LibInfo libInfo = new LibInfo();
                long loadAddress = 0;
                if (isFixedAddressPermitted && ((this.mInBrowserProcess && this.mBrowserUsesSharedRelro) || this.mWaitForSharedRelros)) {
                    loadAddress = this.mCurrentLoadAddress;
                    if (loadAddress > this.mBaseLoadAddress + 201326592) {
                        errorMessage = "Load address outside reservation, for: " + libFilePath;
                        Log.e(TAG, errorMessage, new Object[0]);
                        throw new UnsatisfiedLinkError(errorMessage);
                    }
                }
                String sharedRelRoName = libFilePath;
                if (zipFilePath != null) {
                    if (nativeLoadLibraryInZipFile(zipFilePath, libFilePath, loadAddress, libInfo)) {
                        sharedRelRoName = zipFilePath;
                    } else {
                        errorMessage = "Unable to load library: " + libFilePath + ", in: " + zipFilePath;
                        Log.e(TAG, errorMessage, new Object[0]);
                        throw new UnsatisfiedLinkError(errorMessage);
                    }
                } else if (!nativeLoadLibrary(libFilePath, loadAddress, libInfo)) {
                    errorMessage = "Unable to load library: " + libFilePath;
                    Log.e(TAG, errorMessage, new Object[0]);
                    throw new UnsatisfiedLinkError(errorMessage);
                }
                if (NativeLibraries.sEnableLinkerTests) {
                    String tag;
                    if (this.mInBrowserProcess) {
                        tag = "BROWSER_LIBRARY_ADDRESS";
                    } else {
                        tag = "RENDERER_LIBRARY_ADDRESS";
                    }
                    Log.i(TAG, String.format(Locale.US, "%s: %s %x", new Object[]{tag, libFilePath, Long.valueOf(libInfo.mLoadAddress)}), new Object[0]);
                }
                if (this.mInBrowserProcess && !nativeCreateSharedRelro(sharedRelRoName, this.mCurrentLoadAddress, libInfo)) {
                    Log.w(TAG, String.format(Locale.US, "Could not create shared RELRO for %s at %x", new Object[]{libFilePath, Long.valueOf(this.mCurrentLoadAddress)}), new Object[0]);
                }
                if (!(loadAddress == 0 || this.mCurrentLoadAddress == 0)) {
                    this.mCurrentLoadAddress = (libInfo.mLoadAddress + libInfo.mLoadSize) + 16777216;
                }
                this.mLoadedLibraries.put(sharedRelRoName, libInfo);
                return;
            }
            throw new AssertionError();
        }
    }

    @CalledByNative
    public static void postCallbackOnMainThread(long opaque) {
        ThreadUtils.postOnUiThread(new AnonymousClass1(opaque));
    }
}
