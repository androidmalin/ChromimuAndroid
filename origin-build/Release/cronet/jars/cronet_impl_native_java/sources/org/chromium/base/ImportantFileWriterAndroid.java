package org.chromium.base;

import org.chromium.base.annotations.JNINamespace;

@JNINamespace("base::android")
public class ImportantFileWriterAndroid {
    private static native boolean nativeWriteFileAtomically(String str, byte[] bArr);

    public static boolean writeFileAtomically(String fileName, byte[] data) {
        return nativeWriteFileAtomically(fileName, data);
    }
}
