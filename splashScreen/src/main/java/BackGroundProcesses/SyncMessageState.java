package BackGroundProcesses;

import android.util.Log;

import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.HashMap;
import java.util.List;

import trumplabs.schoolapp.Constants;
import utility.Utility;

/**
 * Created by ashish on 8/1/15.
 */
public class SyncMessageState {
    public static void sync(){
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
            Log.d("DEBUG_SYNC_MESSAGE_STATE", "00 filtered messages size " + messages.size());

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
}
