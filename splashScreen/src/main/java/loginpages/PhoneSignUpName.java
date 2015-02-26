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
import android.widget.EditText;
import android.widget.Spinner;

import com.parse.ParseCloud;
import com.parse.ParseException;

import java.util.HashMap;
import java.util.List;

import library.UtilString;
import trumplab.textslate.R;
import utility.Utility;

/**
 * Created by ashish on 26/2/15.
 */
public class PhoneSignUpName extends ActionBarActivity {
    Spinner titleSpinner;
    EditText displayNameET;
    EditText phoneNumberET;


    static String role = "";
    static String displayName = "";
    static String phoneNumber = "";
    static int titleSpinnerPosition = 0;
    static String title = "";
    ProgressDialog pdialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signup_name);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        titleSpinner = (Spinner) findViewById(R.id.mr_spinner);
        displayNameET = (EditText) findViewById(R.id.displaynameid);
        phoneNumberET = (EditText) findViewById(R.id.phoneid);

        displayNameET.setText(displayName);
        phoneNumberET.setText(phoneNumber);
        titleSpinner.setSelection(titleSpinnerPosition);

        if(getIntent() != null && getIntent().getExtras() != null) {
            role = getIntent().getExtras().getString("role");
        }
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
        displayName = displayNameET.getText().toString();
        phoneNumber = phoneNumberET.getText().toString();
        titleSpinnerPosition = titleSpinner.getSelectedItemPosition();
        if (UtilString.isBlank(displayName))
            Utility.toast("Incorrect Display Name");
        else if (titleSpinnerPosition == 0)
            Utility.toast("Choose a title!");
        else if (UtilString.isBlank(phoneNumber) || phoneNumber.length() != 10)
            Utility.toast("Incorrect Mobile Number");
        else{
            Intent nextIntent;
            if(role.equals("teacher")) {
                nextIntent = new Intent(getBaseContext(), PhoneSignUpSchool.class);
                displayName = UtilString.changeFirstToCaps(displayName);
                title = titleSpinner.getSelectedItem().toString();
                startActivity(nextIntent);
            }
            else{
                pdialog = new ProgressDialog(this);
                pdialog.setCancelable(false);
                pdialog.setMessage("Please Wait...");
                pdialog.show();

                GenerateVerificationCode generateVerificationCode = new GenerateVerificationCode();
                generateVerificationCode.execute();
            }
        }
    }

    public class GenerateVerificationCode extends AsyncTask<Void, Void, Void> {
        List<List<String>> joinedGroups; //this will contain updated joined_groups
        Boolean success = false;
        public GenerateVerificationCode(){
        }

        @Override
        protected Void doInBackground(Void... params) {
            //setting parameters
            HashMap<String, Object> param = new HashMap<String, Object>();
            param.put("number", phoneNumber);

            Log.d("DEBUG_SIGNUP_NAME", "calling genCode() with " + phoneNumber);
            try {
                success = ParseCloud.callFunction("genCode", param);
            } catch (ParseException e) {
                e.printStackTrace();
                return null;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result){
            if(pdialog != null){
                pdialog.dismiss();
            }

            if(success) {
                Intent nextIntent = new Intent(getBaseContext(), PhoneSignUpVerfication.class);
                nextIntent.putExtra("login", false);
                startActivity(nextIntent);
            }
            else{
                Utility.toast("Oops ! Sign up failed. Try again");
            }
        }
    }
}
