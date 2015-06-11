package loginpages;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.parse.GetCallback;
import com.parse.ParseAnalytics;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseSession;
import com.parse.ParseUser;

import java.util.HashMap;
import java.util.Map;

import additionals.SmsListener;
import fr.castorflex.android.smoothprogressbar.SmoothProgressBar;
import library.UtilString;
import trumplab.textslate.R;
import trumplabs.schoolapp.Application;
import trumplabs.schoolapp.Constants;
import trumplabs.schoolapp.MainActivity;
import utility.SessionManager;
import utility.Tools;
import utility.Utility;

/**
 * Created by ashish on 26/2/15.
 */
public class PhoneSignUpVerfication extends ActionBarActivity {
    public static EditText verificationCodeET;
    static ProgressDialog pdialog;
    static SmoothProgressBar smoothProgressBar;
    static TextView errorMsgTV;
    static TextView resendActionTV;

    static String verificationCode;
    static Context activityContext;
    static Boolean isLogin;
    static Menu menu;

    private static CountDownTimer countDownTimer = null;
    static TextView timerTV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signup_verification);

        activityContext = this;

        verificationCodeET = (EditText) findViewById(R.id.verificationCode);
        timerTV = (TextView) findViewById(R.id.timerText);
        smoothProgressBar = (SmoothProgressBar) findViewById(R.id.progressHeader);
        errorMsgTV = (TextView) findViewById(R.id.errorMessage);
        resendActionTV = (TextView) findViewById(R.id.resendAction);
        TextView header = (TextView) findViewById(R.id.header);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if(getIntent() != null && getIntent().getExtras() != null) {
            isLogin = getIntent().getExtras().getBoolean("login");
        }


        String headerText;
        if(isLogin)
            headerText = "+91"+ PhoneLoginPage.phoneNumber ;
        else
            headerText =  "+91" + PhoneSignUpName.phoneNumber;

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

                if(isLogin){
                    PhoneSignUpName.GenerateVerificationCode generateVerificationCode = new PhoneSignUpName.GenerateVerificationCode(2, PhoneLoginPage.phoneNumber);
                    generateVerificationCode.execute();
                }
                else{
                    PhoneSignUpName.GenerateVerificationCode generateVerificationCode = new PhoneSignUpName.GenerateVerificationCode(2, PhoneSignUpName.phoneNumber);
                    generateVerificationCode.execute();
                }
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
                verificationCode = verificationCodeET.getText().toString();
                if(UtilString.isBlank(verificationCode) || verificationCode.length() != 4){
                    Utility.toast("Please enter the 4-digit verification code");
                }
                else if(Utility.isInternetExist(this)) {
                    Tools.hideKeyboard(this);
                    pdialog = new ProgressDialog(activityContext);
                    pdialog.setCancelable(true);
                    pdialog.setCanceledOnTouchOutside(false);
                    pdialog.setMessage("Please Wait...");
                    pdialog.show();

                    VerifyCodeTask verifyCodeTask = new VerifyCodeTask(verificationCode);
                    verifyCodeTask.execute();
                }
                else {
                    Utility.toast("Check your Internet connection");
                }
                break;

            case android.R.id.home:
                onBackPressed();
                break;

            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public static void smsListenerVerifyTask(String code){
        pdialog = new ProgressDialog(PhoneSignUpVerfication.activityContext);
        pdialog.setCancelable(false);
        pdialog.setMessage("Please Wait...");
        pdialog.show();

        //countDownTimer.cancel();
        //smoothProgressBar.setVisibility(View.GONE);

        Log.d("DEBUG_SMS_LISTENER", "triggering PhoneSignUpVerfication.VerifyCodeTask");
        VerifyCodeTask verifyCodeTask = new VerifyCodeTask(code);
        verifyCodeTask.execute();
    }

    public static void showError(String error, boolean timerCancel){
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
            Log.d("DEBUG_SIGNUP_VER", "Can't show error as error textview NULL");
        }
    }

    public static void hideVerifyOption(){
        MenuItem verify = menu.findItem(R.id.verify);
        if(verify != null){
            verify.setVisible(false);
        }
    }

    static void showResendAction(){
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

    public static class VerifyCodeTask extends AsyncTask<Void, Void, Void> {
        Boolean loginError = false; //session code login status
        Boolean networkError = false; //parse exception
        Boolean verifyError = false; //code verification status
        Boolean userDoesNotExistsError = false; //user doesnot exist - during login
        Boolean userAlreadyExistsError = false; //user already exists - during sigup
        Boolean unexpectedError = false;

        Boolean taskSuccess = false;

        String code;

        public VerifyCodeTask(String tcode){//code to verify. Number will be taken from relevant activity
            code = tcode;
        }

        @Override
        protected Void doInBackground(Void... params) {
            //setting parameters
            HashMap<String, Object> param = new HashMap<String, Object>();
            param.put("code", Integer.parseInt(code));


            if(!isLogin) {
                param.put("number", PhoneSignUpName.phoneNumber);
                param.put("name", /*PhoneSignUpName.title + " " + */ PhoneSignUpName.displayName);
                param.put("role", PhoneSignUpName.role);
            }
            else{
                param.put("number", PhoneLoginPage.phoneNumber);
            }

            try {
                HashMap<String, Object> result = ParseCloud.callFunction("verifyCod", param);
                Boolean success = (Boolean) result.get("flag");
                String sessionToken = (String) result.get("sessionToken");
                if(success != null && success && sessionToken != null){
                    try{
                        ParseUser user = ParseUser.become(sessionToken);
                        if (user != null) {
                            taskSuccess = true;
                            if(isLogin){
                                PostLoginTask postLoginTask = new PostLoginTask(user);
                                postLoginTask.execute();
                            }
                            else {
                                SessionManager session = new SessionManager(Application.getAppContext());
                                session.setSignUpAccount();

                                // The current user is now set to user. Do registration in default class
                                PostSignUpTask postSignUpTask = new PostSignUpTask(user);
                                postSignUpTask.execute();
                            }
                        } else {
                            // The token could not be validated.
                            Log.d("DEBUG_SIGNUP_VERIFICATION", "parseuser become - returned user null");
                            loginError = true;
                        }
                    }
                    catch (ParseException e){
                        Log.d("DEBUG_SIGNUP_VERIFICATION", "parseuser become - parse exception");
                        if(e.getCode() == ParseException.CONNECTION_FAILED){
                            networkError = true;
                        }
                        else {
                            loginError = true;
                        }
                    }
                }
                else{
                    Log.d("DEBUG_SIGNUP_VERIFICATION", "verifyCode error");
                    verifyError = true;
                }
            } catch (ParseException e) {
                Log.d("DEBUG_SIGNUP_VERIFICATION", "network error with message " + e.getMessage() + " code "  + e.getCode());
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
            Log.d("DEBUG_SIGNUP_VERIFICATION", "background : returning null");
            return null;
        }

        @Override
        protected void onPostExecute(Void result){
            Log.d("DEBUG_SIGNUP_VERIFICATION", "onPostExecute() of VerifyCodeTask with taskSuccess " + taskSuccess);
            if(!taskSuccess){
                if(pdialog != null){
                    pdialog.dismiss();
                }
            }
            if(networkError){
                Utility.toastLong("Connection failure");
                showError("Unable to establish connection. Please try again", false);
                showResendAction();
            }
            else if(unexpectedError){
                Utility.toastLong("Oops ! some error occured.");
                showError( "Some unexpected error occured. Please try again", false);
                showResendAction();
            }
            else if(verifyError){
                Utility.toastLong("Wrong verification code");
                showError("Wrong verification code.\nPlease re-enter code and try again", false);
            }
            else if(loginError){
                Utility.toastLong("Error logging in"); //code was verified but login unsuccessful
                showError("Some unexpected error occurred while logging in.\nTry again", true);
                showResendAction();
            }
            else if(userAlreadyExistsError){
                Utility.toastLong("This number is already in use.\nPlease recheck you number");
                //take back ot Login Page
                Intent nextIntent = new Intent(Application.getAppContext(), PhoneSignUpName.class);
                nextIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                Application.getAppContext().startActivity(nextIntent);
                //showError("This number is already in use. Please try logging in");
            }
            else if(userDoesNotExistsError){
                Utility.toastLong("No account for this number exists.\nPlease recheck you number");
                Intent nextIntent = new Intent(Application.getAppContext(), PhoneLoginPage.class);
                nextIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                Application.getAppContext().startActivity(nextIntent);
                //showError("No account for this number exists. Please try signing up");
            }
        }
    }


    static class PostLoginTask extends  AsyncTask<Void, Void, Void> {
        ParseUser user;

        public PostLoginTask(ParseUser u) {
            user = u;
        }

        protected Void doInBackground(Void... params) {
            fillDetailsInSessionInBackground(true);
            Utility.updateCurrentTime(user);

            Utility.setNewIdFlagInstallation();
            boolean installationStatus = Utility.checkParseInstallation();
            if(installationStatus){
                Log.d("DEBUG_SIGNUP_VERIFICATION", "PostLoginTask : installation save SUCCESS");
            }
            else{
                Log.d("DEBUG_SIGNUP_VERIFICATION", "PostLoginTask : installation save FAILED");
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (pdialog != null) {
                pdialog.dismiss();
            }


            //Switching to MainActivity
            Intent intent = new Intent(activityContext, MainActivity.class);
            activityContext.startActivity(intent);

            //Analytics to measure total successful logins
            Map<String, String> dimensions = new HashMap<String, String>();
            dimensions.put("Login", "Total Login");
            ParseAnalytics.trackEvent("Login", dimensions);

        }
    }

    static class PostSignUpTask extends AsyncTask<Void, Void, Void>
    {
        ParseUser currentUser;
        public PostSignUpTask(ParseUser u) {
            currentUser = u;
        }
        @Override
        protected Void doInBackground(Void... params) {

            fillDetailsInSessionInBackground(false);
            
            Utility.updateCurrentTime(currentUser);

            Utility.setNewIdFlagInstallation();
            boolean installationStatus = Utility.checkParseInstallation();
            if(installationStatus){
                Log.d("DEBUG_SIGNUP_VERIFICATION", "PostSignUpTask : installation save SUCCESS");
            }
            else{
                Log.d("DEBUG_SIGNUP_VERIFICATION", "PostSignUpTask : installation save FAILED");
            }

            return null;
        }


        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if(pdialog != null){
                pdialog.dismiss();
            }
            if(currentUser == null) return; //won't happen as called after successful login

            //variable storing that its first time app <signup>user
            Constants.IS_SIGNUP = true;

            Intent intent = new Intent(activityContext, MainActivity.class);
            intent.putExtra("flag", "SIGNUP");
            activityContext.startActivity(intent);


            //Analytics to measure total successful signups
            Map<String, String> dimensions = new HashMap<String, String>();
            dimensions.put("Signup", "Total Signup");
            ParseAnalytics.trackEvent("Signup", dimensions);
        }
    }

    static void fillDetailsInSessionInBackground(final boolean login){
        ParseSession.getCurrentSessionInBackground(new GetCallback<ParseSession>() {
            @Override
            public void done(ParseSession parseSession, ParseException e) {
                if(e == null){
                    String model = "NA";
                    if (android.os.Build.MODEL != null)
                        model = android.os.Build.MODEL;
                    parseSession.put("model", model);
                    parseSession.put("os", "ANDROID");

                    if(login){
                        if (PhoneLoginPage.mLastLocation != null) {
//                            Log.d("DEBUG_SIGNUP_VERIFICATION", "login putting location in session");
                            parseSession.put("lat", PhoneLoginPage.mLastLocation.getLatitude());
                            parseSession.put("long", PhoneLoginPage.mLastLocation.getLongitude());
                        }
                    }
                    else {
                        if (PhoneSignUpName.mLastLocation != null) {
//                            Log.d("DEBUG_SIGNUP_VERIFICATION", "signup putting location in session");
                            parseSession.put("lat", PhoneSignUpName.mLastLocation.getLatitude());
                            parseSession.put("long", PhoneSignUpName.mLastLocation.getLongitude());
                        }
                    }

                    parseSession.saveEventually();
                }
                else{
                    e.printStackTrace();
                }
            }
        });
    }
}
