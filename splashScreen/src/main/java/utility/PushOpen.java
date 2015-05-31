package utility;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;

import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;

import additionals.InviteParents;
import additionals.OpenURL;
import library.UtilString;
import notifications.EventCheckerAlarmReceiver;
import notifications.NotificationGenerator;
import profileDetails.ProfilePage;
import trumplabs.schoolapp.Classrooms;
import trumplabs.schoolapp.Constants;
import trumplabs.schoolapp.InviteTeacher;
import trumplabs.schoolapp.MainActivity;

public class PushOpen extends ActionBarActivity {
    String type;
    String action;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        onNewIntent(getIntent());

        //type and action will never be null. Handled in NotificationGenerator
        if(type.equals(Constants.NORMAL_NOTIFICATION)){
            NotificationGenerator.normalNotificationList.clear();
        }

        Intent i = null;

        if (type.equals(Constants.TRANSITION_NOTIFICATION)) {
            if(action.equals(Constants.INVITE_TEACHER_ACTION)){
                i = new Intent(this, InviteTeacher.class);
            }
            else if(action.equals(Constants.INVITE_PARENT_ACTION)){
                i = new Intent(this, InviteParents.class);

                String classCode = getIntent().getExtras().getString("classCode");
                String className = getIntent().getExtras().getString("className");


                if((!UtilString.isBlank(classCode))  && (!UtilString.isBlank(className))) {
                    Log.d("DEBUG_PUSH_OPEN", "invite parent action " + classCode + " " + className);
                    i.putExtra("classCode", classCode);
                    i.putExtra("className", className);
                }
                else
                    i = new Intent(this, MainActivity.class); //go to main activity
            }
            else if(action.equals(Constants.CLASSROOMS_ACTION)){
                i = new Intent(this, MainActivity.class);
                ParseUser user = ParseUser.getCurrentUser();
                if (user != null && user.getString("role").equals(Constants.TEACHER))
                    i.putExtra("VIEWPAGERINDEX", 0);
                i.putExtra("pushOpen", true);
            }
            else if(action.equals(Constants.OUTBOX_ACTION)){
                i = new Intent(this, MainActivity.class);
                ParseUser user = ParseUser.getCurrentUser();
                if (user != null && user.getString("role").equals(Constants.TEACHER))
                    i.putExtra("VIEWPAGERINDEX", 1);
                i.putExtra("pushOpen", true);
            }
            else if(action.equals(Constants.CREATE_CLASS_ACTION)){

                i = new Intent(this, MainActivity.class);
                ParseUser user = ParseUser.getCurrentUser();
                if (user != null && user.getString("role").equals(Constants.TEACHER))
                    i.putExtra("VIEWPAGERINDEX", 0);
                i.putExtra("flag", "CREATE_CLASS");
                i.putExtra("pushOpen", true);
            }
        }
        else if (type.equals(Constants.LINK_NOTIFICATION)) {
            i = new Intent(this, OpenURL.class);
            i.putExtra("URL", action);
        }
        else if(type.equals(Constants.UPDATE_NOTIFICATION)){
            i = new Intent(this, ProfilePage.class);
            i.putExtra("action", action);
        }
        else { //normal notification

            i = new Intent(this, MainActivity.class);
            ParseUser user = ParseUser.getCurrentUser();
            if (user != null && user.getString("role").equals(Constants.TEACHER))
                i.putExtra("VIEWPAGERINDEX", 2);
            i.putExtra("pushOpen", true);
        }

        if(i != null) {
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK );
            this.startActivity(i);
        }
        finish(); //this is required so that this pushOpen activity no longer remains in the activity stack
    }


    @Override
    public void onNewIntent(Intent intent) {
        setIntent(intent);
        type = intent.getExtras().getString("type");
        action = intent.getExtras().getString("action");


        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
        int notificationId = intent.getIntExtra("notificationId", 100);
        Log.d("DEBUG_PUSH_OPEN", type +  " " + action + " notid " + notificationId);
        if(notificationId == 100)
            notificationManager.cancelAll();
        else
            notificationManager.cancel(notificationId);
    }

    public static class UserRemovedTask extends AsyncTask<Void, Void, Void> {

        String classCode;
        String className;
        public UserRemovedTask(String name, String code){
            className = name;
            classCode = code;
        }

        @Override
        protected Void doInBackground(Void... params) {
            if((!UtilString.isBlank(classCode))  && (!UtilString.isBlank(className))) {
                ParseQuery<ParseObject> query = ParseQuery.getQuery(Constants.CODE_GROUP);
                query.fromLocalDatastore();
                query.whereEqualTo("code", classCode);

                ParseUser user = ParseUser.getCurrentUser();

                //updating user entry
                if (user != null) {
                    try {
                        user.fetch();
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }

                    //user should be non-null for generating local message
                    ParseObject codeGroupObject = null;
                    try {
                        codeGroupObject = query.getFirst();

                        if (codeGroupObject != null) {
                            EventCheckerAlarmReceiver.generateLocalMessage(utility.Config.RemovalMsg, classCode, codeGroupObject.getString("Creator"), codeGroupObject.getString("senderId"), codeGroupObject.getString("name"), user);
                            Log.d("DEBUG_PUSH_OPEN", "UserRemovedTask : local message generated");
                        }
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
            }
            else{
                Log.d("DEBUG_PUSH_OPEN", "UserRemovedTask : classCode/className null");
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result){
            //update joined classes adapter.
            ParseUser user = ParseUser.getCurrentUser();
            if(user != null){
                Classrooms.joinedGroups = Classrooms.getJoinedGroups(user);
                if(Classrooms.joinedClassAdapter != null){
                    Classrooms.joinedClassAdapter.notifyDataSetChanged();
                }
            }
        }
    }


}