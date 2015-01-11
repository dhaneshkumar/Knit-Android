package BackGroundProcesses;

import java.util.List;

import trumplabs.schoolapp.Constants;
import trumplabs.schoolapp.MainActivity;
import trumplabs.schoolapp.Messages;
import utility.Config;
import utility.Queries;
import utility.Utility;
import android.os.AsyncTask;
import android.view.View;

import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;


public class Inbox extends AsyncTask<Void, Void, String[]> {

  String[] mStrings;
  private List<ParseObject> msgs;
  private List<ParseObject> newMsgs;
  private Queries query;
  private boolean newDataStatus = false;
  

  public Inbox( List<ParseObject> msgs)
  {
    this.msgs = msgs;
    query = new Queries();
  }
  
  @Override
  protected String[] doInBackground(Void... params) {

    Utility.ls("inbox running....");
    int initialSize = -1;
    if(msgs != null)
    {
      initialSize =  msgs.size();
    }
    
    
    newMsgs = query.getServerInboxMsgs();
    
    if(newMsgs != null)
    {
      if(newMsgs.size()- initialSize == 0)
      {
        newDataStatus = true;
      }
      
      /*
       * Deleting extra element from list
       */
      while(newMsgs.size() > Config.inboxMsgCount)
      {
        newMsgs.remove(newMsgs.size()-1);
      }
      
      Messages.msgs = newMsgs;
    }

    //update Messages.totalInboxMessages
    ParseUser user = ParseUser.getCurrentUser();

    if (user == null)
      Utility.logout();

    ParseQuery<ParseObject> query = ParseQuery.getQuery("GroupDetails");
    query.fromLocalDatastore();
    //query.orderByDescending(Constants.TIMESTAMP);
    query.whereEqualTo("userId", user.getUsername());
    try{
      Messages.totalInboxMessages = query.count();
    }
    catch(ParseException e){
      e.printStackTrace();
    }

    //SyncMessageDetails.syncStatus();
    //SyncMessageDetails.fetchLikeConfusedCountInbox();
    //SyncMessageDetails.fetchLikeConfusedCountOutbox();

    return mStrings;
  }

  @Override
  protected void onPostExecute(String[] result) {
    Constants.updatedTimestamp = false;
    
    if(MainActivity.mHeaderProgressBar != null)
      MainActivity.mHeaderProgressBar.setVisibility(View.GONE);
    if(Messages.myadapter != null)
      Messages.myadapter.notifyDataSetChanged();
    if(Messages.mPullToRefreshLayout != null)
      Messages.mPullToRefreshLayout.setRefreshing(false);

    if(newDataStatus)
    {
      //Utility.toast("No new messages to show");
      newDataStatus = false;
    }

    super.onPostExecute(result);
  }
}
