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
    static int ALARM_ID = 1; //this is the id for all alarms triggered

    static int INTERVAL = 2 * Constants.MINUTE_MILLISEC;

    public static void triggerAlarm(Context context){

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmReceiver.class);

        PendingIntent sender = PendingIntent.getBroadcast(context, ALARM_ID , intent, PendingIntent.FLAG_CANCEL_CURRENT);

        Calendar cal = Calendar.getInstance();
		am.setRepeating(AlarmManager.RTC_WAKEUP,cal.getTimeInMillis()+ INTERVAL/5,
                INTERVAL, sender);
        Log.d("DEBUG_ALARM_GENERATOR",  "Scheduling after every 2 minutes");
    }

    public static void cancelAlarm(Context context){

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmReceiver.class);

        PendingIntent sender = PendingIntent.getBroadcast(context, ALARM_ID , intent, PendingIntent.FLAG_CANCEL_CURRENT);

        am.cancel(sender);
        Log.d("DEBUG_ALARM_GENERATOR",  "Cancelled all alarms");
    }
}
