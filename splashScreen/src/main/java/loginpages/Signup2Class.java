package loginpages;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SignUpCallback;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import baseclasses.MyActionBarActivity;
import joinclasses.JoinedHelper;
import joinclasses.School;
import library.UtilString;
import notifications.AlarmReceiver;
import notifications.NotificationGenerator;
import trumplab.textslate.R;
import trumplabs.schoolapp.Application;
import trumplabs.schoolapp.Constants;
import utility.Config;
import utility.Queries;
import utility.Queries2;
import utility.SessionManager;
import utility.Tools;
import utility.Utility;


/**
 * Signup class  : Crate new user
 *
 */
public class Signup2Class extends ActionBarActivity {
    EditText password_etxt;
    EditText repassword_etxt;
    Button create_btn;
    String emailid;
    String school;
    LinearLayout progressLayout;
    LinearLayout signUpLayout;
    String passwordtxt;
    Activity activity;
    private String role;
    String userId;
    String childName;
    String code;
    private String grpName;
    ParseUser user;

    protected void onCreate(android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signup2_layout);

        //Intializing variables
        password_etxt = (EditText) findViewById(R.id.passwordinput);
        repassword_etxt = (EditText) findViewById(R.id.repasswordinput);
        create_btn = (Button) findViewById(R.id.create_button);
        signUpLayout = (LinearLayout) findViewById(R.id.signupLayout);
        progressLayout = (LinearLayout) findViewById(R.id.progressLayout);
        activity = this;
        final String mr = getIntent().getExtras().getString("MR");
        final String name = getIntent().getExtras().getString("name");
        final String phone = getIntent().getExtras().getString("phone");
        role = getIntent().getExtras().getString("role");
        emailid = getIntent().getExtras().getString("email");

        //setting school name
        if (role.equals(Constants.TEACHER))
            school = getIntent().getExtras().getString("school");

        //create button clicked functionality
        create_btn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Utility.isInternetOn(activity)) {

                    //Varifying password match
                    passwordtxt = password_etxt.getText().toString().trim();
                    String repasswordtxt = repassword_etxt.getText().toString().trim();
                    if (UtilString.isBlank(passwordtxt))
                        Utility.toast("Enter Correct Password");
                    else if (UtilString.isBlank(repasswordtxt) || !passwordtxt.equals(repasswordtxt))
                        Utility.toast("Passwords don't match");
                    else {


                        //Hiding the keyboard from screen
                        Tools.hideKeyboard(Signup2Class.this);
                        signUpLayout.setVisibility(View.GONE);
                        progressLayout.setVisibility(View.VISIBLE);

                        // creating a new user and adding fields to itand pushing this to parse cloud
                        user = new ParseUser();
                        if (!UtilString.isBlank(emailid))
                            user.setUsername(emailid);
                        else
                            user.setUsername(phone);
                        user.setPassword(passwordtxt);
                        if (!UtilString.isBlank(emailid))
                            user.setEmail(emailid);
                        user.put("phone", phone);
                        user.put("name", mr + " " + name);
                        user.put("role", role);
                      /*
                        if (role.equals(Constants.TEACHER)) {
                            String schoolId = School.schoolIdExist(school);
                            if (schoolId != null)
                                user.put("school", schoolId);
                        }*/
                        if (mr.equals("Mr.")) {
                            user.put("sex", "M");
                        } else
                            user.put("sex", "F");

                        user.put("OS", "ANDROID");

                        //storing model no of mobile
                        if (android.os.Build.MODEL != null)
                            user.put("MODEL", android.os.Build.MODEL);

                        userId = emailid;
                        childName = mr + " " + name;

                        // Show the progress dialog
                        // turnOnProgressDialog("Signup", "Please wait while I sign you up");
                        user.signUpInBackground(new SignUpCallback() {
                            public void done(ParseException e) {

                                if (e == null) {

                                    //Create welcome notification and message

                                   // Utility.toast("Signup Successful");

                                    //locally creating session
                                    SessionManager session = new SessionManager(Application.getAppContext());
                                    session.setSignUpAccount();

                                    /*
                                    * default group code
                                    */
                                    if (role.equals("teacher")) {
                                        code = Config.defaultTeacherGroupCode;
                                    } else {
                                        code = Config.defaultParentGroupCode;
                                    }

                                    StoreSchoolInBackground storeSchoolInBackground = new StoreSchoolInBackground();
                                    storeSchoolInBackground.execute();



                                    Utility.ls("signing up..........4....");



                                } else {

                                    // Sign up didn't succeed. Look at the ParseException
                                    // to figure out what went wrong
                                    signUpLayout.setVisibility(View.VISIBLE);
                                    progressLayout.setVisibility(View.GONE);
                                    String result = "Sign up failed.... Try again.";
                                    try {
                                        if (Queries.isEmailExist(emailid)) {
                                            result = "Email-id already exists";


                                        }
                                    } catch (ParseException e1) {
                                    }
                                    Utility.toast(result);
                                }
                            }
                        });
                    }
                } else {
                    Utility.toast("Check your Internet connection");
                }
            }
        });
    };


    class StoreSchoolInBackground extends AsyncTask<Void, Void, Void>
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
            } catch (ParseException e1) {
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
            if(currentUser == null) return;

            //here create welcome notification and message
            if(user.getString("role").equals("teacher")){
                NotificationGenerator.generateNotification(getApplicationContext(), Constants.WELCOME_MESSAGE_TEACHER, Constants.DEFAULT_NAME, Constants.NORMAL_NOTIFICATION, Constants.INBOX_ACTION);
                AlarmReceiver.generateLocalMessage(Constants.WELCOME_MESSAGE_TEACHER, Constants.DEFAULT_NAME, user);
            }
            else if(user.getString("role").equals("parent")){
                NotificationGenerator.generateNotification(getApplicationContext(), Constants.WELCOME_MESSAGE_PARENT, Constants.DEFAULT_NAME, Constants.NORMAL_NOTIFICATION, Constants.INBOX_ACTION);
                AlarmReceiver.generateLocalMessage(Constants.WELCOME_MESSAGE_PARENT, Constants.DEFAULT_NAME, user);
            }
            else{
                NotificationGenerator.generateNotification(getApplicationContext(), Constants.WELCOME_MESSAGE_STUDENT, Constants.DEFAULT_NAME, Constants.NORMAL_NOTIFICATION, Constants.INBOX_ACTION);
                AlarmReceiver.generateLocalMessage(Constants.WELCOME_MESSAGE_STUDENT, Constants.DEFAULT_NAME, user);
            }

            //Switching to MainActivity
            Intent intent = new Intent(getBaseContext(), LoginPanda.class);
            startActivity(intent);
        }
    }

    class joinDefaultGroup extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            if (ParseUser.getCurrentUser() != null && childName != null) {

                childName = childName.trim();
                childName = UtilString.parseString(childName);

                String childName = ParseUser.getCurrentUser().getString("name");
                childName = UtilString.parseString(childName);

                JoinedHelper.joinClass(code, childName, true);
            }
            return null;
        }
    }
}