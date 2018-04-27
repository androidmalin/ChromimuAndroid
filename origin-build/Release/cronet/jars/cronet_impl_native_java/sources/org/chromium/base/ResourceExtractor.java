package org.chromium.base;

import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Handler;
import android.os.Looper;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class ResourceExtractor {
    static final /* synthetic */ boolean $assertionsDisabled = (!ResourceExtractor.class.desiredAssertionStatus() ? true : $assertionsDisabled);
    private static final String FALLBACK_LOCALE = "en-US";
    private static final String ICU_DATA_FILENAME = "icudtl.dat";
    private static final String TAG = "base";
    private static final String V8_NATIVES_DATA_FILENAME = "natives_blob.bin";
    private static final String V8_SNAPSHOT_DATA_FILENAME = "snapshot_blob.bin";
    private static ResourceExtractor sInstance;
    private final String[] mAssetsToExtract = detectFilesToExtract();
    private ExtractTask mExtractTask;

    private class ExtractTask extends AsyncTask<Void, Void, Void> {
        private static final int BUFFER_SIZE = 16384;
        private final List<Runnable> mCompletionCallbacks;

        private ExtractTask() {
            this.mCompletionCallbacks = new ArrayList();
        }

        private void extractResourceHelper(InputStream is, File outFile, byte[] buffer) throws IOException {
            Throwable th;
            OutputStream os = null;
            File tmpOutputFile = new File(outFile.getPath() + ".tmp");
            try {
                OutputStream os2 = new FileOutputStream(tmpOutputFile);
                try {
                    Log.i(ResourceExtractor.TAG, "Extracting resource %s", outFile);
                    while (true) {
                        int count = is.read(buffer, 0, BUFFER_SIZE);
                        if (count == -1) {
                            break;
                        }
                        os2.write(buffer, 0, count);
                    }
                    StreamUtil.closeQuietly(os2);
                    StreamUtil.closeQuietly(is);
                    if (!tmpOutputFile.renameTo(outFile)) {
                        throw new IOException();
                    }
                } catch (Throwable th2) {
                    th = th2;
                    os = os2;
                    StreamUtil.closeQuietly(os);
                    StreamUtil.closeQuietly(is);
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                StreamUtil.closeQuietly(os);
                StreamUtil.closeQuietly(is);
                throw th;
            }
        }

        private void doInBackgroundImpl() {
            File outputDir = ResourceExtractor.this.getOutputDir();
            if (outputDir.exists() || outputDir.mkdirs()) {
                int i;
                String assetName;
                String extractSuffix = BuildInfo.getExtractedFileSuffix();
                String[] existingFileNames = outputDir.list();
                boolean allFilesExist = existingFileNames != null ? true : ResourceExtractor.$assertionsDisabled;
                if (allFilesExist) {
                    List<String> existingFiles = Arrays.asList(existingFileNames);
                    for (String assetName2 : ResourceExtractor.this.mAssetsToExtract) {
                        allFilesExist &= existingFiles.contains(assetName2 + extractSuffix);
                    }
                }
                if (!allFilesExist) {
                    ResourceExtractor.this.deleteFiles(existingFileNames);
                    AssetManager assetManager = ContextUtils.getApplicationAssets();
                    byte[] buffer = new byte[BUFFER_SIZE];
                    String[] access$100 = ResourceExtractor.this.mAssetsToExtract;
                    int length = access$100.length;
                    i = 0;
                    while (i < length) {
                        assetName2 = access$100[i];
                        File output = new File(outputDir, assetName2 + extractSuffix);
                        TraceEvent.begin("ExtractResource");
                        try {
                            extractResourceHelper(assetManager.open(assetName2), output, buffer);
                            TraceEvent.end("ExtractResource");
                            i++;
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        } catch (Throwable th) {
                            TraceEvent.end("ExtractResource");
                        }
                    }
                    return;
                }
                return;
            }
            throw new RuntimeException();
        }

        protected Void doInBackground(Void... unused) {
            TraceEvent.begin("ResourceExtractor.ExtractTask.doInBackground");
            try {
                doInBackgroundImpl();
                return null;
            } finally {
                TraceEvent.end("ResourceExtractor.ExtractTask.doInBackground");
            }
        }

        private void onPostExecuteImpl() {
            for (int i = 0; i < this.mCompletionCallbacks.size(); i++) {
                ((Runnable) this.mCompletionCallbacks.get(i)).run();
            }
            this.mCompletionCallbacks.clear();
        }

        protected void onPostExecute(Void result) {
            TraceEvent.begin("ResourceExtractor.ExtractTask.onPostExecute");
            try {
                onPostExecuteImpl();
            } finally {
                TraceEvent.end("ResourceExtractor.ExtractTask.onPostExecute");
            }
        }
    }

    public static ResourceExtractor get() {
        if (sInstance == null) {
            sInstance = new ResourceExtractor();
        }
        return sInstance;
    }

    private static String[] detectFilesToExtract() {
        String language = LocaleUtils.getUpdatedLanguageForChromium(Locale.getDefault().getLanguage());
        ArrayList<String> activeLocalePakFiles = new ArrayList(6);
        for (String locale : BuildConfig.COMPRESSED_LOCALES) {
            if (locale.startsWith(language)) {
                activeLocalePakFiles.add(locale + ".pak");
            }
        }
        if (activeLocalePakFiles.isEmpty() && BuildConfig.COMPRESSED_LOCALES.length > 0) {
            if ($assertionsDisabled || Arrays.asList(BuildConfig.COMPRESSED_LOCALES).contains(FALLBACK_LOCALE)) {
                activeLocalePakFiles.add("en-US.pak");
            } else {
                throw new AssertionError();
            }
        }
        Log.i(TAG, "Android Locale: %s requires .pak files: %s", defaultLocale, activeLocalePakFiles);
        return (String[]) activeLocalePakFiles.toArray(new String[activeLocalePakFiles.size()]);
    }

    public void waitForCompletion() {
        if (this.mExtractTask != null && !shouldSkipPakExtraction()) {
            try {
                this.mExtractTask.get();
            } catch (Exception e) {
                if (!$assertionsDisabled) {
                    throw new AssertionError();
                }
            }
        }
    }

    public void addCompletionCallback(Runnable callback) {
        ThreadUtils.assertOnUiThread();
        Handler handler = new Handler(Looper.getMainLooper());
        if (shouldSkipPakExtraction()) {
            handler.post(callback);
        } else if (!$assertionsDisabled && this.mExtractTask == null) {
            throw new AssertionError();
        } else if (!$assertionsDisabled && this.mExtractTask.isCancelled()) {
            throw new AssertionError();
        } else if (this.mExtractTask.getStatus() == Status.FINISHED) {
            handler.post(callback);
        } else {
            this.mExtractTask.mCompletionCallbacks.add(callback);
        }
    }

    public void startExtractingResources() {
        if (this.mExtractTask == null && !shouldSkipPakExtraction()) {
            this.mExtractTask = new ExtractTask();
            this.mExtractTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
        }
    }

    private File getAppDataDir() {
        return new File(PathUtils.getDataDirectory());
    }

    private File getOutputDir() {
        return new File(getAppDataDir(), "paks");
    }

    private static void deleteFile(File file) {
        if (file.exists() && !file.delete()) {
            Log.w(TAG, "Unable to remove %s", file.getName());
        }
    }

    private void deleteFiles(String[] existingFileNames) {
        deleteFile(new File(getAppDataDir(), ICU_DATA_FILENAME));
        deleteFile(new File(getAppDataDir(), V8_NATIVES_DATA_FILENAME));
        deleteFile(new File(getAppDataDir(), V8_SNAPSHOT_DATA_FILENAME));
        if (existingFileNames != null) {
            for (String fileName : existingFileNames) {
                deleteFile(new File(getOutputDir(), fileName));
            }
        }
    }

    private static boolean shouldSkipPakExtraction() {
        return get().mAssetsToExtract.length == 0 ? true : $assertionsDisabled;
    }
}
