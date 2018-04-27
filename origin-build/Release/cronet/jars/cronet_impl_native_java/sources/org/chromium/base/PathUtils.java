package org.chromium.base;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.SystemClock;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.chromium.base.annotations.CalledByNative;
import org.chromium.base.annotations.MainDex;
import org.chromium.base.metrics.RecordHistogram;

@MainDex
public abstract class PathUtils {
    static final /* synthetic */ boolean $assertionsDisabled;
    private static final int CACHE_DIRECTORY = 3;
    private static final int DATABASE_DIRECTORY = 2;
    private static final int DATA_DIRECTORY = 0;
    private static final int DOWNLOAD_INTERNAL_DIRECTORY = 4;
    private static final String DOWNLOAD_INTERNAL_DIRECTORY_NAME = "download_internal";
    private static final int NUM_DIRECTORIES = 5;
    private static final int THUMBNAIL_DIRECTORY = 1;
    private static final String THUMBNAIL_DIRECTORY_NAME = "textures";
    private static String sDataDirectorySuffix;
    private static AsyncTask<Void, Void, String[]> sDirPathFetchTask;
    private static final AtomicBoolean sInitializationStarted = new AtomicBoolean();

    private static class Holder {
        private static final String[] DIRECTORY_PATHS = PathUtils.getOrComputeDirectoryPaths();

        private Holder() {
        }
    }

    static {
        boolean z;
        if (PathUtils.class.desiredAssertionStatus()) {
            z = $assertionsDisabled;
        } else {
            z = true;
        }
        $assertionsDisabled = z;
    }

    private PathUtils() {
    }

    private static String[] getOrComputeDirectoryPaths() {
        try {
            StrictModeContext unused;
            Throwable th;
            Throwable th2;
            if (!sDirPathFetchTask.cancel($assertionsDisabled)) {
                return (String[]) sDirPathFetchTask.get();
            }
            unused = StrictModeContext.allowDiskWrites();
            try {
                String[] privateDataDirectorySuffixInternal = setPrivateDataDirectorySuffixInternal();
                if (unused == null) {
                    return privateDataDirectorySuffixInternal;
                }
                $closeResource(null, unused);
                return privateDataDirectorySuffixInternal;
            } catch (Throwable th3) {
                th = th3;
            }
            return null;
            if (unused != null) {
                $closeResource(th2, unused);
            }
            throw th;
        } catch (InterruptedException e) {
        } catch (ExecutionException e2) {
        }
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

    private static String[] setPrivateDataDirectorySuffixInternal() {
        paths = new String[5];
        Context appContext = ContextUtils.getApplicationContext();
        paths[0] = appContext.getDir(sDataDirectorySuffix, 0).getPath();
        paths[1] = appContext.getDir(THUMBNAIL_DIRECTORY_NAME, 0).getPath();
        paths[4] = appContext.getDir(DOWNLOAD_INTERNAL_DIRECTORY_NAME, 0).getPath();
        paths[2] = appContext.getDatabasePath("foo").getParent();
        if (appContext.getCacheDir() != null) {
            paths[3] = appContext.getCacheDir().getPath();
        }
        return paths;
    }

    public static void setPrivateDataDirectorySuffix(String suffix) {
        if (!sInitializationStarted.getAndSet(true)) {
            if ($assertionsDisabled || ContextUtils.getApplicationContext() != null) {
                sDataDirectorySuffix = suffix;
                sDirPathFetchTask = new AsyncTask<Void, Void, String[]>() {
                    protected String[] doInBackground(Void... unused) {
                        return PathUtils.setPrivateDataDirectorySuffixInternal();
                    }
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
                return;
            }
            throw new AssertionError();
        }
    }

    private static String getDirectoryPath(int index) {
        return Holder.DIRECTORY_PATHS[index];
    }

    @CalledByNative
    public static String getDataDirectory() {
        if ($assertionsDisabled || sDirPathFetchTask != null) {
            return getDirectoryPath(0);
        }
        throw new AssertionError("setDataDirectorySuffix must be called first.");
    }

    @CalledByNative
    public static String getDatabaseDirectory() {
        if ($assertionsDisabled || sDirPathFetchTask != null) {
            return getDirectoryPath(2);
        }
        throw new AssertionError("setDataDirectorySuffix must be called first.");
    }

    @CalledByNative
    public static String getCacheDirectory() {
        if ($assertionsDisabled || sDirPathFetchTask != null) {
            return getDirectoryPath(3);
        }
        throw new AssertionError("setDataDirectorySuffix must be called first.");
    }

    @CalledByNative
    public static String getThumbnailCacheDirectory() {
        if ($assertionsDisabled || sDirPathFetchTask != null) {
            return getDirectoryPath(1);
        }
        throw new AssertionError("setDataDirectorySuffix must be called first.");
    }

    @CalledByNative
    public static String getDownloadInternalDirectory() {
        if ($assertionsDisabled || sDirPathFetchTask != null) {
            return getDirectoryPath(4);
        }
        throw new AssertionError("setDataDirectorySuffix must be called first.");
    }

    @CalledByNative
    private static String getDownloadsDirectory() {
        Throwable th;
        StrictModeContext unused = StrictModeContext.allowDiskReads();
        Throwable th2 = null;
        try {
            long time = SystemClock.elapsedRealtime();
            String downloadsPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
            RecordHistogram.recordTimesHistogram("Android.StrictMode.DownloadsDir", SystemClock.elapsedRealtime() - time, TimeUnit.MILLISECONDS);
            if (unused != null) {
                $closeResource(null, unused);
            }
            return downloadsPath;
        } catch (Throwable th22) {
            Throwable th3 = th22;
            th22 = th;
            th = th3;
        }
        if (unused != null) {
            $closeResource(th22, unused);
        }
        throw th;
    }

    @CalledByNative
    private static String getNativeLibraryDirectory() {
        ApplicationInfo ai = ContextUtils.getApplicationContext().getApplicationInfo();
        if ((ai.flags & 128) != 0 || (ai.flags & 1) == 0) {
            return ai.nativeLibraryDir;
        }
        return "/system/lib/";
    }

    @CalledByNative
    public static String getExternalStorageDirectory() {
        return Environment.getExternalStorageDirectory().getAbsolutePath();
    }
}
