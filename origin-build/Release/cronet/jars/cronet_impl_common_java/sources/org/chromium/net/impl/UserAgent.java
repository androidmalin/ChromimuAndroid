package org.chromium.net.impl;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Build.VERSION;
import java.util.Locale;

public final class UserAgent {
    private static final int VERSION_CODE_UNINITIALIZED = 0;
    private static final Object sLock = new Object();
    private static int sVersionCode = 0;

    private UserAgent() {
    }

    public static String from(Context context) {
        StringBuilder builder = new StringBuilder();
        builder.append(context.getPackageName());
        builder.append('/');
        builder.append(versionFromContext(context));
        builder.append(" (Linux; U; Android ");
        builder.append(VERSION.RELEASE);
        builder.append("; ");
        builder.append(Locale.getDefault().toString());
        String model = Build.MODEL;
        if (model.length() > 0) {
            builder.append("; ");
            builder.append(model);
        }
        String id = Build.ID;
        if (id.length() > 0) {
            builder.append("; Build/");
            builder.append(id);
        }
        builder.append(";");
        appendCronetVersion(builder);
        builder.append(')');
        return builder.toString();
    }

    static String getQuicUserAgentIdFrom(Context context) {
        StringBuilder builder = new StringBuilder();
        builder.append(context.getPackageName());
        appendCronetVersion(builder);
        return builder.toString();
    }

    private static int versionFromContext(Context context) {
        int i;
        synchronized (sLock) {
            if (sVersionCode == 0) {
                try {
                    sVersionCode = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
                } catch (NameNotFoundException e) {
                    throw new IllegalStateException("Cannot determine package version");
                }
            }
            i = sVersionCode;
        }
        return i;
    }

    private static void appendCronetVersion(StringBuilder builder) {
        builder.append(" Cronet/");
        builder.append(ImplVersion.getCronetVersion());
    }
}
