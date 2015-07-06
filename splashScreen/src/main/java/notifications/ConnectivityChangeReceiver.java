package notifications;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

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
        Log.d(LOGTAG, "onReceive() : entered");

        if(Utility.isInternetExistWithoutPopup()){
            Log.d(LOGTAG, "onReceive() : connected");
            SendPendingMessages.spawnThread(false);
        }
        else{
            Log.d(LOGTAG, "onReceive() : not connected");
        }
    }
}