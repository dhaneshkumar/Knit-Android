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

    public static void sendPendingMessages(){
        ParseQuery parseQuery = new ParseQuery(Constants.SENT_MESSAGES_TABLE);
        parseQuery.fromLocalDatastore();
        parseQuery.whereEqualTo("pending", true);

        try{
            List<ParseObject> messages = parseQuery.find();
            //handle only text messages for now(temporary)
            for(ParseObject msg : messages){
                if(!UtilString.isBlank(msg.getString("title"))){
                    //non-empty text content
                    Log.d(LOGTAG, "pending msg " + msg.getString("title"));
                    SendMessage.sendTextMessageCloud(msg, false);
                }
            }
        }
        catch (ParseException e){
            e.printStackTrace();
        }
    }
}
