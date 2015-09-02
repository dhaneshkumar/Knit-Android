package chat;

/**
 * Created by ashish on 28/8/15.
 */

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.parse.ParseUser;

import org.json.JSONObject;

import java.util.Arrays;

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
                String channel = customData.optString("channel", null);
                String msgTitle = customData.optString("msgTitle", null);
                String msgContent = customData.optString("msgContent", null);

                String senderName = customData.optString("senderName", null);
                String senderParseUsername = customData.optString("senderParseUsername", null);

                showNotification(context, msgTitle, msgContent, channel, senderName, senderParseUsername);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    void showNotification(Context context, String msgTitle, String msgContent, String channel, String senderName, String senderParseUsername){
        //currently transition->classrooms
        //todo 1) if specified 'channel' chat page active, don't do anything here(that activity will handle it); 2) add new transition action 'chat'
        //if app is active and notification is for other channel whose activity is not visible, then their registered listeners will automatically handle it
        ParseUser currentUser = ParseUser.getCurrentUser();
        if(currentUser != null) {
            String myUsername = currentUser.getUsername();

            String[] tokens = channel.split("-");
            Log.d("__CHAT_noti_rec", "roomId split into=" + Arrays.toString(tokens));
            if(tokens.length != 2) {
                return;
            }

            String classCode = tokens[0];
            String parentParseUsername = tokens[1];
            String chatAs = ChatConfig.TEACHER;
            //chatAs replacement
            if(parentParseUsername.equals(myUsername)){
                chatAs = ChatConfig.NON_TEACHER;
            }

            String opponentName = senderName;
            String opponentParseUsername = senderParseUsername;

            Bundle params = new Bundle();
            params.putString("chatAs", chatAs);
            params.putString("classCode", classCode);
            params.putString("opponentName", opponentName);
            params.putString("opponentParseUsername", opponentParseUsername);

            Log.d("__CHAT_noti_rec", chatAs + ", " + classCode + ", " + opponentName + ", " + opponentParseUsername);

            NotificationGenerator.generateNotification(context, msgContent, msgTitle,
                    Constants.Notifications.TRANSITION_NOTIFICATION, Constants.Actions.CHAT_ACTION, params);
        }
    }
}