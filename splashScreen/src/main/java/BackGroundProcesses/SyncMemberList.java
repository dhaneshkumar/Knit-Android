package BackGroundProcesses;

import android.os.Handler;
import android.os.Message;
import android.view.View;

import java.util.List;

import trumplabs.schoolapp.ClassMembers;
import trumplabs.schoolapp.Classrooms;
import trumplabs.schoolapp.MemberDetails;
import utility.Queries;

public class SyncMemberList {

  private String groupCode;
  private List<MemberDetails> memberDetails;
  private Queries query;

  public SyncMemberList(String groupCode) {
    this.groupCode = groupCode;
    this.query = new Queries();
  }

  /*public void execute() {


    new Thread() {
      public void run() {
       // memberDetails = query.getServerClassMembers(groupCode);
        //messageHandler.sendEmptyMessage(0);
      }
    }.start();

  }
*/
  private Handler messageHandler = new Handler() {
    public void handleMessage(Message msg) {
      super.handleMessage(msg);

      if (memberDetails != null) {
        ClassMembers.memberDetails = memberDetails;

        if (ClassMembers.mHeaderProgressBar != null)
          ClassMembers.mHeaderProgressBar.setVisibility(View.GONE);

        if (ClassMembers.myadapter != null)
          ClassMembers.myadapter.notifyDataSetChanged();

        if (Classrooms.createdClassAdapter != null)
          Classrooms.createdClassAdapter.notifyDataSetChanged();
      }
    }
  };
}
