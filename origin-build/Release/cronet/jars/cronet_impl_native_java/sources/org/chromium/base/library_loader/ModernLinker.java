package org.chromium.base.library_loader;

import android.os.Bundle;
import java.util.HashMap;
import java.util.Locale;
import javax.annotation.Nullable;
import org.chromium.base.Log;
import org.chromium.base.PathUtils;
import org.chromium.base.library_loader.Linker.LibInfo;

class ModernLinker extends Linker {
    static final /* synthetic */ boolean $assertionsDisabled = (!ModernLinker.class.desiredAssertionStatus() ? true : $assertionsDisabled);
    private static final String TAG = "LibraryLoader";
    private long mBaseLoadAddress = -1;
    private long mCurrentLoadAddress = -1;
    private boolean mInBrowserProcess = true;
    private boolean mInitialized;
    private HashMap<String, LibInfo> mLoadedLibraries;
    private boolean mPrepareLibraryLoadCalled;
    private HashMap<String, LibInfo> mSharedRelros;
    private Bundle mSharedRelrosBundle;
    private boolean mWaitForSharedRelros;

    private static native boolean nativeCreateSharedRelro(String str, long j, String str2, LibInfo libInfo);

    private static native String nativeGetCpuAbi();

    private static native boolean nativeLoadLibrary(String str, long j, LibInfo libInfo);

    private ModernLinker() {
    }

    static Linker create() {
        return new ModernLinker();
    }

    private void ensureInitializedLocked() {
        if (!$assertionsDisabled && !Thread.holdsLock(this.mLock)) {
            throw new AssertionError();
        } else if (!$assertionsDisabled && !NativeLibraries.sUseLinker) {
            throw new AssertionError();
        } else if (!this.mInitialized) {
            Linker.loadLinkerJniLibrary();
            this.mInitialized = true;
        }
    }

    public boolean isUsingBrowserSharedRelros() {
        return $assertionsDisabled;
    }

    public void prepareLibraryLoad() {
        if ($assertionsDisabled || NativeLibraries.sUseLinker) {
            synchronized (this.mLock) {
                if ($assertionsDisabled || !this.mPrepareLibraryLoadCalled) {
                    ensureInitializedLocked();
                    if (this.mInBrowserProcess) {
                        setupBaseLoadAddressLocked();
                        this.mSharedRelros = new HashMap();
                    }
                    this.mLoadedLibraries = new HashMap();
                    this.mCurrentLoadAddress = this.mBaseLoadAddress;
                    this.mPrepareLibraryLoadCalled = true;
                } else {
                    throw new AssertionError();
                }
            }
            return;
        }
        throw new AssertionError();
    }

    public void finishLibraryLoad() {
        synchronized (this.mLock) {
            if ($assertionsDisabled || this.mPrepareLibraryLoadCalled) {
                if (!(this.mInBrowserProcess || this.mSharedRelros == null)) {
                    closeLibInfoMap(this.mSharedRelros);
                    this.mSharedRelros = null;
                }
                if (NativeLibraries.sEnableLinkerTests) {
                    runTestRunnerClassForTesting(0, this.mInBrowserProcess);
                }
            } else {
                throw new AssertionError();
            }
        }
    }

    private void waitForSharedRelrosLocked() {
        if (!$assertionsDisabled && !Thread.holdsLock(this.mLock)) {
            throw new AssertionError();
        } else if (this.mSharedRelros == null) {
            while (this.mSharedRelros == null) {
                try {
                    this.mLock.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    public void useSharedRelros(Bundle bundle) {
        synchronized (this.mLock) {
            this.mSharedRelros = createLibInfoMapFromBundle(bundle);
            this.mLock.notifyAll();
        }
    }

    public Bundle getSharedRelros() {
        Bundle bundle;
        synchronized (this.mLock) {
            if (this.mInBrowserProcess) {
                if (this.mSharedRelrosBundle == null && this.mSharedRelros != null) {
                    this.mSharedRelrosBundle = createBundleFromLibInfoMap(this.mSharedRelros);
                }
                bundle = this.mSharedRelrosBundle;
            } else {
                bundle = null;
            }
        }
        return bundle;
    }

    public void disableSharedRelros() {
        synchronized (this.mLock) {
            this.mInBrowserProcess = $assertionsDisabled;
            this.mWaitForSharedRelros = $assertionsDisabled;
        }
    }

    public void initServiceProcess(long baseLoadAddress) {
        synchronized (this.mLock) {
            if ($assertionsDisabled || !this.mPrepareLibraryLoadCalled) {
                this.mInBrowserProcess = $assertionsDisabled;
                this.mWaitForSharedRelros = true;
                this.mBaseLoadAddress = baseLoadAddress;
            } else {
                throw new AssertionError();
            }
        }
    }

    public long getBaseLoadAddress() {
        long j;
        synchronized (this.mLock) {
            ensureInitializedLocked();
            setupBaseLoadAddressLocked();
            j = this.mBaseLoadAddress;
        }
        return j;
    }

    private void setupBaseLoadAddressLocked() {
        if ($assertionsDisabled || Thread.holdsLock(this.mLock)) {
            if (this.mBaseLoadAddress == -1) {
                this.mBaseLoadAddress = getRandomBaseLoadAddress();
            }
            if (this.mBaseLoadAddress == 0) {
                Log.w(TAG, "Disabling shared RELROs due address space pressure", new Object[0]);
                this.mWaitForSharedRelros = $assertionsDisabled;
                return;
            }
            return;
        }
        throw new AssertionError();
    }

    void loadLibraryImpl(@Nullable String zipFilePath, String libFilePath, boolean isFixedAddressPermitted) {
        synchronized (this.mLock) {
            if ($assertionsDisabled || this.mPrepareLibraryLoadCalled) {
                String dlopenExtPath;
                if (zipFilePath != null) {
                    dlopenExtPath = zipFilePath + "!/lib/" + nativeGetCpuAbi() + "/crazy." + libFilePath;
                } else {
                    dlopenExtPath = libFilePath;
                }
                if (this.mLoadedLibraries.containsKey(dlopenExtPath)) {
                    return;
                }
                String errorMessage;
                long loadAddress = 0;
                if (!this.mInBrowserProcess && this.mWaitForSharedRelros && isFixedAddressPermitted) {
                    loadAddress = this.mCurrentLoadAddress;
                    if (loadAddress > this.mBaseLoadAddress + 201326592) {
                        errorMessage = "Load address outside reservation, for: " + libFilePath;
                        Log.e(TAG, errorMessage, new Object[0]);
                        throw new UnsatisfiedLinkError(errorMessage);
                    }
                }
                LibInfo libInfo = new LibInfo();
                if (this.mInBrowserProcess && this.mCurrentLoadAddress != 0) {
                    String relroPath = PathUtils.getDataDirectory() + "/RELRO:" + libFilePath;
                    if (nativeCreateSharedRelro(dlopenExtPath, this.mCurrentLoadAddress, relroPath, libInfo)) {
                        this.mSharedRelros.put(dlopenExtPath, libInfo);
                    } else {
                        Log.w(TAG, "Unable to create shared relro: " + relroPath, new Object[0]);
                    }
                } else if (!(this.mInBrowserProcess || this.mCurrentLoadAddress == 0 || !this.mWaitForSharedRelros)) {
                    waitForSharedRelrosLocked();
                    if (this.mSharedRelros.containsKey(dlopenExtPath)) {
                        libInfo = (LibInfo) this.mSharedRelros.get(dlopenExtPath);
                    }
                }
                if (nativeLoadLibrary(dlopenExtPath, loadAddress, libInfo)) {
                    if (NativeLibraries.sEnableLinkerTests) {
                        String tag;
                        if (this.mInBrowserProcess) {
                            tag = "BROWSER_LIBRARY_ADDRESS";
                        } else {
                            tag = "RENDERER_LIBRARY_ADDRESS";
                        }
                        Log.i(TAG, String.format(Locale.US, "%s: %s %x", new Object[]{tag, libFilePath, Long.valueOf(libInfo.mLoadAddress)}), new Object[0]);
                    }
                    if (!(loadAddress == 0 || this.mCurrentLoadAddress == 0)) {
                        this.mCurrentLoadAddress = (libInfo.mLoadAddress + libInfo.mLoadSize) + 16777216;
                    }
                    this.mLoadedLibraries.put(dlopenExtPath, libInfo);
                    return;
                }
                errorMessage = "Unable to load library: " + dlopenExtPath;
                Log.e(TAG, errorMessage, new Object[0]);
                throw new UnsatisfiedLinkError(errorMessage);
            }
            throw new AssertionError();
        }
    }
}
