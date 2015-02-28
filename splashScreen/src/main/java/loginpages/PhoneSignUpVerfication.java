package loginpages;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.parse.LogInCallback;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseInstallation;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import additionals.SmsListener;
import BackGroundProcesses.MemberList;
import joinclasses.JoinedHelper;
import library.UtilString;
import notifications.AlarmReceiver;
import notifications.NotificationGenerator;
import trumplab.textslate.R;
import trumplabs.schoolapp.Application;
import trumplabs.schoolapp.Constants;
import trumplabs.schoolapp.MainActivity;
import utility.Config;
import utility.Queries;
import utility.SessionManager;
import utility.Tools;
import utility.Utility;

/**
 * Created by ashish on 26/2/15.
 */
public class PhoneSignUpVerfication extends ActionBarActivity {
    EditText verificationCodeET;
    public static Button verifyButton;
    static ProgressDialog pdialog;

    static String verificationCode;
    static Context activityContext;
    static Boolean isLogin;

    static Boolean manualVerifyOngoing = true; //NOT used differently handle dialog when auto verify & manual verify is triggered

    private static CountDownTimer countDownTimer;
    TextView timerTV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signup_verification);

        activityContext = this;

        verificationCodeET = (EditText) findViewById(R.id.verificationCode);
        timerTV = (TextView) findViewById(R.id.timerText);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if(getIntent() != null && getIntent().getExtras() != null) {
            isLogin = getIntent().getExtras().getBoolean("login");
        }

        countDownTimer = new MyCountDownTimer(1*60*1000, 1000); //5 minutes, tick every second
        countDownTimer.start();
        timerTV.setText("1 : 00");
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.phone_verification, menu);
        return true;
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.verify:
                verificationCode = verificationCodeET.getText().toString();
                if(UtilString.isBlank(verificationCode)){
                    Utility.toast("Please enter the verification code");
                }
                else {
                    manualVerifyOngoing = true;
                    pdialog = new ProgressDialog(activityContext);
                    pdialog.setCancelable(false);
                    pdialog.setMessage("Please Wait...");
                    pdialog.show();

                    VerifyCodeTask verifyCodeTask = new VerifyCodeTask(verificationCode);
                    verifyCodeTask.execute();
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
        countDownTimer.cancel();
        pdialog = new ProgressDialog(PhoneSignUpVerfication.activityContext);
        pdialog.setCancelable(false);
        pdialog.setMessage("Please Wait...");
        pdialog.show();
        Log.d("DEBUG_SMS_LISTENER", "triggering PhoneSignUpVerfication.VerifyCodeTask");
        VerifyCodeTask verifyCodeTask = new VerifyCodeTask(code);
        verifyCodeTask.execute();
    }

    public void onBackPressed() {
        SmsListener.unRegister(); //important so that it doesnot trigger when out of context
        super.onBackPressed();
    }

    public class MyCountDownTimer extends CountDownTimer{
        public  MyCountDownTimer(long startTime, long interval){
            super(startTime, interval);
        }

        @Override
        public void onFinish(){
            SmsListener.unRegister(); //stop listening for new messages
            timerTV.setText("Sorry! Unable to verify automatically. Please enter code manually");
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
                String model = "NA";
                if (android.os.Build.MODEL != null)
                    model = android.os.Build.MODEL;
                param.put("modal", model);
                param.put("os", "ANDROID");

                param.put("name", PhoneSignUpName.displayName);

                if (PhoneSignUpName.role.equals("teacher")) {
                    param.put("school", PhoneSignUpSchool.schoolName);
                }

                param.put("role", PhoneSignUpName.role);

                String title = PhoneSignUpName.title;
                if (title != null && title.equals("Mr.")) {
                    param.put("sex", "M");
                } else {
                    param.put("sex", "F");
                }
            }
            else{
                param.put("number", PhoneLoginPage.phoneNumber);
            }

            try {
                HashMap<String, Object> result = ParseCloud.callFunction("verifyCode", param);
                Boolean success = (Boolean) result.get("flag");
                String sessionToken = (String) result.get("sessionToken");
                if(success != null && success && sessionToken != null){
                    ParseUser.becomeInBackground(sessionToken, new LogInCallback() {
                        public void done(ParseUser user, ParseException e) {
                            if(e == null) {
                                if (user != null) {
                                    if(isLogin){
                                        PostLoginTask postLoginTask = new PostLoginTask(user);
                                        postLoginTask.execute();
                                    }
                                    else {
                                        SessionManager session = new SessionManager(Application.getAppContext());
                                        session.setSignUpAccount();

                                        // The current user is now set to user. Do registration in default class
                                        Log.d("DEBUG_SIGNUP_VERIFICATION", "calling storeSchoolInBackground");
                                        StoreSchoolInBackground storeSchoolInBackground = new StoreSchoolInBackground();
                                        storeSchoolInBackground.execute();
                                    }
                                } else {
                                    // The token could not be validated.
                                    loginError = true;
                                }
                            }
                            else{
                                Log.d("Network error", "verify error");
                                loginError = true;
                            }
                        }
                    });
                }
                else{
                    Log.d("DEBUG_SIGNUP_VERIFICATION", "verify error");
                    verifyError = true;
                }
            } catch (ParseException e) {
                Log.d("DEBUG_SIGNUP_VERIFICATION", "network error with message " + e.getMessage() + " code "  + e.getCode());
                if(e.getMessage().equals("USER_DOESNOT_EXISTS")){
                    userDoesNotExistsError = true;
                }
                else if(e.getMessage().equals("USER_ALREADY_EXISTS")){
                    userAlreadyExistsError = true;
                }
                else {
                    networkError = true;
                }
                e.printStackTrace();
                return null;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result){

            if(networkError || verifyError || loginError || userAlreadyExistsError || userDoesNotExistsError){
                if(pdialog != null && manualVerifyOngoing){
                    pdialog.dismiss();
                    manualVerifyOngoing = false;
                }
            }
            if(networkError){
                Utility.toast("Oops ! Network Error");
            }
            else if(verifyError){
                Utility.toast("Wrong verification code");
            }
            else if(loginError){
                Utility.toast("Error logging in");
            }
            else if(userAlreadyExistsError){
                Utility.toast("This username already exists. Please try logging in");
            }
            else if(userDoesNotExistsError){
                Utility.toast("This user account does not exist. Please try signing up");
            }
        }
    }


    static class PostLoginTask extends  AsyncTask<Void, Void, Void> {
        ParseUser user;

        public PostLoginTask(ParseUser u) {
            user = u;
        }

        protected Void doInBackground(Void... params) {
            Utility.updateCurrentTime(user);
            Queries query = new Queries();
            try {
                query.refreshChannels();
            } catch (ParseException e1) {
            }

            LoginPage.setDefaultGroupCheck(user);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (pdialog != null && manualVerifyOngoing) {
                pdialog.dismiss();
                manualVerifyOngoing = false;
            }

            //Switching to MainActivity
            Intent intent = new Intent(activityContext, MainActivity.class);
            activityContext.startActivity(intent);
        }
    }

    static class StoreSchoolInBackground extends AsyncTask<Void, Void, Void>
    {
        ParseUser currentUser;
        @Override
        protected Void doInBackground(Void... params) {

            currentUser = ParseUser.getCurrentUser(); //won't be null

            if(currentUser == null) return null;
            /*//storing school on database server
            try {
                if (role.equals(Constants.TEACHER)) {
                    String schoolId = School.getSchoolObjectId(school);
                    if (schoolId != null)
                        user.put("school", schoolId);
                }
            } catch (ParseException e2) {
            }*/


            Utility.updateCurrentTime(currentUser);


            //storing username in parseInstallation table
            ParseInstallation installation = ParseInstallation.getCurrentInstallation();
            installation.put("username", currentUser.getUsername());
            List<String> channelList = new ArrayList<String>();
            installation.put("channels", channelList);
            try {
                if(currentUser.getUsername() != null)
                    Log.d("Install", currentUser.getUsername());
                else
                    Log.d("Install", "username null");


                installation.save();
                Log.d("DEBUG_SIGNUP_VERIFICATION", "installation save success");
            } catch (ParseException e1) {
                Log.d("DEBUG_SIGNUP_VERIFICATION", "installation save FAILED");
                System.out.println("Install failed not saved");
                e1.getCode();
                e1.getMessage();
                e1.printStackTrace();
            }

              /*
                * Joining default groups
                */
            joinDefaultGroup joinGroup = new joinDefaultGroup();
            joinGroup.execute();

            return null;
        }


        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if(pdialog != null && manualVerifyOngoing){
                pdialog.dismiss();
                manualVerifyOngoing = false;
            }
            if(currentUser == null) return; //won't happen as called after successful login

            //here create welcome notification and message
            if(currentUser.getString("role").equals("teacher")){
                NotificationGenerator.generateNotification(activityContext, Constants.WELCOME_MESSAGE_TEACHER, Constants.DEFAULT_NAME, Constants.NORMAL_NOTIFICATION, Constants.INBOX_ACTION);
                AlarmReceiver.generateLocalMessage(Constants.WELCOME_MESSAGE_TEACHER, Constants.DEFAULT_NAME, currentUser);
            }
            else if(currentUser.getString("role").equals("parent")){
                NotificationGenerator.generateNotification(activityContext, Constants.WELCOME_MESSAGE_PARENT, Constants.DEFAULT_NAME, Constants.NORMAL_NOTIFICATION, Constants.INBOX_ACTION);
                AlarmReceiver.generateLocalMessage(Constants.WELCOME_MESSAGE_PARENT, Constants.DEFAULT_NAME, currentUser);
            }
            else{
                NotificationGenerator.generateNotification(activityContext, Constants.WELCOME_MESSAGE_STUDENT, Constants.DEFAULT_NAME, Constants.NORMAL_NOTIFICATION, Constants.INBOX_ACTION);
                AlarmReceiver.generateLocalMessage(Constants.WELCOME_MESSAGE_STUDENT, Constants.DEFAULT_NAME, currentUser);
            }

            //Switching to MainActivity
            Intent intent = new Intent(activityContext, LoginPanda.class);
            activityContext.startActivity(intent);
        }
    }

    static class joinDefaultGroup extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            ParseUser user = ParseUser.getCurrentUser();
            if (user != null) {

                String childName = user.getString("name");
                String role = user.getString("role");
                String code = null;
                if (role.equals("teacher")) {
                    code = Config.defaultTeacherGroupCode;
                } else {
                    code = Config.defaultParentGroupCode;
                }
                childName = UtilString.parseString(childName);

                JoinedHelper.joinClass(code, childName, true);
            }
            return null;
        }
    }
}