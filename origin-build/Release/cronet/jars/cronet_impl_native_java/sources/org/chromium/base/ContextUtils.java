package org.chromium.base;

import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.os.Build.VERSION;
import android.preference.PreferenceManager;
import org.chromium.base.annotations.JNINamespace;
import org.chromium.base.annotations.MainDex;

@MainDex
@JNINamespace("base::android")
public class ContextUtils {
    private static final String TAG = "ContextUtils";
    private static Context sApplicationContext;

    private static class Holder {
        private static SharedPreferences sSharedPreferences = ContextUtils.fetchAppSharedPreferences();

        private Holder() {
        }
    }

    public static Context getApplicationContext() {
        return sApplicationContext;
    }

    public static void initApplicationContext(Context appContext) {
        if (sApplicationContext == null || sApplicationContext == appContext) {
            initJavaSideApplicationContext(appContext);
            return;
        }
        throw new RuntimeException("Attempting to set multiple global application contexts.");
    }

    private static SharedPreferences fetchAppSharedPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(sApplicationContext);
    }

    public static SharedPreferences getAppSharedPreferences() {
        return Holder.sSharedPreferences;
    }

    @VisibleForTesting
    public static void initApplicationContextForTests(Context appContext) {
        if (appContext instanceof Application) {
            ApplicationStatus.initialize((Application) appContext);
        }
        initJavaSideApplicationContext(appContext);
        Holder.sSharedPreferences = fetchAppSharedPreferences();
    }

    public static void startForegroundService(Context context, Intent intent) {
        if (VERSION.SDK_INT >= 26) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    private static void initJavaSideApplicationContext(Context appContext) {
        if (appContext == null) {
            throw new RuntimeException("Global application context cannot be set to null.");
        }
        sApplicationContext = appContext;
    }

    public static AssetManager getApplicationAssets() {
        Context context = getApplicationContext();
        while (context instanceof ContextWrapper) {
            context = ((ContextWrapper) context).getBaseContext();
        }
        return context.getAssets();
    }
}
