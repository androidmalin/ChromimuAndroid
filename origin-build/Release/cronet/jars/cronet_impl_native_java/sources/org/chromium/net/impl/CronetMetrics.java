package org.chromium.net.impl;

import android.support.annotation.Nullable;
import java.util.Date;
import org.chromium.base.VisibleForTesting;
import org.chromium.net.RequestFinishedInfo.Metrics;

@VisibleForTesting
public final class CronetMetrics extends Metrics {
    static final /* synthetic */ boolean $assertionsDisabled = (!CronetMetrics.class.desiredAssertionStatus());
    private final long mConnectEndMs;
    private final long mConnectStartMs;
    private final long mDnsEndMs;
    private final long mDnsStartMs;
    private final long mPushEndMs;
    private final long mPushStartMs;
    @Nullable
    private final Long mReceivedByteCount;
    private final long mRequestEndMs;
    private final long mRequestStartMs;
    private final long mResponseStartMs;
    private final long mSendingEndMs;
    private final long mSendingStartMs;
    @Nullable
    private final Long mSentByteCount;
    private final boolean mSocketReused;
    private final long mSslEndMs;
    private final long mSslStartMs;
    @Nullable
    private final Long mTotalTimeMs;
    @Nullable
    private final Long mTtfbMs;

    @Nullable
    private static Date toDate(long timestamp) {
        if (timestamp != -1) {
            return new Date(timestamp);
        }
        return null;
    }

    private static boolean checkOrder(long start, long end) {
        return (end >= start && start != -1) || end == -1;
    }

    public CronetMetrics(@Nullable Long ttfbMs, @Nullable Long totalTimeMs, @Nullable Long sentByteCount, @Nullable Long receivedByteCount) {
        this.mTtfbMs = ttfbMs;
        this.mTotalTimeMs = totalTimeMs;
        this.mSentByteCount = sentByteCount;
        this.mReceivedByteCount = receivedByteCount;
        this.mRequestStartMs = -1;
        this.mDnsStartMs = -1;
        this.mDnsEndMs = -1;
        this.mConnectStartMs = -1;
        this.mConnectEndMs = -1;
        this.mSslStartMs = -1;
        this.mSslEndMs = -1;
        this.mSendingStartMs = -1;
        this.mSendingEndMs = -1;
        this.mPushStartMs = -1;
        this.mPushEndMs = -1;
        this.mResponseStartMs = -1;
        this.mRequestEndMs = -1;
        this.mSocketReused = false;
    }

    public CronetMetrics(long requestStartMs, long dnsStartMs, long dnsEndMs, long connectStartMs, long connectEndMs, long sslStartMs, long sslEndMs, long sendingStartMs, long sendingEndMs, long pushStartMs, long pushEndMs, long responseStartMs, long requestEndMs, boolean socketReused, long sentByteCount, long receivedByteCount) {
        if (!$assertionsDisabled && !checkOrder(dnsStartMs, dnsEndMs)) {
            throw new AssertionError();
        } else if (!$assertionsDisabled && !checkOrder(connectStartMs, connectEndMs)) {
            throw new AssertionError();
        } else if (!$assertionsDisabled && !checkOrder(sslStartMs, sslEndMs)) {
            throw new AssertionError();
        } else if (!$assertionsDisabled && !checkOrder(sendingStartMs, sendingEndMs)) {
            throw new AssertionError();
        } else if (!$assertionsDisabled && !checkOrder(pushStartMs, pushEndMs)) {
            throw new AssertionError();
        } else if (!$assertionsDisabled && requestEndMs < responseStartMs) {
            throw new AssertionError();
        } else if (!$assertionsDisabled && dnsStartMs < requestStartMs && dnsStartMs != -1) {
            throw new AssertionError();
        } else if (!$assertionsDisabled && sendingStartMs < requestStartMs && sendingStartMs != -1) {
            throw new AssertionError();
        } else if (!$assertionsDisabled && sslStartMs < connectStartMs && sslStartMs != -1) {
            throw new AssertionError();
        } else if ($assertionsDisabled || responseStartMs >= sendingStartMs || responseStartMs == -1) {
            this.mRequestStartMs = requestStartMs;
            this.mDnsStartMs = dnsStartMs;
            this.mDnsEndMs = dnsEndMs;
            this.mConnectStartMs = connectStartMs;
            this.mConnectEndMs = connectEndMs;
            this.mSslStartMs = sslStartMs;
            this.mSslEndMs = sslEndMs;
            this.mSendingStartMs = sendingStartMs;
            this.mSendingEndMs = sendingEndMs;
            this.mPushStartMs = pushStartMs;
            this.mPushEndMs = pushEndMs;
            this.mResponseStartMs = responseStartMs;
            this.mRequestEndMs = requestEndMs;
            this.mSocketReused = socketReused;
            this.mSentByteCount = Long.valueOf(sentByteCount);
            this.mReceivedByteCount = Long.valueOf(receivedByteCount);
            if (requestStartMs == -1 || responseStartMs == -1) {
                this.mTtfbMs = null;
            } else {
                this.mTtfbMs = Long.valueOf(responseStartMs - requestStartMs);
            }
            if (requestStartMs == -1 || requestEndMs == -1) {
                this.mTotalTimeMs = null;
            } else {
                this.mTotalTimeMs = Long.valueOf(requestEndMs - requestStartMs);
            }
        } else {
            throw new AssertionError();
        }
    }

    @Nullable
    public Date getRequestStart() {
        return toDate(this.mRequestStartMs);
    }

    @Nullable
    public Date getDnsStart() {
        return toDate(this.mDnsStartMs);
    }

    @Nullable
    public Date getDnsEnd() {
        return toDate(this.mDnsEndMs);
    }

    @Nullable
    public Date getConnectStart() {
        return toDate(this.mConnectStartMs);
    }

    @Nullable
    public Date getConnectEnd() {
        return toDate(this.mConnectEndMs);
    }

    @Nullable
    public Date getSslStart() {
        return toDate(this.mSslStartMs);
    }

    @Nullable
    public Date getSslEnd() {
        return toDate(this.mSslEndMs);
    }

    @Nullable
    public Date getSendingStart() {
        return toDate(this.mSendingStartMs);
    }

    @Nullable
    public Date getSendingEnd() {
        return toDate(this.mSendingEndMs);
    }

    @Nullable
    public Date getPushStart() {
        return toDate(this.mPushStartMs);
    }

    @Nullable
    public Date getPushEnd() {
        return toDate(this.mPushEndMs);
    }

    @Nullable
    public Date getResponseStart() {
        return toDate(this.mResponseStartMs);
    }

    @Nullable
    public Date getRequestEnd() {
        return toDate(this.mRequestEndMs);
    }

    @Nullable
    public boolean getSocketReused() {
        return this.mSocketReused;
    }

    @Nullable
    public Long getTtfbMs() {
        return this.mTtfbMs;
    }

    @Nullable
    public Long getTotalTimeMs() {
        return this.mTotalTimeMs;
    }

    @Nullable
    public Long getSentByteCount() {
        return this.mSentByteCount;
    }

    @Nullable
    public Long getReceivedByteCount() {
        return this.mReceivedByteCount;
    }
}
