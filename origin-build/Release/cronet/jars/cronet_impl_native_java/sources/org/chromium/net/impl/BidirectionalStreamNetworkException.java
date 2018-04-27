package org.chromium.net.impl;

import org.chromium.base.VisibleForTesting;
import org.chromium.net.NetError;

@VisibleForTesting
public class BidirectionalStreamNetworkException extends NetworkExceptionImpl {
    static final /* synthetic */ boolean $assertionsDisabled = (!BidirectionalStreamNetworkException.class.desiredAssertionStatus());

    public BidirectionalStreamNetworkException(String message, int errorCode, int cronetInternalErrorCode) {
        super(message, errorCode, cronetInternalErrorCode);
    }

    public boolean immediatelyRetryable() {
        switch (this.mCronetInternalErrorCode) {
            case NetError.ERR_QUIC_HANDSHAKE_FAILED /*-358*/:
            case NetError.ERR_SPDY_PING_FAILED /*-352*/:
                if ($assertionsDisabled || this.mErrorCode == 11) {
                    return true;
                }
                throw new AssertionError();
            default:
                return super.immediatelyRetryable();
        }
    }
}
