package org.chromium.net.urlconnection;

import java.io.IOException;
import java.io.OutputStream;
import org.chromium.net.UploadDataProvider;

abstract class CronetOutputStream extends OutputStream {
    private boolean mClosed;
    private IOException mException;
    private boolean mRequestCompleted;

    abstract void checkReceivedEnoughContent() throws IOException;

    abstract UploadDataProvider getUploadDataProvider();

    abstract void setConnected() throws IOException;

    CronetOutputStream() {
    }

    public void close() throws IOException {
        this.mClosed = true;
    }

    void setRequestCompleted(IOException exception) {
        this.mException = exception;
        this.mRequestCompleted = true;
    }

    protected void checkNotClosed() throws IOException {
        if (this.mRequestCompleted) {
            checkNoException();
            throw new IOException("Writing after request completed.");
        } else if (this.mClosed) {
            throw new IOException("Stream has been closed.");
        }
    }

    protected void checkNoException() throws IOException {
        if (this.mException != null) {
            throw this.mException;
        }
    }
}
