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

import trumplabs.schoolapp.Constants;
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
     * @how Now for messages which have seen_status 0, seenCountIncrement cloud function is called and if
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
            {Utility.logout(); return;}

        String username = user.getUsername();

        //Now find all those messages for which status is 0(i.e Seen but not Notified)
        ParseQuery<ParseObject> seenQuery = ParseQuery.getQuery("GroupDetails");
        seenQuery.fromLocalDatastore();
        seenQuery.whereEqualTo(Constants.SEEN_STATUS, 0);
        seenQuery.whereMatches(Constants.USER_ID, username);
        seenQuery.setLimit(Config.inboxMsgRefreshTotal); //at max this many

        try{
            List<ParseObject> newSeenMessages = seenQuery.find();

            Log.d("DEBUG_SEEN_HANDLER", "syncSeenJob() newSeenMessages count " + newSeenMessages.size());
            if(newSeenMessages.size() == 0){
                return;
            }

            ArrayList<String> newSeenObjectIds = new ArrayList<>();

            for(int i=0; i < newSeenMessages.size(); i++){
                newSeenObjectIds.add(newSeenMessages.get(i).getObjectId());
            }

            HashMap<String, Object> parameters = new HashMap<>();
            parameters.put("array", newSeenObjectIds);

            boolean result = ParseCloud.callFunction("updateSeenCount", parameters);
            Log.d("DEBUG_SEEN_HANDLER", "syncSeenJob() : updateSeenCount result " + result);
            if(result){
                //change seen status of messages
                for(int i=0; i < newSeenMessages.size(); i++){
                    ParseObject msg = newSeenMessages.get(i);
                    msg.put(Constants.SEEN_STATUS, 1);
                }
                //now pin all at once using pinAll call
                ParseObject.pinAll(newSeenMessages);
            }
        }
        catch(ParseException e){
            e.printStackTrace();
        }
    }

    @Override
    protected void onPostExecute(String[] result) {
        Log.d("DEBUG_SEEN_HANDLER", "Done");
        super.onPostExecute(result);
    }
}
