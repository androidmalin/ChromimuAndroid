package org.chromium.base;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build.VERSION;
import org.chromium.base.annotations.CalledByNative;
import org.chromium.base.annotations.JNINamespace;
import org.chromium.base.metrics.CachedMetrics.BooleanHistogramSample;

@JNINamespace("base::android")
public class SysUtils {
    static final /* synthetic */ boolean $assertionsDisabled;
    private static final int ANDROID_LOW_MEMORY_DEVICE_THRESHOLD_MB = 512;
    private static final int ANDROID_O_LOW_MEMORY_DEVICE_THRESHOLD_MB = 1024;
    private static final String TAG = "SysUtils";
    private static Integer sAmountOfPhysicalMemoryKB;
    private static Boolean sLowEndDevice;
    private static BooleanHistogramSample sLowEndMatches = new BooleanHistogramSample("Android.SysUtilsLowEndMatches");

    private static native void nativeLogPageFaultCountToTracing();

    static {
        boolean z;
        if (SysUtils.class.desiredAssertionStatus()) {
            z = $assertionsDisabled;
        } else {
            z = true;
        }
        $assertionsDisabled = z;
    }

    private SysUtils() {
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static int detectAmountOfPhysicalMemoryKB() {
        /*
        r8 = "^MemTotal:\\s+([0-9]+) kB$";
        r5 = java.util.regex.Pattern.compile(r8);
        r4 = android.os.StrictMode.allowThreadDiskReads();
        r1 = new java.io.FileReader;	 Catch:{ Exception -> 0x006d }
        r8 = "/proc/meminfo";
        r1.<init>(r8);	 Catch:{ Exception -> 0x006d }
        r6 = new java.io.BufferedReader;	 Catch:{ all -> 0x0068 }
        r6.<init>(r1);	 Catch:{ all -> 0x0068 }
    L_0x0016:
        r2 = r6.readLine();	 Catch:{ all -> 0x0063 }
        if (r2 != 0) goto L_0x002e;
    L_0x001c:
        r8 = "SysUtils";
        r9 = "/proc/meminfo lacks a MemTotal entry?";
        android.util.Log.w(r8, r9);	 Catch:{ all -> 0x0063 }
    L_0x0023:
        r6.close();	 Catch:{ all -> 0x0068 }
        r1.close();	 Catch:{ Exception -> 0x006d }
        android.os.StrictMode.setThreadPolicy(r4);
    L_0x002c:
        r7 = 0;
    L_0x002d:
        return r7;
    L_0x002e:
        r3 = r5.matcher(r2);	 Catch:{ all -> 0x0063 }
        r8 = r3.find();	 Catch:{ all -> 0x0063 }
        if (r8 == 0) goto L_0x0016;
    L_0x0038:
        r8 = 1;
        r8 = r3.group(r8);	 Catch:{ all -> 0x0063 }
        r7 = java.lang.Integer.parseInt(r8);	 Catch:{ all -> 0x0063 }
        r8 = 1024; // 0x400 float:1.435E-42 double:5.06E-321;
        if (r7 > r8) goto L_0x0079;
    L_0x0045:
        r8 = "SysUtils";
        r9 = new java.lang.StringBuilder;	 Catch:{ all -> 0x0063 }
        r9.<init>();	 Catch:{ all -> 0x0063 }
        r10 = "Invalid /proc/meminfo total size in kB: ";
        r9 = r9.append(r10);	 Catch:{ all -> 0x0063 }
        r10 = 1;
        r10 = r3.group(r10);	 Catch:{ all -> 0x0063 }
        r9 = r9.append(r10);	 Catch:{ all -> 0x0063 }
        r9 = r9.toString();	 Catch:{ all -> 0x0063 }
        android.util.Log.w(r8, r9);	 Catch:{ all -> 0x0063 }
        goto L_0x0023;
    L_0x0063:
        r8 = move-exception;
        r6.close();	 Catch:{ all -> 0x0068 }
        throw r8;	 Catch:{ all -> 0x0068 }
    L_0x0068:
        r8 = move-exception;
        r1.close();	 Catch:{ Exception -> 0x006d }
        throw r8;	 Catch:{ Exception -> 0x006d }
    L_0x006d:
        r0 = move-exception;
        r8 = "SysUtils";
        r9 = "Cannot get total physical size from /proc/meminfo";
        android.util.Log.w(r8, r9, r0);	 Catch:{ all -> 0x0083 }
        android.os.StrictMode.setThreadPolicy(r4);
        goto L_0x002c;
    L_0x0079:
        r6.close();	 Catch:{ all -> 0x0068 }
        r1.close();	 Catch:{ Exception -> 0x006d }
        android.os.StrictMode.setThreadPolicy(r4);
        goto L_0x002d;
    L_0x0083:
        r8 = move-exception;
        android.os.StrictMode.setThreadPolicy(r4);
        throw r8;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.chromium.base.SysUtils.detectAmountOfPhysicalMemoryKB():int");
    }

    @CalledByNative
    public static boolean isLowEndDevice() {
        if (sLowEndDevice == null) {
            sLowEndDevice = Boolean.valueOf(detectLowEndDevice());
        }
        return sLowEndDevice.booleanValue();
    }

    public static int amountOfPhysicalMemoryKB() {
        if (sAmountOfPhysicalMemoryKB == null) {
            sAmountOfPhysicalMemoryKB = Integer.valueOf(detectAmountOfPhysicalMemoryKB());
        }
        return sAmountOfPhysicalMemoryKB.intValue();
    }

    @CalledByNative
    public static boolean isCurrentlyLowMemory() {
        ActivityManager am = (ActivityManager) ContextUtils.getApplicationContext().getSystemService("activity");
        MemoryInfo info = new MemoryInfo();
        am.getMemoryInfo(info);
        return info.lowMemory;
    }

    @VisibleForTesting
    public static void resetForTesting() {
        sLowEndDevice = null;
        sAmountOfPhysicalMemoryKB = null;
    }

    public static boolean hasCamera(Context context) {
        PackageManager pm = context.getPackageManager();
        boolean hasCamera = pm.hasSystemFeature("android.hardware.camera");
        if (VERSION.SDK_INT >= 17) {
            return hasCamera | pm.hasSystemFeature("android.hardware.camera.any");
        }
        return hasCamera;
    }

    @TargetApi(19)
    private static boolean detectLowEndDevice() {
        boolean z = true;
        if (!$assertionsDisabled && !CommandLine.isInitialized()) {
            throw new AssertionError();
        } else if (CommandLine.getInstance().hasSwitch(BaseSwitches.ENABLE_LOW_END_DEVICE_MODE)) {
            return true;
        } else {
            if (CommandLine.getInstance().hasSwitch(BaseSwitches.DISABLE_LOW_END_DEVICE_MODE)) {
                return $assertionsDisabled;
            }
            boolean isLowEnd;
            sAmountOfPhysicalMemoryKB = Integer.valueOf(detectAmountOfPhysicalMemoryKB());
            if (sAmountOfPhysicalMemoryKB.intValue() <= 0) {
                isLowEnd = $assertionsDisabled;
            } else if (VERSION.SDK_INT >= 26) {
                isLowEnd = sAmountOfPhysicalMemoryKB.intValue() / ANDROID_O_LOW_MEMORY_DEVICE_THRESHOLD_MB <= ANDROID_O_LOW_MEMORY_DEVICE_THRESHOLD_MB ? true : $assertionsDisabled;
            } else {
                isLowEnd = sAmountOfPhysicalMemoryKB.intValue() / ANDROID_O_LOW_MEMORY_DEVICE_THRESHOLD_MB <= ANDROID_LOW_MEMORY_DEVICE_THRESHOLD_MB ? true : $assertionsDisabled;
            }
            Context appContext = ContextUtils.getApplicationContext();
            boolean isLowRam = $assertionsDisabled;
            if (appContext != null && VERSION.SDK_INT >= 19) {
                isLowRam = ((ActivityManager) ContextUtils.getApplicationContext().getSystemService("activity")).isLowRamDevice();
            }
            BooleanHistogramSample booleanHistogramSample = sLowEndMatches;
            if (isLowEnd != isLowRam) {
                z = $assertionsDisabled;
            }
            booleanHistogramSample.record(z);
            return isLowEnd;
        }
    }

    public static void logPageFaultCountToTracing() {
        nativeLogPageFaultCountToTracing();
    }
}
