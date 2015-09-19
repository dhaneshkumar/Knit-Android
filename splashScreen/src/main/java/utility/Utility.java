package utility;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.media.ThumbnailUtils;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.util.Log;
import android.util.Patterns;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.login.LoginManager;
import com.google.android.gms.plus.Plus;
import com.parse.FunctionCallback;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;

import library.UtilString;
import loginpages.Signup;
import notifications.AlarmTrigger;
import profileDetails.ProfilePage;
import trumplabs.schoolapp.Application;
import trumplabs.schoolapp.Constants;

public class Utility{

    static String appName = "knit";

    public static void ls(String str) {
        if (!UtilString.isBlank(str))
            System.out.println(str);
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

        @Override
        protected Void doInBackground(Void... params) {
            ParseInstallation pi = ParseInstallation.getCurrentInstallation();

            HashMap<String, Object> param = new HashMap<>();
            param.put("installationId", pi.getInstallationId());

            try{
                if(Config.SHOWLOG) Log.d("DEBUG_UTILITY", "logout() - appLogout before calling");
                boolean logoutSuccess = ParseCloud.callFunction("appExit", param);
                if(Config.SHOWLOG) Log.d("DEBUG_UTILITY", "logout() - appLogout returned with" + logoutSuccess);
                success = true;
                return null;
            }
            catch (com.parse.ParseException e){
                LogoutUtility.checkAndHandleInvalidSession(e);
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result){
            //UI thread
            if(success) {
                if(Config.SHOWLOG) Log.d("DEBUG_UTILITY", "logout() : launching Signup activity = " + success);
                LogoutUtility.performLogoutAction();
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


    public static void toast(String str){
        toast(str, false, 17, false); //by default user shouldn't be null while showing toast
    }

    public static void toast(String str, int fontSize){
        toast(str, false, fontSize, false);
    }

    public static void toastDialog(String str){
        toast(str, false, 17, true); //when shown in dialog, activity is paused
    }

    public static void toast(String str, boolean isNullUserOK) {
        toast(str, isNullUserOK, 17, false);
    }

    public static void toast(String str, boolean isNullUserOK, int fontSize) {
        toast(str, isNullUserOK, fontSize, false);
    }

    /*
        @param str Content to show as toast
        @param isNullUserOK whether while showing this toast, null user is acceptable e.g during signup/login process
     */
    public static void toast(String str, boolean isNullUserOK, int fontSize, boolean isNullActivityOK) {

        if(ParseUser.getCurrentUser() == null && !isNullUserOK){
            if(Config.SHOWLOG) Log.d("__A", "toast : parseUser null, hence ignoring content=" + str);
            return;
        }

        //see if app is visible, i.e current activity not null
        if(Application.getCurrentActivity() == null && !isNullActivityOK){
            if(Config.SHOWLOG) Log.d("__A", "toast : app not visible, hence ignoring content=" + str);
            return;
        }

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
        tv.setTextSize(fontSize);
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

            Date d2 = SessionManager.getInstance().getCurrentTime();

            if (d2 == null)
                return "just now";

            long diff = d2.getTime() - d1.getTime();

            long diffMinutes = (diff / Constants.MINUTE_MILLISEC) % 60;
            long diffHours = (diff / Constants.HOUR_MILLISEC) % 24;
            long diffDays = diff / Constants.DAY_MILLISEC;

            if (diffDays != 0) {
                result = diffDays + " day" + Utility.getPluralSuffix((int)diffDays) + " ago";
            } else if (diffHours != 0) {
                result = diffHours + " hr" + Utility.getPluralSuffix((int)diffHours) + " ago";
            } else if (diffMinutes != 0) {
                result = diffMinutes + " min" + Utility.getPluralSuffix((int) diffMinutes) + " ago";

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

    public static class MyDateFormatter {
        private SimpleDateFormat sdfDateYear;
        private SimpleDateFormat sdfYearOnly;
        private SimpleDateFormat sdfTimeOnly;
        private SimpleDateFormat sdfTimeDate;
        private SimpleDateFormat sdfTimeDateYear;

        private static MyDateFormatter instance = null;

        public MyDateFormatter(){
            DateFormatSymbols symbols = new DateFormatSymbols(Locale.getDefault());
            symbols.setAmPmStrings(new String[]{"a", "p"});

            sdfDateYear = new SimpleDateFormat("dd/MM/yy");

            sdfYearOnly = new SimpleDateFormat("yy");

            sdfTimeOnly = new SimpleDateFormat("hh:mma");
            sdfTimeOnly.setDateFormatSymbols(symbols);

            sdfTimeDate = new SimpleDateFormat("dd/MM hh:mma");
            sdfTimeDate.setDateFormatSymbols(symbols);

            sdfTimeDateYear = new SimpleDateFormat("dd/MM/yy hh:mma");
            sdfTimeDateYear.setDateFormatSymbols(symbols);
        }

        public static MyDateFormatter getInstance(){
            if(instance == null){
                instance = new MyDateFormatter();
            }

            return instance;
        }

        public String convertTimeStampNew(Date msgDate){
            Log.d("__timestamp", "enter");
            String result;

            if (msgDate != null) {

                String msgDateString = sdfDateYear.format(msgDate);
                String msgYearString = sdfYearOnly.format(msgDate);
                Log.d("__timestamp", "msgDateString " + msgDateString + ", " + msgYearString);

                Calendar today = Calendar.getInstance();
                String todayDateString = sdfDateYear.format(today.getTime());
                String currentYearString = sdfYearOnly.format(today.getTime());
                Log.d("__timestamp", "todayString " + todayDateString + ", " + currentYearString);

                Calendar yesterday = Calendar.getInstance();
                yesterday.add(Calendar.DATE, -1);
                String yesterdayDateString = sdfDateYear.format(yesterday.getTime());
                Log.d("__timestamp", "yesterdayString " + yesterdayDateString);

                if(msgDateString.equals(todayDateString)){
                    result = sdfTimeOnly.format(msgDate);
                }
                else if(msgDateString.equals(yesterdayDateString)){
                    result = "yesterday " + sdfTimeOnly.format(msgDate);
                }
                else if(msgYearString.equals(currentYearString)) {
                    result = sdfTimeDate.format(msgDate);
                }
                else{
                    result = sdfTimeDateYear.format(msgDate);
                }

                Log.d("__timestamp", "exit");
                return result;
            }

            return "just now";
        }

        public String giveInDetail(Date date){
            if(date != null){
                return sdfTimeDateYear.format(date);
            }
            return "unknown";
        }
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

            toast("No Internet Connection", true); //user null doesn't matter
            return false;
        }

        return false;
    }

    // Creating the thumbnail of the image in media folder
    public static void createThumbnail(Activity getactivity, String fname) {
        Log.d("__TH", "starting for fname=" + fname);
        if(getactivity == null){
            return;
        }
        Display mDisplay = getactivity.getWindowManager().getDefaultDisplay();
        float width = mDisplay.getWidth();
        float height = mDisplay.getHeight();
        float imgframedimen;
        if (width < height)
            imgframedimen = width;
        else
            imgframedimen = height;
        Bitmap b = null;

        int roundedFrameDimension = (int) Math.round(imgframedimen);
        try {
            b = ThumbnailUtils.extractThumbnail(
                    decodeSampledBitmapFromFile(getWorkingAppDir() + "/media/" + fname, roundedFrameDimension, roundedFrameDimension),
                    roundedFrameDimension,
                    roundedFrameDimension);
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
        Log.d("__TH", "ending for fname=" + fname);
    }

    public static Bitmap decodeSampledBitmapFromFile(String filePath,
                                                         int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options); //sets the size in options.outHeight, options.outWidth

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        Log.d("__TH", "sample size=" + options.inSampleSize + ", fname=" + filePath + ", reqDim=" + reqWidth);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(filePath, options);
    }

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;

        Log.d("__TH", "raw h=" + height + ", w=" + width);

        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
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

            File docsfolder =
                    new File(Environment.getExternalStorageDirectory() + "/" + appName + "/docs");
            if (!docsfolder.exists()) {
                if (docsfolder.mkdir()) {
                }

            }
        }
        return Environment.getExternalStorageDirectory() + "/" + appName;
    }

    public static class FileDetails{
        public String fName;
        public long fSize;
    }

    public static FileDetails getFileNameFromUri(Uri uri) {
        FileDetails details = new FileDetails();
        details.fName = null;
        details.fSize = -1;

        if (uri.getScheme().equals("content")) {
            Cursor cursor = Application.getAppContext().getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    details.fName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                    details.fSize = cursor.getLong(cursor.getColumnIndex(OpenableColumns.SIZE));
                }
            } finally {
                cursor.close();
            }
        }

        if (details.fName == null) {
            details.fName = uri.getPath();
            int cut = details.fName.lastIndexOf('/');
            if (cut != -1) {
                details.fName = details.fName.substring(cut + 1);
            }
        }
        return details;
    }

    /*
        Use this to give a unique name to captured or picked image before storing 'media' folder
        This will be unique globally across all users, any time (as anytime a user can have only one session)
     */
    public static String getUniqueImageName(ParseUser currentParseUser){
        String parseUserObjectId = currentParseUser.getObjectId();
        Long timeInMillis = new Date().getTime();

        return parseUserObjectId + "_" + timeInMillis + ".jpg";
    }

    public static String getUniqueFileName(ParseUser currentParseUser, String fileName, String extension){
        fileName = removeSpecialChars(fileName);

        String parseUserObjectId = currentParseUser.getObjectId();
        Long timeInMillis = new Date().getTime();

        if(fileName.length() > 20){
            fileName = fileName.substring(0, 20);
        }
        return fileName + "_" + parseUserObjectId + "_" + timeInMillis + "." + extension;
    }

    public static String getFileLocationInAppFolder(String fileName){
        if(isFileImageType(fileName)){
            Log.d("__file_picker", "getFileLocationInAppFolder image type : " + fileName);
            return getWorkingAppDir() + "/media/" + fileName;
        }
        else {
            //for non-image type files
            Log.d("__file_picker", "getFileLocationInAppFolder fallback : " + fileName);
            return getWorkingAppDir() + "/docs/" + fileName;
        }
    }

    public static String getExtension(String fileName){
        if(fileName != null){
            int i = fileName.lastIndexOf('.');
            if (i >= 0) {
                String extension = fileName.substring(i+1);
                return extension;
            }
        }
        return null;
    }

    public static String getMimeType(String fileName){
        String extension = getExtension(fileName);
        if(extension != null){
            Log.d("__file_picker", "non null extension=" + extension);
            MimeTypeMap myMime = MimeTypeMap.getSingleton();
            String mimeType = myMime.getMimeTypeFromExtension(extension);
            String[] exts = new String[]{"pdf", "doc", "xls", "ppt", "xml", "txt", "html", "csv"};
            for(String e : exts){
                Log.d("__file_picker_check", e + " " + myMime.getMimeTypeFromExtension(e));
            }

            if(mimeType != null){
                Log.d("__file_picker", "non null mimeType=" + mimeType);
                return mimeType;
            }
        }

        //default fallback, won't occur in general
        Log.d("__file_picker", "default fallback");
        return "*/*";
    }

    public static boolean isFileImageType(String fileName){
        if(fileName != null){
            String extension = getExtension(fileName);
            if(extension != null && (extension.toLowerCase().contains("jpg") || extension.toLowerCase().contains("jpeg"))){
                return true;
            }
        }
        return false;
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
            str = str.replaceAll("[^A-Za-z0-9_\\.]", "-");
        }
        return str;
    }


    public static int nonNegative(int x){
        if(x < 0) return 0;
        return x;
    }

    //update current time(sync with server now - needed for first time e.g login, signup)
    public static void updateCurrentTime(){
        if(Config.SHOWLOG) Log.d("DEBUG_UTILITY", "using updateCurrentTime() during login/signup");

        HashMap<String, Date> parameters = new HashMap<String, Date>();
        try{
            Date now = ParseCloud.callFunction("getServerTime", parameters);
            if(now != null) {
                SessionManager.getInstance().setCurrentTime(now);
            }
            else{
                if(Config.SHOWLOG) Log.d("DEBUG_UTILITY_UPADTE_CURRENT_TIME", "getServerTime failed - Date null");
            }
        }
        catch (com.parse.ParseException e){
            if(Config.SHOWLOG) Log.d("DEBUG_UTILITY_UPADTE_CURRENT_TIME", "getServerTime failed - ParseException");
            e.printStackTrace();
        }
    }

    //update current time (but in background as not urgent)
    public static void updateCurrentTimeInBackground(){
        if(Config.SHOWLOG) Log.d("DEBUG_UTILITY", "using updateCurrentTimeInBackground()");

        HashMap<String, Date> parameters = new HashMap<String, Date>();
        ParseCloud.callFunctionInBackground("getServerTime", parameters, new FunctionCallback<Object>() {
            @Override
            public void done(Object now, com.parse.ParseException e) {

                if(e == null && now != null) {
                    Date currentTime = (Date) now;
                    SessionManager.getInstance().setCurrentTime(currentTime);
                }
                else{
                    if(Config.SHOWLOG) Log.d("DEBUG_UTILITY_UPADTE_CURRENT_TIME_BACK", "getServerTime failed");
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
        return (count == 1 ? "" : "s");
    }

    /*
        Check if given number is valid if following conditions satisfy:
        i.e is not blank
            is of exactly 10 digits
            first digit is >= 7 (check only if Config.DETECT_INVALID_NUMBER is true)
     */
    public static boolean isNumberValid(String phoneNumber){
        if(!UtilString.isBlank(phoneNumber) && phoneNumber.length() == 10){
            if(Config.DETECT_INVALID_NUMBER && Character.getNumericValue(phoneNumber.charAt(0)) < 7){
                return false;
            }
            return true;
        }
        return false;
    }

    public static boolean isTagSame(ImageView imageView, String oldTag){
        if(imageView != null && imageView.getTag() != null && oldTag != null && oldTag.equals((String)imageView.getTag())){
            return true;
        }
        return false;
    }

    public static String getAppseeId(){
        String appseeId = SessionManager.getInstance().getAppseeId();
        if(appseeId == null){
            appseeId = Config.APPSEE_ID; //default fallback is the old key
        }
        return appseeId;
    }

    public static void fetchAppseeIdIfNeeded(){
        Date appseeExpiry = SessionManager.getInstance().getDate(SessionManager.KEY_APPSEE_EXPIRY);
        if(appseeExpiry == null){
            appseeExpiry = Config.APPSEE_EXPIRY;
        }

        Date current = new Date();

        Log.d("__appsee", "exp=" + Utility.MyDateFormatter.getInstance().giveInDetail(appseeExpiry) +
                ", cur=" + Utility.MyDateFormatter.getInstance().giveInDetail(current) + " compare=" + current.compareTo(appseeExpiry));

        Date lastChecked = SessionManager.getInstance().getDate(SessionManager.KEY_APPSEE_LAST_CHECKED);
        if(lastChecked == null){
            lastChecked = new GregorianCalendar(2015, 1, 1).getTime(); //1 jan 2015 just some old date
        }
        Log.d("__appsee", "last checked=" + Utility.MyDateFormatter.getInstance().giveInDetail(lastChecked));

        if((current.getTime() - appseeExpiry.getTime() > 0) || (current.getTime() - lastChecked.getTime() > Config.APPSEE_RETRY_INTERVAL_ALWAYS)){

            if(current.getTime() - lastChecked.getTime() > Config.APPSEE_RETRY_INTERVAL){
                //more than 1 day, start a thread to fetch new id
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        /*Log.d("__appsee", "sleep start");
                        TestingUtililty.sleep(5000);
                        String id = "abc";
                        Calendar expiryCal = Calendar.getInstance();
                        expiryCal.add(Calendar.MINUTE, TestingUtililty.multiplier * 5);
                        Date expiry = expiryCal.getTime();
                        Log.d("__appsee", "sleep over");*/

                        String id = null;
                        Date expiry = null;

                        try {
                            ParseQuery keyQuery = new ParseQuery("Appsee");
                            keyQuery.whereEqualTo("name", "appsee");
                            keyQuery.setLimit(1);

                            List<ParseObject> results = keyQuery.find();
                            ParseObject keyDetail = results.get(0);

                            id = keyDetail.getString("key");
                            expiry = keyDetail.getDate("expiry");
                        }
                        catch (ParseException e){
                            e.printStackTrace();
                        }

                        if(id != null && expiry != null){
                            Log.d("__appsee", "success id=" + id + ", expiry=" + Utility.MyDateFormatter.getInstance().giveInDetail(expiry));
                            SessionManager.getInstance().setAppseeId(id);
                            SessionManager.getInstance().setDate(SessionManager.KEY_APPSEE_EXPIRY, expiry);
                            SessionManager.getInstance().setDate(SessionManager.KEY_APPSEE_LAST_CHECKED, new Date());//now
                            Log.d("__appsee", "success persistently set");
                        }
                        else{
                            Log.d("__appsee", "failure");
                        }
                    }
                }).start();
            }
            else{
                Log.d("__appsee", "too frequent to retry");
            }
        }
        else{
            Log.d("__appsee", "not yet expired");
        }
    }

    public static class LogoutUtility{
        //flag so that checkAndHandleInvalidSession does not run multiple times(i.e run only once until next login)
        //is set in checkAndHandleInvalidSession once it runs successfully
        //is unset when ParseUser.becomeUser succeeds
        private static boolean ignoreInvalidSessionCheck = false;

        public static void setIgnoreInvalidSessionCheck(){
            ignoreInvalidSessionCheck = true;
        }

        public static void resetIgnoreInvalidSessionCheck(){
            ignoreInvalidSessionCheck = false;
        }

        public static boolean getIgnoreInvalidSessionCheck(){
            return ignoreInvalidSessionCheck;
        }

        /*
        Looks at the ParseException and takes action if invalid_session_token error.
        Returns true if handled, false otherwise
        */
        public static boolean checkAndHandleInvalidSession(ParseException e){
            if(e != null && e.getCode() == ParseException.INVALID_SESSION_TOKEN){
                if(Config.SHOWLOG) Log.d("__A", "checkAndHandleInvalidSession : calling static logout() : my caller=" + TestingUtililty.getCaller());
                logout();
                return true;
            }
            return false;
        }

        //can call from non-UI thread
        //default called from everywhere(along with checkAndHandleInvalidSession)
        public static void logout() {
            if(Config.SHOWLOG) Log.d("__A", "static logout called : my caller=" + TestingUtililty.getCaller());
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    if(Config.SHOWLOG) Log.d("__A", "logout() : inside job");
                    Utility.toast("Session expired. Please login again !", true);
                    performLogoutAction();
                }
            };

            if(!getIgnoreInvalidSessionCheck() && Application.applicationHandler != null){
                setIgnoreInvalidSessionCheck();
                if(Config.SHOWLOG) Log.d("__A", "logout() : posting the job");
                Application.applicationHandler.post(r);
            }
            else{
                if(Config.SHOWLOG) Log.d("__A", "logout() : SKIPPING posting the job");
            }
        }

        /*
           call from GUI thread only
        */
        public static void performLogoutAction(){
            resetLocalData(); //clear all the flags(general & user specific), stop alarms
            //do this first before unpinning or logout current user

            if(Config.SHOWLOG) Log.d("__A", "checkAndHandleInvalidSession : unpinning current user");
            ParseUser currentUser = ParseUser.getCurrentUser();
            if(currentUser != null){
                currentUser.unpinInBackground();
            }

            ParseUser.logOut(); //NOTE : does not throw exception, make currentUser null

            //fb logout
            LoginManager.getInstance().logOut();

            //google logout
            if(ProfilePage.mGoogleApiClient != null && ProfilePage.mGoogleApiClient.isConnected())
            {
                //removing old user credentials
                Plus.AccountApi.clearDefaultAccount(ProfilePage.mGoogleApiClient);
                ProfilePage.mGoogleApiClient.disconnect();
                ProfilePage.mGoogleApiClient.connect();

                Log.d("google logout", "google log out success");
            }
            else if(ProfilePage.mGoogleApiClient != null && !ProfilePage.mGoogleApiClient.isConnected())
            {
                Log.d("PROFILE_GOOGLE", "google client not connected");
            }

            /*//Not needed anymore because tab count won't change dynamically
            if(MainActivity.myAdapter != null){
                MainActivity.myAdapter.notifyDataSetChanged();
            }*/

            //launch login activity
            startLoginActivity();
        }

        //called in general (and also when currentUser is null)
        //can be called in a thread
        static void resetLocalData(){
            Context _context = Application.getAppContext();
            // After logout redirect user to Loing Activity

            Application.joinedSyncOnce = false;
            Application.lastTimeInboxSync = null;
            Application.lastTimeOutboxSync = null;

            //cancel all alarms set. Very first thing to do
            AlarmTrigger.cancelEventCheckerAlarm(_context);
            AlarmTrigger.cancelRefresherAlarm(_context);
            AlarmTrigger.cancelNotificationAlarm(_context);

            SessionManager session = SessionManager.getInstance();
            session.reSetAppOpeningCount();
            session.reSetSignUpAccount();

            ParseUser currentParseUser = ParseUser.getCurrentUser();
            if(currentParseUser != null){
                session.setCodegroupLocalState(0, currentParseUser.getUsername());
                session.setOutboxLocalState(0, currentParseUser.getUsername());
            }
        }
    }
}