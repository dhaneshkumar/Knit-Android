package utility;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.parse.ParsePushBroadcastReceiver;

import org.json.JSONException;
import org.json.JSONObject;

import library.UtilString;
import notifications.NotificationGenerator;
import trumplabs.schoolapp.Constants;

/**
 * Customizing receiver & generating new notifications
 */
public class MyReceiver extends ParsePushBroadcastReceiver {

    @Override
    public void onPushReceive(Context context, Intent intent) {

        //retrieving json data on push receive
        Bundle extras = intent.getExtras();
        String jsonData = extras.getString("com.parse.Data");
        String contentText = null;
        String groupName = null;

        try {
            Log.d("DEBUG_MY_RECEIVER", "some notification received");

            if(jsonData != null) {
                JSONObject json = new JSONObject(jsonData);

                //content : keys "msg" or "alert"
                contentText = json.optString("msg", null);
                if(UtilString.isBlank(contentText)) {
                    contentText = json.optString("alert", null);
                }

                //notification heading - keys "groupName" or "title"
                groupName = json.optString("groupName", null);
                if(UtilString.isBlank(groupName))
                {
                    groupName = json.optString("title", null);
                }

                //keys "type" and "action"
                String type = json.optString("type");
                String action = json.optString("action");

                //Now get optional keys and put them in a bundle for use as required in NotificationGenerator
                Bundle params = new Bundle();
                params.putString("id", json.optString("id", null));
                params.putString("classCode", json.optString("groupCode", null));

                NotificationGenerator.generateNotification(context, contentText, groupName, type, action, params);
            }
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
