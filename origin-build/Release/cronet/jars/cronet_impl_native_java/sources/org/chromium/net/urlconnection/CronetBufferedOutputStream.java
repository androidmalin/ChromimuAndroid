package org.chromium.net.urlconnection;

import java.io.IOException;
import java.net.ProtocolException;
import java.nio.ByteBuffer;
import org.chromium.net.UploadDataProvider;
import org.chromium.net.UploadDataSink;

final class CronetBufferedOutputStream extends CronetOutputStream {
    private static final int INITIAL_BUFFER_SIZE = 16384;
    private ByteBuffer mBuffer;
    private boolean mConnected = false;
    private final CronetHttpURLConnection mConnection;
    private final int mInitialContentLength;
    private final UploadDataProvider mUploadDataProvider = new UploadDataProviderImpl();

    private class UploadDataProviderImpl extends UploadDataProvider {
        private UploadDataProviderImpl() {
        }

        public long getLength() {
            if (CronetBufferedOutputStream.this.mInitialContentLength == -1) {
                return CronetBufferedOutputStream.this.mConnected ? (long) CronetBufferedOutputStream.this.mBuffer.limit() : (long) CronetBufferedOutputStream.this.mBuffer.position();
            } else {
                return (long) CronetBufferedOutputStream.this.mInitialContentLength;
            }
        }

        public void read(UploadDataSink uploadDataSink, ByteBuffer byteBuffer) {
            int availableSpace = byteBuffer.remaining();
            if (availableSpace < CronetBufferedOutputStream.this.mBuffer.remaining()) {
                byteBuffer.put(CronetBufferedOutputStream.this.mBuffer.array(), CronetBufferedOutputStream.this.mBuffer.position(), availableSpace);
                CronetBufferedOutputStream.this.mBuffer.position(CronetBufferedOutputStream.this.mBuffer.position() + availableSpace);
            } else {
                byteBuffer.put(CronetBufferedOutputStream.this.mBuffer);
            }
            uploadDataSink.onReadSucceeded(false);
        }

        public void rewind(UploadDataSink uploadDataSink) {
            CronetBufferedOutputStream.this.mBuffer.position(0);
            uploadDataSink.onRewindSucceeded();
        }
    }

    CronetBufferedOutputStream(CronetHttpURLConnection connection, long contentLength) {
        if (connection == null) {
            throw new NullPointerException("Argument connection cannot be null.");
        } else if (contentLength > 2147483647L) {
            throw new IllegalArgumentException("Use setFixedLengthStreamingMode() or setChunkedStreamingMode() for requests larger than 2GB.");
        } else if (contentLength < 0) {
            throw new IllegalArgumentException("Content length < 0.");
        } else {
            this.mConnection = connection;
            this.mInitialContentLength = (int) contentLength;
            this.mBuffer = ByteBuffer.allocate(this.mInitialContentLength);
        }
    }

    CronetBufferedOutputStream(CronetHttpURLConnection connection) {
        if (connection == null) {
            throw new NullPointerException();
        }
        this.mConnection = connection;
        this.mInitialContentLength = -1;
        this.mBuffer = ByteBuffer.allocate(INITIAL_BUFFER_SIZE);
    }

    public void write(int oneByte) throws IOException {
        checkNotClosed();
        ensureCanWrite(1);
        this.mBuffer.put((byte) oneByte);
    }

    public void write(byte[] buffer, int offset, int count) throws IOException {
        checkNotClosed();
        ensureCanWrite(count);
        this.mBuffer.put(buffer, offset, count);
    }

    private void ensureCanWrite(int count) throws IOException {
        if (this.mInitialContentLength != -1 && this.mBuffer.position() + count > this.mInitialContentLength) {
            throw new ProtocolException("exceeded content-length limit of " + this.mInitialContentLength + " bytes");
        } else if (this.mConnected) {
            throw new IllegalStateException("Cannot write after being connected.");
        } else if (this.mInitialContentLength == -1 && this.mBuffer.limit() - this.mBuffer.position() <= count) {
            ByteBuffer newByteBuffer = ByteBuffer.allocate(Math.max(this.mBuffer.capacity() * 2, this.mBuffer.capacity() + count));
            this.mBuffer.flip();
            newByteBuffer.put(this.mBuffer);
            this.mBuffer = newByteBuffer;
        }
    }

    void setConnected() throws IOException {
        this.mConnected = true;
        if (this.mBuffer.position() < this.mInitialContentLength) {
            throw new ProtocolException("Content received is less than Content-Length");
        }
        this.mBuffer.flip();
    }

    void checkReceivedEnoughContent() throws IOException {
    }

    UploadDataProvider getUploadDataProvider() {
        return this.mUploadDataProvider;
    }
}
