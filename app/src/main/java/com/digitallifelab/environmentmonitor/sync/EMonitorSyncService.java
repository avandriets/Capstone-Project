package com.digitallifelab.environmentmonitor.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class EMonitorSyncService extends Service {
    private static final Object sSyncAdapterLock = new Object();
    private static EMonitorSyncAdapter sEMonitorSyncAdapter = null;

    @Override
    public void onCreate() {
        Log.d("EMonitorSyncService", "onCreate - EMonitorSyncService");
        synchronized (sSyncAdapterLock) {
            if (sEMonitorSyncAdapter == null) {
                sEMonitorSyncAdapter = new EMonitorSyncAdapter(getApplicationContext(), true);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return sEMonitorSyncAdapter.getSyncAdapterBinder();
    }
}