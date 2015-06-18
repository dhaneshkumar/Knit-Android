package BackGroundProcesses;

import android.util.Log;

import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import library.UtilString;
import trumplabs.schoolapp.Constants;
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

    static final int toastMessageLimit = 2;
    public static List<ParseObject> toastMessageList = new CopyOnWriteArrayList<>(); //contains time stamp of latest 2 messages sent in current session(so that we can show popup for these)

    public static final String LOGTAG = "DEBUG_SEND_PENDING_MSGS";
    public static void spawnThread(){
        Runnable r = new Runnable() {
            @Override
            public void run() {
                sendPendingMessages();
            }
        };
        Thread t = new Thread(r);
        t.start();
    }

    //called when
    public static void addMessageToQueue(ParseObject msg){
        Log.d(LOGTAG, "[GUI] addMessageToQueue() entered");

        //since job is queue won't be null, but still for safety check null
        synchronized (DATA_LOCK) {
            //add to toastMessageList
            if(toastMessageList.size() == toastMessageLimit){
                Log.d(LOGTAG, "[GUI] addMessageToQueue() removed old from toastMessageList");
                toastMessageList.remove(0); //remove oldest one
            }
            Log.d(LOGTAG, "[GUI] addMessageToQueue() added to toastMessageList");
            toastMessageList.add(msg);

            if(jobRunning) {
                if (pendingMessageQueue != null) {
                    pendingMessageQueue.add(msg);
                    Log.d(LOGTAG, "addMessageToQueue added to queue");
                }
            }
            else{
                Log.d(LOGTAG, "addMessageToQueue spawn new thread");
                spawnThread();
            }
        }
        Log.d(LOGTAG, "[GUI] addMessageToQueue() exit");
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
                Log.d(LOGTAG, "sendPendingMessages : start-LOCK released : parse exception in find pending msgs query");
                return;
            }
            Log.d(LOGTAG, "sendPendingMessages : start-LOCK released : end");
        }

        boolean abort = false; //if network error occurred in one of the attempts

        while (true) {
            ParseObject currentMsg = null;
            synchronized (DATA_LOCK){
                Log.d(LOGTAG, "sendPendingMessages : loop-LOCK acquired");
                if(pendingMessageQueue.isEmpty()){
                    jobRunning = false;
                    Log.d(LOGTAG, "sendPendingMessages : loop-LOCK released : queue empty, exiting : ");
                    return;
                }
                if(abort){
                    jobRunning = false;
                    Log.d(LOGTAG, "sendPendingMessages : loop-LOCK released : abort signal(network error), exiting");
                    return;
                }
                currentMsg = pendingMessageQueue.get(0);
                Log.d(LOGTAG, "sendPendingMessages : loop-LOCK released : picking next item in the queue");
            }

            //now try sending this message
            if(currentMsg != null){
                if (!UtilString.isBlank(currentMsg.getString("title")) && UtilString.isBlank(currentMsg.getString("attachment_name"))) {
                    //title non empty, attachment empty
                    Log.d(LOGTAG, "pending text msg content : '" + currentMsg.getString("title") + "'");
                    final int result = SendMessage.sendTextMessageCloud(currentMsg, false);

                    Boolean showToast = false;
                    Date date = currentMsg.getDate("creationTime");
                    if(date != null){
                        //check if list contains this date
                        for(ParseObject msg : SendPendingMessages.toastMessageList){
                            if(currentMsg == msg){ //same reference because they are the same parse objects
                                SendPendingMessages.toastMessageList.remove(currentMsg);
                                showToast = true;
                            }
                        }
                    }

                    //view.post
                    if(showToast) {
                        if (SendMessage.contentLayout != null) {
                            SendMessage.contentLayout.post(new Runnable() {
                                @Override
                                public void run() {
                                    if(result == 0){
                                        Utility.toastDone("Notification Sent");
                                    }
                                    else{
                                        Utility.toast("Unable to send message! We will send it later");
                                    }
                                }
                            });
                        }
                    }

                    if(result == 100){
                        abort = true;
                        continue;
                    }
                }
                if (!UtilString.isBlank(currentMsg.getString("attachment_name"))) {
                    //title non empty, attachment empty
                    Log.d(LOGTAG, "pending text msg " + currentMsg.getString("title"));
                    int result = SendMessage.sendPicMessageCloud(currentMsg, false);
                    if(result == 100){
                        abort = true;
                        continue;
                    }
                }
            }

            //pending msg queue is not empty, remove this currentMsg from queue
            pendingMessageQueue.remove(currentMsg);
        }
    }
}
