package utility;

import android.util.Log;

import com.parse.GetDataCallback;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseUser;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import BackGroundProcesses.UpdateAllClassSubscribers;
import library.UtilString;
import trumplabs.schoolapp.Application;
import trumplabs.schoolapp.Constants;

public class Queries2 {

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

    /*
     * fetch details(Codegroup objects) of all joined and created classes. Called only once after reinstallation
     * @how use giveClassesDetails cloud function
     */
    public static void fetchAllClassDetails(){
        ParseUser parseObject = ParseUser.getCurrentUser();

        if (parseObject == null)
        {
            Utility.LogoutUtility.logout(); return;}

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

            //Fetch subscriber list once and for all in the same thread user for fetching all class details
            Log.d("DEBUG_QUERIES_FETCH_ALL_CLASS_DETAILS", "fetching subscriber list of all created classes once");
            UpdateAllClassSubscribers.updateMembers();
        }
        catch (ParseException e){
            e.printStackTrace();
            Log.d("DEBUG_QUERIES_FETCH_ALL_CLASS_DETAILS", "Failed with exception");
        }
    }
}
