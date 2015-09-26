package loginpages;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
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

import com.facebook.CallbackManager;
import com.facebook.FacebookSdk;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.parse.ParseAnalytics;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseInstallation;
import com.parse.ParseUser;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import additionals.SmsListener;
import baseclasses.MyActionBarActivity;
import library.UtilString;
import profileDetails.ProfilePage;
import trumplab.textslate.R;
import trumplabs.schoolapp.Constants;
import utility.Config;
import utility.SessionManager;
import utility.Utility;

/**
 * Created by ashish on 26/2/15.
 */
public class PhoneSignUpName extends MyActionBarActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {
    Context activityContext;

    EditText displayNameET;
    EditText phoneNumberET;

    String role = "";
    String displayName = "";
    String phoneNumber = "";
    static Location mLastLocation = null;

    ProgressDialog pdialog;

    GoogleApiClient mLocationGoogleApiClient = null;
    boolean callLocationApi = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityContext = this;

        //intializing facebook sdk
        FacebookSdk.sdkInitialize(getApplicationContext());

        setContentView(R.layout.signup_name);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        displayNameET = (EditText) findViewById(R.id.displaynameid);
        phoneNumberET = (EditText) findViewById(R.id.phoneid);

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

    void resetFields(){
        displayName = "";
        phoneNumber = "";
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
            final TextView nextButton = (TextView) view.findViewById(R.id.nextButton);
            TextView header = (TextView) view.findViewById(R.id.header);

            header.setText(msg);

            nextButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                    Intent nextIntent = new Intent(PhoneSignUpName.this, PhoneSignUpVerfication.class);
                    //nextIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    nextIntent.putExtra("purpose", PhoneSignUpVerfication.SIGN_UP);
                    nextIntent.putExtra("phoneNumber", phoneNumber);
                    nextIntent.putExtra("role", role);
                    nextIntent.putExtra("displayName", displayName);

                    GenerateVerificationCode generateVerificationCode = new GenerateVerificationCode(phoneNumber);
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
    }

    protected synchronized void buildLocationGoogleApiClient() {
        mLocationGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        if(Config.SHOWLOG) Log.d("DEBUG_LOCATION", "onConnected() entered, first take last known location, just in case that gps location is not received");

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
    }

    @Override
    public void onConnectionSuspended(int result) {
        // At least one of the API client connect attempts failed
        // No client is connected

        if(Config.SHOWLOG) Log.d("DEBUG_LOCATION", "onConnectionSuspended()");

    }

    /********* end of Location Detection methods ******/

    /****************** class GenerateVerificationCode **********************/
    public static class GenerateVerificationCode extends AsyncTask<Void, Void, Void> {
        Boolean isValid = false;
        Boolean success = false;
        Boolean networkError = false;

        String number;
        public GenerateVerificationCode(String num){ //id identifies the caller, num the phone number
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
                    if(PhoneSignUpVerfication.myWeakReference != null && PhoneSignUpVerfication.myWeakReference.get() != null) {
                        PhoneSignUpVerfication instance = PhoneSignUpVerfication.myWeakReference.get();
                        instance.showError(errorMsg, true);

                        if (Config.DETECT_INVALID_NUMBER) { //if invalid number detection is on
                            instance.hideVerifyOption();
                        }
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
                if(PhoneSignUpVerfication.myWeakReference != null && PhoneSignUpVerfication.myWeakReference.get() != null) {
                    PhoneSignUpVerfication instance = PhoneSignUpVerfication.myWeakReference.get();
                    instance.showError(errorMsg, true);
                    instance.showResendAction();
                }
            }
        }
    }

    public static class FBVerifyLoginTask extends AsyncTask<Void, Void, Void> {
        Boolean networkError = false; //parse exception
        Boolean unexpectedError = false;
        Boolean taskSuccess = false;

        String token;
        String fbUserId;
        ProgressDialog progressDialog;

        //response
        String flag;

        public FBVerifyLoginTask(String token, String fbUserId, ProgressDialog progressDialog){//code to verify. Number will be taken from relevant activity
            this.token = token;
            this.fbUserId = fbUserId;
            this.progressDialog = progressDialog;
        }

        @Override
        protected Void doInBackground(Void... par) {
            if(Config.SHOWLOG) Log.d("D_FB_VERIF", "FBVerifyLoginTask : doInBackground");

            //setting parameters
            HashMap<String, Object> params = new HashMap<String, Object>();

            params.put("accessToken", token);

            //appInstallation params - devicetype, installationId
            params.put("deviceType", "android");
            params.put("installationId", ParseInstallation.getCurrentInstallation().getInstallationId());

            //Sessions save params - os, model, location(lat, long)
            PhoneSignUpVerfication.fillDetailsForSession(true, params);

            try {
                if(Config.SHOWLOG) Log.d("D_FB_VERIF", "appEnter : calling");
                Utility.saveParseInstallationIfNeeded();
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

                            Log.d("D_FB_VERIF", "fbUserId : " + fbUserId);

                            if(!UtilString.isBlank(fbUserId))
                            {
                                final String url = "https://graph.facebook.com/"  + fbUserId + "/picture?type=large";

                                Log.d("D_FB_VERIF", "url : " + url);


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
                ParseUser user = ParseUser.getCurrentUser();
                //If user has joined any class then locally saving it in session manager
                if(user != null && user.getList(Constants.JOINED_GROUPS) != null && user.getList(Constants.JOINED_GROUPS).size() >0) {
                    SessionManager.getInstance().setHasUserJoinedClass();
                }

                PhoneSignUpVerfication.PostLoginTask postLoginTask = new PhoneSignUpVerfication.PostLoginTask(user, progressDialog);
                postLoginTask.execute();
            }

            if(!taskSuccess){
                if(progressDialog != null){
                    progressDialog.dismiss();
                }
                //hide progress bar
            }

            if(networkError){
                Utility.toast("Connection failure", true);
            }
            else if(unexpectedError){
                Utility.toast("Oops ! some error occured.", true);

                if(progressDialog != null){
                    progressDialog.dismiss();
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
}