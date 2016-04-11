package com.digitallifelab.environmentmonitor;

import android.database.Cursor;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.digitallifelab.environmentmonitor.Data.EnvironmentMonitorContract;
import com.digitallifelab.environmentmonitor.Data.OrmLiteCursorLoader;
import com.digitallifelab.environmentmonitor.Data.PointsStore;
import com.digitallifelab.environmentmonitor.Utils.DbInstance;
import com.digitallifelab.environmentmonitor.Utils.MyGoogleApiClient_Singleton;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.SelectArg;
import com.j256.ormlite.stmt.Where;

import java.sql.SQLException;


public class MyMapFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>{

    private static final String LOG_TAG = MyMapFragment.class.getSimpleName();
    private static final int POINTS_LIST_ID_LOADER = 1;
    private MapView mMapView;
    private GoogleMap mMap;

    private Dao<PointsStore,Long> pointsStoreDao;
    private PreparedQuery<PointsStore> preparedQuery;

    private MarkerOptions point1;

    public MyMapFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_map, container, false);

        DbInstance dbInstance = new DbInstance();

        MapsInitializer.initialize(getActivity());

        mMapView = (MapView) rootView.findViewById(R.id.id_map);
        mMapView.onCreate(savedInstanceState);
        setUpMapIfNeeded(rootView);

        try {
            pointsStoreDao = dbInstance.getDatabaseHelper().getPointsDao();

            //TODO add hiding of deleted items
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

        return rootView;
    }

    private void setUpMapIfNeeded(View inflatedView) {
        if (mMap == null) {
            mMap = ((MapView) inflatedView.findViewById(R.id.id_map)).getMap();
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    private void setUpMap() {

        Location mLastLocation = MyGoogleApiClient_Singleton.getmLastLocation();

        double latitude     = 0;
        double longitude    = 0;
        if (mLastLocation != null) {
            latitude = mLastLocation.getLatitude();
            longitude = mLastLocation.getLongitude();
            Log.d(LOG_TAG, "Latitude " + String.valueOf(mLastLocation.getLatitude()));
            Log.d(LOG_TAG, "Longitude " + String.valueOf(mLastLocation.getLongitude()));
        }

        CameraPosition cp = CameraPosition.builder()
                .target(new LatLng(latitude, longitude))
                .zoom(10)                  // Sets the zoom
                .bearing(0)                // Sets the orientation of the camera from north clockwise in degrees
                //.tilt(30)                // Sets the tilt of the camera to 30 degrees
                .build();

        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cp));

    }

    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    public void onDestroy() {
        mMapView.onDestroy();
        super.onDestroy();
    }

    @Override
    public android.support.v4.content.Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Log.d(LOG_TAG, "Create loader");
        return new OrmLiteCursorLoader(getActivity(), pointsStoreDao, preparedQuery);
    }

    @Override
    public void onLoadFinished(android.support.v4.content.Loader<Cursor> loader, Cursor data) {
        Log.d(LOG_TAG, "Finish loading");

        double latitude     = 0.0;
        double longitude    = 0.0;
        String headLine;

        if(data != null && data.getCount() > 0){

            data.moveToFirst();
            do {
                latitude    = data.getDouble(data.getColumnIndex(EnvironmentMonitorContract.Points.LATITUDE));
                longitude   = data.getDouble(data.getColumnIndex(EnvironmentMonitorContract.Points.LONGITUDE));
                headLine    = data.getString(data.getColumnIndex(EnvironmentMonitorContract.Points.HEADLINE));

                mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(latitude, longitude))
                        .title(headLine)
                        //.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_sentiment_dissatisfied_black_18dp))
                );

            }while(data.moveToNext());
        }
    }

    @Override
    public void onLoaderReset(android.support.v4.content.Loader<Cursor> loader) {
    }

    public void changeCameraPosition(Long pId){

        if(pId != -1) {
            DbInstance dbInstance = new DbInstance();
            RuntimeExceptionDao<PointsStore, Long> pointDao = dbInstance.getDatabaseHelper().getPointsDataDao();
            PointsStore point = pointDao.queryForId(pId);


            CameraPosition cp = CameraPosition.builder()
                    .target(new LatLng(point.getLatitude(), point.getLongitude()))
                    .zoom(mMap.getCameraPosition().zoom)                  // Sets the zoom
                    .bearing(mMap.getCameraPosition().bearing)                // Sets the orientation of the camera from north clockwise in degrees
                            //.tilt(30)                // Sets the tilt of the camera to 30 degrees
                    .build();

            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cp));
        }
    }
}
