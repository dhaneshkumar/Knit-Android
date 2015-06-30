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
        //Log.d(LOGTAG, inviteType + " " + inviteMode + " " + classCode);
        ParseQuery query = ParseQuery.getQuery(Constants.INVITATION);
        query.fromLocalDatastore();
        query.whereEqualTo(Constants.PENDING, true);
        query.whereEqualTo(Constants.TYPE, inviteType);
        query.whereEqualTo(Constants.MODE, inviteMode);
        query.whereEqualTo(Constants.USER_ID, ParseUser.getCurrentUser().getUsername());

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



        //Log.d(LOGTAG, "pending for " + inviteType + " " + inviteMode + " " + classCode);

        HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("classCode", classCode);
        parameters.put("type", inviteType);
        parameters.put("mode", inviteMode);

        String teacherName = "";
        if(inviteType == Constants.INVITATION_P2P){
            ParseQuery<ParseObject> classQuery = new ParseQuery<>(Constants.CODE_GROUP);
            classQuery.fromLocalDatastore();
            classQuery.whereEqualTo("code", classCode);

            try{
                ParseObject codegroup = classQuery.getFirst();
                teacherName = codegroup.getString("Creator");
                parameters.put("teacherName", teacherName);
            }
            catch (ParseException e){
                e.printStackTrace();
            }
        }

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
        Log.d(LOGTAG, "sendAllPendingInvites() entered");
        int[] inviteTypes = {Constants.INVITATION_P2P, Constants.INVITATION_P2T, Constants.INVITATION_T2P, Constants.INVITATION_SPREAD};
        String[] inviteModes = {Constants.MODE_PHONE, Constants.MODE_EMAIL};

        ArrayList<String> createdClassCodes = new ArrayList<>();
        ArrayList<String> joinedClassCodes = new ArrayList<>();

        ParseUser user = ParseUser.getCurrentUser();
        if(user != null){
            List<List<String>> createdGroups = user.getList(Constants.CREATED_GROUPS);
            if(createdGroups != null) {
                for (List<String> group : createdGroups) {
                    String code = group.get(0);
                    if(code != null){
                        createdClassCodes.add(code);
                    }
                }
            }

            List<List<String>> joinedGroups = user.getList(Constants.JOINED_GROUPS);
            if(joinedGroups != null) {
                for (List<String> group : joinedGroups) {
                    String code = group.get(0);
                    if(code != null){
                        joinedClassCodes.add(code);
                    }
                }
            }
        }
        else{
            return; //won't happen
        }

        //Now we have all the 3 variables that can vary
        for(int inviteType : inviteTypes){
            for(String inviteMode : inviteModes){
                if(inviteType == Constants.INVITATION_T2P){
                    for(String classCode : createdClassCodes){
                        sendInvitePhonebook(inviteType, inviteMode, classCode);
                    }
                }
                else if(inviteType == Constants.INVITATION_P2P){
                    for(String classCode : joinedClassCodes){
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