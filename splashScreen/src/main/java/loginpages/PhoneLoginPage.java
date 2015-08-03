package loginpages;

import android.accounts.Account;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;
import com.parse.ParseAnalytics;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseInstallation;
import com.parse.ParseUser;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import baseclasses.MyActionBarActivity;
import library.UtilString;
import profileDetails.ProfilePage;
import trumplab.textslate.R;
import trumplabs.schoolapp.Application;
import trumplabs.schoolapp.Constants;
import trumplabs.schoolapp.MainActivity;
import utility.Config;
import utility.SessionManager;
import utility.Utility;

public class PhoneLoginPage extends MyActionBarActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {
    EditText phoneNumberET;
    TextView oldLoginTV;

    Activity activity;

    static String phoneNumber = "";

    static GoogleApiClient mGoogleApiClient = null;
    static Location mLastLocation = null;
    CallbackManager callbackManager;
    static ProgressDialog pdialog;
    static Context activityContext;
    boolean callLocationApi = true;

    /* Request code used to invoke sign in user interactions. */
    private static final int RC_SIGN_IN = 0;

    /* Is there a ConnectionResult resolution in progress? */
    private boolean mIsResolving = false;

    /* Should we automatically resolve ConnectionResults when possible? */
    private boolean mShouldResolve = false;


    protected void onCreate(android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(getApplicationContext());
        callbackManager = CallbackManager.Factory.create();

        setContentView(R.layout.phone_login_page);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ParseUser user = ParseUser.getCurrentUser();
        if (user != null) {
            startActivity(new Intent(getBaseContext(), SplashScreen.class));
            Intent intent = new Intent(getBaseContext(), MainActivity.class);
            startActivity(intent);
        }
        activity = this;
        activityContext = this;
        phoneNumberET = (EditText) findViewById(R.id.phone_id);
        oldLoginTV = (TextView) findViewById(R.id.oldLogin);

        LoginButton loginButton = (LoginButton) findViewById(R.id.login_button);
        SignInButton signInButton = (SignInButton) findViewById(R.id.sign_in_button);

        loginButton.setReadPermissions(Arrays.asList("email"));


        if (getIntent() != null && getIntent().getExtras() != null) {
            if (Config.SHOWLOG) Log.d("DEBUG_PHONE_LOGIN", "setting phone number empty");
            phoneNumber = ""; //reset as called from parent
        } else {//coming back from child, so restore the fields
            phoneNumberET.setText(phoneNumber);
        }

        oldLoginTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent oldLoginIntent = new Intent(getBaseContext(), LoginPage.class);
                oldLoginIntent.putExtra("login", false);
                startActivity(oldLoginIntent);
            }
        });

        buildGoogleApiClient();

        // Callback registration
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Log.d("D_FB_VERIF", "logged in " + AccessToken.getCurrentAccessToken());

                if (AccessToken.getCurrentAccessToken() != null) {
                    //show progress bar
                    pdialog = new ProgressDialog(activityContext);
                    pdialog.setCancelable(true);
                    pdialog.setCanceledOnTouchOutside(false);
                    pdialog.setMessage("Please Wait...");
                    pdialog.show();


                    String token = AccessToken.getCurrentAccessToken().getToken();
                    Log.d("D_FB_VERIF", "access token = " + token);

                    String fbUserId = AccessToken.getCurrentAccessToken().getUserId();

                    PhoneSignUpName.FBVerifyTask fbVerifyTask = new PhoneSignUpName.FBVerifyTask(token, fbUserId, true); //isLogin = false
                    fbVerifyTask.execute();
                } else {
                    Log.d("D_FB_VERIF", "access token null");
                }
            }

            @Override
            public void onCancel() {
                Log.d("D_FB_VERIF", "logged cancelled");

            }

            @Override
            public void onError(FacebookException exception) {
                exception.printStackTrace();
                Log.d("D_FB_VERIF", "FacebookException");
            }
        });

        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                onSignInClicked();
            }
        });

        PackageManager pm = getPackageManager();
        if (!pm.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS) || !pm.hasSystemFeature(PackageManager.FEATURE_LOCATION) || !pm.hasSystemFeature(PackageManager.FEATURE_LOCATION_NETWORK)) {
            if(Config.SHOWLOG) Log.d("DEBUG_LOCATION", "buildGoogleApiClient() feature not available");
            callLocationApi = false;
        }
    }

    private void onSignInClicked() {
        // User clicked the sign-in button, so begin the sign-in process and automatically
        // attempt to resolve any errors that occur.
        mShouldResolve = true;

        if(Utility.isInternetExist())
            mGoogleApiClient.connect();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            // If the error resolution was not successful we should not resolve further.
            if (resultCode != RESULT_OK) {
                mShouldResolve = false;
            }

            mIsResolving = false;
            mGoogleApiClient.connect();
        }
        else
            callbackManager.onActivityResult(requestCode, resultCode, data);
    }



    /*********** Location Detection methods ****************/

    protected void createLocationRequest() {

        if(callLocationApi) {
            LocationRequest mLocationRequest = new LocationRequest();
            mLocationRequest.setInterval(1000);
            mLocationRequest.setFastestInterval(1000);
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        //override the last location by this updated current location if non-null
        if (location != null) {
            mLastLocation = location;
            if(Config.SHOWLOG) Log.d("DEBUG_LOCATION_LOGIN", "onLocationChanged() : location : " + String.valueOf(mLastLocation.getLatitude())
                    + ", " + String.valueOf(mLastLocation.getLongitude()));

            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
    }

    @Override
    public void onStart(){
        super.onStart();
        if(mGoogleApiClient != null) {
            if(Utility.isInternetExistWithoutPopup())
                 mGoogleApiClient.connect();
        }
    }

    @Override
    public void onStop(){
        super.onStop();
       // if(mGoogleApiClient != null) {
            if(Config.SHOWLOG) Log.d("DEBUG_LOCATION_LOGIN", "onStop() client disconnect");
            //mGoogleApiClient.disconnect();
        //}
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(Config.SHOWLOG) Log.d("DEBUG_LOCATION_LOGIN", "onDestroy() client disconnect");
        if(mGoogleApiClient != null)
            mGoogleApiClient.disconnect();
    }

    protected synchronized void buildGoogleApiClient() {
        PackageManager pm = getPackageManager();
        if (!callLocationApi){
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addScope(new Scope("https://www.googleapis.com/auth/userinfo.email"))
                    .addScope(new Scope("https://www.googleapis.com/auth/plus.login"))
                    .addApi(Plus.API)
                    .build();
        }
        else{
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addScope(new Scope("https://www.googleapis.com/auth/userinfo.email"))
                .addScope(new Scope("https://www.googleapis.com/auth/plus.login"))
                .addApi(Plus.API)
                .build();
    }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        if(Config.SHOWLOG) Log.d("DEBUG_LOCATION_LOGIN", "onConnected() entered, first take last known location, just in case that gps location is not received");

        mShouldResolve = false;

        pdialog = new ProgressDialog(activityContext);
        pdialog.setCancelable(true);
        pdialog.setCanceledOnTouchOutside(false);
        pdialog.setMessage("Please Wait...");
        pdialog.show();

        GoogleVerifyTask googleVerifyTask = new  GoogleVerifyTask(true);
        googleVerifyTask.execute();

        if(callLocationApi) {
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if (mLastLocation != null) {
                if (Config.SHOWLOG)
                    Log.d("DEBUG_LOCATION_LOGIN", "onConnected() entered, last known location=" + String.valueOf(mLastLocation.getLatitude())
                            + ", " + String.valueOf(mLastLocation.getLongitude()));
            }

            createLocationRequest();
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        // At least one of the API client connect attempts failed
        // No client is connected
        if(Config.SHOWLOG) Log.d("DEBUG_LOCATION_LOGIN", "onConnectionFailed()");

        if (!mIsResolving && mShouldResolve) {
            if (connectionResult.hasResolution()) {
                try {
                    connectionResult.startResolutionForResult(this, RC_SIGN_IN);
                    mIsResolving = true;
                } catch (IntentSender.SendIntentException e) {
                    mIsResolving = false;
                    mGoogleApiClient.connect();
                }
            } else {
                // Could not resolve the connection result, show the user an
                // error dialog.
                //showErrorDialog(connectionResult);
                Utility.toast("Can't resolve error. Try Again !");
            }
        }
    }

    @Override
    public void onConnectionSuspended(int result) {
        // At least one of the API client connect attempts failed
        // No client is connected
        if(Config.SHOWLOG) Log.d("DEBUG_LOCATION_LOGIN", "onConnectionSuspended()");
    }

    @Override
    public void onPause(){
        if(Config.SHOWLOG) Log.d("DEBUG_LOCATION_LOGIN", "onPause() called");
        if(mGoogleApiClient != null && mGoogleApiClient.isConnected() && callLocationApi) {
            if(Config.SHOWLOG) Log.d("DEBUG_LOCATION_LOGIN", "onPause() removeLocationUpdates");
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
        super.onPause();
    }

    /********* end of Location Detection methods ******/

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.phone_signup_name_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public void onBackPressed() {
        Intent intent = new Intent(getBaseContext(), Signup.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        FragmentManager fm = getSupportFragmentManager();
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.next:
                next();
                break;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    void next(){
        phoneNumber = phoneNumberET.getText().toString();
        if (!Utility.isNumberValid(phoneNumber)) {
            Utility.toast("Incorrect Mobile Number", true);
        }
        else if(Utility.isInternetExist()) {
            /*pdialog = new ProgressDialog(this);
            pdialog.setCancelable(false);
            pdialog.setMessage("Please Wait...");
            pdialog.show();*/

            Intent nextIntent = new Intent(this, PhoneSignUpVerfication.class);
            //nextIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            nextIntent.putExtra("login", true);

            PhoneSignUpName.GenerateVerificationCode generateVerificationCode = new PhoneSignUpName.GenerateVerificationCode(2, phoneNumber);
            startActivity(nextIntent);

            generateVerificationCode.execute();

            //Analytics to measure requested login count
            Map<String, String> dimensions = new HashMap<String, String>();
            dimensions.put("Login", "Requested Login");
            ParseAnalytics.trackEvent("Login", dimensions);

        }

    }


    class GoogleVerifyTask extends AsyncTask<Void, Void, Void> {
        Boolean networkError = false; //parse exception
        Boolean unexpectedError = false;
        Boolean taskSuccess = false;
        boolean isLogin;
        String idToken = "";
        String accessToken = "";
        String SERVER_CLIENT_ID = "838906570879-nujge366mj36s29elltobjnehh9e1a5j.apps.googleusercontent.com";

        //response
        String flag;

        public GoogleVerifyTask(boolean isLogin){//code to verify. Number will be taken from relevant activity
            this.isLogin = isLogin;
        }

        @Override
        protected Void doInBackground(Void... par) {
            Log.d("D_GOOGLE_VERIF", "FBVerifyTask : doInBackground");


            /*
            Retrieving idToken
             */
            String accountName = Plus.AccountApi.getAccountName(mGoogleApiClient);
            Account account = new Account(accountName, GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE);

            List<String> scopeList = Arrays.asList(new String[]{
                    "https://www.googleapis.com/auth/plus.login",
                    "https://www.googleapis.com/auth/userinfo.email"
            });

            //String scope = String.format("audience:server:client_id:%s:api_scope:%s", SERVER_CLIENT_ID, TextUtils.join(" ", scopeList));
            String scope = String.format("audience:server:client_id:%s", SERVER_CLIENT_ID);

            try {
                idToken =  GoogleAuthUtil.getToken(Application.getAppContext(), account, scope);
            } catch (UserRecoverableAuthException e) {
                // Requesting an authorization code will always throw
                // UserRecoverableAuthException on the first call to GoogleAuthUtil.getToken
                // because the user must consent to offline access to their data.  After
                // consent is granted control is returned to your activity in onActivityResult
                // and the second call to GoogleAuthUtil.getToken will succeed.
                startActivityForResult(e.getIntent(), RC_SIGN_IN);
                return null;
            } catch (IOException e) {
                return null;
            } catch (GoogleAuthException e) {
                return null;
            }

             /*
            Retrieving accessToken
             */
            String scopes = "oauth2:profile email";

            Log.i("D_GOOGLE_VERIFY", "Access token:  starting...." );
            try {
                accessToken = GoogleAuthUtil.getToken(getApplicationContext(), accountName, scopes);
            } catch (IOException e) {
            } catch (UserRecoverableAuthException e) {
                startActivityForResult(e.getIntent(), RC_SIGN_IN);
            } catch (GoogleAuthException e) {
            }

            if(!UtilString.isBlank(accessToken) && !UtilString.isBlank(idToken)) {

                Log.d("D_GOOGLE_VERIFY", "accessToken : " + accessToken);
                Log.d("D_GOOGLE_VERIFY", "idToken : " + idToken);

                //setting parameters
                HashMap<String, Object> params = new HashMap<String, Object>();


                params.put("accessToken", accessToken);
                params.put("idToken", idToken);

                //appInstallation params - devicetype, installationId
                params.put("deviceType", "android");
                params.put("installationId", ParseInstallation.getCurrentInstallation().getInstallationId());

                //Sessions save params - os, model, location(lat, long)
                PhoneSignUpVerfication.fillDetailsForSession(isLogin, params);

                try {
                    Log.d("D_GOOGLE_VERIFY", "appEnter : calling");
                    HashMap<String, Object> result = ParseCloud.callFunction("appEnter", params);
                    String sessionToken = (String) result.get("sessionToken");
                    flag = (String) result.get("flag");

                    if (!UtilString.isBlank(sessionToken)) {
                        try {
                            Log.d("D_GOOGLE_VERIFY", "parseuser become calling " + ParseUser.getCurrentUser());
                            ParseUser user = ParseUser.become(sessionToken);
                            if (user != null) {
                                Log.d("__A", "setting ignoreInvalidSessionCheck to false");
                                Utility.LogoutUtility.resetIgnoreInvalidSessionCheck();

                                Log.d("D_GOOGLE_VERIFY", "parseuser become - returned user correct with given token=" + sessionToken + ", currentsessiontoken=" + user.getSessionToken());
                                taskSuccess = true;

                                if (Plus.PeopleApi.getCurrentPerson(mGoogleApiClient) != null) {
                                    Person currentPerson = Plus.PeopleApi.getCurrentPerson(mGoogleApiClient);
                                    final String url = currentPerson.getImage().getUrl();

                                    if(!UtilString.isBlank(url))
                                    {
                                        //call refresher
                                        Runnable r = new Runnable() {
                                            @Override
                                            public void run() {
                                                try {
                                                    ProfilePage.setSocialProfilePic(url);
                                                } catch (IOException e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                        };
                                        Thread t = new Thread(r);
                                        t.setPriority(Thread.MIN_PRIORITY);
                                        t.start();
                                    }

                                }

                            /* remaining work in onPostExecute since new Asynctask to be created and started in GUI thread*/
                            } else {
                                // The token could not be validated.
                                Log.d("D_GOOGLE_VERIFY", "parseuser become - returned user null");
                                unexpectedError = true;
                            }
                        } catch (ParseException e) {
                            Utility.LogoutUtility.checkAndHandleInvalidSession(e);
                            Log.d("D_GOOGLE_VERIFY", "parseuser become - parse exception");
                            if (e.getCode() == ParseException.CONNECTION_FAILED) {
                                networkError = true;
                            } else {
                                unexpectedError = true;
                            }
                        }
                    } else {
                        Log.d("D_GOOGLE_VERIFY", "verifyCode error");
                        unexpectedError = true;
                    }
                } catch (ParseException e) {
                    Utility.LogoutUtility.checkAndHandleInvalidSession(e);
                    Log.d("D_GOOGLE_VERIFY", "network error with message " + e.getMessage() + " code " + e.getCode());
                    if (e.getCode() == ParseException.CONNECTION_FAILED) {
                        networkError = true;
                    } else {
                        unexpectedError = true;
                    }
                    e.printStackTrace();
                }
            }
            else
            {
                if(UtilString.isBlank(accessToken))
                    Log.d("D_GOOGLE_VERIFY", "accessToken is null");
                if(UtilString.isBlank(idToken))
                    Log.d("D_GOOGLE_VERIFY", "idToken is null");
            }
            Log.d("D_GOOGLE_VERIFY", "background : returning null");
            return null;
        }

        @Override
        protected void onPostExecute(Void result){
            Log.d("D_FB_VERIF", "onPostExecute() of VerifyCodeTask with taskSuccess " + taskSuccess + ", flag=" + flag);

            if(taskSuccess){
                SessionManager session = new SessionManager(Application.getAppContext());
                ParseUser user = ParseUser.getCurrentUser();
                //If user has joined any class then locally saving it in session manager
                if(user != null && user.getList(Constants.JOINED_GROUPS) != null && user.getList(Constants.JOINED_GROUPS).size() >0) {
                    session.setHasUserJoinedClass();
                }

                Log.d("fblogin", "starting fb");

                if(flag != null && flag.equals("logIn")){
                    isLogin = true;
                }

                if(isLogin){
                    PhoneSignUpVerfication.PostLoginTask postLoginTask = new PhoneSignUpVerfication.PostLoginTask(user, pdialog);
                    postLoginTask.execute();

                    Log.d("fblogin", "starting fb login activity");
                }
                else {
                    session.setSignUpAccount();

                    // The current user is now set to user. Do registration in default class
                    PhoneSignUpVerfication.PostSignUpTask postSignUpTask = new PhoneSignUpVerfication.PostSignUpTask(user, pdialog);
                    postSignUpTask.execute();
                }
            }

            if(!taskSuccess){
                if(pdialog != null){
                    pdialog.dismiss();
                }
                //hide progress bar
            }

            if(networkError){
                Utility.toast("Connection failure", true);
            }
            else if(unexpectedError){
                Utility.toast("Oops ! some error occured.", true);
                if(pdialog != null){
                    pdialog.dismiss();
                }
            }
        }
    }
}
