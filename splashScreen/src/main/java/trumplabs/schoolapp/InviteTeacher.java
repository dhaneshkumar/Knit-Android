package trumplabs.schoolapp;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseUser;

import java.io.IOException;
import java.util.HashMap;

import additionals.ReadSchoolFile;
import baseclasses.MyActionBarActivity;
import library.UtilString;
import trumplab.textslate.R;
import utility.Utility;

/**
 * Created by ashish on 17/1/15.
 */

public class InviteTeacher extends MyActionBarActivity {
    LinearLayout detailsLayout;
    LinearLayout congratsLayout;
    LinearLayout progressBarLayout;
    AutoCompleteTextView schoolEditText;
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
        schoolEditText = (AutoCompleteTextView) findViewById(R.id.schoolEditText);
        teacherEditText = (EditText) findViewById(R.id.teacherEditText);
        phoneEditText = (EditText) findViewById(R.id.phoneEditText);
        emailEditText = (EditText) findViewById(R.id.emailEditText);
        childEditText = (EditText) findViewById(R.id.childEditText);



        ArrayAdapter adapter;
        try {

            ReadSchoolFile readSchoolFile = new ReadSchoolFile();
            adapter =
                    new ArrayAdapter(this, android.R.layout.simple_list_item_1, readSchoolFile.getSchoolsList().toArray());
            schoolEditText.setAdapter(adapter);
        } catch (IOException e) {
            e.printStackTrace();
        }


        //Adding home back button
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //setting title of activity
        getSupportActionBar().setTitle("Invite Teachers");

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


        //setting box color transition for teacher name editText
        teacherEditText.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void afterTextChanged(Editable s) {
                //Changing sending box color
                if (s.length() > 0) {
                    setColor(teacherEditText);
                    teacherFlag = true;
                    setButtonColor();
                }
                else {
                    unSetColor(teacherEditText);
                    teacherFlag = false;
                    setButtonColor();
                }
            }
        });

        //setting box color transition for school name editText
        schoolEditText.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void afterTextChanged(Editable s) {
                //Changing sending box color
                if (s.length() > 0) {
                    setColor(schoolEditText);
                    schoolFlag= true;
                    setButtonColor();
                }
                else {
                    unSetColor(schoolEditText);
                    schoolFlag = false;
                    setButtonColor();
                }
            }
        });

        //setting box color transition for child name editText
        childEditText.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void afterTextChanged(Editable s) {
                //Changing sending box color
                if (s.length() > 0)
                    setColor(childEditText);
                else
                    unSetColor(childEditText);
            }
        });

        //setting box color transition for phone name editText
        phoneEditText.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void afterTextChanged(Editable s) {
                //Changing sending box color
                if (s.length() > 0) {
                    setColor(phoneEditText);
                    phoneFlag = true;
                    setButtonColor();
                }
                else {
                    unSetColor(phoneEditText);
                    phoneFlag = false;
                    setButtonColor();
                }
            }
        });

        //setting box color transition for email name editText
        emailEditText.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void afterTextChanged(Editable s) {
                //Changing sending box color
                if (s.length() > 0) {
                    setColor(emailEditText);
                    emailFlag = true;
                    setButtonColor();
                }
                else {
                    unSetColor(emailEditText);
                    emailFlag = false;
                    setButtonColor();
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
                {Utility.logout(); return null;}

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
                detailsLayout.setVisibility(View.VISIBLE);

                String text = "You have successfully invited teacher to create a classroom. You will be the first one to know when they come on-board";

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
                alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String value = input.getText().toString();

                        Intent intent = new Intent(InviteTeacher.this, MainActivity.class);
                        startActivity(intent);
                    }
                });

//                alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
//                    public void onClick(DialogInterface dialog, int whichButton) {
//                        dialog.dismiss();
//                    }
//                });

                alert.show();



            }
            else{
                detailsLayout.setVisibility(View.VISIBLE);
                Utility.toast("Some error occurred. Please try again");
            }
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    private  void setColor(EditText editText)
    {
        int sdk = android.os.Build.VERSION.SDK_INT;
        if(sdk < android.os.Build.VERSION_CODES.JELLY_BEAN) {
            editText.setBackgroundDrawable( getResources().getDrawable(R.drawable.button_color_boundry) );
        } else {
            editText.setBackground( getResources().getDrawable(R.drawable.button_color_boundry));
        }
    }

    private  void unSetColor(EditText editText)
    {
        int sdk = android.os.Build.VERSION.SDK_INT;
        if(sdk < android.os.Build.VERSION_CODES.JELLY_BEAN) {
            editText.setBackgroundDrawable( getResources().getDrawable(R.drawable.round_corner_grey_color) );
        } else {
            editText.setBackground( getResources().getDrawable(R.drawable.round_corner_grey_color));
        }
    }


    private void setButtonColor()
    {
        int sdk = android.os.Build.VERSION.SDK_INT;

        if( (schoolFlag && teacherFlag && phoneFlag)  || (schoolFlag && teacherFlag && emailFlag) )
        {

            if(sdk < android.os.Build.VERSION_CODES.JELLY_BEAN) {
                submitButton.setBackgroundDrawable( getResources().getDrawable(R.drawable.round_corner_button_color) );
            } else {
                submitButton.setBackground( getResources().getDrawable(R.drawable.round_corner_button_color));
            }
        }
        else
        {
            if(sdk < android.os.Build.VERSION_CODES.JELLY_BEAN) {
                submitButton.setBackgroundDrawable( getResources().getDrawable(R.drawable.round_corner_light_button_color) );
            } else {
                submitButton.setBackground( getResources().getDrawable(R.drawable.round_corner_light_button_color));
            }
        }
    }
}
