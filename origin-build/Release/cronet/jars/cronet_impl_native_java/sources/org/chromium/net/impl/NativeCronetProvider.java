package org.chromium.net.impl;

import android.content.Context;
import org.chromium.base.annotations.UsedByReflection;
import org.chromium.net.CronetEngine.Builder;
import org.chromium.net.CronetProvider;
import org.chromium.net.ExperimentalCronetEngine;

public class NativeCronetProvider extends CronetProvider {
    @UsedByReflection("CronetProvider.java")
    public NativeCronetProvider(Context context) {
        super(context);
    }

    public Builder createBuilder() {
        return new ExperimentalCronetEngine.Builder(new NativeCronetEngineBuilderWithLibraryLoaderImpl(this.mContext));
    }

    public String getName() {
        return "App-Packaged-Cronet-Provider";
    }

    public String getVersion() {
        return ImplVersion.getCronetVersion();
    }

    public boolean isEnabled() {
        return true;
    }
}
