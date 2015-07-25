package loginpages;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
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
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.parse.ParseAnalytics;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseInstallation;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import additionals.SmsListener;
import baseclasses.MyActionBarActivity;
import library.UtilString;
import trumplab.textslate.R;
import utility.Config;
import utility.Utility;

/**
 * Created by ashish on 26/2/15.
 */
public class PhoneSignUpName extends MyActionBarActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {
    EditText displayNameET;
    EditText phoneNumberET;

    static String role = "";
    static String displayName = "";
    static String phoneNumber = "";
    static ProgressDialog pdialog;

    static GoogleApiClient mGoogleApiClient = null;
    static Location mLastLocation = null;
    CallbackManager callbackManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //intializing facebook sdk
        FacebookSdk.sdkInitialize(getApplicationContext());
        callbackManager = CallbackManager.Factory.create();

        setContentView(R.layout.signup_name);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        displayNameET = (EditText) findViewById(R.id.displaynameid);
        phoneNumberET = (EditText) findViewById(R.id.phoneid);
        LoginButton loginButton = (LoginButton) findViewById(R.id.login_button);
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
                Log.d("FB_LOGin", "logged in");

                if(AccessToken.getCurrentAccessToken() != null){
               //     RequestData();


                }
            }

            @Override
            public void onCancel() {
                Log.d("FB_LOGin", "logged cancelled");
            }

            @Override
            public void onError(FacebookException exception) {
                Log.d("FB_LOGin", "logged in");
            }
        });
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
                        //    http://stackoverflow.com/questions/19855072/android-get-facebook-profile-picture


                        String token = AccessToken.getCurrentAccessToken().getToken();

                        if(token != null && !UtilString.isBlank(role))
                        {
                            HashMap<String, Object> param = new HashMap<String, Object>();
                            param.put("role", role);
                            param.put("token", token);

                            String emailId = Utility.getAccountEmail();
                            Log.d("__Y", "got email from account " + emailId);
                            if(emailId != null){
                                param.put("emailId", emailId);
                            }

                            //appInstallation params - devicetype, installationId
                            param.put("deviceType", "android");
                            param.put("installationId", ParseInstallation.getCurrentInstallation().getInstallationId());

                        }


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
                .build();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d("DEBUG_LOCATION", "onConnected() entered, first take last known location, just in case that gps location is not received");

        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if(mLastLocation != null){
            Log.d("DEBUG_LOCATION", "onConnected() entered, last known location=" + String.valueOf(mLastLocation.getLatitude())
                    + ", " + String.valueOf(mLastLocation.getLongitude()));
        }

        createLocationRequest();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // At least one of the API client connect attempts failed
        // No client is connected
        Log.d("DEBUG_LOCATION", "onConnectionFailed()");
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

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        Intent intent = new Intent(PhoneSignUpName.this, Signup.class);
        startActivity(intent);
    }
}