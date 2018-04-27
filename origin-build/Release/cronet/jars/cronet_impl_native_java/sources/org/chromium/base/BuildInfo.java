package org.chromium.base;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Build.VERSION;
import android.text.TextUtils;
import org.chromium.base.annotations.CalledByNative;

public class BuildInfo {
    public static final int ABI_NAME_INDEX = 14;
    public static final int ANDROID_BUILD_FP_INDEX = 11;
    public static final int ANDROID_BUILD_ID_INDEX = 2;
    public static final int BRAND_INDEX = 0;
    public static final int DEVICE_INDEX = 1;
    public static final int GMS_CORE_VERSION_INDEX = 12;
    public static final int INSTALLER_PACKAGE_NAME_INDEX = 13;
    private static final int MAX_FINGERPRINT_LENGTH = 128;
    public static final int MODEL_INDEX = 4;
    private static final String TAG = "BuildInfo";

    private BuildInfo() {
    }

    @CalledByNative
    public static String[] getAll() {
        try {
            String abiString;
            String packageName = ContextUtils.getApplicationContext().getPackageName();
            PackageManager pm = ContextUtils.getApplicationContext().getPackageManager();
            PackageInfo pi = pm.getPackageInfo(packageName, 0);
            String versionCode = pi.versionCode <= 0 ? "" : Integer.toString(pi.versionCode);
            String versionName = pi.versionName == null ? "" : pi.versionName;
            CharSequence label = pm.getApplicationLabel(pi.applicationInfo);
            String packageLabel = label == null ? "" : label.toString();
            String installerPackageName = pm.getInstallerPackageName(packageName);
            if (installerPackageName == null) {
                installerPackageName = "";
            }
            if (VERSION.SDK_INT >= 21) {
                abiString = TextUtils.join(", ", Build.SUPPORTED_ABIS);
            } else {
                abiString = "ABI1: " + Build.CPU_ABI + ", ABI2: " + Build.CPU_ABI2;
            }
            long version = pi.versionCode > 10 ? (long) pi.versionCode : pi.lastUpdateTime;
            String extractedFileSuffix = String.format("@%s", new Object[]{Long.toHexString(version)});
            return new String[]{Build.BRAND, Build.DEVICE, Build.ID, Build.MANUFACTURER, Build.MODEL, String.valueOf(VERSION.SDK_INT), Build.TYPE, packageLabel, packageName, versionCode, versionName, getAndroidBuildFingerprint(), getGMSVersionCode(pm), installerPackageName, abiString, extractedFileSuffix};
        } catch (NameNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getAndroidBuildFingerprint() {
        return Build.FINGERPRINT.substring(0, Math.min(Build.FINGERPRINT.length(), MAX_FINGERPRINT_LENGTH));
    }

    private static String getGMSVersionCode(PackageManager packageManager) {
        String msg = "gms versionCode not available.";
        try {
            msg = Integer.toString(packageManager.getPackageInfo("com.google.android.gms", 0).versionCode);
        } catch (NameNotFoundException e) {
            Log.d(TAG, "GMS package is not found.", e);
        }
        return msg;
    }

    public static String getPackageVersionName() {
        return getAll()[10];
    }

    public static String getExtractedFileSuffix() {
        return getAll()[15];
    }

    public static String getPackageLabel() {
        return getAll()[7];
    }

    public static String getPackageName() {
        return ContextUtils.getApplicationContext().getPackageName();
    }

    public static boolean isDebugAndroid() {
        return "eng".equals(Build.TYPE) || "userdebug".equals(Build.TYPE);
    }

    @Deprecated
    public static boolean isAtLeastOMR1() {
        return VERSION.SDK_INT >= 27;
    }

    public static boolean isAtLeastP() {
        return VERSION.CODENAME.equals("P");
    }

    @Deprecated
    public static boolean targetsAtLeastOMR1() {
        return ContextUtils.getApplicationContext().getApplicationInfo().targetSdkVersion >= 27;
    }

    public static boolean targetsAtLeastP() {
        return isAtLeastP() && ContextUtils.getApplicationContext().getApplicationInfo().targetSdkVersion == 10000;
    }
}
