package BackGroundProcesses;

import android.os.AsyncTask;
import android.util.Log;

import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import joinclasses.JoinedClasses;
import joinclasses.JoinedHelper;
import joinclasses.School;
import library.UtilString;
import trumplabs.schoolapp.Application;
import trumplabs.schoolapp.Classrooms;
import trumplabs.schoolapp.Constants;
import trumplabs.schoolapp.MainActivity;
import utility.Config;
import utility.SessionManager;
import utility.Utility;

/**
 * Updates suggestions list for user in background
 * <p/>
 * Created by Dhanesh on 12/25/2014.
 */
public class UpdateSuggestions extends AsyncTask<Void, Void, String> {

    public UpdateSuggestions()
    {
    }

    public String doInBackgroundCore(){


        Utility.ls("update suggestion running in background....");
        ParseUser user = ParseUser.getCurrentUser();

        //validating user
        if (user == null)
        {
            Utility.logout();
            return null;
        }

        String userId = user.getUsername();

        SessionManager sm = new SessionManager(Application.getAppContext());
        //check if Codegroup data has been fetched or not yet. If not just return
        if(sm.getCodegroupLocalState(user.getUsername()) == 0){
            Log.d("DEBUG_UPDATE_SUGGESTIONS", "Codegroup data not yet availabe locally. So returning");
            return userId;
        }


        List<List<String>> joinedClasses = user.getList("joined_groups");
        if(joinedClasses == null || joinedClasses.size() == 0){
            Log.d("DEBUG_UPDATE_SUGGESTIONS", "joined_group size is 0");
            return userId; //We're done. No joined groups
        }

        Log.d("DEBUG_UPDATE_SUGGESTIONS", "joined_group size is " + joinedClasses.size());

        ArrayList<String> joinedClassCodes = new ArrayList<String>();
        for(int i=0; i<joinedClasses.size(); i++){
            joinedClassCodes.add(joinedClasses.get(i).get(0));
        }

        //find Codegroup objects of joined classes
        ParseQuery joinedQuery = new ParseQuery("Codegroup");
        joinedQuery.fromLocalDatastore();
        joinedQuery.whereEqualTo("userId", user.getUsername());
        joinedQuery.whereContainedIn("code", joinedClassCodes);

        try {
            List<ParseObject> joinedGroups = joinedQuery.find();
            if(joinedGroups == null || joinedGroups.size() == 0){
                Log.d("DEBUG_UPDATE_SUGGESTIONS", "Zero code group size");
                return userId;
            }

            ArrayList<HashMap<String, String>> candidateCodegroups = filterCandidateCodegroups(joinedGroups);

            //if candidateCodegroups is empty, nothing left to do
            if(candidateCodegroups.isEmpty()){
                Log.d("DEBUG_UPDATE_SUGGESTIONS", "candidateCodegroups empty");
                return userId;
            }

            Date latestSuggestionDate = getLatestSuggestionDate(user);

            HashMap<String, Object> params = new HashMap<String, Object>();
            params.put("input", candidateCodegroups);
            params.put("date", latestSuggestionDate);

            try {
                List<ParseObject> suggestedClasses = ParseCloud.callFunction("suggestClasses", params);
                if(suggestedClasses != null){
                    for(int i=0; i<suggestedClasses.size(); i++){
                        ParseObject suggestedClass = suggestedClasses.get(i);
                        suggestedClass.put(Constants.IS_SUGGESTION, true); //set suggestion flag
                        suggestedClass.put(Constants.USER_ID, userId);
                    }

                    ParseObject.pinAll(suggestedClasses);
                    Log.d("DEBUG_UPDATE_SUGGESTIONS", "pinned all suggestedClasses at once. count " + suggestedClasses.size());
                }
            } catch (ParseException e) {
                Log.d("DEBUG_UPDATE_SUGGESTIONS", "suggestClasses() cloud function failed");
                e.printStackTrace();
            }
        }
        catch (ParseException e){
            Log.d("DEBUG_UPDATE_SUGGESTIONS", "local Codegroup query for joined classes failed");
            e.printStackTrace();
        }

        return userId;
    }

    //select those codegroup objects which have non-null "school" and "standard" is NOT "NA"
    static ArrayList<HashMap<String, String>> filterCandidateCodegroups(List<ParseObject> joinedGroups){
        ArrayList<HashMap<String, String>> candidateCodegroups = new ArrayList<HashMap<String, String>>();

        for(int i=0; i<joinedGroups.size(); i++){
            ParseObject group = joinedGroups.get(i);
            if(group.getString("school") == null || group.getString("school").equalsIgnoreCase("other") ||
                    group.getString("standard") == null || group.getString("standard").equalsIgnoreCase("NA")){
                continue; //ignore this codegroup
            }
            HashMap<String, String> candidate = new HashMap<String, String>();
            candidate.put("school", group.getString("school"));
            candidate.put("standard", group.getString("standard"));
            if(group.getString("division") == null){
                candidate.put("division", "NA");
            }
            else{
                candidate.put("division", group.getString("division"));
            }
            candidateCodegroups.add(candidate);//add this candidate
        }
        return candidateCodegroups;
    }

    //get the latest createdAt date among codegroups with (isSuggestion == true)
    static Date getLatestSuggestionDate(ParseUser user){
        Date latestSuggestionDate = Utility.getOriginDate(); //origin date
        ParseQuery dateQuery = new ParseQuery("Codegroup");
        dateQuery.fromLocalDatastore();
        dateQuery.whereEqualTo(Constants.USER_ID, user.getUsername());
        dateQuery.whereEqualTo(Constants.IS_SUGGESTION, true);
        dateQuery.orderByDescending(Constants.TIMESTAMP);
        dateQuery.setLimit(1); //we need the latest one only

        try{
            List<ParseObject> groups = dateQuery.find();
            if(groups == null || groups.size() == 0){
                //do nothing. Already default origin date set
                Log.d("DEBUG_UPDATE_SUGGESTIONS", "no local suggestions yet. Using default origin date");
            }
            else{
                ParseObject latestSuggestion = groups.get(0);
                latestSuggestionDate = latestSuggestion.getCreatedAt();
                Log.d("DEBUG_UPDATE_SUGGESTIONS", "latestSuggestionDate set to " + latestSuggestionDate + " of class " + latestSuggestion.getString("code"));
            }
        }
        catch (ParseException e){
            Log.d("DEBUG_UPDATE_SUGGESTIONS", "ParseException getting latestSuggestionDate. Using default origin date");
            e.printStackTrace();
        }
        return latestSuggestionDate;
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
            Classrooms.suggestedGroups = JoinedHelper.getSuggestionList(userId);

            if(Classrooms.suggestedClassAdapter != null)
                Classrooms.suggestedClassAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onPostExecute(String userId) {
        onPostExecuteCore(userId);
    }
}
