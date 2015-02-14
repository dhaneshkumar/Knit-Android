package BackGroundProcesses;

import java.util.HashMap;
import java.util.List;

import joinclasses.JoinedClasses;
import trumplabs.schoolapp.Constants;
import utility.Utility;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;

import com.parse.DeleteCallback;
import com.parse.FindCallback;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

/**
 *  leave joined classroom in background.
 */
public class DeleteJoinedGroup extends AsyncTask<Void, Void, Boolean> {
  private String userId;
  private String groupCode;
  private String grpName;

  public DeleteJoinedGroup(String groupCode, String grpName) {

    ParseUser user = ParseUser.getCurrentUser();

    if (user == null)
      {Utility.logout(); return;}

    this.userId = user.getUsername();
    this.groupCode = groupCode;
    this.grpName = grpName;
  }

  @Override
  protected Boolean doInBackground(Void... params) {

    Utility.ls("deleted joined group running....");

    ParseUser user = ParseUser.getCurrentUser();
    if (user != null) {

        //setting parameters
        HashMap<String, Object> param = new HashMap<String, Object>();
        param.put("classcode", groupCode);
        param.put("installationObjectId", ParseInstallation.getCurrentInstallation().getObjectId());

        boolean unsubscribeClass = false;
        try {
            unsubscribeClass = ParseCloud.callFunction("leaveclass", param);
        } catch (ParseException e) {
            e.printStackTrace();
        }


        //If class if subscribed successfully
        if(unsubscribeClass)
        {

            try {
                user.fetch();

            } catch (ParseException e) {
                e.printStackTrace();
            }
            return true;
        }
    }
    return false;
  }



  @Override
  protected void onPostExecute(Boolean result) {

    if(result) {

        JoinedClasses.joinedGroups = ParseUser.getCurrentUser().getList("joined_groups");

        if (JoinedClasses.joinedadapter != null)
            JoinedClasses.joinedadapter.notifyDataSetChanged();

        if (JoinedClasses.progressBarLayout != null)
            JoinedClasses.progressBarLayout.setVisibility(View.GONE);
        if (JoinedClasses.editProfileLayout != null)
            JoinedClasses.editProfileLayout.setVisibility(View.VISIBLE);
    }

    super.onPostExecute(result);
  }

}
