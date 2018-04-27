package org.chromium.base.library_loader;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
public @interface LibraryLoadFromApkStatusCodes {
    public static final int MAX = 6;
    public static final int NOT_SUPPORTED_OBSOLETE = 1;
    public static final int SUCCESSFUL = 3;
    public static final int SUPPORTED_OBSOLETE = 2;
    public static final int UNKNOWN = 0;
    public static final int USED_NO_MAP_EXEC_SUPPORT_FALLBACK_OBSOLETE = 5;
    public static final int USED_UNPACK_LIBRARY_FALLBACK = 4;
}
