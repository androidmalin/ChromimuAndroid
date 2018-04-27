package org.chromium.base;

import org.chromium.base.annotations.CalledByNative;

public interface Callback<T> {

    public static abstract class Helper {
        @CalledByNative("Helper")
        static void onObjectResultFromNative(Callback callback, Object result) {
            callback.onResult(result);
        }

        @CalledByNative("Helper")
        static void onBooleanResultFromNative(Callback callback, boolean result) {
            callback.onResult(Boolean.valueOf(result));
        }

        @CalledByNative("Helper")
        static void onIntResultFromNative(Callback callback, int result) {
            callback.onResult(Integer.valueOf(result));
        }
    }

    void onResult(T t);
}
