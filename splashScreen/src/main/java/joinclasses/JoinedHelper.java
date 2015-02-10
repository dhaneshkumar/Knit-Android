package joinclasses;

import android.graphics.Paint;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;

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
import notifications.AlarmReceiver;
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

        Log.d("join", "class joining started.........");
        Log.d("join", classcode);
        Log.d("join", childname);

        //setting parameters
        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("classcode", classcode);
        params.put("childname", childname);
        params.put("installationId", ParseInstallation.getCurrentInstallation().getObjectId());

        ParseObject codeGroupObject = null;
        try {
            codeGroupObject = ParseCloud.callFunction("joinnewclass2", params);


            Log.d("join", "class joining");
        } catch (ParseException e) {
            e.printStackTrace();

            Log.d("join", "class joining failed 0 --");
            return 0;
        }

        ParseUser user = ParseUser.getCurrentUser();
        String userId = user.getUsername();

        if (codeGroupObject == null)
            return 0;
        else {

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

            if(!UtilString.isBlank(senderId)) {
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



            if(!defaultGroupFlag) {
                //locally generating joining notification and inbox msg
                NotificationGenerator.generateNotification(Application.getAppContext(), utility.Config.welcomeMsg, Constants.DEFAULT_NAME, Constants.NORMAL_NOTIFICATION, Constants.INBOX_ACTION);
                AlarmReceiver.generateLocalMessage(utility.Config.welcomeMsg, classcode, codeGroupObject.getString("Creator"), codeGroupObject.getString("name"), user, Application.getAppContext());


                //fetch classroom suggestions from server
                String schoolId = codeGroupObject.getString("school");
                String standard = codeGroupObject.getString("standard");
                String division = codeGroupObject.getString("divison");

                School.storeSuggestions(schoolId, standard, division, userId);
            }

            return 1;
        }
    }



    /**
     * @param userId
     * @return classrooms suggestion list
     * @how All codegroup classrooms - joined classrooms - removed list classrooms - created classrooms = suggestion list
     */
    public static List<List<String>> getSuggestionList(String userId) {
        ParseQuery<ParseObject> query = ParseQuery.getQuery(Constants.CODE_GROUP);
        query.fromLocalDatastore();
        query.whereEqualTo("userId", userId);

        List<ParseObject> codeGroupList = null;
        try {
            codeGroupList = query.find();

            if (codeGroupList != null && codeGroupList.size() > 0) {
                List<List<String>> joinedList = ParseUser.getCurrentUser().getList(Constants.JOINED_GROUPS);
                List<List<String>> removedList = ParseUser.getCurrentUser().getList(Constants.REMOVED_GROUPS);
                List<List<String>> createdList = ParseUser.getCurrentUser().getList(Constants.CREATED_GROUPS);

                //removing joined list
                if (joinedList != null) {
                    for (int i = 0; i < joinedList.size(); i++) {
                        for (int j = 0; j < codeGroupList.size(); j++) {
                            String code = codeGroupList.get(j).getString("code");

                            if (!UtilString.isBlank(code)) {
                                if (code.trim().equals(joinedList.get(i).get(0).trim()))
                                    codeGroupList.remove(j);

                            }
                        }

                    }
                }

                //removing removedlist classrooms
                if (removedList != null) {
                    for (int i = 0; i < removedList.size(); i++) {
                        for (int j = 0; j < codeGroupList.size(); j++) {
                            String code = codeGroupList.get(j).getString("code");
                            if (!UtilString.isBlank(code)) {
                                if (code.trim().equals(removedList.get(i).get(0).trim()))
                                    codeGroupList.remove(j);
                            }
                        }

                    }
                }

                //removing createdlist classrooms
                if (createdList != null) {
                    for (int i = 0; i < createdList.size(); i++) {
                        for (int j = 0; j < codeGroupList.size(); j++) {
                            String code = codeGroupList.get(j).getString("code");
                            if (!UtilString.isBlank(code)) {
                                if (code.trim().equals(createdList.get(i).get(0).trim()))
                                    codeGroupList.remove(j);
                            }
                        }

                    }
                }


                //creating new list of suggestions
                List<List<String>> suggestionList = new ArrayList<List<String>>();

                for (int i = 0; i < codeGroupList.size(); i++) {
                    List<String> suggestion = new ArrayList<String>();

                    String code = codeGroupList.get(i).getString("code");
                    String groupName = codeGroupList.get(i).getString("name");

                    if ((!UtilString.isBlank(code)) && (!UtilString.isBlank(groupName))) {
                        suggestion.add(code);
                        suggestion.add(groupName);
                        suggestionList.add(suggestion);
                    }
                }


                return suggestionList;
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return null;
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

            if(aBoolean)
            {
                Utility.toast("Udpated Associated Name");

                JoinedClasses.joinedGroups = ParseUser.getCurrentUser().getList("joined_groups");

                if (JoinedClasses.joinedadapter != null)
                    JoinedClasses.joinedadapter.notifyDataSetChanged();
            }
            else
                Utility.toast("Sorry, Couldn't Update Associated name");

            JoinedClasses.progressBarLayout.setVisibility(View.GONE);
            JoinedClasses.editProfileLayout.setVisibility(View.VISIBLE);

            super.onPostExecute(aBoolean);
        }
    }




}
