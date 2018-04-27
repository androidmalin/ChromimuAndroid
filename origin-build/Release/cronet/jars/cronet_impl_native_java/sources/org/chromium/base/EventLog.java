package org.chromium.base;

import org.chromium.base.annotations.CalledByNative;
import org.chromium.base.annotations.JNINamespace;

@JNINamespace("base::android")
public class EventLog {
    @CalledByNative
    public static void writeEvent(int tag, int value) {
        android.util.EventLog.writeEvent(tag, value);
    }
}
