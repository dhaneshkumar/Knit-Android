package loginpages;

import java.util.List;

import library.UtilString;
import trumplab.textslate.R;
import trumplabs.schoolapp.Application;
import trumplabs.schoolapp.Constants;
import trumplabs.schoolapp.MainActivity;
import utility.Queries;
import utility.SessionManager;
import utility.Tools;
import utility.Utility;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.app.FragmentManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import baseclasses.MyActionBarActivity;

import com.parse.LogInCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

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
  Animation animationFadeIn;
  LinearLayout mainLayuot;

  public static String emailidtoset = "";
  public static String passwordtoset = "";
  public static int count = 0;// for the sake of dummy data

  protected void onCreate(android.os.Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.loginfirst_layout);

    mainLayuot = (LinearLayout) findViewById(R.id.mainLayout);
    animationFadeIn = AnimationUtils.loadAnimation(this, R.anim.fadein);
    mainLayuot.startAnimation(animationFadeIn);

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
              if (user != null) {

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
      case R.id.signupmenu:

        if (Utility.isInternetOn(this)) {
          signUpFlag = true;
          loginLayout.setVisibility(View.GONE);
          progressLayout.setVisibility(View.VISIBLE);
          logText.setVisibility(View.GONE);
          GetSchools getSchools = new GetSchools(fm);
          getSchools.execute();
        }
        break;
      default:
        break;
    }

    return super.onOptionsItemSelected(item);
  }

  @Override
  public void onBackPressed() {

    if (!signUpFlag) {
      Intent intent = new Intent();
      intent.setAction(Intent.ACTION_MAIN);
      intent.addCategory(Intent.CATEGORY_HOME);
      startActivity(intent);
    } else
      signUpFlag = false;
  }

  private void setDefaultGroupCheck(ParseUser user) {
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



  private class GetSchools extends AsyncTask<Void, Void, Void> {
    private FragmentManager fm;

    GetSchools(FragmentManager fm) {
      this.fm = fm;
    }

    @Override
    protected Void doInBackground(Void... params) {

      ParseQuery<ParseObject> query = ParseQuery.getQuery("SCHOOLS");
      query.orderByAscending("school_name");

      try {
        List<ParseObject> schoolList = query.find();

        if (schoolList != null)
          ParseObject.pinAll(schoolList);


      } catch (ParseException e) {
      }

      return null;
    }

    @Override
    protected void onPostExecute(Void result) {

      loginLayout.setVisibility(View.VISIBLE);
      progressLayout.setVisibility(View.GONE);
      logText.setVisibility(View.VISIBLE);

      signUp0Class signUp0 = new signUp0Class();

      fm.beginTransaction().add(signUp0, "Choose Profession!").commitAllowingStateLoss();
      // signUp0.show(fm, "Choose Profession!");
    }


  }
}
