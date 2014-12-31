package BackGroundProcesses;

import java.util.ArrayList;
import java.util.List;

import trumplabs.schoolapp.Application;
import trumplabs.schoolapp.ClassMembers;
import trumplabs.schoolapp.Classrooms;
import trumplabs.schoolapp.Constants;
import trumplabs.schoolapp.MainActivity;
import utility.SessionManager;
import utility.Utility;
import android.os.AsyncTask;
import android.view.View;

import com.parse.ParseException;
import com.parse.ParseUser;

/*
 * Retrieve created class list in background and save it locally
 */
public class CreatedClassRooms extends AsyncTask<Void, Void, String[]> {

  private List<List<String>> createdGroups;
  private String userId;
  private String[] mString;

  public CreatedClassRooms() {
    ParseUser user = ParseUser.getCurrentUser();

    if (user == null)
      Utility.logout();

    this.userId = user.getUsername();
  }

  @Override
  protected String[] doInBackground(Void... params) {
    ParseUser user = ParseUser.getCurrentUser();

    Utility.ls("created classroooms running....");


    if (user != null) {

      try {
        user.fetchIfNeeded();
      } catch (ParseException e) {
      }

      createdGroups = user.getList(Constants.CREATED_GROUPS);

      if (createdGroups == null) {
        createdGroups = new ArrayList<List<String>>();
      }
    }
    return mString;
  }


  @Override
  protected void onPostExecute(String[] result) {

    Classrooms.createdGroups = createdGroups;
    if (Classrooms.myadapter != null)
      Classrooms.myadapter.notifyDataSetChanged();

    /*
     * Updating member list
     */

    ParseUser user = ParseUser.getCurrentUser();
    if (user != null) {
      List<List<String>> createdGroupList = user.getList(Constants.CREATED_GROUPS);

      if (createdGroupList != null) {
        ClassMembers classMembers = new ClassMembers();
        classMembers.intializeBackgroundParameters();

        for (int i = 0; i < createdGroupList.size(); i++) {
          /*
           * updating member list
           */

          SessionManager session = new SessionManager(Application.getAppContext());
          int count = session.getAppOpeningCount();

          if (count % 5 == 0) {
            MemberList memberList = new MemberList(createdGroupList.get(i).get(0), true, true);
            memberList.execute();
          }
          else
          {
            MemberList memberList = new MemberList(createdGroupList.get(i).get(0), true, false);
            memberList.execute();
          }

          /*
           * Retriving class msgs
           */
        }
      }
    }

    if (MainActivity.mHeaderProgressBar != null)
      MainActivity.mHeaderProgressBar.setVisibility(View.GONE);

    if (MainActivity.progressBarLayout != null)
      MainActivity.progressBarLayout.setVisibility(View.GONE);
    if (MainActivity.editLayout != null)
      MainActivity.editLayout.setVisibility(View.VISIBLE);
    super.onPostExecute(result);
  }
}
