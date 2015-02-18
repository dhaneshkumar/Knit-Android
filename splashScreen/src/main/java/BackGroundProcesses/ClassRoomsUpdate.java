package BackGroundProcesses;

import android.util.Log;

import com.parse.GetDataCallback;
import com.parse.Parse;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import library.UtilString;
import utility.Utility;

/**
 * Created by ashish on 14/2/15.
 *
 * update the name and profile pic associated with joined classrooms. Called periodically.
 * It also updates the name in Codegroup table as we use Codegroup table to show joined classes.
 */
public class ClassRoomsUpdate {
    public static void fetchUpdates(){
        ParseUser user = ParseUser.getCurrentUser();
        if(user != null){
            ParseQuery joinedQuery = new ParseQuery("Codegroup");
            joinedQuery.fromLocalDatastore();
            joinedQuery.whereEqualTo("userId", user.getUsername());

            try{
                List<ParseObject> joinedGroups = joinedQuery.find();

                ArrayList<String> joinedCodes = new ArrayList<>();
                for(int i=0; i<joinedGroups.size(); i++){
                    joinedCodes.add(joinedGroups.get(i).getString("senderId"));
                }

                HashMap<String, Object> parameters = new HashMap<String, Object>();
                parameters.put("joinedObjectIds", joinedCodes);

                try{
                    List<Map<String, Object>>  resultUsers = ParseCloud.callFunction("getUpdatesUserDetail", parameters);
                    Log.d("DEBUG_CLASS_ROOMS_UPDATE", "fetchUpdates() : result Users size " + resultUsers.size());

                    //now iterate in the list and update the User table and put dirty mark for those whose profile pic has changed
                    //Also update Codegroup table if name has changed

                    for (int u=0; u<resultUsers.size(); u++){
                        Map<String, Object> userInfo = resultUsers.get(u);
                        updateUser(user.getUsername(), userInfo);
                    }
                }
                catch (ParseException e1){
                    e1.printStackTrace();
                }
            }
            catch (ParseException e){
                e.printStackTrace();
            }
        }
    }

    /*
        updates the user table by taking userInfo.
     */
    public static void updateUser(String currentUserName, Map<String, Object> userInfo){
        String username = (String) userInfo.get("username");
        ParseFile newPid = (ParseFile) userInfo.get("pid");
        String newName = (String) userInfo.get("name");

        ParseQuery userQuery = new ParseQuery("User");
        userQuery.fromLocalDatastore();
        userQuery.whereEqualTo("username", username);
        userQuery.whereEqualTo("userId", currentUserName); //associated with current logged in user

        try{
            List<ParseObject> userObjects = userQuery.find();
            boolean nameChanged = false; //flag to use whether require change name in Codegroup table
            if(userObjects == null){
                return;
            }
            if(userObjects.size() == 0){
                nameChanged = true;
                //create a new User object and pin with dirty true
                ParseObject newUser = new ParseObject("User");
                newUser.put("username", username);
                if(newPid != null){
                    newUser.put("pid", newPid);
                    newUser.put("dirty", true);
                }
                else{
                    newUser.put("dirty", false);
                }
                Log.d("DEBUG_CLASS_ROOMS_UPDATE", "updateUser() : creating a new User object with dirty " +
                        Boolean.toString(newUser.getBoolean("dirty")));

                newUser.put("name", newName);
                newUser.put("userId", currentUserName);
                newUser.pinInBackground();
            }
            else{
                ParseObject oldUser = userObjects.get(0);
                boolean changed = false; //flag telling whether some info has changed. Used whether to pin or not
                if(!oldUser.getString("name").equals(newName)){
                    //update the name here

                    Log.d("DEBUG_CLASS_ROOMS_UPDATE", "updateUser() : updating user's name to " + newName);
                    oldUser.put("name", newName);
                    changed = true;
                    nameChanged = true;
                }

                if(newPid != null){
                    if(oldUser.getParseFile("pid") == null || !oldUser.getParseFile("pid").getName().equals(newPid.getName())){//if existing is null or has different name from newPid
                        Log.d("DEBUG_CLASS_ROOMS_UPDATE", "updateUser() : updating user's pic to " + newPid.getName());
                        oldUser.put("pid", newPid);
                        oldUser.put("dirty", true); //has become dirty. i.e need to fetch updated profile pic
                        changed = true;
                    }
                }

                if(changed){
                    oldUser.pin();
                }
            }

            if(nameChanged) {//query Codegroup and change "Creator" in Codegroup table(where senderId = username)
                Log.d("DEBUG_CLASS_ROOMS_UPDATE", "updateUser() : updating Codegroup table for senderId = " + username);
                ParseQuery classQuery = new ParseQuery("Codegroup");
                classQuery.fromLocalDatastore();
                classQuery.whereEqualTo("senderId", username);
                classQuery.whereEqualTo("userId", currentUserName);

                List<ParseObject> classList = classQuery.find();
                if (classList != null) {
                    for (int i = 0; i < classList.size(); i++) {
                        ParseObject oldClass = classList.get(i);
                        oldClass.put("Creator", newName);
                    }
                    ParseObject.pinAll(classList);
                }
            }
        }
        catch (ParseException e){
            e.printStackTrace();
        }
    }

    /*
        fetch new profile pics for dirty marked Users
     */
    public static void fetchProfilePics(String currentUserName){
        ParseQuery dirtyUserQuery = new ParseQuery("User");
        dirtyUserQuery.fromLocalDatastore();
        dirtyUserQuery.whereEqualTo("dirty", true);
        dirtyUserQuery.whereEqualTo("userId", currentUserName);

        try{
            List<ParseObject> dirtyUsers = dirtyUserQuery.find();
            Log.d("DEBUG_CLASS_ROOMS_UPDATE", "fetchProfilePics() : dirty user count " + dirtyUsers.size());

            for(int i=0; i<dirtyUsers.size(); i++){
                ParseObject dirtyUser = dirtyUsers.get(i);
                downloadProfileImage(dirtyUser);
            }
        }
        catch (ParseException e){
            e.printStackTrace();
        }
    }


    public static void downloadProfileImage(final ParseObject dirtyUser) {
        final String senderId = dirtyUser.getString("username");
        final ParseFile senderImagefile = dirtyUser.getParseFile("pid");

        Log.d("DEBUG_CLASS_ROOMS_UPDATE", "downloadProfileImage() : start downloading pic for " + senderId);

        if (senderImagefile != null && (!UtilString.isBlank(senderId))) {
            senderImagefile.getDataInBackground(new GetDataCallback() {
                public void done(byte[] data, ParseException e) {
                    if (e == null) {
                        //Image download successful
                        FileOutputStream fos;
                        try {
                            fos =
                                    new FileOutputStream(Utility.getWorkingAppDir() + "/thumbnail/" + senderId
                                            + "_PC.jpg");
                            try {
                                fos.write(data);

                                //download and save is success. Set dirty bit to false and pin
                                dirtyUser.put("dirty", false);
                                dirtyUser.pin();
                                Log.d("DEBUG_CLASS_ROOMS_UPDATE", "downloadProfileImage() : profile pic download and save successful " + senderId);
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            } catch (ParseException e1) {
                                e1.printStackTrace();
                            }finally{
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
}