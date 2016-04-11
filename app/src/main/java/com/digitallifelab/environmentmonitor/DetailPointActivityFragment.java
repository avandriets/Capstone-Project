package com.digitallifelab.environmentmonitor;

import android.app.ActivityOptions;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.digitallifelab.environmentmonitor.Data.AccountsStore;
import com.digitallifelab.environmentmonitor.Data.EnvironmentMonitorContract;
import com.digitallifelab.environmentmonitor.Data.EnvironmentService;
import com.digitallifelab.environmentmonitor.Data.OrmLiteCursorLoader;
import com.digitallifelab.environmentmonitor.Data.PicturesStore;
import com.digitallifelab.environmentmonitor.Data.PointsStore;
import com.digitallifelab.environmentmonitor.Utils.DbInstance;
import com.digitallifelab.environmentmonitor.Utils.ImageUtility;
import com.digitallifelab.environmentmonitor.Utils.NetworkServiceUtility;
import com.digitallifelab.environmentmonitor.Utils.Utility;
import com.digitallifelab.environmentmonitor.adapters.ImagesRecyclerViewAdapter;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.SelectArg;
import com.j256.ormlite.stmt.Where;

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

import butterknife.Bind;
import butterknife.ButterKnife;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * A placeholder fragment containing a simple view.
 */
public class DetailPointActivityFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    @Bind(R.id.headlineTextView)    TextView tvHeadLine;
    @Bind(R.id.dateTextView)        TextView tvDateTime;
    @Bind(R.id.descriptionTextView) TextView tvDescription;
    @Bind(R.id.textAuthor)          TextView tvAuthor;

    private static final int POINT_ID_LOADER = 2;
    private static final String LOG_TAG = DetailPointActivityFragment.class.getSimpleName();

    private DbInstance                  dbInstance  = null;
    private Dao<PointsStore,Long>       pointsStoreDao;
    private PreparedQuery<PointsStore>  preparedQuery;

    private long                        current_id;
    private ArrayList<PicturesStore>    picStore;

    private LinearLayoutManager lLayout;
    RecyclerView rView;
    ImagesRecyclerViewAdapter rcAdapter;

    private Menu mOptionsMenu;
    boolean mTwoPane = false;
    private View viewForSnackBar;
    private ProgressDialog  progressDialog;
    String shareText;

    public DetailPointActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_detail_point, container, false);

        dbInstance = new DbInstance();

        ButterKnife.bind(this, rootView);

        Bundle arguments = getArguments();

        if (arguments != null) {
            current_id = arguments.getLong(MainActivity.KEY_POINT_ID);
        }

        try {
            pointsStoreDao = dbInstance.getDatabaseHelper().getPointsDao();

            QueryBuilder<PointsStore, Long> queryBuilder = pointsStoreDao.queryBuilder();
            Where<PointsStore, Long> where = queryBuilder.where();
            SelectArg selectArg = new SelectArg();
            selectArg.setValue(current_id);

            SelectArg selectArgDeleted =new SelectArg();
            selectArgDeleted.setValue(0);

            where.eq(EnvironmentMonitorContract.Points.LOCAL_ID, selectArg)
                    .and()
                    .eq(EnvironmentMonitorContract.Points.IS_DELETED, selectArgDeleted);

            preparedQuery = where.prepare();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        viewForSnackBar = rootView.findViewById(R.id.id_linear_layout);

        picStore = new ArrayList<PicturesStore>();

        lLayout = new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL,false);

        rView = (RecyclerView)rootView.findViewById(R.id.id_imageViewer);
        rView.setHasFixedSize(true);
        rView.setLayoutManager(lLayout);

        View mEmptyView = (View)rootView.findViewById(R.id.id_no_images_caption);

        rcAdapter = new ImagesRecyclerViewAdapter(getActivity(), picStore,
                mEmptyView,
                new ImagesRecyclerViewAdapter.ImagesAdapterOnClickHandler() {

                    @Override
                    public void onClick(int position, ImagesRecyclerViewAdapter.ImageViewHolders vh) {
                        ArrayList<String> picList = new ArrayList<String>();
                        for (PicturesStore ps : picStore) {
                            if (ps.getPhoto_was_uploaded() == 0) {
                                picList.add(Uri.fromFile(new File(ps.getLocal_photo_path())).toString());
                            } else {
                                picList.add(ps.getFull_photo_url());
                            }
                        }

                        final Intent i = new Intent(getActivity(), ImageDetailActivity.class);
                        i.putExtra(ImageDetailActivity.EXTRA_IMAGE, position);
                        i.putStringArrayListExtra(ImageDetailActivity.IMAGES_ARRAY, picList);

                        if (Utility.hasJellyBean()) {
                            ActivityOptions options = ActivityOptions.makeScaleUpAnimation(vh.countryPhoto, 0, 0, vh.countryPhoto.getWidth(), vh.countryPhoto.getHeight());
                            getActivity().startActivity(i, options.toBundle());
                        } else {
                            startActivity(i);
                        }
                    }
                },
                new ImagesRecyclerViewAdapter.ImagesAdapterOnMenuItemClickListenerHandler() {
                    @Override
                    public void onMenuItemClick(int position, MenuItem item) {
                        Log.d(LOG_TAG, "on MenuItem click");
                    }
                },
                new ImagesRecyclerViewAdapter.ImagesAdapterOnCreateContextMenuHandler() {
                    @Override
                    public void onCreateContextMenu(int position, ImagesRecyclerViewAdapter.ImageViewHolders vh, ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                        Log.d(LOG_TAG, "on MenuItem created");
                    }
                }
        );

        rView.setAdapter(rcAdapter);

        android.support.v4.content.Loader<Object> loader = getLoaderManager().getLoader(POINT_ID_LOADER);

        if (loader != null && !loader.isReset()) {
            getLoaderManager().restartLoader(POINT_ID_LOADER, null, this);
        } else {
            getLoaderManager().initLoader(POINT_ID_LOADER, null, this);
        }

        if(getActivity().findViewById(R.id.fragment_detail_container_tablet) != null){
            mTwoPane = true;
        }

        return rootView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    private void finishCreatingMenu(Menu menu) {
        // Retrieve the share menu item
        MenuItem menuItem = menu.findItem(R.id.action_share);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_detail_point, menu);
        mOptionsMenu = menu;

        finishCreatingMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_edit) {

            if(IsOwner()) {
                if (mTwoPane) {

                    Bundle arguments = new Bundle();
                    arguments.putLong(MainActivity.KEY_POINT_ID, current_id);

                    DetailPointActivityFragment fragment = new DetailPointActivityFragment();
                    fragment.setArguments(arguments);

                    getActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_detail_container_tablet, fragment)
                            .commit();

                } else {

                    //getActivity().finish();

                    Intent intent = new Intent(getActivity(), EditPointActivity.class);
                    intent.putExtra(MainActivity.KEY_POINT_ID, current_id);

                    ActivityCompat.startActivity(getActivity(), intent, null);
                }
            }else{
                Snackbar.make(viewForSnackBar, R.string.not_item_owner_warning, Snackbar.LENGTH_LONG).show();
            }

            return true;
        }else if(id == R.id.action_delete){
            if(IsOwner()) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

                builder.setMessage(R.string.dialog_message_delete_point).setTitle(R.string.dialog_delete_title);

                builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        RuntimeExceptionDao<PointsStore, Long> daoPoint = dbInstance.getDatabaseHelper().getPointsDataDao();
                        PointsStore deletingPoint = daoPoint.queryForId(current_id);

                        ShowProgress(true);

                        if(Utility.isNetworkAvailable(getActivity())) {
                            new DeletePointTask(deletingPoint).execute();
                        }else{
                            ShowProgress(false);
                            Toast.makeText(getActivity(), "No internet connection available, data cannot be deleted.", Toast.LENGTH_SHORT).show();
                            getActivity().getContentResolver().insert(EnvironmentMonitorContract.POINTS_CONTENT_URI, null);
                        }
                    }
                });

                builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                    }
                });

                AlertDialog dialog = builder.create();
                dialog.show();
            }else{
                Snackbar.make(viewForSnackBar, R.string.not_item_owner_warning, Snackbar.LENGTH_LONG).show();
            }
            return true;
        }else if(id == R.id.action_show_on_map){
            return true;
        }else if(id == R.id.action_share){

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
            startActivity(shareIntent);

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private boolean IsOwner() {

        AccountsStore curUser = AccountsStore.getActiveUser();

        RuntimeExceptionDao<PointsStore, Long> daoPoint = dbInstance.getDatabaseHelper().getPointsDataDao();
        PointsStore deletingPoint = daoPoint.queryForId(current_id);

        if(deletingPoint.getUser_email().equals(curUser.getEmail())){
            return true;
        }else {
            return false;
        }

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Log.d(LOG_TAG, "Create loader");
        return new OrmLiteCursorLoader(getActivity(), pointsStoreDao, preparedQuery);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        if (data != null && data.getCount() > 0) {

            tvHeadLine.setText(data.getString(data.getColumnIndex(EnvironmentMonitorContract.Points.HEADLINE)));

            shareText = tvHeadLine.getText().toString();

            tvDescription.setText(data.getString(data.getColumnIndex(EnvironmentMonitorContract.Points.FULL_DESCRIPTION)));
            //tvDateTime.setText(Utility.MillSecToString(data.getLong(data.getColumnIndex(EnvironmentMonitorContract.Points.CREATED_AT))));
            tvDateTime.setText(Utility.getFriendlyDayString(getActivity(), data.getLong(data.getColumnIndex(EnvironmentMonitorContract.Points.CREATED_AT)), false));

            tvAuthor.setText(data.getString(data.getColumnIndex(EnvironmentMonitorContract.Points.FIRST_NAME)) + " "
                    + data.getString(data.getColumnIndex(EnvironmentMonitorContract.Points.LAST_NAME)));

            picStore.clear();
            RuntimeExceptionDao<PicturesStore, Long> simpleDao = dbInstance.getDatabaseHelper().getPicturesDataDao();

            QueryBuilder<PicturesStore, Long> qb = simpleDao.queryBuilder();
            try {

                qb.where().eq(PicturesStore.POINT_ID, data.getLong(data.getColumnIndex(EnvironmentMonitorContract.Points.LOCAL_ID)))
                        .and()
                        .eq(PicturesStore.IS_DELETED,0);
                List<PicturesStore> list = simpleDao.query(qb.prepare());

                for (PicturesStore pic : list) {
                    picStore.add(pic);
                }

                rcAdapter.ChangeItemsSource(picStore);
                rcAdapter.notifyDataSetChanged();

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }


    public class DeletePointTask extends AsyncTask<Void, Void, Intent> {

        private PointsStore   deletingPoint;
        private RuntimeExceptionDao<PointsStore, Long> daoPoint;

        private Retrofit retrofit = null;
        private EnvironmentService service;

        DeletePointTask(PointsStore deletingPoint) {
            this.deletingPoint     = deletingPoint;

            retrofit = new Retrofit.Builder()
                    .baseUrl(Utility.BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            daoPoint    = dbInstance.getDatabaseHelper().getPointsDataDao();

            service = retrofit.create(EnvironmentService.class);
        }

        @Override
        protected Intent doInBackground(Void... params) {

            Log.d(LOG_TAG, "Delete point");

            AccountsStore acc = AccountsStore.getActiveUser();

            Intent res = NetworkServiceUtility.SendDeletePointToServer(deletingPoint, service, "Bearer " + acc.getMy_server_access_token(), dbInstance);
            return res;
        }

        @Override
        protected void onPostExecute(Intent intent) {

            ShowProgress(false);

            if (intent.hasExtra(Utility.KEY_ERROR_MESSAGE_ELEMENTS)) {

                String jsonStr_error = intent.getStringExtra(Utility.KEY_ERROR_MESSAGE_ELEMENTS);

                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

                builder.setMessage(jsonStr_error).setTitle(R.string.message_title_error);

                builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        finishSubmitElement(false);
                    }
                });

                AlertDialog dialog = builder.create();
                dialog.show();

            } else {
                finishSubmitElement(true);
            }
        }

        @Override
        protected void onCancelled() {
            ShowProgress(false);
        }

    }

    private void finishSubmitElement(boolean b) {

        if (b) {
            getActivity().getContentResolver().insert(EnvironmentMonitorContract.POINTS_CONTENT_URI, null);

            if (MainActivity.class.isInstance(getActivity())) {
                ((PointsListFragment.Callback) getActivity()).onItemSelected((long) -1, null);
            } else {
                getActivity().finish();
            }
        }

    }

    private void ShowProgress(boolean b) {
        if(b){
            if(progressDialog == null){
                progressDialog = new ProgressDialog(getActivity(), R.style.AppTheme);

                progressDialog.setIndeterminate(true);
                progressDialog.setMessage(getString(R.string.deleting_data_from_server));
                progressDialog.setCancelable(false);
            }
            progressDialog.show();
        }else{
            if(progressDialog != null){
                progressDialog.hide();
            }
        }
    }

}