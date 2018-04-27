package org.chromium.net;

import android.os.ParcelFileDescriptor;
import android.os.ParcelFileDescriptor.AutoCloseInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public final class UploadDataProviders {

    private interface FileChannelProvider {
        FileChannel getChannel() throws IOException;
    }

    /* renamed from: org.chromium.net.UploadDataProviders$1 */
    class AnonymousClass1 implements FileChannelProvider {
        final /* synthetic */ File val$file;

        AnonymousClass1(File file) {
            this.val$file = file;
        }

        public FileChannel getChannel() throws IOException {
            return new FileInputStream(this.val$file).getChannel();
        }
    }

    /* renamed from: org.chromium.net.UploadDataProviders$2 */
    class AnonymousClass2 implements FileChannelProvider {
        final /* synthetic */ ParcelFileDescriptor val$fd;

        AnonymousClass2(ParcelFileDescriptor parcelFileDescriptor) {
            this.val$fd = parcelFileDescriptor;
        }

        public FileChannel getChannel() throws IOException {
            if (this.val$fd.getStatSize() != -1) {
                return new AutoCloseInputStream(this.val$fd).getChannel();
            }
            this.val$fd.close();
            throw new IllegalArgumentException("Not a file: " + this.val$fd);
        }
    }

    private static final class ByteBufferUploadProvider extends UploadDataProvider {
        private final ByteBuffer mUploadBuffer;

        private ByteBufferUploadProvider(ByteBuffer uploadBuffer) {
            this.mUploadBuffer = uploadBuffer;
        }

        public long getLength() {
            return (long) this.mUploadBuffer.limit();
        }

        public void read(UploadDataSink uploadDataSink, ByteBuffer byteBuffer) {
            if (byteBuffer.hasRemaining()) {
                if (byteBuffer.remaining() >= this.mUploadBuffer.remaining()) {
                    byteBuffer.put(this.mUploadBuffer);
                } else {
                    int oldLimit = this.mUploadBuffer.limit();
                    this.mUploadBuffer.limit(this.mUploadBuffer.position() + byteBuffer.remaining());
                    byteBuffer.put(this.mUploadBuffer);
                    this.mUploadBuffer.limit(oldLimit);
                }
                uploadDataSink.onReadSucceeded(false);
                return;
            }
            throw new IllegalStateException("Cronet passed a buffer with no bytes remaining");
        }

        public void rewind(UploadDataSink uploadDataSink) {
            this.mUploadBuffer.position(0);
            uploadDataSink.onRewindSucceeded();
        }
    }

    private static final class FileUploadProvider extends UploadDataProvider {
        private volatile FileChannel mChannel;
        private final Object mLock;
        private final FileChannelProvider mProvider;

        private FileUploadProvider(FileChannelProvider provider) {
            this.mLock = new Object();
            this.mProvider = provider;
        }

        public long getLength() throws IOException {
            return getChannel().size();
        }

        public void read(UploadDataSink uploadDataSink, ByteBuffer byteBuffer) throws IOException {
            if (byteBuffer.hasRemaining()) {
                FileChannel channel = getChannel();
                int bytesRead = 0;
                while (bytesRead == 0) {
                    int read = channel.read(byteBuffer);
                    if (read == -1) {
                        break;
                    }
                    bytesRead += read;
                }
                uploadDataSink.onReadSucceeded(false);
                return;
            }
            throw new IllegalStateException("Cronet passed a buffer with no bytes remaining");
        }

        public void rewind(UploadDataSink uploadDataSink) throws IOException {
            getChannel().position(0);
            uploadDataSink.onRewindSucceeded();
        }

        private FileChannel getChannel() throws IOException {
            if (this.mChannel == null) {
                synchronized (this.mLock) {
                    if (this.mChannel == null) {
                        this.mChannel = this.mProvider.getChannel();
                    }
                }
            }
            return this.mChannel;
        }

        public void close() throws IOException {
            FileChannel channel = this.mChannel;
            if (channel != null) {
                channel.close();
            }
        }
    }

    public static UploadDataProvider create(File file) {
        return new FileUploadProvider(new AnonymousClass1(file));
    }

    public static UploadDataProvider create(ParcelFileDescriptor fd) {
        return new FileUploadProvider(new AnonymousClass2(fd));
    }

    public static UploadDataProvider create(ByteBuffer buffer) {
        return new ByteBufferUploadProvider(buffer.slice());
    }

    public static UploadDataProvider create(byte[] data, int offset, int length) {
        return new ByteBufferUploadProvider(ByteBuffer.wrap(data, offset, length).slice());
    }

    public static UploadDataProvider create(byte[] data) {
        return create(data, 0, data.length);
    }

    private UploadDataProviders() {
    }
}
