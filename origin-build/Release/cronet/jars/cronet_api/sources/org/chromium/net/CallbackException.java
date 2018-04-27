package org.chromium.net;

public abstract class CallbackException extends CronetException {
    protected CallbackException(String message, Throwable cause) {
        super(message, cause);
    }
}
