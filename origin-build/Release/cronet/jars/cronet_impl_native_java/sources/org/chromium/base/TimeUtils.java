package org.chromium.base;

import org.chromium.base.annotations.JNINamespace;
import org.chromium.base.annotations.MainDex;

@MainDex
@JNINamespace("base::android")
public class TimeUtils {
    public static native long nativeGetTimeTicksNowUs();

    private TimeUtils() {
    }
}
