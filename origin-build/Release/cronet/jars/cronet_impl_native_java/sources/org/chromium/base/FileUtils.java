package org.chromium.base;

import android.content.Context;
import android.net.Uri;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class FileUtils {
    private static final String TAG = "FileUtils";

    public static void recursivelyDeleteFile(File currentFile) {
        ThreadUtils.assertOnBackgroundThread();
        if (currentFile.isDirectory()) {
            File[] files = currentFile.listFiles();
            if (files != null) {
                for (File file : files) {
                    recursivelyDeleteFile(file);
                }
            }
        }
        if (!currentFile.delete()) {
            Log.e(TAG, "Failed to delete: " + currentFile, new Object[0]);
        }
    }

    public static void batchDeleteFiles(List<File> files) {
        ThreadUtils.assertOnBackgroundThread();
        for (File file : files) {
            if (file.exists()) {
                recursivelyDeleteFile(file);
            }
        }
    }

    public static boolean extractAsset(Context context, String assetName, File dest) {
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            inputStream = context.getAssets().open(assetName);
            OutputStream outputStream2 = new BufferedOutputStream(new FileOutputStream(dest));
            try {
                byte[] buffer = new byte[8192];
                while (true) {
                    int c = inputStream.read(buffer);
                    if (c != -1) {
                        outputStream2.write(buffer, 0, c);
                    } else {
                        inputStream.close();
                        outputStream2.close();
                        outputStream = outputStream2;
                        return true;
                    }
                }
            } catch (IOException e) {
                outputStream = outputStream2;
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e2) {
                    }
                }
                if (outputStream != null) {
                    return false;
                }
                try {
                    outputStream.close();
                    return false;
                } catch (IOException e3) {
                    return false;
                }
            }
        } catch (IOException e4) {
            if (inputStream != null) {
                inputStream.close();
            }
            if (outputStream != null) {
                return false;
            }
            outputStream.close();
            return false;
        }
    }

    public static Uri getUriForFile(File file) {
        Uri uri = null;
        try {
            uri = ContentUriUtils.getContentUriFromFile(file);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Could not create content uri: " + e, new Object[0]);
        }
        if (uri == null) {
            return Uri.fromFile(file);
        }
        return uri;
    }
}
