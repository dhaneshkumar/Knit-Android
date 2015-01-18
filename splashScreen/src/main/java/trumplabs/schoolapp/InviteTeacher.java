package trumplabs.schoolapp;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseUser;

import java.util.HashMap;

import baseclasses.MyActionBarActivity;
import joinclasses.School;
import library.UtilString;
import loginpages.Signup1_5Class;
import trumplab.textslate.R;
import utility.Utility;

/**
 * Created by ashish on 17/1/15.
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
    Button submitButton;

    static String schoolName;
    static String teacherName;
    static String phoneNo;
    static String email;
    static String childName;

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
        TextView title = (TextView) findViewById(R.id.title);

        Typeface typeface = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Light.ttf");
        title.setTypeface(typeface);



        //submit button click response
        submitButton = (Button) findViewById(R.id.submitButton);
        submitButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                schoolName = schoolEditText.getText().toString();
                teacherName = teacherEditText.getText().toString();
                phoneNo = phoneEditText.getText().toString();
                email = emailEditText.getText().toString();
                childName = childEditText.getText().toString();

                if (UtilString.isBlank(schoolName))
                    Utility.toast("Incorrect school name");
                else if (UtilString.isBlank(teacherName))
                    Utility.toast("Incorrect teacher name");
                else if (UtilString.isBlank(phoneNo) && UtilString.isBlank(email))
                    Utility.toast("Please enter atleast phone number or email");
                else {
                    if (!Utility.isInternetOn(InviteTeacher.this)) {
                        Utility.toast("No internet Connection!");
                        return;
                    }

                    detailsLayout.setVisibility(View.GONE);
                    progressBarLayout.setVisibility(View.VISIBLE);

                    InviteTeacherTask inviteTask = new InviteTeacherTask();
                    inviteTask.execute();

                }
            }
        });
    };


    public class InviteTeacherTask extends AsyncTask<Void, Void, Void>{
        int response;

        public InviteTeacherTask(){

        }
        @Override
        protected Void doInBackground(Void... params) {
            Log.d("DEBUG_INVITE_TEACHER", "inviting in asynctask");
            ParseUser user = ParseUser.getCurrentUser();

            if (user == null)
                Utility.logout();

            String senderId = user.getUsername();

            HashMap<String, String> parameters = new HashMap<String, String>();
            parameters.put("senderId", senderId);
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

            /*try{
                Thread.sleep(5000);
            }
            catch (InterruptedException e){
                e.printStackTrace();
            }*/
            Log.d("DEBUG_INVITE_TEACHER", "invitation over. Showing congrats view");
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            progressBarLayout.setVisibility(View.GONE);
            if(response == 1) {
                congratsLayout.setVisibility(View.VISIBLE);
            }
            else{
                detailsLayout.setVisibility(View.VISIBLE);
                Utility.toast("Some error occurred. Please try again");
            }
        }
    }

}
