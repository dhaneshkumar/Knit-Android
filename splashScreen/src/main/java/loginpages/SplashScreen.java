package loginpages;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;

import com.parse.ParseAnalytics;
import com.parse.ParseUser;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import BackGroundProcesses.UpdateAllClassSubscribers;
import baseclasses.MyActionBarActivity;
import notifications.AlarmTrigger;
import trumplab.textslate.R;
import trumplabs.schoolapp.Application;
import trumplabs.schoolapp.Constants;
import trumplabs.schoolapp.FontsOverride;
import trumplabs.schoolapp.MainActivity;
import utility.SessionManager;

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

    static boolean isRefresherAlarmTriggered = false; //flag telling whether alarm for event checker has been triggered or not

    @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.logosplash);

    logoLayout = (LinearLayout) findViewById(R.id.logoLayout);
    getSupportActionBar().hide();

    // setting new font
    FontsOverride.setDefaultFont(this, "MONOSPACE", "fonts/Roboto-Regular.ttf");

    // setting up animation effect
    animationFadeIn = AnimationUtils.loadAnimation(this, R.anim.fadein);
    logoLayout.startAnimation(animationFadeIn);

        ParseUser freshUser = ParseUser.getCurrentUser();

        /*if(freshUser != null) {
            Utility.updateCurrentTimeInBackground(freshUser);
            freshUser.fetchInBackground();
        }*/

    /*
     * Set refresher alarm to update everything in background
     */
    if(!isRefresherAlarmTriggered){
        AlarmTrigger.triggerRefresherAlarm(Application.getAppContext());
        isRefresherAlarmTriggered = true;
    }

        //trigger notification alarm (TODO take care of boot issues)
        AlarmTrigger.triggerNotificationAlarm(Application.getAppContext());

     /*
        Refreshing subscriber list of all classrooms in background
         */
        UpdateAllClassSubscribers.update();

    /*
     * Setting App opening count Each time app count increases by one
     */
        final SessionManager session = new SessionManager(Application.getAppContext());
    if(ParseUser.getCurrentUser() != null){ //only if logged in
        session.setAppOpeningCount();
    }

       // FacebookSdk.sdkInitialize(getApplicationContext());

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
          sleep(1000);

            Constants.actionBarHeight = getSupportActionBar().getHeight();

            Log.d("action", "refresher action bar :" + getSupportActionBar().getHeight());

          // start loginpage activity

            ParseUser user = ParseUser.getCurrentUser();

            if(user == null)
            {
                Intent loginIntent = new Intent(getBaseContext(), Signup.class);
                startActivity(loginIntent);
                // we override the transition
                overridePendingTransition(R.anim.transition_in, R.anim.transition_out);
            }
            else
            {
                /*
                Putting security check to support old users while updating app
                 */
                if (user.getList(Constants.JOINED_GROUPS) != null && user.getList(Constants.JOINED_GROUPS).size() > 0) {
                    SessionManager sessionManager = new SessionManager(Application.getAppContext());
                    sessionManager.setHasUserJoinedClass();
                }


                Intent loginIntent = new Intent(getBaseContext(), MainActivity.class);
                startActivity(loginIntent);
                // we override the transition
                overridePendingTransition(R.anim.transition_in, R.anim.transition_out);
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
