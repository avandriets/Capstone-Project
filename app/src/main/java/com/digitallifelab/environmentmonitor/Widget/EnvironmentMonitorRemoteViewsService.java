package com.digitallifelab.environmentmonitor.Widget;

import android.annotation.TargetApi;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import com.digitallifelab.environmentmonitor.Data.EnvironmentMonitorContract;
import com.digitallifelab.environmentmonitor.Data.MessagesStore;
import com.digitallifelab.environmentmonitor.Data.PointsStore;
import com.digitallifelab.environmentmonitor.R;
import com.digitallifelab.environmentmonitor.Utils.DbInstance;
import com.digitallifelab.environmentmonitor.Utils.Utility;
import com.j256.ormlite.android.AndroidDatabaseResults;
import com.j256.ormlite.dao.CloseableIterator;
import com.j256.ormlite.stmt.QueryBuilder;

import java.sql.SQLException;
import java.text.SimpleDateFormat;


@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class EnvironmentMonitorRemoteViewsService extends RemoteViewsService {

    public final String LOG_TAG = EnvironmentMonitorRemoteViewsService.class.getSimpleName();

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

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new RemoteViewsFactory() {
            private Cursor data = null;

            @Override
            public void onCreate() {
                // Nothing to do
            }

            @Override
            public void onDataSetChanged() {
                if (data != null) {
                    data.close();
                }

                // This method is called by the app hosting the widget (e.g., the launcher)
                // However, our ContentProvider is not exported so it doesn't have access to the
                // data. Therefore we need to clear (and finally restore) the calling identity so
                // that calls use our process and permission
                final long identityToken = Binder.clearCallingIdentity();

                //TODO change to OrmLite
                //Uri scoreTable =  EnvironmentMonitorContract.POINTS_CONTENT_URI;
                //data = getContentResolver().query(scoreTable, null, null, null, "");

                DbInstance dbInstance = new DbInstance();
                if(dbInstance.getDatabaseHelper() == null){
                    dbInstance = new DbInstance();
                    dbInstance.SetDBHelper(getApplicationContext());
                }

                QueryBuilder<PointsStore, Long> qb = dbInstance.getDatabaseHelper().getPointsDataDao().queryBuilder();

                CloseableIterator iterator = null; //dbInstance.getDatabaseHelper().getPointsDataDao().iterator();
                try {
                    iterator = qb.limit(5).orderBy(MessagesStore.UPDATED_AT, false).iterator();

                    AndroidDatabaseResults results = (AndroidDatabaseResults)iterator.getRawResults();
                    data = results.getRawCursor();

                } catch (SQLException e) {

                }

                Binder.restoreCallingIdentity(identityToken);
            }

            @Override
            public void onDestroy() {
                if (data != null) {
                    data.close();
                    data = null;
                }
            }

            @Override
            public int getCount() {
                return data == null ? 0 : data.getCount();
            }

            @Override
            public RemoteViews getViewAt(int position) {

                if (position == AdapterView.INVALID_POSITION ||
                        data == null || !data.moveToPosition(position)) {
                    return null;
                }

                RemoteViews views = new RemoteViews(getPackageName(), R.layout.point_list_item_widger);

                views.setTextViewText(R.id.id_point_list_item_headline, data.getString(data.getColumnIndex(HEADLINE)));

                views.setTextViewText(R.id.id_list_item_part_article_textview , data.getString(data.getColumnIndex(FULL_DESCRIPTION)) );

                views.setTextViewText(R.id.id_list_item_date_textview , Utility.getFriendlyDayString(getApplicationContext(), data.getLong(data.getColumnIndex(CREATED_AT)), false));

                views.setTextViewText(R.id.idAuthor , data.getString(data.getColumnIndex(FIRST_NAME)) + " " + data.getString(data.getColumnIndex(LAST_NAME)));

                SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("HH:mm");
                views.setTextViewText(R.id.id_list_item_time_textview , shortenedDateFormat.format(data.getLong(data.getColumnIndex(CREATED_AT))));

                return views;
            }

            @Override
            public RemoteViews getLoadingView() {
                return new RemoteViews(getPackageName(), R.layout.point_list_item_widger);
            }

            @Override
            public int getViewTypeCount() {
                return 1;
            }

            @Override
            public long getItemId(int position) {
                if (data.moveToPosition(position))
                    return data.getLong(data.getColumnIndex(EnvironmentMonitorContract.Points.LOCAL_ID));
                return position;
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }
        };
    }
}
