package org.chromium.net;

import java.util.concurrent.Executor;

public abstract class ExperimentalUrlRequest extends UrlRequest {

    public static abstract class Builder extends org.chromium.net.UrlRequest.Builder {
        public abstract Builder addHeader(String str, String str2);

        public abstract Builder allowDirectExecutor();

        public abstract ExperimentalUrlRequest build();

        public abstract Builder disableCache();

        public abstract Builder setHttpMethod(String str);

        public abstract Builder setPriority(int i);

        public abstract Builder setUploadDataProvider(UploadDataProvider uploadDataProvider, Executor executor);

        public Builder disableConnectionMigration() {
            return this;
        }

        public Builder addRequestAnnotation(Object annotation) {
            return this;
        }
    }
}
