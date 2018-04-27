package org.chromium.base;

import android.annotation.SuppressLint;
import android.os.Build.VERSION;
import android.os.Process;
import android.os.StrictMode;
import android.os.StrictMode.ThreadPolicy;
import android.os.SystemClock;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.chromium.base.annotations.JNINamespace;
import org.chromium.base.annotations.MainDex;

@MainDex
@JNINamespace("base::android")
public class EarlyTraceEvent {
    @VisibleForTesting
    static final int STATE_DISABLED = 0;
    @VisibleForTesting
    static final int STATE_ENABLED = 1;
    @VisibleForTesting
    static final int STATE_FINISHED = 3;
    @VisibleForTesting
    static final int STATE_FINISHING = 2;
    private static final String TRACE_CONFIG_FILENAME = "/data/local/chrome-trace-config.json";
    @VisibleForTesting
    static List<Event> sCompletedEvents;
    private static final Object sLock = new Object();
    @VisibleForTesting
    static Map<String, Event> sPendingEvents;
    @VisibleForTesting
    static volatile int sState = 0;

    @VisibleForTesting
    static final class Event {
        static final /* synthetic */ boolean $assertionsDisabled = (!EarlyTraceEvent.class.desiredAssertionStatus());
        final long mBeginThreadTimeMillis = SystemClock.currentThreadTimeMillis();
        final long mBeginTimeNanos = elapsedRealtimeNanos();
        long mEndThreadTimeMillis;
        long mEndTimeNanos;
        final String mName;
        final int mThreadId = Process.myTid();

        Event(String name) {
            this.mName = name;
        }

        void end() {
            if (!$assertionsDisabled && this.mEndTimeNanos != 0) {
                throw new AssertionError();
            } else if ($assertionsDisabled || this.mEndThreadTimeMillis == 0) {
                this.mEndTimeNanos = elapsedRealtimeNanos();
                this.mEndThreadTimeMillis = SystemClock.currentThreadTimeMillis();
            } else {
                throw new AssertionError();
            }
        }

        @SuppressLint({"NewApi"})
        @VisibleForTesting
        static long elapsedRealtimeNanos() {
            if (VERSION.SDK_INT >= 17) {
                return SystemClock.elapsedRealtimeNanos();
            }
            return SystemClock.elapsedRealtime() * 1000000;
        }
    }

    private static native void nativeRecordEarlyEvent(String str, long j, long j2, int i, long j3);

    static void maybeEnable() {
        ThreadUtils.assertOnUiThread();
        boolean shouldEnable = false;
        ThreadPolicy oldPolicy = StrictMode.allowThreadDiskReads();
        try {
            if (CommandLine.isInitialized() && CommandLine.getInstance().hasSwitch("trace-startup")) {
                shouldEnable = true;
            } else {
                try {
                    shouldEnable = new File(TRACE_CONFIG_FILENAME).exists();
                } catch (SecurityException e) {
                }
            }
            StrictMode.setThreadPolicy(oldPolicy);
            if (shouldEnable) {
                enable();
            }
        } catch (Throwable th) {
            StrictMode.setThreadPolicy(oldPolicy);
        }
    }

    @VisibleForTesting
    static void enable() {
        synchronized (sLock) {
            if (sState != 0) {
                return;
            }
            sCompletedEvents = new ArrayList();
            sPendingEvents = new HashMap();
            sState = 1;
        }
    }

    static void disable() {
        synchronized (sLock) {
            if (enabled()) {
                sState = 2;
                maybeFinishLocked();
                return;
            }
        }
    }

    static boolean isActive() {
        int state = sState;
        if (state == 1 || state == 2) {
            return true;
        }
        return false;
    }

    static boolean enabled() {
        return sState == 1;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static void begin(java.lang.String r4) {
        /*
        r2 = enabled();
        if (r2 != 0) goto L_0x0007;
    L_0x0006:
        return;
    L_0x0007:
        r1 = new org.chromium.base.EarlyTraceEvent$Event;
        r1.<init>(r4);
        r3 = sLock;
        monitor-enter(r3);
        r2 = enabled();	 Catch:{ all -> 0x0017 }
        if (r2 != 0) goto L_0x001a;
    L_0x0015:
        monitor-exit(r3);	 Catch:{ all -> 0x0017 }
        goto L_0x0006;
    L_0x0017:
        r2 = move-exception;
        monitor-exit(r3);	 Catch:{ all -> 0x0017 }
        throw r2;
    L_0x001a:
        r2 = sPendingEvents;	 Catch:{ all -> 0x0017 }
        r0 = r2.put(r4, r1);	 Catch:{ all -> 0x0017 }
        r0 = (org.chromium.base.EarlyTraceEvent.Event) r0;	 Catch:{ all -> 0x0017 }
        monitor-exit(r3);	 Catch:{ all -> 0x0017 }
        if (r0 == 0) goto L_0x0006;
    L_0x0025:
        r2 = new java.lang.IllegalArgumentException;
        r3 = "Multiple pending trace events can't have the same name";
        r2.<init>(r3);
        throw r2;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.chromium.base.EarlyTraceEvent.begin(java.lang.String):void");
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static void end(java.lang.String r4) {
        /*
        r1 = isActive();
        if (r1 != 0) goto L_0x0007;
    L_0x0006:
        return;
    L_0x0007:
        r2 = sLock;
        monitor-enter(r2);
        r1 = isActive();	 Catch:{ all -> 0x0012 }
        if (r1 != 0) goto L_0x0015;
    L_0x0010:
        monitor-exit(r2);	 Catch:{ all -> 0x0012 }
        goto L_0x0006;
    L_0x0012:
        r1 = move-exception;
        monitor-exit(r2);	 Catch:{ all -> 0x0012 }
        throw r1;
    L_0x0015:
        r1 = sPendingEvents;	 Catch:{ all -> 0x0012 }
        r0 = r1.remove(r4);	 Catch:{ all -> 0x0012 }
        r0 = (org.chromium.base.EarlyTraceEvent.Event) r0;	 Catch:{ all -> 0x0012 }
        if (r0 != 0) goto L_0x0021;
    L_0x001f:
        monitor-exit(r2);	 Catch:{ all -> 0x0012 }
        goto L_0x0006;
    L_0x0021:
        r0.end();	 Catch:{ all -> 0x0012 }
        r1 = sCompletedEvents;	 Catch:{ all -> 0x0012 }
        r1.add(r0);	 Catch:{ all -> 0x0012 }
        r1 = sState;	 Catch:{ all -> 0x0012 }
        r3 = 2;
        if (r1 != r3) goto L_0x0031;
    L_0x002e:
        maybeFinishLocked();	 Catch:{ all -> 0x0012 }
    L_0x0031:
        monitor-exit(r2);	 Catch:{ all -> 0x0012 }
        goto L_0x0006;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.chromium.base.EarlyTraceEvent.end(java.lang.String):void");
    }

    @VisibleForTesting
    static void resetForTesting() {
        sState = 0;
        sCompletedEvents = null;
        sPendingEvents = null;
    }

    private static void maybeFinishLocked() {
        if (!sCompletedEvents.isEmpty()) {
            dumpEvents(sCompletedEvents);
            sCompletedEvents.clear();
        }
        if (sPendingEvents.isEmpty()) {
            sState = 3;
            sPendingEvents = null;
            sCompletedEvents = null;
        }
    }

    private static void dumpEvents(List<Event> events) {
        long offsetNanos = (TimeUtils.nativeGetTimeTicksNowUs() * 1000) - Event.elapsedRealtimeNanos();
        for (Event e : events) {
            nativeRecordEarlyEvent(e.mName, e.mBeginTimeNanos + offsetNanos, e.mEndTimeNanos + offsetNanos, e.mThreadId, e.mEndThreadTimeMillis - e.mBeginThreadTimeMillis);
        }
    }
}
