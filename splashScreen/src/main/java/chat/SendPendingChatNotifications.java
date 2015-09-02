package chat;

import android.util.Log;

import com.onesignal.OneSignal;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import library.UtilString;
import trumplabs.schoolapp.Application;
import trumplabs.schoolapp.Constants;
import utility.Config;
import utility.Utility;

/**
 * Created by ashish on 16/6/15.
 */
public class SendPendingChatNotifications {
    static final String DATA_LOCK = "DATA_LOCK"; //lock while accessing/updating following 2 data items

    static List<ParseObject> pendingNotificationQueue = null; //data item 1
    static boolean jobRunning = false; //data item 2

    public static final String LOGTAG = "__CHAT_not";

    //This is called from GUI as it spawns a new thread
    public static void spawnThread(){
        if(!Utility.isInternetExistWithoutPopup()){
            return;
        }

        synchronized (DATA_LOCK) {
            if (jobRunning) {
                return;
            }
        }

        Runnable r = new Runnable() {
            @Override
            public void run() {
                sendPendingChatNotifications();
            }
        };
        Thread t = new Thread(r);
        t.start();
    }

    public static boolean isJobRunning(){
        return jobRunning;
    }

    public static void addNotificationToQueue(ParseObject msg){
        List<ParseObject> msgList = new ArrayList<>();
        msgList.add(msg);
        addMessageListToQueue(msgList);
    }

    //called when send button clicked in SendMessage page(GUI)
    //order among these msgs in msgList doesn't matter as created at same time(multicast)
    public static void addMessageListToQueue(List<ParseObject> msgList){
        if(Config.SHOWLOG) Log.d(LOGTAG, "[GUI] addNotificationToQueue() entered");

        synchronized (DATA_LOCK) {
            //add to toastMessageList
            if(Config.SHOWLOG) Log.d(LOGTAG, "[GUI] addNotificationToQueue() added " + msgList.size() + " to toastMessageList");

            if(jobRunning) {
                if (pendingNotificationQueue != null) {
                    pendingNotificationQueue.addAll(msgList);
                    if(Config.SHOWLOG) Log.d(LOGTAG, "[GUI] addNotificationToQueue() added to queue");
                }
            }
            else{
                if(Config.SHOWLOG) Log.d(LOGTAG, "[GUI] addNotificationToQueue() spawn new thread");
                spawnThread();
            }
        }
        if(Config.SHOWLOG) Log.d(LOGTAG, "[GUI] addNotificationToQueue() exit");
    }

    //must always be called in a thread, it first finds dirty messages(in asc order) and sends them one by one, aborts if anyone fails due to network error
    public static void sendPendingChatNotifications(){

        synchronized (DATA_LOCK){
            if(Config.SHOWLOG) Log.d(LOGTAG, "sendPendingChatNotifications : start-LOCK acquired : begin");
            if(jobRunning) {
                if(Config.SHOWLOG) Log.d(LOGTAG, "sendPendingChatNotifications : start-LOCK released : already one running, returning");
                return;
            }

            if(Config.SHOWLOG) Log.d(LOGTAG, "sendPendingChatNotifications : job started");

            jobRunning = true;

            ParseQuery parseQuery = ParseQuery.getQuery(ChatConfig.ChatNotificationTable.TABLE);
            parseQuery.fromLocalDatastore();
            parseQuery.whereEqualTo("pending", true);
            parseQuery.orderByAscending("time");

            try{
                pendingNotificationQueue = parseQuery.find();
                if(Config.SHOWLOG) Log.d(LOGTAG, "pending count=" + pendingNotificationQueue.size());
            }
            catch (ParseException e){
                e.printStackTrace();
                jobRunning = false;
                if(Config.SHOWLOG) Log.d(LOGTAG, "sendPendingChatNotifications : start-LOCK released : parse exception in find pending msgs query");
                return;
            }
            if(Config.SHOWLOG) Log.d(LOGTAG, "sendPendingChatNotifications : start-LOCK released : end");
        }

        while (true) {
            ParseObject nextNotification = null;
            synchronized (DATA_LOCK){
                if(Config.SHOWLOG) Log.d(LOGTAG, "sendPendingChatNotifications : loop-LOCK acquired");
                if(pendingNotificationQueue.isEmpty()){
                    jobRunning = false;
                    if(Config.SHOWLOG) Log.d(LOGTAG, "sendPendingChatNotifications : loop-LOCK released : queue empty");
                    return;
                }

                nextNotification = pendingNotificationQueue.get(0);

                if(Config.SHOWLOG) Log.d(LOGTAG, "sendPendingChatNotifications : loop-LOCK released : picking next item in the queue");
            }

            // now try sending this message if its not null and status is pending to avoid duplicates
            // due to race condition b/w 1) pinning (and adding to pending list) and 2) above query
            // in case of multicast messaging
            if(nextNotification != null && nextNotification.getBoolean(ChatConfig.ChatNotificationTable.PENDING)){
                nextNotification.put(ChatConfig.ChatNotificationTable.PENDING, false); //temporary to avoid duplicates
                sendOneSignalNotification(nextNotification);
            }
            else{
                if(Config.SHOWLOG) Log.d(LOGTAG, "currentMsg is either null or duplicate(not pending)");
            }

            //pending msg queue is not empty, remove this currentMsg from queue, works even if currentMsg is null(which won't happen but still)
            pendingNotificationQueue.remove(nextNotification);
        }
    }

    /*
        Need input, channelId, mUsername, opponentOneSignalId
     */
    static void sendOneSignalNotification(final ParseObject notification){

        try {
            JSONObject contents = new JSONObject();
            contents.put("en", "");

            JSONObject headings = new JSONObject();
            headings.put("en", "");

            JSONObject data = new JSONObject();
            String channel = notification.getString(ChatConfig.ChatNotificationTable.CHANNEL);
            String msgTitle = notification.getString(ChatConfig.ChatNotificationTable.MSG_TITLE);
            String msgContent = notification.getString(ChatConfig.ChatNotificationTable.MSG_CONTENT);
            String opponentOneSignalId = notification.getString(ChatConfig.ChatNotificationTable.OPP_ONE_SIGNAL_ID);

            if(UtilString.isBlank(channel) || UtilString.isBlank(msgTitle) || UtilString.isBlank(msgContent) || UtilString.isBlank(opponentOneSignalId)){
                //invalid notification, just unpin and forget
                notification.unpinInBackground();
                return;
            }

            data.put("channel", channel);
            data.put("msgTitle", msgTitle);
            data.put("msgContent", msgContent);

            JSONArray playerIds = new JSONArray();
            playerIds.put(opponentOneSignalId);

            Map<String, Object> jsonMap = new HashMap<>();
            jsonMap.put("contents", contents); //actual content
            jsonMap.put("include_player_ids",playerIds); //receipients
            jsonMap.put("headings", headings); //title
            jsonMap.put("data", data); //extra json
            jsonMap.put("android_background_data", true); //so that com.onesignal.BackgroundBroadcast.RECEIVE broadcast is called

            JSONObject notificationJSON = new JSONObject(jsonMap);

            Log.d(LOGTAG, notificationJSON + "");

            OneSignal.postNotification(notificationJSON, new OneSignal.PostNotificationResponseHandler() {
                @Override
                public void onSuccess(JSONObject jsonObject) {
                    //success, hence just unpin and forget
                    notification.unpinInBackground();
                    Application.applicationHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Utility.toast("notification sent");
                        }
                    });
                }

                @Override
                public void onFailure(final JSONObject jsonObject) {
                    notification.put(ChatConfig.ChatNotificationTable.PENDING, true);
                    notification.pinInBackground(); //pending from temporary(false) to true
                    if(Config.SHOWLOG) Log.d(LOGTAG, "postNotification onFailure() : " + jsonObject);
                    Application.applicationHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Utility.toast("notification failed " + jsonObject);
                        }
                    });
                }
            });
        }
        catch (JSONException e){
            e.printStackTrace();
        }
    }
}
