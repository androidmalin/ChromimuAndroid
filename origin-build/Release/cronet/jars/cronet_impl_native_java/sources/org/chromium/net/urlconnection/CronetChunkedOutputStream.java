package org.chromium.net.urlconnection;

import java.io.IOException;
import java.net.HttpRetryException;
import java.nio.ByteBuffer;
import org.chromium.net.UploadDataProvider;
import org.chromium.net.UploadDataSink;

final class CronetChunkedOutputStream extends CronetOutputStream {
    private final ByteBuffer mBuffer;
    private final CronetHttpURLConnection mConnection;
    private boolean mLastChunk = false;
    private final MessageLoop mMessageLoop;
    private final UploadDataProvider mUploadDataProvider = new UploadDataProviderImpl();

    private class UploadDataProviderImpl extends UploadDataProvider {
        private UploadDataProviderImpl() {
        }

        public long getLength() {
            return -1;
        }

        public void read(UploadDataSink uploadDataSink, ByteBuffer byteBuffer) {
            if (byteBuffer.remaining() >= CronetChunkedOutputStream.this.mBuffer.remaining()) {
                byteBuffer.put(CronetChunkedOutputStream.this.mBuffer);
                CronetChunkedOutputStream.this.mBuffer.clear();
                uploadDataSink.onReadSucceeded(CronetChunkedOutputStream.this.mLastChunk);
                if (!CronetChunkedOutputStream.this.mLastChunk) {
                    CronetChunkedOutputStream.this.mMessageLoop.quit();
                    return;
                }
                return;
            }
            int oldLimit = CronetChunkedOutputStream.this.mBuffer.limit();
            CronetChunkedOutputStream.this.mBuffer.limit(CronetChunkedOutputStream.this.mBuffer.position() + byteBuffer.remaining());
            byteBuffer.put(CronetChunkedOutputStream.this.mBuffer);
            CronetChunkedOutputStream.this.mBuffer.limit(oldLimit);
            uploadDataSink.onReadSucceeded(false);
        }

        public void rewind(UploadDataSink uploadDataSink) {
            uploadDataSink.onRewindError(new HttpRetryException("Cannot retry streamed Http body", -1));
        }
    }

    CronetChunkedOutputStream(CronetHttpURLConnection connection, int chunkLength, MessageLoop messageLoop) {
        if (connection == null) {
            throw new NullPointerException();
        } else if (chunkLength <= 0) {
            throw new IllegalArgumentException("chunkLength should be greater than 0");
        } else {
            this.mBuffer = ByteBuffer.allocate(chunkLength);
            this.mConnection = connection;
            this.mMessageLoop = messageLoop;
        }
    }

    public void write(int oneByte) throws IOException {
        ensureBufferHasRemaining();
        this.mBuffer.put((byte) oneByte);
    }

    public void write(byte[] buffer, int offset, int count) throws IOException {
        checkNotClosed();
        if (buffer.length - offset < count || offset < 0 || count < 0) {
            throw new IndexOutOfBoundsException();
        }
        int toSend = count;
        while (toSend > 0) {
            int sent = Math.min(toSend, this.mBuffer.remaining());
            this.mBuffer.put(buffer, (offset + count) - toSend, sent);
            toSend -= sent;
            ensureBufferHasRemaining();
        }
    }

    public void close() throws IOException {
        super.close();
        if (!this.mLastChunk) {
            this.mLastChunk = true;
            this.mBuffer.flip();
        }
    }

    void setConnected() throws IOException {
    }

    void checkReceivedEnoughContent() throws IOException {
    }

    UploadDataProvider getUploadDataProvider() {
        return this.mUploadDataProvider;
    }

    private void ensureBufferHasRemaining() throws IOException {
        if (!this.mBuffer.hasRemaining()) {
            uploadBufferInternal();
        }
    }

    private void uploadBufferInternal() throws IOException {
        checkNotClosed();
        this.mBuffer.flip();
        this.mMessageLoop.loop();
        checkNoException();
    }
}
