package notifications;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import BackGroundProcesses.Refresher;
import trumplabs.schoolapp.Application;
import trumplabs.schoolapp.Constants;
import utility.Config;
import utility.SessionManager;

/**
 * Created by ashish on 18/1/15.
 */

public class NotificationAlarmReceiver extends WakefulBroadcastReceiver {

    public NotificationAlarmReceiver() {
    }

    // Called when the BroadcastReceiver gets an Intent it's registered to receive

    @Override
    public void onReceive(final Context context, Intent intent) {
        Log.d("DEBUG_NOT_ALARM", "onReceive() : entered");
        //check for pending messages
        ParseQuery query = new ParseQuery(Constants.PendingNotification.TABLE);
        query.fromLocalDatastore();
        query.orderByAscending(Constants.PendingNotification.TIME); //handle the order in which they arrived
        List<ParseObject> notificationList = new ArrayList<>();
        try{
            notificationList = query.find();
            Log.d("DEBUG_NOT_ALARM", "onReceive() : pending count=" + notificationList.size());
        }
        catch (ParseException e){
            Log.d("DEBUG_NOT_ALARM", "onReceive() : exception " + e.getMessage());
            e.printStackTrace();
        }

        //if no more notifications pending, then cancel alarm
        if(notificationList.isEmpty()){
            AlarmTrigger.cancelNotificationAlarm(context);
            return;
        }

        //collect stale notifications
        List<ParseObject> staleList = new ArrayList<>();
        Date now = Calendar.getInstance().getTime();

        for(ParseObject notification : notificationList){
            Date notTime = notification.getDate(Constants.PendingNotification.TIME);
            if(notTime == null || (now.getTime() - notTime.getTime() > Config.NOTIFICATION_STALE_PERIOD)){
                //add to stale list
                staleList.add(notification);
            }
            else{
                break; //got the first correct notification
            }
        }

        //remove stale from original list
        notificationList.removeAll(staleList);

        Log.d("DEBUG_NOT_ALARM", "onReceive() : stale notifications=" + staleList.size());

        //Now handle next 2 notifications remaining in notificationList
        for(int i = 0; i < 2 && i < notificationList.size(); i++){
            ParseObject notification = notificationList.get(i);
            String msg = notification.getString(Constants.PendingNotification.MSG);
            String groupName = notification.getString(Constants.PendingNotification.GROUP_NAME);
            String type = notification.getString(Constants.PendingNotification.TYPE);
            String action = notification.getString(Constants.PendingNotification.ACTION);

            if(type == null || action==null || groupName==null || msg==null){//won't happen but for safety
                staleList.add(notification);
                continue;
            }

            if(action.equals(Constants.Actions.LIKE_ACTION) || action.equals(Constants.Actions.CONFUSE_ACTION)){
                String id = notification.getString(Constants.PendingNotification.ID);
                if(id != null){
                    Bundle extras = new Bundle();
                    extras.putString("id", id);
                    Log.d("DEBUG_NOT_ALARM", "onReceive() : generating notification msg=" + msg);
                    NotificationGenerator.generateNotification(context, msg, groupName, type, action, extras);
                }
            }
            staleList.add(notification);
        }

        //unpin 'now stale' notifications
        try{
            ParseObject.unpinAll(staleList);
        }
        catch (ParseException e){
            e.printStackTrace();
        }

        //merge member count notification ?? Really ??
    }
}