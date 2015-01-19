package notifications;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.Date;
import java.util.List;

import trumplabs.schoolapp.Constants;
import utility.Config;
import utility.Queries;
import utility.SessionManager;

/**
 * Created by ashish on 18/1/15.
 */

public class AlarmReceiver extends WakefulBroadcastReceiver {
    SessionManager session;

    //event name for different events
    //event id will be <username> + "-" + eventname(above)
    static String parentNoActivityEvent = "parent_no_activity"; //2 hours since signup
    static String teacherNoActivityEvent = "teacher_no_activity"; // 1 hour since signup
    static String teacherNoSubEvent = "teacher_no_sub" ; // + <CLASSID> no subscribers 3 days
    static String teacherNoMsgEvent = "teacher_no_msg"; // + <CLASSID> no message 5 days

    //notification ids for different events. NOT USED CURRENTLY
    static int parentNoActivityId = 100;
    static int teacherNoActivityId = 101;
    static int teacherNoSubId = 102;
    static int teacherNoMsgId = 103;

    //messages for different events
    static String parentNoActivityContent = "Invite teacher. You seem not to have joined any classes";
    static String teacherNoActivityContent = "Please create a class and invite parents";
    static String teacherNoSubContent = "You don't have any subscribers yet for class. Please invite parents onboard to class";
    static String teacherNoMsgContent = "You haven't sent any messages yet to class ";

    //time interval before event is supposed to occur
    static long parentNoActivityInterval = 2 * Constants.MINUTE_MILLISEC;
    static long teacherNoActivityInterval = 2 * Constants.MINUTE_MILLISEC;
    static long teacherNoSubInterval = 5 * Constants.MINUTE_MILLISEC;
    static long teacherNoMsgInterval = 5 * Constants.MINUTE_MILLISEC;

    ParseUser user;
    Context alarmContext;

    Queries queryInstance;
    public AlarmReceiver() {
        queryInstance = new Queries();
    }

    // Called when the BroadcastReceiver gets an Intent it's registered to receive

    @Override
    public void onReceive(final Context context, Intent intent) {
        Log.d("DEBUG_ALARM_RECEIVER", "onReceive");
        alarmContext = context;

        user = ParseUser.getCurrentUser();
        if(user == null) return;

        session = new SessionManager(context);

        checkForEvents();
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

    //parent hasn't joined any class since he has signed up
    public void parentNoActivity(){
        String eventid = user.getUsername() + "-" + parentNoActivityEvent;
        if(session.getAlarmEventState(eventid)) {
            Log.d("DEBUG_ALARM_RECEIVER", "parentNoActivity() " + eventid + " already happened");
            return; //we're done
        }

        //check if parent has joined any groups
        List<String> joinedGroups = user.getList("joined_groups");

        if(joinedGroups != null && joinedGroups.size() > 1) {
            //> 1 since we have default 1 class joined
            Log.d("DEBUG_ALARM_RECEIVER", "parentNoActivity() already has joined extra class");
            session.setAlarmEventState(eventid, true);
            return;
        }

        Date signupTime = user.getCreatedAt();
        Date now = null;
        try{
            now = session.getCurrentTime();
        }
        catch (java.text.ParseException e){
            e.printStackTrace();
            return; //can't proceed further
        }

        Long interval = now.getTime() - signupTime.getTime();
        Log.d("DEBUG_ALARM_RECEIVER", "parentNoActivity() joining interval" + interval/(Constants.MINUTE_MILLISEC) + "minutes");
        if(interval > parentNoActivityInterval){
            NotificationGenerator.generateNotification(alarmContext, parentNoActivityContent , Constants.DEFAULT_NAME);

            generateLocalMessage(parentNoActivityContent, Config.defaultParentGroupCode);

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

        Date now = null;
        try{
            now = session.getCurrentTime();
        }
        catch (java.text.ParseException e){
            e.printStackTrace();
            return; //can't proceed further
        }
        if(now == null) return;

        Long interval = now.getTime() - signupTime.getTime();
        Log.d("DEBUG_ALARM_RECEIVER", "teacherNoActivity() joining interval" + interval/(Constants.MINUTE_MILLISEC) + "minutes");
        if(interval > teacherNoActivityInterval){
            NotificationGenerator.generateNotification(alarmContext, teacherNoActivityContent, Constants.DEFAULT_NAME);
            generateLocalMessage(teacherNoActivityContent, Constants.DEFAULT_NAME);
            Log.d("DEBUG_ALARM_RECEIVER", "teacherNoActivity() " + eventid + " state changed to true");
            session.setAlarmEventState(eventid, true);
        }
    }

    //A created class has no subscribers
    public void teacherNoSub(){
        List<List<String>> createdGroups = user.getList(Constants.CREATED_GROUPS);
        if(createdGroups == null || createdGroups.size() == 0) return;

        for(int i=0; i<createdGroups.size(); i++){
            List<String> group = createdGroups.get(i);
            String groupCode = group.get(0); //0 is code
            String eventid = user.getUsername() + "-" + teacherNoSubEvent + "-" + groupCode;
            if(session.getAlarmEventState(eventid)) {
                Log.d("DEBUG_ALARM_RECEIVER", "teacherNoSub() " + eventid + " already happened");
                return; //we're done
            }


            int memberCount = 0;
            try{
                memberCount = queryInstance.getMemberCount(groupCode);
            }
            catch (ParseException e){
                e.printStackTrace();
                return; //can't proceed further
            }

            if(memberCount > 0) {
                Log.d("DEBUG_ALARM_RECEIVER", "teacherNoSub() " + eventid  +" already has subscribers");
                session.setAlarmEventState(eventid, true);
                return;
            }

            ParseObject classroom = null;
            try{
                classroom = queryInstance.getClassObject(groupCode);
            }
            catch (ParseException e){
                e.printStackTrace();
                return; //can't proceed
            }
            if(classroom == null) return;

            Date classCreationTime = classroom.getCreatedAt();
            if(classCreationTime == null) return;

            Date now = null;
            try{
                now = session.getCurrentTime();
            }
            catch (java.text.ParseException e){
                e.printStackTrace();
                return; //can't proceed further
            }
            if(now == null) return;

            Long interval = now.getTime() - classCreationTime.getTime();
            Log.d("DEBUG_ALARM_RECEIVER", "teacherNoSub() " + eventid + " class creation interval " + interval/(Constants.MINUTE_MILLISEC) + "minutes");
            if(interval > teacherNoSubInterval){
                String className = classroom.getString("name"); //get class name

                if(className == null || className.isEmpty()){
                    className = groupCode;
                }

                NotificationGenerator.generateNotification(alarmContext, teacherNoSubContent + className, Constants.DEFAULT_NAME);
                generateLocalMessage(teacherNoSubContent + className, Constants.DEFAULT_NAME);
                Log.d("DEBUG_ALARM_RECEIVER", "teacherNoSub() " + eventid + " state changed to true");
                session.setAlarmEventState(eventid, true);
            }
        }
    }

    //A created class has no messages sent yet
    public void teacherNoMsg(){
        List<List<String>> createdGroups = user.getList(Constants.CREATED_GROUPS);
        if(createdGroups == null || createdGroups.size() == 0) return;

        for(int i=0; i<createdGroups.size(); i++){
            List<String> group = createdGroups.get(i);
            String groupCode = group.get(0); //0 is code
            String eventid = user.getUsername() + "-" + teacherNoMsgEvent + "-" + groupCode;
            if(session.getAlarmEventState(eventid)) {
                Log.d("DEBUG_ALARM_RECEIVER", "teacherNoMsg() " + eventid + " already happened");
                return; //we're done
            }


            //get number of sent messages
            ParseQuery<ParseObject> query = ParseQuery.getQuery("SentMessages");
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
            }

            if(numMessagesSent > 0){
                Log.d("DEBUG_ALARM_RECEIVER", "teacherNoMsg() " + eventid  +" already has sent messages");
                session.setAlarmEventState(eventid, true);
                return;
            }

            ParseObject classroom = null;
            try{
                classroom = queryInstance.getClassObject(groupCode);
            }
            catch (ParseException e){
                e.printStackTrace();
                return; //can't proceed
            }
            if(classroom == null) return;

            Date classCreationTime = classroom.getCreatedAt();
            if(classCreationTime == null) return;

            Date now = null;
            try{
                now = session.getCurrentTime();
            }
            catch (java.text.ParseException e){
                e.printStackTrace();
                return; //can't proceed further
            }
            if(now == null) return;

            Long interval = now.getTime() - classCreationTime.getTime();
            Log.d("DEBUG_ALARM_RECEIVER", "teacherNoMsg() " + eventid + " class creation interval " + interval/(Constants.MINUTE_MILLISEC) + "minutes");
            if(interval > teacherNoMsgInterval){
                String className = classroom.getString("name"); //get class name

                if(className == null || className.isEmpty()){
                    className = groupCode;
                }

                NotificationGenerator.generateNotification(alarmContext, teacherNoMsgContent + className, Constants.DEFAULT_NAME);
                generateLocalMessage(teacherNoMsgContent + className, Constants.DEFAULT_NAME);
                Log.d("DEBUG_ALARM_RECEIVER", "teacherNoMsg() " + eventid + " state changed to true");
                session.setAlarmEventState(eventid, true);
            }
        }
    }

    private void generateLocalMessage(String content, String code){
        //generate local message
        final ParseObject localMsg = new ParseObject("LocalMessages");
        localMsg.put("Creator", Constants.DEFAULT_CREATOR);
        localMsg.put("code", code);
        localMsg.put("name", Constants.DEFAULT_NAME);
        localMsg.put("title", content);
        localMsg.put("userId", user.getUsername());
        localMsg.put("senderId", Constants.DEFAULT_SENDER_ID);

        try{
            localMsg.put("creationTime", session.getCurrentTime());
        }
        catch (java.text.ParseException e) {
            e.printStackTrace();
        }

        localMsg.pinInBackground();
    }
}