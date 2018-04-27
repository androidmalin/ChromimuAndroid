package org.chromium.net.impl;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
public @interface HttpCacheType {
    public static final int DISABLED = 0;
    public static final int DISK = 1;
    public static final int MEMORY = 2;
}
