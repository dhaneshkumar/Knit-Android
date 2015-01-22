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
        seenQuery.setLimit(Config.inboxMsgCount); //at max this many

        List<ParseObject> msgPinPending = new ArrayList<ParseObject>();

        try{
            List<ParseObject> newSeenMessages = seenQuery.find();
            Log.d("DEBUG_SEEN_HANDLER", "newSeenMessages count " + newSeenMessages.size());
            for(int i=0; i < newSeenMessages.size(); i++){
                Log.d("DEBUG_SEEN_HANDLER", "Sending seen");
                ParseObject msg = newSeenMessages.get(i);
                HashMap<String, String> parameters = new HashMap<String, String>();
                parameters.put("objectId", msg.getObjectId());
                Integer res = ParseCloud.callFunction("seenCountIncrement", parameters);
                //res is number returned
                Log.d("DEBUG_SEEN_HANDLER", "Received result " + res);
                if(res >= 0){
                    msg.put(Constants.SEEN_STATUS, 1); //Notified to cloud
                    msgPinPending.add(msg);
                }
            }
            //now pin all at once using pinAll call
            ParseObject.pinAll(msgPinPending);

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
