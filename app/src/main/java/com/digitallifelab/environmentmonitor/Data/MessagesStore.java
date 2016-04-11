package com.digitallifelab.environmentmonitor.Data;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "messages")
public class MessagesStore {

    public static final String LOCAL_ID          = "_id";
    public static final String SERVER_ID         = "SEVER_ID";
    public static final String POINT_ID          = "POINT_ID";
    public static final String MESSAGE_BODY      = "MESSAGE_BODY";
    public static final String CREATED_AT        = "CREATED_AT";
    public static final String UPDATED_AT        = "UPDATED_AT";
    public static final String USER_NAME         = "USER_NAME";
    public static final String FIRST_NAME        = "FIRST_NAME";
    public static final String LAST_NAME         = "LAST_NAME";
    public static final String USER_ID           = "USER_ID";
    public static final String USER_EMAIL        = "USER_EMAIL";
    public static final String USER_PHOTO_URL    = "USER_PHOTO_URL";
    public static final String IS_NEW            = "IS_NEW";
    public static final String WAS_SENT          = "WAS_SENT";


    @DatabaseField(canBeNull = false, columnName = LOCAL_ID, generatedId = true)
    private long local_id;

    @DatabaseField(canBeNull = true, columnName = SERVER_ID)
    private long server_id;

    @DatabaseField(canBeNull = true, columnName = POINT_ID, foreign = true, foreignAutoRefresh = true, foreignColumnName = EnvironmentMonitorContract.Points.LOCAL_ID)
    private PointsStore point;

    @DatabaseField(canBeNull = true, columnName = MESSAGE_BODY)
    private String comment;

    @DatabaseField(canBeNull = true, columnName = CREATED_AT)
    private long created_at;

    @DatabaseField(canBeNull = true, columnName = UPDATED_AT)
    private long updated_at;

    @DatabaseField(canBeNull = true, columnName = USER_NAME)
    private String user_name;

    @DatabaseField(canBeNull = true, columnName = FIRST_NAME)
    private String first_name;

    @DatabaseField(canBeNull = true, columnName = LAST_NAME)
    private String last_name;

    @DatabaseField(canBeNull = true, columnName = USER_ID)
    private long user_id;

    @DatabaseField(canBeNull = true, columnName = USER_EMAIL)
    private String user_email;

    @DatabaseField(canBeNull = true, columnName = USER_PHOTO_URL)
    private String user_photo_url;

    @DatabaseField(canBeNull = false, columnName = IS_NEW)
    private int is_new;

    @DatabaseField(canBeNull = false, columnName = WAS_SENT)
    private int was_sent;

    public long pollution_mark;

    public MessagesStore() {
    }

    public MessagesStore(PointsStore point, long created_at, String message) {

        AccountsStore acc = AccountsStore.getActiveUser();

        this.point = point;
        this.comment = message;
        this.created_at = created_at;
        this.updated_at = created_at;
        this.is_new     = 1;
        this.was_sent   = 0;

        if (acc != null) {
            this.user_name  = acc.getUser_name();
            this.first_name = acc.getFirst_name();
            this.last_name  = acc.getLast_name();
            this.user_email = acc.getEmail();
            this.user_photo_url = acc.getPhotoUrl();
        }
    }

    public String getUser_photo_url() {
        return user_photo_url;
    }

    public void setUser_photo_url(String user_photo_url) {
        this.user_photo_url = user_photo_url;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
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

    public int getWas_sent() {
        return was_sent;
    }

    public void setWas_sent(int was_sent) {
        this.was_sent = was_sent;
    }
}
