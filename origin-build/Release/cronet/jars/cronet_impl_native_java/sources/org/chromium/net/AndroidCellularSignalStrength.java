package org.chromium.net;

import android.annotation.TargetApi;
import android.os.Build.VERSION;
import android.os.Handler;
import android.os.HandlerThread;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import org.chromium.base.ApplicationStatus;
import org.chromium.base.ApplicationStatus.ApplicationStateListener;
import org.chromium.base.ContextUtils;
import org.chromium.base.ThreadUtils;
import org.chromium.base.annotations.CalledByNative;
import org.chromium.base.annotations.JNINamespace;

@JNINamespace("net::android")
public class AndroidCellularSignalStrength {
    private static final AndroidCellularSignalStrength sInstance = new AndroidCellularSignalStrength();
    private volatile int mSignalLevel = CellularSignalStrengthError.ERROR_NOT_SUPPORTED;

    private class CellStateListener extends PhoneStateListener implements ApplicationStateListener {
        private final TelephonyManager mTelephonyManager = ((TelephonyManager) ContextUtils.getApplicationContext().getSystemService("phone"));

        CellStateListener() {
            ThreadUtils.assertOnBackgroundThread();
            if (this.mTelephonyManager.getSimState() == 5) {
                ApplicationStatus.registerApplicationStateListener(this);
                onApplicationStateChange(ApplicationStatus.getStateForApplication());
            }
        }

        private void register() {
            this.mTelephonyManager.listen(this, 256);
        }

        private void unregister() {
            AndroidCellularSignalStrength.this.mSignalLevel = CellularSignalStrengthError.ERROR_NOT_SUPPORTED;
            this.mTelephonyManager.listen(this, 0);
        }

        @TargetApi(23)
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            if (ApplicationStatus.getStateForApplication() == 1) {
                AndroidCellularSignalStrength.this.mSignalLevel = signalStrength.getLevel();
            }
        }

        public void onApplicationStateChange(int newState) {
            if (newState == 1) {
                register();
            } else if (newState == 2) {
                unregister();
            }
        }
    }

    private AndroidCellularSignalStrength() {
        if (VERSION.SDK_INT >= 23) {
            HandlerThread handlerThread = new HandlerThread("AndroidCellularSignalStrength");
            handlerThread.start();
            new Handler(handlerThread.getLooper()).post(new Runnable() {
                public void run() {
                    CellStateListener cellStateListener = new CellStateListener();
                }
            });
        }
    }

    @TargetApi(23)
    @CalledByNative
    private static int getSignalStrengthLevel() {
        return sInstance.mSignalLevel;
    }
}
