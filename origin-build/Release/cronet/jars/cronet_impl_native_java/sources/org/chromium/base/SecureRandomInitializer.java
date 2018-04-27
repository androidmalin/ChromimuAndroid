package org.chromium.base;

import android.annotation.SuppressLint;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.SecureRandom;

@SuppressLint({"SecureRandom"})
public class SecureRandomInitializer {
    private static final int NUM_RANDOM_BYTES = 16;

    public static void initialize(SecureRandom generator) throws IOException {
        Throwable th;
        FileInputStream fis = new FileInputStream("/dev/urandom");
        Throwable th2 = null;
        try {
            byte[] seedBytes = new byte[16];
            if (fis.read(seedBytes) != seedBytes.length) {
                throw new IOException("Failed to get enough random data.");
            }
            generator.setSeed(seedBytes);
            if (th2 != null) {
                try {
                    fis.close();
                    return;
                } catch (Throwable th3) {
                    th2.addSuppressed(th3);
                    return;
                }
            }
            fis.close();
            return;
        } catch (Throwable th22) {
            Throwable th4 = th22;
            th22 = th3;
            th3 = th4;
        }
        if (th22 != null) {
            try {
                fis.close();
            } catch (Throwable th5) {
                th22.addSuppressed(th5);
            }
        } else {
            fis.close();
        }
        throw th3;
        throw th3;
    }
}
