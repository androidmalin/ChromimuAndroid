package org.chromium.net.impl;

import android.util.Log;
import android.util.Pair;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.Executor;
import org.chromium.net.ExperimentalUrlRequest.Builder;
import org.chromium.net.UploadDataProvider;
import org.chromium.net.UrlRequest.Callback;

public class UrlRequestBuilderImpl extends Builder {
    private static final String ACCEPT_ENCODING = "Accept-Encoding";
    private static final String TAG = UrlRequestBuilderImpl.class.getSimpleName();
    private boolean mAllowDirectExecutor = false;
    private final Callback mCallback;
    private final CronetEngineBase mCronetEngine;
    private boolean mDisableCache;
    private boolean mDisableConnectionMigration;
    private final Executor mExecutor;
    private String mMethod;
    private int mPriority = 3;
    private Collection<Object> mRequestAnnotations;
    private final ArrayList<Pair<String, String>> mRequestHeaders = new ArrayList();
    private UploadDataProvider mUploadDataProvider;
    private Executor mUploadDataProviderExecutor;
    private final String mUrl;

    UrlRequestBuilderImpl(String url, Callback callback, Executor executor, CronetEngineBase cronetEngine) {
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

    public Builder setHttpMethod(String method) {
        if (method == null) {
            throw new NullPointerException("Method is required.");
        }
        this.mMethod = method;
        return this;
    }

    public UrlRequestBuilderImpl addHeader(String header, String value) {
        if (header == null) {
            throw new NullPointerException("Invalid header name.");
        } else if (value == null) {
            throw new NullPointerException("Invalid header value.");
        } else {
            if (ACCEPT_ENCODING.equalsIgnoreCase(header)) {
                Log.w(TAG, "It's not necessary to set Accept-Encoding on requests - cronet will do this automatically for you, and setting it yourself has no effect. See https://crbug.com/581399 for details.", new Exception());
            } else {
                this.mRequestHeaders.add(Pair.create(header, value));
            }
            return this;
        }
    }

    public UrlRequestBuilderImpl disableCache() {
        this.mDisableCache = true;
        return this;
    }

    public UrlRequestBuilderImpl disableConnectionMigration() {
        this.mDisableConnectionMigration = true;
        return this;
    }

    public UrlRequestBuilderImpl setPriority(int priority) {
        this.mPriority = priority;
        return this;
    }

    public UrlRequestBuilderImpl setUploadDataProvider(UploadDataProvider uploadDataProvider, Executor executor) {
        if (uploadDataProvider == null) {
            throw new NullPointerException("Invalid UploadDataProvider.");
        } else if (executor == null) {
            throw new NullPointerException("Invalid UploadDataProvider Executor.");
        } else {
            if (this.mMethod == null) {
                this.mMethod = "POST";
            }
            this.mUploadDataProvider = uploadDataProvider;
            this.mUploadDataProviderExecutor = executor;
            return this;
        }
    }

    public UrlRequestBuilderImpl allowDirectExecutor() {
        this.mAllowDirectExecutor = true;
        return this;
    }

    public UrlRequestBuilderImpl addRequestAnnotation(Object annotation) {
        if (annotation == null) {
            throw new NullPointerException("Invalid metrics annotation.");
        }
        if (this.mRequestAnnotations == null) {
            this.mRequestAnnotations = new ArrayList();
        }
        this.mRequestAnnotations.add(annotation);
        return this;
    }

    public UrlRequestBase build() {
        UrlRequestBase request = this.mCronetEngine.createRequest(this.mUrl, this.mCallback, this.mExecutor, this.mPriority, this.mRequestAnnotations, this.mDisableCache, this.mDisableConnectionMigration, this.mAllowDirectExecutor);
        if (this.mMethod != null) {
            request.setHttpMethod(this.mMethod);
        }
        Iterator it = this.mRequestHeaders.iterator();
        while (it.hasNext()) {
            Pair<String, String> header = (Pair) it.next();
            request.addHeader((String) header.first, (String) header.second);
        }
        if (this.mUploadDataProvider != null) {
            request.setUploadDataProvider(this.mUploadDataProvider, this.mUploadDataProviderExecutor);
        }
        return request;
    }
}
