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

import joinclasses.JoinedHelper;
import trumplabs.schoolapp.Classrooms;
import trumplabs.schoolapp.Constants;
import trumplabs.schoolapp.MainActivity;
import utility.Utility;

/**
 * Created by ashish on 25/2/15.
 */
public class FetchSuggestionsOnJoin extends AsyncTask<Void, Void, String> {
    String classCode; //class whose related suggestions has to be fetched
    ParseObject joinedClass;
    public FetchSuggestionsOnJoin(String code){
        classCode = code;
    }

    public String doInBackgroundCore(){
        ParseUser user = ParseUser.getCurrentUser();
        Log.d("DEBUG_FETCH_SUGGESTIONS", "fetch suggestions for joined class " + classCode);

        //validating user
        if (user == null)
        {
            Utility.logout();
            return null;
        }

        String userId = user.getUsername();

        ParseQuery joinedQuery = new ParseQuery("Codegroup");
        joinedQuery.fromLocalDatastore();
        joinedQuery.whereEqualTo("userId", userId);
        joinedQuery.whereEqualTo("code", classCode);

        try {
            joinedClass = joinedQuery.getFirst();
        }
        catch (ParseException e){
            Log.d("DEBUG_FETCH_SUGGESTIONS", "local Codegroup query for " + classCode + " failed");
            e.printStackTrace();
            return userId;
        }

        //check if this class is a candidate for fetching suggestions i.e has school & standard set properly
        if(!UpdateSuggestions.checkIfCandidateCodegroup(joinedClass)){
            Log.d("DEBUG_FETCH_SUGGESTIONS", "Not a candidate for suggestion " + classCode);
            return userId;
        }

        //call cloud function
        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("school", joinedClass.getString("school"));
        params.put("standard", joinedClass.getString("standard"));
        if(joinedClass.getString("divison") == null){
            params.put("division", "NA");
        }
        else{
            params.put("division", joinedClass.getString("divison"));
        }

        try {
            List<ParseObject> suggestedClasses = ParseCloud.callFunction("suggestClass", params);
            if(suggestedClasses != null){
                for(int i=0; i<suggestedClasses.size(); i++){
                    ParseObject suggestedClass = suggestedClasses.get(i);
                    suggestedClass.put(Constants.IS_SUGGESTION, true); //set suggestion flag
                    suggestedClass.put(Constants.USER_ID, userId);
                }

                ParseObject.pinAll(suggestedClasses);
                Log.d("DEBUG_FETCH_SUGGESTIONS", "pinned all suggestedClasses at once. count " + suggestedClasses.size());
            }
        } catch (ParseException e) {
            Log.d("DEBUG_FETCH_SUGGESTIONS", "suggestClass() cloud function failed");
            e.printStackTrace();
        }

        return userId;
    }

    @Override
    protected String doInBackground(Void... params) {
        String userId = doInBackgroundCore();
        return userId;
    }

    public void onPostExecuteHelper(final String userId){
        if(MainActivity.mHeaderProgressBar != null){
            MainActivity.mHeaderProgressBar.post(new Runnable() {
                @Override
                public void run() {
                    onPostExecuteCore(userId);
                }
            });
        }
    }

    public void onPostExecuteCore(String userId){
        if (userId != null) {
           /* Classrooms.suggestedGroups = JoinedHelper.getSuggestionList(userId);

            if(Classrooms.suggestedClassAdapter != null)
                Classrooms.suggestedClassAdapter.notifyDataSetChanged();*/
        }
    }

    @Override
    protected void onPostExecute(String userId) {
        onPostExecuteCore(userId);
    }
}
