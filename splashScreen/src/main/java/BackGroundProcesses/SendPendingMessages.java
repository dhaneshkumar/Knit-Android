package BackGroundProcesses;

import android.util.Log;

import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import library.UtilString;
import trumplabs.schoolapp.Application;
import trumplabs.schoolapp.ComposeMessage;
import trumplabs.schoolapp.ComposeMessageHelper;
import trumplabs.schoolapp.Constants;
import trumplabs.schoolapp.MainActivity;
import trumplabs.schoolapp.Outbox;
import trumplabs.schoolapp.SendMessage;
import utility.Config;
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

        synchronized (DATA_LOCK) {
            if (jobRunning) {
                return;
            }
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

        if(Config.SHOWLOG) Log.d(LOGTAG, "[GUI] addMessageToQueue() entered");

        //since job is queue won't be null, but still for safety check null
        synchronized (DATA_LOCK) {
            //add to toastMessageList
            if(Config.SHOWLOG) Log.d(LOGTAG, "[GUI] addMessageToQueue() added " + msgList.size() + " to toastMessageList");
            toastMessageList.addAll(msgList);

            if(toastMessageList.size() > toastMessageLimit){
                if(Config.SHOWLOG) Log.d(LOGTAG, "[GUI] addMessageToQueue() removed old " + (toastMessageList.size() - toastMessageLimit) + " messages from toastMessageList");
                toastMessageList.subList(0, toastMessageList.size()-toastMessageLimit).clear(); //keep only toastMessageLimit messages
            }

            if(jobRunning) {
                if (pendingMessageQueue != null) {
                    pendingMessageQueue.addAll(msgList);
                    ComposeMessage.sendButtonClicked = false; //Since added to queue, hence a job is already running
                    if(Config.SHOWLOG) Log.d(LOGTAG, "[GUI] addMessageToQueue() added to queue");
                }
            }
            else{
                if(Config.SHOWLOG) Log.d(LOGTAG, "[GUI] addMessageToQueue() spawn new thread");
                spawnThread(true);
            }
        }
        if(Config.SHOWLOG) Log.d(LOGTAG, "[GUI] addMessageToQueue() exit");
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
            if(Config.SHOWLOG) Log.d(LOGTAG, "sendPendingMessages : start-LOCK acquired : begin");
            if(jobRunning) {
                if(Config.SHOWLOG) Log.d(LOGTAG, "sendPendingMessages : start-LOCK released : already one running, returning");
                return;
            }

            if(Config.SHOWLOG) Log.d(LOGTAG, "sendPendingMessages : job started");

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
                if(Config.SHOWLOG) Log.d(LOGTAG, "sendPendingMessages : start-LOCK released : parse exception in find pending msgs query");
                return;
            }
            if(Config.SHOWLOG) Log.d(LOGTAG, "sendPendingMessages : start-LOCK released : end");
        }

        boolean abort = false; //if session_invalid error in one of the cloud calls

        //boolean errorToastShown = false; //show only once

        while (true) {
            List<ParseObject> nextBatch = null;
            synchronized (DATA_LOCK){
                if(Config.SHOWLOG) Log.d(LOGTAG, "sendPendingMessages : loop-LOCK acquired");
                if(pendingMessageQueue.isEmpty() || abort){
                    jobRunning = false;
                    isLive = false;
                    //notify SendMessage adapter so that retry button may be shown/hidden
                    notifyAllAdapters();
                    if(Config.SHOWLOG) Log.d(LOGTAG, "sendPendingMessages : loop-LOCK released : queue empty or abort=" + abort);
                    return;
                }

                nextBatch = getNextBatch(pendingMessageQueue);

                if(Config.SHOWLOG) Log.d(LOGTAG, "sendPendingMessages : loop-LOCK released : picking next item in the queue");
            }

            // now try sending this message if its not null and status is pending to avoid duplicates
            // due to race condition b/w 1) pinning (and adding to pending list) and 2) above query
            // in case of multicast messaging
            if(nextBatch != null && nextBatch.size() > 0 && nextBatch.get(0) != null && nextBatch.get(0).getBoolean("pending")){

                ParseObject master = nextBatch.get(0);

                int res = -1;
                if (!UtilString.isBlank(master.getString("title")) && UtilString.isBlank(master.getString("attachment_name"))) {
                    //title non empty, attachment empty
                    if(Config.SHOWLOG) Log.d(LOGTAG, "pending text msg content : '" + master.getString("title") + "'" + ", multicast=" + nextBatch.size());
                    res = ComposeMessageHelper.sendMultiTextMessageCloud(nextBatch);
                }

                if (!UtilString.isBlank(master.getString("attachment_name"))) {
                    //title non empty, attachment empty
                    if(Config.SHOWLOG) Log.d(LOGTAG, "pending pic msg attachment name : " + master.getString("attachment_name") + ", multicast=" + nextBatch.size());
                    res = ComposeMessageHelper.sendMultiPicMessageCloud(nextBatch);
                }

                final int result = res;

                //process the result
                Boolean showToast = false;

                //check if list contains this message
                for(ParseObject msg : SendPendingMessages.toastMessageList){
                    for(ParseObject t : nextBatch) {
                        if (t == msg) { //same reference because they are the same parse objects
                            SendPendingMessages.toastMessageList.remove(t);
                            showToast = true;
                        }
                    }
                }

                if(result == ParseException.INVALID_SESSION_TOKEN){
                    abort = true;
                    continue;
                }

                if(Config.SHOWLOG) Log.d(LOGTAG, "result=" + result + " isLive=" + isLive);

                //view.post globally shown - so show even if in some other activity. Hence use MainActivity's view as it won't be null if the app is running
                if(showToast && isLive) {
                    final String className = master.getString(Constants.GroupDetails.NAME);
                    if (Application.applicationHandler != null) {
                        Application.applicationHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (result == 0) {
                                    Utility.toast("Notification Sent");
                                }
                                else if (result == 200) { //class has been deleted
                                    Utility.toast("Class " + className + " has already been deleted");
                                }
                                else if (result == 201) { //no subscribers in class
                                    Utility.toast("You can't send message to class " + className + " as it has no members");
                                }
                            }
                        });
                    }
                }
            }
            else{
                if(Config.SHOWLOG) Log.d(LOGTAG, "currentMsg is either null or duplicate(not pending)");
            }

            //pending msg queue is not empty, remove this currentMsg from queue, works even if currentMsg is null(which won't happen but still)
            pendingMessageQueue.removeAll(nextBatch);
        }
    }

    static List<ParseObject> getNextBatch(List<ParseObject> pendingMessageQueue){
        if(pendingMessageQueue == null || pendingMessageQueue.size() == 0){
            return null;
        }

        List<ParseObject> batch = new ArrayList<>();
        ParseObject master = pendingMessageQueue.get(0);
        batch.add(master); //first object added.

        if(master == null || !master.containsKey(Constants.BATCH_ID)){
            return batch;
        }

        //Now add all those which have same batch_id as this
        long masterBatchId = master.getLong(Constants.BATCH_ID);
        for(int i=1; i<pendingMessageQueue.size(); i++){
            ParseObject candidate = pendingMessageQueue.get(i);

            if(candidate.containsKey(Constants.BATCH_ID) && candidate.getLong(Constants.BATCH_ID) == masterBatchId){
                batch.add(candidate);
            }
            else{
                break;
            }
        }

        return batch;
    }
}
