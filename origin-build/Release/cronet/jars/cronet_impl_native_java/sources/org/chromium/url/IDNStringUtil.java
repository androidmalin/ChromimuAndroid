package org.chromium.url;

import java.net.IDN;
import org.chromium.base.annotations.CalledByNative;
import org.chromium.base.annotations.JNINamespace;

@JNINamespace("url::android")
public class IDNStringUtil {
    @CalledByNative
    private static String idnToASCII(String src) {
        try {
            return IDN.toASCII(src, 2);
        } catch (Exception e) {
            return null;
        }
    }
}
