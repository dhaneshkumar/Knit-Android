package utility;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Build;
import android.util.Log;

import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import library.UtilString;
import trumplabs.schoolapp.Constants;

@SuppressLint("SimpleDateFormat")
public class SessionManager {
  // Shared Preferences
  public static SharedPreferences pref;

  // Editor for Shared preferences
  Editor editor;

  // Context
  Context _context;

  // Shared pref mode
  int PRIVATE_MODE = 0;

  private static final String PREF_NAME = "AndroidHivePref";
  public static final String APP_OPENING_COUNT = "app_opening_count";
  public static final String SIGNUP = "signUP";
  public static final String CHILD_NAME_LIST ="childNameList";
  public static final String DEFAULT_CLASS_EXIST = "defaultClasExist";
  public static final String TIME_DELTA = "time_delta";
  public static final String ACTIONBAR_HEIGHT = "actionBarHeight";

  public SessionManager() {}


  public SessionManager(Context c) {
    this._context = c;
    pref = _context.getSharedPreferences(PREF_NAME, PRIVATE_MODE);
    editor = pref.edit();
  }

  
  /*
   * maintaining child list
   */
  @TargetApi(Build.VERSION_CODES.HONEYCOMB)
  public void addChildName(String childNmae)
  {
    Set<String> childSet = pref.getStringSet(CHILD_NAME_LIST, null);
    
    if(childSet == null)
      childSet = new HashSet<String>();
    
    childSet.add(childNmae);
    editor.putStringSet(CHILD_NAME_LIST, childSet);
    editor.commit();
    
  }

  @TargetApi(Build.VERSION_CODES.HONEYCOMB)
  public List<String> getChildList()
  {
    Set<String> childSet = pref.getStringSet(CHILD_NAME_LIST, null);
    
    List<String> childList;
    if(childSet != null)
    {
      childList= new ArrayList<String>(childSet);
      return childList;
    }
    else
      return null;
  }
  
  
  @TargetApi(Build.VERSION_CODES.HONEYCOMB)
  public void reSetChildList()
  {
    editor.putStringSet(CHILD_NAME_LIST, null);
    editor.commit();
   
  }

    /************************************************************************/
  /*
   * keeping app opening count
   */
  public void setAppOpeningCount() {
    int openingCount = pref.getInt(APP_OPENING_COUNT, 0);
    
    openingCount++;
    editor.putInt(APP_OPENING_COUNT, openingCount);
    editor.commit();
  }

  public void reSetAppOpeningCount() {
    editor.putInt(APP_OPENING_COUNT, 0);
    editor.commit();
  }
  public int getAppOpeningCount() {
    int openingCount = pref.getInt(APP_OPENING_COUNT, 0);
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
   * Checking its sign up or login.
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
        Log.d("DEBUG_SESSION_MANAGER", "setCurrentTime delta is " + serverTime + "-" + deviceTime + "= " + delta);
        editor.putLong(TIME_DELTA, delta);
        editor.commit();
    }


    /*
      server_time = local_time + delta(stored in shared prefs)
     */
    public Date getCurrentTime() throws ParseException {
        long delta = pref.getLong(TIME_DELTA, 0);

        Calendar now = Calendar.getInstance();
        now.add(Calendar.MILLISECOND, (int)delta);
//    Log.d("DEBUG_SESSION_MANAGER", "server time is " + now.getTimeInMillis());

        return now.getTime();
    }
  
  /*
   * Check whether default group exist or not
   */

  public void setDefaultClassExtst() {

    editor.putBoolean(DEFAULT_CLASS_EXIST, true);
    editor.commit();
  }
  
  public boolean getDefaultClassExtst() {

    boolean flag= pref.getBoolean(DEFAULT_CLASS_EXIST, false);
    return flag;
  }
  
  public void reSetDefaultClassExtst() {

    editor.putBoolean(DEFAULT_CLASS_EXIST, false);
    editor.commit();

  }


    /*
  * keeping new entry in code map
  */
    public String getClassName(String code) {
        String className = pref.getString(code, null);

        if(!UtilString.isBlank(className)) return className;

        ParseQuery<ParseObject> query = ParseQuery.getQuery(Constants.CODE_GROUP);
        query.fromLocalDatastore();
        query.whereEqualTo("code", code);

        try {
            ParseObject obj = query.getFirst();

            if(obj != null)
            {
                String name = obj.getString("name");
                if(!UtilString.isBlank(name))
                {
                    editor.putString(code, name);
                    editor.commit();
                    return name;
                }
            }
        } catch (com.parse.ParseException e) {
            e.printStackTrace();
        }

     return null;
    }

/***************************************************************************************/

    /*
    * Maintaining action bar height
    */
    public void setActionBarHeight(int height) {

        editor.putInt(ACTIONBAR_HEIGHT, height);
        editor.commit();
    }

    public void reSetActionBarHeight() {
        editor.putInt(ACTIONBAR_HEIGHT, 0);
        editor.commit();
    }
    public int getActionBarHeight() {
        int version = pref.getInt(ACTIONBAR_HEIGHT, 0);
        return version;
    }
}
