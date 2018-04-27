// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.net.impl;

// chromium/src/out/Release/gen/components/cronet/android/templates/cronet_impl_version_java/org/chromium/net/impl/ImplVersion.java
// Version based on chrome/VERSION.
public class ImplVersion {
    private static final String CRONET_VERSION = "65.0.3289.0";
    private static final int API_LEVEL = 8;
    private static final String LAST_CHANGE = "b357085d75686a4a276a4b829b7faff0e1aaa897-refs/heads/master@{#522969}";

    /**
     * Private constructor. All members of this class should be static.
     */
    private ImplVersion() {
    }

    public static String getCronetVersionWithLastChange() {
        return CRONET_VERSION + "@" + LAST_CHANGE.substring(0, 8);
    }

    public static int getApiLevel() {
        return API_LEVEL;
    }

    public static String getCronetVersion() {
        return CRONET_VERSION;
    }

    public static String getLastChange() {
        return LAST_CHANGE;
    }
}
