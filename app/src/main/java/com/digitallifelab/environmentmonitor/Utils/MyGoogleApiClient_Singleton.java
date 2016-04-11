package com.digitallifelab.environmentmonitor.Utils;
import android.location.Location;

import com.google.android.gms.common.api.GoogleApiClient;


public class MyGoogleApiClient_Singleton {

    private static final String TAG = MyGoogleApiClient_Singleton.class.getSimpleName();

    private static MyGoogleApiClient_Singleton instance = null;

    private static GoogleApiClient mGoogleApiClient = null;

    private static Location mLastLocation = null;

    public MyGoogleApiClient_Singleton() {

    }

    public static MyGoogleApiClient_Singleton getInstance(GoogleApiClient aGoogleApiClient) {
        if(instance == null) {
            instance = new MyGoogleApiClient_Singleton();
            if (mGoogleApiClient == null)
                mGoogleApiClient = aGoogleApiClient;
        }
        return instance;
    }

    public static Location getmLastLocation() {
        return mLastLocation;
    }

    public static void setmLastLocation(Location mLastLocation) {
        MyGoogleApiClient_Singleton.mLastLocation = mLastLocation;
    }

    public GoogleApiClient get_GoogleApiClient(){
        return mGoogleApiClient;
    }


}