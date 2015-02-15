package BackGroundProcesses;

import android.os.AsyncTask;
import android.util.Log;
import android.view.View;

import com.parse.ParseException;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;

import joinclasses.JoinedClasses;
import trumplabs.schoolapp.Application;
import trumplabs.schoolapp.Constants;
import utility.Config;
import utility.Queries2;
import utility.SessionManager;
import utility.Utility;

/**
 * Refreshing joined-classes in background
 * Sync joined-classes of user with server
 */
public class JoinedClassRooms extends AsyncTask<Void, Void, String[]> {

  private String userId;
  private List<List<String>> joinedGroups;

  @Override
  protected String[] doInBackground(Void... params) {
    doInBackgroundCore();
    return null;
  }

  public void doInBackgroundCore(){

        //validating current user
        ParseUser user = ParseUser.getCurrentUser();
        if (user == null)
        {Utility.logout(); return;}

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

                          Log.d("JOIN", "updating profile...");
                          //updating profile image of teacher
                          joinQuery.updateProfileImage(grpCode, userId);
                      }
                  } catch (ParseException e) {
                      e.printStackTrace();
                  }
              }
          }
  }

  public void onPostExecuteHelper(){
      if(JoinedClasses.mHeaderProgressBar != null){
          JoinedClasses.mHeaderProgressBar.post(new Runnable() {
              @Override
              public void run() {
                onPostExecuteCore();
              }
          });
      }
  }

  public void onPostExecuteCore(){
      JoinedClasses.joinedGroups = joinedGroups;
      if (JoinedClasses.joinedadapter != null)
          JoinedClasses.joinedadapter.notifyDataSetChanged();

      if (JoinedClasses.mHeaderProgressBar != null)
          JoinedClasses.mHeaderProgressBar.setVisibility(View.GONE);
  }

  @Override
  protected void onPostExecute(String[] result) {
    onPostExecuteCore();
    super.onPostExecute(result);
  }

}
