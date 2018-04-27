package org.chromium.net;

import org.chromium.base.ApplicationStatus;
import org.chromium.base.ApplicationStatus.ApplicationStateListener;
import org.chromium.base.VisibleForTesting;
import org.chromium.net.NetworkChangeNotifierAutoDetect.RegistrationPolicy;

public class RegistrationPolicyApplicationStatus extends RegistrationPolicy implements ApplicationStateListener {
    private boolean mDestroyed;

    protected void init(NetworkChangeNotifierAutoDetect notifier) {
        super.init(notifier);
        ApplicationStatus.registerApplicationStateListener(this);
        onApplicationStateChange(getApplicationState());
    }

    protected void destroy() {
        if (!this.mDestroyed) {
            ApplicationStatus.unregisterApplicationStateListener(this);
            this.mDestroyed = true;
        }
    }

    public void onApplicationStateChange(int newState) {
        if (newState == 1) {
            register();
        } else if (newState == 2) {
            unregister();
        }
    }

    @VisibleForTesting
    int getApplicationState() {
        return ApplicationStatus.getStateForApplication();
    }
}
