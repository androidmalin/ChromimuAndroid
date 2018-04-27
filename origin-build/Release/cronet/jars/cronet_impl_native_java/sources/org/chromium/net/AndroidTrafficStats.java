package org.chromium.net;

import android.net.TrafficStats;
import android.os.Process;
import org.chromium.base.annotations.CalledByNative;
import org.chromium.base.annotations.JNINamespace;

@JNINamespace("net::android::traffic_stats")
public class AndroidTrafficStats {
    private AndroidTrafficStats() {
    }

    @CalledByNative
    private static long getTotalTxBytes() {
        long bytes = TrafficStats.getTotalTxBytes();
        return bytes != -1 ? bytes : 0;
    }

    @CalledByNative
    private static long getTotalRxBytes() {
        long bytes = TrafficStats.getTotalRxBytes();
        return bytes != -1 ? bytes : 0;
    }

    @CalledByNative
    private static long getCurrentUidTxBytes() {
        long bytes = TrafficStats.getUidTxBytes(Process.myUid());
        return bytes != -1 ? bytes : 0;
    }

    @CalledByNative
    private static long getCurrentUidRxBytes() {
        long bytes = TrafficStats.getUidRxBytes(Process.myUid());
        return bytes != -1 ? bytes : 0;
    }
}
