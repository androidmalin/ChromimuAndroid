package org.chromium.base;

import android.os.StrictMode;
import android.os.StrictMode.ThreadPolicy;
import android.os.StrictMode.VmPolicy;
import java.io.Closeable;

public final class StrictModeContext implements Closeable {
    private final ThreadPolicy mThreadPolicy;
    private final VmPolicy mVmPolicy;

    private StrictModeContext(ThreadPolicy threadPolicy, VmPolicy vmPolicy) {
        this.mThreadPolicy = threadPolicy;
        this.mVmPolicy = vmPolicy;
    }

    private StrictModeContext(ThreadPolicy threadPolicy) {
        this(threadPolicy, null);
    }

    private StrictModeContext(VmPolicy vmPolicy) {
        this(null, vmPolicy);
    }

    public static StrictModeContext allowAllVmPolicies() {
        VmPolicy oldPolicy = StrictMode.getVmPolicy();
        StrictMode.setVmPolicy(VmPolicy.LAX);
        return new StrictModeContext(oldPolicy);
    }

    public static StrictModeContext allowDiskWrites() {
        return new StrictModeContext(StrictMode.allowThreadDiskWrites());
    }

    public static StrictModeContext allowDiskReads() {
        return new StrictModeContext(StrictMode.allowThreadDiskReads());
    }

    public void close() {
        if (this.mThreadPolicy != null) {
            StrictMode.setThreadPolicy(this.mThreadPolicy);
        }
        if (this.mVmPolicy != null) {
            StrictMode.setVmPolicy(this.mVmPolicy);
        }
    }
}
