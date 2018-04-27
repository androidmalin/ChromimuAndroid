package org.chromium.base;

import org.chromium.base.annotations.CalledByNative;
import org.chromium.base.annotations.MainDex;

@MainDex
public class JNIUtils {
    static final /* synthetic */ boolean $assertionsDisabled = (!JNIUtils.class.desiredAssertionStatus());
    private static Boolean sSelectiveJniRegistrationEnabled;

    @CalledByNative
    public static Object getClassLoader() {
        return JNIUtils.class.getClassLoader();
    }

    @CalledByNative
    public static boolean isSelectiveJniRegistrationEnabled() {
        if (sSelectiveJniRegistrationEnabled == null) {
            sSelectiveJniRegistrationEnabled = Boolean.valueOf(false);
        }
        return sSelectiveJniRegistrationEnabled.booleanValue();
    }

    public static void enableSelectiveJniRegistration() {
        if ($assertionsDisabled || sSelectiveJniRegistrationEnabled == null) {
            sSelectiveJniRegistrationEnabled = Boolean.valueOf(true);
            return;
        }
        throw new AssertionError();
    }
}
