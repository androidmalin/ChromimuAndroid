package org.chromium.net.urlconnection;

import java.io.IOException;
import java.net.HttpRetryException;
import java.net.ProtocolException;
import java.nio.ByteBuffer;
import org.chromium.base.VisibleForTesting;
import org.chromium.net.UploadDataProvider;
import org.chromium.net.UploadDataSink;

final class CronetFixedModeOutputStream extends CronetOutputStream {
    @VisibleForTesting
    private static int sDefaultBufferLength = 16384;
    private final ByteBuffer mBuffer;
    private long mBytesWritten;
    private final CronetHttpURLConnection mConnection;
    private final long mContentLength;
    private final MessageLoop mMessageLoop;
    private final UploadDataProvider mUploadDataProvider = new UploadDataProviderImpl();

    private class UploadDataProviderImpl extends UploadDataProvider {
        private UploadDataProviderImpl() {
        }

        public long getLength() {
            return CronetFixedModeOutputStream.this.mContentLength;
        }

        public void read(UploadDataSink uploadDataSink, ByteBuffer byteBuffer) {
            if (byteBuffer.remaining() >= CronetFixedModeOutputStream.this.mBuffer.remaining()) {
                byteBuffer.put(CronetFixedModeOutputStream.this.mBuffer);
                CronetFixedModeOutputStream.this.mBuffer.clear();
                uploadDataSink.onReadSucceeded(false);
                CronetFixedModeOutputStream.this.mMessageLoop.quit();
                return;
            }
            int oldLimit = CronetFixedModeOutputStream.this.mBuffer.limit();
            CronetFixedModeOutputStream.this.mBuffer.limit(CronetFixedModeOutputStream.this.mBuffer.position() + byteBuffer.remaining());
            byteBuffer.put(CronetFixedModeOutputStream.this.mBuffer);
            CronetFixedModeOutputStream.this.mBuffer.limit(oldLimit);
            uploadDataSink.onReadSucceeded(false);
        }

        public void rewind(UploadDataSink uploadDataSink) {
            uploadDataSink.onRewindError(new HttpRetryException("Cannot retry streamed Http body", -1));
        }
    }

    CronetFixedModeOutputStream(CronetHttpURLConnection connection, long contentLength, MessageLoop messageLoop) {
        if (connection == null) {
            throw new NullPointerException();
        } else if (contentLength < 0) {
            throw new IllegalArgumentException("Content length must be larger than 0 for non-chunked upload.");
        } else {
            this.mContentLength = contentLength;
            this.mBuffer = ByteBuffer.allocate((int) Math.min(this.mContentLength, (long) sDefaultBufferLength));
            this.mConnection = connection;
            this.mMessageLoop = messageLoop;
            this.mBytesWritten = 0;
        }
    }

    public void write(int oneByte) throws IOException {
        checkNotClosed();
        checkNotExceedContentLength(1);
        ensureBufferHasRemaining();
        this.mBuffer.put((byte) oneByte);
        this.mBytesWritten++;
        uploadIfComplete();
    }

    public void write(byte[] buffer, int offset, int count) throws IOException {
        checkNotClosed();
        if (buffer.length - offset < count || offset < 0 || count < 0) {
            throw new IndexOutOfBoundsException();
        }
        checkNotExceedContentLength(count);
        int toSend = count;
        while (toSend > 0) {
            ensureBufferHasRemaining();
            int sent = Math.min(toSend, this.mBuffer.remaining());
            this.mBuffer.put(buffer, (offset + count) - toSend, sent);
            toSend -= sent;
        }
        this.mBytesWritten += (long) count;
        uploadIfComplete();
    }

    private void ensureBufferHasRemaining() throws IOException {
        if (!this.mBuffer.hasRemaining()) {
            uploadBufferInternal();
        }
    }

    private void uploadIfComplete() throws IOException {
        if (this.mBytesWritten == this.mContentLength) {
            uploadBufferInternal();
        }
    }

    private void uploadBufferInternal() throws IOException {
        checkNotClosed();
        this.mBuffer.flip();
        this.mMessageLoop.loop();
        checkNoException();
    }

    private void checkNotExceedContentLength(int numBytes) throws ProtocolException {
        if (this.mBytesWritten + ((long) numBytes) > this.mContentLength) {
            throw new ProtocolException("expected " + (this.mContentLength - this.mBytesWritten) + " bytes but received " + numBytes);
        }
    }

    void setConnected() throws IOException {
    }

    void checkReceivedEnoughContent() throws IOException {
        if (this.mBytesWritten < this.mContentLength) {
            throw new ProtocolException("Content received is less than Content-Length.");
        }
    }

    UploadDataProvider getUploadDataProvider() {
        return this.mUploadDataProvider;
    }

    @VisibleForTesting
    static void setDefaultBufferLengthForTesting(int length) {
        sDefaultBufferLength = length;
    }
}
