package BackGroundProcesses;

import android.util.Log;

import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import library.UtilString;
import trumplabs.schoolapp.ComposeMessage;
import trumplabs.schoolapp.ComposeMessageHelper;
import trumplabs.schoolapp.Constants;
import trumplabs.schoolapp.MainActivity;
import trumplabs.schoolapp.Outbox;
import trumplabs.schoolapp.SendMessage;
import utility.Utility;

/**
 * Created by ashish on 16/6/15.
 */
public class SendPendingMessages {
    static final String DATA_LOCK = "DATA_LOCK"; //lock while accessing/updating following 2 data items

    static List<ParseObject> pendingMessageQueue = null; //data item 1
    static boolean jobRunning = false; //data item 2

    static boolean isLive = false; //this flag tells whether latest request was from GUI and if yes then show the toast if no internet connection error
    //needs to be set in spawnThread and cleared when job is over

    static final int toastMessageLimit = 2;
    public static List<ParseObject> toastMessageList = new CopyOnWriteArrayList<>(); //contains time stamp of latest 2 messages sent in current session(so that we can show popup for these)

    public static final String LOGTAG = "DEBUG_SEND_PENDING_MSGS";

    //This is called from GUI as it spawns a new thread
    public static void spawnThread(boolean gui){
        isLive = gui;
        if(!Utility.isInternetExistWithoutPopup()){
            ComposeMessage.sendButtonClicked = false;
            isLive = false;
            notifyAllAdapters();
            return;
        }

        Runnable r = new Runnable() {
            @Override
            public void run() {
                sendPendingMessages();
            }
        };
        Thread t = new Thread(r);
        t.start();
    }

    public static boolean isJobRunning(){
        return jobRunning;
    }

    public static void addMessageToQueue(ParseObject msg){
        List<ParseObject> msgList = new ArrayList<>();
        msgList.add(msg);
        addMessageListToQueue(msgList);
    }

    //called when send button clicked in SendMessage page(GUI)
    //order among these msgs in msgList doesn't matter as created at same time(multicast)
    public static void addMessageListToQueue(List<ParseObject> msgList){
        Utility.isInternetExist(); //show toast if no internet connection once

        Log.d(LOGTAG, "[GUI] addMessageToQueue() entered");

        //since job is queue won't be null, but still for safety check null
        synchronized (DATA_LOCK) {
            //add to toastMessageList
            Log.d(LOGTAG, "[GUI] addMessageToQueue() added " + msgList.size() + " to toastMessageList");
            toastMessageList.addAll(msgList);

            if(toastMessageList.size() > toastMessageLimit){
                Log.d(LOGTAG, "[GUI] addMessageToQueue() removed old " + (toastMessageList.size() - toastMessageLimit) + " messages from toastMessageList");
                toastMessageList.subList(0, toastMessageList.size()-toastMessageLimit).clear(); //keep only toastMessageLimit messages
            }

            if(jobRunning) {
                if (pendingMessageQueue != null) {
                    pendingMessageQueue.addAll(msgList);
                    ComposeMessage.sendButtonClicked = false; //Since added to queue, hence a job is already running
                    Log.d(LOGTAG, "[GUI] addMessageToQueue() added to queue");
                }
            }
            else{
                Log.d(LOGTAG, "[GUI] addMessageToQueue() spawn new thread");
                spawnThread(true);
            }
        }
        Log.d(LOGTAG, "[GUI] addMessageToQueue() exit");
    }

    //notifies adapters of SendMessage and Outbox
    //called when jobRunning state changes
    public static void notifyAllAdapters(){
        SendMessage.notifyAdapter();
        Outbox.notifyAdapter();
    }

    //must always be called in a thread, it first finds dirty messages(in asc order) and sends them one by one, aborts if anyone fails due to network error
    public static void sendPendingMessages(){

        synchronized (DATA_LOCK){
            Log.d(LOGTAG, "sendPendingMessages : start-LOCK acquired : begin");
            if(jobRunning) {
                Log.d(LOGTAG, "sendPendingMessages : start-LOCK released : already one running, returning");
                return;
            }

            Log.d(LOGTAG, "sendPendingMessages : job started");

            jobRunning = true;
            ComposeMessage.sendButtonClicked = false; //Since new job has now started
            notifyAllAdapters();

            ParseQuery parseQuery = ParseQuery.getQuery(Constants.SENT_MESSAGES_TABLE);
            parseQuery.fromLocalDatastore();
            parseQuery.whereEqualTo("pending", true);
            parseQuery.orderByAscending("creationTime");

            try{
                pendingMessageQueue = parseQuery.find();
            }
            catch (ParseException e){
                e.printStackTrace();
                jobRunning = false;
                isLive = false;
                notifyAllAdapters();
                Log.d(LOGTAG, "sendPendingMessages : start-LOCK released : parse exception in find pending msgs query");
                return;
            }
            Log.d(LOGTAG, "sendPendingMessages : start-LOCK released : end");
        }

        boolean abort = false; //if session_invalid error in one of the cloud calls

        //boolean errorToastShown = false; //show only once

        while (true) {
            ParseObject currentMsg = null;
            synchronized (DATA_LOCK){
                Log.d(LOGTAG, "sendPendingMessages : loop-LOCK acquired");
                if(pendingMessageQueue.isEmpty() || abort){
                    jobRunning = false;
                    isLive = false;
                    //notify SendMessage adapter so that retry button may be shown/hidden
                    notifyAllAdapters();
                    Log.d(LOGTAG, "sendPendingMessages : loop-LOCK released : queue empty or abort=" + abort);
                    return;
                }

                currentMsg = pendingMessageQueue.get(0);
                Log.d(LOGTAG, "sendPendingMessages : loop-LOCK released : picking next item in the queue");
            }

            // now try sending this message if its not null and status is pending to avoid duplicates
            // due to race condition b/w 1) pinning (and adding to pending list) and 2) above query
            // in case of multicast messaging
            if(currentMsg != null && currentMsg.getBoolean("pending")){

                int res = -1;
                if (!UtilString.isBlank(currentMsg.getString("title")) && UtilString.isBlank(currentMsg.getString("attachment_name"))) {
                    //title non empty, attachment empty
                    Log.d(LOGTAG, "pending text msg content : '" + currentMsg.getString("title") + "'");
                    res = ComposeMessageHelper.sendTextMessageCloud(currentMsg, false);
                }

                if (!UtilString.isBlank(currentMsg.getString("attachment_name"))) {
                    //title non empty, attachment empty
                    Log.d(LOGTAG, "pending pic msg attachment name : " + currentMsg.getString("attachment_name"));
                    res = ComposeMessageHelper.sendPicMessageCloud(currentMsg, false);
                }

                final int result = res;

                if(result == ParseException.INVALID_SESSION_TOKEN){
                    abort = true;
                    continue;
                }

                //process the result
                Boolean showToast = false;

                //check if list contains this message
                for(ParseObject msg : SendPendingMessages.toastMessageList){
                    if(currentMsg == msg){ //same reference because they are the same parse objects
                        SendPendingMessages.toastMessageList.remove(currentMsg);
                        showToast = true;
                    }
                }

                Log.d(LOGTAG, "result=" + result + " isLive=" + isLive);

                //view.post globally shown - so show even if in some other activity. Hence use MainActivity's view as it won't be null if the app is running
                if(showToast && isLive) {
                    final String className = currentMsg.getString(Constants.GroupDetails.NAME);
                    if (MainActivity.viewpager != null) {
                        MainActivity.viewpager.post(new Runnable() {
                            @Override
                            public void run() {
                                if(result == 0){
                                    Utility.toast("Notification Sent");
                                }
                                else if(result == 200){ //class has been deleted
                                    Utility.toast("Class " + className + " has already been deleted");
                                }
                            }
                        });
                    }
                }
            }
            else{
                Log.d(LOGTAG, "currentMsg is either null or duplicate(not pending)");
            }

            //pending msg queue is not empty, remove this currentMsg from queue, works even if currentMsg is null(which won't happen but still)
            pendingMessageQueue.remove(currentMsg);
        }
    }
}
