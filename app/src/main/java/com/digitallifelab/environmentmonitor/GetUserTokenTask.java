package com.digitallifelab.environmentmonitor;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.digitallifelab.environmentmonitor.Data.AccountsStore;
import com.digitallifelab.environmentmonitor.Utils.ManageAccountsToken;
import com.digitallifelab.environmentmonitor.Utils.OnGetGoogleTokenTaskCompleted;
import com.digitallifelab.environmentmonitor.Utils.Utility;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.UserRecoverableAuthException;

import org.json.JSONException;

import java.io.IOException;


public class GetUserTokenTask extends AsyncTask<Void, Void, Intent> {

    private static final String LOG_TAG = GetUserTokenTask.class.getSimpleName();
    public static final String KEY_ERROR_MESSAGE_GET_TOKEN      = "Key_error_get_google_token";

    MainActivity mainActivity;
    String mScope;
    String mEmail;
    private OnGetGoogleTokenTaskCompleted listener;

    GetUserTokenTask(MainActivity context, String name, String scope, OnGetGoogleTokenTaskCompleted listener) {
        this.mainActivity   = context;
        this.mScope         = scope;
        this.mEmail         = name;
        this.listener       = listener;
    }

    /**
     * Executes the asynchronous job. This runs when you call execute()
     * on the AsyncTask instance.
     */
    @Override
    protected Intent doInBackground(Void... params) {

        Bundle data = new Bundle();

        try {
            boolean result = ManageAccountsToken.GetTokens(mainActivity, mEmail);

            // Try again
            if(!result){
                result = ManageAccountsToken.GetTokens(mainActivity, mEmail);
            }

            if(result){
                data.putString(Utility.KEY_ACCESS_TOKEN, "was get");
            }

        } catch (IOException e) {
            Log.e(LOG_TAG, "GoogleAuthUtil.getToken IOException " + e.getMessage());
            data.putString(KEY_ERROR_MESSAGE_GET_TOKEN, e.getMessage());
        } catch (UserRecoverableAuthException e) {
            Log.e(LOG_TAG, "UserRecoverableAuthException: " + e.getMessage());
            mainActivity.handleException(e);
            data.putString(KEY_ERROR_MESSAGE_GET_TOKEN, e.getMessage());
        } catch (GoogleAuthException e) {
            Log.e(LOG_TAG, "GoogleAuthException: " + e.getMessage());
            data.putString(KEY_ERROR_MESSAGE_GET_TOKEN, e.getMessage());
        } catch (JSONException e) {
            Log.e(LOG_TAG, "GoogleAuthException: " + e.getMessage());
            data.putString(KEY_ERROR_MESSAGE_GET_TOKEN, e.getMessage());
        }

        final Intent res = new Intent();
        res.putExtras(data);

        return res;
    }

    @Override
    protected void onPostExecute(Intent intent) {
        super.onPostExecute(intent);
        listener.onGetGoogleTokenTaskCompleted(intent);
    }

}
