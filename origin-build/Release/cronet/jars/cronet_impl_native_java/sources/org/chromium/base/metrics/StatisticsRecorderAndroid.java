package org.chromium.base.metrics;

import org.chromium.base.annotations.JNINamespace;

@JNINamespace("base::android")
public final class StatisticsRecorderAndroid {
    private static native String nativeToJson(int i);

    private StatisticsRecorderAndroid() {
    }

    public static String toJson(int verbosityLevel) {
        return nativeToJson(verbosityLevel);
    }
}
