package utility;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.media.ThumbnailUtils;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.util.Patterns;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.FunctionCallback;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParseUser;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;
import java.util.regex.Pattern;

import baseclasses.MyActionBarActivity;
import library.UtilString;
import loginpages.Signup;
import notifications.AlarmTrigger;
import profileDetails.ProfilePage;
import trumplabs.schoolapp.Application;
import trumplabs.schoolapp.MainActivity;

public class Utility extends MyActionBarActivity {

    static String appName = "knit";

    public static void ls(String str) {
        if (!UtilString.isBlank(str))
            System.out.println(str);
    }

    /*
        check if local parseinstallation has "id" field set which implies that cloud database
        has an entry corresponding to it(this id is the object id in cloud database).
        If not, use "appInstallation" cloud function to create an entry
        on cloud.
     */
    public static boolean checkParseInstallation(){
        ParseInstallation parseInstallation = ParseInstallation.getCurrentInstallation();

        if(parseInstallation == null){ //this should never ever happen
            Log.d("DEBUG_UTILITY", "checkParseInstallation : getCurrentInstallation() returned null");
            return false;
        }

        if(parseInstallation.getString("id") != null){  //this single handedly means that installation is all set.
                                                        // This is cleared during singup/signin to
                                                        // prevent use of stale ids
            return true; //we're done
        }

        //Since neither id nor objectId set, call the cloud function.
        Log.d("DEBUG_UTILITY", "checkParseInstalltion : calling appInstallation cloud function");
        HashMap<String, Object> param = new HashMap<String, Object>();
        param.put("deviceType", "android");
        param.put("installationId", parseInstallation.getInstallationId());

        try{
            String id = ParseCloud.callFunction("appInstallation", param);
            parseInstallation.put("id", id);
            parseInstallation.pin();
            Log.d("DEBUG_UTILITY", "checkParseInstalltion : success cloud function with id " + id);
            return true;
        }
        catch (com.parse.ParseException e){
            Log.d("DEBUG_UTILITY", "checkParseInstallation : failure cloud function");
            e.printStackTrace();
            return false;
        }
    }

    //default called from everywhere(when ParseUser null) except ProfilePage
    public static void logout() {
        LogoutTask.resetLocalData();
        if(MainActivity.viewpager != null){
            MainActivity.viewpager.post(new Runnable() {
                @Override
                public void run() {
                    startLoginActivity(); //called from UI thread
                }
            });
        }
    }

    //called from ProfilePage logout option
    public static void logoutProfilePage(){
        LogoutTask logoutTask = new LogoutTask();
        logoutTask.execute();
    }

    //call from UI thread only
    static void startLoginActivity(){
        Intent i = new Intent(Application.getAppContext(), Signup.class);
        // Closing all the Activities
        // Add new Flag to start new Activity
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        Application.getAppContext().startActivity(i);
    }

    public static class LogoutTask extends AsyncTask<Void, Void, Void> {
        boolean success; //for static classes, explicitly initialize the variables
        LogoutTask(){
            success = false;
            if(ProfilePage.profileLayout!= null){
                ProfilePage.profileLayout.setVisibility(View.GONE);
                ProfilePage.progressBarLayout.setVisibility(View.VISIBLE);
            }
        }

        //called in general (and also when currentUser is null)
        //can be called in a thread
        static void resetLocalData(){
            Context _context = Application.getAppContext();
            // After logout redirect user to Loing Activity

            Application.lastTimeJoinedSync = null;
            Application.lastTimeInboxSync = null;
            Application.lastTimeOutboxSync = null;

            //cancel all alarms set. Very first thing to do
            AlarmTrigger.cancelEventCheckerAlarm(_context);
            AlarmTrigger.cancelRefresherAlarm(_context);

            SessionManager session = new SessionManager(_context);
            session.reSetAppOpeningCount();
            session.reSetSignUpAccount();
        }

        @Override
        protected Void doInBackground(Void... params) {
            ParseInstallation pi = ParseInstallation.getCurrentInstallation();

            HashMap<String, Object> param = new HashMap<>();
            param.put("installationId", pi.getInstallationId());

            try{
                Log.d("DEBUG_UTILITY", "logout() - appLogout before calling");
                boolean logoutSuccess = ParseCloud.callFunction("appExit", param);
                Log.d("DEBUG_UTILITY", "logout() - appLogout returned with" + logoutSuccess);
                ParseUser.logOut();
                Log.d("DEBUG_UTILITY", "logout() - appLogout ParseUser.logOut() over");
                resetLocalData();
                Log.d("DEBUG_UTILITY", "logout() - appLogout resetLocalData() over");
                success = true;
                return null;
            }
            catch (com.parse.ParseException e){
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result){
            //UI thread
            if(success) {
                Log.d("DEBUG_UTILITY", "logout() : launching Signup activity = " + success);
                startLoginActivity();
            }
            else{
                //show profile page again
                if(ProfilePage.profileLayout!= null){
                    ProfilePage.profileLayout.setVisibility(View.VISIBLE);
                    ProfilePage.progressBarLayout.setVisibility(View.GONE);
                    if(!success){
                        Utility.toast("Unable to logout !");
                    }
                }
            }
        }
    }



    public static void toast(String str) {
        LinearLayout layout = new LinearLayout(Application.getAppContext());
        layout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        layout.setBackgroundColor(Color.parseColor("#FBB51E"));

        WindowManager wm =
                (WindowManager) Application.getAppContext().getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();

        // finding width of screen
        int currentapiVersion = android.os.Build.VERSION.SDK_INT;
        int width;
        int height;
        if (currentapiVersion >= android.os.Build.VERSION_CODES.HONEYCOMB) {
            Point size = new Point();
            display.getSize(size);
            width = size.x;
            height = size.y;
        } else {
            width = display.getWidth();
            height = display.getHeight();
        }

        TextView tv = new TextView(Application.getAppContext());
        // set the TextView properties like color, size etc
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(17);
        tv.setPadding(50, 20, 50, 20);
        tv.setLayoutParams(new LayoutParams(width, height/8));
        tv.setGravity(Gravity.CENTER);

        // set the text you want to show in Toast
        tv.setText(str);
        layout.addView(tv);

        Toast toast = new Toast(Application.getAppContext()); // context is object of Context write
        // Set The layout as Toast View
        toast.setView(layout);
        toast.setGravity(Gravity.TOP,0,0);
        toast.show();
    }


    public static void copyToClipBoard(Context activity, String label, String content) {

        if(UtilString.isBlank(label)  || UtilString.isBlank(content))
            return;
        else {

            int currentapiVersion = android.os.Build.VERSION.SDK_INT;
            if (currentapiVersion >= android.os.Build.VERSION_CODES.HONEYCOMB) {
                android.content.ClipboardManager clipboard =
                        (android.content.ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText(label, content);
                clipboard.setPrimaryClip(clip);
            } else {
                android.text.ClipboardManager clipboard =
                        (android.text.ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                clipboard.setText(content);
            }

            Utility.toast(label.trim() + " copied");
        }
    }

    public static String convertTimeStamp(Date d1){


        String result;

        if (d1 != null) {
            SessionManager sm = new SessionManager(Application.getAppContext());

            Date d2 = sm.getCurrentTime();

            if (d2 == null)
                return "just now";

            long diff = d2.getTime() - d1.getTime();

            long diffMinutes = diff / (60 * 1000) % 60;
            long diffHours = diff / (60 * 60 * 1000) % 24;
            long diffDays = diff / (24 * 60 * 60 * 1000);

            if (diffDays != 0) {
                result = diffDays + " days ago";
            } else if (diffHours != 0) {
                result = diffHours + " hrs ago";
            } else if (diffMinutes != 0) {
                result = diffMinutes + " mins ago";

                if (diffMinutes < 3)
                    result = "just now";
            } else {
                result = "just now";
                // result = diffSeconds + "secs";
            }

            return result;
        }

        return "just now";
    }

    /**
     * Checks for internet connection but doesn't show any popup
     * @return
     */
    public static final boolean isInternetExistWithoutPopup() {
        ConnectivityManager connec =
                (ConnectivityManager) Application.getAppContext().getSystemService(
                        Context.CONNECTIVITY_SERVICE);

        // are we connected to the net
        if (connec.getNetworkInfo(0).getState() == NetworkInfo.State.CONNECTED || connec.getNetworkInfo(1).getState() == NetworkInfo.State.CONNECTED)
            return true;
        else if (connec.getNetworkInfo(0).getState() == NetworkInfo.State.DISCONNECTED || connec.getNetworkInfo(1).getState() == NetworkInfo.State.DISCONNECTED)
            return false;

        return false;
    }



    public static boolean isInternetExist() {
        ConnectivityManager connec =
                (ConnectivityManager) Application.getAppContext().getSystemService(
                        Context.CONNECTIVITY_SERVICE);
        // ARE WE CONNECTED TO THE NET
        if (connec.getNetworkInfo(0).getState() == NetworkInfo.State.CONNECTED
                || connec.getNetworkInfo(1).getState() == NetworkInfo.State.CONNECTED) {

            return true;
        } else if (connec.getNetworkInfo(0).getState() == NetworkInfo.State.DISCONNECTED
                || connec.getNetworkInfo(1).getState() == NetworkInfo.State.DISCONNECTED) {

            toast("No Internet Connection");
            return false;
        }

        return false;
    }

    public static void savePicInAppFolder(String filepath) {

        if (filepath == null) {
            Utility.toast("path null");
            return;
        }

        String fname = filepath.substring(filepath.lastIndexOf("/") + 1);
        String targetPath = Utility.getWorkingAppDir() + "/media/" + fname;

        Log.d("DEBUG_UTILITY", "savePicInAppFolder calling for a gallery image");
        ScalingUtilities.scaleAndSave(filepath, targetPath);
    }

    // Creating the thumbnail of the image in media folder
    public static void createThumbnail(Activity getactivity, String fname) {
        Display mDisplay = getactivity.getWindowManager().getDefaultDisplay();
        float width = mDisplay.getWidth();
        float height = mDisplay.getHeight();
        float imgframedimen;
        if (width < height)
            imgframedimen = width;
        else
            imgframedimen = height;
        Bitmap b = null;

        try {
            b = ThumbnailUtils.extractThumbnail(
                    BitmapFactory.decodeFile(getWorkingAppDir() + "/media/" + fname),
                            (int) Math.round(imgframedimen), (int) Math.round(imgframedimen));
        } catch (Exception e) {
            e.printStackTrace();
        }

        File file = new File(getWorkingAppDir() + "/thumbnail/", fname);
        if (file.exists())
            file.delete();
        try {
            FileOutputStream outstream = new FileOutputStream(file);
            b.compress(Bitmap.CompressFormat.JPEG, 50, outstream);
            outstream.flush();
            outstream.close();
        } catch (Exception e) {
        }
    }

    // Return the path to App Directory
    public static String getWorkingAppDir() {
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            File directory = new File(Environment.getExternalStorageDirectory() + "/" + appName);
            if (!directory.exists()) {
                if (directory.mkdir()) {
                }
            }

            File mediafolder =
                    new File(Environment.getExternalStorageDirectory() + "/" + appName + "/media");
            if (!mediafolder.exists()) {
                if (mediafolder.mkdir()) {
                }
            }

            File thumbnailfolder =
                    new File(Environment.getExternalStorageDirectory() + "/" + appName + "/thumbnail");
            if (!thumbnailfolder.exists()) {
                if (thumbnailfolder.mkdir()) {
                }

            }
        }
        return Environment.getExternalStorageDirectory() + "/" + appName;
    }

    public static String classColourCode(String className) {
        String[] colours =
                {"#3F51B5", "#9C27B0", "#00BCD4", "#009688", "#607D8B", "#FF5722", "#4CAF50", "#673AB7",
                "#8BC34A", "#795548", "#FF9800"};

        int hashCode = className.hashCode();


        int index = Math.abs(hashCode) % 11;
        return colours[index];
    }


    public static String removeSpecialChars(String str) {
        if (!UtilString.isBlank(str)) {
            str = str.replaceAll("[^\\w\\s-]", "");
        }
        return str;
    }


    public static int nonNegative(int x){
        if(x < 0) return 0;
        return x;
    }

    //update current time(sync with server now - needed for first time e.g login, signup)
    public static void updateCurrentTime(){
        Log.d("DEBUG_UTILITY", "using updateCurrentTime() during login/signup");
        SessionManager session = new SessionManager(Application.getAppContext());

        HashMap<String, Date> parameters = new HashMap<String, Date>();
        try{
            Date now = ParseCloud.callFunction("getServerTime", parameters);
            if(now != null) {
                session.setCurrentTime(now);
            }
            else{
                Log.d("DEBUG_UTILITY_UPADTE_CURRENT_TIME", "getServerTime failed - Date null");
            }
        }
        catch (com.parse.ParseException e){
            Log.d("DEBUG_UTILITY_UPADTE_CURRENT_TIME", "getServerTime failed - ParseException");
            e.printStackTrace();
        }
    }

    //update current time (but in background as not urgent)
    public static void updateCurrentTimeInBackground(){
        Log.d("DEBUG_UTILITY", "using updateCurrentTimeInBackground()");
        final SessionManager session = new SessionManager(Application.getAppContext());

        HashMap<String, Date> parameters = new HashMap<String, Date>();
        ParseCloud.callFunctionInBackground("getServerTime", parameters, new FunctionCallback<Object>() {
            @Override
            public void done(Object now, com.parse.ParseException e) {

                if(e == null && now != null) {
                    Date currentTime = (Date) now;
                    session.setCurrentTime(currentTime);
                }
                else{
                    Log.d("DEBUG_UTILITY_UPADTE_CURRENT_TIME_BACK", "getServerTime failed");
                }
            }
        });
    }

    /*
        Used get contents of parseobject in json form for debugging(print)
     */
    public static JSONObject parseObjectToJson(ParseObject parseObject){
        JSONObject jsonObject = new JSONObject();
        Set<String> keys = parseObject.keySet();
        for (String key : keys) {
            Object objectValue = parseObject.get(key);

            try{
                if(objectValue == null){
                    jsonObject.put(key, "#NULL");
                }
                else{
                    jsonObject.put(key, objectValue.toString());
                }
            }
            catch (JSONException e){
                e.printStackTrace();
            }
        }
        return jsonObject;
    }

    //convert 'dpi'(density independent pixels) to actual pixels according to screen density
    public static int dpiToPixels(int dpi){
        float scale = Application.getAppContext().getResources().getDisplayMetrics().density;
        int pixels = (int) (dpi * scale + 0.5f);
        return pixels;
    }

    public static String getAccountEmail(){
        Pattern emailPattern = Patterns.EMAIL_ADDRESS; // API level 8+
        Account[] accounts = AccountManager.get(Application.getAppContext()).getAccounts();
        for (Account account : accounts) {
            if (emailPattern.matcher(account.name).matches()) {
                String possibleEmail = account.name;
                if(possibleEmail != null){
                    return possibleEmail;
                }
            }
        }
        return null;
    }

    /*
        input : int count
        returns: "s" if count > 0
                 "" otherwise
     */
    public static String getPluralSuffix(int count){
        return (count > 1 ? "s" : "");
    }

    public static void checkAndHandleInvalidSession(ParseException e){
        if(e.getCode() == ParseException.INVALID_SESSION_TOKEN){
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    Log.d("__A", "checkAndHandleInvalidSession : inside job");
                    Utility.toast("Session expired. Please login again !");
                    ParseUser.logOut(); //NOTE : does not throw exception, make currentUser null
                    startLoginActivity();
                }
            };

            Log.d("__A", "checkAndHandleInvalidSession : posting job to Application.applicationHandler");
            Application.applicationHandler.post(r);
        }
    }
}
