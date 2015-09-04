package utility;

import android.content.Intent;
import android.os.Bundle;

import com.parse.ParseUser;

import notifications.NotificationGenerator;
import profileDetails.ProfilePage;
import trumplab.textslate.BuildConfig;
import trumplabs.schoolapp.Application;
import trumplabs.schoolapp.Constants;

/**
 * Created by ashish on 7/7/15.
 */
public class TestingUtililty {
    public static void testingTutorial(){
        if(!BuildConfig.DEBUG){
            return;
        }

        //Constants.IS_SIGNUP = true;

        ParseUser currentParseUser = ParseUser.getCurrentUser();
        if(currentParseUser == null){
            return;
        }

        /*SessionManager mgr = new SessionManager(Application.getAppContext());
        String p_flag = currentParseUser.getUsername() + Constants.TutorialKeys.PARENT_RESPONSE;
        String t_flag = currentParseUser.getUsername() + Constants.TutorialKeys.TEACHER_RESPONSE;
        String o_flag = currentParseUser.getUsername() + Constants.TutorialKeys.OPTIONS;
        String c_flag = currentParseUser.getUsername() + Constants.TutorialKeys.COMPOSE;
        String i_flag = currentParseUser.getUsername() + Constants.TutorialKeys.JOIN_INVITE;
        mgr.setTutorialState(p_flag, false);
        mgr.setTutorialState(t_flag, false);
        mgr.setTutorialState(o_flag, false);
        mgr.setTutorialState(c_flag, false);
        mgr.setTutorialState(i_flag, false);*/

        //delete SentMessges
        /*ParseQuery deleteOutbox = new ParseQuery(Constants.SENT_MESSAGES_TABLE);
        deleteOutbox.fromLocalDatastore();
        deleteOutbox.whereEqualTo("userId", currentParseUser.getUsername());
        try{
            List<ParseObject> msgs = deleteOutbox.find();
            if(Config.SHOWLOG) Log.d("_DELETE_OUTBOX_", "deleted " + msgs.size());
            ParseObject.unpinAll(msgs);
        }
        catch (ParseException e){
            e.printStackTrace();
        }*/

        //delete Inbox messages
        /*ParseQuery deleteInbox = new ParseQuery(Constants.TABLE);
        deleteInbox.fromLocalDatastore();
        deleteInbox.whereEqualTo("userId", currentParseUser.getUsername());
        try{
            List<ParseObject> msgs = deleteInbox.find();
            if(Config.SHOWLOG) Log.d("_DELETE_OUTBOX_", "deleted " + msgs.size());
            ParseObject.unpinAll(msgs);
        }
        catch (ParseException e){
            e.printStackTrace();
        }*/

        //delete local created messages
        /*ParseQuery deleteLocal = new ParseQuery("LocalMessages");
        deleteLocal.fromLocalDatastore();
        deleteLocal.whereEqualTo("userId", currentParseUser.getUsername());
        try{
            List<ParseObject> msgs = deleteLocal.find();
            if(Config.SHOWLOG) Log.d("_DELETE_OUTBOX_", "deleted " + msgs.size());
            ParseObject.unpinAll(msgs);
        }
        catch (ParseException e){
            e.printStackTrace();
        }*/
    }

    static boolean gen = false;
    public static void testingLocalNotification(){
        if(!BuildConfig.DEBUG){
            return;
        }

        //testing local notification flow
        if(!gen){
            gen = true;
            Bundle extras = new Bundle();
            extras.putString("grpCode", "ZCO3103");
            extras.putString("grpName", "O CLOCK");
            //NotificationGenerator.generateNotification(this, "Send a message now !", Constants.DEFAULT_NAME, Constants.Notifications.TRANSITION_NOTIFICATION, Constants.Actions.SEND_MESSAGE_ACTION, extras);
            //NotificationGenerator.generateNotification(this, "invite teacher now!" , Constants.DEFAULT_NAME, Constants.Notifications.TRANSITION_NOTIFICATION, Constants.Actions.INVITE_TEACHER_ACTION);
            NotificationGenerator.generateNotification(Application.getAppContext(), "invite parent to your class", Constants.DEFAULT_NAME, Constants.Notifications.TRANSITION_NOTIFICATION, Constants.Actions.INVITE_PARENT_ACTION, extras);
        }
    }

    public static void makeCurrentUserNull(){
        if(!BuildConfig.DEBUG){
            return;
        }

        ParseUser currentParseUser = ParseUser.getCurrentUser();
        if(currentParseUser != null){
            currentParseUser.unpinInBackground();
        }
        ParseUser.logOut();
    }

    //get caller class and method
    static String getCaller() {
        int INDEX = 4;
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        if (stackTraceElements.length > INDEX) {
            return stackTraceElements[INDEX].getClassName() + ":" + stackTraceElements[INDEX].getMethodName();
        }

        return "NONE";
    }

    //create intentional delay while doing something(for testing, e.g loading of images)
    static void sleep(int millis){
        if(!BuildConfig.DEBUG){
            return;
        }

        try{
            Thread.sleep(millis);
        }
        catch (InterruptedException e){
            e.printStackTrace();
        }
    }

    public static void launchProfileActivity(){
        Intent i = new Intent(Application.getAppContext(), ProfilePage.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Application.getAppContext().startActivity(i);
    }
}
