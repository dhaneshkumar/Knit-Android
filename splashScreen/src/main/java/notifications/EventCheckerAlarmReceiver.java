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

import trumplabs.schoolapp.Application;
import trumplabs.schoolapp.Classrooms;
import trumplabs.schoolapp.Constants;
import utility.Config;
import utility.Queries;
import utility.SessionManager;
import utility.Utility;

/**
 * Created by ashish on 18/1/15.
 */

public class EventCheckerAlarmReceiver extends WakefulBroadcastReceiver {
    SessionManager session;

    static long teacherConfusingMsgThreshold = 5; //how may confused_count to call a post confusing(greater than or equal to)
    static int teacherConfusingMsgScanCount = 1; //how many messages to look in each classroom for confusing event

    //event name for different events
    //event id will be <username> + "-" + eventname(above)
    static String parentNoActivityEvent = "parent_no_activity"; //5 hours since signup
    static String parentTip1Event = "parent_tip_1"; //1 hour
    static String teacherNoActivityEvent = "teacher_no_activity"; // 1 hour since signup
    static String teacherTip1Event = "teacher_tip_1"; //1 hour
    static String teacherNoSubEvent = "teacher_no_sub" ; // + <CLASSID> no subscribers 3 days
    static String teacherNoMsgEvent = "teacher_no_msg"; // + <CLASSID> no message 5 days
    static String teacherConfusingMsgEvent = "teacher_confusing_msg"; //+ <MSG_OBJECT_ID> more than 5 confusing
    static String teacherSendingDailyTip = "teacher_sending_daily_tip";

    //messages for different events
    public static String parentNoActivityContent = "You haven't joined any classroom yet. Don't have any class-code? Invite teacher."; //go to invite page
    public static String parentTip1Content = "You can respond to messages via 2 buttons - push \"thumbs-up\" to like and \"question-mark\" to show that you are confused."; //go to inbox

    public static String teacherNoActivityContent = "You haven't created any classroom yet. Create you first classroom."; //go to create class dialog
    public static String teacherTip1Content = "Parents can respond to your messages only via 2 buttons? They push \"thumbs-up\" to like and \"question-mark\" to show they're confused."; //go to outbox
    public static String teacherNoSubContent = /*Your classroom <classname> + */ " doesn't have any subscribers yet. Invite parents and students now !"; //go to invite page
    public static String teacherNoMsgContent = "You haven't sent any messages yet to class "; //+ "Send Now" //go inside class
    public static String teacherConfusingMsgContent = /* <confused_count> + */ " parents/students seem to be confused regarding your recent post in class "; // [class name] go to outbox


    //Not used
    static String teacherSendingDailyTipContent = "How's going so far? Have you tried sending daily tips to parents ? Many of our teachers do that already and parents love it. Give it a try I would say.";


    //time interval before event is supposed to occur
    static long parentNoActivityInterval = 5 * Constants.HOUR_MILLISEC; //5 hours
    static long teacherNoActivityInterval = 1 * Constants.HOUR_MILLISEC; //1 hours
    static long teacherNoSubInterval = 3 * Constants.DAY_MILLISEC; //3 days
    static long teacherNoMsgInterval = 5 * Constants.DAY_MILLISEC; //5 days
    static long teacherSendingDailyTipInterval = 3 * Constants.DAY_MILLISEC; //3 days of first classroom creation
    static long tip1Interval = 1 * Constants.HOUR_MILLISEC; //1 hour

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
            parentTip1();
        }
        if(user.getString("role").equalsIgnoreCase("teacher")){
            teacherNoActivity();
            teacherNoSub();
            teacherNoMsg();
            teacherConfusingMessage();
            //teacherSendingDailyTip();
            teacherTip1();
        }
    }

    //parent hasn't joined any class since he has signed up

    public void parentTip1(){
        String eventid = user.getUsername() + "-" + parentTip1Event;
        if(session.getAlarmEventState(eventid)) {
            Log.d("DEBUG_ALARM_RECEIVER", "parentTip1() " + eventid + " already happened");
            return; //we're done
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
        Log.d("DEBUG_ALARM_RECEIVER", "parentTip1() joining interval" + interval/(Constants.MINUTE_MILLISEC) + "minutes");
        if(interval > tip1Interval){
            NotificationGenerator.generateNotification(alarmContext, parentTip1Content , Constants.DEFAULT_NAME, Constants.NORMAL_NOTIFICATION, Constants.INBOX_ACTION);
            Log.d("DEBUG_ALARM_RECEIVER", "parentTip1() " + eventid + " state changed to true");
            session.setAlarmEventState(eventid, true);
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
            NotificationGenerator.generateNotification(alarmContext, teacherNoActivityContent, Constants.DEFAULT_NAME, Constants.TRANSITION_NOTIFICATION, Constants.CREATE_CLASS_ACTION);
            //generateLocalMessage(teacherNoActivityContent, Constants.DEFAULT_NAME);
            Log.d("DEBUG_ALARM_RECEIVER", "teacherNoActivity() " + eventid + " state changed to true");
            session.setAlarmEventState(eventid, true);
        }
    }

    public void teacherTip1(){
        String eventid = user.getUsername() + "-" + teacherTip1Event;
        if(session.getAlarmEventState(eventid)) {
            Log.d("DEBUG_ALARM_RECEIVER", "teacherTip1() " + eventid + " already happened");
            return; //we're done
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
        Log.d("DEBUG_ALARM_RECEIVER", "teacherTip1() joining interval" + interval/(Constants.MINUTE_MILLISEC) + "minutes");
        if(interval > tip1Interval){
            NotificationGenerator.generateNotification(alarmContext, teacherTip1Content , Constants.DEFAULT_NAME, Constants.TRANSITION_NOTIFICATION, Constants.OUTBOX_ACTION);
            Log.d("DEBUG_ALARM_RECEIVER", "teacherTip1() " + eventid + " state changed to true");
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
                Log.d("DEBUG_ALARM_RECEIVER", "teacherNoSub() : creating notification and local message for " + groupCode + " " + className);
                extras.putString("grpCode", groupCode);
                extras.putString("grpName", className);

                String text = "Your classroom " + className + teacherNoSubContent;
                NotificationGenerator.generateNotification(alarmContext, text, Constants.DEFAULT_NAME, Constants.TRANSITION_NOTIFICATION, Constants.INVITE_PARENT_ACTION, extras);
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

                Bundle extras = new Bundle();
                Log.d("DEBUG_ALARM_RECEIVER", "teacherNoMsg() : creating notification and local message for " + groupCode + " " + className);
                extras.putString("grpCode", groupCode);
                extras.putString("grpName", className);

                NotificationGenerator.generateNotification(alarmContext, teacherNoMsgContent + className + ". Send a message now !", Constants.DEFAULT_NAME, Constants.TRANSITION_NOTIFICATION, Constants.SEND_MESSAGE_ACTION);
                //generateLocalMessage(teacherNoMsgContent + className, Constants.DEFAULT_NAME);
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

                    NotificationGenerator.generateNotification(alarmContext, notificationMessage, Constants.DEFAULT_NAME, Constants.TRANSITION_NOTIFICATION, Constants.OUTBOX_ACTION);
                    //generateLocalMessage(notificationMessage, Constants.DEFAULT_NAME);
                    Log.d("DEBUG_ALARM_RECEIVER", "teacherConfusingMessage() " + eventid + " state changed to true");
                    session.setAlarmEventState(eventid, true);
                }
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


        try{
            localMsg.put("creationTime", session.getCurrentTime());
        }
        catch (java.text.ParseException e) {
            e.printStackTrace();
        }

        localMsg.pinInBackground();
    }
}