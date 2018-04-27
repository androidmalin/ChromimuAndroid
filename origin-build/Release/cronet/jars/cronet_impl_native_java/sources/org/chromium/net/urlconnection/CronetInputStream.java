package org.chromium.net.urlconnection;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import org.chromium.net.PrivateKeyType;

class CronetInputStream extends InputStream {
    private static final int READ_BUFFER_SIZE = 32768;
    private ByteBuffer mBuffer;
    private IOException mException;
    private final CronetHttpURLConnection mHttpURLConnection;
    private boolean mResponseDataCompleted;

    public CronetInputStream(CronetHttpURLConnection httpURLConnection) {
        this.mHttpURLConnection = httpURLConnection;
    }

    public int read() throws IOException {
        getMoreDataIfNeeded();
        if (hasUnreadData()) {
            return this.mBuffer.get() & PrivateKeyType.INVALID;
        }
        return -1;
    }

    public int read(byte[] buffer, int byteOffset, int byteCount) throws IOException {
        if (byteOffset < 0 || byteCount < 0 || byteOffset + byteCount > buffer.length) {
            throw new IndexOutOfBoundsException();
        } else if (byteCount == 0) {
            return 0;
        } else {
            getMoreDataIfNeeded();
            if (!hasUnreadData()) {
                return -1;
            }
            int bytesRead = Math.min(this.mBuffer.limit() - this.mBuffer.position(), byteCount);
            this.mBuffer.get(buffer, byteOffset, bytesRead);
            return bytesRead;
        }
    }

    void setResponseDataCompleted(IOException exception) {
        this.mException = exception;
        this.mResponseDataCompleted = true;
        this.mBuffer = null;
    }

    private void getMoreDataIfNeeded() throws IOException {
        if (this.mResponseDataCompleted) {
            if (this.mException != null) {
                throw this.mException;
            }
        } else if (!hasUnreadData()) {
            if (this.mBuffer == null) {
                this.mBuffer = ByteBuffer.allocateDirect(READ_BUFFER_SIZE);
            }
            this.mBuffer.clear();
            this.mHttpURLConnection.getMoreData(this.mBuffer);
            if (this.mException != null) {
                throw this.mException;
            } else if (this.mBuffer != null) {
                this.mBuffer.flip();
            }
        }
    }

    private boolean hasUnreadData() {
        return this.mBuffer != null && this.mBuffer.hasRemaining();
    }
}
