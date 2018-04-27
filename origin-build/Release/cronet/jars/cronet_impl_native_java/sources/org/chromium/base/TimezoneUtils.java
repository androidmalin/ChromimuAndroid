package org.chromium.base;

import android.os.StrictMode;
import android.os.StrictMode.ThreadPolicy;
import java.util.TimeZone;
import org.chromium.base.annotations.CalledByNative;
import org.chromium.base.annotations.JNINamespace;
import org.chromium.base.annotations.MainDex;

@MainDex
@JNINamespace("base::android")
class TimezoneUtils {
    private TimezoneUtils() {
    }

    @CalledByNative
    private static String getDefaultTimeZoneId() {
        ThreadPolicy oldPolicy = StrictMode.allowThreadDiskReads();
        String timezoneID = TimeZone.getDefault().getID();
        StrictMode.setThreadPolicy(oldPolicy);
        return timezoneID;
    }
}
