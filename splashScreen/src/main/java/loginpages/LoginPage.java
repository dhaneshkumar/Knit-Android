package loginpages;

import android.app.Activity;
import android.content.Intent;
import android.support.v4.app.FragmentManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.parse.LogInCallback;
import com.parse.ParseException;
import com.parse.ParseUser;

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

public class LoginPage extends MyActionBarActivity {
  EditText username_etxt;
  EditText password_etxt;
  Button signin_btn;
  LinearLayout loginLayout;
  LinearLayout progressLayout;
  String email;
  String passwd;
  TextView logText;
  Activity activity;
  boolean signUpFlag = false;
  LinearLayout mainLayuot;

  public static String emailidtoset = "";
  public static String passwordtoset = "";
  public static int count = 0;// for the sake of dummy data

  protected void onCreate(android.os.Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.loginfirst_layout);

    mainLayuot = (LinearLayout) findViewById(R.id.mainLayout);

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
          Utility.toast("Enter your Email-id");
        } else if (UtilString.isBlank(passwd)) {
          Utility.toast("Enter your Password");
        } else {


          /*
           * Hiding the keyboard from screen
           */
          Tools.hideKeyboard(LoginPage.this);
          loginLayout.setVisibility(View.GONE);
          progressLayout.setVisibility(View.VISIBLE);
          // getSupportActionBar().hide();

          ParseUser.logInInBackground(email, passwd, new LogInCallback() {
            public void done(ParseUser user, ParseException e) {
              if (e == null && user != null) {

                //update current server time
                Utility.updateCurrentTime(user);

                /*
                 * Adding channels & storing group member data locally
                 */

                Queries query = new Queries();
                try {
                  query.refreshChannels();
                } catch (ParseException e1) {
                }

                /*
                 * Checking for existence of default group.
                 */
                setDefaultGroupCheck(user);


                Intent intent = new Intent(getBaseContext(), MainActivity.class);
                startActivity(intent);



              } else {

                getSupportActionBar().show();
                loginLayout.setVisibility(View.VISIBLE);
                progressLayout.setVisibility(View.GONE);
                Utility.toast("Log in failed....  Try again.");
              }
            }
          });
        }

      }
    });
  };

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
        if (Utility.isInternetOn(this)) {
          ForgotPassword forgotpassdialog = new ForgotPassword();
          forgotpassdialog.show(fm, "Forgot Password?");
        }
        break;

      default:
        break;
    }

    return super.onOptionsItemSelected(item);
  }



  public static void setDefaultGroupCheck(ParseUser user) {
    if (user == null)
      return;

    List<List<String>> joinedGroupList = user.getList("joined_groups");
    String role = user.getString(Constants.ROLE);
    String defaultGroupCode = null;

    if (role.equals(Constants.TEACHER)) {
      defaultGroupCode = "TS49518";
    } else {
      defaultGroupCode = "TS29734";
    }

    if (joinedGroupList != null) {
      for (int i = 0; i < joinedGroupList.size(); i++) {
        if (joinedGroupList.get(i).get(0) == null)
          break;

        if (defaultGroupCode.equals(joinedGroupList.get(i).get(0))) {
          SessionManager session = new SessionManager(Application.getAppContext());
          session.setDefaultClassExtst();

          break;
        }
      }
    }
  }




}
