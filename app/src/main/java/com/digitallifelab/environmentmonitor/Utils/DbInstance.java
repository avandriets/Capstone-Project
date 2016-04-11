package com.digitallifelab.environmentmonitor.Utils;

import android.content.Context;
import com.digitallifelab.environmentmonitor.Data.DatabaseHelper;
import com.j256.ormlite.android.apptools.OpenHelperManager;

public class DbInstance {

    private static DatabaseHelper databaseHelper = null;

    public DbInstance() {
    }

    public void SetDBHelper(Context context)
    {
        if (databaseHelper == null) {
            databaseHelper = OpenHelperManager.getHelper(context, DatabaseHelper.class);
        }
    }

    public DatabaseHelper getDatabaseHelper(){
        return databaseHelper;
    }

    public void releaseHelper() {
        databaseHelper = null;
    }
}
