package chat;

/**
 * Created by ashish on 28/8/15.
 */

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import org.json.JSONObject;

import notifications.NotificationGenerator;
import trumplabs.schoolapp.Constants;

public class OneSignalBroadcastReceiver extends BroadcastReceiver {

    // You may consider adding a wake lock here if you need to make sure the devices doesn't go to sleep while processing.
    // We recommend starting a service of your own here if your doing any async calls or doing any heavy processing.
    @Override
    public void onReceive(Context context, Intent intent) {

        com.onesignal.GcmBroadcastReceiver d; //analyze the flow from here to make sure OneSignal doesnot show notification by itself

        Bundle dataBundle = intent.getBundleExtra("data");

        Log.d("__CHAT_OS_Rec", "overall bundle=" + dataBundle);

        try {
            Log.d("__CHAT_OS_Rec", "Notification content: " + dataBundle.getString("alert"));
            Log.d("__CHAT_OS_Rec", "Notification title: " + dataBundle.getString("title"));
            Log.d("__CHAT_OS_Rec", "Is Your App Active: " + dataBundle.getBoolean("isActive"));

            JSONObject customJSON = new JSONObject(dataBundle.getString("custom"));
            if (customJSON.has("a")) {
                Log.d("__CHAT_OS_Rec", "additionalData: " + customJSON.getJSONObject("a").toString());
                JSONObject customData = customJSON.getJSONObject("a");
                String msgTitle = customData.getString("msgTitle");
                String msgContent = customData.getString("msgContent");
                String channel = customData.getString("channel");
                showNotification(context, msgTitle, msgContent, channel);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    void showNotification(Context context, String msgTitle, String msgContent, String channel){
        //currently transition->classrooms
        //todo 1) if specified 'channel' chat page active, don't do anything here(that activity will handle it); 2) add new transition action 'chat'
        //if app is active and notification is for other channel whose activity is not visible, then their registered listeners will automatically handle it
        NotificationGenerator.generateNotification(context, msgContent, msgTitle,
                Constants.Notifications.TRANSITION_NOTIFICATION, Constants.Actions.CLASSROOMS_ACTION);
    }
}