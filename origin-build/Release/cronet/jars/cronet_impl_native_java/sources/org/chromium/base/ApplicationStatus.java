package org.chromium.base;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.app.Application.ActivityLifecycleCallbacks;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Window.Callback;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.chromium.base.annotations.CalledByNative;
import org.chromium.base.annotations.JNINamespace;
import org.chromium.base.annotations.MainDex;

@MainDex
@JNINamespace("base::android")
public class ApplicationStatus {
    static final /* synthetic */ boolean $assertionsDisabled;
    private static final String TOOLBAR_CALLBACK_INTERNAL_WRAPPER_CLASS = "android.support.v7.internal.app.ToolbarActionBar$ToolbarCallbackWrapper";
    private static final String TOOLBAR_CALLBACK_WRAPPER_CLASS = "android.support.v7.app.ToolbarActionBar$ToolbarCallbackWrapper";
    private static final String WINDOW_PROFILER_CALLBACK = "com.android.tools.profiler.support.event.WindowProfilerCallback";
    @SuppressLint({"StaticFieldLeak"})
    private static Activity sActivity;
    private static final Map<Activity, ActivityInfo> sActivityInfo = new ConcurrentHashMap();
    private static final ObserverList<ApplicationStateListener> sApplicationStateListeners = new ObserverList();
    @SuppressLint({"SupportAnnotationUsage"})
    private static Integer sCachedApplicationState;
    private static final Object sCachedApplicationStateLock = new Object();
    private static final ObserverList<ActivityStateListener> sGeneralActivityStateListeners = new ObserverList();
    private static boolean sIsInitialized;
    private static ApplicationStateListener sNativeApplicationStateListener;
    private static final ObserverList<WindowFocusChangedListener> sWindowFocusListeners = new ObserverList();

    public interface WindowFocusChangedListener {
        void onWindowFocusChanged(Activity activity, boolean z);
    }

    public interface ApplicationStateListener {
        void onApplicationStateChange(int i);
    }

    private static class ActivityInfo {
        private ObserverList<ActivityStateListener> mListeners;
        private int mStatus;

        private ActivityInfo() {
            this.mStatus = 6;
            this.mListeners = new ObserverList();
        }

        public int getStatus() {
            return this.mStatus;
        }

        public void setStatus(int status) {
            this.mStatus = status;
        }

        public ObserverList<ActivityStateListener> getListeners() {
            return this.mListeners;
        }
    }

    public interface ActivityStateListener {
        void onActivityStateChange(Activity activity, int i);
    }

    private static class WindowCallbackProxy implements InvocationHandler {
        private final Activity mActivity;
        private final Callback mCallback;

        public WindowCallbackProxy(Activity activity, Callback callback) {
            this.mCallback = callback;
            this.mActivity = activity;
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getName().equals("onWindowFocusChanged") && args.length == 1 && (args[0] instanceof Boolean)) {
                onWindowFocusChanged(((Boolean) args[0]).booleanValue());
                return null;
            }
            try {
                return method.invoke(this.mCallback, args);
            } catch (InvocationTargetException e) {
                if (e.getCause() instanceof AbstractMethodError) {
                    throw e.getCause();
                }
                throw e;
            }
        }

        public void onWindowFocusChanged(boolean hasFocus) {
            this.mCallback.onWindowFocusChanged(hasFocus);
            Iterator it = ApplicationStatus.sWindowFocusListeners.iterator();
            while (it.hasNext()) {
                ((WindowFocusChangedListener) it.next()).onWindowFocusChanged(this.mActivity, hasFocus);
            }
        }
    }

    private static native void nativeOnApplicationStateChange(int i);

    static {
        boolean z;
        if (ApplicationStatus.class.desiredAssertionStatus()) {
            z = $assertionsDisabled;
        } else {
            z = true;
        }
        $assertionsDisabled = z;
    }

    private ApplicationStatus() {
    }

    public static void registerWindowFocusChangedListener(WindowFocusChangedListener listener) {
        sWindowFocusListeners.addObserver(listener);
    }

    public static void unregisterWindowFocusChangedListener(WindowFocusChangedListener listener) {
        sWindowFocusListeners.removeObserver(listener);
    }

    public static void initialize(Application application) {
        if (!sIsInitialized) {
            sIsInitialized = true;
            registerWindowFocusChangedListener(new WindowFocusChangedListener() {
                public void onWindowFocusChanged(Activity activity, boolean hasFocus) {
                    if (hasFocus && activity != ApplicationStatus.sActivity) {
                        int state = ApplicationStatus.getStateForActivity(activity);
                        if (state != 6 && state != 5) {
                            ApplicationStatus.sActivity = activity;
                        }
                    }
                }
            });
            application.registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
                public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                    ApplicationStatus.onStateChange(activity, 1);
                    Callback callback = activity.getWindow().getCallback();
                    activity.getWindow().setCallback((Callback) Proxy.newProxyInstance(Callback.class.getClassLoader(), new Class[]{Callback.class}, new WindowCallbackProxy(activity, callback)));
                }

                public void onActivityDestroyed(Activity activity) {
                    ApplicationStatus.onStateChange(activity, 6);
                    checkCallback(activity);
                }

                public void onActivityPaused(Activity activity) {
                    ApplicationStatus.onStateChange(activity, 4);
                    checkCallback(activity);
                }

                public void onActivityResumed(Activity activity) {
                    ApplicationStatus.onStateChange(activity, 3);
                    checkCallback(activity);
                }

                public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
                    checkCallback(activity);
                }

                public void onActivityStarted(Activity activity) {
                    ApplicationStatus.onStateChange(activity, 2);
                    checkCallback(activity);
                }

                public void onActivityStopped(Activity activity) {
                    ApplicationStatus.onStateChange(activity, 5);
                    checkCallback(activity);
                }

                private void checkCallback(Activity activity) {
                }
            });
        }
    }

    private static void assertInitialized() {
        if (!$assertionsDisabled && !sIsInitialized) {
            throw new AssertionError();
        }
    }

    private static void onStateChange(Activity activity, int newState) {
        if (activity == null) {
            throw new IllegalArgumentException("null activity is not supported");
        }
        if (sActivity == null || newState == 1 || newState == 3 || newState == 2) {
            sActivity = activity;
        }
        int oldApplicationState = getStateForApplication();
        if (newState == 1) {
            if (VERSION.SDK_INT >= 26 || $assertionsDisabled || !sActivityInfo.containsKey(activity)) {
                sActivityInfo.put(activity, new ActivityInfo());
            } else {
                throw new AssertionError();
            }
        }
        synchronized (sCachedApplicationStateLock) {
            sCachedApplicationState = null;
        }
        ActivityInfo info = (ActivityInfo) sActivityInfo.get(activity);
        info.setStatus(newState);
        if (newState == 6) {
            sActivityInfo.remove(activity);
            if (activity == sActivity) {
                sActivity = null;
            }
        }
        Iterator it = info.getListeners().iterator();
        while (it.hasNext()) {
            ((ActivityStateListener) it.next()).onActivityStateChange(activity, newState);
        }
        it = sGeneralActivityStateListeners.iterator();
        while (it.hasNext()) {
            ((ActivityStateListener) it.next()).onActivityStateChange(activity, newState);
        }
        int applicationState = getStateForApplication();
        if (applicationState != oldApplicationState) {
            it = sApplicationStateListeners.iterator();
            while (it.hasNext()) {
                ((ApplicationStateListener) it.next()).onApplicationStateChange(applicationState);
            }
        }
    }

    @VisibleForTesting
    public static void onStateChangeForTesting(Activity activity, int newState) {
        onStateChange(activity, newState);
    }

    public static Activity getLastTrackedFocusedActivity() {
        return sActivity;
    }

    public static List<WeakReference<Activity>> getRunningActivities() {
        assertInitialized();
        List<WeakReference<Activity>> activities = new ArrayList();
        for (Activity activity : sActivityInfo.keySet()) {
            activities.add(new WeakReference(activity));
        }
        return activities;
    }

    public static int getStateForActivity(@Nullable Activity activity) {
        assertInitialized();
        if (activity == null) {
            return 6;
        }
        ActivityInfo info = (ActivityInfo) sActivityInfo.get(activity);
        if (info != null) {
            return info.getStatus();
        }
        return 6;
    }

    @CalledByNative
    public static int getStateForApplication() {
        int intValue;
        synchronized (sCachedApplicationStateLock) {
            if (sCachedApplicationState == null) {
                sCachedApplicationState = Integer.valueOf(determineApplicationState());
            }
            intValue = sCachedApplicationState.intValue();
        }
        return intValue;
    }

    public static boolean hasVisibleActivities() {
        int state = getStateForApplication();
        if (state == 1 || state == 2) {
            return true;
        }
        return $assertionsDisabled;
    }

    public static boolean isEveryActivityDestroyed() {
        return sActivityInfo.isEmpty();
    }

    public static void registerStateListenerForAllActivities(ActivityStateListener listener) {
        sGeneralActivityStateListeners.addObserver(listener);
    }

    @SuppressLint({"NewApi"})
    public static void registerStateListenerForActivity(ActivityStateListener listener, Activity activity) {
        if ($assertionsDisabled || activity != null) {
            assertInitialized();
            ActivityInfo info = (ActivityInfo) sActivityInfo.get(activity);
            if (VERSION.SDK_INT >= 26 && info == null && !activity.isDestroyed()) {
                info = new ActivityInfo();
                sActivityInfo.put(activity, info);
            }
            if ($assertionsDisabled || !(info == null || info.getStatus() == 6)) {
                info.getListeners().addObserver(listener);
                return;
            }
            throw new AssertionError();
        }
        throw new AssertionError();
    }

    public static void unregisterActivityStateListener(ActivityStateListener listener) {
        sGeneralActivityStateListeners.removeObserver(listener);
        for (ActivityInfo info : sActivityInfo.values()) {
            info.getListeners().removeObserver(listener);
        }
    }

    public static void registerApplicationStateListener(ApplicationStateListener listener) {
        sApplicationStateListeners.addObserver(listener);
    }

    public static void unregisterApplicationStateListener(ApplicationStateListener listener) {
        sApplicationStateListeners.removeObserver(listener);
    }

    public static void destroyForJUnitTests() {
        sApplicationStateListeners.clear();
        sGeneralActivityStateListeners.clear();
        sActivityInfo.clear();
        sWindowFocusListeners.clear();
        sIsInitialized = $assertionsDisabled;
        synchronized (sCachedApplicationStateLock) {
            sCachedApplicationState = null;
        }
        sActivity = null;
        sNativeApplicationStateListener = null;
    }

    @CalledByNative
    private static void registerThreadSafeNativeApplicationStateListener() {
        ThreadUtils.runOnUiThread(new Runnable() {
            public void run() {
                if (ApplicationStatus.sNativeApplicationStateListener == null) {
                    ApplicationStatus.sNativeApplicationStateListener = new ApplicationStateListener() {
                        public void onApplicationStateChange(int newState) {
                            ApplicationStatus.nativeOnApplicationStateChange(newState);
                        }
                    };
                    ApplicationStatus.registerApplicationStateListener(ApplicationStatus.sNativeApplicationStateListener);
                }
            }
        });
    }

    private static int determineApplicationState() {
        boolean hasPausedActivity = $assertionsDisabled;
        boolean hasStoppedActivity = $assertionsDisabled;
        for (ActivityInfo info : sActivityInfo.values()) {
            int state = info.getStatus();
            if (state != 4 && state != 5 && state != 6) {
                return 1;
            }
            if (state == 4) {
                hasPausedActivity = true;
            } else if (state == 5) {
                hasStoppedActivity = true;
            }
        }
        if (hasPausedActivity) {
            return 2;
        }
        if (hasStoppedActivity) {
            return 3;
        }
        return 4;
    }
}
