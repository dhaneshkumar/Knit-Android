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

/**
 * Created by ashish on 6/1/15.
 */
public class SeenHandler extends AsyncTask<Void, Void, String[]> {
    String[] mStrings;
    private List<ParseObject> msgs;

    public SeenHandler(){
        msgs = Messages.msgs;
    }

    @Override
    protected String[] doInBackground(Void... params) {
        Log.d("DEBUG_SEEN_HANDLER", "Starting");
        String username = ParseUser.getCurrentUser().getUsername();

        ParseQuery<ParseObject> query = ParseQuery.getQuery("SeenStatus");
        query.whereMatches("username", username);
        query.fromLocalDatastore();

        for(int i=0; i<msgs.size(); i++){
            ParseObject msg = msgs.get(i);
            if(msg.getObjectId() != null) {
                query.whereMatches("messageId", msg.getObjectId());
                try {
                    Log.d("DEBUG_SEEN_HANDLER", "querying with msg id" + msg.getObjectId());
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
                Log.d("DEBUG_SEEN_HANDLER", "msg id null");
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

        return mStrings;
    }

    @Override
    protected void onPostExecute(String[] result) {
        Log.d("DEBUG_SEEN_HANDLER", "Done");
        super.onPostExecute(result);
    }
}
