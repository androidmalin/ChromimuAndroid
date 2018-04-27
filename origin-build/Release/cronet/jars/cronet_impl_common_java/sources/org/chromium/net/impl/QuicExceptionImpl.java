package org.chromium.net.impl;

import org.chromium.net.QuicException;

public class QuicExceptionImpl extends QuicException {
    private final NetworkExceptionImpl mNetworkException;
    private final int mQuicDetailedErrorCode;

    public QuicExceptionImpl(String message, int netErrorCode, int quicDetailedErrorCode) {
        super(message, null);
        this.mNetworkException = new NetworkExceptionImpl(message, 10, netErrorCode);
        this.mQuicDetailedErrorCode = quicDetailedErrorCode;
    }

    public String getMessage() {
        StringBuilder b = new StringBuilder(this.mNetworkException.getMessage());
        b.append(", QuicDetailedErrorCode=").append(this.mQuicDetailedErrorCode);
        return b.toString();
    }

    public int getErrorCode() {
        return this.mNetworkException.getErrorCode();
    }

    public int getCronetInternalErrorCode() {
        return this.mNetworkException.getCronetInternalErrorCode();
    }

    public boolean immediatelyRetryable() {
        return this.mNetworkException.immediatelyRetryable();
    }

    public int getQuicDetailedErrorCode() {
        return this.mQuicDetailedErrorCode;
    }
}
