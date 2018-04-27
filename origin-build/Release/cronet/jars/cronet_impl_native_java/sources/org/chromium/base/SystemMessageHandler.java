package org.chromium.base;

import android.annotation.SuppressLint;
import android.os.Build.VERSION;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.MessageQueue.IdleHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.chromium.base.annotations.CalledByNative;
import org.chromium.base.annotations.JNINamespace;
import org.chromium.base.annotations.MainDex;

@MainDex
@JNINamespace("base")
class SystemMessageHandler extends Handler {
    private static final int DELAYED_SCHEDULED_WORK = 2;
    private static final int SCHEDULED_WORK = 1;
    private static final String TAG = "cr.SysMessageHandler";
    private final IdleHandler mIdleHandler = new IdleHandler() {
        public boolean queueIdle() {
            if (SystemMessageHandler.this.mNativeMessagePumpForUI == 0) {
                return false;
            }
            SystemMessageHandler.this.nativeDoIdleWork(SystemMessageHandler.this.mNativeMessagePumpForUI);
            return true;
        }
    };
    private long mNativeMessagePumpForUI;
    private boolean mScheduledDelayedWork;

    private static class MessageCompat {
        static final MessageWrapperImpl IMPL;

        interface MessageWrapperImpl {
            void setAsynchronous(Message message, boolean z);
        }

        static class LegacyMessageWrapperImpl implements MessageWrapperImpl {
            private Method mMessageMethodSetAsynchronous;

            LegacyMessageWrapperImpl() {
                try {
                    this.mMessageMethodSetAsynchronous = Message.class.getMethod("setAsynchronous", new Class[]{Boolean.TYPE});
                } catch (NoSuchMethodException e) {
                    Log.e(SystemMessageHandler.TAG, "Failed to load Message.setAsynchronous method", e);
                } catch (RuntimeException e2) {
                    Log.e(SystemMessageHandler.TAG, "Exception while loading Message.setAsynchronous method", e2);
                }
            }

            public void setAsynchronous(Message msg, boolean async) {
                if (this.mMessageMethodSetAsynchronous != null) {
                    try {
                        this.mMessageMethodSetAsynchronous.invoke(msg, new Object[]{Boolean.valueOf(async)});
                    } catch (IllegalAccessException e) {
                        Log.e(SystemMessageHandler.TAG, "Illegal access to async message creation, disabling.", new Object[0]);
                        this.mMessageMethodSetAsynchronous = null;
                    } catch (IllegalArgumentException e2) {
                        Log.e(SystemMessageHandler.TAG, "Illegal argument for async message creation, disabling.", new Object[0]);
                        this.mMessageMethodSetAsynchronous = null;
                    } catch (InvocationTargetException e3) {
                        Log.e(SystemMessageHandler.TAG, "Invocation exception during async message creation, disabling.", new Object[0]);
                        this.mMessageMethodSetAsynchronous = null;
                    } catch (RuntimeException e4) {
                        Log.e(SystemMessageHandler.TAG, "Runtime exception during async message creation, disabling.", new Object[0]);
                        this.mMessageMethodSetAsynchronous = null;
                    }
                }
            }
        }

        static class LollipopMr1MessageWrapperImpl implements MessageWrapperImpl {
            LollipopMr1MessageWrapperImpl() {
            }

            @SuppressLint({"NewApi"})
            public void setAsynchronous(Message msg, boolean async) {
                msg.setAsynchronous(async);
            }
        }

        private MessageCompat() {
        }

        public static void setAsynchronous(Message message, boolean async) {
            IMPL.setAsynchronous(message, async);
        }

        static {
            if (VERSION.SDK_INT >= 22) {
                IMPL = new LollipopMr1MessageWrapperImpl();
            } else {
                IMPL = new LegacyMessageWrapperImpl();
            }
        }
    }

    private native void nativeDoIdleWork(long j);

    private native void nativeDoRunLoopOnce(long j, boolean z);

    protected SystemMessageHandler(long nativeMessagePumpForUI) {
        this.mNativeMessagePumpForUI = nativeMessagePumpForUI;
        Looper.myQueue().addIdleHandler(this.mIdleHandler);
    }

    public void handleMessage(Message msg) {
        if (this.mNativeMessagePumpForUI != 0) {
            boolean delayed;
            if (msg.what == 2) {
                delayed = true;
            } else {
                delayed = false;
            }
            if (delayed) {
                this.mScheduledDelayedWork = false;
            }
            nativeDoRunLoopOnce(this.mNativeMessagePumpForUI, delayed);
        }
    }

    @CalledByNative
    private void scheduleWork() {
        sendMessage(obtainAsyncMessage(1));
    }

    @CalledByNative
    private void scheduleDelayedWork(long millis) {
        if (this.mScheduledDelayedWork) {
            removeMessages(2);
        }
        this.mScheduledDelayedWork = true;
        sendMessageDelayed(obtainAsyncMessage(2), millis);
    }

    @CalledByNative
    private void shutdown() {
        this.mNativeMessagePumpForUI = 0;
    }

    private Message obtainAsyncMessage(int what) {
        Message msg = Message.obtain();
        msg.what = what;
        MessageCompat.setAsynchronous(msg, true);
        return msg;
    }

    @CalledByNative
    private static SystemMessageHandler create(long nativeMessagePumpForUI) {
        return new SystemMessageHandler(nativeMessagePumpForUI);
    }
}
