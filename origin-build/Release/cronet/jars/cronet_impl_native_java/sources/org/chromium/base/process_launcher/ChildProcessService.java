package org.chromium.base.process_launcher;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public abstract class ChildProcessService extends Service {
    private final ChildProcessServiceImpl mChildProcessServiceImpl;

    protected ChildProcessService(ChildProcessServiceDelegate delegate) {
        this.mChildProcessServiceImpl = new ChildProcessServiceImpl(delegate);
    }

    public void onCreate() {
        super.onCreate();
        this.mChildProcessServiceImpl.create(getApplicationContext(), getApplicationContext());
    }

    public void onDestroy() {
        super.onDestroy();
        this.mChildProcessServiceImpl.destroy();
    }

    public IBinder onBind(Intent intent) {
        stopSelf();
        return this.mChildProcessServiceImpl.bind(intent, -1);
    }
}
