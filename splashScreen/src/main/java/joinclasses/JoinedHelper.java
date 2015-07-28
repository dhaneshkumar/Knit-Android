package joinclasses;

import android.util.Log;

import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParseUser;

import java.io.File;
import java.util.HashMap;
import java.util.List;

import library.UtilString;
import notifications.EventCheckerAlarmReceiver;
import notifications.NotificationGenerator;
import trumplabs.schoolapp.Application;
import trumplabs.schoolapp.Constants;
import utility.Config;
import utility.Queries;
import utility.Queries2;
import utility.Utility;

/**
 * Contains helper functions for joined class
 * <p/>
 * Created by Dhanesh on 12/23/2014.
 */
public class JoinedHelper {

    /**
     * join new class
     * @param classcode, childname
     * @How : first checks whether all group added or not, if not then it call parse cloud function and join that class
     *       fetch ParseUser’s joinedgroup
     *       locally download profile pic of sender in background
     *       send local msg to inbox
     *       send internal notification
     *       store all classroom suggestions <parse cloud function>
     *
     */
    public static int joinClass(String classcode, String childname)  {

        //check class already added or not
        if(Queries.isJoinedClassExist(classcode))
            return 2;

        if(Config.SHOWLOG) Log.d("join", "class joining started current.........");
        if(Config.SHOWLOG) Log.d("join", classcode);
        if(Config.SHOWLOG) Log.d("join", childname);

        //setting parameters
        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("classCode", classcode);
        params.put("associateName", childname);
        params.put("installationId", ParseInstallation.getCurrentInstallation().getInstallationId());

        HashMap<String, Object> result = null;

        try {
            result = ParseCloud.callFunction("joinClass3", params);
            if(Config.SHOWLOG) Log.d("join", "class joining");
        } catch (ParseException e) {
            Utility.LogoutUtility.checkAndHandleInvalidSession(e);
            e.printStackTrace();

            if(e.getMessage().equals("No such class exits"))
                return 3;
            return 0;
        }


        ParseUser currentParseUser = ParseUser.getCurrentUser();
        if(currentParseUser == null){
            return 0;
        }

        String userId = currentParseUser.getUsername();

        if (result == null)
            return 0;

        ParseObject codeGroupObject = (ParseObject) result.get("codegroup");
        List<ParseObject> oldMessages = (List<ParseObject>) result.get("messages");
        List<List<String>> updatedJoinedGroups = (List<List<String>>) result.get(Constants.JOINED_GROUPS);

        if(codeGroupObject == null || updatedJoinedGroups == null){
            return 0;
        }

        if(Config.SHOWLOG) Log.d("join", "code object not null");
        //successfully joined the classroom
        codeGroupObject.put(Constants.USER_ID, userId);
        currentParseUser.put(Constants.JOINED_GROUPS, updatedJoinedGroups);
        try {
            currentParseUser.pin();
            codeGroupObject.pin();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        /*
         * download profile pic of teacher locally
        */
        String senderId = codeGroupObject.getString(Constants.Codegroup.SENDER_ID);
        ParseFile senderPic = codeGroupObject.getParseFile(Constants.Codegroup.SENDER_PIC);

        if (!UtilString.isBlank(senderId)) {
            senderId = senderId.replaceAll("@", "");
            String filePath =
                    Utility.getWorkingAppDir() + "/thumbnail/" + senderId + "_PC.jpg";
            final File senderThumbnailFile = new File(filePath);

            if (!senderThumbnailFile.exists()) {

                Queries2 imageQuery = new Queries2();

                if (senderPic != null)
                    imageQuery.downloadProfileImage(senderId, senderPic);
            } else {
                // Utility.toast("image already exist ");
            }
        }

        //locally generating joining notification and inbox msg
        if(Config.SHOWLOG) Log.d("DEBUG_JOINED_HELPER", "generating notification and local message");
        NotificationGenerator.generateNotification(Application.getAppContext(), utility.Config.welcomeMsg, codeGroupObject.getString(Constants.Codegroup.NAME), Constants.Notifications.NORMAL_NOTIFICATION, Constants.Actions.INBOX_ACTION);
        EventCheckerAlarmReceiver.generateLocalMessage(utility.Config.welcomeMsg, classcode, codeGroupObject.getString(Constants.Codegroup.CREATOR), codeGroupObject.getString(Constants.Codegroup.SENDER_ID), codeGroupObject.getString(Constants.Codegroup.NAME), currentParseUser);

        if(oldMessages != null)
        {
            for(int i=0 ; i<oldMessages.size(); i++)
            {
                ParseObject msg = oldMessages.get(i);
                msg.put(Constants.USER_ID, currentParseUser.getUsername());

                msg.put(Constants.GroupDetails.LIKE, false);
                msg.put(Constants.GroupDetails.CONFUSING, false);
                msg.put(Constants.GroupDetails.SYNCED_LIKE, false);
                msg.put(Constants.GroupDetails.SYNCED_CONFUSING, false);

                msg.put(Constants.GroupDetails.DIRTY_BIT, false);
                msg.put(Constants.GroupDetails.SEEN_STATUS, 0);
            }

            try {
                ParseObject.pinAll(oldMessages);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return 1;
    }
}
