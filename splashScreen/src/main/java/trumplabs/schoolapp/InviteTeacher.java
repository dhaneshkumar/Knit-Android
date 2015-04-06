package trumplabs.schoolapp;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.facebook.appevents.AppEventsLogger;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseUser;

import java.util.HashMap;

import baseclasses.MyActionBarActivity;
import library.UtilString;
import loginpages.Signup;
import trumplab.textslate.R;
import utility.Config;
import utility.Tools;
import utility.Utility;

/**
 * Created by ashish on 17/1/15.
 * Invite teacher to join the platform by filling out details of the teacher
 */

public class InviteTeacher extends MyActionBarActivity {
    LinearLayout detailsLayout;
    LinearLayout congratsLayout;
    LinearLayout progressBarLayout;
    EditText schoolEditText;
    EditText teacherEditText;
    EditText phoneEditText;
    EditText emailEditText;
    EditText childEditText;

    static String schoolName;
    static String teacherName;
    static String phoneNo;
    static String email;
    static String childName;

    boolean schoolFlag = false;
    boolean teacherFlag = false;
    boolean phoneFlag = false;
    boolean emailFlag = false;

    protected void onCreate(android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.invite_teacher);

        detailsLayout = (LinearLayout) findViewById(R.id.detailsLayout);
        congratsLayout = (LinearLayout) findViewById(R.id.congratsLayout);
        progressBarLayout = (LinearLayout) findViewById(R.id.progressBarLayout);
        schoolEditText = (EditText) findViewById(R.id.schoolEditText);
        teacherEditText = (EditText) findViewById(R.id.teacherEditText);
        phoneEditText = (EditText) findViewById(R.id.phoneEditText);
        emailEditText = (EditText) findViewById(R.id.emailEditText);
        childEditText = (EditText) findViewById(R.id.childEditText);

        //Adding home back button
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //setting title of activity
        getSupportActionBar().setTitle("Invite Teacher");


    };



    @Override
    protected void onResume() {
        super.onResume();

        //facebook ad tracking
        // Logs 'install' and 'app activate' App Events.
        AppEventsLogger.activateApp(this, Config.FB_APP_ID);
    }

    @Override
    protected void onPause() {
        super.onPause();

        //facebook tracking : time spent on app by people
        // Logs 'app deactivate' App Event.
        AppEventsLogger.deactivateApp(this, Config.FB_APP_ID);
    }



    public class InviteTeacherTask extends AsyncTask<Void, Void, Void>{
        int response;

        public InviteTeacherTask(){

        }
        @Override
        protected Void doInBackground(Void... params) {
            Log.d("DEBUG_INVITE_TEACHER", "inviting in asynctask");
            ParseUser user = ParseUser.getCurrentUser();

            HashMap<String, String> parameters = new HashMap<String, String>();

            if (user != null) {
                String senderId = user.getUsername();
                parameters.put("senderId", senderId);
            }

            parameters.put("schoolName", schoolName);
            parameters.put("teacherName", teacherName);
            parameters.put("childName", childName);
            parameters.put("email", email);
            parameters.put("phoneNo", phoneNo);


            try{
                response = ParseCloud.callFunction("inviteTeacher", parameters);
            }
            catch (ParseException e){
                e.printStackTrace();
            }

            Log.d("DEBUG_INVITE_TEACHER", "invitation over. Showing congrats view");
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            progressBarLayout.setVisibility(View.GONE);
            if(response == 1) {
                detailsLayout.setVisibility(View.VISIBLE);

                String text = "You have successfully invited your teacher to use Knit. You will be the first one to know when they come on-board";

                AlertDialog.Builder alert = new AlertDialog.Builder(InviteTeacher.this);

                alert.setTitle("Hurray!");

                LinearLayout layout = new LinearLayout(InviteTeacher.this);
                layout.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams params =
                        new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT);
                params.setMargins(50,30,50,30);

                final TextView input = new TextView(InviteTeacher.this);
                input.setText(text);
                layout.addView(input, params);
                alert.setView(layout);

                Tools.hideKeyboard(InviteTeacher.this);
                alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        if(ParseUser.getCurrentUser() == null) {
                            Intent intent = new Intent(InviteTeacher.this, Signup.class);
                            startActivity(intent);
                        }
                        else{
                            Intent intent = new Intent(InviteTeacher.this, MainActivity.class);
                            startActivity(intent);
                        }
                    }
                });

                alert.show();

            }
            else{
                detailsLayout.setVisibility(View.VISIBLE);
                Utility.toast("Some error occurred. Please try again");
            }
        }
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.invite_teacher, menu);
        return true;
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Tools.hideKeyboard(InviteTeacher.this);
                onBackPressed();
                break;
            case  R.id.submit:

                schoolName = schoolEditText.getText().toString();
                teacherName = teacherEditText.getText().toString();
                phoneNo = phoneEditText.getText().toString();
                email = emailEditText.getText().toString();
                childName = childEditText.getText().toString();

                if (UtilString.isBlank(schoolName))
                    Utility.toast("Enter school name");
                else if (UtilString.isBlank(teacherName))
                    Utility.toast("Enter teacher name");
                else if ((UtilString.isBlank(phoneNo) || phoneNo.length() != 10) && UtilString.isBlank(email))
                    Utility.toast("Please enter atleast phone number or email");
                else {
                    if(! Utility.isInternetExist(InviteTeacher.this)) {
                        break;
                    }

                    detailsLayout.setVisibility(View.GONE);
                    progressBarLayout.setVisibility(View.VISIBLE);

                    InviteTeacherTask inviteTask = new InviteTeacherTask();
                    inviteTask.execute();
                }

                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }



}
