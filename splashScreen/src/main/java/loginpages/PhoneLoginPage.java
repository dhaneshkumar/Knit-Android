package loginpages;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.parse.LogInCallback;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseUser;

import java.util.HashMap;
import java.util.List;

import baseclasses.MyActionBarActivity;
import library.UtilString;
import trumplab.textslate.R;
import trumplabs.schoolapp.Application;
import trumplabs.schoolapp.Constants;
import trumplabs.schoolapp.MainActivity;
import utility.Queries;
import utility.SessionManager;
import utility.Tools;
import utility.Utility;

public class PhoneLoginPage extends MyActionBarActivity {
  EditText phoneNumberET;
    ProgressDialog pdialog;

  Activity activity;

    static String phoneNumber = "";

  protected void onCreate(android.os.Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.phone_login_page);

    ParseUser user = ParseUser.getCurrentUser();
    if (user != null) {
      startActivity(new Intent(getBaseContext(), SplashScreen.class));
      Intent intent = new Intent(getBaseContext(), MainActivity.class);
      startActivity(intent);
    }
    activity = this;
      phoneNumberET = (EditText) findViewById(R.id.phone_id);

      if(getIntent()!=null && getIntent().getExtras() != null){
          phoneNumber = ""; //reset as called from parent
      }
      else {//coming back from child, so restore the fields
          phoneNumberET.setText(phoneNumber);
      }
  };

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.phone_signup_name_menu, menu);
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    FragmentManager fm = getSupportFragmentManager();
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
        phoneNumber = phoneNumberET.getText().toString();
        if (UtilString.isBlank(phoneNumber) || phoneNumber.length() != 10)
            Utility.toast("Incorrect Mobile Number");
        else {
            pdialog = new ProgressDialog(this);
            pdialog.setCancelable(false);
            pdialog.setMessage("Please Wait...");
            pdialog.show();

            GenerateVerificationCode generateVerificationCode = new GenerateVerificationCode();
            generateVerificationCode.execute();
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

            Log.d("DEBUG_PHONE_LOGIN", "calling genCode() with " + phoneNumber);
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
                nextIntent.putExtra("login", true);
                startActivity(nextIntent);
            }
            else{
                Utility.toast("Oops ! Sign up failed. Try again");
            }
        }
    }
}
