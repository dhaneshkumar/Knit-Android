package joinclasses;

import android.os.AsyncTask;
import android.util.Log;

import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.io.File;
import java.util.ArrayList;
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
     * @param defaultGroupFlag : true if joinClass is called from signup Class, else false
     * @How : first checks whether all group added or not, if not then it call parse cloud function and join that class
     *       fetch ParseUser’s joinedgroup
     *       locally download profile pic of sender in background
     *       send local msg to inbox
     *       send internal notification
     *       store all classroom suggestions <parse cloud function>
     *
     */
    public static int joinClass(String classcode, String childname, boolean defaultGroupFlag)  {

        //check class already added or not
        if(!defaultGroupFlag) {
            if(Queries.isJoinedClassExist(classcode))
                return 2;
        }

        Log.d("join", "class joining started current.........");
        Log.d("join", classcode);
        Log.d("join", childname);

        //setting parameters
        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("classCode", classcode);
        params.put("associateName", childname);
        params.put("installationObjectId", ParseInstallation.getCurrentInstallation().getObjectId());

        if(ParseInstallation.getCurrentInstallation() != null)
        {
            if(ParseInstallation.getCurrentInstallation().getObjectId() != null)
                Log.d("JOIN", ParseInstallation.getCurrentInstallation().getObjectId());
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

            Log.d("join", "class joining failed 0 --");
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

                Log.d("join", "code object not null");
                //successfully joined the classroom
                codeGroupObject.put("userId", userId);
                try {
                    codeGroupObject.pin();
                } catch (ParseException e) {
                    e.printStackTrace();
                }


                //fetching parse user's joined group details
                try {
                    user.fetch();
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



            if(!defaultGroupFlag) {
                //locally generating joining notification and inbox msg
                NotificationGenerator.generateNotification(Application.getAppContext(), utility.Config.welcomeMsg, Constants.DEFAULT_NAME, Constants.NORMAL_NOTIFICATION, Constants.INBOX_ACTION);
                EventCheckerAlarmReceiver.generateLocalMessage(utility.Config.welcomeMsg, classcode, codeGroupObject.getString("Creator"), codeGroupObject.getString("senderId"), codeGroupObject.getString("name"), user);
            }


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



    /**
     * @param userId
     * @return classrooms suggestion list
     * @how All codegroup classrooms - joined classrooms - removed list classrooms - created classrooms = suggestion list
     */
    public static List<ParseObject> getSuggestionList(String userId) {

        List<List<String>> joinedList = ParseUser.getCurrentUser().getList(Constants.JOINED_GROUPS);
        List<List<String>> removedList = ParseUser.getCurrentUser().getList(Constants.REMOVED_GROUPS);
        List<List<String>> createdList = ParseUser.getCurrentUser().getList(Constants.CREATED_GROUPS);

        ArrayList<String> ignoreList = new ArrayList<String>(); //these is list of classcode which are not be included in suggestions

        if(joinedList != null){
            for(int i=0; i<joinedList.size(); i++){
                ignoreList.add(joinedList.get(i).get(0));
            }
        }
        if(removedList != null){
            for(int i=0; i<removedList.size(); i++){
                ignoreList.add(removedList.get(i).get(0));
            }
        }
        if(createdList != null){
            for(int i=0; i<createdList.size(); i++){
                ignoreList.add(createdList.get(i).get(0));
            }
        }

        Log.d("DEBUG_GET_SUGGESTION", "ignore list size " + ignoreList.size());

        ParseQuery<ParseObject> query = ParseQuery.getQuery(Constants.CODE_GROUP);
        query.fromLocalDatastore();
        query.whereEqualTo("userId", userId);
        query.whereNotContainedIn("code", ignoreList);
        query.whereEqualTo("classExist", true); //get only classes which exist
        query.whereEqualTo(Constants.IS_SUGGESTION, true); //check for suggestion flag

        try {
            List<ParseObject> codeGroupList = query.find();

            if (codeGroupList != null) {
                //creating new list of suggestions
                List<ParseObject> finalSuggestionList = new ArrayList<ParseObject>();

                for (int i = 0; i < codeGroupList.size(); i++) {
                    ParseObject suggestion = codeGroupList.get(i);
                    String code = suggestion.getString("code");
                    String groupName = suggestion.getString("name");

                    if ((!UtilString.isBlank(code)) && (!UtilString.isBlank(groupName))) {
                        finalSuggestionList.add(suggestion);
                    }
                }

                Log.d("DEBUG_GET_SUGGESTION", "final suggestion list size = " + finalSuggestionList.size());
                return finalSuggestionList;
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        Log.d("DEBUG_GET_SUGGESTION", "something wrong happened. Returning empty list");
        return new ArrayList<ParseObject>();
    }


    /**
     * background class to update associated name
     */
    public static class UpdateAssociatedName extends AsyncTask<String, Void, Boolean>
    {

        @Override
        protected Boolean doInBackground(String... params) {

            String classcode = params[0];
            String childName = params[1];

            childName = childName.trim();
            childName = UtilString.parseString(childName);
            childName = UtilString.changeFirstToCaps(childName);    //changing first letter to caps

            //calling parse cloud function to update associated name
            HashMap<String, Object> param = new HashMap<String, Object>();
            param.put("childname", childName);
            param.put("classcode", classcode);

            boolean isNameUpdate = false;
            try {

                isNameUpdate = ParseCloud.callFunction("changeAssociateName", param);
            } catch (ParseException e) {
                e.printStackTrace();
            }


            if(isNameUpdate) {
                try {
                    ParseUser.getCurrentUser().fetch();
                } catch (ParseException e) {
                    e.printStackTrace();
                }

                return true;
            }

            return false;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {

           /* if(aBoolean)
            {
                Utility.toast("Udpated Associated Name");

                JoinedClasses.joinedGroups = ParseUser.getCurrentUser().getList("joined_groups");

                if (JoinedClasses.joinedadapter != null)
                    JoinedClasses.joinedadapter.notifyDataSetChanged();
            }
            else
                Utility.toast("Sorry, Couldn't Update Associated name");

            JoinedClasses.progressBarLayout.setVisibility(View.GONE);
            JoinedClasses.editProfileLayout.setVisibility(View.VISIBLE);*/

            super.onPostExecute(aBoolean);
        }
    }




}
