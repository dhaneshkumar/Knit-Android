package BackGroundProcesses;

import android.util.Log;

import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import library.UtilString;
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

    //called when send button clicked in SendMessage page(GUI)
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
                    SendMessage.sendButtonClicked = false; //Since added to queue, hence a job is already running
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
            SendMessage.sendButtonClicked = false; //Since new job has now started
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

        boolean abort = false; //if network error occurred in one of the attempts

        while (true) {
            ParseObject currentMsg = null;
            synchronized (DATA_LOCK){
                Log.d(LOGTAG, "sendPendingMessages : loop-LOCK acquired");
                if(pendingMessageQueue.isEmpty()){
                    jobRunning = false;
                    isLive = false;
                    //notify SendMessage adapter so that retry button may be shown/hidden
                    notifyAllAdapters();
                    Log.d(LOGTAG, "sendPendingMessages : loop-LOCK released : queue empty, exiting : ");
                    return;
                }
                if(abort){
                    jobRunning = false;
                    isLive = false;
                    //notify SendMessage adapter so that retry button may be shown/hidden
                    notifyAllAdapters();
                    Log.d(LOGTAG, "sendPendingMessages : loop-LOCK released : abort signal(network error), exiting");
                    return;
                }
                currentMsg = pendingMessageQueue.get(0);
                Log.d(LOGTAG, "sendPendingMessages : loop-LOCK released : picking next item in the queue");
            }

            //now try sending this message
            if(currentMsg != null){
                int res = -1;
                if (!UtilString.isBlank(currentMsg.getString("title")) && UtilString.isBlank(currentMsg.getString("attachment_name"))) {
                    //title non empty, attachment empty
                    Log.d(LOGTAG, "pending text msg content : '" + currentMsg.getString("title") + "'");
                    res = SendMessage.sendTextMessageCloud(currentMsg, false);
                }

                if (!UtilString.isBlank(currentMsg.getString("attachment_name"))) {
                    //title non empty, attachment empty
                    Log.d(LOGTAG, "pending pic msg attachment name : " + currentMsg.getString("attachment_name"));
                    res = SendMessage.sendPicMessageCloud(currentMsg, false);
                }

                final int result = res;

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

                if(result == 100 && isLive){
                    showToast = true; //if network error and latest request was gui(send/retry) then show toast of "internet connection"
                }

                //view.post globally shown - so show even if in some other activity. Hence use MainActivity's view as it won't be null if the app is running
                if(showToast) {
                    if (MainActivity.viewpager != null) {
                        MainActivity.viewpager.post(new Runnable() {
                            @Override
                            public void run() {
                                if(result == 0){
                                    Utility.toastDone("Notification Sent");
                                }
                                else if(result == 100){//aborting
                                    Utility.toast("Sending failed ! Check your internet connection !");
                                }
                                else{
                                    Utility.toast("Unable to send message! We will send it later");
                                }
                            }
                        });
                    }
                }

                /*if(res == 100){
                    abort = true;
                    continue;
                }*/
            }

            //pending msg queue is not empty, remove this currentMsg from queue
            pendingMessageQueue.remove(currentMsg);
        }
    }
}
