package org.chromium.net.impl;

import android.support.annotation.NonNull;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.concurrent.atomic.AtomicBoolean;

final class InputStreamChannel implements ReadableByteChannel {
    private static final int MAX_TMP_BUFFER_SIZE = 16384;
    private static final int MIN_TMP_BUFFER_SIZE = 4096;
    private final InputStream mInputStream;
    private final AtomicBoolean mIsOpen = new AtomicBoolean(true);

    private InputStreamChannel(@NonNull InputStream inputStream) {
        this.mInputStream = inputStream;
    }

    static ReadableByteChannel wrap(@NonNull InputStream inputStream) {
        if (inputStream instanceof FileInputStream) {
            return ((FileInputStream) inputStream).getChannel();
        }
        return new InputStreamChannel(inputStream);
    }

    public int read(ByteBuffer dst) throws IOException {
        int read;
        if (dst.hasArray()) {
            read = this.mInputStream.read(dst.array(), dst.arrayOffset() + dst.position(), dst.remaining());
            if (read > 0) {
                dst.position(dst.position() + read);
            }
        } else {
            byte[] tmpBuf = new byte[Math.min(MAX_TMP_BUFFER_SIZE, Math.min(Math.max(this.mInputStream.available(), MIN_TMP_BUFFER_SIZE), dst.remaining()))];
            read = this.mInputStream.read(tmpBuf);
            if (read > 0) {
                dst.put(tmpBuf, 0, read);
            }
        }
        return read;
    }

    public boolean isOpen() {
        return this.mIsOpen.get();
    }

    public void close() throws IOException {
        if (this.mIsOpen.compareAndSet(true, false)) {
            this.mInputStream.close();
        }
    }
}
