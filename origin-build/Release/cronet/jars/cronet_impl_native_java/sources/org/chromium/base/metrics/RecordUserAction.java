package org.chromium.base.metrics;

import org.chromium.base.ThreadUtils;
import org.chromium.base.VisibleForTesting;
import org.chromium.base.annotations.CalledByNative;
import org.chromium.base.annotations.JNINamespace;

@JNINamespace("base::android")
public class RecordUserAction {
    static final /* synthetic */ boolean $assertionsDisabled = (!RecordUserAction.class.desiredAssertionStatus());
    private static Throwable sDisabledBy;
    private static long sNativeActionCallback;

    /* renamed from: org.chromium.base.metrics.RecordUserAction$1 */
    class AnonymousClass1 implements Runnable {
        final /* synthetic */ String val$action;

        AnonymousClass1(String str) {
            this.val$action = str;
        }

        public void run() {
            RecordUserAction.nativeRecordUserAction(this.val$action);
        }
    }

    public interface UserActionCallback {
        @CalledByNative("UserActionCallback")
        void onActionRecorded(String str);
    }

    private static native long nativeAddActionCallbackForTesting(UserActionCallback userActionCallback);

    private static native void nativeRecordUserAction(String str);

    private static native void nativeRemoveActionCallbackForTesting(long j);

    @VisibleForTesting
    public static void setDisabledForTests(boolean disabled) {
        if (!disabled || sDisabledBy == null) {
            sDisabledBy = disabled ? new Throwable() : null;
            return;
        }
        throw new IllegalStateException("UserActions are already disabled.", sDisabledBy);
    }

    public static void record(String action) {
        if (sDisabledBy == null) {
            if (ThreadUtils.runningOnUiThread()) {
                nativeRecordUserAction(action);
            } else {
                ThreadUtils.runOnUiThread(new AnonymousClass1(action));
            }
        }
    }

    public static void setActionCallbackForTesting(UserActionCallback callback) {
        if ($assertionsDisabled || sNativeActionCallback == 0) {
            sNativeActionCallback = nativeAddActionCallbackForTesting(callback);
            return;
        }
        throw new AssertionError();
    }

    public static void removeActionCallbackForTesting() {
        if ($assertionsDisabled || sNativeActionCallback != 0) {
            nativeRemoveActionCallbackForTesting(sNativeActionCallback);
            sNativeActionCallback = 0;
            return;
        }
        throw new AssertionError();
    }
}
