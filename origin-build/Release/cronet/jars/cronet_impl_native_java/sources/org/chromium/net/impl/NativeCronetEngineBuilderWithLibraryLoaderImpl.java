package org.chromium.net.impl;

import android.content.Context;
import org.chromium.net.CronetEngine.Builder;
import org.chromium.net.impl.VersionSafeCallbacks.LibraryLoader;

public class NativeCronetEngineBuilderWithLibraryLoaderImpl extends NativeCronetEngineBuilderImpl {
    private LibraryLoader mLibraryLoader;

    public NativeCronetEngineBuilderWithLibraryLoaderImpl(Context context) {
        super(context);
    }

    public CronetEngineBuilderImpl setLibraryLoader(Builder.LibraryLoader loader) {
        this.mLibraryLoader = new LibraryLoader(loader);
        return this;
    }

    LibraryLoader libraryLoader() {
        return this.mLibraryLoader;
    }
}
