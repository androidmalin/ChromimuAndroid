package org.chromium.net;

import java.util.concurrent.Executor;

public abstract class NetworkQualityRttListener {
    private final Executor mExecutor;

    public abstract void onRttObservation(int i, long j, int i2);

    public NetworkQualityRttListener(Executor executor) {
        if (executor == null) {
            throw new IllegalStateException("Executor must not be null");
        }
        this.mExecutor = executor;
    }

    public Executor getExecutor() {
        return this.mExecutor;
    }
}
