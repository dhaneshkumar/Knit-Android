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
  TextView oldLoginTV;
  static ProgressDialog pdialog;

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
      oldLoginTV = (TextView) findViewById(R.id.oldLogin);

      if(getIntent()!=null && getIntent().getExtras() != null){
          Log.d("DEBUG_PHONE_LOGIN", "setting phone number empty");
          phoneNumber = ""; //reset as called from parent
      }
      else {//coming back from child, so restore the fields
          phoneNumberET.setText(phoneNumber);
      }

      oldLoginTV.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) {
              Intent oldLoginIntent = new Intent(getBaseContext(), LoginPage.class);
              oldLoginIntent.putExtra("login", false);
              startActivity(oldLoginIntent);
          }
      });
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

            PhoneSignUpSchool.GenerateVerificationCode generateVerificationCode = new PhoneSignUpSchool.GenerateVerificationCode(2, phoneNumber);
            generateVerificationCode.execute();
        }
    }
}
