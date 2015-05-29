package BackGroundProcesses;

import android.util.Log;

import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            {Utility.logout(); return;}
        }

        String username = user.getUsername();

        ParseQuery query = new ParseQuery("GroupDetails");
        query.fromLocalDatastore();
        query.whereMatches("userId", username);
        query.orderByDescending(Constants.TIMESTAMP); //since limited, consider only latest ones
        query.setLimit(Config.inboxMsgRefreshTotal); //consider only limited number of messages

        query.whereEqualTo(Constants.DIRTY_BIT, true);
        //consider only new messages which contains SYNCED like and confusing status
        query.whereExists(Constants.SYNCED_CONFUSING);
        query.whereExists(Constants.SYNCED_LIKE);

        try{
            List<ParseObject> messages = query.find();
            Log.d("DEBUG_SYNC_STATE", "Dirty messages count " + messages.size());

            if(messages == null || messages.size() == 0){
                Log.d("DEBUG_SYNC_STATE", "No dirty messages. We're done");
                return;
            }

            ArrayList<String> msgIds = new ArrayList<>();
            HashMap<String, ArrayList<Integer>> stateChangeMap = new HashMap<>();
            HashMap<String, ArrayList<Integer>> currentStateMap = new HashMap<>();

            for(int i=0; i<messages.size(); i++){
                ParseObject msg = messages.get(i);
                int like = msg.getBoolean(Constants.LIKE) ? 1 : 0;
                int confusing = msg.getBoolean(Constants.CONFUSING) ? 1 : 0;
                int synced_like = msg.getBoolean(Constants.SYNCED_LIKE) ? 1 : 0;
                int synced_confusing = msg.getBoolean(Constants.SYNCED_CONFUSING) ? 1 : 0;

                ArrayList<Integer> changes = new ArrayList<Integer>();
                int likeChange = like - synced_like;
                int confusingChange = confusing - synced_confusing;

                if(likeChange == 0 && confusingChange == 0){ //This should not happen as msg was marked dirty.
                                                            // No changes at all. Just move to next message
                    Log.d("DEBUG_SYNC_STATE", "false DIRTY " + msg.getObjectId());
                    msg.put(Constants.DIRTY_BIT, false);
                    continue;
                }

                msgIds.add(messages.get(i).getObjectId());

                changes.add(likeChange);
                changes.add(confusingChange);
                stateChangeMap.put(msg.getObjectId(), changes);

                //store current state so that we know what was the synced state(if L/C status changes while sync is on)
                ArrayList<Integer> current = new ArrayList<Integer>();
                current.add(like);
                current.add(confusing);
                currentStateMap.put(msg.getObjectId(), current);

                Log.d("DEBUG_SYNC_STATE", "new L/C = " + like + "/" + confusing +
                        "|| synced L/C = " + synced_like + "/" + synced_confusing +
                        "|| diff L/C = " + changes.get(0) + "/" + changes.get(1));
            }

            if(msgIds.size() == 0){
                Log.d("DEBUG_SYNC_STATE", "No need to call sync : all false dirty. We're done");
                return;
            }

            HashMap<String, Object> parameters = new HashMap<>();
            parameters.put("array", msgIds);
            parameters.put("input", stateChangeMap);

            try{
                boolean result = ParseCloud.callFunction("updateLikeAndConfusionCount", parameters);
                // if success, then for the those messages which we synced,
                // put current (saved) like and confusing status into SYNCED like and confusing status
                // Now if synced and actual state(this may change while we we syncing) are same,
                // then change change dirty bit false
                if(result){
                    for(int i=0; i<messages.size(); i++) {
                        ParseObject msg = messages.get(i);

                        //do this on if it was the message being synced
                        if(stateChangeMap.containsKey(msg.getObjectId())){
                            ArrayList<Integer> current = currentStateMap.get(msg.getObjectId());
                            if(current.size() == 2) {
                                msg.put(Constants.SYNCED_LIKE, current.get(0) == 1);
                                msg.put(Constants.SYNCED_CONFUSING, current.get(1) == 1);
                            }

                            //check if not dirty
                            if(msg.getBoolean(Constants.SYNCED_CONFUSING) == msg.getBoolean(Constants.CONFUSING) &&
                                    msg.getBoolean(Constants.SYNCED_LIKE) == msg.getBoolean(Constants.LIKE)) {
                                msg.put(Constants.DIRTY_BIT, false);
                            }
                        }
                    }
                    ParseObject.pinAll(messages);
                    Log.d("DEBUG_SYNC_STATE", "pinned all messages");
                }
            }
            catch (ParseException e){
                e.printStackTrace();
            }
        }
        catch (ParseException e){
            Log.d("DEBUG_SYNC_STATE", "ParseException");
            e.printStackTrace();
        }
    }

    /**
     * updates like and confused count of messages in inbox
     */
    public static void fetchLikeConfusedCountInbox(){
        ParseUser user = ParseUser.getCurrentUser();
        if(user == null) return;

        //fetch received messages from local GroupDetails table only(not locally generated msgs)
        ParseQuery<ParseObject> query = ParseQuery.getQuery("GroupDetails");
        query.fromLocalDatastore();
        query.orderByDescending(Constants.TIMESTAMP);
        query.whereEqualTo("userId", user.getUsername());
        query.setLimit(Config.inboxMsgRefreshTotal);


        List<ParseObject> msgs = null;
        try {
            msgs = query.find();
        }
        catch (ParseException e){
            e.printStackTrace();
            return; //we're done
        }

        if(msgs == null || msgs.size() == 0){
            Log.d("DEBUG_SYNC", "no inbox messages(in GroupDetails). We're done");
            return;
        }

        ArrayList<String> msgIds = new ArrayList<>();
        for(int i=0; i<msgs.size(); i++){
            msgIds.add(msgs.get(i).getObjectId());
            //Log.d("DEBUG_SYNC", "LOCAL MSG (before) " + Utility.parseObjectToJson(msgs.get(i)));
        }

        HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("array", msgIds);

        try{
            HashMap<String, List<Integer>> updateCountMap = ParseCloud.callFunction("updateCount2", parameters);
            if(updateCountMap != null){
                Log.d("DEBUG_SYNC", "fetchLikeConfusedCountInbox : sent : " + msgs.size() + "requests ; received " + updateCountMap.size() + " updates");

                for(int i=0; i<msgs.size(); i++){
                    ParseObject msg = msgs.get(i);
                    List<Integer> counts = updateCountMap.get(msg.getObjectId()); //[seen, like, confused]
                    if(counts != null) {
                        //formula for correct display of count(like/confused) : received count - synced status + current status
                        //seen count correct as is
                        int like = msg.getBoolean(Constants.LIKE) ? 1 : 0;
                        int confusing = msg.getBoolean(Constants.CONFUSING) ? 1 : 0;
                        int synced_like = msg.getBoolean(Constants.SYNCED_LIKE) ? 1 : 0;
                        int synced_confusing = msg.getBoolean(Constants.SYNCED_CONFUSING) ? 1 : 0;

                        msg.put(Constants.SEEN_COUNT, counts.get(0));
                        msg.put(Constants.LIKE_COUNT, counts.get(1) - synced_like + like);
                        msg.put(Constants.CONFUSED_COUNT, counts.get(2) - synced_confusing + confusing);
                        Log.d("DEBUG_SYNC", "Updated inbox msg " + Utility.parseObjectToJson(msg));
                    }
                }

                ParseObject.pinAll(msgs);
                Log.d("DEBUG_SYNC", "fetchLikeConfusedCountInbox : pinning over");
            }
        }
        catch (ParseException e){
            Log.d("DEBUG_SYNC", "fetchLikeConfusedCountInbox : parse exception while fetching updates");
            e.printStackTrace();
        }

        // messages adapter needs to be notified of data-set changed.
        // This happens in the thread running syncOtherInboxDetails() after all sub-tasks in it are over
    }

    /**
     * updates like and confused count of messages in outbox
     */
    public static void fetchLikeConfusedCountOutbox(){
        ParseUser parseObject = ParseUser.getCurrentUser();

        if (parseObject == null)
            {Utility.logout(); return;}
        if(!parseObject.getString("role").equalsIgnoreCase("teacher")){
            return;
        }

        String userId = parseObject.getUsername();

        List<List<String>> createdGroups = parseObject.getList(Constants.CREATED_GROUPS);
        if(createdGroups == null) return;

        ArrayList<String> createdClassCodes = new ArrayList<>();

        for(int i=0; i<createdGroups.size(); i++) {
            List<String> group = createdGroups.get(i);
            createdClassCodes.add(group.get(0));
        }
        ParseQuery outboxQuery = ParseQuery.getQuery("SentMessages");
        outboxQuery.fromLocalDatastore();
        outboxQuery.orderByDescending("creationTime");
        outboxQuery.whereEqualTo("userId", userId);
        outboxQuery.whereContainedIn("code", createdClassCodes); //0 is code
        outboxQuery.setLimit(Config.outboxMsgRefreshTotal);

        List<ParseObject> recentSentMessages = null;
        try {
            recentSentMessages = outboxQuery.find();
        }
        catch (ParseException e) {
            e.printStackTrace();
        }

        if(recentSentMessages == null || recentSentMessages.size() == 0){
            Log.d("DEBUG_SYNC", "fetchLikeConfusedCountOutbox : no outbox messages. We're done");
            return;
        }

        ArrayList<String> msgIds = new ArrayList<>();
        for(int i=0; i<recentSentMessages.size(); i++){
            String id = recentSentMessages.get(i).getString("objectId");
            if(id != null)
                msgIds.add(id);
        }

        if(msgIds.size() == 0){
            Log.d("DEBUG_SYNC", "fetchLikeConfusedCountOutbox : list message ids empty. We're done");
            return;
        }

        HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("array", msgIds);

        try{
            HashMap<String, List<Integer>> updateCountMap = ParseCloud.callFunction("updateCount2", parameters);
            if(updateCountMap != null){
                Log.d("DEBUG_SYNC", "fetchLikeConfusedCountOutbox : sent : " + recentSentMessages.size() + "requests ; received " + updateCountMap.size() + " updates");

                for(int i=0; i<recentSentMessages.size(); i++){
                    ParseObject msg = recentSentMessages.get(i);
                    List<Integer> counts = updateCountMap.get(msg.getString("objectId")); //[seen, like, confused]
                    if(counts != null) {
                        msg.put(Constants.LIKE_COUNT, counts.get(1));
                        msg.put(Constants.CONFUSED_COUNT, counts.get(2));
                        msg.put(Constants.SEEN_COUNT, counts.get(0));
                        //Log.d("DEBUG_SYNC", "Updated outbox msg " + Utility.parseObjectToJson(msg));
                    }
                }

                ParseObject.pinAll(recentSentMessages);
            }
        }
        catch (ParseException e){
            Log.d("DEBUG_SYNC", "fetchLikeConfusedCountOutbox : parse exception while fetching updates");
            e.printStackTrace();
        }
    }
}
