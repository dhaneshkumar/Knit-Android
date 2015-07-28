package BackGroundProcesses;

import android.util.Log;
import android.view.View;

import com.parse.ParseUser;

import java.util.Calendar;

import trumplabs.schoolapp.Application;
import trumplabs.schoolapp.Classrooms;
import trumplabs.schoolapp.MainActivity;
import utility.Config;
import utility.Utility;

/**
 * Refreshing joined-classes in background
 * Sync joined-classes of user with server
 */
public class JoinedClassRooms{

  public static void doInBackgroundCore(){
      if(Config.SHOWLOG) Log.d("DEBUG_JOINED", "fetching name/pic updates and setting lastTimeJoinedSync");

    //validating current user
    ParseUser user = ParseUser.getCurrentUser();
    if (user == null)
    {
        Utility.LogoutUtility.logout(); return;}


    String userId = user.getUsername();

    //just update all the Users info as we need to update name and profile pic only
    ClassRoomsUpdate.fetchUpdates();
    ClassRoomsUpdate.fetchProfilePics(userId);
  }

  public static void onPostExecuteHelper(){
      if(MainActivity.mHeaderProgressBar != null){
          MainActivity.mHeaderProgressBar.post(new Runnable() {
              @Override
              public void run() {
                onPostExecuteCore();
              }
          });
      }
  }

  static void onPostExecuteCore(){
      if (Classrooms.joinedClassAdapter != null)
          Classrooms.joinedClassAdapter.notifyDataSetChanged();

      if (MainActivity.mHeaderProgressBar != null)
          MainActivity.mHeaderProgressBar.setVisibility(View.GONE);
  }
}
