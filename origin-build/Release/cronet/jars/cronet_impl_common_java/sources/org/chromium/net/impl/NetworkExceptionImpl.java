package org.chromium.net.impl;

import org.chromium.net.NetworkException;

public class NetworkExceptionImpl extends NetworkException {
    static final /* synthetic */ boolean $assertionsDisabled = (!NetworkExceptionImpl.class.desiredAssertionStatus());
    protected final int mCronetInternalErrorCode;
    protected final int mErrorCode;

    public NetworkExceptionImpl(String message, int errorCode, int cronetInternalErrorCode) {
        super(message, null);
        if (!$assertionsDisabled && (errorCode <= 0 || errorCode >= 12)) {
            throw new AssertionError();
        } else if ($assertionsDisabled || cronetInternalErrorCode < 0) {
            this.mErrorCode = errorCode;
            this.mCronetInternalErrorCode = cronetInternalErrorCode;
        } else {
            throw new AssertionError();
        }
    }

    public int getErrorCode() {
        return this.mErrorCode;
    }

    public int getCronetInternalErrorCode() {
        return this.mCronetInternalErrorCode;
    }

    public boolean immediatelyRetryable() {
        switch (this.mErrorCode) {
            case 3:
            case 4:
            case 5:
            case 6:
            case LoadState.RESOLVING_PROXY_FOR_URL /*8*/:
                return true;
            default:
                return false;
        }
    }

    public String getMessage() {
        StringBuilder b = new StringBuilder(super.getMessage());
        b.append(", ErrorCode=").append(this.mErrorCode);
        if (this.mCronetInternalErrorCode != 0) {
            b.append(", InternalErrorCode=").append(this.mCronetInternalErrorCode);
        }
        b.append(", Retryable=").append(immediatelyRetryable());
        return b.toString();
    }
}
