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

      //just update all the Users info as we need to update name and profile pic only
    ClassRoomsUpdate.fetchUpdates();
    ClassRoomsUpdate.fetchProfilePics(userId);
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
