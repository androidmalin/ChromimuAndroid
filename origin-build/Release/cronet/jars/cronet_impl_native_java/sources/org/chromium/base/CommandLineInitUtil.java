package org.chromium.base;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build.VERSION;
import android.provider.Settings.Global;
import android.provider.Settings.System;
import java.io.File;

public final class CommandLineInitUtil {
    private static final String COMMAND_LINE_FILE_PATH = "/data/local";
    private static final String COMMAND_LINE_FILE_PATH_DEBUG_APP = "/data/local/tmp";
    private static final String TAG = "CommandLineInitUtil";

    private CommandLineInitUtil() {
    }

    public static void initCommandLine(Context context, String fileName) {
        if (!CommandLine.isInitialized()) {
            File commandLineFile = getAlternativeCommandLinePath(context, fileName);
            if (commandLineFile != null) {
                Log.i(TAG, "Initializing command line from alternative file " + commandLineFile.getPath(), new Object[0]);
            } else {
                commandLineFile = new File(COMMAND_LINE_FILE_PATH, fileName);
                Log.d(TAG, "Initializing command line from " + commandLineFile.getPath());
            }
            CommandLine.initFromFile(commandLineFile.getPath());
        }
    }

    private static File getAlternativeCommandLinePath(Context context, String fileName) {
        File alternativeCommandLineFile = new File(COMMAND_LINE_FILE_PATH_DEBUG_APP, fileName);
        if (!alternativeCommandLineFile.exists()) {
            return null;
        }
        try {
            if (BuildInfo.isDebugAndroid()) {
                return alternativeCommandLineFile;
            }
            String debugApp;
            if (VERSION.SDK_INT < 17) {
                debugApp = getDebugAppPreJBMR1(context);
            } else {
                debugApp = getDebugAppJBMR1(context);
            }
            if (debugApp != null && debugApp.equals(context.getApplicationContext().getPackageName())) {
                return alternativeCommandLineFile;
            }
            return null;
        } catch (RuntimeException e) {
            Log.e(TAG, "Unable to detect alternative command line file", new Object[0]);
        }
    }

    @SuppressLint({"NewApi"})
    private static String getDebugAppJBMR1(Context context) {
        boolean adbEnabled = true;
        if (Global.getInt(context.getContentResolver(), "adb_enabled", 0) != 1) {
            adbEnabled = false;
        }
        if (adbEnabled) {
            return Global.getString(context.getContentResolver(), "debug_app");
        }
        return null;
    }

    private static String getDebugAppPreJBMR1(Context context) {
        boolean adbEnabled = true;
        if (System.getInt(context.getContentResolver(), "adb_enabled", 0) != 1) {
            adbEnabled = false;
        }
        if (adbEnabled) {
            return System.getString(context.getContentResolver(), "debug_app");
        }
        return null;
    }
}
