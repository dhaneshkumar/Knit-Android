package notifications;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import BackGroundProcesses.SendPendingMessages;
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

        if(role.equalsIgnoreCase(Constants.TEACHER)) {
            if (Utility.isInternetExistWithoutPopup()){
                if(Config.SHOWLOG) Log.d(LOGTAG, "onReceive() : connected");
                SendPendingMessages.spawnThread(false);
            }
            else{
                if(Config.SHOWLOG) Log.d(LOGTAG, "onReceive() : not connected");
            }
        }
    }
}