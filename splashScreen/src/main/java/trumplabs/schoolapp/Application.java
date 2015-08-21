package trumplabs.schoolapp;


import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.parse.Parse;
import com.parse.ParseCrashReporting;
import com.parse.ParseObject;
import com.parse.ParseUser;
import com.parse.PushService;

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

import utility.Config;

public class Application extends android.app.Application {
	
	 private static Context context;
	 private static Activity mCurrentActivity = null;

     public static boolean joinedSyncOnce = false; //sync joined classes info only once on app start

     public static boolean mainActivityVisible = false; //whether MainActivity is visible or not.
     public static Date lastTimeInboxSync = null;
     public static Date lastTimeOutboxSync = null;
        /*this is the last time when inbox/outbox were refreshed - either
        1) manually by clicking on refresh button
        2) or by Refresher in background
        3) or in onCreate of the MainActivity fragments

        It is required to make sure that 1) and 3) occur only if time gap is Config.inboxOutboxUpdateGap or more

        initialized to null*/

    public static Handler applicationHandler;

    public static ConcurrentHashMap<String, ParseObject> globalCodegroupMap; //code => Codegroup object
    //avoid unnecessary queries on Codegroup (in Classrooms, ComposeMessage pages)

  public Application() {
	  Parse.enableLocalDatastore(this);
  }

  @Override
  public void onCreate() 
  {
      if(Config.SHOWLOG) Log.d("__A","onCreate Application, earlier applicationHandler null=" + (applicationHandler==null));

      applicationHandler = new Handler();

      globalCodegroupMap = new ConcurrentHashMap<>();

    super.onCreate();

      FontsOverride.setDefaultFont(this, "MONOSPACE","fonts/Roboto-Regular.ttf");

      try {
          Class.forName("android.os.AsyncTask");
      }
      catch(Throwable ignore) {
          // ignored
      }

 // Enable Crash Reporting on parse analytics
    ParseCrashReporting.enable(this);

	// Initialize the Parse SDK.
   
	Parse.initialize(this, Config.APP_ID, Config.CLIENT_KEY);

      //Enable revocable sessions
      ParseUser.enableRevocableSessionInBackground();

	Application.context = getApplicationContext();


	// Specify an Activity to handle all pushes by   default.
	//PushService.setDefaultPushCallback(this, trumplabs.schoolapp.MainActivity.class);
	PushService.setDefaultPushCallback(this, trumplabs.schoolapp.MainActivity.class,getResources().getIdentifier("notifications", "drawable", getPackageName()));

  }
  
  
  public static Context getAppContext() {
      return Application.context;
  }
  
  
  public static Activity getCurrentActivity(){
        return mCurrentActivity;
  }

  public static void setCurrentActivity(Activity cActivity){
        mCurrentActivity = cActivity;
  }

    public static boolean isAppForeground(){
        //at least onResume of some activity
        return mCurrentActivity != null;
    }
}