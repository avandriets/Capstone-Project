package com.digitallifelab.environmentmonitor.gcm;


import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;
import com.digitallifelab.environmentmonitor.Data.AccountsStore;
import com.digitallifelab.environmentmonitor.Data.EnvironmentMonitorContract;
import com.digitallifelab.environmentmonitor.Data.EnvironmentService;
import com.digitallifelab.environmentmonitor.Data.MessagesStore;
import com.digitallifelab.environmentmonitor.Data.PicturesStore;
import com.digitallifelab.environmentmonitor.Data.PointsStore;
import com.digitallifelab.environmentmonitor.Data.ifSendNotification;
import com.digitallifelab.environmentmonitor.DetailPointActivity;
import com.digitallifelab.environmentmonitor.MainActivity;
import com.digitallifelab.environmentmonitor.R;
import com.digitallifelab.environmentmonitor.Utils.DbInstance;
import com.digitallifelab.environmentmonitor.Utils.ManageAccountsToken;
import com.digitallifelab.environmentmonitor.Utils.NetworkServiceUtility;
import com.digitallifelab.environmentmonitor.Utils.Utility;
import com.google.android.gms.gcm.GcmListenerService;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.util.List;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MyGcmListenerService extends GcmListenerService {

    private static final String TAG = "MyGcmListenerService";

    public static final int NOTIFICATION_ID = 1;
    private static final java.lang.String EXTRA_PICTURE_PK  = "DeletePicture";
    private static final java.lang.String EXTRA_POINT_PK    = "DeletePoint";
    private static final java.lang.String EXTRA_MESSAGE     = "NewMessage";
    private static final java.lang.String EXTRA_POINT     = "NewPoint";

    /**
     * Called when message is received.
     *
     * @param from SenderID of the sender.
     * @param data Data bundle containing message data as key/value pairs.
     *             For Set of keys use data.keySet().
     */
    @Override
    public void onMessageReceived(String from, Bundle data) {

        Log.d(TAG, "onMessageReceived");

        String picturePk;
        String pointPk;
        boolean destroyHelper = false;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String displayNotificationsKey = this.getString(R.string.pref_enable_notifications_key);
        boolean displayNotifications = prefs.getBoolean(displayNotificationsKey, Boolean.parseBoolean(this.getString(R.string.pref_enable_notifications_default)));

        if (!data.isEmpty()) {

            String senderId = getString(R.string.gcm_defaultSenderId); //getString(R.string.gcm_defaultSenderId);
            if (senderId.length() == 0) {
                Toast.makeText(this, "SenderID string needs to be set", Toast.LENGTH_LONG).show();
            }

            if ((senderId).equals(from)) {

                DbInstance dbInstance = new DbInstance();

                if(dbInstance.getDatabaseHelper() == null){
                    dbInstance.SetDBHelper(this);
                    destroyHelper = true;
                }

                AccountsStore acc = AccountsStore.getActiveUser();

                if(!ManageAccountsToken.TokenWasGet(acc, this)){
                    Log.e(TAG, "Could not get token.");
                    return;
                }

                if(data.containsKey(EXTRA_PICTURE_PK)) {

                    picturePk = data.getString(EXTRA_PICTURE_PK);

                    RuntimeExceptionDao<PicturesStore, Long> daoPicture = dbInstance.getDatabaseHelper().getPicturesDataDao();

                    List<PicturesStore> picList = daoPicture.queryForEq(PicturesStore.SERVER_ID, picturePk);

                    if(picList.size() > 0){
                        daoPicture.delete(picList.get(0));
                    }

                    getContentResolver().insert(EnvironmentMonitorContract.POINTS_CONTENT_URI, null);

                }else if(data.containsKey(EXTRA_POINT_PK)){

                    pointPk = data.getString(EXTRA_POINT_PK);

                    RuntimeExceptionDao<PointsStore, Long> daoPoint = dbInstance.getDatabaseHelper().getPointsDataDao();
                    RuntimeExceptionDao<PicturesStore, Long> daoPicture = dbInstance.getDatabaseHelper().getPicturesDataDao();
                    RuntimeExceptionDao<MessagesStore, Long> daoMessage = dbInstance.getDatabaseHelper().getMessagesDataDao();

                    List<PointsStore> pointsList = daoPoint.queryForEq(EnvironmentMonitorContract.Points.SERVER_ID, pointPk);

                    if(pointsList.size() > 0){

                        //Delete pictures
                        List<PicturesStore> picList = daoPicture.queryForEq(PicturesStore.POINT_ID, pointsList.get(0).getLocal_id());
                        for (PicturesStore picS : picList) {
                            daoPicture.delete(picS);
                        }

                        //Delete messages
                        List<MessagesStore> messagesList = daoMessage.queryForEq(MessagesStore.POINT_ID, pointsList.get(0).getLocal_id());
                        for (MessagesStore messS : messagesList) {
                            daoMessage.delete(messS);
                        }

                        daoPoint.delete(pointsList.get(0));
                    }

                    getContentResolver().insert(EnvironmentMonitorContract.POINTS_CONTENT_URI, null);
                }else if(data.containsKey(EXTRA_MESSAGE)){

                    String pointData = data.getString(EXTRA_MESSAGE);
                    Log.i(TAG, "ARRAY: " + pointData);
                    boolean SendNotifyIfPkDoesntExist = false;

                    List<MessagesStore> listOfObjects = dbInstance.getDatabaseHelper().getMessagesDataDao().queryForEq(MessagesStore.SERVER_ID, pointData);

                    if(listOfObjects == null || listOfObjects.size() == 0){
                        SendNotifyIfPkDoesntExist = true;
                    }

                    JSONArray pointsArray = getPointFromServer(pointData);

                    if(pointsArray != null) {

                        List<PointsStore> objects_List = NetworkServiceUtility.ParsePointsJsonArray(pointsArray, dbInstance, this, new ifSendNotification());


                        getContentResolver().insert(EnvironmentMonitorContract.POINTS_CONTENT_URI, null);

                        boolean sendNotify = false;
                        String my_Email = AccountsStore.getActiveUser().getEmail();
                        long pointId = -1;

                        for (PointsStore po :objects_List) {
                            if(po.getUser_email().equals(my_Email)){
                                sendNotify = true;
                                pointId = po.getLocal_id();
                            }
                        }

                        if(sendNotify && displayNotifications && SendNotifyIfPkDoesntExist)
                            sendNotification(getString(R.string.new_message_notyfication), pointId);
                    }
                }else if(data.containsKey(EXTRA_POINT)){

                    String pointData = data.getString(EXTRA_POINT);
                    Log.i(TAG, "ARRAY: " + pointData);
                    JSONArray pointsArray = getPointFromServer(pointData);

                    if(pointsArray != null)
                        NetworkServiceUtility.ParsePointsJsonArray(pointsArray, dbInstance, this, new ifSendNotification());

                    getContentResolver().insert(EnvironmentMonitorContract.POINTS_CONTENT_URI, null);
                }

                NetworkServiceUtility.updateWidgets(this);

                if(destroyHelper){
                    OpenHelperManager.releaseHelper();
                    dbInstance.releaseHelper();
                }
            }
            Log.i(TAG, "Received: " + data.toString());
        }
    }

    private JSONArray getPointFromServer(String pointPk){

        AccountsStore acc = AccountsStore.getActiveUser();

        if(acc != null && ManageAccountsToken.TokenWasGet(acc, this)) {

            String authString = "Bearer " + acc.getMy_server_access_token();
            //GET DATA from server
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(Utility.BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            EnvironmentService service = retrofit.create(EnvironmentService.class);

            Call<ResponseBody> retGetPoints = service.getPoint( Long.decode(pointPk), authString);

            try {
                Response<ResponseBody> response = retGetPoints.execute();

                if (response.isSuccessful()) {

                    Log.d(TAG, "CallBack response is success " + response);

                    String jsonString = Utility.ReadRetrofitResponseToString(response);

                    JSONArray pointsArray = new JSONArray();
                    pointsArray.put(new JSONObject(jsonString));

                    return pointsArray;

                } else {
                    Log.e(TAG, "Cannot get point from server");
                }
            } catch (IOException | JSONException e) {
                e.printStackTrace();
                Log.e(TAG, e.getMessage());
            }
        }

        return null;
    }

    /**
     *  Put the message into a notification and post it.
     *  This is just one simple example of what you might choose to do with a GCM message.
     *
     * @param message The alert message to be posted.
     */
    private void sendNotification(String message, long pId) {

        Log.d(TAG, "sendNotification");

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        Intent intent = new Intent(this, DetailPointActivity.class);
        intent.putExtra(MainActivity.KEY_POINT_ID,pId);

        //PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);
        PendingIntent contentIntent = PendingIntent.getActivity(this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        // Notifications using both a large and a small icon (which yours should!) need the large
        // icon as a bitmap. So we need to create that here from the resource ID, and pass the
        // object along in our notification builder. Generally, you want to use the app icon as the
        // small icon, so that users understand what app is triggering this notification.
        Bitmap largeIcon = BitmapFactory.decodeResource(this.getResources(), R.mipmap.ic_launcher);
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setLargeIcon(largeIcon)
                        .setContentTitle(this.getString(R.string.app_name))
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                        .setContentText(message)
                        .setPriority(NotificationCompat.PRIORITY_HIGH);
        mBuilder.setContentIntent(contentIntent);

        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }
}
