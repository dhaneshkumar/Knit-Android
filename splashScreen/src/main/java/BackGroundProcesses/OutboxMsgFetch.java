package BackGroundProcesses;

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
public class OutboxMsgFetch{
    /**
     * fetch outbox messages and pin it into "SentMessages" table
     */
    public static void fetchOutboxMessages(){
        //do this for the first few(=Config.outboxRefreshLimit
        ParseUser parseObject = ParseUser.getCurrentUser();

        if (parseObject == null)
            {
                Utility.LogoutUtility.logout(); return;}

        String userId = parseObject.getUsername();

        //Delete local outbox messages for current user. Query "SentMessages"
        ParseQuery deleteQuery = new ParseQuery(Constants.SENT_MESSAGES_TABLE);
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
            List<ParseObject> outboxMessages = ParseCloud.callFunction("showLatestMessagesWithLimit2", parameters);
           // Log.d("DEBUG_FETCH_OUTBOX_MESSAGES", "fetched " + outboxMessages.size() + "messages");
            for(int i=0; i<outboxMessages.size(); i++){
                ParseObject outboxMsg = outboxMessages.get(i);
                //Now create a SentMessage object and pin it
                ParseObject sentMsg = new ParseObject(Constants.SENT_MESSAGES_TABLE);
                sentMsg.put("objectId", outboxMsg.getObjectId());
                sentMsg.put("Creator", outboxMsg.getString("Creator"));
                sentMsg.put("code", outboxMsg.getString("code"));
                sentMsg.put("title", outboxMsg.getString("title"));
                sentMsg.put("name", outboxMsg.getString("name"));
                sentMsg.put("creationTime", outboxMsg.getCreatedAt());
                sentMsg.put("senderId", outboxMsg.getString("senderId"));
                sentMsg.put("userId", userId);
                sentMsg.put(Constants.GroupDetails.LIKE_COUNT, outboxMsg.getInt(Constants.GroupDetails.LIKE_COUNT)); //0 if like_count not defined
                sentMsg.put(Constants.GroupDetails.CONFUSED_COUNT, outboxMsg.getInt(Constants.GroupDetails.CONFUSED_COUNT));
                sentMsg.put(Constants.GroupDetails.SEEN_COUNT, outboxMsg.getInt(Constants.GroupDetails.SEEN_COUNT));

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
            Outbox.groupDetails = Queries.getLocalOutbox(); //get locally stored outbox messages and notify adapter

            //Notify Outbox fragment about these new messages
            Outbox.refreshSelf();
        }
        catch (ParseException e){
          //  Log.d("DEBUG_FETCH_OUTBOX_MESSAGES", "Error in parsecloud function calling maybe");
            e.printStackTrace();
        }
    }
}
