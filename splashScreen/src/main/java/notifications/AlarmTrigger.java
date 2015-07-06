package notifications;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Calendar;

import utility.Config;

/**
 * Created by ashish on 18/1/15.
 */
public class AlarmTrigger {
    final static int EVENT_CHECKER_ALARM_ID = 1; //this  is the id event checker alarm

    final static int REFRESHER_ALARM_ID = 2; //this  is the id refresher alarm

    final static int NOTIFICATION_ALARM_ID = 3; //this  is the id notification alarm

    public static void triggerEventCheckerAlarm(Context context){

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, EventCheckerAlarmReceiver.class);

        PendingIntent sender = PendingIntent.getBroadcast(context, EVENT_CHECKER_ALARM_ID, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        Calendar cal = Calendar.getInstance();
		am.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis() + Config.EVENT_CHECKER_INTERVAL / 3,
                Config.EVENT_CHECKER_INTERVAL, sender);
        Log.d("DEBUG_ALARM_TRIGGER",  "Scheduled event checker alarm");
    }

    public static void cancelEventCheckerAlarm(Context context){

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, EventCheckerAlarmReceiver.class);

        PendingIntent sender = PendingIntent.getBroadcast(context, EVENT_CHECKER_ALARM_ID, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        am.cancel(sender);
        Log.d("DEBUG_ALARM_TRIGGER", "Cancelled event checker alarm");
    }

    public static void triggerRefresherAlarm(Context context){

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, RefresherAlarmReceiver.class);

        PendingIntent sender = PendingIntent.getBroadcast(context, REFRESHER_ALARM_ID, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        Calendar cal = Calendar.getInstance();
        am.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis() + Config.REFRESHER_INTERVAL,
                Config.REFRESHER_INTERVAL, sender); //first trigger after refresher-interval

        Log.d("DEBUG_ALARM_TRIGGER", "Scheduled refresher alarm");

        //But first trigger just now manually. Can't depend on alarm to trigger right now as it is inexact for repeating alarms
        RefresherAlarmReceiver.spawnRefresherThread();
    }

    public static void cancelRefresherAlarm(Context context){

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, RefresherAlarmReceiver.class);

        PendingIntent sender = PendingIntent.getBroadcast(context, REFRESHER_ALARM_ID, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        am.cancel(sender);
        Log.d("DEBUG_ALARM_TRIGGER", "Cancelled refresher alarm");
    }

    public static boolean isNotificationAlarmTriggered(Context context){
        Intent intent = new Intent(context, NotificationAlarmReceiver.class);
        boolean alarmUp = (PendingIntent.getBroadcast(context, NOTIFICATION_ALARM_ID, intent, PendingIntent.FLAG_NO_CREATE) != null);
        return alarmUp;
    }

    public static void triggerNotificationAlarm(Context context){
        //check if already running, if yes then don't do anything
        //otherwise set alarm

        if(isNotificationAlarmTriggered(context)) {
            Log.d("DEBUG_NOT_ALARM", "triggerNotificationAlarm : already running");
            return;
        }

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, NotificationAlarmReceiver.class);

        PendingIntent sender = PendingIntent.getBroadcast(context, NOTIFICATION_ALARM_ID, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        Calendar cal = Calendar.getInstance();
        //first one trigger now !!
        am.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(),
                Config.NOTIFICATION_INTERVAL, sender); //first trigger now

        Log.d("DEBUG_NOT_ALARM", "triggerNotificationAlarm : scheduled notification alarm");
    }

    public static void cancelNotificationAlarm(Context context){

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, NotificationAlarmReceiver.class);

        PendingIntent sender = PendingIntent.getBroadcast(context, NOTIFICATION_ALARM_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        am.cancel(sender); //stop alarm

        sender.cancel(); //delete pending intent so that isNotificationAlarmTriggered() works as expected

        Log.d("DEBUG_NOT_ALARM", "Cancelled notification alarm");
    }
}