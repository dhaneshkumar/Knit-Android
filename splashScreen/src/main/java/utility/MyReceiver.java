package utility;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.parse.ParseObject;
import com.parse.ParsePushBroadcastReceiver;
import com.parse.ParseUser;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;

import library.UtilString;
import notifications.AlarmTrigger;
import notifications.NotificationAlarmReceiver;
import notifications.NotificationGenerator;
import trumplabs.schoolapp.Constants;

/**
 * Customizing receiver & generating new notifications
 */
public class MyReceiver extends ParsePushBroadcastReceiver {

    @Override
    public void onPushReceive(Context context, Intent intent) {
        if(ParseUser.getCurrentUser() == null) return;

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
                String type = json.optString("type", null);
                String action = json.optString("action", null);

                if(type == null || action==null || groupName==null || contentText==null){
                    Log.d("DEBUG_MY_RECEIVER", "Ignoring Notification : some parameters null");
                    return; //we don't cater to notifications without type or action
                }

                //Now get optional keys and put them in a bundle for use as required in NotificationGenerator
                Bundle params = new Bundle();
                params.putString("id", json.optString("id", null)); //for like/confused
                params.putString("classCode", json.optString("groupCode", null)); //for user removed notification

                if(type.equals(Constants.Notifications.TRANSITION_NOTIFICATION) &&
                        (action.equals(Constants.Actions.LIKE_ACTION) || action.equals(Constants.Actions.CONFUSE_ACTION))){
                    Log.d(NotificationAlarmReceiver.LOGTAG, "received t=" + type + ", a=" + action + ", gname=" + groupName + ", msg=" + contentText);
                    //store in table
                    String msgId = json.optString("id", null); //required for like/confuse action
                    if(UtilString.isBlank(msgId)){
                        return; //ignore it
                    }
                    ParseObject notObject = new ParseObject(Constants.PendingNotification.TABLE);
                    notObject.put(Constants.PendingNotification.TYPE, type);
                    notObject.put(Constants.PendingNotification.ACTION, action);
                    notObject.put(Constants.PendingNotification.GROUP_NAME, groupName);
                    notObject.put(Constants.PendingNotification.MSG, contentText);

                    notObject.put(Constants.PendingNotification.ID, msgId);

                    notObject.put(Constants.PendingNotification.TIME, Calendar.getInstance().getTime());//local(sync not needed)
                    notObject.pinInBackground();//won't fail in general

                    //Trigger notification alarm, if not running already
                    AlarmTrigger.triggerNotificationAlarm(context);
                }
                else if(type.equals(Constants.Notifications.TRANSITION_NOTIFICATION) &&
                        action.equals(Constants.Actions.MEMBER_ACTION)){
                    Log.d(NotificationAlarmReceiver.LOGTAG, "received t=" + type + ", a=" + action + ", gname=" + groupName + ", msg=" + contentText);
                    //store in table
                    String classCode = json.optString("groupCode", null); //required for like/confuse action
                    if(UtilString.isBlank(classCode)){
                        return; //ignore it
                    }
                    ParseObject notObject = new ParseObject(Constants.PendingNotification.TABLE);
                    notObject.put(Constants.PendingNotification.TYPE, type);
                    notObject.put(Constants.PendingNotification.ACTION, action);
                    notObject.put(Constants.PendingNotification.GROUP_NAME, groupName);
                    notObject.put(Constants.PendingNotification.MSG, contentText);

                    notObject.put(Constants.PendingNotification.CLASS_CODE, classCode);

                    notObject.put(Constants.PendingNotification.TIME, Calendar.getInstance().getTime());//local(sync not needed)
                    notObject.pinInBackground();//won't fail in general

                    //Trigger notification alarm, if not running already
                    AlarmTrigger.triggerNotificationAlarm(context);
                }
                else {
                    //show notification immediately
                    NotificationGenerator.generateNotification(context, contentText, groupName, type, action, params);
                }
            }
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
