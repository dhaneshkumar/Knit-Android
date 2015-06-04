package BackGroundProcesses;

import android.util.Log;

import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import trumplabs.schoolapp.Constants;

/**
 * Created by ashish on 4/6/15.
 */
public class InviteTasks {
    static final String LOGTAG = "DEBUG_INVITE_TASKS";
    static boolean t2p_phone_running = false;
    public static void sendInvitePhonebook(int inviteType, String classCode){
        String mode = Constants.MODE_PHONE;
        if(t2p_phone_running) return;

        ParseQuery query = ParseQuery.getQuery(Constants.INVITATION);
        query.fromLocalDatastore();
        query.whereEqualTo(Constants.PENDING, true);
        query.whereEqualTo(Constants.TYPE, inviteType);
        query.whereEqualTo(Constants.MODE, mode);
        if(inviteType == Constants.INVITATION_T2P) {
            query.whereEqualTo(Constants.CLASS_CODE, classCode);
        }

        List<ParseObject> pendingInvitations = null;
        try{
            pendingInvitations = query.find();
        }
        catch (ParseException e){
            e.printStackTrace();
        }

        if(pendingInvitations == null || pendingInvitations.isEmpty()){
            t2p_phone_running = false; //release lock
            return;
        }

        HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("classCode", classCode);
        parameters.put("type", inviteType);
        parameters.put("mode", mode);

        List<HashMap<String, String>> data = new ArrayList<>();
        for(ParseObject invitation : pendingInvitations){
            HashMap<String, String> userData = new HashMap<>();
            if(invitation.getString(Constants.RECEIVER_NAME) != null && invitation.getString(Constants.RECEIVER) != null) {
                userData.put("name", invitation.getString(Constants.RECEIVER_NAME));
                userData.put("phone", invitation.getString(Constants.RECEIVER));
                data.add(userData);
            }
        }
        parameters.put("data", data);

        //call cloud function inviteUsers
        String result = "false";
        try{
            result = ParseCloud.callFunction("inviteUsers", parameters);
        }
        catch (ParseException e){
            e.printStackTrace();
        }

        Log.d(LOGTAG, "type=" + inviteType + ", mode=" + mode +
                ", count=" + data.size() + "/" + pendingInvitations.size() + ", RESULT=" + result);

        if(result.equals("true")) {
            //if success change pending flag of each of the invitations and pin them all
            for (ParseObject invitation : pendingInvitations) {
                invitation.put(Constants.PENDING, false);
            }

            try {
                ParseObject.pinAll(pendingInvitations);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        //before returning release the "lock"
        t2p_phone_running = false;
    }
}