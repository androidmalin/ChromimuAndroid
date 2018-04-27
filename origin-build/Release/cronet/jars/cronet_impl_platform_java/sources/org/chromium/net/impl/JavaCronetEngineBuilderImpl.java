package org.chromium.net.impl;

import android.content.Context;
import org.chromium.net.ExperimentalCronetEngine;

public class JavaCronetEngineBuilderImpl extends CronetEngineBuilderImpl {
    public JavaCronetEngineBuilderImpl(Context context) {
        super(context);
    }

    public ExperimentalCronetEngine build() {
        if (getUserAgent() == null) {
            setUserAgent(getDefaultUserAgent());
        }
        return new JavaCronetEngine(this);
    }
}
