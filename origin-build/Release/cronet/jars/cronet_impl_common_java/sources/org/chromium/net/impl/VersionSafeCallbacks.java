package org.chromium.net.impl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.Executor;
import org.chromium.net.BidirectionalStream;
import org.chromium.net.BidirectionalStream.Callback;
import org.chromium.net.CronetException;
import org.chromium.net.NetworkQualityRttListener;
import org.chromium.net.NetworkQualityThroughputListener;
import org.chromium.net.RequestFinishedInfo;
import org.chromium.net.RequestFinishedInfo.Listener;
import org.chromium.net.UploadDataProvider;
import org.chromium.net.UploadDataSink;
import org.chromium.net.UrlRequest;
import org.chromium.net.UrlRequest.StatusListener;
import org.chromium.net.UrlResponseInfo;
import org.chromium.net.UrlResponseInfo.HeaderBlock;

public class VersionSafeCallbacks {

    public static final class BidirectionalStreamCallback extends Callback {
        private final Callback mWrappedCallback;

        public BidirectionalStreamCallback(Callback callback) {
            this.mWrappedCallback = callback;
        }

        public void onStreamReady(BidirectionalStream stream) {
            this.mWrappedCallback.onStreamReady(stream);
        }

        public void onResponseHeadersReceived(BidirectionalStream stream, UrlResponseInfo info) {
            this.mWrappedCallback.onResponseHeadersReceived(stream, info);
        }

        public void onReadCompleted(BidirectionalStream stream, UrlResponseInfo info, ByteBuffer buffer, boolean endOfStream) {
            this.mWrappedCallback.onReadCompleted(stream, info, buffer, endOfStream);
        }

        public void onWriteCompleted(BidirectionalStream stream, UrlResponseInfo info, ByteBuffer buffer, boolean endOfStream) {
            this.mWrappedCallback.onWriteCompleted(stream, info, buffer, endOfStream);
        }

        public void onResponseTrailersReceived(BidirectionalStream stream, UrlResponseInfo info, HeaderBlock trailers) {
            this.mWrappedCallback.onResponseTrailersReceived(stream, info, trailers);
        }

        public void onSucceeded(BidirectionalStream stream, UrlResponseInfo info) {
            this.mWrappedCallback.onSucceeded(stream, info);
        }

        public void onFailed(BidirectionalStream stream, UrlResponseInfo info, CronetException error) {
            this.mWrappedCallback.onFailed(stream, info, error);
        }

        public void onCanceled(BidirectionalStream stream, UrlResponseInfo info) {
            this.mWrappedCallback.onCanceled(stream, info);
        }
    }

    public static final class LibraryLoader extends org.chromium.net.CronetEngine.Builder.LibraryLoader {
        private final org.chromium.net.CronetEngine.Builder.LibraryLoader mWrappedLoader;

        public LibraryLoader(org.chromium.net.CronetEngine.Builder.LibraryLoader libraryLoader) {
            this.mWrappedLoader = libraryLoader;
        }

        public void loadLibrary(String libName) {
            this.mWrappedLoader.loadLibrary(libName);
        }
    }

    public static final class NetworkQualityRttListenerWrapper extends NetworkQualityRttListener {
        private final NetworkQualityRttListener mWrappedListener;

        public NetworkQualityRttListenerWrapper(NetworkQualityRttListener listener) {
            super(listener.getExecutor());
            this.mWrappedListener = listener;
        }

        public void onRttObservation(int rttMs, long whenMs, int source) {
            this.mWrappedListener.onRttObservation(rttMs, whenMs, source);
        }

        public Executor getExecutor() {
            return this.mWrappedListener.getExecutor();
        }

        public int hashCode() {
            return this.mWrappedListener.hashCode();
        }

        public boolean equals(Object o) {
            if (o == null || !(o instanceof NetworkQualityRttListenerWrapper)) {
                return false;
            }
            return this.mWrappedListener.equals(((NetworkQualityRttListenerWrapper) o).mWrappedListener);
        }
    }

    public static final class NetworkQualityThroughputListenerWrapper extends NetworkQualityThroughputListener {
        private final NetworkQualityThroughputListener mWrappedListener;

        public NetworkQualityThroughputListenerWrapper(NetworkQualityThroughputListener listener) {
            super(listener.getExecutor());
            this.mWrappedListener = listener;
        }

        public void onThroughputObservation(int throughputKbps, long whenMs, int source) {
            this.mWrappedListener.onThroughputObservation(throughputKbps, whenMs, source);
        }

        public Executor getExecutor() {
            return this.mWrappedListener.getExecutor();
        }

        public int hashCode() {
            return this.mWrappedListener.hashCode();
        }

        public boolean equals(Object o) {
            if (o == null || !(o instanceof NetworkQualityThroughputListenerWrapper)) {
                return false;
            }
            return this.mWrappedListener.equals(((NetworkQualityThroughputListenerWrapper) o).mWrappedListener);
        }
    }

    public static final class RequestFinishedInfoListener extends Listener {
        private final Listener mWrappedListener;

        public RequestFinishedInfoListener(Listener listener) {
            super(listener.getExecutor());
            this.mWrappedListener = listener;
        }

        public void onRequestFinished(RequestFinishedInfo requestInfo) {
            this.mWrappedListener.onRequestFinished(requestInfo);
        }

        public Executor getExecutor() {
            return this.mWrappedListener.getExecutor();
        }
    }

    public static final class UploadDataProviderWrapper extends UploadDataProvider {
        private final UploadDataProvider mWrappedProvider;

        public UploadDataProviderWrapper(UploadDataProvider provider) {
            this.mWrappedProvider = provider;
        }

        public long getLength() throws IOException {
            return this.mWrappedProvider.getLength();
        }

        public void read(UploadDataSink uploadDataSink, ByteBuffer byteBuffer) throws IOException {
            this.mWrappedProvider.read(uploadDataSink, byteBuffer);
        }

        public void rewind(UploadDataSink uploadDataSink) throws IOException {
            this.mWrappedProvider.rewind(uploadDataSink);
        }

        public void close() throws IOException {
            this.mWrappedProvider.close();
        }
    }

    public static final class UrlRequestCallback extends UrlRequest.Callback {
        private final UrlRequest.Callback mWrappedCallback;

        public UrlRequestCallback(UrlRequest.Callback callback) {
            this.mWrappedCallback = callback;
        }

        public void onRedirectReceived(UrlRequest request, UrlResponseInfo info, String newLocationUrl) throws Exception {
            this.mWrappedCallback.onRedirectReceived(request, info, newLocationUrl);
        }

        public void onResponseStarted(UrlRequest request, UrlResponseInfo info) throws Exception {
            this.mWrappedCallback.onResponseStarted(request, info);
        }

        public void onReadCompleted(UrlRequest request, UrlResponseInfo info, ByteBuffer byteBuffer) throws Exception {
            this.mWrappedCallback.onReadCompleted(request, info, byteBuffer);
        }

        public void onSucceeded(UrlRequest request, UrlResponseInfo info) {
            this.mWrappedCallback.onSucceeded(request, info);
        }

        public void onFailed(UrlRequest request, UrlResponseInfo info, CronetException error) {
            this.mWrappedCallback.onFailed(request, info, error);
        }

        public void onCanceled(UrlRequest request, UrlResponseInfo info) {
            this.mWrappedCallback.onCanceled(request, info);
        }
    }

    public static final class UrlRequestStatusListener extends StatusListener {
        private final StatusListener mWrappedListener;

        public UrlRequestStatusListener(StatusListener listener) {
            this.mWrappedListener = listener;
        }

        public void onStatus(int status) {
            this.mWrappedListener.onStatus(status);
        }
    }
}
