package loginpages;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
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

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.parse.ParseAnalytics;
import com.parse.ParseCloud;
import com.parse.ParseException;

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
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signup_name);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        displayNameET = (EditText) findViewById(R.id.displaynameid);
        phoneNumberET = (EditText) findViewById(R.id.phoneid);

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
            Utility.toast("Incorrect Display Name");
        else if (UtilString.isBlank(phoneNumber) || phoneNumber.length() != 10)
            Utility.toast("Incorrect Mobile Number");
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
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
        if (mLastLocation != null) {
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
        Log.d("DEBUG_LOCATION", "buildGoogleApiClient() entered");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d("DEBUG_LOCATION", "onConnected() entered");
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
                    Utility.toast(toastMsg);
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

                Utility.toast(toastMsg);
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