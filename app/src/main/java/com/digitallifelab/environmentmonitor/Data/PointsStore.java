package com.digitallifelab.environmentmonitor.Data;


import com.digitallifelab.environmentmonitor.Utils.Utility;
import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;
import com.digitallifelab.environmentmonitor.Data.EnvironmentMonitorContract.Points;

import java.io.Serializable;

@DatabaseTable(tableName = "points")
public class PointsStore implements Serializable {

    @DatabaseField(canBeNull = false, columnName = Points.LOCAL_ID, generatedId = true)
    private long local_id;

    @DatabaseField(canBeNull = true, columnName = Points.SERVER_ID)
    private long server_id;

    @DatabaseField(canBeNull = true, columnName = Points.HEADLINE)
    private String headline;

    @DatabaseField(canBeNull = true, columnName = Points.FULL_DESCRIPTION)
    private String full_description;

    @DatabaseField(canBeNull = true, columnName = Points.LONGITUDE)
    private double longitude;

    @DatabaseField(canBeNull = true, columnName = Points.LATITUDE)
    private double latitude;

    @DatabaseField(canBeNull = true, columnName = Points.ATTITUDE)
    private double attitude;

    @DatabaseField(canBeNull = true, columnName = Points.CREATED_AT)
    private long created_at;

    @DatabaseField(canBeNull = true, columnName = Points.UPDATED_AT)
    private long updated_at;

    @DatabaseField(canBeNull = true, columnName = Points.USER_NAME)
    private String user_name;

    @DatabaseField(canBeNull = true, columnName = Points.FIRST_NAME)
    private String first_name;

    @DatabaseField(canBeNull = true, columnName = Points.LAST_NAME)
    private String last_name;

    @DatabaseField(canBeNull = true, columnName = Points.USER_ID)
    private long user_id;

    @DatabaseField(canBeNull = true, columnName = Points.TYPE)
    private long type;

    @DatabaseField(canBeNull = true, columnName = Points.USER_EMAIL)
    private String user_email;

    @DatabaseField(canBeNull = false, columnName = Points.IS_NEW)
    private int is_new;

    @DatabaseField(canBeNull = false, columnName = Points.IS_CHANGED)
    private int is_changed;

    @DatabaseField(canBeNull = false, columnName = Points.IS_DELETED)
    private int is_deleted;

    @ForeignCollectionField
    private transient ForeignCollection<PicturesStore> pictures;

    @DatabaseField(canBeNull = false, columnName = Points.POINT_WAS_UPLOADED)
    private int photo_was_uploaded;


    public PointsStore() {
    }

    public PointsStore(String headline, String full_description, double longitude, double latitude,
                       double attitude, long created_at, long updated_at, String user_name,
                       String first_name, String last_name, String user_email) {

        this.headline = headline;
        this.full_description = full_description;
        this.longitude  = longitude;
        this.latitude   = latitude;
        this.attitude   = attitude;
        this.created_at = created_at;
        this.updated_at = updated_at;
        this.user_name  = user_name;
        this.first_name = first_name;
        this.last_name  = last_name;
        this.type       = Utility.PHOTO_FACT;
        this.user_email = user_email;
        this.is_new     = 1;
        this.is_changed = 0;
        this.is_deleted = 0;
        this.photo_was_uploaded = 0;
    }

    public String getUser_email() {
        return user_email;
    }

    public void setUser_email(String user_email) {
        this.user_email = user_email;
    }

    public int getIs_new() {
        return is_new;
    }

    public void setIs_new(int is_new) {
        this.is_new = is_new;
    }

    public int getIs_changed() {
        return is_changed;
    }

    public void setIs_changed(int is_changed) {
        this.is_changed = is_changed;
    }

    public int getIs_deleted() {
        return is_deleted;
    }

    public void setIs_deleted(int is_deleted) {
        this.is_deleted = is_deleted;
    }

    public long getLocal_id() {
        return local_id;
    }

    public void setLocal_id(long local_id) {
        this.local_id = local_id;
    }

    public long getServer_id() {
        return server_id;
    }

    public void setServer_id(long server_id) {
        this.server_id = server_id;
    }

    public String getHeadline() {
        return headline;
    }

    public void setHeadline(String headline) {
        this.headline = headline;
    }

    public String getFull_description() {
        return full_description;
    }

    public void setFull_description(String full_description) {
        this.full_description = full_description;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getAttitude() {
        return attitude;
    }

    public void setAttitude(double attitude) {
        this.attitude = attitude;
    }

    public long getCreated_at() {
        return created_at;
    }

    public void setCreated_at(long created_at) {
        this.created_at = created_at;
    }

    public long getUpdated_at() {
        return updated_at;
    }

    public void setUpdated_at(long updated_at) {
        this.updated_at = updated_at;
    }

    public String getUser_name() {
        return user_name;
    }

    public void setUser_name(String user_name) {
        this.user_name = user_name;
    }

    public String getFirst_name() {
        return first_name;
    }

    public void setFirst_name(String first_name) {
        this.first_name = first_name;
    }

    public String getLast_name() {
        return last_name;
    }

    public void setLast_name(String last_name) {
        this.last_name = last_name;
    }

    public long getUser_id() {
        return user_id;
    }

    public void setUser_id(long user_id) {
        this.user_id = user_id;
    }

    public long getType() {
        return type;
    }

    public void setType(long type) {
        this.type = type;
    }

    public ForeignCollection<PicturesStore> getPictures() {
        return pictures;
    }

    public int getPhoto_was_uploaded() {
        return photo_was_uploaded;
    }

    public void setPhoto_was_uploaded(int photo_was_uploaded) {
        this.photo_was_uploaded = photo_was_uploaded;
    }
}
