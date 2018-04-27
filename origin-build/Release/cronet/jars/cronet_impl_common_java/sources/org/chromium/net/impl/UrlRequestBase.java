package org.chromium.net.impl;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.Executor;
import org.chromium.net.ExperimentalUrlRequest;
import org.chromium.net.UploadDataProvider;

public abstract class UrlRequestBase extends ExperimentalUrlRequest {
    static final /* synthetic */ boolean $assertionsDisabled = (!UrlRequestBase.class.desiredAssertionStatus());

    @Retention(RetentionPolicy.SOURCE)
    public @interface StatusValues {
    }

    protected abstract void addHeader(String str, String str2);

    protected abstract void setHttpMethod(String str);

    protected abstract void setUploadDataProvider(UploadDataProvider uploadDataProvider, Executor executor);

    public static int convertLoadState(int loadState) {
        if ($assertionsDisabled || (loadState >= 0 && loadState <= 16)) {
            switch (loadState) {
                case 0:
                    return 0;
                case 2:
                    return 1;
                case 3:
                    return 2;
                case 4:
                    return 3;
                case 5:
                    return 4;
                case LoadState.DOWNLOADING_PROXY_SCRIPT /*7*/:
                    return 5;
                case LoadState.RESOLVING_PROXY_FOR_URL /*8*/:
                    return 6;
                case LoadState.RESOLVING_HOST_IN_PROXY_SCRIPT /*9*/:
                    return 7;
                case LoadState.ESTABLISHING_PROXY_TUNNEL /*10*/:
                    return 8;
                case LoadState.RESOLVING_HOST /*11*/:
                    return 9;
                case LoadState.CONNECTING /*12*/:
                    return 10;
                case LoadState.SSL_HANDSHAKE /*13*/:
                    return 11;
                case LoadState.SENDING_REQUEST /*14*/:
                    return 12;
                case LoadState.WAITING_FOR_RESPONSE /*15*/:
                    return 13;
                case LoadState.READING_RESPONSE /*16*/:
                    return 14;
                default:
                    throw new IllegalArgumentException("No request status found.");
            }
        }
        throw new AssertionError();
    }
}
