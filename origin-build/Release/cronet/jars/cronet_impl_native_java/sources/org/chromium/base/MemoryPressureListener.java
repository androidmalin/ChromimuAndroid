package org.chromium.base;

import android.app.Activity;
import android.content.ComponentCallbacks2;
import android.content.res.Configuration;
import org.chromium.base.annotations.CalledByNative;
import org.chromium.base.annotations.MainDex;

@MainDex
public class MemoryPressureListener {
    private static final String ACTION_LOW_MEMORY = "org.chromium.base.ACTION_LOW_MEMORY";
    private static final String ACTION_TRIM_MEMORY = "org.chromium.base.ACTION_TRIM_MEMORY";
    private static final String ACTION_TRIM_MEMORY_MODERATE = "org.chromium.base.ACTION_TRIM_MEMORY_MODERATE";
    private static final String ACTION_TRIM_MEMORY_RUNNING_CRITICAL = "org.chromium.base.ACTION_TRIM_MEMORY_RUNNING_CRITICAL";

    private static native void nativeOnMemoryPressure(int i);

    @CalledByNative
    private static void registerSystemCallback() {
        ContextUtils.getApplicationContext().registerComponentCallbacks(new ComponentCallbacks2() {
            public void onTrimMemory(int level) {
                MemoryPressureListener.maybeNotifyMemoryPresure(level);
            }

            public void onLowMemory() {
                MemoryPressureListener.nativeOnMemoryPressure(2);
            }

            public void onConfigurationChanged(Configuration configuration) {
            }
        });
    }

    public static boolean handleDebugIntent(Activity activity, String action) {
        if (ACTION_LOW_MEMORY.equals(action)) {
            simulateLowMemoryPressureSignal(activity);
        } else if (ACTION_TRIM_MEMORY.equals(action)) {
            simulateTrimMemoryPressureSignal(activity, 80);
        } else if (ACTION_TRIM_MEMORY_RUNNING_CRITICAL.equals(action)) {
            simulateTrimMemoryPressureSignal(activity, 15);
        } else if (!ACTION_TRIM_MEMORY_MODERATE.equals(action)) {
            return false;
        } else {
            simulateTrimMemoryPressureSignal(activity, 60);
        }
        return true;
    }

    public static void maybeNotifyMemoryPresure(int level) {
        if (level >= 80) {
            nativeOnMemoryPressure(2);
        } else if (level >= 40 || level == 15) {
            nativeOnMemoryPressure(1);
        }
    }

    private static void simulateLowMemoryPressureSignal(Activity activity) {
        activity.getApplication().onLowMemory();
        activity.onLowMemory();
    }

    private static void simulateTrimMemoryPressureSignal(Activity activity, int level) {
        activity.getApplication().onTrimMemory(level);
        activity.onTrimMemory(level);
    }
}
