package org.chromium.net.impl;

public class ImplVersion {
    private static final int API_LEVEL = 8;
    private static final String CRONET_VERSION = "65.0.3289.0";
    private static final String LAST_CHANGE = "b357085d75686a4a276a4b829b7faff0e1aaa897-refs/heads/master@{#522969}";

    private ImplVersion() {
    }

    public static String getCronetVersionWithLastChange() {
        return "65.0.3289.0@" + LAST_CHANGE.substring(0, 8);
    }

    public static int getApiLevel() {
        return 8;
    }

    public static String getCronetVersion() {
        return CRONET_VERSION;
    }

    public static String getLastChange() {
        return LAST_CHANGE;
    }
}
