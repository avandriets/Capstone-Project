package com.digitallifelab.environmentmonitor;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.digitallifelab.environmentmonitor.Data.AccountsStore;
import com.digitallifelab.environmentmonitor.Utils.DbInstance;
import com.digitallifelab.environmentmonitor.Utils.MyGoogleApiClient_Singleton;
import com.digitallifelab.environmentmonitor.Utils.Utility;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.Scope;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import java.sql.SQLException;
import java.util.List;
import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity {

    private static final String LOG_TAG  = LoginActivity.class.getSimpleName();

    private static final int GOOGLE_SIGN_IN = 100;

    @Bind(R.id.errorLabel)
    TextView errorLabel;

    @Bind(R.id.sign_in_button)
    SignInButton signInButton;

    private View mProgressView;
    private View mLoginFormView;

    MyGoogleApiClient_Singleton instance = null;
    private DbInstance dbInstance = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        ButterKnife.bind(this);

        dbInstance = new DbInstance();

        //Check internet connection
        if(!Utility.isNetworkAvailable(this)){
            errorLabel.setVisibility(View.VISIBLE);
        }else{
            errorLabel.setVisibility(View.GONE);
        }

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                //.requestIdToken(Utility.CLIENT_ID_SERVER)
                .requestEmail()
                .requestScopes(new Scope(Scopes.PLUS_LOGIN), new Scope(Scopes.PLUS_ME))
                .requestServerAuthCode(Utility.Get_Client_ID_Server(this), false)
                .build();

        instance = new MyGoogleApiClient_Singleton();

        signInButton.setSize(SignInButton.SIZE_STANDARD);
        signInButton.setScopes(gso.getScopeArray());

        for (int i = 0; i < signInButton.getChildCount(); i++) {
            View v = signInButton.getChildAt(i);

            if (v instanceof TextView) {
                TextView tv = (TextView) v;
                tv.setPadding(0, 0, 20, 0);
                return;
            }
        }

        //bind progress dialog
        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);
    }

    @OnClick(R.id.sign_in_button)
    public void sign_in_click(View view) {

        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(instance.get_GoogleApiClient());
        startActivityForResult(signInIntent, GOOGLE_SIGN_IN);

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == GOOGLE_SIGN_IN) {
            if(resultCode == RESULT_OK) {
                GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
                handleGoogleSignInResult(result);
            }
        }
    }

    private void handleGoogleSignInResult(GoogleSignInResult result) {

        Log.d(LOG_TAG, "Google SignInResult " + result.isSuccess());

        if (result.isSuccess()) {
            // Signed in successfully, show authenticated UI.
            GoogleSignInAccount acct = result.getSignInAccount();

            Uri usersPhoto = acct.getPhotoUrl();
            if(usersPhoto == null){
                usersPhoto = new Uri.Builder().build();
            }
            saveUserInfoToDB(acct.getEmail(), acct.getDisplayName(), usersPhoto, AccountsStore.OAuthProviders.google);

            Log.d(LOG_TAG, "Finish Google SignIn");
            setResult(RESULT_OK, null);

            finish();

        } else {
            Toast.makeText(LoginActivity.this, "Cannot SignIn to server.", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveUserInfoToDB(String  email, String  name, Uri photoUrl,  AccountsStore.OAuthProviders provider) {

        RuntimeExceptionDao<AccountsStore, String> dao = dbInstance.getDatabaseHelper().getAccountsDataDao();

        try {

            List<AccountsStore> list = dao.query(dao.queryBuilder().where().eq(AccountsStore.EMAIL_FIELD_NAME, email).prepare());

            if(list.size() == 0)
            {
                // create some entries in the onCreate
                AccountsStore UserAccount = new AccountsStore(email, name, photoUrl.toString(), provider);
                UserAccount.setActive(1);

                dao.create(UserAccount);
            }
            else{
                list.get(0).setActive(1);
                dao.update(list.get(0));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public void onBackPressed() {
        // disable going back to the MainActivity
        moveTaskToBack(true);

        if(instance.get_GoogleApiClient().isConnected()){
            instance.get_GoogleApiClient().disconnect();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if(instance.get_GoogleApiClient().isConnected())
            instance.get_GoogleApiClient().connect();
    }

}

