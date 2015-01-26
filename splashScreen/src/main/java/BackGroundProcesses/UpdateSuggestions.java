package BackGroundProcesses;

import android.graphics.Paint;
import android.os.AsyncTask;
import android.view.View;

import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import joinclasses.JoinedClasses;
import joinclasses.JoinedHelper;
import joinclasses.School;
import library.UtilString;
import profileDetails.ProfilePage;
import trumplabs.schoolapp.Constants;
import utility.Config;
import utility.Utility;

/**
 * Updates suggestions list for user in background
 * <p/>
 * Created by Dhanesh on 12/25/2014.
 */
public class UpdateSuggestions extends AsyncTask<Void, Void, String> {

    public String doInBackgroundCore(){

        Utility.ls("update suggestion running....");
        ParseUser user = ParseUser.getCurrentUser();

        if (user == null)
            return null;
        String userId = user.getUsername();

        List<List<String>> suggestions = user.getList(Constants.JOINED_GROUPS);
        String role = user.getString(Constants.ROLE);

        String defaultCode = null;
        if (role.equals(Constants.TEACHER))
            defaultCode = Config.defaultTeacherGroupCode;
        else
            defaultCode = Config.defaultParentGroupCode;

        if (suggestions != null) {
            Set<String> stringList = new HashSet<String>();

            //removing default group from list
            for (int i = 0; i < suggestions.size(); i++) {
                if (suggestions.get(i).get(0).equals(defaultCode))
                    continue;
                else {
                    ParseQuery<ParseObject> query = ParseQuery.getQuery(Constants.CODE_GROUP);
                    query.fromLocalDatastore();
                    query.whereEqualTo("code", suggestions.get(i).get(0));

                    Utility.ls(suggestions.get(i).get(0));
                    try {
                        ParseObject obj = query.getFirst();

                        if (obj != null) {
                            String temp = "";

                            String school = obj.getString("school");
                            String standard = obj.getString("standard");
                            String division = obj.getString(Constants.DIVISION);

                            if (!UtilString.isBlank(school) && !UtilString.isBlank(standard) && !UtilString.isBlank(division)) {
                                temp = school + "~" + standard + "~" + division;
                                stringList.add(temp);

                                //  Utility.ls(temp);
                            }
                        }

                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
            }


            if (stringList != null && stringList.size() > 0) {
                for (String item : stringList) {
                    String[] itemList = item.split("~");

                    if (itemList == null)
                        continue;

                    if (itemList.length == 3) {
                        String school = itemList[0];
                        String standard = itemList[1];
                        String division = itemList[2];

                        //    Utility.ls("refresh : " + school + " " + standard + " " + division);

                        School.storeSuggestions(school, standard, division, userId);
                    }


                }
            }

        }
        return userId;
    }

    @Override
    protected String doInBackground(Void... params) {
        String userId = doInBackgroundCore();
        return userId;
    }


    public void onPostExecuteHelper(final String userId){
        if(JoinedClasses.mHeaderProgressBar != null){
            JoinedClasses.mHeaderProgressBar.post(new Runnable() {
                @Override
                public void run() {
                    onPostExecuteCore(userId);
                }
            });
        }
    }

    public void onPostExecuteCore(String userId){

        if (userId != null && JoinedHelper.getSuggestionList(userId) != null) {
            JoinedClasses.suggestedGroups = JoinedHelper.getSuggestionList(userId);

            if(JoinedClasses.suggestionAdapter != null)
                JoinedClasses.suggestionAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onPostExecute(String userId) {
        onPostExecuteCore(userId);
    }
}
