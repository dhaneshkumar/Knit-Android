package utility;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

import com.parse.ParseUser;

import java.util.Calendar;
import java.util.Date;

import trumplabs.schoolapp.Application;

@SuppressLint("SimpleDateFormat")
public class SessionManager {
    // Shared Preferences
    SharedPreferences pref;

    // Editor for Shared preferences
    Editor editor;

    // Shared pref mode
    public static final int PRIVATE_MODE = 0;

    private static final String PREF_NAME = "AndroidHivePref";
    public static final String APP_OPENING_COUNT = "app_opening_count";
    public static final String SIGNUP = "signUP";
    public static final String TIME_DELTA = "time_delta";
    public static final String ACTIONBAR_HEIGHT = "actionBarHeight";
    public static final String USER_HAS_JOINED_CLASS = "userHasJoinedClass";

    static SessionManager myInstance; //this will be (initialized if needed and) returned any time when needed

    public static SessionManager getInstance(){
        if(myInstance == null){
            myInstance = new SessionManager(Application.getAppContext());
        }

        return myInstance;
    }

    private SessionManager(Context context) {
        pref = context.getSharedPreferences(PREF_NAME, PRIVATE_MODE);
        editor = pref.edit();
    }


    //range ID_LOW to ID_HIGH { 0 to (ID_LOW-1) reserved for NORMAL notification or other newer types }
    //         0         10        0-9
    public static final int ID_LOW = 10;
    public static final int ID_HIGH = 1000;
    public int getNextNotificationId(){
        int id = pref.getInt("notification_id", -1);
        if(id < ID_LOW || id > ID_HIGH){
            id = ID_LOW;
        }
        editor.putInt("notification_id", id + 1);
        editor.commit();
        return id;
    }



    /************************************************************************/
  /*
   * keeping app opening count
   */
    public void setAppOpeningCount() {
        int openingCount = pref.getInt(APP_OPENING_COUNT, 0);
        if(Config.SHOWLOG) Log.d("DEBUG_SESSION_MANAGER", "setAppOpeningCount from " + openingCount);

        openingCount++;
        editor.putInt(APP_OPENING_COUNT, openingCount);
        editor.commit();
    }

    public void reSetAppOpeningCount() {
        if(Config.SHOWLOG) Log.d("DEBUG_SESSION_MANAGER", "resetAppOpeningCount");
        editor.putInt(APP_OPENING_COUNT, 0);
        editor.commit();
    }

    public int getAppOpeningCount() {
        int openingCount = pref.getInt(APP_OPENING_COUNT, 0);
        if(Config.SHOWLOG) Log.d("DEBUG_SESSION_MANAGER", "getAppOpeningCount = " + openingCount);
        return openingCount;
    }

    /*********************************************************************************/

    //Handle reinstall/relogin to prevent duplicate outbox data
    // 0 means no local valid data present. So clear local outbox and fetch new messages
    // 1 means valid data. So no need to download outbox messages
    public void setOutboxLocalState(int val, String userId){
        editor.putInt("outbox-" + userId, val);
        editor.commit();
    }

    public int getOutboxLocalState(String userId){
        return pref.getInt("outbox-" + userId, 0);
    }

    /*
        Handle reisntall/relogin to see if Codegroup data for joined and created clasees has been
        fetched or not
        value 1 = fetched and pinned locally
        value 0 = Not yet fetched successfully
     */
    public void setCodegroupLocalState(int val, String userId){
        editor.putInt("codegroup-" + userId, val);
        editor.commit();
    }

    public int getCodegroupLocalState(String userId){
        return pref.getInt("codegroup-" + userId, 0);
    }

    /*
   * Checking its sign up or login.  **************************************************
   */
    public void setSignUpAccount()
    {
        editor.putBoolean(SIGNUP, true);
        editor.commit();
    }

    public boolean getSignUpAccount()
    {
        boolean signup = pref.getBoolean(SIGNUP, false);
        return signup;
    }

    public void reSetSignUpAccount()
    {
        editor.putBoolean(SIGNUP, false);
        editor.commit();
    }


    public void setAlarmEventState(String eventId, boolean state){
        editor.putBoolean(eventId, state);
        editor.commit();
    }

    public boolean getAlarmEventState(String eventId){
        return pref.getBoolean(eventId, false);
    }


    //id is <username>_<tutorial_name>
    public void setTutorialState(String id, boolean state){
        editor.putBoolean(id, state);
        editor.commit();
    }

    public boolean getTutorialState(String eventId){
        return pref.getBoolean(eventId, false);
    }

  /*
   * updating current time
   * We will store delta instead of server time.
   * So next time we need time on server, server_time = local_time + delta
   */

    public void setCurrentTime(Date time) {
        Calendar now = Calendar.getInstance();
        long deviceTime = now.getTimeInMillis();
        long serverTime = time.getTime();

        long delta = serverTime - deviceTime;
        if(Config.SHOWLOG) Log.d("DEBUG_SESSION_MANAGER", "setCurrentTime delta is " + serverTime + "-" + deviceTime + "= " + delta);
        editor.putLong(TIME_DELTA, delta);
        editor.commit();
    }


    /*
      server_time = local_time + delta(stored in shared prefs)
      return value is non-null
     */
    public Date getCurrentTime(){
        long delta = pref.getLong(TIME_DELTA, 0);
        Calendar now = Calendar.getInstance();
        now.add(Calendar.MILLISECOND, (int) delta);
        return now.getTime();
    }

    /******************************************************************************************/


    /**
     * User has joined any classroom or not, anytime
     */
    public void setHasUserJoinedClass()
    {
        //variable format : username + flag
        ParseUser currentParseUser = ParseUser.getCurrentUser();
        if(currentParseUser != null) {
            editor.putBoolean(USER_HAS_JOINED_CLASS + currentParseUser.getUsername(), true);
            editor.commit();
        }
    }

    public boolean getHasUserJoinedClass()
    {
        ParseUser currentParseUser = ParseUser.getCurrentUser();
        if(currentParseUser != null) {
            return pref.getBoolean(USER_HAS_JOINED_CLASS + currentParseUser.getUsername(), false);
        }
        return false;
    }

    public boolean getBooleanValue(String key){
        return pref.getBoolean(key, false);
    }

    public void setBooleanValue(String key, Boolean value){
        editor.putBoolean(key, value);
        editor.commit();
    }

    /****************************************************************************/
    //storing compaign information


    public void setCompaignDetails(String key, String value)
    {
        editor.putString(key, value);
        editor.commit();
    }

    public String getCompaignDetails(String key)
    {
        String value = pref.getString(key, null);
        return value;
    }

    public void reSetCompaignDetails(String key)
    {
        editor.putString(key, null);
        editor.commit();
    }

    /*************************************/
    public void setDate(String key, Date value){
        Long t = value.getTime();
        editor.putLong(key, t);
        editor.commit();
    }

    public Date getDate(String key){
        Long t = pref.getLong(key, -1);
        if(t >= 0){
            Date expiry = new Date(t);
            return expiry;
        }
        return null;
    }

    /********** school input **************/
    public static final String SCHOOL_INPUT_BASE_COUNT = "school_input_base_count"; //open count when updated app first launched
    public static final String SCHOOL_INPUT_SHOW_COUNT = "school_input_show_count"; //how many times school input dialog shown

    public static final String PHONE_INPUT_BASE_COUNT = "phone_input_base_count"; //open count when updated app first launched
    public static final String PHONE_INPUT_SHOW_COUNT = "phone_input_show_count"; //how many times school input dialog shown

    public void setInteger(String key, int value){
        editor.putInt(key, value);
        editor.commit();
    }

    public int getInteger(String key){
        int value = pref.getInt(key, -1);
        return value;
    }

    /*********** app version **************/
    public static final String APP_VERSION = "app_version";
    public void setString(String key, String value){
        editor.putString(key, value);
        editor.commit();
    }

    public String getString(String key){
        String value = pref.getString(key, null);
        return value;
    }
}
