package org.chromium.net;

import org.chromium.net.NetworkChangeNotifierAutoDetect.RegistrationPolicy;

public class RegistrationPolicyAlwaysRegister extends RegistrationPolicy {
    protected void init(NetworkChangeNotifierAutoDetect notifier) {
        super.init(notifier);
        register();
    }

    protected void destroy() {
    }
}
