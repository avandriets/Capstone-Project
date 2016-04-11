package com.digitallifelab.environmentmonitor.Data;


import android.content.ContentResolver;
import android.net.Uri;

public class EnvironmentMonitorContract {

    public static final String CONTENT_AUTHORITY = "com.digitallifelab.environmentmonitor";

    public static final String PATH_POINTS              = "points";
    public static final String PATH_POINTS_PICTURES     = "pictures";

    public static final String POINTS_CONTENT_TYPE         = ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_POINTS;
    public static final String POINTS_CONTENT_ITEM_TYPE    = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_POINTS;

    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);
    public static final Uri POINTS_CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_POINTS).build();

    public class Points {

        //Fields names
        public static final String LOCAL_ID          = "_id";
        public static final String SERVER_ID         = "SEVER_ID";
        public static final String HEADLINE          = "HEADLINE";
        public static final String FULL_DESCRIPTION  = "FULL_DESCRIPTION";
        public static final String LONGITUDE         = "LONGITUDE";
        public static final String LATITUDE          = "LATITUDE";
        public static final String ATTITUDE          = "ATTITUDE";
        public static final String CREATED_AT        = "CREATED_AT";
        public static final String UPDATED_AT        = "UPDATED_AT";
        public static final String USER_NAME         = "USER_NAME";
        public static final String FIRST_NAME        = "FIRST_NAME";
        public static final String LAST_NAME         = "LAST_NAME";
        public static final String USER_ID           = "USER_ID";
        public static final String TYPE              = "TYPE";
        public static final String USER_EMAIL        = "USER_EMAIL";
        public static final String IS_NEW            = "IS_NEW";
        public static final String IS_CHANGED        = "IS_CHANGED";
        public static final String IS_DELETED        = "IS_DELETED";
        public static final String POINT_WAS_UPLOADED = "POINT_WAS_UPLOADED";
    }
}
