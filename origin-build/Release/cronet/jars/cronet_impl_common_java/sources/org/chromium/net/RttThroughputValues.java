package org.chromium.net;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
public @interface RttThroughputValues {
    public static final int INVALID_RTT_THROUGHPUT = -1;
}
