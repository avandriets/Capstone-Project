package com.digitallifelab.environmentmonitor.Data;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "pictures")
public class PicturesStore {

    public static final String ID                   = "_id";
    public static final String SERVER_ID            = "SERVER_ID";
    public static final String FULL_PHOTO_URL       = "FULL_PHOTO_URL";
    public static final String POINT_ID             = "POINT_ID";
    public static final String CREATED_AT           = "CREATED_AT";
    public static final String UPDATED_AT           = "UPDATED_AT";
    public static final String PHOTO_WAS_UPLOADED   = "PHOTO_WAS_UPLOADED";
    public static final String LOCAL_PHOTO_PATH     = "LOCAL_PHOTO_PATH";
    public static final String IS_NEW               = "IS_NEW";
    public static final String IS_CHANGED           = "IS_CHANGED";
    public static final String IS_DELETED           = "IS_DELETED";

    @DatabaseField(canBeNull = false, columnName = ID, generatedId = true)
    private long id;

    @DatabaseField(canBeNull = true, columnName = SERVER_ID)
    private long server_id;

    @DatabaseField(canBeNull = true, columnName = FULL_PHOTO_URL)
    private String full_photo_url;

    @DatabaseField(canBeNull = false, columnName = POINT_ID, foreign = true, foreignAutoRefresh = true, maxForeignAutoRefreshLevel = 3, foreignColumnName = EnvironmentMonitorContract.Points.LOCAL_ID)
    private PointsStore point;

    @DatabaseField(canBeNull = true, columnName = CREATED_AT)
    private long created_at;

    @DatabaseField(canBeNull = true, columnName = UPDATED_AT)
    private long updated_at;

    @DatabaseField(canBeNull = false, columnName = PHOTO_WAS_UPLOADED)
    private int photo_was_uploaded;

    @DatabaseField(canBeNull = true, columnName = LOCAL_PHOTO_PATH)
    private String local_photo_path;

    @DatabaseField(canBeNull = false, columnName = IS_NEW)
    private int is_new;

    @DatabaseField(canBeNull = false, columnName = IS_CHANGED)
    private int is_changed;

    @DatabaseField(canBeNull = false, columnName = IS_DELETED)
    private int is_deleted;

    public PicturesStore() {
    }

    public PicturesStore(String full_photo_url, PointsStore point, long created_at, long updated_at,
                         int photo_was_uploaded, String local_photo_path) {

        this.full_photo_url = full_photo_url;
        this.point = point;
        this.created_at = created_at;
        this.updated_at = updated_at;
        this.photo_was_uploaded = photo_was_uploaded;
        this.local_photo_path = local_photo_path;
        this.is_new     = 1;
        this.is_changed = 0;
        this.is_deleted = 0;
    }

    public long getServer_id() {
        return server_id;
    }

    public void setServer_id(long server_id) {
        this.server_id = server_id;
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

    public int getPhoto_was_uploaded() {
        return photo_was_uploaded;
    }

    public void setPhoto_was_uploaded(int photo_was_uploaded) {
        this.photo_was_uploaded = photo_was_uploaded;
    }

    public String getLocal_photo_path() {
        return local_photo_path;
    }

    public void setLocal_photo_path(String local_photo_path) {
        this.local_photo_path = local_photo_path;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getFull_photo_url() {
        return full_photo_url;
    }

    public void setFull_photo_url(String full_photo_url) {
        this.full_photo_url = full_photo_url;
    }

    public PointsStore getPoint() {
        return point;
    }

    public void setPoint(PointsStore point) {
        this.point = point;
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
}
