package notifications;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Calendar;

import trumplabs.schoolapp.Constants;

/**
 * Created by ashish on 18/1/15.
 */
public class AlarmTrigger {
    static int EVENT_CHECKER_ALARM_ID = 1; //this  is the id event checker alarm

    static int EVENT_CHECKER_INTERVAL = 15 * Constants.MINUTE_MILLISEC; //15 minutes

    static int REFRESHER_ALARM_ID = 2; //this  is the id refresher alarm

    static int REFRESHER_INTERVAL = 15 * Constants.MINUTE_MILLISEC; //15 minutes

    public static void triggerEventCheckerAlarm(Context context){

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, EventCheckerAlarmReceiver.class);

        PendingIntent sender = PendingIntent.getBroadcast(context, EVENT_CHECKER_ALARM_ID, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        Calendar cal = Calendar.getInstance();
		am.setRepeating(AlarmManager.RTC_WAKEUP,cal.getTimeInMillis()+ EVENT_CHECKER_INTERVAL/3,
                EVENT_CHECKER_INTERVAL, sender);
        Log.d("DEBUG_ALARM_TRIGGER",  "Scheduled event checker alarm");
    }

    public static void cancelEventCheckerAlarm(Context context){

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, EventCheckerAlarmReceiver.class);

        PendingIntent sender = PendingIntent.getBroadcast(context, EVENT_CHECKER_ALARM_ID, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        am.cancel(sender);
        Log.d("DEBUG_ALARM_TRIGGER",  "Cancelled event checker alarm");
    }

    public static void triggerRefresherAlarm(Context context){

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, RefresherAlarmReceiver.class);

        PendingIntent sender = PendingIntent.getBroadcast(context, REFRESHER_ALARM_ID, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        Calendar cal = Calendar.getInstance();
        am.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis() + REFRESHER_INTERVAL,
                REFRESHER_INTERVAL, sender); //first trigger after refresher-interval

        Log.d("DEBUG_ALARM_TRIGGER",  "Scheduled refresher alarm");

        //But first trigger just now manually. Can't depend on alarm to trigger right now as it is inexact for repeating alarms
        RefresherAlarmReceiver.spawnRefresherThread();
    }

    public static void cancelRefresherAlarm(Context context){

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, RefresherAlarmReceiver.class);

        PendingIntent sender = PendingIntent.getBroadcast(context, REFRESHER_ALARM_ID, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        am.cancel(sender);
        Log.d("DEBUG_ALARM_TRIGGER",  "Cancelled refresher alarm");
    }
}