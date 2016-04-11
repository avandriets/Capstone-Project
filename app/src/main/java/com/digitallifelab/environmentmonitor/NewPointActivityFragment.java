package com.digitallifelab.environmentmonitor;

import android.app.ActivityOptions;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
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
import com.digitallifelab.environmentmonitor.Data.PicturesStore;
import com.digitallifelab.environmentmonitor.Data.PointsStore;
import com.digitallifelab.environmentmonitor.Utils.DbInstance;
import com.digitallifelab.environmentmonitor.Utils.ImageUtility;
import com.digitallifelab.environmentmonitor.Utils.MyGoogleApiClient_Singleton;
import com.digitallifelab.environmentmonitor.Utils.NetworkServiceUtility;
import com.digitallifelab.environmentmonitor.Utils.Utility;
import com.digitallifelab.environmentmonitor.adapters.ImagesRecyclerViewAdapter;
import com.j256.ormlite.dao.RuntimeExceptionDao;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
public class NewPointActivityFragment extends Fragment {

    private static final String LOG_TAG = NewPointActivityFragment.class.getSimpleName();

    public static final int CAPTURE_IMAGE_THUMBNAIL_ACTIVITY_REQUEST_CODE = 1888;
    private static final int SELECT_FILE = 1890;

    @Bind(R.id.headlineTextView)    TextView tvHeadLine;
    @Bind(R.id.descriptionTextView) TextView tvDescription;
    @Bind(R.id.dateTextView)        TextView tvDateTime;
    @Bind(R.id.textAuthor)          TextView tvAuthor;

    private DbInstance                  dbInstance  = null;

    private LinearLayoutManager lLayout;
    RecyclerView rView;
    ImagesRecyclerViewAdapter rcAdapter;

    Date currentDate;

    private String mImageFileName;
    private ArrayList<PicturesStore>    picStore;
    private ArrayList<String> mArrayOfOriginalImages;

    boolean twoPane = false;

    private View            viewForSnackBar;
    private ProgressDialog  progressDialog;

    public NewPointActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_new_point, container, false);

        dbInstance = new DbInstance();

        ButterKnife.bind(this, rootView);

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
                mArrayOfOriginalImages  =  savedInstanceState.getStringArrayList(Utility.IMAGES_ORIGINAL_ARRAY_KEY);

                if( mArrayOfOriginalImages != null) {
                    for (int i = 0; i < mArrayOfOriginalImages.size(); i++) {

                        String originalFileName = mArrayOfOriginalImages.get(i);

                        picStore.add(new PicturesStore(null, null, currentDate.getTime(), currentDate.getTime(), 0 , originalFileName));
                    }
                }
            }
        }

        tvDateTime.setText(Utility.getFriendlyDayString(getActivity(), currentDate.getTime(), false));
        tvAuthor.setText(R.string.me_label);

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
                        Uri uri = Uri.fromFile(new File(ps.getLocal_photo_path()));
                        picList.add(uri.toString());
                        //picList.add(ps.getLocal_photo_path());
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

                // 1. Instantiate an AlertDialog.Builder with its constructor
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

                builder.setMessage(R.string.dialog_message_delete).setTitle(R.string.dialog_delete_title);

                builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        String str = deletingPic.getLocal_photo_path();
                        str = str.substring(7,str.length());
                        ImageUtility.DeleteImageFromGalleryBase(getActivity(), str);
                        File f_delete = new File(str);
                        if (f_delete.exists()) {
                            if (f_delete.delete()) {
                                Log.d(LOG_TAG,"file Deleted :" + deletingPic.getLocal_photo_path());
                            } else {
                                Log.e(LOG_TAG, "file not Deleted :" + deletingPic.getLocal_photo_path());
                            }
                        }

                        mArrayOfOriginalImages.remove(position);
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
                                //takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoFile.getAbsolutePath());
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
        inflater.inflate(R.menu.menu_new_point, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_save_new_point) {

            ShowProgress(true);
            final PointsStore newPoint = Savepoint();

            if(Utility.isNetworkAvailable(getActivity())) {
                new PostNewElementTask(newPoint).execute();
            }else{
                ShowProgress(false);

                Toast.makeText(getActivity(), R.string.error_no_internet_connection, Toast.LENGTH_SHORT).show();
                getActivity().getContentResolver().insert(EnvironmentMonitorContract.POINTS_CONTENT_URI, null);

                if(MainActivity.class.isInstance(getActivity())){
                    ((PointsListFragment.Callback)getActivity()).onItemSelected(newPoint.getLocal_id(), null);
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

                    //mArrayOfOriginalImages.add(Uri.fromFile(newFullFile).toString());
                    mArrayOfOriginalImages.add(newFullFile.getAbsolutePath());

                    picStore.add(new PicturesStore(null,
                                    null,
                                    currentDate.getTime(),
                                    currentDate.getTime(),
                                    0,
                                    //Uri.fromFile(newFullFile).toString()
                                    newFullFile.getAbsolutePath()
                            )
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

                    //mArrayOfOriginalImages.add(Uri.fromFile(newFullFile).toString());
                    mArrayOfOriginalImages.add(newFullFile.getAbsolutePath());

                    picStore.add(new PicturesStore(null,
                                    null,
                                    currentDate.getTime(),
                                    currentDate.getTime(),
                                    0,
                                    newFullFile.getAbsolutePath()
                                    //Uri.fromFile(newFullFile).toString()
                            )
                    );

                    Uri addedFile = addImageToGallery(newFullFile.getAbsolutePath(), getActivity());
                    rcAdapter.notifyDataSetChanged();
                }

            }
        }
    }

    private PointsStore Savepoint() {

        AccountsStore curUser = AccountsStore.getActiveUser();

        RuntimeExceptionDao<PointsStore, Long>      daoPoint    = dbInstance.getDatabaseHelper().getPointsDataDao();
        RuntimeExceptionDao<PicturesStore, Long>    daoPicture  = dbInstance.getDatabaseHelper().getPicturesDataDao();


        Location mLastLocation = MyGoogleApiClient_Singleton.getmLastLocation();

        double latitude = 0;
        double longitude = 0;
        if (mLastLocation != null) {
            latitude = mLastLocation.getLatitude();
            longitude = mLastLocation.getLongitude();
            Log.d(LOG_TAG, "Latitude " + String.valueOf(mLastLocation.getLatitude()));
            Log.d(LOG_TAG, "Longitude " + String.valueOf(mLastLocation.getLongitude()));
        }

        long curTime = currentDate.getTime();

        PointsStore newPoint = new PointsStore(tvHeadLine.getText().toString(), tvDescription.getText().toString(),
                longitude,latitude,0
                ,curTime
                ,curTime
                ,curUser.getUser_name()
                ,curUser.getFirst_name()
                ,curUser.getLast_name()
                ,curUser.getEmail()
        );

        daoPoint.create(newPoint);
        newPoint.setServer_id(newPoint.getLocal_id());
        daoPoint.update(newPoint);


        for (PicturesStore ps : picStore) {
            Log.d(LOG_TAG, "New point picture add");

            ps.setPoint(newPoint);
            ps.setCreated_at(curTime);
            ps.setUpdated_at(curTime);

            daoPicture.create(ps);
        }

        return newPoint;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(Utility.FILE_NAME_KEY, mImageFileName);

        if(mArrayOfOriginalImages.size() > 0) {
            outState.putStringArrayList(Utility.IMAGES_ORIGINAL_ARRAY_KEY, mArrayOfOriginalImages);
        }
    }


    public class PostNewElementTask extends AsyncTask<Void, Void, Intent> {

        private PointsStore   postedPoint;

        private Retrofit retrofit = null;
        private EnvironmentService service;

        PostNewElementTask(PointsStore postedPoint) {
            this.postedPoint    = postedPoint;

            retrofit = new Retrofit.Builder()
                    .baseUrl(Utility.BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            service = retrofit.create(EnvironmentService.class);
        }

        @Override
        protected Intent doInBackground(Void... params) {

            Log.d(LOG_TAG, "Async task send element to server");

            AccountsStore acc = AccountsStore.getActiveUser();

            Intent res = NetworkServiceUtility.SendNewPointToServer(postedPoint, service, "Bearer " + acc.getMy_server_access_token(), dbInstance);

            return res;
        }

        @Override
        protected void onPostExecute(final Intent intent) {

            ShowProgress(false);
            finishSubmitElement(postedPoint, intent);

        }

        @Override
        protected void onCancelled() {
            ShowProgress(false);
        }

    }

    private void finishSubmitElement(final PointsStore postedPoint, Intent errorIntent) {

        if (errorIntent.hasExtra(Utility.KEY_ERROR_MESSAGE_ELEMENTS) || errorIntent.hasExtra(Utility.KEY_ERROR_UPLOAD_PICTURES)) {

            String jsonStr_error;
            if(errorIntent.hasExtra(Utility.KEY_ERROR_MESSAGE_ELEMENTS)) {
                jsonStr_error = errorIntent.getStringExtra(Utility.KEY_ERROR_MESSAGE_ELEMENTS);
            }else{
                jsonStr_error = errorIntent.getStringExtra(Utility.KEY_ERROR_UPLOAD_PICTURES);
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

            builder.setMessage(jsonStr_error).setTitle(R.string.message_title_error);

            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {

                    getActivity().getContentResolver().insert(EnvironmentMonitorContract.POINTS_CONTENT_URI, null);
                    if(MainActivity.class.isInstance(getActivity())){
                        ((PointsListFragment.Callback)getActivity()).onItemSelected(postedPoint.getLocal_id(), null);
                    }else{
                        getActivity().finish();
                    }

                }
            });

            AlertDialog dialog = builder.create();
            dialog.show();

        } else {

            getActivity().getContentResolver().insert(EnvironmentMonitorContract.POINTS_CONTENT_URI, null);
            if(MainActivity.class.isInstance(getActivity())){
                ((PointsListFragment.Callback)getActivity()).onItemSelected(postedPoint.getLocal_id(), null);
            }else{
                getActivity().finish();
            }
        }
    }

    private void ShowProgress(boolean b) {

        if(b){
            if(progressDialog == null){
                progressDialog = new ProgressDialog(getActivity(), R.style.AppTheme);

                progressDialog.setIndeterminate(true);
                progressDialog.setMessage(getString(R.string.send_data_to_server));
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
