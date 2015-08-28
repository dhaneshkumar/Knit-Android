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
            if (customJSON.has("a"))
                Log.d("__CHAT_OS_Rec", "additionalData: " + customJSON.getJSONObject("a").toString());
        } catch (Throwable t) {
            t.printStackTrace();
        }

        showNotification(); //TODO implement showNotification()
    }

    void showNotification(){

    }
}