package org.chromium.base;

import org.chromium.base.annotations.JNINamespace;

@JNINamespace("base::android")
public abstract class PathService {
    public static final int DIR_MODULE = 3;

    private static native void nativeOverride(int i, String str);

    private PathService() {
    }

    public static void override(int what, String path) {
        nativeOverride(what, path);
    }
}
