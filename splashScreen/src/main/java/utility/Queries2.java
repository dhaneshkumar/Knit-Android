package utility;

import android.util.Log;

import com.parse.GetDataCallback;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import library.UtilString;
import trumplabs.schoolapp.Application;
import trumplabs.schoolapp.Classrooms;
import trumplabs.schoolapp.Constants;

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

    /*
     * fetch details(Codegroup objects) of all joined and created classes. Called only once after reinstallation
     * @how use giveClassesDetails cloud function
     */
    public static void fetchAllClassDetails(){
        ParseUser parseObject = ParseUser.getCurrentUser();

        if (parseObject == null)
        {Utility.logout(); return;}

        String userId = parseObject.getUsername();

        HashMap<String, String> parameters = new HashMap<String, String>();
        try{
            List<ParseObject> codegroupEntries = ParseCloud.callFunction("giveClassesDetails", parameters);
            if(codegroupEntries != null){
                for(int i=0; i<codegroupEntries.size(); i++){
                    ParseObject codegroup = codegroupEntries.get(i);
                    codegroup.put(Constants.USER_ID, userId);
                }
            }
            ParseObject.pinAll(codegroupEntries);
            final SessionManager sm = new SessionManager(Application.getAppContext());
            sm.setCodegroupLocalState(1, userId); //set the flag locally that outbox data is valid
            Log.d("DEBUG_QUERIES_FETCH_ALL_CLASS_DETAILS", "Pinned all. State changed to 1");
        }
        catch (ParseException e){
            e.printStackTrace();
            Log.d("DEBUG_QUERIES_FETCH_ALL_CLASS_DETAILS", "Failed with exception");
        }
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
