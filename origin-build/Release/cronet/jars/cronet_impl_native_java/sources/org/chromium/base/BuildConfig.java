package org.chromium.base;

public class BuildConfig {
    public static final String[] COMPRESSED_LOCALES = new String[0];
    public static final boolean DCHECK_IS_ON = false;
    public static final String[] UNCOMPRESSED_LOCALES = new String[0];

    public static boolean isMultidexEnabled() {
        return false;
    }
}
