package org.chromium.net.impl;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.Executor;
import org.chromium.net.BidirectionalStream.Callback;
import org.chromium.net.ExperimentalBidirectionalStream;
import org.chromium.net.ExperimentalCronetEngine;
import org.chromium.net.ExperimentalUrlRequest.Builder;
import org.chromium.net.UrlRequest;

public abstract class CronetEngineBase extends ExperimentalCronetEngine {

    @Retention(RetentionPolicy.SOURCE)
    public @interface RequestPriority {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface StreamPriority {
    }

    protected abstract ExperimentalBidirectionalStream createBidirectionalStream(String str, Callback callback, Executor executor, String str2, List<Entry<String, String>> list, int i, boolean z, Collection<Object> collection);

    protected abstract UrlRequestBase createRequest(String str, UrlRequest.Callback callback, Executor executor, int i, Collection<Object> collection, boolean z, boolean z2, boolean z3);

    public Builder newUrlRequestBuilder(String url, UrlRequest.Callback callback, Executor executor) {
        return new UrlRequestBuilderImpl(url, callback, executor, this);
    }
}
