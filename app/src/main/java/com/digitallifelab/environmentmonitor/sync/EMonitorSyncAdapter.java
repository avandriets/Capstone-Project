package com.digitallifelab.environmentmonitor.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncRequest;
import android.content.SyncResult;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import com.digitallifelab.environmentmonitor.Data.AccountsStore;
import com.digitallifelab.environmentmonitor.Data.EnvironmentMonitorContract;
import com.digitallifelab.environmentmonitor.Data.EnvironmentService;
import com.digitallifelab.environmentmonitor.Data.MessagesStore;
import com.digitallifelab.environmentmonitor.Data.PicturesStore;
import com.digitallifelab.environmentmonitor.Data.PointsStore;
import com.digitallifelab.environmentmonitor.R;
import com.digitallifelab.environmentmonitor.Utils.DbInstance;
import com.digitallifelab.environmentmonitor.Utils.ManageAccountsToken;
import com.digitallifelab.environmentmonitor.Utils.NetworkServiceUtility;
import com.digitallifelab.environmentmonitor.Utils.Utility;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.stmt.QueryBuilder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class EMonitorSyncAdapter extends AbstractThreadedSyncAdapter {

    public final String LOG_TAG = EMonitorSyncAdapter.class.getSimpleName();
    public static final String ACTION_DATA_UPDATED = "com.digitallifelab.environmentmonitor.ACTION_DATA_UPDATED";
    // Interval at which to sync, in seconds.
    // 60 seconds (1 minute) * 180 = 3 hours
    public static final int SYNC_INTERVAL = 60 * 180;
    public static final int SYNC_FLEXTIME = SYNC_INTERVAL/3;

    public EMonitorSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);

    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {

        Log.d(LOG_TAG, "Starting sync");

        if(!Utility.isNetworkAvailable(getContext())){
            Log.e(LOG_TAG, "No internet connection.");
            return;
        }

        boolean destroyHelper = false;

        //Synchronize data with server
        DbInstance dbInstance = new DbInstance();

        AccountsStore acc = AccountsStore.getActiveUser();

        if(acc != null && ManageAccountsToken.TokenWasGet(acc, getContext())) {

            if(dbInstance.getDatabaseHelper() == null){
                dbInstance.SetDBHelper(getContext());
            }

            String authString = "Bearer " + acc.getMy_server_access_token();
            //GET DATA from server
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(Utility.BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            EnvironmentService service = retrofit.create(EnvironmentService.class);

            GetDataFromServer(service, authString, dbInstance);

            SendDataToServer(service, authString, dbInstance);

            getContext().getContentResolver().insert(EnvironmentMonitorContract.POINTS_CONTENT_URI, null);

            if(destroyHelper){
                OpenHelperManager.releaseHelper();
                dbInstance.releaseHelper();
            }
        }

        return;
    }

    private void SendDataToServer(EnvironmentService service, String authString, DbInstance dbInstance) {

        if(dbInstance.getDatabaseHelper() == null)
            return;

        RuntimeExceptionDao<PointsStore, Long>      daoPoint   = dbInstance.getDatabaseHelper().getPointsDataDao();
        RuntimeExceptionDao<MessagesStore, Long>    daoMessage = dbInstance.getDatabaseHelper().getMessagesDataDao();
        RuntimeExceptionDao<PicturesStore, Long>    daoPicture = dbInstance.getDatabaseHelper().getPicturesDataDao();

        List<PointsStore> PointsList;
        List<PicturesStore> PictureList;
        List<MessagesStore> MessageList;

        //Get point for upload
        QueryBuilder<PointsStore, Long> qb = daoPoint.queryBuilder();
        QueryBuilder<PicturesStore, Long> qbPictures = daoPicture.queryBuilder();
        QueryBuilder<MessagesStore, Long> qbMessages = daoMessage.queryBuilder();

        try {

            //Send points
            qb.where().eq(EnvironmentMonitorContract.Points.POINT_WAS_UPLOADED, 0).or().eq(EnvironmentMonitorContract.Points.IS_CHANGED, 1).or().eq(EnvironmentMonitorContract.Points.IS_NEW, 1);
            PointsList = daoPoint.query(qb.prepare());
            SendPoints(PointsList,service,authString,dbInstance);

            //Send pictures
            qbPictures.where().eq(PicturesStore.PHOTO_WAS_UPLOADED, 0).or().eq(PicturesStore.IS_NEW, 1);
            PictureList = daoPicture.query(qbPictures.prepare());
            SendPictures(PictureList, service, authString, dbInstance);

            qbMessages.where().eq(MessagesStore.WAS_SENT, 0).or().eq(MessagesStore.IS_NEW, 1);
            MessageList = daoMessage.query(qbMessages.prepare());
            SendMessages(MessageList, service, authString, dbInstance);

        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    private void SendMessages(List<MessagesStore> messageList, EnvironmentService service, String authString, DbInstance dbInstance) {

        for (MessagesStore onePoint:messageList) {
            NetworkServiceUtility.SendMessageToServer(onePoint, onePoint.getPoint(), service, authString, dbInstance);
        }

    }

    private void SendPictures(List<PicturesStore> pictureList, EnvironmentService service, String authString, DbInstance dbInstance) {

        for (PicturesStore onePoint:pictureList) {
            if(onePoint.getIs_deleted() == 1){
                NetworkServiceUtility.SendDeletePictureToServer(onePoint, service, authString, dbInstance);
            }else{
                NetworkServiceUtility.SendPictureToServer(onePoint, onePoint.getPoint(), service, authString, dbInstance);
            }
        }
    }

    private void SendPoints(List<PointsStore> PointsList, EnvironmentService service, String authString, DbInstance dbInstance ){

        for (PointsStore onePoint:PointsList) {
            if(onePoint.getIs_deleted() == 1){
                NetworkServiceUtility.SendDeletePointToServer(onePoint, service, authString, dbInstance);
            }else{
                NetworkServiceUtility.SendNewPointToServer(onePoint, service, authString, dbInstance);
            }
        }
    }

    private void GetDataFromServer(EnvironmentService service, String authString, DbInstance dbInstance){

        Bundle data = new Bundle();

        Call<ResponseBody> retGetPoints = service.getPoints(authString);

        try {
            Response<ResponseBody> response = retGetPoints.execute();

            if (response.isSuccessful()) {

                Log.d(LOG_TAG, "CallBack response is success " + response);

                String jsonString = Utility.ReadRetrofitResponseToString(response);

                JSONArray pointsArray = new JSONArray(jsonString);
                if (pointsArray != null) {
                    NetworkServiceUtility.ParsePointsJsonArray(pointsArray, dbInstance, getContext());
                }

            } else {
                JSONObject error_obj = Utility.ReadRetrofitResponseToJsonObj(response);
                data.putString(Utility.KEY_ERROR_MESSAGE_ELEMENTS, response.message() + " " + error_obj.toString());
            }

        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Helper method to schedule the sync adapter periodic execution
     */
    public static void configurePeriodicSync(Context context, int syncInterval, int flexTime) {
        Account account = getSyncAccount(context);
        String authority = context.getString(R.string.content_authority);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // we can enable inexact timers in our periodic sync
            SyncRequest request = new SyncRequest.Builder().
                    syncPeriodic(syncInterval, flexTime).
                    setSyncAdapter(account, authority).
                    setExtras(new Bundle()).build();
            ContentResolver.requestSync(request);
        } else {
            ContentResolver.addPeriodicSync(account, authority, new Bundle(), syncInterval);
        }
    }

    /**
     * Helper method to have the sync adapter sync immediately
     * @param context The context used to access the account service
     */
    public static void syncImmediately(Context context) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSync(getSyncAccount(context),
                context.getString(R.string.content_authority), bundle);
    }

    /**
     * Helper method to get the fake account to be used with SyncAdapter, or make a new one
     * if the fake account doesn't exist yet.  If we make a new account, we call the
     * onAccountCreated method so we can initialize things.
     *
     * @param context The context used to access the account service
     * @return a fake account.
     */
    public static Account getSyncAccount(Context context) {
        // Get an instance of the Android account manager
        AccountManager accountManager =
                (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);

        // Create the account type and default account
        Account newAccount = new Account(
                context.getString(R.string.app_name), context.getString(R.string.sync_account_type));

        // If the password doesn't exist, the account doesn't exist
        if ( null == accountManager.getPassword(newAccount) ) {

        /*
         * Add the account and account type, no password or user data
         * If successful, return the Account object, otherwise report an error.
         */
            if (!accountManager.addAccountExplicitly(newAccount, "", null)) {
                return null;
            }
            /*
             * If you don't set android:syncable="true" in
             * in your <provider> element in the manifest,
             * then call ContentResolver.setIsSyncable(account, AUTHORITY, 1)
             * here.
             */

            onAccountCreated(newAccount, context);
        }
        return newAccount;
    }

    private static void onAccountCreated(Account newAccount, Context context) {
        /*
         * Since we've created an account
         */
        EMonitorSyncAdapter.configurePeriodicSync(context, SYNC_INTERVAL, SYNC_FLEXTIME);

        /*
         * Without calling setSyncAutomatically, our periodic sync will not be enabled.
         */
        ContentResolver.setSyncAutomatically(newAccount, context.getString(R.string.content_authority), true);

        /*
         * Finally, let's do a sync to get things started
         */
        syncImmediately(context);
    }

    public static void initializeSyncAdapter(Context context) {
        getSyncAccount(context);
    }
}