package org.chromium.base;

import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;
import org.chromium.base.annotations.MainDex;

@MainDex
public abstract class CommandLine {
    static final /* synthetic */ boolean $assertionsDisabled;
    private static final String SWITCH_PREFIX = "--";
    private static final String SWITCH_TERMINATOR = "--";
    private static final String SWITCH_VALUE_SEPARATOR = "=";
    private static final String TAG = "CommandLine";
    private static final AtomicReference<CommandLine> sCommandLine = new AtomicReference();

    private static class JavaCommandLine extends CommandLine {
        static final /* synthetic */ boolean $assertionsDisabled = (!CommandLine.class.desiredAssertionStatus() ? true : CommandLine.$assertionsDisabled);
        private ArrayList<String> mArgs = new ArrayList();
        private int mArgsBegin = 1;
        private HashMap<String, String> mSwitches = new HashMap();

        JavaCommandLine(@Nullable String[] args) {
            super();
            if (args == null || args.length == 0 || args[0] == null) {
                this.mArgs.add("");
            } else {
                this.mArgs.add(args[0]);
                appendSwitchesInternal(args, 1);
            }
            if (!$assertionsDisabled && this.mArgs.size() <= 0) {
                throw new AssertionError();
            }
        }

        protected String[] getCommandLineArguments() {
            return (String[]) this.mArgs.toArray(new String[this.mArgs.size()]);
        }

        public boolean hasSwitch(String switchString) {
            return this.mSwitches.containsKey(switchString);
        }

        public String getSwitchValue(String switchString) {
            String value = (String) this.mSwitches.get(switchString);
            return (value == null || value.isEmpty()) ? null : value;
        }

        public void appendSwitch(String switchString) {
            appendSwitchWithValue(switchString, null);
        }

        public void appendSwitchWithValue(String switchString, String value) {
            Object obj;
            HashMap hashMap = this.mSwitches;
            if (value == null) {
                obj = "";
            } else {
                String str = value;
            }
            hashMap.put(switchString, obj);
            String combinedSwitchString = "--" + switchString;
            if (!(value == null || value.isEmpty())) {
                combinedSwitchString = combinedSwitchString + CommandLine.SWITCH_VALUE_SEPARATOR + value;
            }
            ArrayList arrayList = this.mArgs;
            int i = this.mArgsBegin;
            this.mArgsBegin = i + 1;
            arrayList.add(i, combinedSwitchString);
        }

        public void appendSwitchesAndArguments(String[] array) {
            appendSwitchesInternal(array, 0);
        }

        private void appendSwitchesInternal(String[] array, int skipCount) {
            boolean parseSwitches = true;
            for (String arg : array) {
                if (skipCount > 0) {
                    skipCount--;
                } else {
                    if (arg.equals("--")) {
                        parseSwitches = CommandLine.$assertionsDisabled;
                    }
                    if (parseSwitches && arg.startsWith("--")) {
                        String[] parts = arg.split(CommandLine.SWITCH_VALUE_SEPARATOR, 2);
                        appendSwitchWithValue(parts[0].substring("--".length()), parts.length > 1 ? parts[1] : null);
                    } else {
                        this.mArgs.add(arg);
                    }
                }
            }
        }
    }

    private static class NativeCommandLine extends CommandLine {
        static final /* synthetic */ boolean $assertionsDisabled = (!CommandLine.class.desiredAssertionStatus() ? true : CommandLine.$assertionsDisabled);

        public NativeCommandLine(@Nullable String[] args) {
            super();
            CommandLine.nativeInit(args);
        }

        public boolean hasSwitch(String switchString) {
            return CommandLine.nativeHasSwitch(switchString);
        }

        public String getSwitchValue(String switchString) {
            return CommandLine.nativeGetSwitchValue(switchString);
        }

        public void appendSwitch(String switchString) {
            CommandLine.nativeAppendSwitch(switchString);
        }

        public void appendSwitchWithValue(String switchString, String value) {
            CommandLine.nativeAppendSwitchWithValue(switchString, value);
        }

        public void appendSwitchesAndArguments(String[] array) {
            CommandLine.nativeAppendSwitchesAndArguments(array);
        }

        public boolean isNativeImplementation() {
            return true;
        }

        protected String[] getCommandLineArguments() {
            if ($assertionsDisabled) {
                return null;
            }
            throw new AssertionError();
        }

        protected void destroy() {
            throw new IllegalStateException("Can't destroy native command line after startup");
        }
    }

    private static native void nativeAppendSwitch(String str);

    private static native void nativeAppendSwitchWithValue(String str, String str2);

    private static native void nativeAppendSwitchesAndArguments(String[] strArr);

    private static native String nativeGetSwitchValue(String str);

    private static native boolean nativeHasSwitch(String str);

    private static native void nativeInit(String[] strArr);

    @VisibleForTesting
    public abstract void appendSwitch(String str);

    public abstract void appendSwitchWithValue(String str, String str2);

    public abstract void appendSwitchesAndArguments(String[] strArr);

    protected abstract String[] getCommandLineArguments();

    public abstract String getSwitchValue(String str);

    @VisibleForTesting
    public abstract boolean hasSwitch(String str);

    static {
        boolean z;
        if (CommandLine.class.desiredAssertionStatus()) {
            z = $assertionsDisabled;
        } else {
            z = true;
        }
        $assertionsDisabled = z;
    }

    public String getSwitchValue(String switchString, String defaultValue) {
        String value = getSwitchValue(switchString);
        return TextUtils.isEmpty(value) ? defaultValue : value;
    }

    public boolean isNativeImplementation() {
        return $assertionsDisabled;
    }

    protected void destroy() {
    }

    public static boolean isInitialized() {
        return sCommandLine.get() != null ? true : $assertionsDisabled;
    }

    @VisibleForTesting
    public static CommandLine getInstance() {
        CommandLine commandLine = (CommandLine) sCommandLine.get();
        if ($assertionsDisabled || commandLine != null) {
            return commandLine;
        }
        throw new AssertionError();
    }

    public static void init(@Nullable String[] args) {
        setInstance(new JavaCommandLine(args));
    }

    public static void initFromFile(String file) {
        char[] buffer = readUtf8FileFullyCrashIfTooBig(file, 65536);
        init(buffer == null ? null : tokenizeQuotedArguments(buffer));
    }

    @VisibleForTesting
    public static void reset() {
        setInstance(null);
    }

    @VisibleForTesting
    static String[] tokenizeQuotedArguments(char[] buffer) {
        ArrayList<String> args = new ArrayList();
        StringBuilder arg = null;
        char currentQuote = '\u0000';
        for (char c : buffer) {
            if ((currentQuote == '\u0000' && (c == '\'' || c == '\"')) || c == currentQuote) {
                if (arg == null || arg.length() <= 0 || arg.charAt(arg.length() - 1) != '\\') {
                    currentQuote = currentQuote == '\u0000' ? c : '\u0000';
                } else {
                    arg.setCharAt(arg.length() - 1, c);
                }
            } else if (currentQuote != '\u0000' || !Character.isWhitespace(c)) {
                if (arg == null) {
                    arg = new StringBuilder();
                }
                arg.append(c);
            } else if (arg != null) {
                args.add(arg.toString());
                arg = null;
            }
        }
        if (arg != null) {
            if (currentQuote != '\u0000') {
                Log.w(TAG, "Unterminated quoted string: " + arg);
            }
            args.add(arg.toString());
        }
        return (String[]) args.toArray(new String[args.size()]);
    }

    public static void enableNativeProxy() {
        sCommandLine.set(new NativeCommandLine(getJavaSwitchesOrNull()));
    }

    @Nullable
    public static String[] getJavaSwitchesOrNull() {
        CommandLine commandLine = (CommandLine) sCommandLine.get();
        if (commandLine != null) {
            return commandLine.getCommandLineArguments();
        }
        return null;
    }

    private static void setInstance(CommandLine commandLine) {
        CommandLine oldCommandLine = (CommandLine) sCommandLine.getAndSet(commandLine);
        if (oldCommandLine != null) {
            oldCommandLine.destroy();
        }
    }

    private static char[] readUtf8FileFullyCrashIfTooBig(String fileName, int sizeLimit) {
        Throwable th;
        Reader reader = null;
        File f = new File(fileName);
        long fileLength = f.length();
        if (fileLength == 0) {
            return null;
        }
        if (fileLength > ((long) sizeLimit)) {
            throw new RuntimeException("File " + fileName + " length " + fileLength + " exceeds limit " + sizeLimit);
        }
        try {
            char[] buffer = new char[((int) fileLength)];
            Reader reader2 = new InputStreamReader(new FileInputStream(f), "UTF-8");
            try {
                int charsRead = reader2.read(buffer);
                if ($assertionsDisabled || !reader2.ready()) {
                    if (charsRead < buffer.length) {
                        buffer = Arrays.copyOfRange(buffer, 0, charsRead);
                    }
                    if (reader2 != null) {
                        try {
                            reader2.close();
                        } catch (IOException e) {
                            Log.e(TAG, "Unable to close file reader.", e);
                        }
                    }
                    reader = reader2;
                    return buffer;
                }
                throw new AssertionError();
            } catch (FileNotFoundException e2) {
                reader = reader2;
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e3) {
                        Log.e(TAG, "Unable to close file reader.", e3);
                    }
                }
                return null;
            } catch (IOException e4) {
                reader = reader2;
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e32) {
                        Log.e(TAG, "Unable to close file reader.", e32);
                    }
                }
                return null;
            } catch (Throwable th2) {
                th = th2;
                reader = reader2;
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e322) {
                        Log.e(TAG, "Unable to close file reader.", e322);
                    }
                }
                throw th;
            }
        } catch (FileNotFoundException e5) {
            if (reader != null) {
                reader.close();
            }
            return null;
        } catch (IOException e6) {
            if (reader != null) {
                reader.close();
            }
            return null;
        } catch (Throwable th3) {
            th = th3;
            if (reader != null) {
                reader.close();
            }
            throw th;
        }
    }

    private CommandLine() {
    }
}
