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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import additionals.SmsListener;
import baseclasses.MyActionBarActivity;
import library.UtilString;
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
    static Context activityContext;

    EditText displayNameET;
    EditText phoneNumberET;

    static String role = "";
    static String displayName = "";
    static String phoneNumber = "";
    static ProgressDialog pdialog;

    static GoogleApiClient mGoogleApiClient = null;
    static Location mLastLocation = null;
    CallbackManager callbackManager;
    String TAG = "google_login";

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

        loginButton.setReadPermissions(Arrays.asList("email"));

        if(getIntent() != null && getIntent().getExtras() != null) {
            resetFields();
            role = getIntent().getExtras().getString("role");

            //Utility.toast(role);
        }
        else{//on press back from next activity. Use previous values to show
            displayNameET.setText(displayName);
            phoneNumberET.setText(phoneNumber);
        }

        buildGoogleApiClient();

        // Callback registration
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Log.d("D_FB_VERIF", "logged in " + AccessToken.getCurrentAccessToken());

                if(AccessToken.getCurrentAccessToken() != null){
                    //show progress bar
                    pdialog = new ProgressDialog(activityContext);
                    pdialog.setCancelable(true);
                    pdialog.setCanceledOnTouchOutside(false);
                    pdialog.setMessage("Please Wait...");
                    pdialog.show();


                    String token = AccessToken.getCurrentAccessToken().getToken();
                    Log.d("D_FB_VERIF", "access token = " + token);

                    FBVerifyTask fbVerifyTask = new FBVerifyTask(token, false); //isLogin = false
                    fbVerifyTask.execute();
                }
                else{
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

    }

    private void onSignInClicked() {
        // User clicked the sign-in button, so begin the sign-in process and automatically
        // attempt to resolve any errors that occur.
        mShouldResolve = true;
        mGoogleApiClient.connect();

        // Show a message to the user that we are signing in.
       // mStatusTextView.setText(R.string.signing_in);

        Utility.toast("Signing in to google account");
    }


    public void RequestData(){

        GraphRequest request = GraphRequest.newMeRequest(AccessToken.getCurrentAccessToken(), new GraphRequest.GraphJSONObjectCallback() {
            @Override
            public void onCompleted(JSONObject object, GraphResponse response) {

                JSONObject json = response.getJSONObject();
                try {
                    if (json != null) {

                        Log.d("FB_SIGNUP", "name : " + json.getString("name"));
                        //    Log.d("FB_SIGNUP", "email : " + json.getString("email"));
                        Log.d("FB_SIGNUP", "id : " + json.getString("id"));
                        Log.d("FB_SIGNUP", "Link : " + json.getString("link"));
                        Log.d("FB_SIGNUP", "age_range : " + json.getString("age_range"));
                        Log.d("FB_SIGNUP", "gender : " + json.getString("gender"));
                        Log.d("FB_SIGNUP", "locale : " + json.getString("locale"));
                        Log.d("FB_SIGNUP", "verified : " + json.getString("verified"));
                        Log.d("FB_SIGNUP", "access token : " + AccessToken.getCurrentAccessToken());

                        // set profile pic
                        //   http://stackoverflow.com/questions/19855072/android-get-facebook-profile-picture

                        String token = AccessToken.getCurrentAccessToken().getToken();

                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        Bundle parameters = new Bundle();
        parameters.putString("fields", "id,name,link,email,picture, gender, age_range, locale, verified, location");
        request.setParameters(parameters);
        request.executeAsync();
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

                if (mGoogleApiClient.isConnected()) {
                    Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
                    mGoogleApiClient.disconnect();
                }

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
        else if (UtilString.isBlank(phoneNumber) || phoneNumber.length() != 10)
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
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onLocationChanged(Location location) {
        //override the last location by this updated current location if non-null
        if (location != null) {
            mLastLocation = location;
            Log.d("DEBUG_LOCATION", "onLocationChanged() : location : " + String.valueOf(mLastLocation.getLatitude())
                    + ", " + String.valueOf(mLastLocation.getLongitude()));

            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
    }

    @Override
    public void onStart(){
        super.onStart();
        if(mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    public void onStop(){
        super.onStop();
        if(mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
    }

    protected synchronized void buildGoogleApiClient() {
        PackageManager pm = getPackageManager();
        if (!pm.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS) || !pm.hasSystemFeature(PackageManager.FEATURE_LOCATION) || !pm.hasSystemFeature(PackageManager.FEATURE_LOCATION_NETWORK)) {
            Log.d("DEBUG_LOCATION", "buildGoogleApiClient() feature not available");
            return;
        }

        Log.d("DEBUG_LOCATION", "buildGoogleApiClient() entered");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addScope(new Scope("https://www.googleapis.com/auth/userinfo.email"))
                .addScope(new Scope("https://www.googleapis.com/auth/plus.login"))
                .addApi(Plus.API)
                .build();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d("DEBUG_LOCATION", "onConnected() entered, first take last known location, just in case that gps location is not received");



       Utility.toast("Yo. Signed IN to account");

        mShouldResolve = false;

        GetIdTokenTask getIdTokenTask = new GetIdTokenTask();
        getIdTokenTask.execute();

        RetrieveTokenTask retrieveTokenTask = new RetrieveTokenTask();
        retrieveTokenTask.execute();


        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if(mLastLocation != null){
            Log.d("DEBUG_LOCATION", "onConnected() entered, last known location=" + String.valueOf(mLastLocation.getLatitude())
                    + ", " + String.valueOf(mLastLocation.getLongitude()));
        }

        createLocationRequest();

        if (Plus.PeopleApi.getCurrentPerson(mGoogleApiClient) != null) {
            Person currentPerson = Plus.PeopleApi.getCurrentPerson(mGoogleApiClient);
            String personName = currentPerson.getDisplayName();
            Person.Image personPhoto = currentPerson.getImage();
            String personGooglePlusProfile = currentPerson.getUrl();

            String email = Plus.AccountApi.getAccountName(mGoogleApiClient);

            String t = "login";
            Log.d(t, "name : " + personName);
            Log.d(t, "url : " + personGooglePlusProfile);
            Log.d(t, "email : " + email);
        }


    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        // At least one of the API client connect attempts failed
        // No client is connected
        Log.d("DEBUG_LOCATION", "onConnectionFailed()");


        if (!mIsResolving && mShouldResolve) {
            if (connectionResult.hasResolution()) {
                try {
                    connectionResult.startResolutionForResult(this, RC_SIGN_IN);
                    mIsResolving = true;
                } catch (IntentSender.SendIntentException e) {
                    Log.e(TAG, "Could not resolve ConnectionResult.", e);
                    mIsResolving = false;
                    mGoogleApiClient.connect();
                }
            } else {
                // Could not resolve the connection result, show the user an
                // error dialog.
                //showErrorDialog(connectionResult);
                Utility.toast("Can't resolve error. Try Again !");
            }
        } else {
            // Show the signed-out UI
            //  showSignedOutUI();
        }
    }

    @Override
    public void onConnectionSuspended(int result) {
        // At least one of the API client connect attempts failed
        // No client is connected
        Log.d("DEBUG_LOCATION", "onConnectionSuspended()");

    }

    @Override
    public void onPause(){
        Log.d("DEBUG_LOCATION", "onPause() called");
        if(mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
        super.onPause();
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
            Log.d("DEBUG_SIGNUP_SCHOOL", "calling genCode2() with " + number);
            try {
                isValid = ParseCloud.callFunction("genCode2", param);
                success = true;
            } catch (ParseException e) {
                Utility.LogoutUtility.checkAndHandleInvalidSession(e);
                Log.d("DEBUG_SIGNUP_SCHOOL", "exception with code " + e.getCode());
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

    class FBVerifyTask extends AsyncTask<Void, Void, Void> {
        Boolean networkError = false; //parse exception
        Boolean unexpectedError = false;

        Boolean taskSuccess = false;

        String token;
        boolean isLogin;

        //response
        String flag;

        public FBVerifyTask(String token, boolean isLogin){//code to verify. Number will be taken from relevant activity
            this.token = token;
            this.isLogin = isLogin;
        }

        @Override
        protected Void doInBackground(Void... par) {
            Log.d("D_FB_VERIF", "FBVerifyTask : doInBackground");

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
                params.put("number", PhoneLoginPage.phoneNumber);
            }

            //appInstallation params - devicetype, installationId
            params.put("deviceType", "android");
            params.put("installationId", ParseInstallation.getCurrentInstallation().getInstallationId());

            //Sessions save params - os, model, location(lat, long)
            PhoneSignUpVerfication.fillDetailsForSession(isLogin, params);

            try {
                Log.d("D_FB_VERIF", "appEnter : calling");
                HashMap<String, Object> result = ParseCloud.callFunction("appEnter", params);
                String sessionToken = (String) result.get("sessionToken");
                flag = (String) result.get("flag");

                if(!UtilString.isBlank(sessionToken)){
                    try{
                        Log.d("D_FB_VERIF", "parseuser become calling " + ParseUser.getCurrentUser());
                        ParseUser user = ParseUser.become(sessionToken);
                        if (user != null) {
                            Log.d("__A", "setting ignoreInvalidSessionCheck to false");
                            Utility.LogoutUtility.resetIgnoreInvalidSessionCheck();

                            Log.d("D_FB_VERIF", "parseuser become - returned user correct with given token=" + sessionToken +", currentsessiontoken=" + user.getSessionToken());
                            taskSuccess = true;
                            /* remaining work in onPostExecute since new Asynctask to be created and started in GUI thread*/
                        } else {
                            // The token could not be validated.
                            Log.d("D_FB_VERIF", "parseuser become - returned user null");
                            unexpectedError = true;
                        }
                    }
                    catch (ParseException e){
                        Utility.LogoutUtility.checkAndHandleInvalidSession(e);
                        Log.d("D_FB_VERIF", "parseuser become - parse exception");
                        if(e.getCode() == ParseException.CONNECTION_FAILED){
                            networkError = true;
                        }
                        else {
                            unexpectedError = true;
                        }
                    }
                }
                else{
                    Log.d("D_FB_VERIF", "verifyCode error");
                    unexpectedError = true;
                }
            } catch (ParseException e) {
                Utility.LogoutUtility.checkAndHandleInvalidSession(e);
                Log.d("D_FB_VERIF", "network error with message " + e.getMessage() + " code "  + e.getCode());
                if(e.getCode() == ParseException.CONNECTION_FAILED){
                    networkError = true;
                }
                else {
                    unexpectedError = true;
                }
                e.printStackTrace();
            }
            Log.d("D_FB_VERIF", "background : returning null");
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



    private class GetIdTokenTask extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... params) {
            String accountName = Plus.AccountApi.getAccountName(mGoogleApiClient);
            Account account = new Account(accountName, GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE);
            String SERVER_CLIENT_ID = "838906570879-nujge366mj36s29elltobjnehh9e1a5j.apps.googleusercontent.com";

            List<String> scopeList = Arrays.asList(new String[]{
                    "https://www.googleapis.com/auth/plus.login",
                    "https://www.googleapis.com/auth/userinfo.email"
            });

            //String scope = String.format("audience:server:client_id:%s:api_scope:%s", SERVER_CLIENT_ID, TextUtils.join(" ", scopeList));
            String scope = String.format("audience:server:client_id:%s", SERVER_CLIENT_ID);
            //   String scopes = "oauth2:server:client_id:" + SERVER_CLIENT_ID; // Not the app's client ID.


            String scopesString = Scopes.PLUS_LOGIN;
          /*
            String scopes = "oauth2:server:client_id:" + Consts.GOOGLE_PLUS_SERVER_CLIENT_ID + ":api_scope:" + scopesString;
            OR
            String scopes = "audience:server:client_id:" + Consts.GOOGLE_PLUS_SERVER_CLIENT_ID;*/

            // .addScope(new Scope("https://www.googleapis.com/auth/userinfo.email"))
            //.addScope(new Scope("https://www.googleapis.com/auth/plus.login"))


            try {
                return GoogleAuthUtil.getToken(getApplicationContext(), account, scope);
            } catch (UserRecoverableAuthException e) {
                // Requesting an authorization code will always throw
                // UserRecoverableAuthException on the first call to GoogleAuthUtil.getToken
                // because the user must consent to offline access to their data.  After
                // consent is granted control is returned to your activity in onActivityResult
                // and the second call to GoogleAuthUtil.getToken will succeed.
                startActivityForResult(e.getIntent(), RC_SIGN_IN);
                return null;
            } catch (IOException e) {
                Log.e(TAG, "Error retrieving ID token.", e);
                return null;
            } catch (GoogleAuthException e) {
                Log.e(TAG, "Error retrieving ID token.", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            Log.i(TAG, "ID token: " + result);
            if (result != null) {
                //Utility.toast("got Token");
                // Successfully retrieved ID Token
                // ...
            } else {
                // There was some error getting the ID Token
                // ...
            }
        }
    }

        class RetrieveTokenTask extends AsyncTask<String, Void, String> {

            @Override
            protected String doInBackground(String... params) {
                String accountName = Plus.AccountApi.getAccountName(mGoogleApiClient);;
                String scopes = "oauth2:profile email";
                String token = null;

                Log.i(TAG, "Access token:  starting...." );
                try {
                    token = GoogleAuthUtil.getToken(getApplicationContext(), accountName, scopes);
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage());
                } catch (UserRecoverableAuthException e) {
                    startActivityForResult(e.getIntent(), RC_SIGN_IN);
                } catch (GoogleAuthException e) {
                    Log.e(TAG, e.getMessage());
                }
                return token;
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                Log.i(TAG, "Access token: " + s);
            }
        }


}