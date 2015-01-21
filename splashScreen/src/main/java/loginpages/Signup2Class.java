package loginpages;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
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
import com.parse.SaveCallback;
import com.parse.SignUpCallback;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import baseclasses.MyActionBarActivity;
import joinclasses.School;
import library.UtilString;
import trumplab.textslate.R;
import trumplabs.schoolapp.Application;
import trumplabs.schoolapp.Constants;
import trumplabs.schoolapp.MainActivity;
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
                        if (mr.equals("Mr")) {
                            user.put("sex", "M");
                        } else
                            user.put("sex", "F");
                        if (android.os.Build.MODEL != null)
                            user.put("MODEL", android.os.Build.MODEL);
                        Utility.ls("signing up..............");
                        userId = emailid;
                        childName = mr + " " + name;

                        // Show the progress dialog
                        // turnOnProgressDialog("Signup", "Please wait while I sign you up");
                        user.signUpInBackground(new SignUpCallback() {
                            public void done(ParseException e) {

                                if (e == null) {
                                    Utility.toast("Signup Successful");
                                    Utility.ls("signing up.............0......");

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

                                    /*
                                    * Storing current time stamp
                                    */
                                    Utility.ls("signing up.............1 ...");
                                    try {
                                        if (role.equals(Constants.TEACHER)) {
                                                String schoolId = School.getSchoolObjectId(school);
                                                if (schoolId != null)
                                                    user.put("school", schoolId);
                                        }
                                    } catch (ParseException e2) {
                                    }
                                    Utility.ls("signing up............1.5..");
                                    user.put("test", true);

                                    //saving in background
                                    user.saveInBackground(new SaveCallback() {
                                        @Override
                                        public void done(ParseException e) {
                                            Date currentDate = user.getUpdatedAt();
                                            SessionManager sm = new SessionManager(Application.getAppContext());
                                            sm.setCurrentTime(currentDate);
                                        }
                                    });
                                    Utility.ls("signing up............1.8..");

                                    //storing username in parseInstallation table
                                    ParseInstallation installation = ParseInstallation.getCurrentInstallation();
                                    installation.put("username", user.getUsername());
                                    try {
                                        installation.save();
                                    } catch (ParseException e1) {
                                    }
                                    Queries query = new Queries();

                                    /*
                                    * Resetting channels
                                    */
                                    query.reSetChannels();

                                    /*
                                    * Joining default groups
                                    */
                                    joinDefaultGroup joinGroup = new joinDefaultGroup();
                                    joinGroup.execute();
                                    Utility.ls("signing up..........4....");

                                    //Switching to MainActivity
                                    Intent intent = new Intent(getBaseContext(), LoginPanda.class);
                                    startActivity(intent);
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
    }

    ;

    class joinDefaultGroup extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            if (userId != null && childName != null) {
/*
* Retrieving user details
*/
                childName = childName.trim();
                childName = UtilString.parseString(childName);
                ParseUser user = ParseUser.getCurrentUser();
                if (user != null) {
/********************* < joining class room > *************************/
                    ParseQuery<ParseObject> query = ParseQuery.getQuery("Codegroup");
                    query.whereEqualTo("code", code);
                    ParseObject a;
                    try {
                        a = query.getFirst();
                        if (a != null) {
                            if (a.get("name") != null && a.getBoolean("classExist")) {
                                String senderId = a.getString("senderId");
                                ParseFile senderPic = a.getParseFile("senderPic");
                                if (!UtilString.isBlank(a.get("name").toString())) {
                                    grpName = a.get("name").toString();

                                    // Enable to receive push
                                    ParseInstallation pi = ParseInstallation.getCurrentInstallation();
                                    if (pi != null) {
                                        pi.addUnique("channels", code);
                                        pi.saveEventually();
                                    }
                                    List<List<String>> oldList = user.getList("joined_groups");
                                    if (oldList == null)
                                        oldList = new ArrayList<List<String>>();
                                    List<String> list = new ArrayList<String>();
                                    list.add(code);
                                    list.add(grpName);
                                    list.add(childName);
                                    oldList.add(0, list);
                                    user.put("joined_groups", oldList);
                                    user.saveEventually();

                                    // Adding this user as member in GroupMembers table
                                    final ParseObject groupMembers = new ParseObject("GroupMembers");
                                    groupMembers.put("code", code);
                                    groupMembers.put("name", user.getString("name"));
                                    List<String> boys = new ArrayList<String>();
                                    boys.add(childName.trim());
                                    groupMembers.put("children_names", boys);
                                    if (user.getEmail() != null)
                                        groupMembers.put("emailId", user.getEmail());
                                    groupMembers.saveEventually();
                                    groupMembers.pin();
                                    SessionManager session = new SessionManager(Application.getAppContext());
                                    session.setDefaultClassExtst();
                                    Queries2 memberQuery = new Queries2();
                                    try {
                                        memberQuery.storeGroupMember(code, userId, true);
                                    } catch (ParseException e1) {
                                    }
                                }

                                /*
                                * Saving locally in Codegroup table
                                */
                                a.put("userId", userId);
                                a.pin();

                                /*
                                * download pic locally
                                */
                                senderId = senderId.replaceAll("@", "");
                                String filePath = Utility.getWorkingAppDir() + "/thumbnail/" + senderId + "_PC.jpg";
                                final File senderThumbnailFile = new File(filePath);
                                if (!senderThumbnailFile.exists()) {
                                    Queries2 imageQuery = new Queries2();
                                    if (senderPic != null)
                                        imageQuery.downloadProfileImage(senderId, senderPic);
                                } else {
                                }
                            }
                        }
                    } catch (ParseException e) {
                    }
                }
            }
            return null;
        }
    }
}