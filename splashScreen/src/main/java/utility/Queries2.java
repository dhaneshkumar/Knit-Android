package utility;

import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import library.UtilString;
import trumplabs.schoolapp.ClassMembers;
import trumplabs.schoolapp.Classrooms;
import trumplabs.schoolapp.Constants;
import trumplabs.schoolapp.Messages;

import com.parse.GetCallback;
import com.parse.GetDataCallback;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

public class Queries2 {

    /**
     * Tell whether given class exist locally or not
     * @param code
     * @param userId
     * @return
     */
    public boolean isCodegroupExist(String code, String userId) {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Codegroup");
        query.fromLocalDatastore();
        query.whereEqualTo("code", code);
        query.whereEqualTo("userId", userId);

        Utility.ls(code + " : " + userId + " doesn't exist");
        ParseObject obj;
        try {
            obj = query.getFirst();

            if (obj != null) {
                return true;
            }

        } catch (ParseException e) {

            e.printStackTrace();
            return false;
        }

        return false;
    }

    /**
     * Update profile image of teacher
     * @param code
     * @param userId
     * @throws ParseException
     */
    public void updateProfileImage(String code, String userId) throws ParseException {
    /*
     * fetching updated codegroup entry from server
     */
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Codegroup");
        query.fromLocalDatastore();
        query.whereEqualTo("code", code);
        query.whereEqualTo("userId", userId);


        Log.d("JOIN", "old pic,,,,ppp ");

        ParseObject obj = query.getFirst();
        if (obj != null) {

            String oldPic = obj.getString("picName");

            obj.fetch();    //fetching from server

            //retrieving server pic name
            String newPic = obj.getString("picName");
            String senderId = obj.getString("senderId");
            senderId = senderId.replaceAll("@", "");
            ParseFile senderPic = obj.getParseFile("senderPic");
            Log.d("JOIN", "old pic : " + oldPic);
            Log.d("JOIN", "old pic : ---");

            if (UtilString.isBlank(oldPic)) {

                //no image locally then download it
                if(!UtilString.isBlank(newPic))
                    downloadProfileImage(senderId, senderPic);

                Log.d("JOIN", "newpic : " + newPic);

            } else if ((!UtilString.isBlank(oldPic)) && (!UtilString.isBlank(newPic))) {

                Log.d("JOIN", "old pic : " + oldPic);
                Log.d("JOIN", "new pic : " + newPic);

                if (!oldPic.equals(newPic)) {

                    downloadProfileImage(senderId, senderPic);
                } else {
                    final File senderThumbnailFile =
                            new File(Utility.getWorkingAppDir() + "/thumbnail/" + senderId + "_PC.jpg");
                    if (!senderThumbnailFile.exists()) {

                        downloadProfileImage(senderId, senderPic);
                    }

                }

            } else {
                final File senderThumbnailFile =
                        new File(Utility.getWorkingAppDir() + "/thumbnail/" + senderId + "_PC.jpg");
                if (!senderThumbnailFile.exists()) {

                    downloadProfileImage(senderId, senderPic);
                }
            }

        }
        else
        {
            Log.d("JOIN", "obj... null ");
        }


    }

    /**
     * Downloading image from server and storing it locally
     * @param senderId
     * @param senderImagefile
     */
    public static void downloadProfileImage(final String senderId, ParseFile senderImagefile) {

        if (senderImagefile != null && (!UtilString.isBlank(senderId))) {
            senderImagefile.getDataInBackground(new GetDataCallback() {
                public void done(byte[] data, ParseException e) {
                    if (e == null) {
                        // ////Image download successful
                        FileOutputStream fos;
                        try {
                            fos =
                                    new FileOutputStream(Utility.getWorkingAppDir() + "/thumbnail/" + senderId
                                            + "_PC.jpg");
                            try {
                                fos.write(data);
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            } finally {
                                try {
                                    fos.close();
                                } catch (IOException e1) {
                                    e1.printStackTrace();
                                }
                            }

                        } catch (FileNotFoundException e2) {
                            e2.printStackTrace();
                        }

                        // Might be a problem when net is too slow :/
                        System.out.println("Profile Image Downloaded"); // ************************************
                    } else {
                        // Image not downloaded
                        System.out.println("Profile Image not Downloaded"); // **********************************
                    }
                }
            });
        }


    }

    /**
     * Locally storing codegroup entry corresponding to given class-code
     * @param code
     * @param userId
     */
    public void storeCodegroup(String code, String userId) {

        //setting parameters
        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("classcode", code);

        ParseObject codeGroupObject = null;

        //calling parse cloud function to create class
        try {
            codeGroupObject = ParseCloud.callFunction("getCodegroup", params);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        if (codeGroupObject != null)
        {
            codeGroupObject.put("userId", userId);
            try {
                codeGroupObject.pin();
            } catch (ParseException e) {
                e.printStackTrace();
            }

            //Downloading profile pic of teacher
            String senderId = codeGroupObject.getString("senderId");
            senderId = senderId.replaceAll("@", "");

            downloadProfileImage(senderId, codeGroupObject.getParseFile("senderPic"));
        }
    }


    public boolean isGroupMemberExist(String code, String userId) throws ParseException {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("GroupMembers");
        query.fromLocalDatastore();
        query.whereEqualTo("code", code);
        query.whereEqualTo("userId", userId);
        query.whereEqualTo("emailId", userId);
        ParseObject obj = query.getFirst();

        if (obj != null) {
            return true;
        }

        return false;
    }


    public static void storeGroupMember(String code, String userId, boolean adapterFlag)
            throws ParseException {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("GroupMembers");
        query.whereEqualTo("code", code);
        query.whereEqualTo("emailId", userId);

        ParseObject obj = query.getFirst();

        if (obj != null) {
            if (obj.getCreatedAt() != null) {


                ParseObject joinedObj = new ParseObject("JoinedTiming");
                joinedObj.put("objectId", obj.getObjectId());
                joinedObj.put("code", code);
                joinedObj.put("joiningTime", obj.getCreatedAt());
                joinedObj.put("userId", userId);

                joinedObj.pin();

                //   Utility.ls(obj.getCreatedAt() + "joining time");

                if (adapterFlag) {
                    if (ClassMembers.myadapter != null)
                        ClassMembers.myadapter.notifyDataSetChanged();

                    if (Classrooms.myadapter != null)
                        Classrooms.myadapter.notifyDataSetChanged();
                }

            }
        } else {
            Utility.ls("null object");
        }
    }


    public static Date getGroupJoinedTime(String code, String userId) throws ParseException {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("JoinedTiming");
        query.fromLocalDatastore();

        query.whereEqualTo("code", code.trim());
        query.whereEqualTo("userId", userId.trim());

        ParseObject obj = query.getFirst();

        if (obj != null)
            return (Date) obj.get("joiningTime");

        return null;
    }


    public boolean isItemExist(List<ParseObject> groupDetails, ParseObject item) {
        if (groupDetails == null)
            return false;

        String itemTitle = item.getString("title");
        Date itemDate = item.getCreatedAt();

        for (int i = 0; i < groupDetails.size(); i++) {
            String title = groupDetails.get(i).getString("title");
            Date date = groupDetails.get(i).getCreatedAt();

            if (!UtilString.isBlank(itemTitle)) {
                if (title.trim().equals(itemTitle.trim()) && date == itemDate)
                    return true;
            }
        }

        return false;
    }

}
