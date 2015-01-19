package utility;

import library.UtilString;

import org.json.JSONException;
import org.json.JSONObject;

import notifications.NotificationGenerator;
import trumplab.textslate.R;
import trumplabs.schoolapp.Messages;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.parse.ParsePushBroadcastReceiver;

public class MyReceiver extends ParsePushBroadcastReceiver {

    @Override
    public void onPushReceive(Context context, Intent intent) {

        Bundle extras = intent.getExtras();
        String jsonData = extras.getString("com.parse.Data");
        PendingIntent deleteIntent;

        try {
            String channel = intent.getExtras().getString("com.parse.Channel");

            if(jsonData != null) {
                JSONObject json = new JSONObject(jsonData);
                String contenttext = json.getString("msg");

                String groupname = json.getString("groupName");

                NotificationGenerator.generateNotification(context, contenttext, groupname);
            }

        } catch (JSONException e) {
            Log.d("yo", "JSONException: " + e.getMessage());
        }
    }

    @Override
    protected void onPushOpen(Context context, Intent intent)
    {
        Log.d("Myreceiver","PushOpen called");
        NotificationGenerator.count=0;
        for(int i=0;i<10;i++)
        {
            NotificationGenerator.events[i]="";
        }
    }

    @Override
    protected void onPushDismiss(Context context,Intent intent)
    {
        Log.d("Myreceiver","PushDismiss called");
        NotificationGenerator.count=0;
        for(int i=0;i<10;i++)
        {
            NotificationGenerator.events[i]="";
        }
    }
}

