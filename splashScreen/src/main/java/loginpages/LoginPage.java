package loginpages;

import android.app.Activity;
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

import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseInstallation;
import com.parse.ParseUser;

import java.util.HashMap;

import baseclasses.MyActionBarActivity;
import library.UtilString;
import trumplab.textslate.R;
import trumplabs.schoolapp.Application;
import trumplabs.schoolapp.Constants;
import trumplabs.schoolapp.MainActivity;
import utility.Config;
import utility.SessionManager;
import utility.Tools;
import utility.Utility;

public class LoginPage extends MyActionBarActivity {
  private static final String LOGTAG = "OLD_LOGIN";

  EditText username_etxt;
  EditText password_etxt;
  Button signin_btn;
  LinearLayout loginLayout;
  LinearLayout progressLayout;
  String email;
  String passwd;
  TextView logText;
  Activity activity;
  LinearLayout mainLayuot;

  public static int count = 0;// for the sake of dummy data

  protected void onCreate(android.os.Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.loginfirst_layout);

    mainLayuot = (LinearLayout) findViewById(R.id.mainLayout);

    getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    ParseUser user = ParseUser.getCurrentUser();
    if (user != null) {
      startActivity(new Intent(getBaseContext(), SplashScreen.class));
      Intent intent = new Intent(getBaseContext(), MainActivity.class);
      startActivity(intent);
    }
    activity = this;

    // variable intialization
    username_etxt = (EditText) findViewById(R.id.usernameid);
    password_etxt = (EditText) findViewById(R.id.passwordid);
    signin_btn = (Button) findViewById(R.id.signin_button);
    loginLayout = (LinearLayout) findViewById(R.id.loginlayout);
    progressLayout = (LinearLayout) findViewById(R.id.progresslayout);
    logText = (TextView) findViewById(R.id.logText);


    signin_btn.setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View v) {

        /*
         * Login to parse.com
         */
        email = username_etxt.getText().toString().trim();
        passwd = password_etxt.getText().toString().trim();


        if (UtilString.isBlank(email)) {
          Utility.toast("Enter your Email-id", true);
        } else if (UtilString.isBlank(passwd)) {
          Utility.toast("Enter your Password", true);
        } else if(Utility.isInternetExist()){
          /*
           * Hiding the keyboard from screen
           */
          Tools.hideKeyboard(LoginPage.this);
          loginLayout.setVisibility(View.GONE);
          progressLayout.setVisibility(View.VISIBLE);
          // getSupportActionBar().hide();

          OldLoginTask oldLoginTask = new OldLoginTask(email, passwd);
          oldLoginTask.execute();
        }
      }
    });

  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.main, menu);
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    FragmentManager fm = getSupportFragmentManager();
    switch (item.getItemId()) {
      case R.id.forgotpassword:
          if(Utility.isInternetExist()) {
          ForgotPassword forgotpassdialog = new ForgotPassword();
          forgotpassdialog.show(fm, "Forgot Password?");
        }
        break;

      case android.R.id.home:
        onBackPressed();
        break;

      default:
        break;
    }

    return super.onOptionsItemSelected(item);
  }

  class OldLoginTask extends AsyncTask<Void, Void, Void> {
    boolean taskSuccess = false;
    boolean networkError = false;
    boolean userDoesNotExistsError = false; //user doesnot exist - during login

    String email;
    String password;
    public OldLoginTask(String email, String password){
      this.email = email;
      this.password = password;
    }

    protected Void doInBackground(Void... par) {
      HashMap<String, Object> params = new HashMap<>();
      params.put("email", email);
      params.put("password", password);

      params.put("deviceType", "android");
      params.put("installationId", ParseInstallation.getCurrentInstallation().getInstallationId());

      PhoneSignUpVerfication.fillDetailsForSession(true, params);

      try {
        HashMap<String, Object> result = ParseCloud.callFunction("appEnter", params);
        String sessionToken = (String) result.get("sessionToken");

        if(!UtilString.isBlank(sessionToken)){
          ParseUser user = ParseUser.become(sessionToken);
          if (user != null) {
            taskSuccess = true;
            Utility.LogoutUtility.resetIgnoreInvalidSessionCheck();
            Utility.updateCurrentTimeInBackground();

            //If user has joined any class then locally saving it in session manager
            if(user.getList(Constants.JOINED_GROUPS) != null && user.getList(Constants.JOINED_GROUPS).size() >0) {
              SessionManager.getInstance().setHasUserJoinedClass();
            }
          } else {
            // The token could not be validated.
            if(Config.SHOWLOG) Log.d(LOGTAG, "parseuser become - returned user null");
          }
        }
        else{
          if(Config.SHOWLOG) Log.d(LOGTAG, "verifyCode result not correct");
        }
      }
      catch (ParseException e){
        Utility.LogoutUtility.checkAndHandleInvalidSession(e);

        if(Config.SHOWLOG) Log.d(LOGTAG, "verifyCode/becomeUser ParseException, error-code=" + e.getCode());
        if(e.getCode() == ParseException.CONNECTION_FAILED){
          networkError = true;
        }
        if(e.getMessage().equals("USER_DOESNOT_EXISTS")){
          userDoesNotExistsError = true;
        }
        e.printStackTrace();
      }

      return null;
    }

    @Override
    protected void onPostExecute(Void result){
      if(taskSuccess){
        //Switching to MainActivity
        Intent intent = new Intent(LoginPage.this, MainActivity.class);
        LoginPage.this.startActivity(intent);
      }
      else{
        getSupportActionBar().show();
        loginLayout.setVisibility(View.VISIBLE);
        progressLayout.setVisibility(View.GONE);

        if(networkError) {
          Utility.toast("Connection failure", true);
        }
        else if(userDoesNotExistsError){
          Utility.toast("Wrong email or password. Check again.", true);
        }
        else{
          Utility.toast("Log in failed.... Please try again", true);
        }
      }
    }
  }

}
