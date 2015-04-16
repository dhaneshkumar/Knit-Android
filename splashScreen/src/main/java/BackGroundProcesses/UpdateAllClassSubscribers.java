package BackGroundProcesses;

import android.os.AsyncTask;
import android.util.Log;

import com.parse.ParseUser;

import java.util.List;

import trumplabs.schoolapp.Constants;

/**
 * It updates subscribers list of all classes of a user.
 */
public class UpdateAllClassSubscribers extends AsyncTask<Void, Void, Void> {
    @Override
    protected Void doInBackground(Void... params) {

        ParseUser user = ParseUser.getCurrentUser();
        if(user != null)
        {
            if(user.getString("role").equals(Constants.TEACHER))
            {
                List<List<String>> createdClassroom = user.getList(Constants.CREATED_GROUPS);
                if(createdClassroom != null)
                {
                    for(int i=0; i<createdClassroom.size();i++)
                    {
                        MemberList memberList = new MemberList(createdClassroom.get(i).get(0));
                        memberList.execute();

                        Log.d("SUBSCRIBER_UPDATE", createdClassroom.get(i).get(0));
                    }
                }
            }
        }

        return null;
    }
}
