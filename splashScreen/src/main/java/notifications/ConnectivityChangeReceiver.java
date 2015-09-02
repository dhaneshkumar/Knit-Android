package notifications;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

import com.parse.ParseUser;

import BackGroundProcesses.SendPendingMessages;
import chat.SendPendingChatNotifications;
import trumplabs.schoolapp.Constants;
import utility.Config;
import utility.Utility;

/**
 * Created by ashish on 18/1/15.
 */

public class ConnectivityChangeReceiver extends WakefulBroadcastReceiver {
    static final String LOGTAG = "DBG_CONNECTIVITY_RECVR";

    public ConnectivityChangeReceiver() {
    }

    @Override
    public void onReceive(final Context context, Intent intent) {
        if(Config.SHOWLOG) Log.d(LOGTAG, "onReceive() : entered");

        ParseUser currentParseUser = ParseUser.getCurrentUser();
        if(currentParseUser == null){
            return;
        }

        String role = currentParseUser.getString("role");
        if(role == null)
            return;

        SendPendingChatNotifications.spawnThread();

        if(role.equalsIgnoreCase(Constants.TEACHER)) {
            if (Utility.isInternetExistWithoutPopup()){
                if(Config.SHOWLOG) Log.d(LOGTAG, "onReceive() : connected");
                SendPendingMessages.spawnThread(false);

                //ZCY1350-0000003003
                //Firebase mFirebaseRef = new Firebase(ChatActivityRecyclerView.FIREBASE_URL).child("ZCY1350-0000003003"); //ONLY THIS WORKS, NO NEED FOR NEXT STMT
                //mFirebaseRef.keepSynced(true); //not required
            }
            else{
                if(Config.SHOWLOG) Log.d(LOGTAG, "onReceive() : not connected");
            }
        }
    }
}