package loginpages;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.parse.ParseCloud;
import com.parse.ParseException;

import java.util.HashMap;
import java.util.List;

import baseclasses.MyActionBarActivity;
import library.UtilString;
import trumplab.textslate.R;
import trumplabs.schoolapp.Application;
import utility.Utility;

/**
 * Created by ashish on 26/2/15.
 */
public class PhoneSignUpName extends MyActionBarActivity {
    Spinner titleSpinner;
    EditText displayNameET;
    EditText phoneNumberET;

    TextView classNameTV;
    TextView teacherNameTV;
    LinearLayout classDetailsLayout;

    static String role = "";
    static String displayName = "";
    static String phoneNumber = "";
    static int titleSpinnerPosition = 0;
    static String title = "";
    static ProgressDialog pdialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signup_name);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        titleSpinner = (Spinner) findViewById(R.id.mr_spinner);
        displayNameET = (EditText) findViewById(R.id.displaynameid);
        phoneNumberET = (EditText) findViewById(R.id.phoneid);

        classNameTV = (TextView) findViewById(R.id.className);
        teacherNameTV = (TextView) findViewById(R.id.teacherName);
        classDetailsLayout = (LinearLayout) findViewById(R.id.classDetails);


        if(getIntent() != null && getIntent().getExtras() != null) {
            resetFields();
            role = getIntent().getExtras().getString("role");
        }
        else{//on press back from next activity. Use previous values to show
            displayNameET.setText(displayName);
            phoneNumberET.setText(phoneNumber);
            titleSpinner.setSelection(titleSpinnerPosition);
        }

        if(role.equals("teacher")){
            classDetailsLayout.setVisibility(View.GONE);
        }
        else{
            classDetailsLayout.setVisibility(View.VISIBLE);
            classNameTV.setText(PhoneSignUpClassDetails.className);
            teacherNameTV.setText(PhoneSignUpClassDetails.teacherName);
        }


        ArrayAdapter adapter = ArrayAdapter.createFromResource(this, R.array.title, R.layout.spinner_item);
        titleSpinner.setAdapter(adapter);

    }

    void resetFields(){
        displayName = "";
        phoneNumber = "";
        title = "";
        titleSpinnerPosition = 0;
    }

    /*public void onBackPressed() {
        Intent intent = new Intent(getBaseContext(), Signup.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }*/

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
        displayName = displayNameET.getText().toString();
        phoneNumber = phoneNumberET.getText().toString();
        titleSpinnerPosition = titleSpinner.getSelectedItemPosition();
        title = titleSpinner.getSelectedItem().toString();
        if (UtilString.isBlank(displayName))
            Utility.toast("Incorrect Display Name");
        else if (titleSpinnerPosition == 0)
            Utility.toast("Choose a title!");
        else if (UtilString.isBlank(phoneNumber) || phoneNumber.length() != 10)
            Utility.toast("Incorrect Mobile Number");
        else if(Utility.isInternetExist(this)) {
            //Removed school input page. So directly go to verification page
            /*pdialog = new ProgressDialog(this);
            pdialog.setCancelable(false);
            pdialog.setMessage("Please Wait...");
            pdialog.show();*/

            //Changing first letter to caps
            displayName = UtilString.changeFirstToCaps(displayName);

            Intent nextIntent = new Intent(this, PhoneSignUpVerfication.class);
            //nextIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            nextIntent.putExtra("login", false);

            PhoneSignUpSchool.GenerateVerificationCode generateVerificationCode = new PhoneSignUpSchool.GenerateVerificationCode(1, phoneNumber);
            startActivity(nextIntent);

            generateVerificationCode.execute();
        }
        else{
            Utility.toast("Check your Internet connection");
        }
    }
}
