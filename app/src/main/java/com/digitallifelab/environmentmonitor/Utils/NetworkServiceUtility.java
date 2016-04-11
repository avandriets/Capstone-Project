package com.digitallifelab.environmentmonitor.Utils;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.digitallifelab.environmentmonitor.Data.EnvironmentMonitorContract;
import com.digitallifelab.environmentmonitor.Data.EnvironmentService;
import com.digitallifelab.environmentmonitor.Data.MessagesStore;
import com.digitallifelab.environmentmonitor.Data.PicturesStore;
import com.digitallifelab.environmentmonitor.Data.PointsStore;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.stmt.QueryBuilder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

public class NetworkServiceUtility {

    public static Intent SendDeletePictureToServer(PicturesStore deletingPicture, EnvironmentService service, String authString, DbInstance dbInstance) {

        RuntimeExceptionDao<PicturesStore, Long> daoPicture    = dbInstance.getDatabaseHelper().getPicturesDataDao();

        Bundle data = new Bundle();

        try {

            if (deletingPicture.getIs_new() == 1) {
                daoPicture.delete(deletingPicture);
            } else {

                Call<ResponseBody> retPostPicture = service.deletePicture(deletingPicture.getServer_id(), authString);

                Response<ResponseBody> respond = retPostPicture.execute();

                if (respond.isSuccessful()) {
                    daoPicture.delete(deletingPicture);
                } else {
                    data.putString(Utility.KEY_ERROR_MESSAGE_ELEMENTS, respond.message());
                    deletingPicture.setIs_deleted(1);
                    daoPicture.update(deletingPicture);
                }
            }

        } catch (IOException e) {
            data.putString(Utility.KEY_ERROR_MESSAGE_ELEMENTS, e.getMessage());
        }

        final Intent res = new Intent();
        res.putExtras(data);
        return res;

    }

    public static Intent SendDeletePointToServer(PointsStore deletingPoint, EnvironmentService service, String authString, DbInstance dbInstance) {

        RuntimeExceptionDao<PointsStore, Long>      daoPoint    = dbInstance.getDatabaseHelper().getPointsDataDao();

        Bundle data = new Bundle();

        try {

            if (deletingPoint.getIs_new() == 1) {
                daoPoint.delete(deletingPoint);
            } else {

                Call<ResponseBody> retPostPicture = service.deletePoint(deletingPoint.getServer_id(), authString);

                Response<ResponseBody> respond = retPostPicture.execute();

                if (respond.isSuccessful()) {
                    daoPoint.delete(deletingPoint);
                } else {
                    data.putString(Utility.KEY_ERROR_MESSAGE_ELEMENTS, respond.message());
                    deletingPoint.setIs_deleted(1);
                    daoPoint.update(deletingPoint);
                }
            }

        } catch (IOException e) {
            data.putString(Utility.KEY_ERROR_MESSAGE_ELEMENTS, e.getMessage());
        }

        final Intent res = new Intent();
        res.putExtras(data);
        return res;
    }

    public static PicturesStore SendPictureToServer(PicturesStore pict, PointsStore owner, EnvironmentService service, String authString, DbInstance dbInstance) {

        Bundle data = new Bundle();
        Bundle dataPicturesUpload = new Bundle();


        long newId = owner.getServer_id();

        RequestBody idRequestBody = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(newId));

        File file = new File(pict.getLocal_photo_path());
        RequestBody fileRequestBody = RequestBody.create(MediaType.parse("image/*"), file);

        Call<ResponseBody> retPoint = service.postPointsPictures(idRequestBody, fileRequestBody, authString);

        RuntimeExceptionDao<PicturesStore, Long>    daoPicture = dbInstance.getDatabaseHelper().getPicturesDataDao();

        try {

            Response<ResponseBody> response = retPoint.execute();

            if (response.isSuccessful()) {

                Date gmtTime;
                Date fromGmt;

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");//new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", new Locale("ru","RU"));
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

                JSONObject pict_obj = Utility.ReadRetrofitResponseToJsonObj(response);

                if (pict_obj != null) {

                    pict.setPoint(owner);
                    pict.setPhoto_was_uploaded(1);
                    pict.setIs_new(0);
                    pict.setServer_id(pict_obj.getLong("id"));
                    pict.setFull_photo_url(pict_obj.getString("full_photoURL"));

                    gmtTime = sdf.parse(pict_obj.getString("created_at"));
                    fromGmt = new Date(gmtTime.getTime() + TimeZone.getDefault().getOffset(gmtTime.getTime()));
                    pict.setCreated_at(fromGmt.getTime());

                    gmtTime = sdf.parse(pict_obj.getString("updated_at"));
                    fromGmt = new Date(gmtTime.getTime() + TimeZone.getDefault().getOffset(gmtTime.getTime()));
                    pict.setUpdated_at(fromGmt.getTime());
                    pict.setPhoto_was_uploaded(1);

                    daoPicture.update(pict);

                    return pict;
                }
            } else {
                JSONObject error_obj = Utility.ReadRetrofitResponseToJsonObj(response);
                data.putString(Utility.KEY_ERROR_MESSAGE_ELEMENTS, response.message() + " " + error_obj.toString());
            }

        } catch (ParseException | IOException | JSONException e) {
            data.putString(Utility.KEY_ERROR_MESSAGE_ELEMENTS, e.getMessage());
        }

        final Intent res = new Intent();
        res.putExtras(data);
        res.putExtras(dataPicturesUpload);

        return null;
    }

    public static Intent SendMessageToServer(MessagesStore message, PointsStore owner, EnvironmentService service, String authString, DbInstance dbInstance) {

        Bundle data = new Bundle();
        Bundle dataPicturesUpload = new Bundle();


        message.pollution_mark = owner.getServer_id();
        Call<ResponseBody> retPoint = service.postMessage( message, authString);

        RuntimeExceptionDao<MessagesStore, Long>    daoMessage = dbInstance.getDatabaseHelper().getMessagesDataDao();

        try {

            Response<ResponseBody> response = retPoint.execute();

            if (response.isSuccessful()) {

                Date gmtTime;
                Date fromGmt;

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");//new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", new Locale("ru","RU"));
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

                JSONObject json_obj = Utility.ReadRetrofitResponseToJsonObj(response);

                if (json_obj != null) {

                    message.setFirst_name(json_obj.getString("first_name"));
                    message.setLast_name(json_obj.getString("last_name"));
                    message.setUser_name(json_obj.getString("username"));
                    message.setUser_id(json_obj.getLong("user_id"));

                    gmtTime = sdf.parse(json_obj.getString("created_at"));
                    fromGmt = new Date(gmtTime.getTime() + TimeZone.getDefault().getOffset(gmtTime.getTime()));
                    message.setCreated_at(fromGmt.getTime());

                    gmtTime = sdf.parse(json_obj.getString("updated_at"));
                    fromGmt = new Date(gmtTime.getTime() + TimeZone.getDefault().getOffset(gmtTime.getTime()));
                    message.setUpdated_at(fromGmt.getTime());

                    message.setServer_id(json_obj.getLong("id"));
                    message.setIs_new(0);
                    message.setWas_sent(1);

                    daoMessage.update(message);

                }
            } else {
                JSONObject error_obj = Utility.ReadRetrofitResponseToJsonObj(response);
                data.putString(Utility.KEY_ERROR_MESSAGE_ELEMENTS, response.message() + " " + error_obj.toString());
            }

        } catch (ParseException | IOException | JSONException e) {
            data.putString(Utility.KEY_ERROR_MESSAGE_ELEMENTS, e.getMessage());
        }

        final Intent res = new Intent();
        res.putExtras(data);
        res.putExtras(dataPicturesUpload);

        return res;
    }

    public static MessagesStore GetMessagesWithServerIDExists(long _id, RuntimeExceptionDao<MessagesStore, Long> dao) {

        List<MessagesStore> listOfObjects = dao.queryForEq(MessagesStore.SERVER_ID, _id);

        if(listOfObjects != null && listOfObjects.size() > 0){
            return listOfObjects.get(0);
        }

        return null;
    }

    public static PicturesStore GetPictureWithServerIDExists(long _id, RuntimeExceptionDao<PicturesStore, Long> dao) {

        List<PicturesStore> listOfObjects = dao.queryForEq(PicturesStore.SERVER_ID, _id);

        if(listOfObjects != null && listOfObjects.size() > 0){
            return listOfObjects.get(0);
        }

        return null;
    }

    public static PointsStore GetPointWithServerIDExists(long point_id, RuntimeExceptionDao<PointsStore, Long> daoPoint) {

        List<PointsStore> listOfPoints = daoPoint.queryForEq(EnvironmentMonitorContract.Points.SERVER_ID, point_id);

        if(listOfPoints != null && listOfPoints.size() > 0){
            return listOfPoints.get(0);
        }

        return null;
    }

    public static Intent SendNewPointToServer(PointsStore onePoint, EnvironmentService service, String authString, DbInstance dbInstance) {

        Bundle data = new Bundle();
        Bundle dataPicturesUpload = new Bundle();

        Call<ResponseBody> retPoint;

        if(onePoint.getLocal_id() != 0 && onePoint.getIs_changed() == 1) {
            retPoint = service.editPoint(onePoint.getServer_id(), onePoint, authString);
        }else {
            retPoint = service.postPoint(onePoint, authString);
        }

        RuntimeExceptionDao<PointsStore, Long>      daoPoint    = dbInstance.getDatabaseHelper().getPointsDataDao();
        RuntimeExceptionDao<PicturesStore, Long>    daoPicture  = dbInstance.getDatabaseHelper().getPicturesDataDao();
        RuntimeExceptionDao<MessagesStore, Long>    daoMessages = dbInstance.getDatabaseHelper().getMessagesDataDao();

        List<PicturesStore> listOfPictures;
        List<MessagesStore> listOfMessages;

        try {

            //Get pictures
            QueryBuilder<PicturesStore, Long> qb = daoPicture.queryBuilder();

            qb.where().eq(PicturesStore.POINT_ID, onePoint.getLocal_id())
                    .and()
                    .eq(PicturesStore.PHOTO_WAS_UPLOADED, 0);

            listOfPictures = daoPicture.query(qb.prepare());

            //Get messages
            QueryBuilder<MessagesStore, Long> qbMessages = daoMessages.queryBuilder();

            qbMessages.where().eq(MessagesStore.POINT_ID, onePoint.getServer_id())
                    .and()
                    .eq(MessagesStore.WAS_SENT, 0);

            listOfMessages = daoMessages.query(qbMessages.prepare());

            //Send request
            Response<ResponseBody> response = retPoint.execute();

            if (response.isSuccessful()) {

                Date gmtTime;
                Date fromGmt;

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");//new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", new Locale("ru","RU"));
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

                JSONObject json_obj = Utility.ReadRetrofitResponseToJsonObj(response);

                if (json_obj != null) {

                    if(onePoint.getIs_new() == 1){

                        onePoint.setFirst_name(json_obj.getString("first_name"));
                        onePoint.setLast_name(json_obj.getString("last_name"));
                        onePoint.setUser_name(json_obj.getString("username"));
                        onePoint.setUser_id(json_obj.getLong("user_id"));

                        gmtTime = sdf.parse(json_obj.getString("created_at"));
                        fromGmt = new Date(gmtTime.getTime() + TimeZone.getDefault().getOffset(gmtTime.getTime()));
                        onePoint.setCreated_at(fromGmt.getTime());

                        gmtTime = sdf.parse(json_obj.getString("updated_at"));
                        fromGmt = new Date(gmtTime.getTime() + TimeZone.getDefault().getOffset(gmtTime.getTime()));
                        onePoint.setUpdated_at(fromGmt.getTime());

                        onePoint.setServer_id(json_obj.getLong("id"));
                        onePoint.setIs_new(0);
                        onePoint.setIs_changed(0);
                        onePoint.setIs_deleted(0);
                        onePoint.setPhoto_was_uploaded(1);

                        daoPoint.update(onePoint);
                    }else{

                        gmtTime = sdf.parse(json_obj.getString("updated_at"));
                        fromGmt = new Date(gmtTime.getTime() + TimeZone.getDefault().getOffset(gmtTime.getTime()));
                        onePoint.setUpdated_at(fromGmt.getTime());
                        onePoint.setPhoto_was_uploaded(1);
                        onePoint.setIs_changed(0);

                        daoPoint.update(onePoint);
                    }

                    //Update pictures
                    for (PicturesStore pict : listOfPictures) {
                        if(pict.getIs_deleted() == 0) {
                            SendPictureToServer(pict, onePoint, service, authString, dbInstance);
                        }else if(pict.getIs_deleted() == 1){
                            SendDeletePictureToServer(pict,service,authString,dbInstance);
                        }
                    }

                    for (MessagesStore message : listOfMessages) {

                        message.setPoint(onePoint);
                        daoMessages.update(message);

                        SendMessageToServer(message, onePoint, service, authString, dbInstance);
                    }

                }

            } else {
                JSONObject error_obj = Utility.ReadRetrofitResponseToJsonObj(response);
                data.putString(Utility.KEY_ERROR_MESSAGE_ELEMENTS, response.message() + " " + error_obj.toString());
            }

        } catch (ParseException | IOException | JSONException | SQLException e) {
            data.putString(Utility.KEY_ERROR_MESSAGE_ELEMENTS, e.getMessage());
        }

        final Intent res = new Intent();
        res.putExtras(data);
        res.putExtras(dataPicturesUpload);

        return res;
    }

    public static List<PicturesStore> ParsePicturesJsonArray(JSONArray jsonPicturesArray, PointsStore point, DbInstance dbInstance, Context context){

        ArrayList<PicturesStore> picStore = new ArrayList<PicturesStore>();

        if(dbInstance.getDatabaseHelper() == null)
            return picStore;

        RuntimeExceptionDao<PicturesStore, Long>    daoPicture = dbInstance.getDatabaseHelper().getPicturesDataDao();

        boolean haveNewPoints = false;

        final String OPPM_PICTURE_ID        = "id";
        final String OPPM_FULL_PHOTOURL     = "full_photoURL";
        final String OPPM_CREATED_AT        = "created_at";
        final String OPPM_UPDATED_AT        = "updated_at";

        Date gmtTime;
        Date fromGmt;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

        int i = 0;

        while (i < jsonPicturesArray.length()) {

            long    pic_picture_id;
            String  pic_full_photoUrl;
            long    pic_created_at;
            long    pic_updated_at;

            try {

                JSONObject pictureItem = jsonPicturesArray.getJSONObject(i);

                pic_picture_id            = pictureItem.getLong(OPPM_PICTURE_ID);
                pic_full_photoUrl         = pictureItem.getString(OPPM_FULL_PHOTOURL);

                gmtTime = sdf.parse(pictureItem.getString(OPPM_CREATED_AT));
                fromGmt = new Date(gmtTime.getTime() + TimeZone.getDefault().getOffset(gmtTime.getTime()));
                pic_created_at = fromGmt.getTime();

                gmtTime = sdf.parse(pictureItem.getString(OPPM_UPDATED_AT));
                fromGmt = new Date(gmtTime.getTime() + TimeZone.getDefault().getOffset(gmtTime.getTime()));
                pic_updated_at = fromGmt.getTime();

                PicturesStore newPs = GetPictureWithServerIDExists(pic_picture_id, daoPicture);

                if(newPs == null)
                {
                    newPs = new PicturesStore(pic_full_photoUrl,point,pic_created_at,pic_updated_at,1,null);

                    newPs.setServer_id(pic_picture_id);
                    newPs.setIs_new(0);
                    newPs.setIs_changed(0);
                    newPs.setIs_deleted(0);
                    newPs.setPhoto_was_uploaded(1);

                    daoPicture.create(newPs);

                    picStore.add(newPs);

                    haveNewPoints = true;
                }

            } catch (JSONException | ParseException e) {

                //Log.e(LOG_TAG, e.getMessage());
                e.printStackTrace();
            }
            i++;
        }

//        if(haveNewPoints){
//            context.getContentResolver().insert(EnvironmentMonitorContract.POINTS_CONTENT_URI, null);
//        }

        return picStore;
    }

    public static List<MessagesStore> ParseMessageJsonArray(JSONArray jsonMessagesArray, PointsStore point, DbInstance dbInstance, Context context){

        ArrayList<MessagesStore> messageStore = new ArrayList<MessagesStore>();

        if(dbInstance.getDatabaseHelper() == null)
            return messageStore;

        RuntimeExceptionDao<MessagesStore, Long>    daoMessage = dbInstance.getDatabaseHelper().getMessagesDataDao();

        boolean haveNewPoints = false;

        final String M_MESSAGE_ID         = "id";
        final String M_CREATED_AT         = "created_at";
        final String M_UPDATED_AT         = "updated_at";
        final String M_USER_EMAIL         = "email";
        final String M_USER_NAME          = "username";
        final String M_USER_ID            = "user_id";
        final String M_FIRST_NAME         = "first_name";
        final String M_LAST_NAME          = "last_name";
        final String M_MESSAGE_BODY       = "comment";

        Date gmtTime;
        Date fromGmt;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

        int i = 0;

        while (i < jsonMessagesArray.length()) {

            long    mess_picture_id;
            long    mess_created_at;
            long    mess_updated_at;
            String  mess_message_body;
            String  mess_user_email;
            String  mess_user_name;
            long    mess_user_id;
            String  mess_first_name;
            String  mess_last_name;

            try {

                JSONObject messageItem = jsonMessagesArray.getJSONObject(i);

                mess_picture_id          = messageItem.getLong(M_MESSAGE_ID);
                mess_message_body        = messageItem.getString(M_MESSAGE_BODY);
                mess_first_name          = messageItem.getString(M_FIRST_NAME);
                mess_last_name           = messageItem.getString(M_LAST_NAME);
                mess_user_name           = messageItem.getString(M_USER_NAME);
                mess_user_id             = messageItem.getLong(M_USER_ID);
                mess_user_email          = messageItem.getString(M_USER_EMAIL);

                gmtTime = sdf.parse(messageItem.getString(M_CREATED_AT));
                fromGmt = new Date(gmtTime.getTime() + TimeZone.getDefault().getOffset(gmtTime.getTime()));
                mess_created_at = fromGmt.getTime();

                gmtTime = sdf.parse(messageItem.getString(M_UPDATED_AT));
                fromGmt = new Date(gmtTime.getTime() + TimeZone.getDefault().getOffset(gmtTime.getTime()));
                mess_updated_at = fromGmt.getTime();

                MessagesStore newMessage = GetMessagesWithServerIDExists(mess_picture_id, daoMessage);

                if(newMessage == null)
                {
                    newMessage = new MessagesStore(point,mess_created_at, mess_message_body);
                    newMessage.setUser_photo_url("");
                    newMessage.setUser_email(mess_user_email);
                    newMessage.setUser_id(mess_user_id);
                    newMessage.setUser_name(mess_user_name);
                    newMessage.setLast_name(mess_last_name);
                    newMessage.setFirst_name(mess_first_name);
                    newMessage.setServer_id(mess_picture_id);
                    newMessage.setUpdated_at(mess_updated_at);
                    newMessage.setServer_id(mess_picture_id);
                    newMessage.setIs_new(0);
                    newMessage.setWas_sent(1);

                    daoMessage.create(newMessage);

                    messageStore.add(newMessage);
                    haveNewPoints = true;
                }

            } catch (JSONException | ParseException e) {

                //Log.e(LOG_TAG, e.getMessage());
                e.printStackTrace();
            }
            i++;
        }

//        if(haveNewPoints){
//            context.getContentResolver().insert(EnvironmentMonitorContract.POINTS_CONTENT_URI, null);
//        }

        return messageStore;
    }

    public static List<PointsStore> ParsePointsJsonArray(JSONArray jsonPointsArray, DbInstance dbInstance, Context context){

        ArrayList<PointsStore> pointsStore = new ArrayList<PointsStore>();

        if(dbInstance.getDatabaseHelper() == null)
            return pointsStore;

        RuntimeExceptionDao<PointsStore, Long>      daoPoint   = dbInstance.getDatabaseHelper().getPointsDataDao();

        boolean haveNewPoints = false;

        final String OPM_PICTURE_LIST = "pictures";
        final String OPM_MESSAGES_LIST = "vote";

        final String OPM_POLLUTION_MARK_ID  = "id";
        final String OPM_HEAD_LINE          = "headline";
        final String OPM_FULL_DESCRIPTION   = "full_description";
        final String OPM_USER_EMAIL         = "email";
        final String OPM_TYPE               = "type";
        final String OPM_USER_NAME          = "username";
        final String OPM_USER_ID            = "user_id";
        final String OPM_FIRST_NAME         = "first_name";
        final String OPM_LAST_NAME          = "last_name";
        final String OPM_LATITUDE           = "latitude";
        final String OPM_LONGITUDE          = "longitude";
        final String OPM_CREATED_AT         = "created_at";
        final String OPM_ATTITUDE           = "attitude";
        final String OPM_UPDATED_AT         = "updated_at";

        Date gmtTime;
        Date fromGmt;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

        int i = 0;
        while (i < jsonPointsArray.length()) {

            long    point_id;
            String  head_line;
            String  full_description;
            int     type;
            String  user_name;
            String  first_name;
            String  last_name;
            String  user_email;
            long     user_id;
            double  latitude;
            double  longitude;
            double  attitude;
            long    created_at;
            long    updated_at;

            try {

                JSONObject pointItem = jsonPointsArray.getJSONObject(i);

                point_id            = pointItem.getLong(OPM_POLLUTION_MARK_ID);
                first_name          = pointItem.getString(OPM_FIRST_NAME);
                last_name           = pointItem.getString(OPM_LAST_NAME);
                head_line           = pointItem.getString(OPM_HEAD_LINE);
                full_description    = pointItem.getString(OPM_FULL_DESCRIPTION);
                type                = pointItem.getInt(OPM_TYPE);
                user_name           = pointItem.getString(OPM_USER_NAME);
                user_id             = pointItem.getLong(OPM_USER_ID);
                user_email          = pointItem.getString(OPM_USER_EMAIL);
                latitude            = pointItem.getDouble(OPM_LATITUDE);
                longitude           = pointItem.getDouble(OPM_LONGITUDE);
                attitude            = pointItem.getDouble(OPM_ATTITUDE);

                gmtTime = sdf.parse(pointItem.getString(OPM_CREATED_AT));
                fromGmt = new Date(gmtTime.getTime() + TimeZone.getDefault().getOffset(gmtTime.getTime()));
                created_at = fromGmt.getTime();

                gmtTime = sdf.parse(pointItem.getString(OPM_UPDATED_AT));
                fromGmt = new Date(gmtTime.getTime() + TimeZone.getDefault().getOffset(gmtTime.getTime()));
                updated_at = fromGmt.getTime();

                PointsStore newPs = GetPointWithServerIDExists(point_id, daoPoint);
                if( newPs != null && newPs.getUpdated_at() < updated_at ){

                    newPs.setHeadline(head_line);
                    newPs.setFull_description(full_description);
                    newPs.setUpdated_at(updated_at);
                    daoPoint.update(newPs);

                    haveNewPoints = true;

                }else if(newPs == null)
                {
                    newPs = new PointsStore(head_line, full_description,
                            longitude, latitude, attitude,
                            created_at, updated_at,
                            user_name, first_name, last_name, user_email);

                    newPs.setServer_id(point_id);
                    newPs.setType(type);
                    newPs.setUser_id(user_id);
                    newPs.setIs_new(0);
                    newPs.setIs_changed(0);
                    newPs.setIs_deleted(0);
                    newPs.setPhoto_was_uploaded(1);

                    daoPoint.create(newPs);

                    haveNewPoints = true;
                }

                pointsStore.add(newPs);
                JSONArray picturesArray = pointItem.getJSONArray(OPM_PICTURE_LIST);
                ParsePicturesJsonArray(picturesArray, newPs, dbInstance, context);

                JSONArray messagesArray = pointItem.getJSONArray(OPM_MESSAGES_LIST);
                ParseMessageJsonArray(messagesArray, newPs, dbInstance, context);

            } catch (JSONException | ParseException e) {
                //Log.e(LOG_TAG, e.getMessage());
                e.printStackTrace();
            }
            i++;
        }

        return pointsStore;
    }
}
