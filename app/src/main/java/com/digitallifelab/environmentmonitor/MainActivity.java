package com.digitallifelab.environmentmonitor;

import android.app.ActivityOptions;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.digitallifelab.environmentmonitor.Data.AccountsStore;
import com.digitallifelab.environmentmonitor.Data.MessagesStore;
import com.digitallifelab.environmentmonitor.Data.PicturesStore;
import com.digitallifelab.environmentmonitor.Data.PointsStore;
import com.digitallifelab.environmentmonitor.Utils.DbInstance;
import com.digitallifelab.environmentmonitor.Utils.MyGoogleApiClient_Singleton;
import com.digitallifelab.environmentmonitor.Utils.OnGetGoogleTokenTaskCompleted;
import com.digitallifelab.environmentmonitor.Utils.Utility;
import com.digitallifelab.environmentmonitor.adapters.PointsRecyclerViewAdapter;
import com.digitallifelab.environmentmonitor.gcm.MyDevice;
import com.digitallifelab.environmentmonitor.gcm.RegistrationIntentService;
import com.digitallifelab.environmentmonitor.sync.EMonitorSyncAdapter;
import com.google.android.gms.auth.GooglePlayServicesAvailabilityException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationServices;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import com.squareup.picasso.Picasso;

import java.sql.SQLException;

import okhttp3.OkHttpClient;


public class MainActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks, OnGetGoogleTokenTaskCompleted, PointsListFragment.Callback{

    private static final String LIST_FRAGMENT_TAG = "list_fragment_tag";
    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    public static final int RC_SIGN_IN_ACTIVITY     = 200;
    private static final String KEY_TOKEN_WS_GET    = "KEY_TOKEN_WAS_GET";
    public static final String KEY_POINT_ID = "point_id";
    public static final String NEW_POINT_FRAGMENT_TAG = "new_point_fragment_tag";
    private static final String LIST_DETAIL_TAG = "detail_tag";
    private static final String MAP_FRAGMENT_TAG = "map_fragment_tag";
    private static final java.lang.String EXCHANGE_TOKEN_WS_GET = "Key_exchange_token_get";

    private boolean mTwoPane;

    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    public static final String SENT_TOKEN_TO_SERVER = "sentTokenToServer";

    private MyGoogleApiClient_Singleton instance    = null;
    private DbInstance                  dbInstance  = null;

    private ProgressDialog      progressDialog;
    private DrawerLayout        mDrawerLayout;

    public static boolean           mGoogleTokenWasGet;
    public static boolean           mExchangeTokenWasGet;

    private ActionBarDrawerToggle   drawerToggle;
    private boolean                 isCurrentViewList = true;
    private NavigationView          navigationView;

    private String units;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        dbInstance = new DbInstance();
        dbInstance.SetDBHelper(this);

        setContentView(R.layout.main_activity);

        Utility.mClientOkHttp = new OkHttpClient();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final ActionBar ab = getSupportActionBar();
        ab.setHomeAsUpIndicator(R.drawable.ic_menu_white_24dp);
        ab.setDisplayHomeAsUpEnabled(true);

        progressDialog = new ProgressDialog(this, R.style.CustomProgress);

        progressDialog.setIndeterminate(true);
        progressDialog.setMessage(getString(R.string.login_label));

        mGoogleTokenWasGet = false;
        if (savedInstanceState != null) {
            mGoogleTokenWasGet = savedInstanceState.getBoolean(KEY_TOKEN_WS_GET);
        }

        mExchangeTokenWasGet = false;
        if (savedInstanceState != null) {
            mExchangeTokenWasGet = savedInstanceState.getBoolean(EXCHANGE_TOKEN_WS_GET);
        }

        if (findViewById(R.id.fragment_detail_container_tablet) != null) {
            mTwoPane = true;
            //TODO make create two fragment panels
            if (savedInstanceState == null)
            {
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_detail_container_tablet, new BlankFragment(), LIST_DETAIL_TAG).commit();
            }
        } else {
            mTwoPane = false;
        }

        SharedPreferences mPreference = PreferenceManager.getDefaultSharedPreferences(this);
        String defaultValue = this.getResources().getString(R.string.default_units_prefs_value);
        units = mPreference.getString("units_prefs", defaultValue);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        if (fab != null) {
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    if (!mTwoPane) {
                        Intent newPointIntent = new Intent(MainActivity.this, NewPointActivity.class);
                        startActivity(newPointIntent);
                    }else{
                        NewPointActivityFragment fragment = new NewPointActivityFragment();

                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.fragment_detail_container_tablet, fragment, NEW_POINT_FRAGMENT_TAG)
                                .commit();
                    }
                }
            });
        }

        EMonitorSyncAdapter.initializeSyncAdapter(this);

        if (checkPlayServices()) {
            // Because this is the initial creation of the app, we'll want to be certain we have
            // a token. If we do not, then we will start the IntentService that will register this
            // application with GCM.
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

            boolean sentToken = sharedPreferences.getBoolean(SENT_TOKEN_TO_SERVER, false);
            if (!sentToken) {
                Intent intent = new Intent(this, RegistrationIntentService.class);
                startService(intent);
            }
        }

        //Init Google Api Client
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                //.requestIdToken(Utility.CLIENT_ID_SERVER)
                .requestEmail()
                .requestScopes(new Scope(Scopes.PLUS_LOGIN), new Scope(Scopes.PLUS_ME))
                .requestServerAuthCode(Utility.Get_Client_ID_Server(this), false)
                .build();

        instance = new MyGoogleApiClient_Singleton();

        if(instance.get_GoogleApiClient() == null){

            GoogleApiClient mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                    .addConnectionCallbacks(this)
                    .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                    .addApi(LocationServices.API)
                    .build();

            instance = MyGoogleApiClient_Singleton.getInstance(mGoogleApiClient);
        }

        AccountsStore acc = AccountsStore.getActiveUser();

        if(acc == null) {
            Intent loginIntent = new Intent(this, LoginActivity.class);
            startActivityForResult(loginIntent, RC_SIGN_IN_ACTIVITY);
        }else{
            if(!mExchangeTokenWasGet && Utility.isNetworkAvailable(this)) {
                GetAccessToken();
            }
        }

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        drawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close);

        mDrawerLayout.setDrawerListener(drawerToggle);

        navigationView = (NavigationView) findViewById(R.id.my_nav_view);
        if (navigationView != null) {
            setupDrawerContent(navigationView, acc);

            if(isCurrentViewList) {
                navigationView.getMenu().getItem(0).setChecked(true);
            }else{
                navigationView.getMenu().getItem(1).setChecked(true);
            }
        }

        if(!Utility.isNetworkAvailable(this)){
            Toast.makeText(this, R.string.error_no_internet_connection, Toast.LENGTH_SHORT).show();
        }

    }

    private void SyncDataImmediately() {
        EMonitorSyncAdapter.syncImmediately(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences mPreference = PreferenceManager.getDefaultSharedPreferences(this);
        String defaultValue = this.getResources().getString(R.string.default_units_prefs_value);
        String newUnits = mPreference.getString("units_prefs", defaultValue);

        MyMapFragment ff = (MyMapFragment)getSupportFragmentManager().findFragmentByTag(MAP_FRAGMENT_TAG);
        if(ff == null) {
            isCurrentViewList = true;
        }

        if(isCurrentViewList) {
            navigationView.getMenu().getItem(0).setChecked(true);
        }else{
            navigationView.getMenu().getItem(1).setChecked(true);
        }

        if(!newUnits.equals(units)){

            PointsListFragment ff1 = (PointsListFragment)getSupportFragmentManager().findFragmentByTag(LIST_FRAGMENT_TAG);
            if(ff1 != null)
            {
                ff1.changeUnits();
            }

        }

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggles
        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

//        if (requestCode == Utility.PERMISSION_WRITE_EXTERNAL_STORAGE) {
//            Utility.makePostRequestSnack(this, findViewById(R.id.id_main_coordinating_layout), Utility.my_list_permissions[0]);
//        } else if(requestCode == Utility.PERMISSION_READ_EXTERNAL_STORAGE) {
//            Utility.makePostRequestSnack(this, findViewById(R.id.id_main_coordinating_layout), Utility.my_list_permissions[1]);
//        }

        if (requestCode == Utility.PERMISSION_WRITE_EXTERNAL_STORAGE) {
            Utility.makePostRequestSnack(this, findViewById(R.id.col), Utility.my_list_permissions[0]);
        } else if(requestCode == Utility.PERMISSION_READ_EXTERNAL_STORAGE) {
            Utility.makePostRequestSnack(this, findViewById(R.id.col), Utility.my_list_permissions[1]);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(KEY_TOKEN_WS_GET, mGoogleTokenWasGet);
        outState.putBoolean(EXCHANGE_TOKEN_WS_GET, mExchangeTokenWasGet);
    }

    private void setupDrawerContent(NavigationView navigationView, AccountsStore acc) {

        if(acc != null) {

            View headerLayout = navigationView.getHeaderView(0);

            ImageView imageView = (ImageView) headerLayout.findViewById(R.id.imageViewCreatorSigned);
            TextView tvUserName = (TextView) headerLayout.findViewById(R.id.UserName);

            tvUserName.setText(acc.getUserDisplayName());

            String full_photo_url = acc.getPhotoUrl();
            if(full_photo_url != null && !full_photo_url.isEmpty())
            Picasso.with(this).load(full_photo_url).placeholder(R.drawable.ic_account_circle_white_48dp).error(R.drawable.ic_error_outline_white_48dp).into(imageView);
        }

        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {

                        int itemId = menuItem.getItemId();

                        //menuItem.setChecked(false);
                        mDrawerLayout.closeDrawers();

                        switch (itemId) {
                            case R.id.nav_logout: {
                                SignOut();
                                return true;
                            }

                            case R.id.nav_points_list_view: {
                                isCurrentViewList = true;
                                //getSupportFragmentManager().beginTransaction().replace(R.id.fragment_list_container, new PointsListFragment(), LIST_FRAGMENT_TAG).commit();
                                MyMapFragment ff = (MyMapFragment)getSupportFragmentManager().findFragmentByTag(MAP_FRAGMENT_TAG);
                                if(ff != null)
                                    getSupportFragmentManager().beginTransaction().remove(ff).commit();

                                return true;
                            }

                            case R.id.nav_map_view: {
                                isCurrentViewList = false;

                                if (!mTwoPane) {
                                    startActivity(new Intent(MainActivity.this, MapActivity.class));
                                    //getSupportFragmentManager().beginTransaction().replace(R.id.fragment_list_container, new MyMapFragment(), MAP_FRAGMENT_TAG).commit();
                                } else {
                                    getSupportFragmentManager().beginTransaction().replace(R.id.fragment_detail_container_tablet, new MyMapFragment(), MAP_FRAGMENT_TAG).commit();
                                }

                                return true;
                            }

                            case R.id.nav_settings: {
                                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                                return true;
                            }

                            case android.R.id.home:
                                mDrawerLayout.openDrawer(GravityCompat.START);
                                return true;
                        }

                        return true;
                    }
                });
    }

    private void SignOut() {

        if(instance.get_GoogleApiClient().isConnected()) {
            Auth.GoogleSignInApi.signOut(instance.get_GoogleApiClient()).setResultCallback(
                    new ResultCallback<Status>() {
                        @Override
                        public void onResult(Status status) {
                            Log.d(LOG_TAG, "Sign out " + status.toString());

                            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);

                            mGoogleTokenWasGet = false;
                            mExchangeTokenWasGet = false;

                            RuntimeExceptionDao<AccountsStore, String>  dao  = dbInstance.getDatabaseHelper().getAccountsDataDao();

                            AccountsStore acc = AccountsStore.getActiveUser();
                            final String token = acc.getMy_server_access_token();

                            if (acc != null) {

                                //TODO unregister when user sign out
                                new Thread(new Runnable() {
                                    public void run() {
                                        String deviceID = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
                                        MyDevice.unregister(deviceID, token);
                                    }
                                }).start();

                                //Clear data
                                ConnectionSource connectionSource = dbInstance.getDatabaseHelper().getConnectionSource();
                                try {
                                    TableUtils.clearTable( connectionSource, PointsStore.class);
                                    TableUtils.clearTable(connectionSource, PicturesStore.class);
                                    TableUtils.clearTable(connectionSource, MessagesStore.class);

                                } catch (SQLException e) {
                                    e.printStackTrace();
                                }

                                sharedPreferences.edit().putBoolean(MainActivity.SENT_TOKEN_TO_SERVER, false).apply();

                                sharedPreferences.edit().putBoolean(Utility.EXCHANGE_TOKEN_WAS_GOT, false).apply();
                                sharedPreferences.edit().putBoolean(Utility.GOOGLE_TOKEN_WAS_GOT, false).apply();

                                acc.setActive(0);
                                dao.update(acc);
                            }

                            Intent loginIntent = new Intent(MainActivity.this, LoginActivity.class);
                            startActivityForResult(loginIntent, RC_SIGN_IN_ACTIVITY);
                        }
                    });
        }

    }

    private boolean checkPlayServices() {

        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);

        if (resultCode != ConnectionResult.SUCCESS) {

            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i(LOG_TAG, "This device is not supported.");
                Toast.makeText(MainActivity.this, R.string.device_not_supported_message, Toast.LENGTH_SHORT).show();
                finish();
            }
            return false;
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        int id = item.getItemId();

        if(id == android.R.id.home){
            mDrawerLayout.openDrawer(GravityCompat.START);
            return true;
        }
        else if(id == R.id.action_refresh_data){
            SyncDataImmediately();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();

        instance.get_GoogleApiClient().connect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.d(LOG_TAG, "On destroy helper && GApiClient.");
//        if (dbInstance.getDatabaseHelper() != null) {
//            OpenHelperManager.releaseHelper();
//
//            dbInstance.releaseHelper();
//        }
//
//        if (instance.get_GoogleApiClient() != null && instance.get_GoogleApiClient().isConnected()) {
//            instance.get_GoogleApiClient().disconnect();
//        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN_ACTIVITY || resultCode == Utility.REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR) {
            if(resultCode == RESULT_OK) {
                GetAccessToken();

                if (checkPlayServices()) {
                    // Because this is the initial creation of the app, we'll want to be certain we have
                    // a token. If we do not, then we will start the IntentService that will register this
                    // application with GCM.
                    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

                    boolean sentToken = sharedPreferences.getBoolean(SENT_TOKEN_TO_SERVER, false);
                    if (!sentToken) {
                        Intent intent = new Intent(this, RegistrationIntentService.class);
                        startService(intent);
                    }
                }
            }
        }
    }

    public void GetAccessToken(){

        AccountsStore activeUser = AccountsStore.getActiveUser();

        if(activeUser != null) {
            if(activeUser.getProvider() == AccountsStore.OAuthProviders.google){

                progressDialog.show();

                new GetUserTokenTask(this, activeUser.getEmail(), Utility.GOOGLE_SCOPE, this).execute();
            }
        }
    }

    public void handleException(final Exception e) {
        // Because this call comes from the AsyncTask, we must ensure that the following
        // code instead executes on the UI thread.
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if(progressDialog.isShowing())
                    progressDialog.dismiss();

                if (e instanceof GooglePlayServicesAvailabilityException) {
                    // The Google Play services APK is old, disabled, or not present.
                    // Show a dialog created by Google Play services that allows
                    // the user to update the APK
                    int statusCode = ((GooglePlayServicesAvailabilityException) e).getConnectionStatusCode();
                    Dialog dialog = GooglePlayServicesUtil.getErrorDialog(statusCode, MainActivity.this, Utility.REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR);
                    dialog.show();
                } else if (e instanceof UserRecoverableAuthException) {
                    // Unable to authenticate, such as when the user has not yet granted
                    // the app access to the account, but the user can fix this.
                    // Forward the user to an activity in Google Play services.
                    Intent intent = ((UserRecoverableAuthException) e).getIntent();
                    startActivityForResult(intent, Utility.REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR);
                }
            }
        });
    }

    @Override
    public void onGetGoogleTokenTaskCompleted(Intent intent) {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        boolean obtainGoogleToken = sharedPreferences.getBoolean(Utility.GOOGLE_TOKEN_WAS_GOT, false);

        if(obtainGoogleToken) {
            mGoogleTokenWasGet = true;
        }

        if(intent.hasExtra(GetUserTokenTask.KEY_ERROR_MESSAGE_GET_TOKEN)){
            Toast.makeText(this, intent.getStringExtra(GetUserTokenTask.KEY_ERROR_MESSAGE_GET_TOKEN), Toast.LENGTH_SHORT).show();
        }else if(!intent.hasExtra(Utility.KEY_ACCESS_TOKEN)){
            Toast.makeText(this, R.string.error_access_message, Toast.LENGTH_SHORT).show();
        }else if(intent.hasExtra(Utility.KEY_ACCESS_TOKEN)){
            mExchangeTokenWasGet = true;
            SyncDataImmediately();
        }

        if(progressDialog.isShowing())
            progressDialog.dismiss();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(LOG_TAG, "Connection failed");

        if (!connectionResult.hasResolution()) {
            GooglePlayServicesUtil.getErrorDialog(connectionResult.getErrorCode(), this,
                    0).show();
            return;
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(LOG_TAG, "onConnected");
        MyGoogleApiClient_Singleton.setmLastLocation(LocationServices.FusedLocationApi.getLastLocation(instance.get_GoogleApiClient()));

        PointsListFragment ff = (PointsListFragment)getSupportFragmentManager().findFragmentByTag(LIST_FRAGMENT_TAG);
        if(ff != null)
        {
            Location loc = MyGoogleApiClient_Singleton.getmLastLocation();
            Log.d(LOG_TAG, "get");
            ff.changeCoordinates(loc.getLatitude(), loc.getLongitude());
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(LOG_TAG, "onConnectionSuspended");
        instance.get_GoogleApiClient().connect();
    }

    @Override
    public void onItemSelected(Long pId, PointsRecyclerViewAdapter.PointsViewHolder vh) {

        if (mTwoPane) {

            if(pId != -1) {

                if(isCurrentViewList) {
                    Bundle arguments = new Bundle();
                    arguments.putLong(MainActivity.KEY_POINT_ID, getIntent().getLongExtra(MainActivity.KEY_POINT_ID, pId));

                    DetailPagerFragment fragment = new DetailPagerFragment();
                    fragment.setArguments(arguments);

                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_detail_container_tablet, fragment)
                            .commit();
                }else{
                    //change camera position
                    MyMapFragment ff = (MyMapFragment)getSupportFragmentManager().findFragmentByTag(MAP_FRAGMENT_TAG);
                    if(ff != null)
                    {
                        ff.changeCameraPosition(pId);
                    }
                }

            }else {

                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_detail_container_tablet, new BlankFragment(), LIST_DETAIL_TAG).commit();
            }

        } else {

            Intent intent = new Intent(this, DetailPointActivity.class);
            intent.putExtra(KEY_POINT_ID,pId);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

                Pair<View, String> pair1 = Pair.create((View)vh.tvHeadline, vh.tvHeadline.getTransitionName());
                Pair<View, String> pair2 = Pair.create((View)vh.tvDescription, vh.tvDescription.getTransitionName());
                Pair<View, String> pair3 = Pair.create((View) vh.tvAuthor, vh.tvAuthor.getTransitionName());
                Pair<View, String> pair4 = Pair.create((View) vh.tvCreateDateTime, vh.tvCreateDateTime.getTransitionName());


                ActivityOptionsCompat options = ActivityOptionsCompat.
                        makeSceneTransitionAnimation(this, pair1, pair2, pair3, pair4);

//                Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(this,
//                        vh.tvHeadline,
//                        vh.tvHeadline.getTransitionName()
//                        ).toBundle();
//                startActivity(intent, bundle);

                startActivity(intent, options.toBundle());
            } else {

                ActivityCompat.startActivity(this, intent, null);
            }

        }
    }
}
