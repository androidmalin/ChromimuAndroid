package org.chromium.net;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;

public abstract class UploadDataProvider implements Closeable {
    public abstract long getLength() throws IOException;

    public abstract void read(UploadDataSink uploadDataSink, ByteBuffer byteBuffer) throws IOException;

    public abstract void rewind(UploadDataSink uploadDataSink) throws IOException;

    public void close() throws IOException {
    }
}
