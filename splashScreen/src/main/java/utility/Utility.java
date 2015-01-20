package utility;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import com.parse.ParseInstallation;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import library.UtilString;
import loginpages.LoginPage;

import loginpages.Signup;
import notifications.AlarmTrigger;
import trumplabs.schoolapp.Application;
import trumplab.textslate.R;

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
import android.os.Environment;
import android.provider.SyncStateContract.Constants;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import baseclasses.MyActionBarActivity;

public class Utility extends MyActionBarActivity {

    static String appName = "knit";

    public static void ls(String str) {
        if (!UtilString.isBlank(str))
            System.out.println(str);
    }


    public static void logout() {

        Context _context = Application.getAppContext();
        // After logout redirect user to Loing Activity

        //cancel all alarms set for notification checking. Very first thing to do
        AlarmTrigger.cancelAlarm(_context);

        SessionManager session = new SessionManager(Application.getAppContext());
        session.reSetAppOpeningCount();
        session.reSetSignUpAccount();
        session.reSetChildList();
        session.reSetDefaultClassExtst();

        Intent i = new Intent(_context, Signup.class);
        // Closing all the Activities
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // Add new Flag to start new Activity
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        ParseUser.logOut();

        ParseInstallation pi = ParseInstallation.getCurrentInstallation();

        List<List<String>> joinedGroups = new ArrayList<List<String>>();
        pi.put("channels", joinedGroups);
        pi.saveInBackground();


        // Staring Login Activity
        _context.startActivity(i);
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

        Utility.toast(label + " copied.");
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

    public static String getCurrentTimeStamp() {
        Date dNow = new Date();
        SimpleDateFormat ft = new SimpleDateFormat("yyyyMMddHHmmss");
        String timeStamp = (ft.format(dNow).toString());
        return timeStamp;
    }

    public static final boolean isInternetOn(Activity activity) {
        ConnectivityManager connec =
                (ConnectivityManager) Application.getAppContext().getSystemService(
                        Context.CONNECTIVITY_SERVICE);
        // ARE WE CONNECTED TO THE NET
        if (connec.getNetworkInfo(0).getState() == NetworkInfo.State.CONNECTED
                || connec.getNetworkInfo(1).getState() == NetworkInfo.State.CONNECTED) {
            // MESSAGE TO SCREEN FOR TESTING (IF REQ)
            // Toast.makeText(this, connectionType + " connected", Toast.LENGTH_SHORT).show();
            final TextView internetbar = (TextView) activity.findViewById(R.id.internetbar);
            internetbar.setVisibility(View.GONE);
            return true;
        } else if (connec.getNetworkInfo(0).getState() == NetworkInfo.State.DISCONNECTED
                || connec.getNetworkInfo(1).getState() == NetworkInfo.State.DISCONNECTED) {
            final TextView internetbar = (TextView) activity.findViewById(R.id.internetbar);
            activity.findViewById(R.id.internetbar).setVisibility(View.VISIBLE);
            return false;
        }

        return false;
    }

    public static String generateCode() {
        int max = 99999;
        int min = 10001;
        Random rand = new Random();
        int randomNum = rand.nextInt((max - min) + 1) + min;

        return "TS" + randomNum;

    }

    public static void savePicInAppFolder(String filepath) {

        if (filepath == null) {
            Utility.toast("path null");
            return;
        }
        Bitmap myBitmap = Utility.decodeFile(new File(filepath));
        String fname = filepath.substring(filepath.lastIndexOf("/") + 1);
        String targetPath = Utility.getWorkingAppDir() + "/media/" + fname;
        File file = new File(targetPath);

        if (file.exists())
            file.delete();
        OutputStream out;
        try {
            out = new FileOutputStream(targetPath);
            myBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
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
            b =
                    ThumbnailUtils.extractThumbnail(
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

    // Return a Bitmap with size around 500KB
    public static Bitmap decodeFile(File f) {
        try {
            // Decode image size
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(new FileInputStream(f), null, o);

            // The new size we want to scale to
            final int REQUIRED_SIZE = 500;

            // Find the correct scale value. It should be the power of 2.
            int scale = 1;
            while (o.outWidth / scale / 2 >= REQUIRED_SIZE && o.outHeight / scale / 2 >= REQUIRED_SIZE)
                scale *= 2;

            // Decode with inSampleSize
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            return BitmapFactory.decodeStream(new FileInputStream(f), null, o2);
        } catch (FileNotFoundException e) {
        }
        return null;
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


    public static List<List<String>> removeItemFromJoinedGroups(ParseUser user, List<String> item) {
        if (user != null) {
            List<List<String>> joinedGroups = user.getList(trumplabs.schoolapp.Constants.JOINED_GROUPS);

            if (joinedGroups != null && item != null) {
                for (int i = 0; i < joinedGroups.size(); i++) {
                    if (joinedGroups.get(i).get(0).trim().equals(item.get(0).trim())) {
                        joinedGroups.remove(i);
                        return joinedGroups;
                    } else
                        return joinedGroups;
                }
            } else
                return joinedGroups;
        }


        return null;

    }

    public static int nonNegative(int x){
        if(x < 0) return 0;
        return x;
    }

    public static void updateCurrentTime(ParseUser freshUser, SessionManager session){
        freshUser.put("test", true);
        try{
            freshUser.save();
            Date currentDate = freshUser.getUpdatedAt();
            session.setCurrentTime(currentDate);
        }
        catch (com.parse.ParseException e){

        }
    }


}
