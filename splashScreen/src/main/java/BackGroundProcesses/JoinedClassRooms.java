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

public class JoinedClassRooms extends AsyncTask<Void, Void, String[]> {

  private String userId;
  private String[] mString;
  private List<List<String>> joinedGroups;
  private boolean loginFlag;

  public JoinedClassRooms() {
    ParseUser user = ParseUser.getCurrentUser();

    if (user == null)
      Utility.logout();

    this.userId = user.getUsername();
    this.loginFlag = false;
  }

  public JoinedClassRooms(boolean loginFlag) {
    ParseUser user = ParseUser.getCurrentUser();

    if (user == null)
      Utility.logout();

    this.userId = user.getUsername();
    this.loginFlag = true;
  }


  @Override
  protected String[] doInBackground(Void... params) {
    ParseUser user = ParseUser.getCurrentUser();

    Utility.ls("joined classrooms running....");
    if (user != null) {
      joinedGroups = user.getList(Constants.JOINED_GROUPS);

      if (joinedGroups == null) {
        joinedGroups = new ArrayList<List<String>>();

      } else {


        /*
         * Adding new joined list
         */
        for (int i = 0; i < joinedGroups.size(); i++) {

          String grpCode = joinedGroups.get(i).get(0).trim();

          /*
           * Log.d("joined"," updated joined grouos --------------------------");
           * 
           * if(user1.getJoined_groups()!= null) Log.d("joined", user1.getJoined_groups()); else
           * Log.d("joined", "empty after refreshing");
           */

          Queries2 joinQuery = new Queries2();
          try {
            if (!joinQuery.isCodegroupExist(grpCode, userId)) {
              joinQuery.storeCodegroup(grpCode, userId);

            } else {
              SessionManager session = new SessionManager(Application.getAppContext());
              int sessionCount = session.getAppOpeningCount();

              if (sessionCount % Config.senderPicUpdationCount == 0)
              {
                joinQuery.updateProfileImage(grpCode, userId);
                
              }
            }

           

          } catch (ParseException e) {
            e.printStackTrace();
          }

          // System.out.println("code : " + grpCode);

          try {
            if (!joinQuery.isGroupMemberExist(grpCode, userId)) {
              joinQuery.storeGroupMember(grpCode, userId, true);
            } else {
            }

          } catch (ParseException e) {

            /*
             * First time there wont be any GroupMember class. So, It will through exception in that
             * case.
             */
            try {
              joinQuery.storeGroupMember(grpCode, userId, false);
            } catch (ParseException e1) {
            }
          }
        }
      }
    }
    return mString;
  }



  @Override
  protected void onPostExecute(String[] result) {

    JoinedClasses.joinedGroups = joinedGroups;
    if (JoinedClasses.joinedadapter != null)
      JoinedClasses.joinedadapter.notifyDataSetChanged();


    if (JoinedClasses.mHeaderProgressBar != null)
      JoinedClasses.mHeaderProgressBar.setVisibility(View.GONE);

    if (loginFlag) {
      /*
       * Updating inbox msgs
       */

      Inbox newInboxMsg = new Inbox(null);
      newInboxMsg.execute();



      /*
       * Updating created class rooms list
       */

      CreatedClassRooms createdClassList = new CreatedClassRooms();
      createdClassList.execute();
    }


    // Utility.toast("joined grp refreshed");
    super.onPostExecute(result);
  }
}
