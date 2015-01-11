package BackGroundProcesses;

import android.util.Log;

import com.parse.Parse;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import trumplabs.schoolapp.ClassMsg;
import trumplabs.schoolapp.Constants;
import trumplabs.schoolapp.Messages;
import utility.Config;
import utility.Utility;

/**
 * Created by ashish on 8/1/15.
 */
public class SyncMessageDetails {
    /**
     * conveys the confused and like status of dirty marked messages to cloud
     * using cloud function updateMessageState
     */
    public static void syncStatus(){
        ParseUser user = ParseUser.getCurrentUser();
        if (user == null) {
            Utility.logout();
            return;
        }

        String username = user.getUsername();

        ParseQuery query = new ParseQuery("GroupDetails");
        query.fromLocalDatastore();
        query.whereMatches("userId", username);
        query.whereEqualTo(Constants.DIRTY_BIT, true);

        try{
            List<ParseObject> messages = query.find();
            Log.d("DEBUG_SYNC_MESSAGE_STATE", "filtered messages size " + messages.size());

            for(int i=0; i<messages.size(); i++){
                ParseObject msg = messages.get(i);
                HashMap<String, String> parameters = new HashMap<String, String>();
                if(msg == null || msg.getObjectId() == null) continue;

                parameters.put("objectId", msg.getObjectId());
                parameters.put("username", username);
                parameters.put("likeStatus", Boolean.toString(msg.getBoolean(Constants.LIKE)));
                parameters.put("confusedStatus", Boolean.toString(msg.getBoolean(Constants.CONFUSING)));
                Integer res = ParseCloud.callFunction("updateMessageState", parameters);

                if(res == 1){
                    Log.d("DEBUG_SYNC_MESSAGE_STATE", "synced message state");
                    msg.put(Constants.DIRTY_BIT, false);
                    msg.pinInBackground();
                }
                else{
                    Log.d("DEBUG_SYNC_MESSAGE_STATE", "something wrong happened");
                }
            }
        }
        catch (ParseException e){
            Log.d("DEBUG_SYNC_MESSAGE_STATE", "ParseException");
            e.printStackTrace();
        }
    }

    /**
     * updates like and confused count of messages in inbox
     */
    public static void fetchLikeConfusedCountInbox(){
        //do this for the first few(=Config.inboxMsgCount) messages in Message Activity's msgs
        List<ParseObject> msgs = Messages.msgs;
        if(msgs == null) return;
        ParseQuery countQuery = new ParseQuery("GroupDetails");

        //We want only confused and like count
        countQuery.selectKeys(Arrays.asList(Constants.LIKE_COUNT, Constants.CONFUSED_COUNT));

        for(int i=0; i< msgs.size() && i< Config.inboxMsgCount; i++){
            ParseObject msg = msgs.get(i);

            HashMap<String, String> parameters = new HashMap<String, String>();
            if(msg == null || msg.getObjectId() == null) continue;

            parameters.put("objectId", msg.getObjectId());
            try{
                Map<String, Object> updatedmsg = ParseCloud.callFunction("getLikeConfusedCount", parameters);
                Log.d("DEBUG_FETCH_COUNT", msg.getObjectId() + "old L/C=" +
                        msg.get(Constants.LIKE_COUNT) + "/" + msg.get(Constants.CONFUSED_COUNT) +
                        "new L/C=" +
                        updatedmsg.get(Constants.LIKE_COUNT) +  "/" + updatedmsg.get(Constants.CONFUSED_COUNT));
                msg.put(Constants.LIKE_COUNT, updatedmsg.get(Constants.LIKE_COUNT));
                msg.put(Constants.CONFUSED_COUNT, updatedmsg.get(Constants.CONFUSED_COUNT));
                msg.pinInBackground();
            }
            catch (ParseException e){
                e.printStackTrace();
            }
        }
        //messages adapter needs to be notified of dataset changed.
        //Currently this happens in Inbox asynctask's onpostexecute
    }

    /**
     * updates like and confused count of messages in outbox
     */
    public static void fetchLikeConfusedCountOutbox(){
        //do this for the first few(=Config.outboxRefreshLimit
        ParseUser parseObject = ParseUser.getCurrentUser();

        if (parseObject == null)
            Utility.logout();
        if(!parseObject.getString("role").equalsIgnoreCase("teacher")){
            return;
        }

        String userId = parseObject.getUsername();

        List<List<String>> createdGroups = parseObject.getList(Constants.CREATED_GROUPS);

        for(int i=0; i<createdGroups.size(); i++){
            List<String> group = createdGroups.get(i);

            ParseQuery outboxQuery = ParseQuery.getQuery("SentMessages");
            outboxQuery.fromLocalDatastore();
            outboxQuery.orderByDescending("creationTime");
            outboxQuery.whereEqualTo("userId", userId);
            outboxQuery.whereEqualTo("code", group.get(0)); //0 is code
            outboxQuery.setLimit(Config.outboxMsgRefreshPerClass);

            List<ParseObject> latestmessages;
            try {
                latestmessages = outboxQuery.find();

                if (latestmessages != null) {
                    Log.d("DEBUG_OUTBOX_COUNT_FETCH", "[" + group.get(0) + "]" + " count " + latestmessages.size());
                    for (int msgno = 0; msgno < latestmessages.size(); msgno++) {
                        ParseObject msg = latestmessages.get(msgno);

                        HashMap<String, String> parameters = new HashMap<String, String>();
                        if(msg == null || msg.getString("objectId") == null) {
                            if(msg != null) {
                                Log.d("DEBUG_OUTBOX_COUNT_FETCH", "msg or object id is NULL" + msg.get("title")
                                        + msg.get("name") + " " + msg.get("code"));
                            }
                            continue;
                        }

                        parameters.put("objectId", msg.getString("objectId"));
                        parameters.put("outbox", "1");
                        try{
                            Map<String, Object> updatedmsg = ParseCloud.callFunction("getLikeConfusedCount", parameters); //also gets seen count

                            Log.d("DEBUG_OUTBOX_COUNT_FETCH", "[" + group.get(0) + "]" + msg.getObjectId() + "old L/C/S=" +
                                    msg.get(Constants.LIKE_COUNT) + "/" + msg.get(Constants.CONFUSED_COUNT) + "/" + msg.get(Constants.SEEN_COUNT)+
                                    " new L/C/S=" +
                                    updatedmsg.get(Constants.LIKE_COUNT) +  "/" + updatedmsg.get(Constants.CONFUSED_COUNT)+"/"+updatedmsg.get(Constants.SEEN_COUNT)) ;

                            msg.put(Constants.LIKE_COUNT, updatedmsg.get(Constants.LIKE_COUNT));
                            msg.put(Constants.CONFUSED_COUNT, updatedmsg.get(Constants.CONFUSED_COUNT));
                            msg.put(Constants.SEEN_COUNT, updatedmsg.get(Constants.SEEN_COUNT));
                            msg.pinInBackground();
                        }
                        catch (ParseException e){
                            e.printStackTrace();
                        }
                    }
                }
            } catch (ParseException e1) {
                e1.printStackTrace();
            }
        }
    }
}
