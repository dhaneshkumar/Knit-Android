package loginpages;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import trumplab.textslate.R;
import trumplabs.schoolapp.Application;
import trumplabs.schoolapp.FontsOverride;
import utility.SessionManager;
import utility.Tools;
import utility.Utility;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import baseclasses.MyActionBarActivity;

import com.parse.ParseAnalytics;
import com.parse.ParseUser;

/**
 * Used to display splash screen on opening the app. It also start a background process to update
 * database of the app
 * 
 * @author dhanesh kumar
 */
public class SplashScreen extends MyActionBarActivity {
  LinearLayout logoLayout;
  Animation animationFadeIn;
  private Handler mHandler = new Handler();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.logosplash);

    logoLayout = (LinearLayout) findViewById(R.id.logoLayout);
    getSupportActionBar().hide();

    // setting new font
    FontsOverride.setDefaultFont(this, "MONOSPACE", "fonts/Roboto-Regular.ttf");



    // seetting up animation effect
    animationFadeIn = AnimationUtils.loadAnimation(this, R.anim.fadein);
    logoLayout.startAnimation(animationFadeIn);



    /*
     * Updating everything like inbox, classrooms, joinedgroups, memberlist in background
     */
    Tools.updateMsgs();

    /*
     * Setting App opening count Each time app count increases by one
     */
    SessionManager session = new SessionManager(Application.getAppContext());
    session.setAppOpeningCount();


    /*
     * Setting parse analytics
     */
    ParseAnalytics.trackAppOpened(getIntent());
    Log.d("mainActivity", "examine intent over");


    /*
     * tracking app opening interval
     */

    Map<String, String> dimensions = new HashMap<String, String>();
    dimensions.put("count", "dailyCount");

    String timeSession = "not set";
    try {
      if (session.getCurrentTime() != null) {
        Date currentTime = session.getCurrentTime();

        SimpleDateFormat ft = new SimpleDateFormat("HH:mm");
        String timeStamp = ft.format(currentTime);
        if (timeStamp != null) {
          if (timeStamp.compareTo("04:00") <= 0 && timeStamp.compareTo("00:01") >= 0)
            timeSession = "00:01 - 04:00";
          else if (timeStamp.compareTo("08:00") <= 0 && timeStamp.compareTo("04:00") >= 0)
            timeSession = "04:00 - 08:00";
          else if (timeStamp.compareTo("12:00") <= 0 && timeStamp.compareTo("08:00") >= 0)
            timeSession = "08:00 - 12:00";
          else if (timeStamp.compareTo("16:00") <= 0 && timeStamp.compareTo("12:00") >= 0)
            timeSession = "12:00 - 16:00";
          else if (timeStamp.compareTo("20:00") <= 0 && timeStamp.compareTo("16:00") >= 0)
            timeSession = "16:00 - 20:00";
          else if (timeStamp.compareTo("24:00") <= 0 && timeStamp.compareTo("20:00") >= 0)
            timeSession = "20:00 - 23:59";
        }
      }
    } catch (ParseException e1) {
    }


    // Send the dimensions to Parse along with the 'read' event
    dimensions.put("opening time", timeSession);
    ParseAnalytics.trackEvent("appOpening", dimensions);

    /*
     * Pausing for 800ms
     */
    Thread logoTimer = new Thread() {
      public void run() {
        try {
          // waits for 800ms
          sleep(1500);

          // start loginpage activity

            ParseUser user = ParseUser.getCurrentUser();

            if(user == null)
            {
                Intent loginIntent = new Intent(getBaseContext(), Signup.class);
                startActivity(loginIntent);
            }
            else
            {
                Intent loginIntent = new Intent(getBaseContext(), LoginPage.class);
                startActivity(loginIntent);
            }


        } catch (InterruptedException e) {
          e.printStackTrace();
        } finally {
          finish();
        }
      }
    };

    // start background process
    logoTimer.start();



  }


}
