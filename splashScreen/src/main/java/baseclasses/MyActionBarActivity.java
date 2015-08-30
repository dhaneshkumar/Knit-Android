package baseclasses;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;

import com.firebase.client.Firebase;
import com.onesignal.OneSignal;
import com.parse.ParseUser;

import chat.ChatConfig;
import chat.ChatNotificationOpenedHandler;
import trumplabs.schoolapp.Application;

public class MyActionBarActivity extends ActionBarActivity{
    public static boolean initializedOneSignal = false;
    public static boolean sentOneSignalId = false;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        if(!initializedOneSignal && ParseUser.getCurrentUser() != null) {
            OneSignal.init(this, ChatConfig.GOOGLE_PROJECT_NUMBER, ChatConfig.ONE_SIGNAL_APP_ID, new ChatNotificationOpenedHandler());
            //OneSignal.enableInAppAlertNotification(false);
            //OneSignal.enableNotificationsWhenActive(false);
            initializedOneSignal = true;
        }

        if(initializedOneSignal){
            //TODO do this only once
            if(ParseUser.getCurrentUser() != null && !sentOneSignalId) {
                sentOneSignalId = true;
                OneSignal.idsAvailable(new OneSignal.IdsAvailableHandler() {
                    @Override
                    public void idsAvailable(String userId, String registrationId) {
                        Log.d("__CHAT onesignal", "userId:" + userId);
                        new Firebase(ChatConfig.FIREBASE_URL).child("Users").child(ParseUser.getCurrentUser().getUsername()).child("oneSignalId").setValue(userId);

                        if (registrationId != null)
                            Log.d("__CHAT onesignal", "registrationId:" + registrationId);
                    }
                });
            }
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
