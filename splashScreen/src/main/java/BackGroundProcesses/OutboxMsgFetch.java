package BackGroundProcesses;

import android.os.AsyncTask;
import android.util.Log;

import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseUser;

import java.util.HashMap;
import java.util.List;

import trumplabs.schoolapp.Outbox;
import utility.Config;
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
            Utility.logout();

        String userId = parseObject.getUsername();

        HashMap<String, String> parameters = new HashMap<String, String>();
        parameters.put("senderId", userId);
        parameters.put("limit", Config.outboxMsgMaxFetchCount+"");

        try{
            List<ParseObject> outboxMessages = ParseCloud.callFunction("getOutboxMessages", parameters);
            Log.d("DEBUG_FETCH_OUTBOX_MESSAGES", "fetched " + outboxMessages.size() + "messages");
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

                if (outboxMsg.get("attachment") != null)
                    sentMsg.put("attachment", outboxMsg.get("attachment"));
                if (outboxMsg.getString("attachment_name") != null)
                    sentMsg.put("attachment_name", outboxMsg.getString("attachment_name"));
                if (outboxMsg.get("senderpic") != null)
                    sentMsg.put("senderpic", outboxMsg.get("senderpic"));

                try {
                    sentMsg.pin();

                } catch (ParseException e2) {
                }
            }
        }
        catch (ParseException e){
            Log.d("DEBUG_FETCH_OUTBOX_MESSAGES", "Error in parsecloud function calling maybe");
            e.printStackTrace();
        }

        //update total message count outbox
        Outbox.updateOutboxTotalMessages();
    }
}
