package org.chromium.net.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;
import org.chromium.net.UrlResponseInfo;
import org.chromium.net.UrlResponseInfo.HeaderBlock;

public final class UrlResponseInfoImpl extends UrlResponseInfo {
    private final HeaderBlockImpl mHeaders;
    private final int mHttpStatusCode;
    private final String mHttpStatusText;
    private final String mNegotiatedProtocol;
    private final String mProxyServer;
    private final AtomicLong mReceivedByteCount = new AtomicLong();
    private final List<String> mResponseInfoUrlChain;
    private final boolean mWasCached;

    public static final class HeaderBlockImpl extends HeaderBlock {
        private final List<Entry<String, String>> mAllHeadersList;
        private Map<String, List<String>> mHeadersMap;

        HeaderBlockImpl(List<Entry<String, String>> allHeadersList) {
            this.mAllHeadersList = allHeadersList;
        }

        public List<Entry<String, String>> getAsList() {
            return this.mAllHeadersList;
        }

        public Map<String, List<String>> getAsMap() {
            if (this.mHeadersMap != null) {
                return this.mHeadersMap;
            }
            Map<String, List<String>> map = new TreeMap(String.CASE_INSENSITIVE_ORDER);
            for (Entry<String, String> entry : this.mAllHeadersList) {
                List<String> values = new ArrayList();
                if (map.containsKey(entry.getKey())) {
                    values.addAll((Collection) map.get(entry.getKey()));
                }
                values.add((String) entry.getValue());
                map.put((String) entry.getKey(), Collections.unmodifiableList(values));
            }
            this.mHeadersMap = Collections.unmodifiableMap(map);
            return this.mHeadersMap;
        }
    }

    public UrlResponseInfoImpl(List<String> urlChain, int httpStatusCode, String httpStatusText, List<Entry<String, String>> allHeadersList, boolean wasCached, String negotiatedProtocol, String proxyServer) {
        this.mResponseInfoUrlChain = Collections.unmodifiableList(urlChain);
        this.mHttpStatusCode = httpStatusCode;
        this.mHttpStatusText = httpStatusText;
        this.mHeaders = new HeaderBlockImpl(Collections.unmodifiableList(allHeadersList));
        this.mWasCached = wasCached;
        this.mNegotiatedProtocol = negotiatedProtocol;
        this.mProxyServer = proxyServer;
    }

    public String getUrl() {
        return (String) this.mResponseInfoUrlChain.get(this.mResponseInfoUrlChain.size() - 1);
    }

    public List<String> getUrlChain() {
        return this.mResponseInfoUrlChain;
    }

    public int getHttpStatusCode() {
        return this.mHttpStatusCode;
    }

    public String getHttpStatusText() {
        return this.mHttpStatusText;
    }

    public List<Entry<String, String>> getAllHeadersAsList() {
        return this.mHeaders.getAsList();
    }

    public Map<String, List<String>> getAllHeaders() {
        return this.mHeaders.getAsMap();
    }

    public boolean wasCached() {
        return this.mWasCached;
    }

    public String getNegotiatedProtocol() {
        return this.mNegotiatedProtocol;
    }

    public String getProxyServer() {
        return this.mProxyServer;
    }

    public long getReceivedByteCount() {
        return this.mReceivedByteCount.get();
    }

    public String toString() {
        return String.format(Locale.ROOT, "UrlResponseInfo@[%s][%s]: urlChain = %s, httpStatus = %d %s, headers = %s, wasCached = %b, negotiatedProtocol = %s, proxyServer= %s, receivedByteCount = %d", new Object[]{Integer.toHexString(System.identityHashCode(this)), getUrl(), getUrlChain().toString(), Integer.valueOf(getHttpStatusCode()), getHttpStatusText(), getAllHeadersAsList().toString(), Boolean.valueOf(wasCached()), getNegotiatedProtocol(), getProxyServer(), Long.valueOf(getReceivedByteCount())});
    }

    public void setReceivedByteCount(long currentReceivedByteCount) {
        this.mReceivedByteCount.set(currentReceivedByteCount);
    }
}
