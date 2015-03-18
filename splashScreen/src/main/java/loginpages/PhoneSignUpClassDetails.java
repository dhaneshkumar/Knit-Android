package loginpages;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.List;

import baseclasses.MyActionBarActivity;
import library.UtilString;
import trumplab.textslate.R;
import trumplabs.schoolapp.Constants;
import trumplabs.schoolapp.InviteTeacher;
import utility.Utility;

/**
 * Created by ashish on 26/2/15.
 */
public class PhoneSignUpClassDetails extends MyActionBarActivity {
    TextView classNameTV;
    TextView teacherNameTV;
    EditText childNameET;
    LinearLayout childDetailsLayout;

    static String role = "";
    static String className = "";
    static String teacherName = "";
    static String childName = "";
    static String classCode = ""; //don't reset it here

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signup_class_details);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        classNameTV = (TextView) findViewById(R.id.className);
        teacherNameTV = (TextView) findViewById(R.id.teacherName);
        childNameET = (EditText) findViewById(R.id.childName);
        childDetailsLayout = (LinearLayout) findViewById(R.id.childDetails);

        if(getIntent() != null && getIntent().getExtras() != null) {
            resetFields();
            role = getIntent().getExtras().getString("role");
            className = getIntent().getExtras().getString("className");
            teacherName = getIntent().getExtras().getString("teacherName");
            classCode = getIntent().getExtras().getString("classCode");
        }

        if(role.equalsIgnoreCase("student")){
            childDetailsLayout.setVisibility(View.GONE);
        }

        classNameTV.setText(className);
        teacherNameTV.setText("by " + teacherName);
        childNameET.setText(childName);
    }

    void resetFields(){
        className = "";
        teacherName = "";
        childName = "";
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
        childName = childNameET.getText().toString();
        if (role.equalsIgnoreCase("parent") && UtilString.isBlank(childName)) {
            Utility.toast("Enter child name !");
        }
        else{
            Intent intent = new Intent(this, PhoneSignUpName.class);
            intent.putExtra("role", role);
            intent.putExtra("classCode", classCode);
            intent.putExtra("teacherName", teacherName);
            intent.putExtra("className", className);
            startActivity(intent);
        }
    }
}
