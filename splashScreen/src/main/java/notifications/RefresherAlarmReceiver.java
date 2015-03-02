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

import BackGroundProcesses.Refresher;
import trumplabs.schoolapp.Application;
import trumplabs.schoolapp.Constants;
import utility.Config;
import utility.Queries;
import utility.SessionManager;
import utility.Utility;

/**
 * Created by ashish on 18/1/15.
 */

public class RefresherAlarmReceiver extends WakefulBroadcastReceiver {
    Context alarmContext;

    public RefresherAlarmReceiver() {
    }

    // Called when the BroadcastReceiver gets an Intent it's registered to receive

    @Override
    public void onReceive(final Context context, Intent intent) {
        Log.d("DEBUG_ALARM_RECEIVER", "onReceive. Spawning a thread for handling events");
        alarmContext = context;
        Runnable r = new Runnable() {
            @Override
            public void run() {
                new Refresher(2);
            }
        };

        Thread t = new Thread(r);
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }
}