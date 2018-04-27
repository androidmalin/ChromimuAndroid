package org.chromium.net;

import java.io.IOException;

public abstract class CronetException extends IOException {
    protected CronetException(String message, Throwable cause) {
        super(message, cause);
    }
}
