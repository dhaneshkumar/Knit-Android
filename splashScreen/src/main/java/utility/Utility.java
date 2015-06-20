package utility;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.media.ThumbnailUtils;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.FunctionCallback;
import com.parse.ParseCloud;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParseUser;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;

import baseclasses.MyActionBarActivity;
import library.UtilString;
import loginpages.Signup;
import notifications.AlarmTrigger;
import profileDetails.ProfilePage;
import trumplab.textslate.R;
import trumplabs.schoolapp.Application;

public class Utility extends MyActionBarActivity {

    static String appName = "knit";

    public static void ls(String str) {
        if (!UtilString.isBlank(str))
            System.out.println(str);
    }

    /*
        clear extra fields : "id"
        set flag "newIdFlag" to true in parseInstallation indicating a signup/singin and that
        not to put old objectId into "id" field as that objectId row might have been deleted on
        cloud server.
     */
    public static void setNewIdFlagInstallation(){
        ParseInstallation parseInstallation = ParseInstallation.getCurrentInstallation();

        if(parseInstallation != null){
            parseInstallation.remove("id"); //remove id key so that in checkParseInstallation,
                                            // we don't get a false indication that id is set
            parseInstallation.put("newIdFlag", true);
            try{
                parseInstallation.pin();
                Log.d("DEBUG_UTILITY", "setNewIdFlagInstallation : remove id field; set newIdFlag successfully");
            }
            catch (com.parse.ParseException e){
                e.printStackTrace();
            }
        }
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

        // Since "id" is not set : Handle Upgrade to new version if newIdFlag not set & if objectId set,
        // and use it to set "id" field
        if(!parseInstallation.getBoolean("newIdFlag") && parseInstallation.getObjectId() != null){
            parseInstallation.put("id", parseInstallation.getObjectId());
            try{
                parseInstallation.pin();
                Log.d("DEBUG_UTILITY", "checkParseInstallation : setting id using already existing objectId");
                return true; //success
            }
            catch (com.parse.ParseException e){
                e.printStackTrace();
            }
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

    public static void logout() {//default called from everywhere except ProfilePage
        LogoutTask logoutTask = new LogoutTask(false, Application.getAppContext());
        logoutTask.execute();
    }

    public static void logoutProfilePage(Context context){
        LogoutTask logoutTask = new LogoutTask(true, context);
        logoutTask.execute();
    }

    public static class LogoutTask extends AsyncTask<Void, Void, Void> {
        boolean isForeground; //whether logout explicitly called
        boolean success; //for static classes, explicitly initialize the variables
        Context _context;
        LogoutTask(boolean isFore, Context context){
            success = false;
            isForeground = isFore;
            _context = context;

            if(isForeground){
                if(ProfilePage.profileLayout!= null){
                    ProfilePage.profileLayout.setVisibility(View.GONE);
                    ProfilePage.progressBarLayout.setVisibility(View.VISIBLE);
                }
            }
        }


        @Override
        protected Void doInBackground(Void... params) {
            Context _context = Application.getAppContext();
            // After logout redirect user to Loing Activity

            Application.lastTimeJoinedSync = null;
            Application.lastTimeInboxSync = null;
            Application.lastTimeOutboxSync = null;

            //cancel all alarms set. Very first thing to do
            AlarmTrigger.cancelEventCheckerAlarm(_context);
            AlarmTrigger.cancelRefresherAlarm(_context);

            SessionManager session = new SessionManager(Application.getAppContext());
            session.reSetAppOpeningCount();
            session.reSetSignUpAccount();
            session.reSetChildList();
            session.reSetDefaultClassJoinStatus();
            session.reSetActionBarHeight();

            ParseInstallation pi = ParseInstallation.getCurrentInstallation();

            HashMap<String, Object> param = new HashMap<String, Object>();
            param.put("installationObjectId", pi.getString("id"));

            try{
                boolean logoutSuccess = ParseCloud.callFunction("appLogout", param);
                Log.d("DEBUG_UTILITY", "logout() - appLogout cloud function result is " + logoutSuccess);
                ParseUser.logOut();
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
            if(isForeground){
                if(ProfilePage.profileLayout!= null){
                    ProfilePage.profileLayout.setVisibility(View.VISIBLE);
                    ProfilePage.progressBarLayout.setVisibility(View.GONE);
                    if(!success){
                        Utility.toast("Unable to logout !");
                    }
                }
            }
            if(!isForeground || success) {//either 1) called due to parseuser null or 2) success flag set
                Log.d("DEBUG_UTILITY", "logout() : launching Signup activity = " + success);
                Intent i = new Intent(_context, Signup.class);
                // Closing all the Activities
                // Add new Flag to start new Activity
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

                _context.startActivity(i);
            }
        }
    }


    public static void toast(String str) {
        LinearLayout layout = new LinearLayout(Application.getAppContext());
        layout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        layout.setBackgroundResource(R.drawable.round_corner_red_color);


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
        tv.setTextSize(16);
        tv.setPadding(50, 20, 50, 20);

        tv.setGravity(Gravity.CENTER);

        // set the text you want to show in Toast
        tv.setText(str);
        layout.addView(tv);

        Toast toast = new Toast(Application.getAppContext()); // context is object of Context write
        // "this" if you are an Activity
        // Set The layout as Toast View
        toast.setView(layout);

        // Position you toast here toast position is 50 dp from bottom you can give any integral value
        toast.setGravity(Gravity.TOP, 0, height / 3);
        toast.show();
    }



    public static void toastDone(String str) {
        LinearLayout layout = new LinearLayout(Application.getAppContext());
        layout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        layout.setBackgroundResource(R.drawable.round_corner_red_color);


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

        ImageView img = new ImageView(Application.getAppContext());


        int sdk = android.os.Build.VERSION.SDK_INT;
        if(sdk < android.os.Build.VERSION_CODES.JELLY_BEAN) {
            img.setBackgroundDrawable( Application.getAppContext().getResources().getDrawable(R.drawable.done) );
        } else {
            img.setBackground( Application.getAppContext().getResources().getDrawable(R.drawable.done));
        }

        layout.addView(img);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(16,6,4,6);


        TextView tv = new TextView(Application.getAppContext());
        // set the TextView properties like color, size etc
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(16);
        tv.setPadding(16, 20, 30, 20);

        tv.setGravity(Gravity.CENTER);

        // set the text you want to show in Toast
        tv.setText(str);
        layout.addView(tv);

        Toast toast = new Toast(Application.getAppContext()); // context is object of Context write
        // "this" if you are an Activity
        // Set The layout as Toast View
        toast.setView(layout);

        // Position you toast here toast position is 50 dp from bottom you can give any integral value
        toast.setGravity(Gravity.TOP, 0, height / 3);
        toast.show();
    }

    public static void toastLong(String str) {
        LinearLayout layout = new LinearLayout(Application.getAppContext());
        layout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        layout.setBackgroundResource(R.drawable.round_corner_red_color);


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
        tv.setTextSize(16);
        tv.setPadding(50, 20, 50, 20);

        tv.setGravity(Gravity.CENTER);

        // set the text you want to show in Toast
        tv.setText(str);
        layout.addView(tv);

        Toast toast = new Toast(Application.getAppContext()); // context is object of Context write
        // "this" if you are an Activity
        // Set The layout as Toast View
        toast.setView(layout);
        toast.setDuration(Toast.LENGTH_LONG);
        // Position you toast here toast position is 50 dp from bottom you can give any integral value
        toast.setGravity(Gravity.TOP, 0, height / 3);
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

            Utility.toastDone(label.trim() + " copied");
        }
    }

    public static String convertTimeStamp(Date d1) throws ParseException {


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
     * @param activity
     * @return
     */
    public static final boolean isInternetExistWithoutPopup(Activity activity) {
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



    public static boolean isInternetExist(Activity activity) {
        ConnectivityManager connec =
                (ConnectivityManager) Application.getAppContext().getSystemService(
                        Context.CONNECTIVITY_SERVICE);
        // ARE WE CONNECTED TO THE NET
        if (connec.getNetworkInfo(0).getState() == NetworkInfo.State.CONNECTED
                || connec.getNetworkInfo(1).getState() == NetworkInfo.State.CONNECTED) {

            return true;
        } else if (connec.getNetworkInfo(0).getState() == NetworkInfo.State.DISCONNECTED
                || connec.getNetworkInfo(1).getState() == NetworkInfo.State.DISCONNECTED) {

            showPopup(activity);
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
                            (int) Math.round(imgframedimen), (int) Math.round(imgframedimen * 0.75));
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
                {"#E6CA5A", "#E4944D", "#E5766A", "#60D1D9", "#BD81D5", "#6AAEDB", "#67D595"};

        int hashCode = className.hashCode();


        int index = Math.abs(hashCode) % 7;
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
    public static void updateCurrentTime(ParseUser freshUser){
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
    public static void updateCurrentTimeInBackground(final ParseUser freshUser){
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

    // The method that displays the popup.
    public static void showPopup(Activity context) {

        LayoutInflater layoutInflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = layoutInflater.inflate(R.layout.no_internet_connection, null);
        final TextView tv1 = (TextView) layout.findViewById(R.id.noInternet);


        // Creating the PopupWindow
        final PopupWindow popup = new PopupWindow(context);
        popup.setContentView(layout);

        /*
        Getting screen width in pixels
         */
        int measuredWidth = 0;
        int measuredHeight = 0;
        WindowManager w = context.getWindowManager();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            Point size = new Point();
            w.getDefaultDisplay().getSize(size);
            measuredWidth = size.x;
            measuredHeight = size.y;
        } else {
            Display d = w.getDefaultDisplay();
            measuredWidth = d.getWidth();
            measuredHeight = d.getHeight();
        }

        popup.setWidth(measuredWidth);
        popup.setHeight(100);
       // popup.setFocusable(true);

        // Clear the default translucent background
        popup.setBackgroundDrawable(new BitmapDrawable());


        //finding statusbar height
        int statusBarHeight = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            statusBarHeight = context.getResources().getDimensionPixelSize(resourceId);
        }




        //finding action bar height
        final SessionManager sessionManager = new SessionManager(Application.getAppContext());
        int actionBarHeight = sessionManager.getActionBarHeight();
        if (actionBarHeight == 0) {
            final TypedValue tv = new TypedValue();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                if (context.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true))
                    actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data, context.getResources().getDisplayMetrics());
            }
            else if (context.getTheme().resolveAttribute( android.support.v7.appcompat.R.attr.actionBarSize, tv, true))
            {
                actionBarHeight = TypedValue.complexToDimensionPixelSize( tv.data,context.getResources().getDisplayMetrics());
            }
        }


        //  Log.d("ACTION", "StatusBar Height= " + statusBarHeight + " , TitleBar Height = " + titleBarHeight);
        Log.d("ACTION","action bar height : " + actionBarHeight);

        Point p = new Point();
        p.x = 0;
        p.y = statusBarHeight + actionBarHeight;

        // Displaying the popup at the specified location, + offsets.
        popup.showAtLocation(layout, Gravity.NO_GRAVITY, p.x , p.y);

        //Auto hide popup after 5 seconds.
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                //Do something here
                try{
                    if(popup != null && popup.isShowing()) {
                        popup.dismiss();
                    }
                } catch (IllegalArgumentException e){
                    e.printStackTrace();
                } catch (Exception e){
                }
            }
        }, 3000);
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
}
