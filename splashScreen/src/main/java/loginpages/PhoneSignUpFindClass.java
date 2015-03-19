package loginpages;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import org.w3c.dom.Text;

import java.util.HashMap;
import java.util.List;

import baseclasses.MyActionBarActivity;
import library.UtilString;
import notifications.AlarmTrigger;
import profileDetails.ProfilePage;
import trumplab.textslate.R;
import trumplabs.schoolapp.Application;
import trumplabs.schoolapp.Constants;
import trumplabs.schoolapp.InviteTeacher;
import utility.SessionManager;
import utility.Utility;

/**
 * Created by ashish on 26/2/15.
 */
public class PhoneSignUpFindClass extends MyActionBarActivity {
    ProgressBar progressBar;
    EditText codeET;
    TextView codeErrorTV;
    TextView inviteTV;

    ImageView nameHelp;
    ImageView popupHead;
    TextView popupTV;


    static String role = "";
    static String classCode = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signup_find_classroom);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        codeET = (EditText) findViewById(R.id.code);
        codeErrorTV = (TextView) findViewById(R.id.codeError);
        inviteTV = (TextView) findViewById(R.id.invite);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);

        nameHelp = (ImageView) findViewById(R.id.nameHelp);
        popupHead = (ImageView) findViewById(R.id.popup_up);
        popupTV = (TextView) findViewById(R.id.popup_text);


        if(getIntent() != null && getIntent().getExtras() != null) {
            resetFields();
            role = getIntent().getExtras().getString("role");
        }
        else{//on press back from next activity. Use previous values to show
            codeET.setText(classCode);
            codeErrorTV.setVisibility(View.INVISIBLE);
        }

        //setting help button clicked functionality
        nameHelp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (popupHead.getVisibility() == View.VISIBLE) {
                    // Its visible
                    popupHead.setVisibility(View.GONE);
                    popupTV.setVisibility(View.GONE);
                } else {
                    // Either gone or invisible
                    popupHead.setVisibility(View.VISIBLE);
                    popupTV.setVisibility(View.VISIBLE);
                }
            }
        });

        inviteTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PhoneSignUpFindClass.this, InviteTeacher.class);
                startActivity(intent);
            }
        });
    }

    void resetFields(){
        classCode = "";
    }

    public void onBackPressed() {
        Intent intent = new Intent(getBaseContext(), Signup.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
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
        classCode = codeET.getText().toString();
        if (UtilString.isBlank(classCode) || classCode.length() != 7)
            Utility.toast("Enter Correct Class Code");
        else if(Utility.isInternetExist(this)) {
            progressBar.setVisibility(View.VISIBLE);
            FindClassTask findClassTask = new FindClassTask(classCode);
            findClassTask.execute();
        }
        else{
            Utility.toast("Check your Internet connection");
        }
    }

    public class FindClassTask extends AsyncTask<Void, Void, Void> {
        boolean success;
        boolean wrongCode;
        String code;
        ParseObject group;

        FindClassTask(String classCode){
            success = false;
            wrongCode = false;
            code = classCode;
        }

        @Override
        protected Void doInBackground(Void... params) {

            HashMap<String, Object> param = new HashMap<String, Object>();
            param.put("code", code);

            try{
                List<ParseObject> classes = ParseCloud.callFunction("findClass", param);
                success = true;
                if(classes == null || classes.size() == 0){
                    wrongCode = true;
                }
                else{
                    group = classes.get(0); //first class
                }
            }
            catch (ParseException e){
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result){
            progressBar.setVisibility(View.GONE);
            if(!success){
                Utility.toast("Unexpected error occurred. Please try again");
            }
            else if(wrongCode){
                codeErrorTV.setVisibility(View.VISIBLE);
            }
            else{
                //go to next activity setting bundle using codegroup object
                Log.d("DEBUG_FIND_CLASS", "starting signup class details activity");
                Intent intent = new Intent(PhoneSignUpFindClass.this, PhoneSignUpClassDetails.class);
                intent.putExtra("role", role);
                intent.putExtra("teacherName", group.getString("Creator"));
                intent.putExtra("className", group.getString("name"));
                intent.putExtra("classCode", classCode);
                startActivity(intent);
            }
        }
    }
}
