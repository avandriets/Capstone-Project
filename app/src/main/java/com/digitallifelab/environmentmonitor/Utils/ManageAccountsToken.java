package com.digitallifelab.environmentmonitor.Utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.digitallifelab.environmentmonitor.Data.AccountsStore;
import com.digitallifelab.environmentmonitor.MainActivity;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.j256.ormlite.dao.RuntimeExceptionDao;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class ManageAccountsToken {

    private static final String LOG_TAG = ManageAccountsToken.class.getSimpleName();

    public static boolean GetTokens(Context context, String email) throws JSONException, IOException, UserRecoverableAuthException, GoogleAuthException {

        DbInstance dbInstance = new DbInstance();
        RuntimeExceptionDao<AccountsStore, String> dao = dbInstance.getDatabaseHelper().getAccountsDataDao();

        String token;

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        AccountsStore acc = AccountsStore.getActiveUser();

        //Get google token
        boolean obtainToken = sharedPreferences.getBoolean(Utility.GOOGLE_TOKEN_WAS_GOT, false);

        sharedPreferences.edit().putBoolean(Utility.EXCHANGE_TOKEN_WAS_GOT, false).apply();

        if (!obtainToken) {
            token = GoogleAuthUtil.getToken(context, email, Utility.GOOGLE_SCOPE, new Bundle());

            acc.setToken(token);
            dao.update(acc);

            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            sharedPreferences.edit().putBoolean(Utility.GOOGLE_TOKEN_WAS_GOT, true).apply();
        } else {
            token = acc.getToken();
        }

        if(token == null){
            sharedPreferences.edit().putBoolean(Utility.GOOGLE_TOKEN_WAS_GOT, false).apply();
            return false;
        }

        boolean exTokenWasGet;

        try {
            exTokenWasGet = ManageAccountsToken.getExchangeToken(token, context);

            if(!exTokenWasGet){
                sharedPreferences.edit().putBoolean(Utility.GOOGLE_TOKEN_WAS_GOT, false).apply();
                return false;
            }else{
                sharedPreferences.edit().putBoolean(Utility.EXCHANGE_TOKEN_WAS_GOT, true).apply();
            }

        } catch(JSONException|IOException ex){
            return false;
        }

        return true;
    }

    public static boolean getExchangeToken(String googleToken, Context context) throws JSONException, IOException {

        String bodyStringRequest = "grant_type=convert_token" + "&" +
                "client_id=" + Utility.Get_My_ServerClientID(context) + "&" +
                "client_secret=" + Utility.Get_My_Server_Secret(context) + "&" +
                "backend=" + "google-oauth2" + "&" +
                "token=" + googleToken;

        Uri convertToken = Uri.parse(Utility.BASE_URL + Utility.CONVERT_TOKEN_URL).buildUpon().build();

        RequestBody body = RequestBody.create(MediaType.parse("content-type; application/x-www-form-urlencoded"), bodyStringRequest);
        Request request = new Request.Builder()
                .url(convertToken.toString())
                .header("content-type", "application/x-www-form-urlencoded")
                .header("Accept", "*/*")
                .post(body)
                .build();

        //TODO check it
        //Response response = Utility.mClientOkHttp.newCall(request).execute();
        Response response = (new OkHttpClient()).newCall(request).execute();// Utility.mClientOkHttp.newCall(request).execute();

        if (!response.isSuccessful()){

            JSONObject json_obj = Utility.ReadHTTPOkResponse(response);
            String error = json_obj.getString("error_description");

            if(error.contains("Invalid Credentials"))
                return false;
            else
                throw new IOException("Cannot access to server.");
        }

        JSONObject json_obj = Utility.ReadHTTPOkResponse(response);
        String access_token;

        if (json_obj != null) {
            access_token = json_obj.get("access_token").toString();

            RuntimeExceptionDao<AccountsStore, String> dao = (new DbInstance()).getDatabaseHelper().getAccountsDataDao();
            AccountsStore acc = AccountsStore.getActiveUser();
            if (acc != null) {
                acc.setMy_server_access_token(access_token);
                dao.update(acc);
            }
        } else {
            return false;
        }

        return true;
    }

    public static boolean TokenWasGet(AccountsStore acc, Context context) {

        boolean result = false;

        //Ask about token
        try {
            result = ManageAccountsToken.GetTokens(context, acc.getEmail());
            // Try again
            if(!result){
                result = ManageAccountsToken.GetTokens(context, acc.getEmail());
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, "GoogleAuthUtil.getToken IOException " + e.getMessage());
        } catch (UserRecoverableAuthException e) {
            Log.e(LOG_TAG, "UserRecoverableAuthException: " + e.getMessage());
        } catch (GoogleAuthException e) {
            Log.e(LOG_TAG, "GoogleAuthException: " + e.getMessage());
        } catch (JSONException e) {
            Log.e(LOG_TAG, "GoogleAuthException: " + e.getMessage());
        }

        return result;
    }
}
