package com.digitallifelab.environmentmonitor.gcm;


import android.util.Log;

import com.digitallifelab.environmentmonitor.Data.AccountsStore;
import com.digitallifelab.environmentmonitor.Data.EnvironmentService;
import com.digitallifelab.environmentmonitor.Utils.Utility;

import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MyDevice {

    private static final String LOG_TAG = MyDevice.class.getSimpleName();
    static public String GCM_SENDER = "<your_project_number>";

    public static void register(String deviceID, String registrationId) {

        Retrofit retrofit;
        EnvironmentService service;

        retrofit = new Retrofit.Builder()
                .baseUrl(Utility.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        service = retrofit.create(EnvironmentService.class);

        AccountsStore acc = AccountsStore.getActiveUser();

        if(acc == null)
            return;

        HashMap<String, String> jsonParameters = new HashMap<String, String>();
        jsonParameters.put("dev_id", deviceID);
        jsonParameters.put("reg_id", registrationId);

        Call<ResponseBody> retPoint = service.registerDevice(jsonParameters, "Bearer " + acc.getMy_server_access_token());
        //Call<ResponseBody> retPoint = service.registerDevice(jsonParameters);

        try {
            Response<ResponseBody> response = retPoint.execute();

            if(response.isSuccessful()){
                JSONObject json_obj = Utility.ReadRetrofitResponseToJsonObj(response);
                Log.d(LOG_TAG, json_obj.toString()+ " " + response.message());
            }else{
                JSONObject error_obj = Utility.ReadRetrofitResponseToJsonObj(response);
                Log.e(LOG_TAG, error_obj.toString() + " " + response.message());
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void unregister(String deviceID, String token) {

        Retrofit retrofit;
        EnvironmentService service;

        retrofit = new Retrofit.Builder()
                .baseUrl(Utility.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        service = retrofit.create(EnvironmentService.class);

        HashMap<String, String> jsonParameters = new HashMap<String, String>();
        jsonParameters.put("dev_id", deviceID);

        Call<ResponseBody> retPoint = service.unregisterDevice(jsonParameters, "Bearer " + token);
        //Call<ResponseBody> retPoint = service.unregisterDevice(jsonParameters);

        try {
            Response<ResponseBody> response = retPoint.execute();

            if(response.isSuccessful()){
                JSONObject json_obj = Utility.ReadRetrofitResponseToJsonObj(response);
                Log.d(LOG_TAG, json_obj.toString()+ " " + response.message());
            }else{
                JSONObject error_obj = Utility.ReadRetrofitResponseToJsonObj(response);
                Log.e(LOG_TAG, error_obj.toString() + " " + response.message());
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}