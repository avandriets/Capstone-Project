package com.digitallifelab.environmentmonitor.Data;
import com.digitallifelab.environmentmonitor.Utils.DbInstance;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.List;


@DatabaseTable(tableName = "accounts")
public class AccountsStore {

    public enum OAuthProviders{
        google, facebook
    }

    public static final String EMAIL_FIELD_NAME             = "EMAIL";
    public static final String USER_DISPLAY_NAME_FIELD_NAME = "USER_DISPLAY_NAME";
    public static final String PHOTO_URL_FIELD_NAME         = "PHOTO_URL";
    public static final String PROVIDER_FIELD_NAME          = "PROVIDER";
    public static final String ACTIVE_FIELD_NAME            = "ACTIVE";
    public static final String GOOGLE_ACCESS_TOKEN_FIELD_NAME       = "GOOGLE_ACCESS_TOKEN";
    public static final String MY_SERVER_ACCESS_TOKEN_FIELD_NAME    = "MY_SERVER_ACCESS_TOKEN";
    public static final String USER_NAME          = "USER_NAME";
    public static final String FIRST_NAME         = "FIRST_NAME";
    public static final String LAST_NAME          = "LAST_NAME";

    @DatabaseField(id = true, canBeNull = false, columnName = EMAIL_FIELD_NAME)
    private String Email;

    @DatabaseField(columnName = USER_DISPLAY_NAME_FIELD_NAME)
    private String UserDisplayName;

    @DatabaseField(columnName = PHOTO_URL_FIELD_NAME)
    private String photoUrl;

    @DatabaseField(columnName = PROVIDER_FIELD_NAME)
    private OAuthProviders provider;

    @DatabaseField(columnName = ACTIVE_FIELD_NAME)
    private int  active;

    @DatabaseField(columnName = GOOGLE_ACCESS_TOKEN_FIELD_NAME)
    private String google_access_token;

    @DatabaseField(columnName = MY_SERVER_ACCESS_TOKEN_FIELD_NAME)
    private String my_server_access_token;

    @DatabaseField(columnName = USER_NAME)
    private String user_name;

    @DatabaseField(columnName = FIRST_NAME)
    private String first_name;

    @DatabaseField(columnName = LAST_NAME)
    private String last_name;


    public AccountsStore() {
        // ORMLite needs a no-arg constructor
    }

    public AccountsStore(String email, String userDisplayName, String photoUrl, OAuthProviders provider) {
        Email = email;
        UserDisplayName = userDisplayName;
        this.photoUrl = photoUrl;
        this.provider = provider;
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

    public int getActive() {
        return active;
    }

    public void setActive(int active) {
        this.active = active;
    }

    public String getToken() {
        return google_access_token;
    }

    public void setToken(String token) {
        this.google_access_token = token;
    }

    public String getMy_server_access_token() {
        return my_server_access_token;
    }

    public void setMy_server_access_token(String my_server_access_token) {
        this.my_server_access_token = my_server_access_token;
    }

    public String getEmail() {
        return Email;
    }

    public void setEmail(String email) {
        Email = email;
    }

    public String getUserDisplayName() {
        return UserDisplayName;
    }

    public void setUserDisplayName(String userDisplayName) {
        UserDisplayName = userDisplayName;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public OAuthProviders getProvider() {
        return provider;
    }

    public void setProvider(OAuthProviders provider) {
        this.provider = provider;
    }

    public static AccountsStore getActiveUser(){

        DbInstance dbInstance  = null;

        dbInstance = new DbInstance();

        RuntimeExceptionDao<AccountsStore, String> simpleDao = dbInstance.getDatabaseHelper().getAccountsDataDao();

        List<AccountsStore> list = simpleDao.queryForAll();

        for (AccountsStore user : list) {
            if(user.active == 1)
                return user;
        }

        return null;
    }
}
