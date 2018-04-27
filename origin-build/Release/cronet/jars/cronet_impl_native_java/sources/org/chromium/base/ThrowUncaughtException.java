package org.chromium.base;

import org.chromium.base.annotations.CalledByNative;
import org.chromium.base.annotations.MainDex;

@MainDex
abstract class ThrowUncaughtException {
    ThrowUncaughtException() {
    }

    @CalledByNative
    private static void post() {
        ThreadUtils.postOnUiThread(new Runnable() {
            public void run() {
                throw new RuntimeException("Intentional exception not caught by JNI");
            }
        });
    }
}
