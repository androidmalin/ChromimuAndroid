package org.chromium.net;

public abstract class ExperimentalBidirectionalStream extends BidirectionalStream {

    public static abstract class Builder extends org.chromium.net.BidirectionalStream.Builder {
        public abstract Builder addHeader(String str, String str2);

        public abstract ExperimentalBidirectionalStream build();

        public abstract Builder delayRequestHeadersUntilFirstFlush(boolean z);

        public abstract Builder setHttpMethod(String str);

        public abstract Builder setPriority(int i);

        public Builder addRequestAnnotation(Object annotation) {
            return this;
        }
    }
}
