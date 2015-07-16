package BackGroundProcesses;

import android.os.AsyncTask;
import android.util.Log;
import android.view.View;

import com.parse.ParseObject;

import java.util.Calendar;
import java.util.List;

import trumplabs.schoolapp.Application;
import trumplabs.schoolapp.Constants;
import trumplabs.schoolapp.MainActivity;
import trumplabs.schoolapp.Messages;
import utility.Config;
import utility.Queries;


public class Inbox extends AsyncTaskProxy<Void, Void, String[]> {

  String[] mStrings;

    private Queries query;
    public static boolean isQueued = false;

    int myid = 0;
    static int id = 0;

  public Inbox()
  {
      isQueued = true; //enable so that another Inbox asynctask is not triggered
      myid = id++;
      Log.d("_DEBUG_INBOX", "queued " + myid);
      query = new Queries();
  }

  public void doInBackgroundCore(){
      Log.d("_DEBUG_INBOX", "running background " + myid);

    //set lastTimeInboxSync
      Application.lastTimeInboxSync = Calendar.getInstance().getTime();

      Log.d("DEBUG_INBOX", "fetching new messages and setting lastTimeInboxSync");

      List<ParseObject> newMsgs = query.getServerInboxMsgs();

      if(newMsgs != null)
      {
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
      Log.d("_DEBUG_INBOX", "helper leaving " + myid);
      isQueued = false; //remove queued flag.

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
      if (MainActivity.progressBarLayout != null)
          MainActivity.progressBarLayout.setVisibility(View.GONE);

      if(Messages.myadapter != null)
          Messages.myadapter.notifyDataSetChanged();
      if(Messages.mPullToRefreshLayout != null)
          Messages.mPullToRefreshLayout.setRefreshing(false);
  }

    //notifies the adapter also
  public static void syncOtherInboxDetails(){
      Log.d("DEBUG_SEEN_HANDLER", "running seenhandler");
      SeenHandler.syncSeenJob(); //don't run as async task as already this is in a background thread.
      SyncMessageDetails.syncStatus();

      if(Messages.mPullToRefreshLayout != null){
          Messages.mPullToRefreshLayout.post(new Runnable() {
              @Override
              public void run() {
                  //Log.d("DEBUG_AFTER_INBOX_COUNT_REFRESH", "Notifying Messages.myadapter");
                  if(Messages.myadapter != null){
                      Messages.myadapter.notifyDataSetChanged();
                  }
              }
          });
      }
  }
    //doesn't notify the adapter
    public void fetchLikeConfusedCountInbox(){
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
      Log.d("_DEBUG_INBOX", "asynctask leaving " + myid);
      isQueued = false; //remove queued flag.

      onPostExecuteCore();
      super.onPostExecute(result);

      /* Handle 'seen' of messages. Assume for now that since app is opened, user would have
         seen the new messages. Do this in a seperate thread */
      Runnable r = new Runnable() {
          @Override
          public void run(){
              syncOtherInboxDetails(); //order is important since first we should convey dirty like/confused status and then fetch updated counts
              fetchLikeConfusedCountInbox();
          }
      };

      Thread t = new Thread(r);
      t.setPriority(Thread.MIN_PRIORITY);
      t.start();
  }
}
