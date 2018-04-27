package org.chromium.net.impl;

import android.annotation.SuppressLint;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.concurrent.Executor;
import org.chromium.net.BidirectionalStream.Callback;
import org.chromium.net.ExperimentalBidirectionalStream;
import org.chromium.net.ExperimentalBidirectionalStream.Builder;

public class BidirectionalStreamBuilderImpl extends Builder {
    private final Callback mCallback;
    private final CronetEngineBase mCronetEngine;
    private boolean mDelayRequestHeadersUntilFirstFlush;
    private final Executor mExecutor;
    private String mHttpMethod = "POST";
    private int mPriority = 3;
    private Collection<Object> mRequestAnnotations;
    private final ArrayList<Entry<String, String>> mRequestHeaders = new ArrayList();
    private final String mUrl;

    BidirectionalStreamBuilderImpl(String url, Callback callback, Executor executor, CronetEngineBase cronetEngine) {
        if (url == null) {
            throw new NullPointerException("URL is required.");
        } else if (callback == null) {
            throw new NullPointerException("Callback is required.");
        } else if (executor == null) {
            throw new NullPointerException("Executor is required.");
        } else if (cronetEngine == null) {
            throw new NullPointerException("CronetEngine is required.");
        } else {
            this.mUrl = url;
            this.mCallback = callback;
            this.mExecutor = executor;
            this.mCronetEngine = cronetEngine;
        }
    }

    public BidirectionalStreamBuilderImpl setHttpMethod(String method) {
        if (method == null) {
            throw new NullPointerException("Method is required.");
        }
        this.mHttpMethod = method;
        return this;
    }

    public BidirectionalStreamBuilderImpl addHeader(String header, String value) {
        if (header == null) {
            throw new NullPointerException("Invalid header name.");
        } else if (value == null) {
            throw new NullPointerException("Invalid header value.");
        } else {
            this.mRequestHeaders.add(new SimpleImmutableEntry(header, value));
            return this;
        }
    }

    public BidirectionalStreamBuilderImpl setPriority(int priority) {
        this.mPriority = priority;
        return this;
    }

    public BidirectionalStreamBuilderImpl delayRequestHeadersUntilFirstFlush(boolean delayRequestHeadersUntilFirstFlush) {
        this.mDelayRequestHeadersUntilFirstFlush = delayRequestHeadersUntilFirstFlush;
        return this;
    }

    public Builder addRequestAnnotation(Object annotation) {
        if (annotation == null) {
            throw new NullPointerException("Invalid metrics annotation.");
        }
        if (this.mRequestAnnotations == null) {
            this.mRequestAnnotations = new ArrayList();
        }
        this.mRequestAnnotations.add(annotation);
        return this;
    }

    @SuppressLint({"WrongConstant"})
    public ExperimentalBidirectionalStream build() {
        return this.mCronetEngine.createBidirectionalStream(this.mUrl, this.mCallback, this.mExecutor, this.mHttpMethod, this.mRequestHeaders, this.mPriority, this.mDelayRequestHeadersUntilFirstFlush, this.mRequestAnnotations);
    }
}
