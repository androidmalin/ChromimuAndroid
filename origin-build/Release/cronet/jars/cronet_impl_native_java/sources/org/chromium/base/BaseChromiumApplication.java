package org.chromium.base;

import android.app.Application;
import android.content.Context;
import org.chromium.base.multidex.ChromiumMultiDexInstaller;

public class BaseChromiumApplication extends Application {
    static final /* synthetic */ boolean $assertionsDisabled = (!BaseChromiumApplication.class.desiredAssertionStatus() ? true : $assertionsDisabled);
    private static final String TAG = "base";

    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        if ($assertionsDisabled || getBaseContext() != null) {
            checkAppBeingReplaced();
            if (BuildConfig.isMultidexEnabled()) {
                ChromiumMultiDexInstaller.install(this);
                return;
            }
            return;
        }
        throw new AssertionError();
    }

    public void initCommandLine() {
    }

    @VisibleForTesting
    public static void initCommandLine(Context context) {
        ((BaseChromiumApplication) context.getApplicationContext()).initCommandLine();
    }

    private void checkAppBeingReplaced() {
        if (getResources() == null) {
            Log.e(TAG, "getResources() null, closing app.", new Object[0]);
            System.exit(0);
        }
    }
}
