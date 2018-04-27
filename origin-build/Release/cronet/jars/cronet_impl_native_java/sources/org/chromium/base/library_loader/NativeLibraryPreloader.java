package org.chromium.base.library_loader;

import android.content.Context;

public abstract class NativeLibraryPreloader {
    public abstract int loadLibrary(Context context);
}
