package BackGroundProcesses;

import android.util.Log;

import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import trumplabs.schoolapp.Constants;

/**
 * Created by ashish on 4/6/15.
 */
public class InviteTasks {
    static final String LOGTAG = "DEBUG_INVITE_TASKS";
    public static void sendInvitePhonebook(int inviteType, String inviteMode, String classCode){
        ParseQuery query = ParseQuery.getQuery(Constants.INVITATION);
        query.fromLocalDatastore();
        query.whereEqualTo(Constants.PENDING, true);
        query.whereEqualTo(Constants.TYPE, inviteType);
        query.whereEqualTo(Constants.MODE, inviteMode);
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
            return;
        }

        HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("classCode", classCode);
        parameters.put("type", inviteType);
        parameters.put("mode", inviteMode);

        List<ArrayList<String>> data = new ArrayList<>();
        for(ParseObject invitation : pendingInvitations){
            ArrayList<String> userData = new ArrayList<>();
            if(invitation.getString(Constants.RECEIVER_NAME) != null && invitation.getString(Constants.RECEIVER) != null) {
                userData.add(invitation.getString(Constants.RECEIVER_NAME));
                userData.add(invitation.getString(Constants.RECEIVER));
                data.add(userData);
            }
        }
        parameters.put("data", data);

        //call cloud function inviteUsers
        boolean result = false;
        try{
            ParseCloud.callFunction("inviteUsers", parameters);
            result = true; //No exception means all went well(wrong number won't receive, and correct number optimistic assumption)
        }
        catch (ParseException e){
            e.printStackTrace();
        }

        Log.d(LOGTAG, "type=" + inviteType + ", mode=" + inviteMode +
                ", count=" + data.size() + "/" + pendingInvitations.size() + ", RESULT=" + result);

        if(result) {
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
    }

    /* Take each combination of type, mode (and class in case of T2P type) and
        send the invites using above method
     */
    static void sendAllPendingInvites(){
        int[] inviteTypes = {Constants.INVITATION_P2P, Constants.INVITATION_P2T, Constants.INVITATION_T2P, Constants.INVITATION_SPREAD};
        String[] inviteModes = {Constants.MODE_PHONE, Constants.MODE_EMAIL};

        ArrayList<String> classCodes = new ArrayList<>();

        ParseUser user = ParseUser.getCurrentUser();
        if(user != null){
            List<List<String>> groups = user.getList("joined_groups");
            if(groups != null) {
                for (List<String> group : groups) {
                    String code = group.get(0);
                    if(code != null){
                        classCodes.add(code);
                    }
                }
            }
        }

        //Now we have all the 3 variables that can vary
        for(int inviteType : inviteTypes){
            for(String inviteMode : inviteModes){
                if(inviteType == Constants.INVITATION_T2P){
                    for(String classCode : classCodes){
                        sendInvitePhonebook(inviteType, inviteMode, classCode);
                    }
                }
                else {
                    sendInvitePhonebook(inviteType, inviteMode, "");
                }
            }
        }
    }
}