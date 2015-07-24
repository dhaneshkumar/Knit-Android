package BackGroundProcesses;

import android.util.Log;

import com.parse.GetDataCallback;
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
import trumplabs.schoolapp.Application;
import trumplabs.schoolapp.Classrooms;
import trumplabs.schoolapp.Constants;
import utility.SessionManager;
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
            SessionManager sm = new SessionManager(Application.getAppContext());
            //check if Codegroup data has been fetched or not yet. If not just return
            if(sm.getCodegroupLocalState(user.getUsername()) == 0){
                Log.d("DEBUG_CLASS_ROOMS_UPDATE", "Codegroup data not yet availabe locally. So returning");
                return;
            }

            List<List<String>> joinedClasses = Classrooms.getJoinedGroups(user); //won't be null
            if(joinedClasses.size() == 0){
                Log.d("DEBUG_CLASS_ROOMS_UPDATE", "joined_group size is 0");
                return; //We're done. No joined groups
            }

            Log.d("DEBUG_CLASS_ROOMS_UPDATE", "joined_group size is " + joinedClasses.size());

            ArrayList<String> joinedClassCodes = new ArrayList<String>();
            for(int i=0; i<joinedClasses.size(); i++){
                joinedClassCodes.add(joinedClasses.get(i).get(0));
            }

            ParseQuery joinedQuery = new ParseQuery(Constants.CODE_GROUP);
            joinedQuery.fromLocalDatastore();
            joinedQuery.whereEqualTo(Constants.USER_ID, user.getUsername());
            joinedQuery.whereContainedIn("code", joinedClassCodes);

            try{
                List<ParseObject> joinedGroups = joinedQuery.find();
                if(joinedGroups == null || joinedGroups.size() == 0){
                    Log.d("DEBUG_CLASS_ROOMS_UPDATE", "Zero code group size");
                    return;
                }

                ArrayList<String> joinedSenderIds = new ArrayList<>();
                for(int i=0; i<joinedGroups.size(); i++){
                    joinedSenderIds.add(joinedGroups.get(i).getString("senderId"));
                    //Log.d("DEBUG_CLASS_ROOMS_UPDATE", i + joinedGroups.get(i).getString("senderId"));
                }

                HashMap<String, Object> parameters = new HashMap<String, Object>();
                parameters.put("joinedObjectIds", joinedSenderIds);

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
                    Log.d("DEBUG_CLASS_ROOMS_UPDATE", "getUpdatesUserDetails() failed");
                    e1.printStackTrace();
                }
            }
            catch (ParseException e){
                Log.d("DEBUG_CLASS_ROOMS_UPDATE", "local Codegroup query failed");
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

        ParseQuery userQuery = new ParseQuery(Constants.UserTable.TABLE);
        userQuery.fromLocalDatastore();
        userQuery.whereEqualTo(Constants.UserTable.USERNAME, username);
        userQuery.whereEqualTo(Constants.USER_ID, currentUserName); //associated with current logged in user

        try{
            List<ParseObject> userObjects = userQuery.find();
            boolean nameChanged = false; //flag to use whether require change name in Codegroup table
            if(userObjects == null){
                return;
            }
            if(userObjects.size() == 0){
                nameChanged = true;
                //create a new User object and pin with dirty true
                ParseObject newUser = new ParseObject(Constants.UserTable.TABLE);
                newUser.put(Constants.UserTable.USERNAME, username);
                if(newPid != null){
                    newUser.put(Constants.UserTable.PID, newPid);
                    newUser.put(Constants.UserTable.DIRTY, true);
                }
                else{
                    newUser.put(Constants.UserTable.DIRTY, false);
                }
                Log.d("DEBUG_CLASS_ROOMS_UPDATE", "updateUser() : creating a new User object with dirty " +
                        Boolean.toString(newUser.getBoolean(Constants.UserTable.DIRTY)));

                newUser.put(Constants.UserTable.NAME, newName);
                newUser.put(Constants.USER_ID, currentUserName);
                newUser.pinInBackground();
            }
            else{
                ParseObject oldUser = userObjects.get(0);
                boolean changed = false; //flag telling whether some info has changed. Used whether to pin or not
                if(!oldUser.getString(Constants.UserTable.NAME).equals(newName)){
                    //update the name here

                    Log.d("DEBUG_CLASS_ROOMS_UPDATE", "updateUser() : updating user's name to " + newName);
                    oldUser.put(Constants.UserTable.NAME, newName);
                    changed = true;
                    nameChanged = true;
                }

                if(newPid != null){
                    if(oldUser.getParseFile(Constants.UserTable.PID) == null || !oldUser.getParseFile(Constants.UserTable.PID).getName().equals(newPid.getName())){//if existing is null or has different name from newPid
                        Log.d("DEBUG_CLASS_ROOMS_UPDATE", "updateUser() : updating user's pic to " + newPid.getName());
                        oldUser.put(Constants.UserTable.PID, newPid);
                        oldUser.put(Constants.UserTable.DIRTY, true); //has become dirty. i.e need to fetch updated profile pic
                        changed = true;
                    }
                }

                if(changed){
                    oldUser.pin();
                }
            }

            if(nameChanged) {//query Codegroup and change "Creator" in Codegroup table(where senderId = username)
                Log.d("DEBUG_CLASS_ROOMS_UPDATE", "updateUser() : updating Codegroup table for senderId = " + username);
                ParseQuery classQuery = new ParseQuery(Constants.CODE_GROUP);
                classQuery.fromLocalDatastore();
                classQuery.whereEqualTo("senderId", username);
                classQuery.whereEqualTo(Constants.USER_ID, currentUserName);

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
        ParseQuery dirtyUserQuery = new ParseQuery(Constants.UserTable.TABLE);
        dirtyUserQuery.fromLocalDatastore();
        dirtyUserQuery.whereEqualTo(Constants.UserTable.DIRTY, true);
        dirtyUserQuery.whereEqualTo(Constants.USER_ID, currentUserName);

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
        String senderId = dirtyUser.getString(Constants.UserTable.USERNAME);
        final ParseFile senderImagefile = dirtyUser.getParseFile(Constants.UserTable.PID);

        Log.d("DEBUG_CLASS_ROOMS_UPDATE", "downloadProfileImage() : start downloading pic for " + senderId);

        if (senderImagefile != null && (!UtilString.isBlank(senderId))) {
            final String senderIdTrimmed = senderId.replaceAll("@", "");
            senderImagefile.getDataInBackground(new GetDataCallback() {
                public void done(byte[] data, ParseException e) {
                    if (e == null) {
                        //Image download successful
                        FileOutputStream fos;
                        try {

                            String filePath = Utility.getWorkingAppDir() + "/thumbnail/" + senderIdTrimmed + "_PC.jpg";
                            fos = new FileOutputStream(filePath);
                            try {
                                fos.write(data);

                                //download and save is success. Set dirty bit to false and pin
                                dirtyUser.put(Constants.UserTable.DIRTY, false);
                                dirtyUser.pin();
                                Log.d("DEBUG_CLASS_ROOMS_UPDATE", "downloadProfileImage() : profile pic download and save successful in " + filePath);
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
                    } else {
                        // Image not downloaded
                    }
                }
            });
        }
        else{
            //since it has no valid image parse file, just set dirty to false
            dirtyUser.put(Constants.UserTable.DIRTY, false);
            try{
                dirtyUser.pin();
            }
            catch (ParseException e){
                //e.printStackTrace();
            }
        }
    }
}