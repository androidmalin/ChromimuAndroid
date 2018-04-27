package org.chromium.base;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.util.Log;
import android.webkit.MimeTypeMap;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.chromium.base.annotations.CalledByNative;

public abstract class ContentUriUtils {
    private static final String TAG = "ContentUriUtils";
    private static FileProviderUtil sFileProviderUtil;
    private static final Object sLock = new Object();

    public interface FileProviderUtil {
        Uri getContentUriFromFile(File file);
    }

    private ContentUriUtils() {
    }

    public static void setFileProviderUtil(FileProviderUtil util) {
        synchronized (sLock) {
            sFileProviderUtil = util;
        }
    }

    public static Uri getContentUriFromFile(File file) {
        synchronized (sLock) {
            if (sFileProviderUtil != null) {
                Uri contentUriFromFile = sFileProviderUtil.getContentUriFromFile(file);
                return contentUriFromFile;
            }
            return null;
        }
    }

    @CalledByNative
    public static int openContentUriForRead(String uriString) {
        AssetFileDescriptor afd = getAssetFileDescriptor(uriString);
        if (afd != null) {
            return afd.getParcelFileDescriptor().detachFd();
        }
        return -1;
    }

    @CalledByNative
    public static boolean contentUriExists(String uriString) {
        AssetFileDescriptor asf = null;
        try {
            asf = getAssetFileDescriptor(uriString);
            boolean z = asf != null;
            if (asf != null) {
                try {
                    asf.close();
                } catch (IOException e) {
                }
            }
            return z;
        } catch (Throwable th) {
            if (asf != null) {
                try {
                    asf.close();
                } catch (IOException e2) {
                }
            }
        }
    }

    @CalledByNative
    public static String getMimeType(String uriString) {
        ContentResolver resolver = ContextUtils.getApplicationContext().getContentResolver();
        Uri uri = Uri.parse(uriString);
        if (!isVirtualDocument(uri)) {
            return resolver.getType(uri);
        }
        String[] streamTypes = resolver.getStreamTypes(uri, "*/*");
        return (streamTypes == null || streamTypes.length <= 0) ? null : streamTypes[0];
    }

    private static AssetFileDescriptor getAssetFileDescriptor(String uriString) {
        ContentResolver resolver = ContextUtils.getApplicationContext().getContentResolver();
        Uri uri = Uri.parse(uriString);
        try {
            if (isVirtualDocument(uri)) {
                String[] streamTypes = resolver.getStreamTypes(uri, "*/*");
                if (streamTypes != null && streamTypes.length > 0) {
                    AssetFileDescriptor openTypedAssetFileDescriptor = resolver.openTypedAssetFileDescriptor(uri, streamTypes[0], null);
                    if (openTypedAssetFileDescriptor == null || openTypedAssetFileDescriptor.getStartOffset() == 0) {
                        return openTypedAssetFileDescriptor;
                    }
                    try {
                        openTypedAssetFileDescriptor.close();
                    } catch (IOException e) {
                    }
                    throw new SecurityException("Cannot open files with non-zero offset type.");
                }
                return null;
            }
            ParcelFileDescriptor pfd = resolver.openFileDescriptor(uri, "r");
            if (pfd != null) {
                return new AssetFileDescriptor(pfd, 0, -1);
            }
            return null;
        } catch (FileNotFoundException e2) {
            Log.w(TAG, "Cannot find content uri: " + uriString, e2);
        } catch (SecurityException e3) {
            Log.w(TAG, "Cannot open content uri: " + uriString, e3);
        } catch (Exception e4) {
            Log.w(TAG, "Unknown content uri: " + uriString, e4);
        }
    }

    public static String getDisplayName(Uri uri, Context context, String columnField) {
        Throwable th;
        Throwable th2;
        if (uri == null) {
            return "";
        }
        Cursor cursor;
        ContentResolver contentResolver = context.getContentResolver();
        try {
            cursor = contentResolver.query(uri, null, null, null, null);
            th = null;
            if (cursor != null) {
                try {
                    if (cursor.getCount() >= 1) {
                        cursor.moveToFirst();
                        int displayNameIndex = cursor.getColumnIndex(columnField);
                        String str;
                        if (displayNameIndex == -1) {
                            str = "";
                            if (cursor == null) {
                                return str;
                            }
                            $closeResource(null, cursor);
                            return str;
                        }
                        str = cursor.getString(displayNameIndex);
                        if (hasVirtualFlag(cursor)) {
                            String[] mimeTypes = contentResolver.getStreamTypes(uri, "*/*");
                            if (mimeTypes != null && mimeTypes.length > 0) {
                                String ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeTypes[0]);
                                if (ext != null) {
                                    str = str + "." + ext;
                                }
                            }
                        }
                        if (cursor == null) {
                            return str;
                        }
                        $closeResource(null, cursor);
                        return str;
                    }
                } catch (Throwable th3) {
                    Throwable th4 = th3;
                    th3 = th2;
                    th2 = th4;
                }
            }
            if (cursor != null) {
                $closeResource(null, cursor);
            }
            return "";
        } catch (NullPointerException e) {
            return "";
        }
        if (cursor != null) {
            $closeResource(th3, cursor);
        }
        throw th2;
    }

    private static /* synthetic */ void $closeResource(Throwable x0, AutoCloseable x1) {
        if (x0 != null) {
            try {
                x1.close();
                return;
            } catch (Throwable th) {
                x0.addSuppressed(th);
                return;
            }
        }
        x1.close();
    }

    private static boolean isVirtualDocument(Uri uri) {
        Throwable th;
        Throwable th2;
        if (VERSION.SDK_INT < 19) {
            return false;
        }
        if (uri == null) {
            return false;
        }
        if (!DocumentsContract.isDocumentUri(ContextUtils.getApplicationContext(), uri)) {
            return false;
        }
        Cursor cursor;
        try {
            cursor = ContextUtils.getApplicationContext().getContentResolver().query(uri, null, null, null, null);
            if (cursor != null) {
                try {
                    if (cursor.getCount() >= 1) {
                        cursor.moveToFirst();
                        boolean hasVirtualFlag = hasVirtualFlag(cursor);
                        if (cursor == null) {
                            return hasVirtualFlag;
                        }
                        $closeResource(null, cursor);
                        return hasVirtualFlag;
                    }
                } catch (Throwable th22) {
                    Throwable th3 = th22;
                    th22 = th;
                    th = th3;
                }
            }
            if (cursor != null) {
                $closeResource(null, cursor);
            }
            return false;
        } catch (NullPointerException e) {
            return false;
        }
        if (cursor != null) {
            $closeResource(th22, cursor);
        }
        throw th;
    }

    private static boolean hasVirtualFlag(Cursor cursor) {
        if (VERSION.SDK_INT < 24) {
            return false;
        }
        int index = cursor.getColumnIndex("flags");
        if (index <= -1 || (cursor.getLong(index) & 512) == 0) {
            return false;
        }
        return true;
    }
}
