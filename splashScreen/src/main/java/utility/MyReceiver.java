package utility;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.parse.ParsePushBroadcastReceiver;

import org.json.JSONException;
import org.json.JSONObject;

import notifications.NotificationGenerator;

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
                String contentText = json.getString("msg");

                String groupName = json.getString("groupName");
                String type = null;
                String action = null;

                if(json.has("type")){
                    type = json.getString("type");
                }
                if(json.has("action")){
                    action = json.getString("action");
                }

                NotificationGenerator.generateNotification(context, contentText, groupName, type, action);
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
