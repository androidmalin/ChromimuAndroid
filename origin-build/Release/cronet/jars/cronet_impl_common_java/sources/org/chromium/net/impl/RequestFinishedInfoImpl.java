package org.chromium.net.impl;

import android.support.annotation.Nullable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collection;
import java.util.Collections;
import org.chromium.net.CronetException;
import org.chromium.net.RequestFinishedInfo;
import org.chromium.net.RequestFinishedInfo.Metrics;
import org.chromium.net.UrlResponseInfo;

public class RequestFinishedInfoImpl extends RequestFinishedInfo {
    private final Collection<Object> mAnnotations;
    @Nullable
    private final CronetException mException;
    private final int mFinishedReason;
    private final Metrics mMetrics;
    @Nullable
    private final UrlResponseInfo mResponseInfo;
    private final String mUrl;

    @Retention(RetentionPolicy.SOURCE)
    public @interface FinishedReason {
    }

    public RequestFinishedInfoImpl(String url, Collection<Object> annotations, Metrics metrics, int finishedReason, @Nullable UrlResponseInfo responseInfo, @Nullable CronetException exception) {
        this.mUrl = url;
        this.mAnnotations = annotations;
        this.mMetrics = metrics;
        this.mFinishedReason = finishedReason;
        this.mResponseInfo = responseInfo;
        this.mException = exception;
    }

    public String getUrl() {
        return this.mUrl;
    }

    public Collection<Object> getAnnotations() {
        if (this.mAnnotations == null) {
            return Collections.emptyList();
        }
        return this.mAnnotations;
    }

    public Metrics getMetrics() {
        return this.mMetrics;
    }

    public int getFinishedReason() {
        return this.mFinishedReason;
    }

    @Nullable
    public UrlResponseInfo getResponseInfo() {
        return this.mResponseInfo;
    }

    @Nullable
    public CronetException getException() {
        return this.mException;
    }
}
