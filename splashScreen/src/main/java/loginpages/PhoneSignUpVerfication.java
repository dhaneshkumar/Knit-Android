package loginpages;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Html;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.appsflyer.AppsFlyerLib;
import com.parse.ParseAnalytics;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseInstallation;
import com.parse.ParseUser;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import BackGroundProcesses.AsyncTaskProxy;
import additionals.SmsListener;
import baseclasses.MyActionBarActivity;
import fr.castorflex.android.smoothprogressbar.SmoothProgressBar;
import library.UtilString;
import trumplab.textslate.R;
import trumplabs.schoolapp.Application;
import trumplabs.schoolapp.Constants;
import trumplabs.schoolapp.MainActivity;
import trumplabs.schoolapp.Messages;
import trumplabs.schoolapp.Outbox;
import utility.Config;
import utility.SessionManager;
import utility.Tools;
import utility.Utility;

/**
 * Created by ashish on 26/2/15.
 */
public class PhoneSignUpVerfication extends MyActionBarActivity {
    EditText verificationCodeET;
    ProgressDialog pdialog;
    SmoothProgressBar smoothProgressBar;
    TextView errorMsgTV;
    TextView resendActionTV;
    String verificationCode;

    int purpose;
    public final static int SIGN_IN = 0;
    public final static int SIGN_UP = 1;
    public final static int UPDATE_PHONE = 2;

    String phoneNumber;
    String displayName;
    String role;

    Menu menu;

    private CountDownTimer countDownTimer = null;
    TextView timerTV;

    Context activityContext;
    Activity thisActivity;

    public static WeakReference<PhoneSignUpVerfication> myWeakReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if(Config.SHOWLOG) Log.d("DEBUG_LOCATION_LOGIN", "PhoneSignUpVerfication - onCreate()");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.signup_verification);

        activityContext = this;
        thisActivity = this;

        verificationCodeET = (EditText) findViewById(R.id.verificationCode);
        timerTV = (TextView) findViewById(R.id.timerText);
        smoothProgressBar = (SmoothProgressBar) findViewById(R.id.progressHeader);
        errorMsgTV = (TextView) findViewById(R.id.errorMessage);
        resendActionTV = (TextView) findViewById(R.id.resendAction);
        TextView header = (TextView) findViewById(R.id.header);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if(getIntent() != null && getIntent().getExtras() != null) {
            Bundle bundle = getIntent().getExtras();
            purpose = bundle.getInt("purpose");
            if(purpose == SIGN_IN){
                phoneNumber = bundle.getString("phoneNumber");
            }
            else if(purpose == SIGN_UP){
                phoneNumber = bundle.getString("phoneNumber");
                displayName = bundle.getString("displayName");
                role = bundle.getString("role");
            }
            else{
                phoneNumber = bundle.getString("phoneNumber");
            }
        }

        verificationCodeET.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    verify();
                    return true;
                }
                return false;
            }
        });

        String headerText = "+91" + phoneNumber;;
        header.setText(Html.fromHtml(headerText), TextView.BufferType.SPANNABLE);

        //Again send the verification code
        resendActionTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                countDownTimer.start();
                smoothProgressBar.setVisibility(View.VISIBLE);
                errorMsgTV.setVisibility(View.INVISIBLE);
                timerTV.setVisibility(View.VISIBLE);
                resendActionTV.setVisibility(View.GONE);

                PhoneSignUpName.GenerateVerificationCode generateVerificationCode = new PhoneSignUpName.GenerateVerificationCode(phoneNumber);
                generateVerificationCode.execute();
            }
        });

        if(countDownTimer == null) {
            countDownTimer = new MyCountDownTimer(5 * 60 * 1000, 1000); //5 minutes, tick every second
        }
        countDownTimer.start();
        timerTV.setText("5 : 00");
        smoothProgressBar.setVisibility(View.VISIBLE); //keep showing until timeout or error
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.phone_verification, menu);
        this.menu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.verify:
                verify();
                break;

            case android.R.id.home:
                onBackPressed();
                break;

            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void verify(){
        verificationCode = verificationCodeET.getText().toString();
        if(UtilString.isBlank(verificationCode) || verificationCode.length() != 4){
            Utility.toast("Please enter the 4-digit verification code", true);
        }
        else if(Utility.isInternetExist()) {
            Tools.hideKeyboard(this);
            pdialog = new ProgressDialog(activityContext);
            pdialog.setCancelable(true);
            pdialog.setCanceledOnTouchOutside(false);
            pdialog.setMessage("Please Wait...");
            pdialog.show();

            if(purpose == UPDATE_PHONE){
                UpdatePhoneTask updatePhoneTask = new UpdatePhoneTask(verificationCode);
                updatePhoneTask.execute();
            }
            else {
                VerifyCodeTask verifyCodeTask = new VerifyCodeTask(verificationCode);
                verifyCodeTask.execute();
            }
        }
    }

    /* call from main(GUI) thread */
    public void smsListenerVerifyTask(String code){
        pdialog = new ProgressDialog(activityContext);
        pdialog.setCancelable(false);
        pdialog.setMessage("Please Wait...");
        pdialog.show();

        //countDownTimer.cancel();
        //smoothProgressBar.setVisibility(View.GONE);

        if(Config.SHOWLOG) Log.d("DEBUG_SMS_LISTENER", "triggering PhoneSignUpVerfication.VerifyCodeTask");
        if(purpose == UPDATE_PHONE){
            UpdatePhoneTask updatePhoneTask = new UpdatePhoneTask(code);
            updatePhoneTask.execute();
        }
        else {
            VerifyCodeTask verifyCodeTask = new VerifyCodeTask(code);
            verifyCodeTask.execute();
        }
    }

    public void showError(String error, boolean timerCancel){
        if(errorMsgTV != null) {
            errorMsgTV.setText(error);
            errorMsgTV.setVisibility(View.VISIBLE);
            smoothProgressBar.setVisibility(View.GONE);
            if(timerCancel) {
                timerTV.setVisibility(View.GONE);
                countDownTimer.cancel();
            }
        }
        else{
            if(Config.SHOWLOG) Log.d("DEBUG_SIGNUP_VER", "Can't show error as error textview NULL");
        }
    }

    public void hideVerifyOption(){
        if(thisActivity != null){
            thisActivity.finish();
            thisActivity = null;
        }
    }

    void showResendAction(){
        resendActionTV.setVisibility(View.VISIBLE);
    }

    public void onBackPressed() {
        countDownTimer.cancel(); //to prevent multiple instances to simultaneously changing the time text
        SmsListener.unRegister(); //important so that it does not trigger when out of context
        super.onBackPressed();
    }

    public class MyCountDownTimer extends CountDownTimer{
        public  MyCountDownTimer(long startTime, long interval){
            super(startTime, interval);
        }

        @Override
        public void onFinish(){
            SmsListener.unRegister(); //stop listening for new messages
            timerTV.setText("0 : 00");
            showError("The verification code has expired. Please click on resend to retry", true);
            showResendAction();
        }

        @Override
        public void onTick(long millisUntilFinished) {
            long remainingSeconds = millisUntilFinished/1000;
            long minuteValue = remainingSeconds/60;
            long secondValue = remainingSeconds%60;
            String secondValueStr = String.format("%02d", secondValue); //format with left padded zeroes
            timerTV.setText(minuteValue + " : " + secondValueStr);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        myWeakReference = new WeakReference<>(this);
        //no need to make this null in onPause as it is a weak reference, won't hold the resource as such
    }

    private class VerifyCodeTask extends AsyncTask<Void, Void, Void> {
        Boolean loginError = false; //session code login status
        Boolean networkError = false; //parse exception
        Boolean verifyError = false; //code verification status
        Boolean userDoesNotExistsError = false; //user doesnot exist - during login
        Boolean userAlreadyExistsError = false; //user already exists - during sigup
        Boolean unexpectedError = false;

        Boolean taskSuccess = false;

        String code;

        //response
        String flag;

        public VerifyCodeTask(String tcode){//code to verify. Number will be taken from relevant activity
            code = tcode;
        }

        @Override
        protected Void doInBackground(Void... par) {
            //setting parameters
            HashMap<String, Object> params = new HashMap<String, Object>();
            params.put("code", Integer.parseInt(code));

            if(purpose == SIGN_UP) {
                params.put("number", phoneNumber);
                params.put("name", displayName);
                params.put("role", role);
                String emailId = Utility.getAccountEmail();
                if(emailId != null){
                    params.put("email", emailId);
                }
            }
            else{
                params.put("number", phoneNumber);
            }

            //appInstallation params - devicetype, installationId
            params.put("deviceType", "android");
            params.put("installationId", ParseInstallation.getCurrentInstallation().getInstallationId());

            //Sessions save params - os, model, location(lat, long)
            fillDetailsForSession((purpose == SIGN_IN), params);

            try {
                Utility.saveParseInstallationIfNeeded();
                HashMap<String, Object> result = ParseCloud.callFunction("appEnter", params);
                String sessionToken = (String) result.get("sessionToken");
                flag = (String) result.get("flag");

                if(!UtilString.isBlank(sessionToken)){
                    try{
                        if(Config.SHOWLOG) Log.d("D_SIGNUP_VERIF", "parseuser become calling " + ParseUser.getCurrentUser());
                        ParseUser user = ParseUser.become(sessionToken);
                        if (user != null) {
                            //if(Config.SHOWLOG) Log.d("__A", "setting ignoreInvalidSessionCheck to false " + Utility.parseObjectToJson(user));
                            Utility.LogoutUtility.resetIgnoreInvalidSessionCheck();

                            if(Config.SHOWLOG) Log.d("D_SIGNUP_VERIF", "parseuser become - returned user correct with given token=" + sessionToken +", currentsessiontoken=" + user.getSessionToken());
                            taskSuccess = true;
                            /* remaining work in onPostExecute since new Asynctask to be created and started in GUI thread*/
                        } else {
                            // The token could not be validated.
                            if(Config.SHOWLOG) Log.d("D_SIGNUP_VERIF", "parseuser become - returned user null");
                            loginError = true;
                        }
                    }
                    catch (ParseException e){
                        Utility.LogoutUtility.checkAndHandleInvalidSession(e);
                        if(Config.SHOWLOG) Log.d("D_SIGNUP_VERIF", "parseuser become - parse exception");
                        if(e.getCode() == ParseException.CONNECTION_FAILED){
                            networkError = true;
                        }
                        else {
                            loginError = true;
                        }
                    }
                }
                else{
                    if(Config.SHOWLOG) Log.d("D_SIGNUP_VERIF", "verifyCode error");
                    verifyError = true;
                }
            } catch (ParseException e) {
                Utility.LogoutUtility.checkAndHandleInvalidSession(e);
                if(Config.SHOWLOG) Log.d("D_SIGNUP_VERIF", "network error with message " + e.getMessage() + " code "  + e.getCode());
                if(e.getCode() == ParseException.CONNECTION_FAILED){
                    networkError = true;
                }
                if(e.getMessage().equals("USER_DOESNOT_EXISTS")){
                    userDoesNotExistsError = true;
                }
                else if(e.getMessage().equals("USER_ALREADY_EXISTS")){
                    userAlreadyExistsError = true;
                }
                else {
                    unexpectedError = true;
                }
                e.printStackTrace();
            }
            if(Config.SHOWLOG) Log.d("D_SIGNUP_VERIF", "background : returning null");
            return null;
        }

        @Override
        protected void onPostExecute(Void result){
            if(taskSuccess){
                ParseUser user = ParseUser.getCurrentUser();
                //If user has joined any class then locally saving it in session manager
                if(user != null && user.getList(Constants.JOINED_GROUPS) != null && user.getList(Constants.JOINED_GROUPS).size() >0) {
                    SessionManager.getInstance().setHasUserJoinedClass();
                }

                if(flag != null && flag.equals("logIn")){
                    purpose = SIGN_IN;
                }

                if(purpose == SIGN_IN){
                    PostLoginTask postLoginTask = new PostLoginTask(user, pdialog);
                    postLoginTask.execute();
                }
                else {
                    SessionManager.getInstance().setSignUpAccount();

                    // The current user is now set to user. Do registration in default class
                    PostSignUpTask postSignUpTask = new PostSignUpTask(user, pdialog);
                    postSignUpTask.execute();
                }
            }

            if(Config.SHOWLOG) Log.d("D_SIGNUP_VERIF", "onPostExecute() of VerifyCodeTask with taskSuccess " + taskSuccess);
            if(!taskSuccess){
                if(pdialog != null){
                    pdialog.dismiss();
                }
            }
            if(networkError){
                Utility.toast("Connection failure", true);
                showError("Unable to establish connection. Please try again", false);
                showResendAction();
            }
            else if(unexpectedError){
                Utility.toast("Oops ! some error occured.", true);
                showError( "Some unexpected error occured. Please try again", false);
                showResendAction();
            }
            else if(verifyError){
                Utility.toast("Wrong verification code", true);
                showError("Wrong verification code.\nPlease re-enter code and try again", false);
            }
            else if(loginError){
                Utility.toast("Error logging in", true); //code was verified but login unsuccessful
                showError("Some unexpected error occurred while logging in.\nTry again", true);
                showResendAction();
            }
            else if(userAlreadyExistsError){
                Utility.toast("This number is already in use.\nPlease recheck you number", true);
                //take back ot Login Page
                Intent nextIntent = new Intent(Application.getAppContext(), PhoneSignUpName.class);
                nextIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                Application.getAppContext().startActivity(nextIntent);
                //showError("This number is already in use. Please try logging in");
            }
            else if(userDoesNotExistsError){
                Utility.toast("No account for this number exists.\nPlease recheck you number", true);
                Intent nextIntent = new Intent(Application.getAppContext(), PhoneLoginPage.class);
                nextIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                Application.getAppContext().startActivity(nextIntent);
                //showError("No account for this number exists. Please try signing up");
            }
        }
    }

    private class UpdatePhoneTask extends AsyncTask<Void, Void, Void> {
        boolean networkError = false; //parse exception
        boolean taskSuccess = false;
        boolean verifyError = false;

        String code;
        public UpdatePhoneTask(String tcode){//code to verify. Number will be taken from relevant activity
            code = tcode;
        }

        @Override
        protected Void doInBackground(Void... par) {
            //setting parameters
            HashMap<String, Object> params = new HashMap<String, Object>();
            params.put("code", Integer.parseInt(code));
            params.put("number", phoneNumber);

            try {
                Object result = ParseCloud.callFunction("updatePhoneNumber", params);
                if(result instanceof Boolean){
                    taskSuccess = (Boolean) result;
                    if(!taskSuccess){
                        verifyError = true;
                    }
                }
            } catch (ParseException e) {
                Utility.LogoutUtility.checkAndHandleInvalidSession(e);
                if(e.getCode() == ParseException.CONNECTION_FAILED){
                    networkError = true;
                }
                e.printStackTrace();
            }
            if(Config.SHOWLOG) Log.d("__phone", "background : returning null");
            return null;
        }

        @Override
        protected void onPostExecute(Void result){
            if(pdialog != null){
                pdialog.dismiss();
            }
            if(Config.SHOWLOG) Log.d("__phone", "onPostExecute() of VerifyCodeTask with taskSuccess " + taskSuccess);

            if(taskSuccess){
                ParseUser user = ParseUser.getCurrentUser();
                //If user has joined any class then locally saving it in session manager
                if(user != null) {
                    user.put("phone", phoneNumber);
                    user.pinInBackground();
                    Utility.toast("Number verified !");
                    Intent intent = new Intent(PhoneSignUpVerfication.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    return;
                }
            }

            if(networkError){
                Utility.toast("Connection failure", true);
                showError("Unable to establish connection. Please try again", false);
                showResendAction();
            } else if(verifyError){
                Utility.toast("Wrong verification code", true);
                showError("Wrong verification code.\nPlease re-enter code and try again", false);
            }
            else {
                Utility.toast("Oops ! some error occured.", true);
                showError( "Some unexpected error occured. Please try again later", false);
                showResendAction();
            }
        }
    }


    static class PostLoginTask extends AsyncTaskProxy<Void, Void, Void> {
        ParseUser user;
        ProgressDialog progressDialog;

        public PostLoginTask(ParseUser user, ProgressDialog progressDialog) {
            if(Config.SHOWLOG) Log.d("D_FB_VERIF", "PostLoginTask running");
            this.user = user;
            this.progressDialog = progressDialog;
        }

        protected Void doInBackground(Void... params) {
            Utility.updateCurrentTime();
            //SessionManager.getInstance().setInteger(SessionManager.SCHOOL_INPUT_BASE_COUNT, SessionManager.getInstance().getAppOpeningCount());
            //SessionManager.getInstance().setInteger(SessionManager.SCHOOL_INPUT_SHOW_COUNT, 0);

            SessionManager.getInstance().setInteger(SessionManager.PHONE_INPUT_BASE_COUNT, SessionManager.getInstance().getAppOpeningCount() - 1);
            SessionManager.getInstance().setInteger(SessionManager.PHONE_INPUT_SHOW_COUNT, 0);

            if(user != null) {
                user.remove("place_name");
                user.remove("place_area");
                user.pinInBackground();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (progressDialog != null) {
                progressDialog.dismiss();
            }

            //Switching to MainActivity
            Intent intent = new Intent(Application.getAppContext(), MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Application.getAppContext().startActivity(intent);

            //Analytics to measure total successful logins
            Map<String, String> dimensions = new HashMap<String, String>();
            dimensions.put("Login", "Total Login");
            ParseAnalytics.trackEvent("Login", dimensions);

        }
    }

    static class PostSignUpTask extends AsyncTaskProxy<Void, Void, Void>
    {
        ParseUser currentParseUser;
        ProgressDialog progressDialog;

        public PostSignUpTask(ParseUser u, ProgressDialog progressDialog) {
            if(Config.SHOWLOG) Log.d("D_FB_VERIF", "PostSignUpTask running");
            currentParseUser = u;
            this.progressDialog = progressDialog;
        }

        @Override
        protected Void doInBackground(Void... params) {

            Utility.updateCurrentTime();
            //SessionManager.getInstance().setInteger(SessionManager.SCHOOL_INPUT_BASE_COUNT, SessionManager.getInstance().getAppOpeningCount());
            //SessionManager.getInstance().setInteger(SessionManager.SCHOOL_INPUT_SHOW_COUNT, 0);

            SessionManager.getInstance().setInteger(SessionManager.PHONE_INPUT_BASE_COUNT, SessionManager.getInstance().getAppOpeningCount() - 1);
            SessionManager.getInstance().setInteger(SessionManager.PHONE_INPUT_SHOW_COUNT, 0);

            //set inbox fetch flag. We dont need to fetch old messages in this account
            if(currentParseUser != null) {
                SessionManager.getInstance().setBooleanValue(currentParseUser.getUsername() + Constants.SharedPrefsKeys.SERVER_INBOX_FETCHED, true);
                currentParseUser.remove("place_name");
                currentParseUser.remove("place_area");
                currentParseUser.pinInBackground();
            }

            return null;
        }


        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if(progressDialog != null){
                progressDialog.dismiss();
            }

            if(currentParseUser == null) return; //won't happen as called after successful login

            //variable storing that its first time app <signup>user
            Constants.IS_SIGNUP = true;

            //reset all tutorial flags just in case another user signs up using the same mobile(without re-opening app)
            //this case is quite rate but still to be on the safe side
            MainActivity.signUpShowcaseShown = false;
            MainActivity.optionsShowcaseShown = false;
            Messages.responseTutorialShown = false;
            Outbox.responseTutorialShown = false;

            Intent intent = new Intent(Application.getAppContext(), MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            intent.putExtra("flag", "SIGNUP");
            if (currentParseUser.getString("role").equals(Constants.TEACHER))
                intent.putExtra("VIEWPAGERINDEX", 1);
            Application.getAppContext().startActivity(intent);

            //Analytics to measure total successful signups
            Map<String, String> dimensions = new HashMap<String, String>();
            dimensions.put("Signup", "Total Signup");
            ParseAnalytics.trackEvent("Signup", dimensions);

            //sending campaign info to appsflyer analytics

            AppsFlyerLib.setCustomerUserId(currentParseUser.getUsername());

            Map<String, Object> eventValue = new HashMap<String, Object>();
            eventValue.put("USERNAME", currentParseUser.getUsername());
            AppsFlyerLib.trackEvent(Application.getAppContext(), "sign_up", eventValue);

            Log.d("SIGN_UP", "singup done...................");

        }
    }

    static void fillDetailsForSession(final boolean login, HashMap<String, Object> params){
        String model = "NA";
        if (Build.MODEL != null)
            model = android.os.Build.MODEL;

        String os = "Android";
        if(Build.VERSION.RELEASE != null){
            os += " " + Build.VERSION.RELEASE;
        }

        params.put("model", model);
        params.put("os", os);

        if(login){
            if (PhoneLoginPage.mLastLocation != null) {
                params.put("lat", PhoneLoginPage.mLastLocation.getLatitude());
                params.put("long", PhoneLoginPage.mLastLocation.getLongitude());
            }
        }
        else {
            if (PhoneSignUpName.mLastLocation != null) {
                params.put("lat", PhoneSignUpName.mLastLocation.getLatitude());
                params.put("long", PhoneSignUpName.mLastLocation.getLongitude());
            }
        }
    }
}
