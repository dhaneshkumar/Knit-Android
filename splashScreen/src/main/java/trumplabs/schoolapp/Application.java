package trumplabs.schoolapp;


import android.app.Activity;
import android.content.Context;

import com.facebook.FacebookSdk;
import com.parse.Parse;
import com.parse.ParseCrashReporting;
import com.parse.ParseInstallation;
import com.parse.PushService;

import java.util.Calendar;
import java.util.Date;

import utility.Config;

public class Application extends android.app.Application {
	
	 private static Context context;
	 private Activity mCurrentActivity = null;

     public static boolean mainActivityVisible = false; //whether MainActivity is visible or not.
     public static Date lastTimeInboxSync = null;
     public static Date lastTimeOutboxSync = null;
        /*this is the last time when inbox/outbox were refreshed - either
        1) manually by clicking on refresh button
        2) or by Refresher in background
        3) or in onCreate of the MainActivity fragments

        It is required to make sure that 1) and 3) occur only if time gap is Config.inboxOutboxUpdateGap or more

        initialized to null*/
     public static Date lastTimeJoinedSync = null;
        /*
            Similar to above but for updating joined class details such as name and profile pic of teachers
         */

  public Application() {
	  Parse.enableLocalDatastore(this);
  }

  @Override
  public void onCreate() 
  {
    super.onCreate();
    FontsOverride.setDefaultFont(this, "MONOSPACE","fonts/Roboto-Regular.ttf");



 // Enable Crash Reporting on parse analytics
    ParseCrashReporting.enable(this);

	// Initialize the Parse SDK.
   
	Parse.initialize(this, Config.APP_ID, Config.CLIENT_KEY);

	Application.context = getApplicationContext();

	// Specify an Activity to handle all pushes by   default.
	//PushService.setDefaultPushCallback(this, trumplabs.schoolapp.MainActivity.class);
	PushService.setDefaultPushCallback(this, trumplabs.schoolapp.MainActivity.class,getResources().getIdentifier("notifications", "drawable", getPackageName()));
  }
  
  
  public static Context getAppContext() {
      return Application.context;
  }
  
  
  public Activity getCurrentActivity(){
        return mCurrentActivity;
  }
  public void setCurrentActivity(Activity mCurrentActivity){
        this.mCurrentActivity = mCurrentActivity;
  }
  
  
  

  
}