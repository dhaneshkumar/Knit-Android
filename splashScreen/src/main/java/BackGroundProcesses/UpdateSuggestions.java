package BackGroundProcesses;

import android.os.AsyncTask;

import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import joinclasses.JoinedClasses;
import joinclasses.JoinedHelper;
import library.UtilString;
import trumplabs.schoolapp.Application;
import trumplabs.schoolapp.Constants;
import utility.Config;
import utility.SessionManager;
import utility.Utility;

/**
 * Updates suggestions list for user in background
 * <p/>
 * Created by Dhanesh on 12/25/2014.
 */
public class UpdateSuggestions extends AsyncTask<Void, Void, String> {
    private boolean refreshFlag;    //true : means calling from joinedGroup refreshing function; false : normal refresher calling

    public UpdateSuggestions()
    {
        this.refreshFlag= false;
    }
    public UpdateSuggestions(boolean refreshFlag)
    {
        this.refreshFlag= refreshFlag;
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


        if(refreshFlag );
        else
        {
            SessionManager sessionManager = new SessionManager(Application.getAppContext());

            /*
            Update suggestions occasionally ,if app opening count is greater than specified limit.
            Since there wont be any suggestions after 2 weeks.
            else update every time.
             */
            if((sessionManager.getAppOpeningCount() > Config.updateSuggestionLimit) &&
                    (sessionManager.getAppOpeningCount() % Config.updateSuggestionInterval!=0))
                return userId;
        }

        List<ParseObject> codeGroupObjectList = null;
        try {
            HashMap<String, Object> params = new HashMap<String, Object>();
            codeGroupObjectList = ParseCloud.callFunction("getAllClassroomSuggestions", params);
        } catch (ParseException e) {
            e.printStackTrace();
        }


        //storing list locally
        if(codeGroupObjectList != null)
        {
            for(int i=0; i < codeGroupObjectList.size(); i++)
            {
                ParseObject obj = codeGroupObjectList.get(i);
                obj.put("userId", userId);
                try {
                    obj.pin();
                } catch (ParseException e) {
                    e.printStackTrace();
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
