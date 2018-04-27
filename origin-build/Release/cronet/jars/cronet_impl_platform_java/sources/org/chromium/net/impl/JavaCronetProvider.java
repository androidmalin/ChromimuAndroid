package org.chromium.net.impl;

import android.content.Context;
import org.chromium.net.CronetEngine.Builder;
import org.chromium.net.CronetProvider;
import org.chromium.net.ExperimentalCronetEngine;

public class JavaCronetProvider extends CronetProvider {
    public JavaCronetProvider(Context context) {
        super(context);
    }

    public Builder createBuilder() {
        return new ExperimentalCronetEngine.Builder(new JavaCronetEngineBuilderImpl(this.mContext));
    }

    public String getName() {
        return "Fallback-Cronet-Provider";
    }

    public String getVersion() {
        return ImplVersion.getCronetVersion();
    }

    public boolean isEnabled() {
        return true;
    }
}
