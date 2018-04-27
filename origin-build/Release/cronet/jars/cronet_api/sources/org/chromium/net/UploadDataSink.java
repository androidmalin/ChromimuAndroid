package org.chromium.net;

public abstract class UploadDataSink {
    public abstract void onReadError(Exception exception);

    public abstract void onReadSucceeded(boolean z);

    public abstract void onRewindError(Exception exception);

    public abstract void onRewindSucceeded();
}
