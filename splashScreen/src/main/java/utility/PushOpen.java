package utility;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import additionals.Invite;
import additionals.OpenURL;
import baseclasses.MyActionBarActivity;
import library.UtilString;
import notifications.EventCheckerAlarmReceiver;
import notifications.NotificationGenerator;
import profileDetails.ProfilePage;
import trumplabs.schoolapp.Classrooms;
import trumplabs.schoolapp.ComposeMessage;
import trumplabs.schoolapp.Constants;
import trumplabs.schoolapp.MainActivity;
import trumplabs.schoolapp.SendMessage;
import trumplabs.schoolapp.Subscribers;

public class PushOpen extends MyActionBarActivity {
    String type;
    String action;

    final private static int OUTBOX_PAGE_INDEX = 0;
    final private static int CLASSROOMS_PAGE_INDEX = 1;
    final private static int INBOX_PAGE_INDEX = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(ParseUser.getCurrentUser() == null){
            finish();
            return;
        }

        onNewIntent(getIntent());

        //type and action will never be null. Handled in NotificationGenerator
        if(type.equals(Constants.Notifications.NORMAL_NOTIFICATION)){
            NotificationGenerator.normalNotificationList.clear();
        }

        Intent i = null;

        if (type.equals(Constants.Notifications.TRANSITION_NOTIFICATION)) {
            if(action.equals(Constants.Actions.INVITE_TEACHER_ACTION)){
                //open the common invite screen
                i = new Intent(this, Invite.class);
                i.putExtra("inviteType", Constants.INVITATION_P2T);
                i.putExtra("source", Constants.SOURCE_NOTIFICATION);
                i.putExtra("pushOpen", true);
            }
            else if(action.equals(Constants.Actions.INVITE_PARENT_ACTION)){
                i = new Intent(this, Invite.class);

                String classCode = getIntent().getExtras().getString("classCode");
                String className = getIntent().getExtras().getString("className");

                if((!UtilString.isBlank(classCode))  && (!UtilString.isBlank(className))) {
                    Log.d("DEBUG_PUSH_OPEN", "invite parent action " + classCode + " " + className);
                    i.putExtra("classCode", classCode);
                    i.putExtra("className", className);
                    i.putExtra("source", Constants.SOURCE_NOTIFICATION);
                    i.putExtra("inviteType", Constants.INVITATION_T2P);
                    i.putExtra("pushOpen", true);
                }
                else
                    i = new Intent(this, MainActivity.class); //go to main activity
            }
            else if(action.equals(Constants.Actions.SEND_MESSAGE_ACTION)){
                i = new Intent(this, ComposeMessage.class);

                String classCode = getIntent().getExtras().getString("classCode");
                String className = getIntent().getExtras().getString("className");

                if((!UtilString.isBlank(classCode))  && (!UtilString.isBlank(className))) {
                    Log.d("DEBUG_PUSH_OPEN", "send message action " + classCode + " " + className);
                    i.putExtra("pushOpen", true);
                    i.putExtra("CLASS_CODE", classCode);
                    i.putExtra("CLASS_NAME", className);
                }
                else {
                    i = new Intent(this, MainActivity.class); //go to main activity
                }
            }
            else if(action.equals(Constants.Actions.CLASSROOMS_ACTION)){
                i = new Intent(this, MainActivity.class);
                ParseUser user = ParseUser.getCurrentUser();
                if (user != null && user.getString("role").equals(Constants.TEACHER))
                    i.putExtra("VIEWPAGERINDEX", CLASSROOMS_PAGE_INDEX);
                i.putExtra("pushOpen", true);
            }
            else if(action.equals(Constants.Actions.OUTBOX_ACTION)){
                i = new Intent(this, MainActivity.class);
                ParseUser user = ParseUser.getCurrentUser();
                if (user != null && user.getString("role").equals(Constants.TEACHER))
                    i.putExtra("VIEWPAGERINDEX", OUTBOX_PAGE_INDEX);
                i.putExtra("pushOpen", true);
            }
            else if(action.equals(Constants.Actions.CREATE_CLASS_ACTION)){

                i = new Intent(this, MainActivity.class);
                ParseUser user = ParseUser.getCurrentUser();
                if (user != null && user.getString("role").equals(Constants.TEACHER))
                    i.putExtra("VIEWPAGERINDEX", CLASSROOMS_PAGE_INDEX);
                i.putExtra("flag", "CREATE_CLASS");
                i.putExtra("pushOpen", true);
            }
            else if(action.equals(Constants.Actions.LIKE_ACTION) || action.equals(Constants.Actions.CONFUSE_ACTION)){//go to outbox and scroll to that message
                i = new Intent(this, MainActivity.class);
                String id = getIntent().getExtras().getString("id");

                ParseUser user = ParseUser.getCurrentUser();
                if (user != null && user.getString("role").equals(Constants.TEACHER))
                    i.putExtra("VIEWPAGERINDEX", OUTBOX_PAGE_INDEX);

                i.putExtra("action", action);
                i.putExtra("id", id);
                i.putExtra("pushOpen", true);
            }
            else if(action.equals(Constants.Actions.MEMBER_ACTION)){//go to outbox and scroll to that message
                i = new Intent(this, Subscribers.class);

                String classCode = getIntent().getExtras().getString("classCode");
                String className = getIntent().getExtras().getString("className");

                if((!UtilString.isBlank(classCode))  && (!UtilString.isBlank(className))) {
                    Log.d("DEBUG_PUSH_OPEN", "member action " + classCode + " " + className);
                    i.putExtra("pushOpen", true);
                    i.putExtra("classCode", classCode);
                    i.putExtra("className", className);
                }
                else {
                    i = new Intent(this, MainActivity.class); //go to main activity
                }
            }
        }
        else if (type.equals(Constants.Notifications.LINK_NOTIFICATION)) {
            i = new Intent(this, OpenURL.class);
            i.putExtra("URL", action);
        }
        else if(type.equals(Constants.Notifications.UPDATE_NOTIFICATION)){
            i = new Intent(this, ProfilePage.class);
            i.putExtra("action", action);
        }
        else { //normal notification
            i = new Intent(this, MainActivity.class);
            ParseUser user = ParseUser.getCurrentUser();
            if (user != null && user.getString("role").equals(Constants.TEACHER))
                i.putExtra("VIEWPAGERINDEX", INBOX_PAGE_INDEX);
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
        int notificationId = intent.getIntExtra("notificationId", -1);
        Log.d("DEBUG_PUSH_OPEN", type +  " " + action + " notid " + notificationId);
        if(notificationId != -1)
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