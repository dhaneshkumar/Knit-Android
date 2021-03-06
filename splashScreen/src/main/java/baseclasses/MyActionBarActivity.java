package baseclasses;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

import trumplabs.schoolapp.Application;

public class MyActionBarActivity extends ActionBarActivity{
    public boolean isVisibleNow;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        isVisibleNow = true;
        Application.setCurrentActivity(this);//important because in onCreate also app is visible

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
       // getSupportActionBar().show();
        
        /*
         * Adding smooth transition
         */
       // overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
    
    protected void onResume() {
        super.onResume();
        isVisibleNow = true;
      //  overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        Application.setCurrentActivity(this);
    }

    protected void onPause() {
        isVisibleNow = false;
        clearReferences();
        super.onPause();
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
