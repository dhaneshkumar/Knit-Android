package BackGroundProcesses;

import android.os.AsyncTask;
import android.util.Log;
import android.view.View;

import com.parse.ParseObject;

import java.util.List;

import trumplabs.schoolapp.Constants;
import trumplabs.schoolapp.MainActivity;
import trumplabs.schoolapp.Messages;
import utility.Config;
import utility.Queries;
import utility.Utility;


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

  public void doInBackgroundCore(){
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
      Messages.updateInboxTotalCount();
  }

  @Override
  protected String[] doInBackground(Void... params) {
    doInBackgroundCore();
    return mStrings;
  }

  public void onPostExecuteHelper(){
      if(MainActivity.mHeaderProgressBar!=null){
          MainActivity.mHeaderProgressBar.post(new Runnable() {
              @Override
              public void run() {
                  onPostExecuteCore();
              }
          });
      }
  }

  public void onPostExecuteCore(){
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
  }

  public void syncOtherInboxDetails(){
      Log.d("DEBUG_SEEN_HANDLER", "running seenhandler");
      SeenHandler seenHandler = new SeenHandler();
      seenHandler.syncSeenJob(); //don't run as async task as already this is in a background thread.

      SyncMessageDetails.syncStatus();
      SyncMessageDetails.fetchLikeConfusedCountInbox();

      if(Messages.mPullToRefreshLayout != null){
          Messages.mPullToRefreshLayout.post(new Runnable() {
              @Override
              public void run() {
                  Log.d("DEBUG_AFTER_INBOX_COUNT_REFRESH", "Notifying Messages.myadapter");
                  if(Messages.myadapter != null){
                      Messages.myadapter.notifyDataSetChanged();
                  }
              }
          });
      }
  }

  @Override
  protected void onPostExecute(String[] result) {
      onPostExecuteCore();
      super.onPostExecute(result);

      /* Handle 'seen' of messages. Assume for now that since app is opened, user would have
    seen the new messages. Do this in a seperate thread */
      Runnable r = new Runnable() {
          @Override
          public void run(){
            syncOtherInboxDetails();
          }
      };

      Thread t = new Thread(r);
      t.setPriority(Thread.MIN_PRIORITY);
      t.start();
  }
}
