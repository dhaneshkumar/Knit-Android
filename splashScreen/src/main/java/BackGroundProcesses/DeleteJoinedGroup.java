package BackGroundProcesses;

import java.util.List;

import joinclasses.JoinedClasses;
import utility.Utility;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;

import com.parse.DeleteCallback;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

public class DeleteJoinedGroup extends AsyncTask<Void, Void, String[]> {
  private String userId;
  private String groupCode;
  private String grpName;

  public DeleteJoinedGroup(String groupCode, String grpName) {

    ParseUser user = ParseUser.getCurrentUser();

    if (user == null)
      Utility.logout();

    this.userId = user.getUsername();
    this.groupCode = groupCode;
    this.grpName = grpName;
  }

  @Override
  protected String[] doInBackground(Void... params) {

    Utility.ls("deleted joined group running....");
    if (ParseUser.getCurrentUser() != null) {

      ParseUser user = ParseUser.getCurrentUser();

      List<List<String>> joined_gorups = user.getList("joined_groups");

      if (joined_gorups != null) {
        for (int i = 0; i < joined_gorups.size(); i++) {
          if (joined_gorups.get(i).get(0).equals(groupCode))
            joined_gorups.remove(i);
        }
      }

      user.put("joined_groups", joined_gorups);
      user.saveEventually();

      if (ParseInstallation.getCurrentInstallation() != null) {


        if (ParseInstallation.getCurrentInstallation().getList("channels") != null) {
          List<String> channelList = ParseInstallation.getCurrentInstallation().getList("channels");
          channelList.remove(groupCode);

          ParseInstallation.getCurrentInstallation().put("channels", channelList);
          ParseInstallation.getCurrentInstallation().saveEventually();
        }
      }


      ParseQuery<ParseObject> delquery3 = new ParseQuery<ParseObject>("GroupMembers");
      delquery3.whereEqualTo("code", groupCode);
      delquery3.whereEqualTo("emailId", ParseUser.getCurrentUser().getEmail());
      delquery3.findInBackground(new FindCallback<ParseObject>() {

        @Override
        public void done(List<ParseObject> objects, ParseException e) {
          if (e == null) {
            ParseObject.deleteAllInBackground(objects, new DeleteCallback() {

              @Override
              public void done(ParseException e) {
                if (e == null)
                  Log.d("textslate", "Query3 success!!");
              }
            });
          }
        }
      });
      /*
       * ParseQuery<ParseObject> delquery33 = new ParseQuery<ParseObject>("messages");
       * delquery33.whereEqualTo("code", groupCode); delquery33.whereEqualTo("userId",
       * ParseUser.getCurrentUser().getEmail()); delquery33.fromLocalDatastore();
       * 
       * try { List<ParseObject> objs = delquery33.find(); if (objs != null) {
       * ParseObject.unpinAll(objs); if (Messages.msgs != null) Messages.msgs.removeAll(objs);
       * 
       * } } catch (ParseException e1) { }
       */


      ParseQuery<ParseObject> delquery33;
      delquery33 = new ParseQuery<ParseObject>("GroupMembers");
      delquery33.fromLocalDatastore();
      delquery33.whereEqualTo("code", groupCode);
      delquery33.whereEqualTo("userId", ParseUser.getCurrentUser().getEmail());
      delquery33.whereEqualTo("emailId", ParseUser.getCurrentUser().getEmail());

      try {
        List<ParseObject> objs = delquery33.find();
        if (objs != null) {
          ParseObject.unpinAll(objs);
        }

        Log.d("textslate", "Local Query3 success!!");
      } catch (ParseException e1) {
        e1.printStackTrace();
      }


      /*
       * updating list items
       */
      if (JoinedClasses.joinedGroups != null) {

        for (int i = 0; i < JoinedClasses.joinedGroups.size(); i++) {
          if (JoinedClasses.joinedGroups.get(i).get(0).equals(groupCode)) {
            JoinedClasses.joinedGroups.remove(i);
            break;
          }
        }
      }
    }
    return null;
  }



  @Override
  protected void onPostExecute(String[] result) {
    // if (Messages.myadapter != null)
    // Messages.myadapter.notifyDataSetChanged();

    if (JoinedClasses.joinedadapter != null)
      JoinedClasses.joinedadapter.notifyDataSetChanged();

    if (JoinedClasses.progressBarLayout != null)
      JoinedClasses.progressBarLayout.setVisibility(View.GONE);
    if (JoinedClasses.editProfileLayout != null)
      JoinedClasses.editProfileLayout.setVisibility(View.VISIBLE);

    super.onPostExecute(result);
  }

}
