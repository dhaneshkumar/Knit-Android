package loginpages;

import android.accounts.Account;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
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
import com.google.android.gms.common.Scopes;
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

import additionals.SmsListener;
import baseclasses.MyActionBarActivity;
import library.UtilString;
import profileDetails.ProfilePage;
import trumplab.textslate.R;
import trumplabs.schoolapp.Application;
import trumplabs.schoolapp.Constants;
import utility.Config;
import utility.SessionManager;
import utility.Utility;

/**
 * Created by ashish on 26/2/15.
 */
public class PhoneSignUpName extends MyActionBarActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {
    boolean defaultAccountClearedOnce = true;
    Context activityContext;

    EditText displayNameET;
    EditText phoneNumberET;

    static String role = "";
    static String displayName = "";
    static String phoneNumber = "";
    static Location mLastLocation = null;

    static ProgressDialog pdialog;

    GoogleApiClient mGoogleApiClient = null;
    GoogleApiClient mLocationGoogleApiClient = null;
    boolean callLocationApi = false;

    CallbackManager callbackManager;
    static String TAG = "google_login";

    /* Request code used to invoke sign in user interactions. */
    private static final int RC_SIGN_IN = 0;

    /* Is there a ConnectionResult resolution in progress? */
    private boolean mIsResolving = false;

    /* Should we automatically resolve ConnectionResults when possible? */
    private boolean mShouldResolve = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        activityContext = this;

        //intializing facebook sdk
        FacebookSdk.sdkInitialize(getApplicationContext());
        callbackManager = CallbackManager.Factory.create();

        setContentView(R.layout.signup_name);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        displayNameET = (EditText) findViewById(R.id.displaynameid);
        phoneNumberET = (EditText) findViewById(R.id.phoneid);
        LoginButton loginButton = (LoginButton) findViewById(R.id.login_button);
        SignInButton signInButton = (SignInButton) findViewById(R.id.sign_in_button);

        setGooglePlusButtonText(signInButton, "Log in with Google");
        loginButton.setReadPermissions(Arrays.asList("email"));

        getSupportActionBar().setTitle("Sign Up");

        if(getIntent() != null && getIntent().getExtras() != null) {
            resetFields();
            role = getIntent().getExtras().getString("role");

            //Utility.toast(role);
        }
        else{//on press back from next activity. Use previous values to show
            displayNameET.setText(displayName);
            phoneNumberET.setText(phoneNumber);
        }

        // Callback registration
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                if (Config.SHOWLOG) Log.d("D_FB_VERIF", "logged in " + AccessToken.getCurrentAccessToken());

                if (AccessToken.getCurrentAccessToken() != null) {
                    //show progress bar
                    pdialog = new ProgressDialog(activityContext);
                    pdialog.setCancelable(true);
                    pdialog.setCanceledOnTouchOutside(false);
                    pdialog.setMessage("Please Wait...");
                    pdialog.show();


                    String token = AccessToken.getCurrentAccessToken().getToken();
                    if (Config.SHOWLOG) Log.d("D_FB_VERIF", "access token = " + token);

                    String fbUserId = AccessToken.getCurrentAccessToken().getUserId();

                    FBVerifyTask fbVerifyTask = new FBVerifyTask(token, fbUserId, false); //isLogin = false
                    fbVerifyTask.execute();
                } else {
                    if (Config.SHOWLOG) Log.d("D_FB_VERIF", "access token null");
                }
            }

            @Override
            public void onCancel() {
                if (Config.SHOWLOG) Log.d("D_FB_VERIF", "logged cancelled");

            }

            @Override
            public void onError(FacebookException exception) {
                exception.printStackTrace();
                if (Config.SHOWLOG) Log.d("D_FB_VERIF", "FacebookException");
            }
        });

        phoneNumberET.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    next();
                    return true;
                }
                return false;
            }
        });

        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                onSignInClicked();
            }
        });

        PackageManager pm = getPackageManager();
        if(pm.hasSystemFeature(PackageManager.FEATURE_LOCATION)){
            if(pm.hasSystemFeature(PackageManager.FEATURE_LOCATION_NETWORK) || pm.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS)){
                callLocationApi = true;
            }
        }

        if(callLocationApi){
            if(Config.SHOWLOG) Log.d("DEBUG_LOCATION", "buildGoogleApiClient() gps/network feature available");
            buildLocationGoogleApiClient();
        }
        else{
            if(Config.SHOWLOG) Log.d("DEBUG_LOCATION", "buildGoogleApiClient() location feature not available");
        }
    }

//changing google login button text
    protected void setGooglePlusButtonText(SignInButton signInButton, String buttonText) {
        // Find the TextView that is inside of the SignInButton and set its text
        for (int i = 0; i < signInButton.getChildCount(); i++) {
            View v = signInButton.getChildAt(i);

            if (v instanceof TextView) {
                TextView tv = (TextView) v;
                tv.setText(buttonText);

                if(android.os.Build.VERSION.SDK_INT >=14)
                    tv.setAllCaps(false);
                return;
            }
        }
    }


    private void onSignInClicked() {
        // User clicked the sign-in button, so begin the sign-in process and automatically
        // attempt to resolve any errors that occur.

        if(Utility.isInternetExist()) {
            if (mGoogleApiClient == null){
                buildGoogleApiClient();
            }

            if(mGoogleApiClient.isConnected()) {
                Log.d("google", "already connected");
                pdialog = new ProgressDialog(activityContext);
                pdialog.setCancelable(true);
                pdialog.setCanceledOnTouchOutside(false);
                pdialog.setMessage("Please Wait...");
                pdialog.show();

                GoogleVerifyTask googleVerifyTask = new GoogleVerifyTask(false);
                googleVerifyTask.execute();
            }
            else {
                Log.d("google", "not connected");
                mShouldResolve = true;
                mGoogleApiClient.connect();
            }
        }
    }

    void resetFields(){
        displayName = "";
        phoneNumber = "";
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();

        inflater.inflate(R.menu.phone_signup_name_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
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
        displayName = displayNameET.getText().toString();
        phoneNumber = phoneNumberET.getText().toString();
        if (UtilString.isBlank(displayName))
            Utility.toast("Incorrect Display Name", true);
        else if (!Utility.isNumberValid(phoneNumber))
            Utility.toast("Incorrect Mobile Number", true);
        else if(Utility.isInternetExist()) {
            //Changing first letter to caps
            displayName = UtilString.changeFirstToCaps(displayName);

            String msg = "Please confirm your number \n" + "+91"+phoneNumber;
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View view =
                    getLayoutInflater().inflate(R.layout.signup_phone_dialog, null);
            builder.setView(view);
            final Dialog dialog = builder.create();
            dialog.setCanceledOnTouchOutside(true);
            dialog.show();

            TextView edit = (TextView) view.findViewById(R.id.edit);
            TextView nextButton = (TextView) view.findViewById(R.id.nextButton);
            TextView header = (TextView) view.findViewById(R.id.header);

            header.setText(msg);

            nextButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                    Intent nextIntent = new Intent(PhoneSignUpName.this, PhoneSignUpVerfication.class);
                    //nextIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    nextIntent.putExtra("login", false);

                    GenerateVerificationCode generateVerificationCode = new GenerateVerificationCode(1, phoneNumber);
                    startActivity(nextIntent);

                    generateVerificationCode.execute();

                    //Analytics to measure requested signup count
                    Map<String, String> dimensions = new HashMap<String, String>();
                    dimensions.put("Signup", "Requested Signup");
                    ParseAnalytics.trackEvent("Signup", dimensions);
                }
            });

            edit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });

        }
    }

    /*********** Location Detection methods ****************/

    protected void createLocationRequest() {
        //mLocationGoogleApiClient is not null and is connected
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationServices.FusedLocationApi.requestLocationUpdates(mLocationGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onLocationChanged(Location location) {
        //override the last location by this updated current location if non-null
        if (location != null) {
            mLastLocation = location;
            if (Config.SHOWLOG)
                Log.d("DEBUG_LOCATION", "onLocationChanged() : location : " + String.valueOf(mLastLocation.getLatitude())
                        + ", " + String.valueOf(mLastLocation.getLongitude()));

            if(mLocationGoogleApiClient != null && mLocationGoogleApiClient.isConnected()){
                LocationServices.FusedLocationApi.removeLocationUpdates(mLocationGoogleApiClient, this);
            }
        }
    }

    @Override
    public void onStart(){
        super.onStart();
        if(mLocationGoogleApiClient != null) {
            mLocationGoogleApiClient.connect();
        }
    }

    @Override
    public void onStop(){
        super.onStop();
        if(mLocationGoogleApiClient != null) {
            if(Config.SHOWLOG) Log.d("DEBUG_LOCATION", "onStop() client disconnect");
            if(mLocationGoogleApiClient.isConnected()) {
                LocationServices.FusedLocationApi.removeLocationUpdates(mLocationGoogleApiClient, this);
            }
            mLocationGoogleApiClient.disconnect();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(Config.SHOWLOG) Log.d("DEBUG_LOCATION", "onDestroy() client disconnect");
        if(mGoogleApiClient != null)
            mGoogleApiClient.disconnect();
    }

    protected synchronized void buildLocationGoogleApiClient() {
        mLocationGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addScope(new Scope("email"))
                .addScope(new Scope(Scopes.PROFILE))
                .addApi(Plus.API)
                .build();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        if(Config.SHOWLOG) Log.d("DEBUG_LOCATION", "onConnected() entered, first take last known location, just in case that gps location is not received");

        if(mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            if(!defaultAccountClearedOnce){
                mShouldResolve = true;
                mGoogleApiClient.clearDefaultAccountAndReconnect();
                defaultAccountClearedOnce = true;
                return;
            }

            mShouldResolve = false;

            pdialog = new ProgressDialog(activityContext);
            pdialog.setCancelable(true);
            pdialog.setCanceledOnTouchOutside(false);
            pdialog.setMessage("Please Wait...");
            pdialog.show();

            GoogleVerifyTask googleVerifyTask = new GoogleVerifyTask(false);
            googleVerifyTask.execute();
        }
        else if(mGoogleApiClient == null){
            Log.d("google", "null client");
        }

        if(mLocationGoogleApiClient != null && mLocationGoogleApiClient.isConnected()) {
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mLocationGoogleApiClient);
            if (mLastLocation != null) {
                if (Config.SHOWLOG) Log.d("DEBUG_LOCATION", "onConnected() entered, last known location=" + String.valueOf(mLastLocation.getLatitude())
                        + ", " + String.valueOf(mLastLocation.getLongitude()));
            }

            createLocationRequest(); //mLocationGoogleApiClient is not null and is connected
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        // At least one of the API client connect attempts failed
        // No client is connected
        if(Config.SHOWLOG) Log.d("DEBUG_LOCATION", "onConnectionFailed()");

        if(mGoogleApiClient == null){//this failure was for mLocationGoogleApiClient, so ignore
            return;
        }

        if(mGoogleApiClient.isConnected()){ //this failure was for mLocationGoogleApiClient, so ignore
            return;
        }

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

        Log.d(TAG, "calling onConnectionSuspended");
        if(Config.SHOWLOG) Log.d("DEBUG_LOCATION", "onConnectionSuspended()");

    }

    /********* end of Location Detection methods ******/

    /****************** class GenerateVerificationCode **********************/
    public static class GenerateVerificationCode extends AsyncTask<Void, Void, Void> {
        Boolean isValid = false;
        Boolean success = false;
        Boolean networkError = false;

        int callerId;
        String number;
        public GenerateVerificationCode(int id, String num){ //id identifies the caller, num the phone number
            callerId = id;
            number = num;
            SmsListener.register();
        }

        @Override
        protected Void doInBackground(Void... params) {
            //setting parameters
            HashMap<String, Object> param = new HashMap<String, Object>();

            param.put("number", number);

            //calling new function
            if(Config.SHOWLOG) Log.d("DEBUG_SIGNUP_SCHOOL", "calling genCode2() with " + number);
            try {
                isValid = ParseCloud.callFunction("genCode2", param);
                success = true;
            } catch (ParseException e) {
                Utility.LogoutUtility.checkAndHandleInvalidSession(e);
                if(Config.SHOWLOG) Log.d("DEBUG_SIGNUP_SCHOOL", "exception with code " + e.getCode());
                if(e.getCode() == ParseException.CONNECTION_FAILED){
                    networkError = true;
                }
                e.printStackTrace();
                return null;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result){

            if(success) {
                if(!isValid){
                    SmsListener.unRegister();
                    String toastMsg = "Invalid Number";
                    String errorMsg = "Please enter a valid mobile number.";
                    Utility.toast(toastMsg, true);
                    //show error and hide timer as sms was not sent successfully
                    PhoneSignUpVerfication.showError(errorMsg, true);

                    if(Config.DETECT_INVALID_NUMBER) { //if invalid number detection is on
                        PhoneSignUpVerfication.hideVerifyOption();
                    }
                }
            }
            else{
                SmsListener.unRegister();
                String toastMsg = "Oops ! some error occured";
                String errorMsg = "Some unexpected error occured. Please try again";
                if(networkError){
                    toastMsg = "Connection failure";
                    errorMsg = "Unable to establish connection. Please try again";
                }

                Utility.toast(toastMsg, true);
                //show error and hide timer as sms was not sent successfully
                PhoneSignUpVerfication.showError(errorMsg, true);
                PhoneSignUpVerfication.showResendAction();
            }
        }
    }

    public static class FBVerifyTask extends AsyncTask<Void, Void, Void> {
        Boolean networkError = false; //parse exception
        Boolean unexpectedError = false;

        Boolean taskSuccess = false;

        String token;
        boolean isLogin;
        String fbUserId;

        //response
        String flag;

        public FBVerifyTask(String token, String fbUserId, boolean isLogin){//code to verify. Number will be taken from relevant activity
            this.token = token;
            this.fbUserId = fbUserId;
            this.isLogin = isLogin;
        }

        @Override
        protected Void doInBackground(Void... par) {
            if(Config.SHOWLOG) Log.d("D_FB_VERIF", "FBVerifyTask : doInBackground");

            //setting parameters
            HashMap<String, Object> params = new HashMap<String, Object>();

            if(!isLogin) {
                params.put("accessToken", token);
                params.put("role", PhoneSignUpName.role);
                String emailId = Utility.getAccountEmail();
                if(emailId != null){
                    params.put("email", emailId);
                }
            }
            else{
                params.put("accessToken", token);
            }

            //appInstallation params - devicetype, installationId
            params.put("deviceType", "android");
            params.put("installationId", ParseInstallation.getCurrentInstallation().getInstallationId());

            //Sessions save params - os, model, location(lat, long)
            PhoneSignUpVerfication.fillDetailsForSession(isLogin, params);

            try {
                if(Config.SHOWLOG) Log.d("D_FB_VERIF", "appEnter : calling");
                HashMap<String, Object> result = ParseCloud.callFunction("appEnter", params);
                String sessionToken = (String) result.get("sessionToken");
                flag = (String) result.get("flag");

                if(!UtilString.isBlank(sessionToken)){
                    try{
                        if(Config.SHOWLOG) Log.d("D_FB_VERIF", "parseuser become calling " + ParseUser.getCurrentUser());
                        ParseUser user = ParseUser.become(sessionToken);
                        if (user != null) {
                            if(Config.SHOWLOG) Log.d("__A", "setting ignoreInvalidSessionCheck to false");
                            Utility.LogoutUtility.resetIgnoreInvalidSessionCheck();

                            if(Config.SHOWLOG) Log.d("D_FB_VERIF", "parseuser become - returned user correct with given token=" + sessionToken +", currentsessiontoken=" + user.getSessionToken());
                            taskSuccess = true;

                            Log.d(TAG, "fbUserId : " + fbUserId);

                            if(!UtilString.isBlank(fbUserId))
                            {
                                final String url = "https://graph.facebook.com/"  + fbUserId + "/picture?type=large";

                                Log.d(TAG, "url : " + url);


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


                            /* remaining work in onPostExecute since new Asynctask to be created and started in GUI thread*/
                        } else {
                            // The token could not be validated.
                            if(Config.SHOWLOG) Log.d("D_FB_VERIF", "parseuser become - returned user null");
                            unexpectedError = true;
                        }
                    }
                    catch (ParseException e){
                        Utility.LogoutUtility.checkAndHandleInvalidSession(e);
                        if(Config.SHOWLOG) Log.d("D_FB_VERIF", "parseuser become - parse exception");
                        if(e.getCode() == ParseException.CONNECTION_FAILED){
                            networkError = true;
                        }
                        else {
                            unexpectedError = true;
                        }
                    }
                }
                else{
                    if(Config.SHOWLOG) Log.d("D_FB_VERIF", "verifyCode error");
                    unexpectedError = true;
                }
            } catch (ParseException e) {
                Utility.LogoutUtility.checkAndHandleInvalidSession(e);
                if(Config.SHOWLOG) Log.d("D_FB_VERIF", "network error with message " + e.getMessage() + " code "  + e.getCode());
                if(e.getCode() == ParseException.CONNECTION_FAILED){
                    networkError = true;
                }
                else {
                    unexpectedError = true;
                }
                e.printStackTrace();
            }
            if(Config.SHOWLOG) Log.d("D_FB_VERIF", "background : returning null");
            return null;
        }

        @Override
        protected void onPostExecute(Void result){
            if(Config.SHOWLOG) Log.d("D_FB_VERIF", "onPostExecute() of VerifyCodeTask with taskSuccess " + taskSuccess + ", flag=" + flag);

            if(taskSuccess){
                SessionManager session = new SessionManager(Application.getAppContext());
                ParseUser user = ParseUser.getCurrentUser();
                //If user has joined any class then locally saving it in session manager
                if(user != null && user.getList(Constants.JOINED_GROUPS) != null && user.getList(Constants.JOINED_GROUPS).size() >0) {
                    session.setHasUserJoinedClass();
                }

                if(flag != null && flag.equals("logIn")){
                    isLogin = true;
                }

                if(isLogin){
                    PhoneSignUpVerfication.PostLoginTask postLoginTask = new PhoneSignUpVerfication.PostLoginTask(user, pdialog);
                    postLoginTask.execute();
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


    @Override
    public void onBackPressed() {
        super.onBackPressed();

        Intent intent = new Intent(PhoneSignUpName.this, Signup.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
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
            Log.d(TAG, "Google VerifyTask : doInBackground");


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
                e.printStackTrace();
                return null;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            } catch (GoogleAuthException e) {
                e.printStackTrace();
                return null;
            }

            if(idToken != null)
                Log.d(TAG, "idToken : " + idToken);
            else
                Log.d(TAG, "idToken null");

             /*
            Retrieving accessToken
             */
            String scopes = "oauth2:profile email";

            Log.i("D_GOOGLE_VERIFY", "Access token:  starting...." );
            try {
                accessToken = GoogleAuthUtil.getToken(getApplicationContext(), accountName, scopes);
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            } catch (UserRecoverableAuthException e) {
                startActivityForResult(e.getIntent(), RC_SIGN_IN);
                e.printStackTrace();
            } catch (GoogleAuthException e) {
                Log.e(TAG, e.getMessage());
            }

            if(!UtilString.isBlank(accessToken) && !UtilString.isBlank(idToken)) {

                Log.d("D_GOOGLE_VERIFY", "accessToken : " + accessToken);
                Log.d("D_GOOGLE_VERIFY", "idToken : " + idToken);

                //setting parameters
                HashMap<String, Object> params = new HashMap<String, Object>();

                if (!isLogin) {
                    params.put("role", PhoneSignUpName.role);
                    String emailId = Utility.getAccountEmail();
                    if (emailId != null) {
                        params.put("email", emailId);
                    }
                }

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

                                if (mGoogleApiClient.isConnected() && Plus.PeopleApi.getCurrentPerson(mGoogleApiClient) != null) {
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


                                    Log.d(TAG, "url : " + url);
                                }
                                else if(!mGoogleApiClient.isConnected()){
                                    mIsResolving = true;
                                    mGoogleApiClient.connect();
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
        protected void onPostExecute(Void result) {
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
                Utility.toast("Oops ! some error occured. Restart and Try Again", true);

                if(pdialog != null){
                    pdialog.dismiss();
                }
            }

        }
    }

}