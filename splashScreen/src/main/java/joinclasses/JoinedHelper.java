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

        Log.d("join", "class joining started current.........");
        Log.d("join", classcode);
        Log.d("join", childname);

        //setting parameters
        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("classCode", classcode);
        params.put("associateName", childname);
        params.put("installationObjectId", ParseInstallation.getCurrentInstallation().getString("id"));

        if(ParseInstallation.getCurrentInstallation() != null)
        {
            if(ParseInstallation.getCurrentInstallation().getString("id") != null)
                Log.d("JOIN", ParseInstallation.getCurrentInstallation().getString("id"));
            else {
                Log.d("JOIN", "object id : null ");

                Log.d("JOIN",ParseInstallation.getCurrentInstallation().getInstallationId());
            }
        }
        else
            Log.d("JOIN", "parseInstallation : null");

        ParseObject codeGroupObject = null;
        HashMap<String, Object> result = null;

        try {
            result = ParseCloud.callFunction("joinClass", params);
            Log.d("join", "class joining");
        } catch (ParseException e) {
            e.printStackTrace();

            if(e.getMessage().equals("No such class exits"))
                return 3;

            return 0;
        }

        ParseUser user = ParseUser.getCurrentUser();
        String userId = user.getUsername();

        if (result == null)
            return 0;
        else {

            codeGroupObject = (ParseObject) result.get("codegroup");
            List<ParseObject> oldMessages = (List<ParseObject>) result.get("messages");

            if(codeGroupObject != null) {

                //fetching parse user's joined group details
                try {
                    user.fetch();
                } catch (ParseException e) {
                    e.printStackTrace();
                }

                Log.d("join", "code object not null");
                //successfully joined the classroom
                codeGroupObject.put("userId", userId);
                try {
                    codeGroupObject.pin();
                } catch (ParseException e) {
                    e.printStackTrace();
                }


             /*
              * download profile pic of teacher locally
              */
                String senderId = codeGroupObject.getString("senderId");
                ParseFile senderPic = codeGroupObject.getParseFile("senderPic");

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
            }

            //locally generating joining notification and inbox msg
            Log.d("DEBUG_JOINED_HELPER", "generating notification and local message");
            NotificationGenerator.generateNotification(Application.getAppContext(), utility.Config.welcomeMsg, codeGroupObject.getString("name"), Constants.Notifications.NORMAL_NOTIFICATION, Constants.Actions.INBOX_ACTION);
            EventCheckerAlarmReceiver.generateLocalMessage(utility.Config.welcomeMsg, classcode, codeGroupObject.getString("Creator"), codeGroupObject.getString("senderId"), codeGroupObject.getString("name"), user);

            if(oldMessages != null)
            {
                for(int i=0 ; i<oldMessages.size(); i++)
                {
                    oldMessages.get(i).put("userId", ParseUser.getCurrentUser().getUsername());
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
}
