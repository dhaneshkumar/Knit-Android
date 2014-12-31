package baseclasses;

import trumplabs.schoolapp.Application;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;

public class MyActivity extends Activity {
    protected Application mMyApp;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        /*
         * Adding smooth transition
         */
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        mMyApp = (Application)this.getApplicationContext();
    }
    protected void onResume() {
        super.onResume();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        mMyApp.setCurrentActivity(this);
    }
    protected void onPause() {
        clearReferences();
        super.onPause();
    }
    protected void onDestroy() {        
        clearReferences();
        super.onDestroy();
    }

    private void clearReferences(){
        Activity currActivity = mMyApp.getCurrentActivity();
        if (currActivity != null && currActivity.equals(this))
            mMyApp.setCurrentActivity(null);
    }
}
