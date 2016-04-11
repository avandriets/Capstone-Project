package com.digitallifelab.environmentmonitor;

import android.app.ActivityOptions;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
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
import android.widget.Button;
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
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static com.digitallifelab.environmentmonitor.Utils.ImageUtility.addImageToGallery;

/**
 * A placeholder fragment containing a simple view.
 */
public class EditPointActivityFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>{

    private static final String LOG_TAG = NewPointActivityFragment.class.getSimpleName();
    private static final int POINT_ID_LOADER = 2;

    public static final int CAPTURE_IMAGE_THUMBNAIL_ACTIVITY_REQUEST_CODE = 1888;
    private static final int SELECT_FILE = 1890;

    @Bind(R.id.headlineTextView)    TextView tvHeadLine;
    @Bind(R.id.descriptionTextView) TextView tvDescription;
    @Bind(R.id.dateTextView)        TextView tvDateTime;
    @Bind(R.id.textAuthor)          TextView tvAuthor;

    private DbInstance dbInstance  = null;

    private LinearLayoutManager lLayout;
    RecyclerView rView;
    ImagesRecyclerViewAdapter rcAdapter;

    Date currentDate;

    private String mImageFileName;
    private ArrayList<PicturesStore>    picStore;
    private ArrayList<PhotoStorage>     mArrayOfOriginalImages;

    boolean twoPane = false;

    private View viewForSnackBar;

    private long                        current_id;
    private Dao<PointsStore,Long>       pointsStoreDao;
    private PreparedQuery<PointsStore>  preparedQuery;
    private ProgressDialog progressDialog;

    public EditPointActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_edit_point, container, false);

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

        mArrayOfOriginalImages = new ArrayList<>();
        picStore = new ArrayList<PicturesStore>();
        currentDate = new Date();

        if(savedInstanceState != null) {

            if(savedInstanceState.containsKey(Utility.FILE_NAME_KEY))
            {
                mImageFileName = savedInstanceState.getString(Utility.FILE_NAME_KEY);
            }

            if(savedInstanceState.containsKey(Utility.IMAGES_ORIGINAL_ARRAY_KEY))
            {
                mArrayOfOriginalImages  =  savedInstanceState.getParcelableArrayList(Utility.IMAGES_ORIGINAL_ARRAY_KEY);

                if( mArrayOfOriginalImages != null) {

                    RuntimeExceptionDao<PicturesStore, Long>    daoPicture  = dbInstance.getDatabaseHelper().getPicturesDataDao();
                    for (int i = 0; i < mArrayOfOriginalImages.size(); i++) {

                        PhotoStorage originalFileName = mArrayOfOriginalImages.get(i);

                        if(originalFileName.newPhoto) {
                            picStore.add(new PicturesStore(null, null, currentDate.getTime(), currentDate.getTime(), 0, originalFileName.photoPath));
                        }else{
                            PicturesStore pic = daoPicture.queryForId(originalFileName.id);
                            picStore.add(pic);
                        }
                    }
                }
            }
        }

        tvDateTime.setText(Utility.MillSecToString(currentDate.getTime()));

        lLayout = new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false);

        rView = (RecyclerView) rootView.findViewById(R.id.id_imageViewer);
        rView.setHasFixedSize(true);
        rView.setLayoutManager(lLayout);

        View mEmptyView = (View) rootView.findViewById(R.id.id_no_images_caption);

        rcAdapter = new ImagesRecyclerViewAdapter(getActivity(), picStore,
                mEmptyView, new ImagesRecyclerViewAdapter.ImagesAdapterOnClickHandler() {
            @Override
            public void onClick(int position, ImagesRecyclerViewAdapter.ImageViewHolders vh) {
                ArrayList<String> picList = new ArrayList<String>();

                for (PicturesStore ps : picStore) {
                    if (ps.getPhoto_was_uploaded() == 0) {
                        //Uri uri = Uri.fromFile(new File(ps.getLocal_photo_path()));
                        Uri uri = Uri.fromFile(new File(ps.getLocal_photo_path()));
                        picList.add(uri.toString());
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
        }, new ImagesRecyclerViewAdapter.ImagesAdapterOnMenuItemClickListenerHandler() {
            @Override
            public void onMenuItemClick(final int position, MenuItem item) {
                Log.d(LOG_TAG, "on MenuItem click");

                final PicturesStore deletingPic = picStore.get(position);

                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

                builder.setMessage(R.string.dialog_message_delete).setTitle(R.string.dialog_delete_title);

                builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        if(deletingPic.getPhoto_was_uploaded() == 0) {
                            String str = deletingPic.getLocal_photo_path();
                            str = str.substring(7, str.length());
                            ImageUtility.DeleteImageFromGalleryBase(getActivity(), str);
                            File f_delete = new File(str);
                            if (f_delete.exists()) {
                                if (f_delete.delete()) {
                                    Log.d(LOG_TAG, "file Deleted :" + deletingPic.getLocal_photo_path());
                                } else {
                                    Log.e(LOG_TAG, "file not Deleted :" + deletingPic.getLocal_photo_path());
                                }
                            }
                        }

                        mArrayOfOriginalImages.remove(position);

                        PicturesStore picture = picStore.get(position);
                        RuntimeExceptionDao<PicturesStore, Long>    daoPicture  = dbInstance.getDatabaseHelper().getPicturesDataDao();

                        if(picture.getPhoto_was_uploaded() == 1 ){
                            picture.setIs_deleted(1);
                            picture.setPhoto_was_uploaded(0);

                            daoPicture.update(picture);

                        }else if(picture.getPoint() != null && picture.getPhoto_was_uploaded() == 0){

                            int deleteResult = daoPicture.delete(picture);
                            Log.d(LOG_TAG, "Picture was deleted : " + String.valueOf(deleteResult) );
                        }


                        picStore.remove(position);
                        rcAdapter.notifyDataSetChanged();
                    }
                });

                builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                    }
                });

                AlertDialog dialog = builder.create();
                dialog.show();
            }
        }, new ImagesRecyclerViewAdapter.ImagesAdapterOnCreateContextMenuHandler() {
            @Override
            public void onCreateContextMenu(int position, ImagesRecyclerViewAdapter.ImageViewHolders vh, ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {

                MenuItem myActionItem = menu.add(R.string.delete_item_label);
                myActionItem.setOnMenuItemClickListener(vh);
            }
        }
        );

        rView.setAdapter(rcAdapter);

        if(getActivity().findViewById(R.id.fragment_detail_container_tablet) != null){
            twoPane = true;
        }

        Button buttonTakePhoto = (Button) rootView.findViewById(R.id.buttonTakePhoto);
        buttonTakePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Ask4TakePhoto();
            }
        });

        FloatingActionButton fab = (FloatingActionButton) rootView.findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Ask4TakePhoto();
            }
        });

        viewForSnackBar = rootView.findViewById(R.id.id_new_point_layout);

        if(twoPane) {
            fab.setVisibility(View.GONE);
            buttonTakePhoto.setVisibility(View.VISIBLE);
        }

        if(savedInstanceState == null)
        {
            android.support.v4.content.Loader<Object> loader = getLoaderManager().getLoader(POINT_ID_LOADER);

            if (loader != null && !loader.isReset()) {
                getLoaderManager().restartLoader(POINT_ID_LOADER, null, this);
            } else {
                getLoaderManager().initLoader(POINT_ID_LOADER, null, this);
            }
        }

        return rootView;
    }

    private void Ask4TakePhoto(){

        boolean haveAllPermissions = false;

        for ( int i = 0; i < Utility.my_list_permissions.length; i++) {

            int resultCode = -1;

            switch(i){
                case 0:
                    resultCode = Utility.PERMISSION_WRITE_EXTERNAL_STORAGE;
                    break;
                case 1:
                    resultCode = Utility.PERMISSION_READ_EXTERNAL_STORAGE;
                    break;
            }

            String strPermission = Utility.my_list_permissions[i];

            if (!Utility.hasPermission(strPermission, getActivity()) ) {
                if(Utility.shouldWeAsk(strPermission, getActivity()))
                {
                    if (twoPane) {
                        getActivity().requestPermissions(Utility.my_list_permissions, resultCode);
                    } else {
                        requestPermissions(Utility.my_list_permissions, resultCode);
                    }
                }
                Utility.markAsAsked(strPermission, getActivity());
            }
            else{

                if(i == Utility.my_list_permissions.length -1){
                    haveAllPermissions = true;
                }

            }
        }

        if(haveAllPermissions){
            TakePhoto();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == Utility.PERMISSION_WRITE_EXTERNAL_STORAGE) {
            Utility.makePostRequestSnack(getActivity(), viewForSnackBar, Utility.my_list_permissions[0]);
        } else if(requestCode == Utility.PERMISSION_READ_EXTERNAL_STORAGE) {
            Utility.makePostRequestSnack(getActivity(), viewForSnackBar, Utility.my_list_permissions[1]);
        }

        //TODO bag in Android 6.0 system don't give permission for application
        if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            android.os.Process.killProcess(android.os.Process.myPid());
        }

    }

    private void TakePhoto() {

        final CharSequence[] items = {getActivity().getString(R.string.take_photo_caption), getActivity().getString(R.string.chose_from_library_caption)};
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getActivity().getString(R.string.add_photo_dialog_caption));
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {

                if (items[item].equals(getActivity().getString(R.string.take_photo_caption))) {

                    Log.d(LOG_TAG, "Start IMAGE capture intent");
                    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {

                        Log.d(LOG_TAG, "Create the File where the photo should go");
                        File photoFile = null;
                        try {
                            photoFile = Utility.createImageFile(getActivity(), "original_" + System.currentTimeMillis());
                            mImageFileName = photoFile.getAbsoluteFile().toString();

                            // Continue only if the File was successfully created
                            if (photoFile != null) {
                                Log.d(LOG_TAG, "Start IMAGE intent");
                                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                                startActivityForResult(takePictureIntent, CAPTURE_IMAGE_THUMBNAIL_ACTIVITY_REQUEST_CODE);
                            }
                        } catch (IOException ex) {
                            Log.e(LOG_TAG, "Error occurred while creating the File :" + ex.getMessage());
                            Toast.makeText(getActivity(), ex.toString(), Toast.LENGTH_SHORT).show();
                        }
                    }

                } else if (items[item].equals(getActivity().getString(R.string.chose_from_library_caption))) {

                    Intent intent = new Intent(Intent.ACTION_PICK,
                            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    intent.setType("image/*");

                    startActivityForResult(
                            Intent.createChooser(intent, getActivity().getString(R.string.select_file_caption)),
                            SELECT_FILE);

                }
            }
        });

        builder.setNegativeButton(R.string.cancel_dialog_button, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
            }
        });

        builder.show();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_edit_point, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_save_point) {

            PointsStore editedPoint = Savepoint();

            if(Utility.isNetworkAvailable(getActivity())) {
                new PutElementTask(editedPoint).execute();
            }else{
                ShowProgress(false);

                Toast.makeText(getActivity(), R.string.error_no_internet_connection, Toast.LENGTH_SHORT).show();
                getActivity().getContentResolver().insert(EnvironmentMonitorContract.POINTS_CONTENT_URI, null);

                if(MainActivity.class.isInstance(getActivity())){
                    ((PointsListFragment.Callback)getActivity()).onItemSelected(editedPoint.getLocal_id(), null);
                }else{
                    getActivity().finish();
                }
            }

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == this.getActivity().RESULT_OK) {

            if (requestCode == CAPTURE_IMAGE_THUMBNAIL_ACTIVITY_REQUEST_CODE) {

                File newFullFile = ImageUtility.ReduceImageFileSize(mImageFileName, getActivity());
                if (newFullFile != null) {

                    mArrayOfOriginalImages.add(new PhotoStorage(true, newFullFile.getAbsolutePath(), 0));

                    picStore.add(new PicturesStore(null,
                                    null,
                                    currentDate.getTime(),
                                    currentDate.getTime(),
                                    0,
                                    newFullFile.getAbsolutePath())
                    );

                    Uri addedFile = addImageToGallery(newFullFile.getAbsolutePath(), getActivity());
                }

                File file = new File(mImageFileName);
                file.delete();

            } else if (requestCode == SELECT_FILE) {

                Uri selectedImageUri    = data.getData();
                String chosenFilePath   = ImageUtility.getRealPathFromURI(selectedImageUri, getActivity().getContentResolver());

                File newFullFile = ImageUtility.ReduceImageFileSize(chosenFilePath, getActivity());

                if (newFullFile != null) {

                    mArrayOfOriginalImages.add(new PhotoStorage(true, newFullFile.getAbsolutePath(), 0));

                    picStore.add(new PicturesStore(null,
                                    null,
                                    currentDate.getTime(),
                                    currentDate.getTime(),
                                    0,
                                    newFullFile.getAbsolutePath())
                    );

                    Uri addedFile = addImageToGallery(newFullFile.getAbsolutePath(), getActivity());
                    rcAdapter.notifyDataSetChanged();
                }

            }
        }
    }

    private PointsStore Savepoint() {

        RuntimeExceptionDao<PointsStore, Long>      daoPoint    = dbInstance.getDatabaseHelper().getPointsDataDao();
        RuntimeExceptionDao<PicturesStore, Long>    daoPicture  = dbInstance.getDatabaseHelper().getPicturesDataDao();

        PointsStore currentPoint = daoPoint.queryForId(current_id);

        long curTime = currentDate.getTime();

        currentPoint.setHeadline(tvHeadLine.getText().toString());
        currentPoint.setFull_description(tvDescription.getText().toString());
        currentPoint.setUpdated_at(currentDate.getTime());
        currentPoint.setIs_changed(1);
        currentPoint.setPhoto_was_uploaded(0);

        daoPoint.update(currentPoint);

        for (PicturesStore ps : picStore) {
            Log.d(LOG_TAG, "New point picture add");

            if(ps.getId() == 0) {
                ps.setPhoto_was_uploaded(0);
                ps.setPoint(currentPoint);
                ps.setCreated_at(curTime);
                ps.setUpdated_at(curTime);

                daoPicture.create(ps);
            }
        }

        return currentPoint;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(Utility.FILE_NAME_KEY, mImageFileName);

        if(mArrayOfOriginalImages.size() > 0) {
            outState.putParcelableArrayList(Utility.IMAGES_ORIGINAL_ARRAY_KEY, mArrayOfOriginalImages);
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
            tvDescription.setText(data.getString(data.getColumnIndex(EnvironmentMonitorContract.Points.FULL_DESCRIPTION)));
            tvDateTime.setText(Utility.getFriendlyDayString(getActivity(), data.getLong(data.getColumnIndex(EnvironmentMonitorContract.Points.CREATED_AT)), false));

            tvAuthor.setText(data.getString(data.getColumnIndex(EnvironmentMonitorContract.Points.FIRST_NAME)) + " "
            + data.getString(data.getColumnIndex(EnvironmentMonitorContract.Points.LAST_NAME)));

            picStore.clear();
            RuntimeExceptionDao<PicturesStore, Long> simpleDao = dbInstance.getDatabaseHelper().getPicturesDataDao();

            QueryBuilder<PicturesStore, Long> qb = simpleDao.queryBuilder();
            try {

                qb.where().eq(PicturesStore.POINT_ID, data.getLong(data.getColumnIndex(EnvironmentMonitorContract.Points.LOCAL_ID)))
                .and().eq(PicturesStore.IS_DELETED,0);
                List<PicturesStore> list = simpleDao.query(qb.prepare());

                for (PicturesStore pic : list) {

                    String pathToFile;
                    if(pic.getPhoto_was_uploaded() == 0) {
                        pathToFile = pic.getLocal_photo_path();
                        //mArrayOfOriginalImages.add(new PhotoStorage(false, pic.getLocal_photo_path()));
                    }else{
                        pathToFile = pic.getFull_photo_url();
                        //mArrayOfOriginalImages.add(new PhotoStorage(false, pic.getFull_photo_url()));
                    }

                    mArrayOfOriginalImages.add(new PhotoStorage(false, pathToFile, pic.getId()));
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

    private void ShowProgress(boolean b) {

        if(b){
            if(progressDialog == null){
                progressDialog = new ProgressDialog(getActivity(), R.style.AppTheme);

                progressDialog.setIndeterminate(true);
                progressDialog.setMessage(getString(R.string.save_element));
                progressDialog.setCancelable(false);
            }
            progressDialog.show();
        }else{
            if(progressDialog != null){
                progressDialog.hide();
            }
        }
    }

    public class PutElementTask extends AsyncTask<Void, Void, Intent> {

        private PointsStore   postedPoint;

        private RuntimeExceptionDao<PointsStore, Long>      daoPoint;
        private RuntimeExceptionDao<PicturesStore, Long>    daoPictures;

        private Retrofit retrofit = null;
        private EnvironmentService service;

        PutElementTask(PointsStore postedPoint) {
            this.postedPoint    = postedPoint;

            retrofit = new Retrofit.Builder()
                    .baseUrl(Utility.BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            daoPoint    = dbInstance.getDatabaseHelper().getPointsDataDao();
            daoPictures = dbInstance.getDatabaseHelper().getPicturesDataDao();

            service = retrofit.create(EnvironmentService.class);
        }

        @Override
        protected Intent doInBackground(Void... params) {

            Log.d(LOG_TAG, "Async task send element to server");

            AccountsStore acc = AccountsStore.getActiveUser();

            Intent res = NetworkServiceUtility.SendNewPointToServer(postedPoint,service, "Bearer " + acc.getMy_server_access_token(), dbInstance);
            return res;
        }

        @Override
        protected void onPostExecute(final Intent intent) {

            ShowProgress(false);

            if (intent.hasExtra(Utility.KEY_ERROR_MESSAGE_ELEMENTS)) {

                String jsonStr_error = intent.getStringExtra(Utility.KEY_ERROR_MESSAGE_ELEMENTS);

                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

                builder.setMessage(jsonStr_error).setTitle(R.string.message_title_error);

                builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        finishSubmitElement(postedPoint);
                    }
                });

                AlertDialog dialog = builder.create();
                dialog.show();

            } else {
                finishSubmitElement(postedPoint);
            }
        }

        @Override
        protected void onCancelled() {
            ShowProgress(false);
        }

    }

    private void finishSubmitElement(PointsStore receivedPoint) {

        getActivity().getContentResolver().insert(EnvironmentMonitorContract.POINTS_CONTENT_URI, null);
        if(MainActivity.class.isInstance(getActivity())){
            ((PointsListFragment.Callback)getActivity()).onItemSelected(receivedPoint.getLocal_id(), null);
        }else{
            getActivity().finish();
        }

    }

}
