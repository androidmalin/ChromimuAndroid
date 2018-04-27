package org.chromium.base;

import java.io.Closeable;
import java.io.IOException;

public class StreamUtil {
    public static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
            }
        }
    }
}
