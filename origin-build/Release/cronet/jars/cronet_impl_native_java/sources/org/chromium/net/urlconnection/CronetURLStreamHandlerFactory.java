package org.chromium.net.urlconnection;

import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import org.chromium.net.impl.CronetEngineBase;

public class CronetURLStreamHandlerFactory implements URLStreamHandlerFactory {
    private final CronetEngineBase mCronetEngine;

    public CronetURLStreamHandlerFactory(CronetEngineBase cronetEngine) {
        if (cronetEngine == null) {
            throw new NullPointerException("CronetEngine is null.");
        }
        this.mCronetEngine = cronetEngine;
    }

    public URLStreamHandler createURLStreamHandler(String protocol) {
        if ("http".equals(protocol) || "https".equals(protocol)) {
            return new CronetHttpURLStreamHandler(this.mCronetEngine);
        }
        return null;
    }
}
