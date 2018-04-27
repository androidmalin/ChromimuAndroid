package org.chromium.net.urlconnection;

import android.annotation.SuppressLint;
import android.util.Pair;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import org.chromium.base.Log;
import org.chromium.net.CronetEngine;
import org.chromium.net.CronetException;
import org.chromium.net.UrlRequest;
import org.chromium.net.UrlRequest.Builder;
import org.chromium.net.UrlRequest.Callback;
import org.chromium.net.UrlResponseInfo;

public class CronetHttpURLConnection extends HttpURLConnection {
    private static final String CONTENT_LENGTH = "Content-Length";
    private static final String TAG = CronetHttpURLConnection.class.getSimpleName();
    private final CronetEngine mCronetEngine;
    private IOException mException;
    private boolean mHasResponseHeadersOrCompleted;
    private CronetInputStream mInputStream = new CronetInputStream(this);
    private final MessageLoop mMessageLoop = new MessageLoop();
    private boolean mOnRedirectCalled;
    private CronetOutputStream mOutputStream;
    private UrlRequest mRequest;
    private final List<Pair<String, String>> mRequestHeaders = new ArrayList();
    private List<Entry<String, String>> mResponseHeadersList;
    private Map<String, List<String>> mResponseHeadersMap;
    private UrlResponseInfo mResponseInfo;

    private class CronetUrlRequestCallback extends Callback {
        public void onResponseStarted(UrlRequest request, UrlResponseInfo info) {
            CronetHttpURLConnection.this.mResponseInfo = info;
            CronetHttpURLConnection.this.mHasResponseHeadersOrCompleted = true;
            CronetHttpURLConnection.this.mMessageLoop.quit();
        }

        public void onReadCompleted(UrlRequest request, UrlResponseInfo info, ByteBuffer byteBuffer) {
            CronetHttpURLConnection.this.mResponseInfo = info;
            CronetHttpURLConnection.this.mMessageLoop.quit();
        }

        public void onRedirectReceived(UrlRequest request, UrlResponseInfo info, String newLocationUrl) {
            CronetHttpURLConnection.this.mOnRedirectCalled = true;
            try {
                URL newUrl = new URL(newLocationUrl);
                boolean sameProtocol = newUrl.getProtocol().equals(CronetHttpURLConnection.this.url.getProtocol());
                if (CronetHttpURLConnection.this.instanceFollowRedirects) {
                    CronetHttpURLConnection.this.url = newUrl;
                }
                if (CronetHttpURLConnection.this.instanceFollowRedirects && sameProtocol) {
                    CronetHttpURLConnection.this.mRequest.followRedirect();
                    return;
                }
            } catch (MalformedURLException e) {
            }
            CronetHttpURLConnection.this.mResponseInfo = info;
            CronetHttpURLConnection.this.mRequest.cancel();
            setResponseDataCompleted(null);
        }

        public void onSucceeded(UrlRequest request, UrlResponseInfo info) {
            CronetHttpURLConnection.this.mResponseInfo = info;
            setResponseDataCompleted(null);
        }

        public void onFailed(UrlRequest request, UrlResponseInfo info, CronetException exception) {
            if (exception == null) {
                throw new IllegalStateException("Exception cannot be null in onFailed.");
            }
            CronetHttpURLConnection.this.mResponseInfo = info;
            setResponseDataCompleted(exception);
        }

        public void onCanceled(UrlRequest request, UrlResponseInfo info) {
            CronetHttpURLConnection.this.mResponseInfo = info;
            setResponseDataCompleted(new IOException("disconnect() called"));
        }

        private void setResponseDataCompleted(IOException exception) {
            CronetHttpURLConnection.this.mException = exception;
            if (CronetHttpURLConnection.this.mInputStream != null) {
                CronetHttpURLConnection.this.mInputStream.setResponseDataCompleted(exception);
            }
            if (CronetHttpURLConnection.this.mOutputStream != null) {
                CronetHttpURLConnection.this.mOutputStream.setRequestCompleted(exception);
            }
            CronetHttpURLConnection.this.mHasResponseHeadersOrCompleted = true;
            CronetHttpURLConnection.this.mMessageLoop.quit();
        }
    }

    public CronetHttpURLConnection(URL url, CronetEngine cronetEngine) {
        super(url);
        this.mCronetEngine = cronetEngine;
    }

    public void connect() throws IOException {
        getOutputStream();
        startRequest();
    }

    public void disconnect() {
        if (this.connected) {
            this.mRequest.cancel();
        }
    }

    public String getResponseMessage() throws IOException {
        getResponse();
        return this.mResponseInfo.getHttpStatusText();
    }

    public int getResponseCode() throws IOException {
        getResponse();
        return this.mResponseInfo.getHttpStatusCode();
    }

    public Map<String, List<String>> getHeaderFields() {
        try {
            getResponse();
            return getAllHeaders();
        } catch (IOException e) {
            return Collections.emptyMap();
        }
    }

    public final String getHeaderField(String fieldName) {
        try {
            getResponse();
            Map<String, List<String>> map = getAllHeaders();
            if (!map.containsKey(fieldName)) {
                return null;
            }
            List<String> values = (List) map.get(fieldName);
            return (String) values.get(values.size() - 1);
        } catch (IOException e) {
            return null;
        }
    }

    public final String getHeaderFieldKey(int pos) {
        Entry<String, String> header = getHeaderFieldEntry(pos);
        if (header == null) {
            return null;
        }
        return (String) header.getKey();
    }

    public final String getHeaderField(int pos) {
        Entry<String, String> header = getHeaderFieldEntry(pos);
        if (header == null) {
            return null;
        }
        return (String) header.getValue();
    }

    public InputStream getInputStream() throws IOException {
        getResponse();
        if (!this.instanceFollowRedirects && this.mOnRedirectCalled) {
            throw new IOException("Cannot read response body of a redirect.");
        } else if (this.mResponseInfo.getHttpStatusCode() < 400) {
            return this.mInputStream;
        } else {
            throw new FileNotFoundException(this.url.toString());
        }
    }

    public OutputStream getOutputStream() throws IOException {
        if (this.mOutputStream == null && this.doOutput) {
            if (this.connected) {
                throw new ProtocolException("Cannot write to OutputStream after receiving response.");
            } else if (isChunkedUpload()) {
                this.mOutputStream = new CronetChunkedOutputStream(this, this.chunkLength, this.mMessageLoop);
                startRequest();
            } else {
                long fixedStreamingModeContentLength = getStreamingModeContentLength();
                if (fixedStreamingModeContentLength != -1) {
                    this.mOutputStream = new CronetFixedModeOutputStream(this, fixedStreamingModeContentLength, this.mMessageLoop);
                    startRequest();
                } else {
                    Log.d(TAG, "Outputstream is being buffered in memory.");
                    String length = getRequestProperty(CONTENT_LENGTH);
                    if (length == null) {
                        this.mOutputStream = new CronetBufferedOutputStream(this);
                    } else {
                        this.mOutputStream = new CronetBufferedOutputStream(this, Long.parseLong(length));
                    }
                }
            }
        }
        return this.mOutputStream;
    }

    @SuppressLint({"NewApi"})
    private long getStreamingModeContentLength() {
        long contentLength = (long) this.fixedContentLength;
        try {
            long superFixedContentLengthLong = getClass().getField("fixedContentLengthLong").getLong(this);
            if (superFixedContentLengthLong != -1) {
                return superFixedContentLengthLong;
            }
            return contentLength;
        } catch (NoSuchFieldException e) {
            return contentLength;
        } catch (IllegalAccessException e2) {
            return contentLength;
        }
    }

    private void startRequest() throws IOException {
        if (!this.connected) {
            Builder requestBuilder = this.mCronetEngine.newUrlRequestBuilder(getURL().toString(), new CronetUrlRequestCallback(), this.mMessageLoop);
            if (this.doOutput) {
                if (this.method.equals("GET")) {
                    this.method = "POST";
                }
                if (this.mOutputStream != null) {
                    requestBuilder.setUploadDataProvider(this.mOutputStream.getUploadDataProvider(), this.mMessageLoop);
                    if (getRequestProperty(CONTENT_LENGTH) == null && !isChunkedUpload()) {
                        addRequestProperty(CONTENT_LENGTH, Long.toString(this.mOutputStream.getUploadDataProvider().getLength()));
                    }
                    this.mOutputStream.setConnected();
                } else if (getRequestProperty(CONTENT_LENGTH) == null) {
                    addRequestProperty(CONTENT_LENGTH, "0");
                }
                if (getRequestProperty("Content-Type") == null) {
                    addRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                }
            }
            for (Pair<String, String> requestHeader : this.mRequestHeaders) {
                requestBuilder.addHeader((String) requestHeader.first, (String) requestHeader.second);
            }
            if (!getUseCaches()) {
                requestBuilder.disableCache();
            }
            requestBuilder.setHttpMethod(this.method);
            this.connected = true;
            this.mRequest = requestBuilder.build();
            this.mRequest.start();
        }
    }

    public InputStream getErrorStream() {
        try {
            getResponse();
            if (this.mResponseInfo.getHttpStatusCode() >= 400) {
                return this.mInputStream;
            }
            return null;
        } catch (IOException e) {
            return null;
        }
    }

    public final void addRequestProperty(String key, String value) {
        setRequestPropertyInternal(key, value, false);
    }

    public final void setRequestProperty(String key, String value) {
        setRequestPropertyInternal(key, value, true);
    }

    private final void setRequestPropertyInternal(String key, String value, boolean overwrite) {
        if (this.connected) {
            throw new IllegalStateException("Cannot modify request property after connection is made.");
        }
        int index = findRequestProperty(key);
        if (index >= 0) {
            if (overwrite) {
                this.mRequestHeaders.remove(index);
            } else {
                throw new UnsupportedOperationException("Cannot add multiple headers of the same key, " + key + ". crbug.com/432719.");
            }
        }
        this.mRequestHeaders.add(Pair.create(key, value));
    }

    public Map<String, List<String>> getRequestProperties() {
        if (this.connected) {
            throw new IllegalStateException("Cannot access request headers after connection is set.");
        }
        Map<String, List<String>> map = new TreeMap(String.CASE_INSENSITIVE_ORDER);
        for (Pair<String, String> entry : this.mRequestHeaders) {
            if (map.containsKey(entry.first)) {
                throw new IllegalStateException("Should not have multiple values.");
            }
            List<String> values = new ArrayList();
            values.add((String) entry.second);
            map.put((String) entry.first, Collections.unmodifiableList(values));
        }
        return Collections.unmodifiableMap(map);
    }

    public String getRequestProperty(String key) {
        int index = findRequestProperty(key);
        if (index >= 0) {
            return (String) ((Pair) this.mRequestHeaders.get(index)).second;
        }
        return null;
    }

    public boolean usingProxy() {
        return false;
    }

    public void setConnectTimeout(int timeout) {
        Log.d(TAG, "setConnectTimeout is not supported by CronetHttpURLConnection");
    }

    void getMoreData(ByteBuffer byteBuffer) throws IOException {
        this.mRequest.read(byteBuffer);
        this.mMessageLoop.loop(getReadTimeout());
    }

    private int findRequestProperty(String key) {
        for (int i = 0; i < this.mRequestHeaders.size(); i++) {
            if (((String) ((Pair) this.mRequestHeaders.get(i)).first).equalsIgnoreCase(key)) {
                return i;
            }
        }
        return -1;
    }

    private void getResponse() throws IOException {
        if (this.mOutputStream != null) {
            this.mOutputStream.checkReceivedEnoughContent();
            if (isChunkedUpload()) {
                this.mOutputStream.close();
            }
        }
        if (!this.mHasResponseHeadersOrCompleted) {
            startRequest();
            this.mMessageLoop.loop();
        }
        checkHasResponseHeaders();
    }

    private void checkHasResponseHeaders() throws IOException {
        if (!this.mHasResponseHeadersOrCompleted) {
            throw new IllegalStateException("No response.");
        } else if (this.mException != null) {
            throw this.mException;
        } else if (this.mResponseInfo == null) {
            throw new NullPointerException("Response info is null when there is no exception.");
        }
    }

    private Entry<String, String> getHeaderFieldEntry(int pos) {
        try {
            getResponse();
            List<Entry<String, String>> headers = getAllHeadersAsList();
            if (pos >= headers.size()) {
                return null;
            }
            return (Entry) headers.get(pos);
        } catch (IOException e) {
            return null;
        }
    }

    private boolean isChunkedUpload() {
        return this.chunkLength > 0;
    }

    private Map<String, List<String>> getAllHeaders() {
        if (this.mResponseHeadersMap != null) {
            return this.mResponseHeadersMap;
        }
        Map<String, List<String>> map = new TreeMap(String.CASE_INSENSITIVE_ORDER);
        for (Entry<String, String> entry : getAllHeadersAsList()) {
            List<String> values = new ArrayList();
            if (map.containsKey(entry.getKey())) {
                values.addAll((Collection) map.get(entry.getKey()));
            }
            values.add((String) entry.getValue());
            map.put((String) entry.getKey(), Collections.unmodifiableList(values));
        }
        this.mResponseHeadersMap = Collections.unmodifiableMap(map);
        return this.mResponseHeadersMap;
    }

    private List<Entry<String, String>> getAllHeadersAsList() {
        if (this.mResponseHeadersList != null) {
            return this.mResponseHeadersList;
        }
        this.mResponseHeadersList = new ArrayList();
        for (Entry<String, String> entry : this.mResponseInfo.getAllHeadersAsList()) {
            if (!((String) entry.getKey()).equalsIgnoreCase("Content-Encoding")) {
                this.mResponseHeadersList.add(new SimpleImmutableEntry(entry));
            }
        }
        this.mResponseHeadersList = Collections.unmodifiableList(this.mResponseHeadersList);
        return this.mResponseHeadersList;
    }
}
