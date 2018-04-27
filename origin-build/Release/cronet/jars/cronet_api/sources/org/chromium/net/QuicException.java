package org.chromium.net;

public abstract class QuicException extends NetworkException {
    public abstract int getQuicDetailedErrorCode();

    protected QuicException(String message, Throwable cause) {
        super(message, cause);
    }
}
