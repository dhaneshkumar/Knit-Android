package BackGroundProcesses;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import trumplabs.schoolapp.Application;
import trumplabs.schoolapp.ClassMembers;
import trumplabs.schoolapp.Classrooms;
import trumplabs.schoolapp.Constants;
import trumplabs.schoolapp.MemberDetails;
import utility.Queries;
import utility.SessionManager;
import utility.Utility;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;

import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;


public class MemberList extends AsyncTask<Void, Void, String[]> {

  private String groupCode;
  private List<MemberDetails> memberDetails;
  private Queries query;
  private String[] mString;
  private boolean openingFlag;
  private boolean reminder;


  
  
  
  public MemberList(String groupCode, boolean openingFlag, boolean reminder) {
    this.groupCode = groupCode;
    this.query = new Queries();
    this.openingFlag =openingFlag;
    this.reminder =reminder;
  }

  @Override
  protected String[] doInBackground(Void... params) {

      ParseUser user = ParseUser.getCurrentUser();
      final SessionManager sessionManager = new SessionManager(Application.getAppContext());
      final int appVersion = sessionManager.getAppMemberVersion();
      final int smsVersion = sessionManager.getSmsMemberVersion();

      //retrieving codegroup entry of given class
      ParseQuery<ParseObject> codeQuery = ParseQuery.getQuery(Constants.CODE_GROUP);
      codeQuery.fromLocalDatastore();
      codeQuery.whereEqualTo("code", groupCode);
      codeQuery.whereEqualTo("userId", user.getUsername());

      ParseObject obj = null;
      try {
          obj = codeQuery.getFirst();
      } catch (ParseException e) {
          e.printStackTrace();
      }

      if (obj != null) {
          //fetching codegroup object from server
          try {
              obj.fetchIfNeeded();
          } catch (ParseException e) {
              e.printStackTrace();
          }

          final int currentAppVersion = obj.getInt(Constants.APP_MEMBER_VERSION);
          final int currentSmsVersion = obj.getInt(Constants.SMS_MEMBER_VERSION);


          Runnable r = new Runnable() {
              @Override
              public void run() {

                  if (currentAppVersion != appVersion) {
                      storeAppMembers(groupCode);
                      sessionManager.setAppMemberVersion(currentAppVersion);
                  }

                  if (currentSmsVersion != smsVersion) {
                      storeSmsMembers(groupCode);
                      sessionManager.setSmsMemberVersion(currentAppVersion);
                  }


                  if (Classrooms.listv != null) {
                      Classrooms.listv.post(new Runnable() {
                          @Override
                          public void run() {
                              Log.d("DEBUG_AFTER_MEMBER_LIST_REFRESH", "Notifying ClassMembers.myadapter & Classrooms.myadapter");

                              if (memberDetails != null) {
                                  ClassMembers.memberDetails = memberDetails;
                              }

                              if (ClassMembers.mHeaderProgressBar != null)
                                  ClassMembers.mHeaderProgressBar.setVisibility(View.GONE);

                              if (ClassMembers.myadapter != null)
                                  ClassMembers.myadapter.notifyDataSetChanged();

                              if (Classrooms.myadapter != null)
                                  Classrooms.myadapter.notifyDataSetChanged();
                          }
                      });
                  }
              }

          };

          Thread t = new Thread(r);
          t.setPriority(Thread.MIN_PRIORITY);
          t.start();
      }

      return null;
  }




  private void storeAppMembers(String code)
  {
      HashMap<String, String> param = new HashMap<String, String>();
      param.put("classcode", code);

      List<ParseObject> memberList = null;
      try {
          memberList = ParseCloud.callFunction("showappsubscribers", param);
      } catch (ParseException e) {
          e.printStackTrace();
      }

      if(memberList != null)
      {
          try {
              ParseObject.pinAll(memberList);
          } catch (ParseException e) {
              e.printStackTrace();
          }
      }
  }

    private void storeSmsMembers(String code)
    {
        HashMap<String, String> param = new HashMap<String, String>();
        param.put("classcode", code);

        List<ParseObject> memberList = null;
        try {
            memberList = ParseCloud.callFunction("showsmssubscribers", param);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        if(memberList != null)
        {

            for(int i=0 ; i < memberList.size(); i++)
            {
                ParseObject member = memberList.get(i);
                member.put("userId", ParseUser.getCurrentUser().getUsername());
                try {
                    member.pin();
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        }
    }


}
