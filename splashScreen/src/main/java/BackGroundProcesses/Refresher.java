package BackGroundProcesses;

import android.util.Log;

import com.parse.ParseUser;

import java.util.Calendar;
import java.util.Date;

import trumplabs.schoolapp.Application;
import trumplabs.schoolapp.Outbox;
import utility.Config;
import utility.Queries2;
import utility.SessionManager;
import utility.Utility;


public class Refresher {
    ParseUser freshUser;

    public Refresher(int appOpeningCount) {
        Log.d("DEBUG_REFRESHER", "Entering Refresher Thread");

        freshUser = ParseUser.getCurrentUser();

        if (freshUser != null) {

            final SessionManager sm = new SessionManager(Application.getAppContext());
            //Utility.checkParseInstallation(); //important for upgrade issues. This will be called first time app is launched after update
      /*
       * Storing current time stamp
       */

            if(Application.isAppForeground()) {
                    Utility.updateCurrentTimeInBackground();
            }

            Log.d("DEBUG_REFRESHER",  "calling background tasks");
        /*
         * Updating inbox msgs
         */
            Log.d("DEBUG_REFRESHER", "Attempting calling Inbox execute()");

            Inbox.syncOtherInboxDetails(); //called always in background but sends only dirty data
            // (modified like, seen, confused status) if any
            // this should be called before fetching like/confused counts

            if((Application.isAppForeground() && isSufficientGapInbox())) { //or if just signed in
                if(!Inbox.isQueued) { //if not already queued
                    Inbox newInboxTask = new Inbox();
                    newInboxTask.doInBackgroundCore();
                    newInboxTask.onPostExecuteHelper(); //done
                    newInboxTask.fetchLikeConfusedCountInbox();
                }
            }
            else{
                Log.d("DEBUG_REFRESHER", "refresher skipping inbox update : visible " + Application.isAppForeground() + " gap " +  isSufficientGapInbox());
            }

           /*
            *   Updating counts for outbox messages only if main activity visible and sufficent gap since last update
            */
            if((Application.isAppForeground() && isSufficientGapOutbox())) {
                Outbox.refreshCountCore();
            }
            else{
                Log.d("DEBUG_REFRESHER", "refresher skipping Outbox update : visible " + Application.isAppForeground() + " gap " + isSufficientGapOutbox());
            }

            /*
                Update total count of outbox messages
             */
            Outbox.updateOutboxTotalMessages(); //simple local function

             /*
             * Updating joined classes teacher details(name, profile pic) if gap is larger than Config.joinedClassUpdateGap
             */
            if(!Application.joinedSyncOnce) {
                Application.joinedSyncOnce = true;
                JoinedClassRooms.doInBackgroundCore();
                JoinedClassRooms.onPostExecuteHelper();
            }
            else{
                Log.d("DEBUG_REFRESHER", "refresher joined classes update - done once");
            }

            //Refresh local outbox data, if not in valid state, clear and fetch new.
            //If already present then no need to fetch outbox messages
            if(freshUser.getString("role").equalsIgnoreCase("teacher")) {
                if(sm.getOutboxLocalState(freshUser.getUsername())==0) {
                    Log.d("DEBUG_REFRESHER", "fetching outbox messages for the first and last time");
                    //no need to do in seperate thread. Already this is running in a background thread
                    OutboxMsgFetch.fetchOutboxMessages();
                }
                else{
                    Log.d("DEBUG_REFRESHER", "local outbox data intact. No need to fetch anything");
                }
            }

            //Fetch codegroup details if not yet fetched after reinstallation
            if(sm.getCodegroupLocalState(freshUser.getUsername()) == 0){
                Log.d("DEBUG_REFRESHER", "fetching Codegroup info for the first and last time");
                Queries2.fetchAllClassDetails();
            }
            else{
                Log.d("DEBUG_REFRESHER", "local Codegroup data intact. No need to fetch anything");
            }

            //Send all pending invites
            InviteTasks.sendAllPendingInvites();

            //Send all pending messages
            SendPendingMessages.spawnThread(false); //direct call since already in a thread
        }
        else {
            Log.d("DEBUG_REFRESHER", "User NULL");
            SessionManager session = new SessionManager(Application.getAppContext());
            session.reSetAppOpeningCount();
        }

        Log.d("DEBUG_REFRESHER", "Leaving Refresher Thread");
    }

    /*
        check if since last time when inbox refreshed, difference is more than 15 minutes
    */
    public static boolean isSufficientGapInbox(){
        Date currentTime = Calendar.getInstance().getTime();
        if(Application.lastTimeInboxSync == null || (currentTime.getTime() - Application.lastTimeInboxSync.getTime() > Config.inboxOutboxUpdateGap)){
            return true;
        }
        return false;
    }

    public static boolean isSufficientGapOutbox(){
        Date currentTime = Calendar.getInstance().getTime();
        if(Application.lastTimeOutboxSync == null || (currentTime.getTime() - Application.lastTimeOutboxSync.getTime() > Config.inboxOutboxUpdateGap)){
            return true;
        }
        return false;
    }

    public static boolean isSufficientGapJoined(){
        Date currentTime = Calendar.getInstance().getTime();
        if(Application.lastTimeJoinedSync == null || (currentTime.getTime() - Application.lastTimeJoinedSync.getTime() > Config.joinedClassUpdateGap)){
            return true;
        }
        return false;
    }
}
