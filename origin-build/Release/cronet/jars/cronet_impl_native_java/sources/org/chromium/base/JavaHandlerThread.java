package org.chromium.base;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.MessageQueue.IdleHandler;
import java.lang.Thread.State;
import java.lang.Thread.UncaughtExceptionHandler;
import org.chromium.base.annotations.CalledByNative;
import org.chromium.base.annotations.JNINamespace;

@JNINamespace("base::android")
public class JavaHandlerThread {
    static final /* synthetic */ boolean $assertionsDisabled = (!JavaHandlerThread.class.desiredAssertionStatus());
    private final HandlerThread mThread;
    private Throwable mUnhandledException;

    private native void nativeInitializeThread(long j, long j2);

    private native void nativeOnLooperStopped(long j);

    private native void nativeStopThread(long j);

    public JavaHandlerThread(String name) {
        this.mThread = new HandlerThread(name);
    }

    @CalledByNative
    private static JavaHandlerThread create(String name) {
        return new JavaHandlerThread(name);
    }

    public Looper getLooper() {
        if ($assertionsDisabled || hasStarted()) {
            return this.mThread.getLooper();
        }
        throw new AssertionError();
    }

    public void maybeStart() {
        if (!hasStarted()) {
            this.mThread.start();
        }
    }

    @CalledByNative
    private void startAndInitialize(long nativeThread, long nativeEvent) {
        maybeStart();
        final long j = nativeThread;
        final long j2 = nativeEvent;
        new Handler(this.mThread.getLooper()).post(new Runnable() {
            public void run() {
                JavaHandlerThread.this.nativeInitializeThread(j, j2);
            }
        });
    }

    @CalledByNative
    private void stopOnThread(final long nativeThread) {
        nativeStopThread(nativeThread);
        Looper.myQueue().addIdleHandler(new IdleHandler() {
            public boolean queueIdle() {
                JavaHandlerThread.this.mThread.getLooper().quit();
                JavaHandlerThread.this.nativeOnLooperStopped(nativeThread);
                return false;
            }
        });
    }

    @CalledByNative
    private void joinThread() {
        boolean joined = false;
        while (!joined) {
            try {
                this.mThread.join();
                joined = true;
            } catch (InterruptedException e) {
            }
        }
    }

    @CalledByNative
    private void stop(final long nativeThread) {
        if ($assertionsDisabled || hasStarted()) {
            Looper looper = this.mThread.getLooper();
            if (isAlive() && looper != null) {
                new Handler(looper).post(new Runnable() {
                    public void run() {
                        JavaHandlerThread.this.stopOnThread(nativeThread);
                    }
                });
                joinThread();
                return;
            }
            return;
        }
        throw new AssertionError();
    }

    private boolean hasStarted() {
        return this.mThread.getState() != State.NEW;
    }

    @CalledByNative
    private boolean isAlive() {
        return this.mThread.isAlive();
    }

    @CalledByNative
    private void listenForUncaughtExceptionsForTesting() {
        this.mThread.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
            public void uncaughtException(Thread t, Throwable e) {
                JavaHandlerThread.this.mUnhandledException = e;
            }
        });
    }

    @CalledByNative
    private Throwable getUncaughtExceptionIfAny() {
        return this.mUnhandledException;
    }
}
