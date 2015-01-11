package BackGroundProcesses;

import android.os.AsyncTask;
import android.util.Log;

import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.HashMap;
import java.util.List;

import trumplabs.schoolapp.Messages;
import utility.Config;
import utility.Utility;

/**
 * Created by ashish on 6/1/15.
 */
public class SeenHandler extends AsyncTask<Void, Void, String[]> {
    String[] mStrings;
    private List<ParseObject> msgs;

    public SeenHandler(){
        msgs = Messages.msgs;
    }

    /**
     * @action Handles the 'seen' status of messages
     * @param params none
     * @return
     * @how from the messages list of Messages activity, adds new messages to local SeenStatus table.
     *      Now for messages which have status 0, seenCountIncrement cloud function is called and if
     *      success its local status is changed to 1
     */
    @Override
    protected String[] doInBackground(Void... params) {
        syncSeenJob();
        return mStrings;
    }

    public void syncSeenJob(){
        if(msgs == null) return;
        Log.d("DEBUG_SEEN_HANDLER", "Starting");
        ParseUser user = ParseUser.getCurrentUser();

        if (user == null)
            Utility.logout();

        String username = user.getUsername();

        ParseQuery<ParseObject> query = ParseQuery.getQuery("SeenStatus");
        query.whereMatches("username", username);
        query.fromLocalDatastore();

        for(int i=0; i<msgs.size() && i<Config.inboxMsgCount; i++){
            ParseObject msg = msgs.get(i);
            if(msg != null && msg.getObjectId() != null) {
                query.whereMatches("messageId", msg.getObjectId());
                try {
                    Log.d("DEBUG_SEEN_HANDLER", "Checking for messages which are not yet in SeenStatus table. id" + msg.getObjectId());
                    List<ParseObject> match = query.find();
                    if (match == null || match.size() == 0) {
                        Log.d("DEBUG_SEEN_HANDLER", "Adding New Message to SeenStatus table");
                        ParseObject msgstatus = new ParseObject("SeenStatus");
                        msgstatus.put("username", username);
                        msgstatus.put("messageId", msg.getObjectId());
                        msgstatus.put("status", 0); //0 means that seen locally;  1 means notified to cloud
                        msgstatus.pin();
                    }
                } catch (ParseException e) {
                    Log.d("DEBUG_SEEN_HANDLER", "error code " + e.getCode());
                    //e.printStackTrace();
                }
            }
            else{
                Log.d("DEBUG_SEEN_HANDLER", "msg or msgid null");
            }
        }

        //Now find all those messages for which status is 0(i.e Seen but not Notified)
        ParseQuery<ParseObject> seenQuery = ParseQuery.getQuery("SeenStatus");
        seenQuery.fromLocalDatastore();
        seenQuery.whereEqualTo("status", 0);
        seenQuery.whereMatches("username", username);

        try{
            List<ParseObject> newSeenMessages = seenQuery.find();
            Log.d("DEBUG_SEEN_HANDLER", "newSeenMessages count " + newSeenMessages.size());
            for(int i=0; i < newSeenMessages.size(); i++){
                Log.d("DEBUG_SEEN_HANDLER", "Sending seen");
                ParseObject msg = newSeenMessages.get(i);
                HashMap<String, String> parameters = new HashMap<String, String>();
                parameters.put("objectId", msg.getString("messageId"));
                Integer res = ParseCloud.callFunction("seenCountIncrement", parameters);
                //res is number returned
                Log.d("DEBUG_SEEN_HANDLER", "Received result " + res);
                if(res >= 0){
                    msg.put("status", 1); //Notified to cloud
                    msg.pin();
                }
            }
        }
        catch(ParseException e){
        }
    }

    @Override
    protected void onPostExecute(String[] result) {
        Log.d("DEBUG_SEEN_HANDLER", "Done");
        super.onPostExecute(result);
    }
}
