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

import BackGroundProcesses.SeenHandler;
import BackGroundProcesses.SyncMessageDetails;
import trumplabs.schoolapp.Constants;
import trumplabs.schoolapp.Messages;
import utility.Config;
import utility.Queries;
import utility.SessionManager;
import utility.Utility;

/**
 * Created by ashish on 18/1/15.
 */

public class AlarmReceiver extends WakefulBroadcastReceiver {
    SessionManager session;

    static long teacherConfusingMsgThreshold = 5; //how may confused_count to call a post confusing(greater than or equal to)
    static int teacherConfusingMsgScanCount = 1; //how many messages to look in each classroom for confusing event

    //event name for different events
    //event id will be <username> + "-" + eventname(above)
    static String parentNoActivityEvent = "parent_no_activity"; //2 hours since signup
    static String teacherNoActivityEvent = "teacher_no_activity"; // 1 hour since signup
    static String teacherNoSubEvent = "teacher_no_sub" ; // + <CLASSID> no subscribers 3 days
    static String teacherNoMsgEvent = "teacher_no_msg"; // + <CLASSID> no message 5 days
    static String teacherConfusingMsgEvent = "teacher_confusing_msg"; //+ <MSG_OBJECT_ID> more than 5 confusing

    //messages for different events
    static String parentNoActivityContent = "You have not joined any classes yet. If you don't have a code, you can invite a teacher";
    static String teacherNoActivityContent = "You haven't created any classes yet. Please create a class and invite parents";
    static String teacherNoSubContent = "You don't have any subscribers yet for class. Please invite parents onboard to class ";
    static String teacherNoMsgContent = "You haven't sent any messages yet to class ";
    static String teacherConfusingMsgContent = /* <confused_count> + */ " parents seem to be confused regarding your recent post in class "; // [class name]


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
        Log.d("DEBUG_ALARM_RECEIVER", "onReceive. Spawning a thread for handling events");
        alarmContext = context;

        user = ParseUser.getCurrentUser();
        if(user == null) return;

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

        session = new SessionManager(alarmContext);
        Utility.updateCurrentTime(user, session);

        if(user.getString("role").equalsIgnoreCase("parent")){
            parentNoActivity();
        }
        if(user.getString("role").equalsIgnoreCase("teacher")){
            teacherNoActivity();
            teacherNoSub();
            teacherNoMsg();
            teacherConfusingMessage();
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
            NotificationGenerator.generateNotification(alarmContext, parentNoActivityContent , Constants.DEFAULT_NAME, Constants.TRANSITION_NOTIFICATION, Constants.INVITE_TEACHER_ACTION);

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
            NotificationGenerator.generateNotification(alarmContext, teacherNoActivityContent, Constants.DEFAULT_NAME, Constants.TRANSITION_NOTIFICATION, Constants.CLASSROOMS_ACTION);
            generateLocalMessage(teacherNoActivityContent, Constants.DEFAULT_NAME);
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
                memberCount = queryInstance.getMemberCount(groupCode);
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

            ParseObject classroom = null;
            try{
                classroom = queryInstance.getClassObject(groupCode);
            }
            catch (ParseException e){
                e.printStackTrace();
                Log.d("DEBUG_ALARM_RECEIVER", "teacherNoSub() exception");
                continue; //can't proceed
            }
            if(classroom == null) {
                Log.d("DEBUG_ALARM_RECEIVER", "teacherNoSub() " + eventid  +"classroom is null without exception");
                continue;
            }
            Log.d("DEBUG_ALARM_RECEIVER", "teacherNoSub() " + eventid  +"1 already has subscribers");

            Date classCreationTime = classroom.getCreatedAt();
            if(classCreationTime == null) continue;
            Log.d("DEBUG_ALARM_RECEIVER", "teacherNoSub() " + eventid  +"2 already has subscribers");

            Date now = null;
            try{
                now = session.getCurrentTime();
            }
            catch (java.text.ParseException e){
                e.printStackTrace();
                continue; //can't proceed further
            }
            Log.d("DEBUG_ALARM_RECEIVER", "teacherNoSub() " + eventid  +"3 already has subscribers");
            if(now == null) continue;
            Log.d("DEBUG_ALARM_RECEIVER", "teacherNoSub() " + eventid  +"4 already has subscribers");

            Long interval = now.getTime() - classCreationTime.getTime();
            Log.d("DEBUG_ALARM_RECEIVER", "teacherNoSub() " + eventid + " class creation interval " + interval/(Constants.MINUTE_MILLISEC) + "minutes");
            if(interval > teacherNoSubInterval){
                String className = classroom.getString("name"); //get class name

                if(className == null || className.isEmpty()){
                    className = groupCode;
                }

                Bundle extras = new Bundle();
                extras.putString("grpCode", groupCode);
                extras.putString("grpName", classroom.getString("name"));

                NotificationGenerator.generateNotification(alarmContext, teacherNoSubContent + className, Constants.DEFAULT_NAME, Constants.TRANSITION_NOTIFICATION, Constants.INVITE_PARENT_ACTION, extras);
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
                continue;
            }

            if(numMessagesSent > 0){
                Log.d("DEBUG_ALARM_RECEIVER", "teacherNoMsg() " + eventid  +" already has sent messages");
                session.setAlarmEventState(eventid, true);
                continue;
            }

            ParseObject classroom = null;
            try{
                classroom = queryInstance.getClassObject(groupCode);
            }
            catch (ParseException e){
                e.printStackTrace();
                continue; //can't proceed
            }
            if(classroom == null) continue;

            Date classCreationTime = classroom.getCreatedAt();
            if(classCreationTime == null) continue;

            Date now = null;
            try{
                now = session.getCurrentTime();
            }
            catch (java.text.ParseException e){
                e.printStackTrace();
                continue; //can't proceed further
            }
            if(now == null) continue;

            Long interval = now.getTime() - classCreationTime.getTime();
            Log.d("DEBUG_ALARM_RECEIVER", "teacherNoMsg() " + eventid + " class creation interval " + interval/(Constants.MINUTE_MILLISEC) + "minutes");
            if(interval > teacherNoMsgInterval){
                String className = classroom.getString("name"); //get class name

                if(className == null || className.isEmpty()){
                    className = groupCode;
                }

                NotificationGenerator.generateNotification(alarmContext, teacherNoMsgContent + className, Constants.DEFAULT_NAME, Constants.TRANSITION_NOTIFICATION, Constants.CLASSROOMS_ACTION);
                generateLocalMessage(teacherNoMsgContent + className, Constants.DEFAULT_NAME);
                Log.d("DEBUG_ALARM_RECEIVER", "teacherNoMsg() " + eventid + " state changed to true");
                session.setAlarmEventState(eventid, true);
            }
        }
    }

    //A latest message in any of the classrooms have confused count >=
    public void teacherConfusingMessage(){
        List<List<String>> createdGroups = user.getList(Constants.CREATED_GROUPS);
        if(createdGroups == null || createdGroups.size() == 0) return;

        for(int i=0; i<createdGroups.size(); i++){
            List<String> group = createdGroups.get(i);
            String groupCode = group.get(0); //0 is code

            ParseQuery<ParseObject> query = ParseQuery.getQuery("SentMessages");
            query.fromLocalDatastore();
            query.orderByDescending("creationTime");
            query.whereEqualTo("userId", user.getUsername());
            query.whereEqualTo("code", groupCode);
            query.setLimit(teacherConfusingMsgScanCount); //We will monitor just these many messages

            List<ParseObject> outMessages = null;
            try{
                outMessages = query.find();
            }
            catch (ParseException e){
                e.printStackTrace();
                continue;
            }

            if(outMessages == null || outMessages.size() ==0){
                continue;
            }


            for(int m=0; m<teacherConfusingMsgScanCount; m++){
                ParseObject msg = outMessages.get(m);
                String eventid = user.getUsername() + "-" + teacherConfusingMsgEvent + "-" + msg.getString("objectId");
                if(session.getAlarmEventState(eventid)) {
                    Log.d("DEBUG_ALARM_RECEIVER", "teacherConfusingMessage() " + eventid + " already happened");
                    continue; //we're done
                }

                int confusedCount = Utility.nonNegative(msg.getInt(Constants.CONFUSED_COUNT));
                Log.d("DEBUG_ALARM_RECEIVER", "teacherConfusingMessage() " + eventid + " confused_count " + confusedCount);
                if(confusedCount >= teacherConfusingMsgThreshold){
                    String notificationMessage = confusedCount + teacherConfusingMsgContent + msg.getString("name");

                    NotificationGenerator.generateNotification(alarmContext, notificationMessage, Constants.DEFAULT_NAME, Constants.TRANSITION_NOTIFICATION, Constants.CLASSROOMS_ACTION);
                    generateLocalMessage(notificationMessage, Constants.DEFAULT_NAME);
                    Log.d("DEBUG_ALARM_RECEIVER", "teacherConfusingMessage() " + eventid + " state changed to true");
                    session.setAlarmEventState(eventid, true);
                }
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