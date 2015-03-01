package loginpages;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;

import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import additionals.SchoolAutoComplete;
import additionals.SmsListener;
import baseclasses.MyActionBarActivity;
import library.DelayAutoCompleteTextView;
import library.UtilString;
import trumplab.textslate.R;
import trumplabs.schoolapp.Application;
import trumplabs.schoolapp.Classrooms;
import utility.Utility;

/**
 * Created by ashish on 26/2/15.
 */
public class PhoneSignUpSchool extends MyActionBarActivity {
    DelayAutoCompleteTextView locationInput;
    AutoCompleteTextView schoolNameView;
    ProgressBar loadingSchools;
    static ProgressDialog pdialog;

    private ArrayAdapter schoolsAdapter;
    private Context activityContext;

    static String schoolName = "";
    static String schoolLocation = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signup_school);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        activityContext = this;

        locationInput = (DelayAutoCompleteTextView) findViewById(R.id.school_location);
        schoolNameView = (AutoCompleteTextView) findViewById(R.id.schoolName);
        loadingSchools = (ProgressBar) findViewById(R.id.loading_schools);

        locationInput.setAdapter(new SchoolAutoComplete.PlacesAutoCompleteAdapter(this, R.layout.school_autocomplete_list_item, R.id.school_location));
        locationInput.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final String value = (String) parent.getItemAtPosition(position);
                Log.d("DEBUG_PROFILE_PAGE", "item clicked " + value);
                schoolNameView.setVisibility(View.GONE);
                loadingSchools.setVisibility(View.VISIBLE); //show progress bar

                new AsyncTask<Void, Void, Void>() {
                    ArrayList<String> schools;
                    @Override
                    protected Void doInBackground( Void... voids ) {
                        schools = SchoolAutoComplete.schoolsNearby(value);
                        return null;
                    }
                    @Override
                    protected void onPostExecute(Void result){
                        schoolsAdapter =
                                new ArrayAdapter(activityContext, android.R.layout.simple_list_item_1, schools);
                        schoolNameView.setAdapter(schoolsAdapter);

                        loadingSchools.setVisibility(View.GONE); //hide progress bar
                        schoolNameView.setText("");
                        schoolNameView.setVisibility(View.VISIBLE); //finally show school list box
                        return;
                    }
                }.execute();
            }
        });
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
        schoolName = schoolNameView.getText().toString();
        schoolLocation = locationInput.getText().toString();

        if (UtilString.isBlank(schoolName))
            Utility.toast("Schoo Name can't be empty");
        if(UtilString.isBlank(schoolLocation))
            Utility.toast("School location can't be empty");
        else{

            pdialog = new ProgressDialog(this);
            pdialog.setCancelable(false);
            pdialog.setMessage("Please Wait...");
            pdialog.show();

            GenerateVerificationCode generateVerificationCode = new GenerateVerificationCode(0, PhoneSignUpName.phoneNumber);
            generateVerificationCode.execute();
        }
    }

    public static class GenerateVerificationCode extends AsyncTask<Void, Void, Void> {
        Boolean success = false;
        int callerId;
        String number;
        public GenerateVerificationCode(int id, String num){ //id identifies the caller, num the phone number
            callerId = id;
            number = num;
            SmsListener.register();
        }

        @Override
        protected Void doInBackground(Void... params) {
            //setting parameters
            HashMap<String, Object> param = new HashMap<String, Object>();

            param.put("number", number);

            Log.d("DEBUG_SIGNUP_SCHOOL", "calling genCode() with " + number);
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
            /*boolean isLogin = false; //flag indicating whether caller is login or not
            if(callerId == 0) {
                if (PhoneSignUpSchool.pdialog != null) {
                    PhoneSignUpSchool.pdialog.dismiss();
                }
            }
            else if(callerId == 1){
                if (PhoneSignUpName.pdialog != null) {
                    PhoneSignUpName.pdialog.dismiss();
                }
            }
            else if(callerId == 2){
                if (PhoneLoginPage.pdialog != null) {
                    PhoneLoginPage.pdialog.dismiss();
                }
                isLogin = true;
            }*/

            if(success) {
                /*Intent nextIntent = new Intent(Application.getAppContext(), PhoneSignUpVerfication.class);
                nextIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                nextIntent.putExtra("login", isLogin);
                Application.getAppContext().startActivity(nextIntent);*/
            }
            else{
                SmsListener.unRegister();
                Utility.toastLong("Oops ! some error occured. Try again");
                PhoneSignUpVerfication.showError("Some unexpected error occured. Please try again");
                PhoneSignUpVerfication.showResendAction();
            }
        }
    }

}