package BackGroundProcesses;

import java.util.ArrayList;
import java.util.List;

import joinclasses.JoinedClasses;
import trumplabs.schoolapp.Application;
import trumplabs.schoolapp.Constants;
import utility.Config;
import utility.Queries2;
import utility.SessionManager;
import utility.Utility;
import android.os.AsyncTask;
import android.view.View;

import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

/**
 * Refreshing joined-classes in background
 * Sync joined-classes of user with server
 */
public class JoinedClassRooms extends AsyncTask<Void, Void, String[]> {

  private String userId;
  private List<List<String>> joinedGroups;

  @Override
  protected String[] doInBackground(Void... params) {

    //validating current user
    ParseUser user = ParseUser.getCurrentUser();
      if (user == null)
      {Utility.logout(); return null;}

      userId = user.getUsername();

      Utility.ls("joined classrooms running in background....");

      joinedGroups = user.getList(Constants.JOINED_GROUPS);

      if (joinedGroups == null) {
        joinedGroups = new ArrayList<List<String>>();

      } else {


        /*
         * Adding new joined list
         */
        for (int i = 0; i < joinedGroups.size(); i++) {

          String grpCode = joinedGroups.get(i).get(0).trim();

          Queries2 joinQuery = new Queries2();

          try {

            /*
              Checking existence of codegroup entry for each joined-groups.
              If it doesn't exist then fetch them from server and store locally
              else update profile pic
               */
            if (!joinQuery.isCodegroupExist(grpCode, userId)) {
              joinQuery.storeCodegroup(grpCode, userId);    //fetching from server and storing locally

            } else {
              SessionManager session = new SessionManager(Application.getAppContext());
              int sessionCount = session.getAppOpeningCount();

                //updating profile image of teacher
              if (sessionCount % Config.senderPicUpdationCount == 0)
              {
                joinQuery.updateProfileImage(grpCode, userId);
              }
            }
          } catch (ParseException e) {
            e.printStackTrace();
          }
          }
      }
    return null;
  }

  @Override
  protected void onPostExecute(String[] result) {

    JoinedClasses.joinedGroups = joinedGroups;
    if (JoinedClasses.joinedadapter != null)
      JoinedClasses.joinedadapter.notifyDataSetChanged();

    if (JoinedClasses.mHeaderProgressBar != null)
      JoinedClasses.mHeaderProgressBar.setVisibility(View.GONE);

    super.onPostExecute(result);
  }
}
