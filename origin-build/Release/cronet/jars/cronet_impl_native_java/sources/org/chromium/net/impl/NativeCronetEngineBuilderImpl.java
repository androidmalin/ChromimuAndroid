package org.chromium.net.impl;

import android.content.Context;
import org.chromium.net.ExperimentalCronetEngine;

public class NativeCronetEngineBuilderImpl extends CronetEngineBuilderImpl {
    public NativeCronetEngineBuilderImpl(Context context) {
        super(context);
    }

    public ExperimentalCronetEngine build() {
        if (getUserAgent() == null) {
            setUserAgent(getDefaultUserAgent());
        }
        ExperimentalCronetEngine builder = new CronetUrlRequestContext(this);
        this.mMockCertVerifier = 0;
        return builder;
    }
}
