package trumplabs.schoolapp;

import android.os.AsyncTask;
import android.view.View;

import com.parse.FunctionCallback;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParsePush;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;

import library.UtilString;
import utility.Utility;

public class ClassMsgFunctions {
    /**
     * Delete a crated classroom
     */
    public static class deleteCreatedClass extends AsyncTask<String, Void, Boolean>
    {

        @Override
        protected Boolean doInBackground(String... param) {

            String groupCode = param[0];

            HashMap<String, String> params = new HashMap<String, String>();
            params.put("classcode", groupCode);

            //calling parse cloud function to delete class
            ParseUser currentUser = ParseUser.getCurrentUser();
            List<List<String>> updatedCreatedGroups = null;
            try {
                updatedCreatedGroups = ParseCloud.callFunction("deleteClass3", params);
            } catch (ParseException e) {
                e.printStackTrace();
            }

            if(updatedCreatedGroups == null || currentUser == null){
                return false;
            }

            currentUser.put(Constants.CREATED_GROUPS, updatedCreatedGroups);
            //classroom has been deleted then
            //pin updated user object
            try {
                currentUser.pin();
            } catch (ParseException e) {
                e.printStackTrace();
            }

            //locally removing sent messages of that class
            ParseQuery<ParseObject> delquery22 = new ParseQuery<ParseObject>(Constants.SENT_MESSAGES_TABLE);
            delquery22.whereEqualTo("code", groupCode);
            delquery22.whereEqualTo("userId", currentUser.getUsername());
            delquery22.setLimit(1000);
            delquery22.fromLocalDatastore();
            try {
                ParseObject.unpinAll(delquery22.find());
            } catch (ParseException e1) {
                e1.printStackTrace();
            }

            //locally removing all members of that group
            ParseQuery<ParseObject> delquery33 = new ParseQuery<ParseObject>(Constants.GROUP_MEMBERS);
            delquery33.whereEqualTo("code", groupCode);
            delquery33.whereEqualTo("userId", currentUser.getUsername());
            delquery33.fromLocalDatastore();
            delquery33.setLimit(1000);
            try {
                ParseObject.unpinAll(delquery33.find());
            } catch (ParseException e1) {
                e1.printStackTrace();
            }

            return true;
        }


        @Override
        protected void onPostExecute(Boolean result) {

            SendMessage.progressLayout.setVisibility(View.GONE);
            SendMessage.contentLayout.setVisibility(View.VISIBLE);

            if(result  &&  SendMessage.currentActivity!= null)
            {
                Utility.toast("Successfully deleted your classroom");

                Classrooms.createdGroups = ParseUser.getCurrentUser().getList(Constants.CREATED_GROUPS);
                MainActivity.setClassListOptions();


                if(Classrooms.createdClassAdapter != null)
                    Classrooms.createdClassAdapter.notifyDataSetChanged();
                if(MainActivity.floatOptionsAdapter != null)
                    MainActivity.floatOptionsAdapter.notifyDataSetChanged();

                //finishing the current activity
                SendMessage.currentActivity.finish();

            }

            super.onPostExecute(result);
        }
    }


    //Updating total sent messages count of this class
    public static void updateTotalClassMessages(String groupCode){

        //Log.d("DEBUG_CLASS_MSG_UPDATE_TOTAL_COUNT", "updating total outbox count");

        //update SendMessage.totalClassMessages
        ParseUser user = ParseUser.getCurrentUser();

        if (user == null)
        {
            Utility.logout(); return;}

        ParseQuery<ParseObject> query = ParseQuery.getQuery(Constants.SENT_MESSAGES_TABLE);
        query.fromLocalDatastore();
        query.whereEqualTo("userId", user.getUsername());
        query.whereEqualTo("code", groupCode);
        try{
            SendMessage.totalClassMessages = query.count();
        }
        catch(ParseException e){
            e.printStackTrace();
        }
    }

}
