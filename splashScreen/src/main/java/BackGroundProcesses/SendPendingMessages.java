package BackGroundProcesses;

import android.util.Log;

import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.List;

import library.UtilString;
import trumplabs.schoolapp.Constants;
import trumplabs.schoolapp.Outbox;
import trumplabs.schoolapp.SendMessage;
import utility.Utility;

/**
 * Created by ashish on 16/6/15.
 */
public class SendPendingMessages {
    static final String count_lock = "COUNT_LOCK"; //lock while accessing/updating count variable
    static final String work_lock = "WORK_LOCK"; //lock while doing the actual sending work. Only 1 thread can do it at any point of time. Second must wait

    static int thread_count = 0; //how many threads running sendPendingMessages method,
                            //we will only allow 2 ( 1 working, 1 pending/blocked)
                            //Any more threads entering will have to just exit

    static final String LOGTAG = "DEBUG_SEND_PENDING_MSGS";
    public static void sendPendingMessagesInBackground(){
        Runnable r = new Runnable() {
            @Override
            public void run() {
                sendPendingMessages();
            }
        };
        Thread t = new Thread(r);
        t.start();
    }

    //must always be called in a thread(because it might have to wait for lock)
    public static void sendPendingMessages(){
        ParseQuery parseQuery = new ParseQuery(Constants.SENT_MESSAGES_TABLE);
        parseQuery.fromLocalDatastore();
        parseQuery.whereEqualTo("pending", true);

        try{
            List<ParseObject> messages = parseQuery.find();
            //handle only text messages for now(temporary)
            for(ParseObject msg : messages){
                if(!UtilString.isBlank(msg.getString("title")) && UtilString.isBlank(msg.getString("attachment_name"))){
                    //title non empty, attachment empty
                    Log.d(LOGTAG, "pending text msg " + msg.getString("title"));
                    SendMessage.sendTextMessageCloud(msg, false);
                }
                if(!UtilString.isBlank(msg.getString("attachment_name"))){
                    //title non empty, attachment empty
                    Log.d(LOGTAG, "pending text msg " + msg.getString("title"));
                    SendMessage.sendPicMessageCloud(msg, false);
                }
            }
        }
        catch (ParseException e){
            e.printStackTrace();
        }
    }
}
