package com.digitallifelab.environmentmonitor.Data;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.support.annotation.Nullable;
import com.digitallifelab.environmentmonitor.Utils.DbInstance;
import com.j256.ormlite.android.AndroidDatabaseResults;
import com.j256.ormlite.dao.CloseableIterator;
import com.j256.ormlite.stmt.QueryBuilder;


public class EnvironmentMonitorProvider extends ContentProvider {

    private static final int POINTS = 100;
    private static final int POINT_ID = 101;

    private DbInstance dbInstance  = null;
    private static final UriMatcher sUriMatcher = buildUriMatcher();


    static UriMatcher buildUriMatcher() {

        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = EnvironmentMonitorContract.CONTENT_AUTHORITY;

        matcher.addURI(authority, EnvironmentMonitorContract.PATH_POINTS, POINTS);
        matcher.addURI(authority, EnvironmentMonitorContract.PATH_POINTS + "/#", POINT_ID);

        return matcher;
    }

    @Override
    public boolean onCreate() {

        dbInstance = new DbInstance();
        dbInstance.SetDBHelper(getContext());

        return true;
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        int uriMatch = sUriMatcher.match(uri);
        switch (uriMatch) {
            case POINT_ID:
            /*String id = uri.getLastPathSegment();
            Card card = null;
            try {
                card = cardDao.queryBuilder().where().eq(Entry.ID_FIELD_NAME, id).queryForFirst();
            } catch (SQLException e) {
                e.printStackTrace();
            }*/
                //return null;
            case POINTS:
                // Return all known entries.
                // Note: Notification URI must be manually set here for loaders to correctly
                // register ContentObservers.
                // build your query


                QueryBuilder<PointsStore, Long> qb;// = dbInstance.getDatabaseHelper().getPointsDataDao().queryBuilder();

                // when you are done, prepare your query and build an iterator
                CloseableIterator<PointsStore> iterator = null;
                Cursor cursor = null;
                try {

                    if(dbInstance.getDatabaseHelper() == null){
                        dbInstance = new DbInstance();
                        dbInstance.SetDBHelper(getContext());
                    }

                    qb = dbInstance.getDatabaseHelper().getPointsDataDao().queryBuilder();

                    iterator = qb.orderBy(MessagesStore.UPDATED_AT,false).iterator(); //dbInstance.getDatabaseHelper().getPointsDataDao().iterator();
                    //iterator = dbInstance.getDatabaseHelper().getPointsDataDao().iterator();
                    AndroidDatabaseResults results = (AndroidDatabaseResults)iterator.getRawResults();
                    cursor = results.getRawCursor();

                    cursor.getCount();
                    cursor.setNotificationUri(this.getContext().getContentResolver(), uri);

                } catch (SQLException e) {
                    e.printStackTrace();
                }
                catch (java.sql.SQLException e) {
                    e.printStackTrace();
                }
                finally
                {
                    //iterator.closeQuietly();
                }
                return cursor;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Nullable
    @Override
    public String getType(Uri uri) {

        final int match = sUriMatcher.match(uri);

        switch (match) {
            case POINTS:
                return EnvironmentMonitorContract.POINTS_CONTENT_TYPE;
            case POINT_ID:
                return EnvironmentMonitorContract.POINTS_CONTENT_ITEM_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues values) {

        getContext().getContentResolver().notifyChange(uri, null);
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
