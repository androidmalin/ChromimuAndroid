package org.chromium.net;

import android.content.Context;
import android.util.Log;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import org.chromium.net.CronetEngine.Builder;

public abstract class CronetProvider {
    private static final String JAVA_CRONET_PROVIDER_CLASS = "org.chromium.net.impl.JavaCronetProvider";
    private static final String NATIVE_CRONET_PROVIDER_CLASS = "org.chromium.net.impl.NativeCronetProvider";
    public static final String PROVIDER_NAME_APP_PACKAGED = "App-Packaged-Cronet-Provider";
    public static final String PROVIDER_NAME_FALLBACK = "Fallback-Cronet-Provider";
    private static final String RES_KEY_CRONET_IMPL_CLASS = "CronetProviderClassName";
    private static final String TAG = CronetProvider.class.getSimpleName();
    protected final Context mContext;

    public abstract Builder createBuilder();

    public abstract String getName();

    public abstract String getVersion();

    public abstract boolean isEnabled();

    protected CronetProvider(Context context) {
        this.mContext = context;
    }

    public String toString() {
        return "[class=" + getClass().getName() + ", name=" + getName() + ", version=" + getVersion() + ", enabled=" + isEnabled() + "]";
    }

    public static List<CronetProvider> getAllProviders(Context context) {
        List<CronetProvider> providers = new ArrayList();
        addCronetProviderFromResourceFile(context, providers);
        addCronetProviderImplByClassName(context, NATIVE_CRONET_PROVIDER_CLASS, providers, false);
        addCronetProviderImplByClassName(context, JAVA_CRONET_PROVIDER_CLASS, providers, false);
        return providers;
    }

    private static boolean addCronetProviderImplByClassName(Context context, String className, List<CronetProvider> providers, boolean logError) {
        try {
            providers.add((CronetProvider) context.getClassLoader().loadClass(className).asSubclass(CronetProvider.class).getConstructor(new Class[]{Context.class}).newInstance(new Object[]{context}));
            return true;
        } catch (InstantiationException e) {
            logReflectiveOperationException(className, logError, e);
            return false;
        } catch (InvocationTargetException e2) {
            logReflectiveOperationException(className, logError, e2);
            return false;
        } catch (NoSuchMethodException e3) {
            logReflectiveOperationException(className, logError, e3);
            return false;
        } catch (IllegalAccessException e4) {
            logReflectiveOperationException(className, logError, e4);
            return false;
        } catch (ClassNotFoundException e5) {
            logReflectiveOperationException(className, logError, e5);
            return false;
        }
    }

    private static void logReflectiveOperationException(String className, boolean logError, Exception e) {
        if (logError) {
            Log.e(TAG, "Unable to load provider class: " + className, e);
        } else {
            Log.d(TAG, "Tried to load " + className + " provider class but it wasn't included in the app classpath");
        }
    }

    private static boolean addCronetProviderFromResourceFile(Context context, List<CronetProvider> providers) {
        int resId = context.getResources().getIdentifier(RES_KEY_CRONET_IMPL_CLASS, "string", context.getPackageName());
        if (resId == 0) {
            return false;
        }
        String className = context.getResources().getString(resId);
        if (addCronetProviderImplByClassName(context, className, providers, true)) {
            return true;
        }
        throw new RuntimeException("Unable to instantiate Cronet implementation class " + className + " that is listed as in the app string resource file under " + RES_KEY_CRONET_IMPL_CLASS + " key");
    }
}
