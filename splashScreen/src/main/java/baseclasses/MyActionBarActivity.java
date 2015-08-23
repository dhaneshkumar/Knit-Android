package baseclasses;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.widget.Toast;

import com.parse.ParseUser;
import com.quickblox.chat.QBChatService;
import com.quickblox.core.QBEntityCallbackImpl;
import com.quickblox.users.model.QBUser;

import java.util.List;

import chat.ChatService;
import trumplabs.schoolapp.Application;

public class MyActionBarActivity extends ActionBarActivity{
    private static boolean sessionActive = false;
    private boolean needToRecreateSession = false;
    private final Handler handler = new Handler();

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
       // getSupportActionBar().show();
        
        /*
         * Adding smooth transition
         */
       // overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

        boolean initialised = ChatService.initIfNeed(this);
        if(!sessionActive || (initialised && savedInstanceState != null)){
            needToRecreateSession = true;
        }else{
            sessionActive = true;
        }
    }
    
    protected void onResume() {
        super.onResume();
      //  overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        Application.setCurrentActivity(this);

        ChatService.initIfNeed(this);
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
        Activity currActivity = Application.getCurrentActivity();
        if (currActivity != null && currActivity.equals(this))
            Application.setCurrentActivity(null);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        if(needToRecreateSession){
            needToRecreateSession = false;

            Log.d("__CHAT", "Need to restore chat connection");
            recreateSession();
        }
    }

    protected void recreateSession(){
        Log.d("__CHAT", "recreateSession called");

        if(ParseUser.getCurrentUser() == null){
            Log.d("__CHAT", "ParseUser null");
            return;
        }

        String username = ParseUser.getCurrentUser().getUsername();
        final QBUser user = new QBUser(username, username);

        sessionActive = false;
        // Restoring Chat session
        //

        Log.d("__CHAT", "Chat attempting login.....");

        ChatService.getInstance().login(user, new QBEntityCallbackImpl() {
            @Override
            public void onSuccess() {
                Log.d("__CHAT", "Chat login onSuccess @" + ChatService.chatLoginStage);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast toast = Toast.makeText(Application.getAppContext(),
                                "Chat login success", Toast.LENGTH_SHORT);
                        toast.show();
                    }
                });

                sessionActive = true;
            }

            @Override
            public void onError(List errors) {

                Log.d("__CHAT", "Chat login @" + ChatService.chatLoginStage + " onError: " + errors);

                Toast toast = Toast.makeText(getApplicationContext(),
                        "Error, trying again in 3 seconds.. Check internet", Toast.LENGTH_SHORT);
                toast.show();

                // try again
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        recreateSession();
                    }
                }, 3000);
            }
        });
    }
}
