package org.chromium.base;

import android.content.res.AssetFileDescriptor;
import android.util.Log;
import java.io.IOException;
import org.chromium.base.annotations.CalledByNative;
import org.chromium.base.annotations.JNINamespace;

@JNINamespace("base::android")
public class ApkAssets {
    private static final String LOGTAG = "ApkAssets";

    @CalledByNative
    public static long[] open(String fileName) {
        long[] jArr;
        AssetFileDescriptor afd = null;
        try {
            afd = ContextUtils.getApplicationContext().getAssets().openNonAssetFd(fileName);
            jArr = new long[]{(long) afd.getParcelFileDescriptor().detachFd(), afd.getStartOffset(), afd.getLength()};
            if (afd != null) {
                try {
                    afd.close();
                } catch (IOException e2) {
                    Log.e(LOGTAG, "Unable to close AssetFileDescriptor", e2);
                }
            }
        } catch (IOException e) {
            if (!(e.getMessage().equals("") || e.getMessage().equals(fileName))) {
                Log.e(LOGTAG, "Error while loading asset " + fileName + ": " + e);
            }
            jArr = new long[3];
            jArr = new long[]{-1, -1, -1};
            if (afd != null) {
                try {
                    afd.close();
                } catch (IOException e22) {
                    Log.e(LOGTAG, "Unable to close AssetFileDescriptor", e22);
                }
            }
        } catch (Throwable th) {
            if (afd != null) {
                try {
                    afd.close();
                } catch (IOException e222) {
                    Log.e(LOGTAG, "Unable to close AssetFileDescriptor", e222);
                }
            }
        }
        return jArr;
    }
}
