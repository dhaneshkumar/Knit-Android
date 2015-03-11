package BackGroundProcesses;

import android.os.AsyncTask;
import android.util.Log;

import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import trumplabs.schoolapp.Application;
import trumplabs.schoolapp.Constants;
import trumplabs.schoolapp.Outbox;
import utility.Config;
import utility.Queries;
import utility.SessionManager;
import utility.Utility;

/**
 * Created by ashish on 11/1/15.
 */
public class OutboxMsgFetch extends AsyncTask<Void, Void, String[]> {

    public OutboxMsgFetch()
    {
    }

    @Override
    protected String[] doInBackground(Void... params) {
        fetchOutboxMessages();

        String[] mStrings = null;
        return mStrings;
    }

    @Override
    protected void onPostExecute(String[] result) {
        super.onPostExecute(result);
    }

    /**
     * fetch outbox messages and pin it into "SentMessages" table
     */
    public static void fetchOutboxMessages(){
        //do this for the first few(=Config.outboxRefreshLimit
        ParseUser parseObject = ParseUser.getCurrentUser();

        if (parseObject == null)
            {Utility.logout(); return;}

        String userId = parseObject.getUsername();

        //Delete local outbox messages for current user. Query "SentMessages"
        ParseQuery deleteQuery = new ParseQuery("SentMessages");
        deleteQuery.fromLocalDatastore();
        deleteQuery.whereMatches("userId", userId);
        try{
            List<ParseObject> msgsToUnpin = deleteQuery.find();
            ParseObject.unpinAll(msgsToUnpin);
        }
        catch (ParseException e){
            e.printStackTrace();
        }

        //Now fetch outbox messages
        HashMap<String, String> parameters = new HashMap<String, String>();
        parameters.put("limit", Config.outboxMsgMaxFetchCount+"");
        parameters.put("classtype", "c"); //created type
        List<ParseObject> newMsgsToPin = new ArrayList<ParseObject>();

        try{
            List<ParseObject> outboxMessages = ParseCloud.callFunction("showLatestMessagesWithLimit", parameters);
           // Log.d("DEBUG_FETCH_OUTBOX_MESSAGES", "fetched " + outboxMessages.size() + "messages");
            for(int i=0; i<outboxMessages.size(); i++){
                ParseObject outboxMsg = outboxMessages.get(i);
                //Now create a SentMessage object and pin it
                ParseObject sentMsg = new ParseObject("SentMessages");
                sentMsg.put("objectId", outboxMsg.getObjectId());
                sentMsg.put("Creator", outboxMsg.getString("Creator"));
                sentMsg.put("code", outboxMsg.getString("code"));
                sentMsg.put("title", outboxMsg.getString("title"));
                sentMsg.put("name", outboxMsg.getString("name"));
                sentMsg.put("creationTime", outboxMsg.getCreatedAt());
                sentMsg.put("senderId", outboxMsg.getString("senderId"));
                sentMsg.put("userId", userId);
                sentMsg.put(Constants.LIKE_COUNT, outboxMsg.getInt(Constants.LIKE_COUNT)); //0 if like_count not defined
                sentMsg.put(Constants.CONFUSED_COUNT, outboxMsg.getInt(Constants.CONFUSED_COUNT));
                sentMsg.put(Constants.SEEN_COUNT, outboxMsg.getInt(Constants.SEEN_COUNT));

                if (outboxMsg.get("attachment") != null)
                    sentMsg.put("attachment", outboxMsg.get("attachment"));
                if (outboxMsg.getString("attachment_name") != null)
                    sentMsg.put("attachment_name", outboxMsg.getString("attachment_name"));

                newMsgsToPin.add(sentMsg);
            }
            ParseObject.pinAll(newMsgsToPin);
            final SessionManager sm = new SessionManager(Application.getAppContext());
            sm.setOutboxLocalState(1, userId); //set the flag locally that outbox data is valid
            //Log.d("DEBUG_FETCH_OUTBOX_MESSAGES", "Pinned all. State changed to 1. Notifying Outbox about it");

            //update total message count outbox
            Outbox.updateOutboxTotalMessages();

            //update the groupDetails of Outbox fragment so that it can use it in adapter
            Queries query = new Queries();
            Outbox.groupDetails = query.getLocalOutbox(); //get locally stored outbox messages and notify adapter

            //Notify Outbox fragment about these new messages
            Outbox.refreshSelf();
        }
        catch (ParseException e){
          //  Log.d("DEBUG_FETCH_OUTBOX_MESSAGES", "Error in parsecloud function calling maybe");
            e.printStackTrace();
        }
    }
}
