package org.chromium.net.urlconnection;

import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import org.chromium.net.impl.CronetEngineBase;

class CronetHttpURLStreamHandler extends URLStreamHandler {
    private final CronetEngineBase mCronetEngine;

    public CronetHttpURLStreamHandler(CronetEngineBase cronetEngine) {
        this.mCronetEngine = cronetEngine;
    }

    public URLConnection openConnection(URL url) throws IOException {
        return this.mCronetEngine.openConnection(url);
    }

    public URLConnection openConnection(URL url, Proxy proxy) throws IOException {
        return this.mCronetEngine.openConnection(url, proxy);
    }
}
