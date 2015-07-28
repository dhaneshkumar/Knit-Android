package notifications;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.Date;
import java.util.List;

import BackGroundProcesses.MemberList;
import trumplabs.schoolapp.Application;
import trumplabs.schoolapp.Classrooms;
import trumplabs.schoolapp.Constants;
import utility.Queries;
import utility.SessionManager;

/**
 * Created by ashish on 18/1/15.
 */

public class EventCheckerAlarmReceiver extends WakefulBroadcastReceiver {
    SessionManager session;

    //event name for different events
    //event id will be <username> + "-" + eventname(above)
    static String parentNoActivityEvent = "parent_no_activity"; //5 hours since signup
    static String teacherNoActivityEvent = "teacher_no_activity"; // 1 hour since signup
    static String teacherNoSubEvent = "teacher_no_sub" ; // + <CLASSID> no subscribers 3 days
    static String teacherNoMsgEvent = "teacher_no_msg"; // + <CLASSID> no message 5 days

    //messages for different events
    public static String parentNoActivityContent = "You haven't joined any classroom yet. Don't have any class-code? Invite teacher."; //go to invite page

    public static String teacherNoActivityContent = "You haven't created any classroom yet. Create you first classroom."; //go to create class dialog
    public static String teacherNoSubContent = /*Your classroom <classname> + */ " doesn't have any subscribers yet. Invite parents and students now !"; //go to invite page
    public static String teacherNoMsgContent = "You haven't sent any messages yet to class "; //+ "Send Now" //go inside class

    //time interval before event is supposed to occur
    static long parentNoActivityInterval = 5 * Constants.HOUR_MILLISEC; //5 hours
    static long teacherNoActivityInterval = 1 * Constants.HOUR_MILLISEC; //1 hours
    static long teacherNoSubInterval = 3 * Constants.DAY_MILLISEC; //3 days
    static long teacherNoMsgInterval = 5 * Constants.DAY_MILLISEC; //5 days

    ParseUser user;
    Context alarmContext;

    Queries queryInstance;
    public EventCheckerAlarmReceiver() {
        queryInstance = new Queries();
    }

    // Called when the BroadcastReceiver gets an Intent it's registered to receive

    @Override
    public void onReceive(final Context context, Intent intent) {
        Log.d("DEBUG_ALARM_RECEIVER", "onReceive. Spawning a thread for handling events");
        alarmContext = context;
        session = new SessionManager(Application.getAppContext());

        user = ParseUser.getCurrentUser();
        if(user == null) {
            Log.d("DEBUG_ALARM_RECEIVER", "onReceive. parse user null");
            return;
        }

        Runnable r = new Runnable() {
            @Override
            public void run(){
                checkForEvents();
            }
        };

        Thread t = new Thread(r);
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }

    public void checkForEvents(){

        if(user.getString("role").equalsIgnoreCase("parent")){
            parentNoActivity();
        }
        if(user.getString("role").equalsIgnoreCase("teacher")){
            teacherNoActivity();
            teacherNoSub();
            teacherNoMsg();
        }
    }

    public void parentNoActivity(){
        String eventid = user.getUsername() + "-" + parentNoActivityEvent;
        if(session.getAlarmEventState(eventid)) {
            Log.d("DEBUG_ALARM_RECEIVER", "parentNoActivity() " + eventid + " already happened");
            return; //we're done
        }

        //check if parent has joined any groups
        List<List<String>> joinedGroups = Classrooms.getJoinedGroups(user); //won't be null

        if(joinedGroups.size() > 0) {//we don't have default Kio class now
            Log.d("DEBUG_ALARM_RECEIVER", "parentNoActivity() already has joined extra class");
            session.setAlarmEventState(eventid, true);
            return;
        }

        Date signupTime = user.getCreatedAt();
        Date now = session.getCurrentTime();

        Long interval = now.getTime() - signupTime.getTime();
        Log.d("DEBUG_ALARM_RECEIVER", "parentNoActivity() joining interval" + interval/(Constants.MINUTE_MILLISEC) + "minutes");
        if(interval > parentNoActivityInterval){
            NotificationGenerator.generateNotification(alarmContext, parentNoActivityContent , Constants.DEFAULT_NAME, Constants.Notifications.TRANSITION_NOTIFICATION, Constants.Actions.INVITE_TEACHER_ACTION);
            Log.d("DEBUG_ALARM_RECEIVER", "parentNoActivity() " + eventid + " state changed to true");
            session.setAlarmEventState(eventid, true);
        }
    }

    //teacher hasn't created any class since he signed up
    public void teacherNoActivity(){
        String eventid = user.getUsername() + "-" + teacherNoActivityEvent;
        if(session.getAlarmEventState(eventid)) {
            Log.d("DEBUG_ALARM_RECEIVER", "teacherNoActivity() " + eventid + " already happened");
            return; //we're done
        }

        //check if teacher has created any groups
        List<String> createdGroups = user.getList(Constants.CREATED_GROUPS);

        if(createdGroups != null && createdGroups.size() > 0) {
            Log.d("DEBUG_ALARM_RECEIVER", "teacherNoActivity() already has classes");
            session.setAlarmEventState(eventid, true);
            return;
        }

        Date signupTime = user.getCreatedAt();

        Date now = session.getCurrentTime();

        Long interval = now.getTime() - signupTime.getTime();
        Log.d("DEBUG_ALARM_RECEIVER", "teacherNoActivity() joining interval" + interval/(Constants.MINUTE_MILLISEC) + "minutes");
        if(interval > teacherNoActivityInterval){
            NotificationGenerator.generateNotification(alarmContext, teacherNoActivityContent, Constants.DEFAULT_NAME, Constants.Notifications.TRANSITION_NOTIFICATION, Constants.Actions.CREATE_CLASS_ACTION);
            //generateLocalMessage(teacherNoActivityContent, Constants.DEFAULT_NAME);
            Log.d("DEBUG_ALARM_RECEIVER", "teacherNoActivity() " + eventid + " state changed to true");
            session.setAlarmEventState(eventid, true);
        }
    }

    //A created class has no subscribers
    public void teacherNoSub(){
        List<List<String>> createdGroups = user.getList(Constants.CREATED_GROUPS);
        if(createdGroups == null || createdGroups.size() == 0) return;

        Log.d("DEBUG_ALARM_RECEIVER", "teacherNoSub() created groups size" + createdGroups.size());

        for(int i=0; i<createdGroups.size(); i++){
            List<String> group = createdGroups.get(i);
            String groupCode = group.get(0); //0 is code
            Log.d("DEBUG_ALARM_RECEIVER", "teacherNoSub() entered  *" + groupCode + "*");
            String eventid = user.getUsername() + "-" + teacherNoSubEvent + "-" + groupCode;
            if(session.getAlarmEventState(eventid)) {
                Log.d("DEBUG_ALARM_RECEIVER", "teacherNoSub() " + eventid + " already happened");
                continue; //we're done
            }


            int memberCount = 0;
            try{
                memberCount = MemberList.getMemberCount(groupCode);
            }
            catch (ParseException e){
                e.printStackTrace();
                Log.d("DEBUG_ALARM_RECEIVER", "exception getting count " + memberCount);
                continue; //can't proceed further
            }

            Log.d("DEBUG_ALARM_RECEIVER", "member count " + memberCount);

            if(memberCount > 0) {
                Log.d("DEBUG_ALARM_RECEIVER", "teacherNoSub() " + eventid  +" already has subscribers");
                session.setAlarmEventState(eventid, true);
                continue;
            }

            ParseObject classroom = Queries.getCodegroupObject(groupCode);

            if(classroom == null) {
                Log.d("DEBUG_ALARM_RECEIVER", "teacherNoSub() " + eventid  +"classroom is null without exception");
                continue;
            }
            Log.d("DEBUG_ALARM_RECEIVER", "teacherNoSub() " + eventid  +"1 already has subscribers");

            Date classCreationTime = classroom.getCreatedAt();
            if(classCreationTime == null) continue;
            Log.d("DEBUG_ALARM_RECEIVER", "teacherNoSub() " + eventid  +"2 already has subscribers");

            Date now = session.getCurrentTime();

            Log.d("DEBUG_ALARM_RECEIVER", "teacherNoSub() " + eventid  +"3 already has subscribers");
            if(now == null) continue;
            Log.d("DEBUG_ALARM_RECEIVER", "teacherNoSub() " + eventid  +"4 already has subscribers");

            Long interval = now.getTime() - classCreationTime.getTime();
            Log.d("DEBUG_ALARM_RECEIVER", "teacherNoSub() " + eventid + " class creation interval " + interval/(Constants.MINUTE_MILLISEC) + "minutes");
            if(interval > teacherNoSubInterval){
                String className = classroom.getString(Constants.Codegroup.NAME); //get class name

                if(className == null || className.isEmpty()){
                    className = groupCode;
                }

                Bundle extras = new Bundle();
                Log.d("DEBUG_ALARM_RECEIVER", "teacherNoSub() : creating notification and local message for " + groupCode + " " + className);
                extras.putString("grpCode", groupCode);
                extras.putString("grpName", className);

                String text = "Your classroom " + className + teacherNoSubContent;
                NotificationGenerator.generateNotification(alarmContext, text, Constants.DEFAULT_NAME, Constants.Notifications.TRANSITION_NOTIFICATION, Constants.Actions.INVITE_PARENT_ACTION, extras);
                //generateLocalMessage(text, Constants.DEFAULT_NAME);
                Log.d("DEBUG_ALARM_RECEIVER", "teacherNoSub() " + eventid + " state changed to true");
                session.setAlarmEventState(eventid, true);
            }
        }
    }

    //A created class has no messages sent yet
    public void teacherNoMsg(){
        List<List<String>> createdGroups = user.getList(Constants.CREATED_GROUPS);
        if(createdGroups == null || createdGroups.size() == 0) return;

        Log.d("DEBUG_ALARM_RECEIVER", "teacherNoMsg() created groups size" + createdGroups.size());

        for(int i=0; i<createdGroups.size(); i++){
            List<String> group = createdGroups.get(i);
            String groupCode = group.get(0); //0 is code
            String eventid = user.getUsername() + "-" + teacherNoMsgEvent + "-" + groupCode;
            if(session.getAlarmEventState(eventid)) {
                Log.d("DEBUG_ALARM_RECEIVER", "teacherNoMsg() " + eventid + " already happened");
                continue; //we're done
            }


            //get number of sent messages
            ParseQuery<ParseObject> query = ParseQuery.getQuery(Constants.SENT_MESSAGES_TABLE);
            query.fromLocalDatastore();
            query.orderByDescending("creationTime");
            query.whereEqualTo("userId", user.getUsername());
            query.whereEqualTo("code", groupCode);

            int numMessagesSent = 0;
            try{
                numMessagesSent = query.count();
            }
            catch (ParseException e){
                e.printStackTrace();
                continue;
            }

            if(numMessagesSent > 0){
                Log.d("DEBUG_ALARM_RECEIVER", "teacherNoMsg() " + eventid  +" already has sent messages");
                session.setAlarmEventState(eventid, true);
                continue;
            }

            ParseObject classroom = Queries.getCodegroupObject(groupCode);
            if(classroom == null) continue;

            Date classCreationTime = classroom.getCreatedAt();
            if(classCreationTime == null) continue;

            Date now = session.getCurrentTime();

            Long interval = now.getTime() - classCreationTime.getTime();
            Log.d("DEBUG_ALARM_RECEIVER", "teacherNoMsg() " + eventid + " class creation interval " + interval/(Constants.MINUTE_MILLISEC) + "minutes");
            if(interval > teacherNoMsgInterval){
                String className = classroom.getString(Constants.Codegroup.NAME); //get class name

                if(className == null || className.isEmpty()){
                    className = groupCode;
                }

                Bundle extras = new Bundle();
                Log.d("DEBUG_ALARM_RECEIVER", "teacherNoMsg() : creating notification and local message for " + groupCode + " " + className);
                extras.putString("grpCode", groupCode);
                extras.putString("grpName", className);

                NotificationGenerator.generateNotification(alarmContext, teacherNoMsgContent + className + ". Send a message now !", Constants.DEFAULT_NAME, Constants.Notifications.TRANSITION_NOTIFICATION, Constants.Actions.SEND_MESSAGE_ACTION, extras);
                //generateLocalMessage(teacherNoMsgContent + className, Constants.DEFAULT_NAME);
                Log.d("DEBUG_ALARM_RECEIVER", "teacherNoMsg() " + eventid + " state changed to true");
                session.setAlarmEventState(eventid, true);
            }
        }
    }

    public static void generateLocalMessage(String content, String code, String creator, String senderId, String grpName, ParseUser user){
        SessionManager session = new SessionManager(Application.getAppContext());
        //generate local message
        final ParseObject localMsg = new ParseObject("LocalMessages");
        localMsg.put("Creator", creator);
        localMsg.put("code", code);
        localMsg.put("name", grpName);
        localMsg.put("title", content);
        localMsg.put("userId", user.getUsername());
        localMsg.put("senderId", senderId);
        localMsg.put("creationTime", session.getCurrentTime());

        localMsg.pinInBackground();
    }
}