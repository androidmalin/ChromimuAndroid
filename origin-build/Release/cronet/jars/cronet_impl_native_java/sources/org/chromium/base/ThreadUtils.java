package org.chromium.base;

import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import org.chromium.base.annotations.CalledByNative;

public class ThreadUtils {
    static final /* synthetic */ boolean $assertionsDisabled = (!ThreadUtils.class.desiredAssertionStatus());
    private static final Object sLock = new Object();
    private static boolean sThreadAssertsDisabled;
    private static Handler sUiThreadHandler;
    private static boolean sWillOverride;

    public static void setWillOverrideUiThread() {
        synchronized (sLock) {
            sWillOverride = true;
        }
    }

    @VisibleForTesting
    public static void setUiThread(Looper looper) {
        synchronized (sLock) {
            if (looper == null) {
                sUiThreadHandler = null;
            } else if (sUiThreadHandler == null || sUiThreadHandler.getLooper() == looper) {
                sUiThreadHandler = new Handler(looper);
            } else {
                throw new RuntimeException("UI thread looper is already set to " + sUiThreadHandler.getLooper() + " (Main thread looper is " + Looper.getMainLooper() + "), cannot set to new looper " + looper);
            }
        }
    }

    private static Handler getUiThreadHandler() {
        Handler handler;
        synchronized (sLock) {
            if (sUiThreadHandler == null) {
                if (sWillOverride) {
                    throw new RuntimeException("Did not yet override the UI thread");
                }
                sUiThreadHandler = new Handler(Looper.getMainLooper());
            }
            handler = sUiThreadHandler;
        }
        return handler;
    }

    public static void runOnUiThreadBlocking(Runnable r) {
        if (runningOnUiThread()) {
            r.run();
            return;
        }
        FutureTask task = new FutureTask(r, null);
        postOnUiThread(task);
        try {
            task.get();
        } catch (Exception e) {
            throw new RuntimeException("Exception occurred while waiting for runnable", e);
        }
    }

    @VisibleForTesting
    public static <T> T runOnUiThreadBlockingNoException(Callable<T> c) {
        try {
            return runOnUiThreadBlocking((Callable) c);
        } catch (ExecutionException e) {
            throw new RuntimeException("Error occurred waiting for callable", e);
        }
    }

    public static <T> T runOnUiThreadBlocking(Callable<T> c) throws ExecutionException {
        FutureTask task = new FutureTask(c);
        runOnUiThread(task);
        try {
            return task.get();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted waiting for callable", e);
        }
    }

    public static <T> FutureTask<T> runOnUiThread(FutureTask<T> task) {
        if (runningOnUiThread()) {
            task.run();
        } else {
            postOnUiThread((FutureTask) task);
        }
        return task;
    }

    public static <T> FutureTask<T> runOnUiThread(Callable<T> c) {
        return runOnUiThread(new FutureTask(c));
    }

    public static void runOnUiThread(Runnable r) {
        if (runningOnUiThread()) {
            r.run();
        } else {
            getUiThreadHandler().post(r);
        }
    }

    public static <T> FutureTask<T> postOnUiThread(FutureTask<T> task) {
        getUiThreadHandler().post(task);
        return task;
    }

    public static void postOnUiThread(Runnable task) {
        getUiThreadHandler().post(task);
    }

    @VisibleForTesting
    public static void postOnUiThreadDelayed(Runnable task, long delayMillis) {
        getUiThreadHandler().postDelayed(task, delayMillis);
    }

    public static void assertOnUiThread() {
        if (!sThreadAssertsDisabled && !$assertionsDisabled && !runningOnUiThread()) {
            throw new AssertionError("Must be called on the UI thread.");
        }
    }

    public static void checkUiThread() {
        if (!sThreadAssertsDisabled && !runningOnUiThread()) {
            throw new IllegalStateException("Must be called on the UI thread.");
        }
    }

    public static void assertOnBackgroundThread() {
        if (!sThreadAssertsDisabled && !$assertionsDisabled && runningOnUiThread()) {
            throw new AssertionError("Must be called on a thread other than UI.");
        }
    }

    public static void setThreadAssertsDisabledForTesting(boolean disabled) {
        sThreadAssertsDisabled = disabled;
    }

    public static boolean runningOnUiThread() {
        return getUiThreadHandler().getLooper() == Looper.myLooper();
    }

    public static Looper getUiThreadLooper() {
        return getUiThreadHandler().getLooper();
    }

    @CalledByNative
    public static void setThreadPriorityAudio(int tid) {
        Process.setThreadPriority(tid, -16);
    }

    @CalledByNative
    private static boolean isThreadPriorityAudio(int tid) {
        return Process.getThreadPriority(tid) == -16;
    }
}
