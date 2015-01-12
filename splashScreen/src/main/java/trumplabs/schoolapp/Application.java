package trumplabs.schoolapp;


import utility.Config;
import android.app.Activity;
import android.content.Context;

import com.parse.Parse;
import com.parse.ParseCrashReporting;
import com.parse.ParseInstallation;
import com.parse.PushService;

public class Application extends android.app.Application {
	
	 private static Context context;
	 private Activity mCurrentActivity = null;

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
	ParseInstallation.getCurrentInstallation().saveEventually();
    
   
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