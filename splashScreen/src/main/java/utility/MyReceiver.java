package utility;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.parse.ParsePushBroadcastReceiver;

import org.json.JSONException;
import org.json.JSONObject;

import library.UtilString;
import notifications.NotificationGenerator;

/**
 * Customizing receiver & generating new notifications
 */
public class MyReceiver extends ParsePushBroadcastReceiver {

    @Override
    public void onPushReceive(Context context, Intent intent) {

        //retrieving json data on push receive
        Bundle extras = intent.getExtras();
        String jsonData = extras.getString("com.parse.Data");
        PendingIntent deleteIntent;
        String contentText = null;
        String groupName = null;

        try {
            String channel = intent.getExtras().getString("com.parse.Channel");

            if(jsonData != null) {
                JSONObject json = new JSONObject(jsonData);

                //Notification message
                contentText = json.getString("msg");
                if(UtilString.isBlank(contentText)) {
                    if(json.has("alert")) {
                        contentText = json.getString("alert");

                    }
                }

                //notification title
                groupName = json.getString("groupName");

                if(UtilString.isBlank(groupName))
                {
                    if(json.has("title")) {
                        groupName = json.getString("title");

                    }
                }

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
            e.printStackTrace();
        }
    }

    @Override
    protected void onPushOpen(Context context, Intent intent)
    {
        //on push open, clear all previous notifications
        NotificationGenerator.count=0;
        for(int i=0;i<10;i++)
        {
            NotificationGenerator.events[i]="";
        }
    }

    @Override
    protected void onPushDismiss(Context context,Intent intent)
    {
        //on push dismiss, clear all previous notifications
        NotificationGenerator.count=0;
        for(int i=0;i<10;i++)
        {
            NotificationGenerator.events[i]="";
        }
    }
}
