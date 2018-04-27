package org.chromium.base;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import org.chromium.base.annotations.CalledByNative;
import org.chromium.base.annotations.JNINamespace;

@JNINamespace("base::android")
public class PowerMonitor {
    static final /* synthetic */ boolean $assertionsDisabled = (!PowerMonitor.class.desiredAssertionStatus());
    private static PowerMonitor sInstance;
    private boolean mIsBatteryPower;

    private static native void nativeOnBatteryChargingChanged();

    public static void createForTests() {
        sInstance = new PowerMonitor();
    }

    public static void create() {
        ThreadUtils.assertOnUiThread();
        if (sInstance == null) {
            Context context = ContextUtils.getApplicationContext();
            sInstance = new PowerMonitor();
            Intent batteryStatusIntent = context.registerReceiver(null, new IntentFilter("android.intent.action.BATTERY_CHANGED"));
            if (batteryStatusIntent != null) {
                onBatteryChargingChanged(batteryStatusIntent);
            }
            IntentFilter powerConnectedFilter = new IntentFilter();
            powerConnectedFilter.addAction("android.intent.action.ACTION_POWER_CONNECTED");
            powerConnectedFilter.addAction("android.intent.action.ACTION_POWER_DISCONNECTED");
            context.registerReceiver(new BroadcastReceiver() {
                public void onReceive(Context context, Intent intent) {
                    PowerMonitor.onBatteryChargingChanged(intent);
                }
            }, powerConnectedFilter);
        }
    }

    private PowerMonitor() {
    }

    private static void onBatteryChargingChanged(Intent intent) {
        boolean z = true;
        if ($assertionsDisabled || sInstance != null) {
            int chargePlug = intent.getIntExtra("plugged", -1);
            PowerMonitor powerMonitor = sInstance;
            if (chargePlug == 2 || chargePlug == 1) {
                z = false;
            }
            powerMonitor.mIsBatteryPower = z;
            nativeOnBatteryChargingChanged();
            return;
        }
        throw new AssertionError();
    }

    @CalledByNative
    private static boolean isBatteryPower() {
        if (sInstance == null) {
            create();
        }
        return sInstance.mIsBatteryPower;
    }
}
