package com.digitallifelab.environmentmonitor;


import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.digitallifelab.environmentmonitor.Data.AccountsStore;
import com.digitallifelab.environmentmonitor.Data.EnvironmentMonitorContract;
import com.digitallifelab.environmentmonitor.Data.EnvironmentService;
import com.digitallifelab.environmentmonitor.Data.MessagesStore;
import com.digitallifelab.environmentmonitor.Data.OrmLiteCursorLoader;
import com.digitallifelab.environmentmonitor.Data.PointsStore;
import com.digitallifelab.environmentmonitor.Utils.DbInstance;
import com.digitallifelab.environmentmonitor.Utils.NetworkServiceUtility;
import com.digitallifelab.environmentmonitor.Utils.Utility;
import com.digitallifelab.environmentmonitor.adapters.MessagesRecyclerViewAdapter;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.SelectArg;
import com.j256.ormlite.stmt.Where;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class MessagesFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String LOG_TAG = MessagesFragment.class.getSimpleName();

    @Bind(R.id.etMessage)    TextView tvMessage;

    private DbInstance dbInstance  = null;
    private Dao<MessagesStore,Long> messagesStoreDao;
    private PreparedQuery<MessagesStore> preparedQuery;

    private long       current_id;

    private LinearLayoutManager         lLayout;
    private MessagesRecyclerViewAdapter mAdapter;
    RecyclerView rView;

    private static final int MESSAGE_ID_LOADER = 2;
    boolean mTwoPane = false;
    private ProgressDialog progressDialog;

    public MessagesFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_points_messages, container, false);

        dbInstance = new DbInstance();

        if(dbInstance.getDatabaseHelper() == null)
            dbInstance.SetDBHelper(getActivity());

        ButterKnife.bind(this, rootView);

        Bundle arguments = getArguments();

        if (arguments != null) {
            current_id = arguments.getLong(MainActivity.KEY_POINT_ID);
        }

        try {
            messagesStoreDao = dbInstance.getDatabaseHelper().getMessagesDao();

            QueryBuilder<MessagesStore, Long> queryBuilder = messagesStoreDao.queryBuilder();

            queryBuilder.orderBy(MessagesStore.UPDATED_AT,false);

            Where<MessagesStore, Long> where = queryBuilder.where();

            SelectArg selectArg = new SelectArg();

            selectArg.setValue(dbInstance.getDatabaseHelper().getPointsDataDao().queryForId(current_id).getLocal_id());

            where.eq(MessagesStore.POINT_ID, selectArg);


            preparedQuery = where.prepare();

        } catch (SQLException e) {
            e.printStackTrace();
        }

        lLayout = new LinearLayoutManager(getActivity());
        lLayout.setReverseLayout(true);

        View mEmptyView = (View)rootView.findViewById(R.id.messages_list_empty);

        mAdapter = new MessagesRecyclerViewAdapter(getActivity(), mEmptyView );

        rView = (RecyclerView)rootView.findViewById(R.id.messagesRecyclerView);
        rView.setHasFixedSize(true);
        rView.setLayoutManager(lLayout);
        rView.setAdapter(mAdapter);

        android.support.v4.content.Loader<Object> loader = getLoaderManager().getLoader(MESSAGE_ID_LOADER);

        if (loader != null && !loader.isReset()) {
            getLoaderManager().restartLoader(MESSAGE_ID_LOADER, null, this);
        } else {
            getLoaderManager().initLoader(MESSAGE_ID_LOADER, null, this);
        }

        if(getActivity().findViewById(R.id.fragment_detail_container_tablet) != null){
            mTwoPane = true;
        }

        return rootView;
    }

    @OnClick(R.id.btSend)
    public void sendMessage() {

        RuntimeExceptionDao<MessagesStore, Long> messagesStores = dbInstance.getDatabaseHelper().getMessagesDataDao();
        RuntimeExceptionDao<PointsStore, Long> currentPoint = dbInstance.getDatabaseHelper().getPointsDataDao();

        PointsStore point = currentPoint.queryForId(current_id);

        MessagesStore newMessage = new MessagesStore(point, System.currentTimeMillis(), tvMessage.getText().toString());
        messagesStores.create(newMessage);

        tvMessage.setText("");

        if(Utility.isNetworkAvailable(getActivity())) {
            new SendMessageTask(newMessage).execute();
        }else{
            ShowProgress(false);

            Toast.makeText(getActivity(), R.string.error_no_internet_connection, Toast.LENGTH_SHORT).show();
            getActivity().getContentResolver().insert(EnvironmentMonitorContract.POINTS_CONTENT_URI, null);

            if(MainActivity.class.isInstance(getActivity())){
                ((PointsListFragment.Callback)getActivity()).onItemSelected(point.getLocal_id(), null);
            }else{
                getActivity().finish();
            }
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
        return new OrmLiteCursorLoader(getActivity(), messagesStoreDao, preparedQuery);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        Log.d(LOG_TAG, "Finish loading");
        mAdapter.changeCursor(data, preparedQuery);
        lLayout.scrollToPositionWithOffset(0, 20);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.changeCursor(null, preparedQuery);
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

    public class SendMessageTask extends AsyncTask<Void, Void, Intent> {

        private MessagesStore   message;
        private PointsStore     pointsStore;

        private Retrofit retrofit = null;
        private EnvironmentService service;

        SendMessageTask(MessagesStore message) {
            this.message    = message;
            this.pointsStore = message.getPoint();


            retrofit = new Retrofit.Builder()
                    .baseUrl(Utility.BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            service = retrofit.create(EnvironmentService.class);
        }

        @Override
        protected Intent doInBackground(Void... params) {

            Log.d(LOG_TAG, "Async task send message to server");

            AccountsStore acc = AccountsStore.getActiveUser();
            Intent res = NetworkServiceUtility.SendMessageToServer(message, pointsStore, service,"Bearer " + acc.getMy_server_access_token(),dbInstance);

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
                        finishSubmitElement();
                    }
                });

                AlertDialog dialog = builder.create();
                dialog.show();

            } else {
                finishSubmitElement();
            }
        }

        @Override
        protected void onCancelled() {
            ShowProgress(false);
        }

    }

    private void finishSubmitElement() {
        getActivity().getContentResolver().insert(EnvironmentMonitorContract.POINTS_CONTENT_URI, null);
    }
}
