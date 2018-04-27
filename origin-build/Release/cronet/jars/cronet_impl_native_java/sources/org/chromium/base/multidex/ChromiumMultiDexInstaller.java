package org.chromium.base.multidex;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build.VERSION;
import android.os.Process;
import android.support.multidex.MultiDex;
import java.lang.reflect.InvocationTargetException;
import org.chromium.base.Log;
import org.chromium.base.VisibleForTesting;

public class ChromiumMultiDexInstaller {
    private static final String IGNORE_MULTIDEX_KEY = ".ignore_multidex";
    private static final String TAG = "base_multidex";

    @VisibleForTesting
    public static void install(Context context) {
        if (VERSION.SDK_INT >= 21 || shouldInstallMultiDex(context)) {
            MultiDex.install(context);
            Log.i(TAG, "Completed multidex installation.", new Object[0]);
            return;
        }
        Log.i(TAG, "Skipping multidex installation: not needed for process.", new Object[0]);
    }

    private static String getProcessName(Context context) {
        try {
            int pid = Process.myPid();
            for (RunningAppProcessInfo processInfo : ((ActivityManager) context.getSystemService("activity")).getRunningAppProcesses()) {
                if (processInfo.pid == pid) {
                    return processInfo.processName;
                }
            }
            return null;
        } catch (SecurityException e) {
            return null;
        }
    }

    private static boolean shouldInstallMultiDex(Context context) {
        try {
            Object retVal = Process.class.getMethod("isIsolated", new Class[0]).invoke(null, new Object[0]);
            if (retVal != null && (retVal instanceof Boolean) && ((Boolean) retVal).booleanValue()) {
                return false;
            }
        } catch (IllegalAccessException e) {
        } catch (IllegalArgumentException e2) {
        } catch (InvocationTargetException e3) {
        } catch (NoSuchMethodException e4) {
        }
        String currentProcessName = getProcessName(context);
        if (currentProcessName == null) {
            return true;
        }
        try {
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(context.getPackageName(), 128);
            if (appInfo == null || appInfo.metaData == null || !appInfo.metaData.getBoolean(currentProcessName + IGNORE_MULTIDEX_KEY, false)) {
                return true;
            }
            return false;
        } catch (NameNotFoundException e5) {
            return true;
        }
    }
}
