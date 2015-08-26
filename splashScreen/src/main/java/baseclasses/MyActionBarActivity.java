package baseclasses;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

import com.onesignal.OneSignal;

import chat.ChatConfig;
import chat.ChatNotificationOpenedHandler;
import trumplabs.schoolapp.Application;

public class MyActionBarActivity extends ActionBarActivity{
    static boolean initializedOneSignal = false;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        if(!initializedOneSignal) {
            OneSignal.init(this, ChatConfig.GOOGLE_PROJECT_NUMBER, ChatConfig.ONE_SIGNAL_APP_ID, new ChatNotificationOpenedHandler());
            initializedOneSignal = true;
        }

        // getSupportActionBar().show();
        
        /*
         * Adding smooth transition
         */
       // overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
    
    protected void onResume() {
        super.onResume();
      //  overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        Application.setCurrentActivity(this);
        OneSignal.onResumed();
    }

    protected void onPause() {
        clearReferences();
        super.onPause();
        OneSignal.onPaused();
    }

    protected void onDestroy() {
        clearReferences();
        super.onDestroy();
       // getSupportActionBar().hide();
    }

    private void clearReferences(){
        Activity currActivity = Application.getCurrentActivity();
        if (currActivity != null && currActivity.equals(this))
            Application.setCurrentActivity(null);
    }
}
