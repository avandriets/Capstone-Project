package com.digitallifelab.environmentmonitor;

import android.annotation.TargetApi;
import android.database.Cursor;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.digitallifelab.environmentmonitor.Data.EnvironmentMonitorContract;
import com.digitallifelab.environmentmonitor.Data.OrmLiteCursorLoader;
import com.digitallifelab.environmentmonitor.Data.PointsStore;
import com.digitallifelab.environmentmonitor.Utils.DbInstance;
import com.digitallifelab.environmentmonitor.Utils.MyGoogleApiClient_Singleton;
import com.digitallifelab.environmentmonitor.adapters.PointsRecyclerViewAdapter;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.SelectArg;
import com.j256.ormlite.stmt.Where;

import java.sql.SQLException;

/**
 * A placeholder fragment containing a simple view.
 */
public class PointsListFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int POINTS_LIST_ID_LOADER = 1;
    private static final String LOG_TAG = PointsListFragment.class.getSimpleName();

    private Dao<PointsStore,Long>         pointsStoreDao;
    private PreparedQuery<PointsStore>    preparedQuery;

    private PointsRecyclerViewAdapter   mAdapter;
    private RecyclerView                mRecyclerView;

    DbInstance dbInstance;
    private double latitude = 0;
    private double longitude = 0;

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (null != mRecyclerView) {
            mRecyclerView.clearOnScrollListeners();
        }
    }

    public interface Callback {
        public void onItemSelected(Long pId, PointsRecyclerViewAdapter.PointsViewHolder vh);
    }

    public PointsListFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_points_list, container, false);

        dbInstance = new DbInstance();

        View emptyView = rootView.findViewById(R.id.points_list_empty);

        Location mLastLocation = MyGoogleApiClient_Singleton.getmLastLocation();
        if (mLastLocation != null) {
            latitude = mLastLocation.getLatitude();
            longitude = mLastLocation.getLongitude();
            Log.d(LOG_TAG, "Latitude " + String.valueOf(mLastLocation.getLatitude()));
            Log.d(LOG_TAG, "Longitude " + String.valueOf(mLastLocation.getLongitude()));
        }

        mAdapter = new PointsRecyclerViewAdapter(getActivity(),
                new PointsRecyclerViewAdapter.PointsAdapterOnClickHandler() {
                    @Override
                    public void onClick(Long date, PointsRecyclerViewAdapter.PointsViewHolder vh) {
                        ((Callback) getActivity()).onItemSelected(date, vh);
                    }
                }, emptyView,  dbInstance, longitude, latitude);

        mRecyclerView = (RecyclerView)rootView.findViewById(R.id.idPointsListView);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.setAdapter(mAdapter);

        final View parallaxView = rootView.findViewById(R.id.parallax_bar);
        if (null != parallaxView) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
                    @Override
                    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                        super.onScrolled(recyclerView, dx, dy);
                        int max = parallaxView.getHeight();
                        if (dy > 0) {
                            parallaxView.setTranslationY(Math.max(-max, parallaxView.getTranslationY() - dy / 2));
                        } else {
                            parallaxView.setTranslationY(Math.min(0, parallaxView.getTranslationY() - dy / 2));
                        }
                    }
                });
            }
        }

        final AppBarLayout appbarView = (AppBarLayout)rootView.findViewById(R.id.appbar);
        if (null != appbarView) {
            ViewCompat.setElevation(appbarView, 0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                    @Override
                    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                        if (0 == mRecyclerView.computeVerticalScrollOffset()) {
                            appbarView.setElevation(0);
                        } else {
                            appbarView.setElevation(appbarView.getTargetElevation());
                        }
                    }
                });
            }
        }

        return rootView;
    }

    public void changeCoordinates(double latitude, double longitude){
        this.latitude   = latitude;
        this.longitude  = longitude;

        mAdapter.SetCoordinates(latitude, longitude);
        mAdapter.notifyDataSetChanged();
    }

    public void changeUnits(){

        mAdapter.setUnits();
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        try {
            pointsStoreDao = dbInstance.getDatabaseHelper().getPointsDao();

            SelectArg selectArgDeleted =new SelectArg();
            selectArgDeleted.setValue(0);

            QueryBuilder<PointsStore, Long> queryBuilder = pointsStoreDao.queryBuilder();
            Where<PointsStore, Long> where = queryBuilder.where().eq(EnvironmentMonitorContract.Points.IS_DELETED, selectArgDeleted);
            preparedQuery = where.prepare();

        } catch (SQLException e) {
            e.printStackTrace();
        }


        android.support.v4.content.Loader<Object> loader = getLoaderManager().getLoader(POINTS_LIST_ID_LOADER);

        if (loader != null && !loader.isReset()) {
            getLoaderManager().restartLoader(POINTS_LIST_ID_LOADER, null, this);
        } else {
            getLoaderManager().initLoader(POINTS_LIST_ID_LOADER, null, this);
        }

        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public android.support.v4.content.Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Log.d(LOG_TAG,"Create loader");
        return new OrmLiteCursorLoader(getActivity(), pointsStoreDao, preparedQuery);
    }

    @Override
    public void onLoadFinished(android.support.v4.content.Loader<Cursor> loader, Cursor data) {
        Log.d(LOG_TAG,"Finish loading");
        mAdapter.changeCursor(data, preparedQuery);
    }

    @Override
    public void onLoaderReset(android.support.v4.content.Loader<Cursor> loader) {
        mAdapter.swapCursor(null); //changeCursor(null, preparedQuery);
    }
}
