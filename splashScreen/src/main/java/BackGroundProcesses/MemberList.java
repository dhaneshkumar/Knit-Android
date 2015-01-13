package BackGroundProcesses;

import java.util.ArrayList;
import java.util.List;

import trumplabs.schoolapp.ClassMembers;
import trumplabs.schoolapp.Classrooms;
import trumplabs.schoolapp.MemberDetails;
import utility.Queries;
import utility.Utility;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;

import com.parse.ParseObject;


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

    Runnable r = new Runnable() {
      @Override
      public void run(){
          Utility.ls("memberlist running....");
          memberDetails = query.getServerClassMembers(groupCode, openingFlag,reminder);

          //following is the onpostexecute thing
          if (Classrooms.listv != null){
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

    return mString;
  }

  @Override
  protected void onPostExecute(String[] result) {


    /*if (memberDetails != null) {
      ClassMembers.memberDetails = memberDetails;

      if (ClassMembers.mHeaderProgressBar != null)
        ClassMembers.mHeaderProgressBar.setVisibility(View.GONE);

      if (ClassMembers.myadapter != null)
        ClassMembers.myadapter.notifyDataSetChanged();

      if (Classrooms.myadapter != null)
        Classrooms.myadapter.notifyDataSetChanged();
    }*/

    super.onPostExecute(result);
  }
}
