package org.chromium.base;

import android.support.annotation.UiThread;
import java.lang.Thread.UncaughtExceptionHandler;
import org.chromium.base.annotations.CalledByNative;
import org.chromium.base.annotations.JNINamespace;
import org.chromium.base.annotations.MainDex;

@MainDex
@JNINamespace("base::android")
public class JavaExceptionReporter implements UncaughtExceptionHandler {
    static final /* synthetic */ boolean $assertionsDisabled = (!JavaExceptionReporter.class.desiredAssertionStatus());
    private final boolean mCrashAfterReport;
    private boolean mHandlingException;
    private final UncaughtExceptionHandler mParent;

    private static native void nativeReportJavaException(boolean z, Throwable th);

    private static native void nativeReportJavaStackTrace(String str);

    private JavaExceptionReporter(UncaughtExceptionHandler parent, boolean crashAfterReport) {
        this.mParent = parent;
        this.mCrashAfterReport = crashAfterReport;
    }

    public void uncaughtException(Thread t, Throwable e) {
        if (!this.mHandlingException) {
            this.mHandlingException = true;
            nativeReportJavaException(this.mCrashAfterReport, e);
        }
        if (this.mParent != null) {
            this.mParent.uncaughtException(t, e);
        }
    }

    @UiThread
    public static void reportStackTrace(String stackTrace) {
        if ($assertionsDisabled || ThreadUtils.runningOnUiThread()) {
            nativeReportJavaStackTrace(stackTrace);
            return;
        }
        throw new AssertionError();
    }

    @CalledByNative
    private static void installHandler(boolean crashAfterReport) {
        Thread.setDefaultUncaughtExceptionHandler(new JavaExceptionReporter(Thread.getDefaultUncaughtExceptionHandler(), crashAfterReport));
    }
}
