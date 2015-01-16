package utility;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Build;

import com.parse.ParseObject;
import com.parse.ParseQuery;

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
  public static final String CURRENT_TIME = "current_time";
  public static final String APP_OPENING_COUNT = "app_opening_count";
  public static final String SIGNUP = "signUP";
  public static final String CHILD_NAME_LIST ="childNameList";
  public static final String DEFAULT_CLASS_EXIST = "defaultClasExist";

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
  
  /*
   * updating current time
   */
  
  public void setCurrentTime(Date time) {
    SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

    String currentDate = df.format(time);

    editor.putString(CURRENT_TIME, currentDate);
    // commit changes
    editor.commit();

  }


  public Date getCurrentTime() throws ParseException {
    SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

    String time = pref.getString(CURRENT_TIME, null);

    if (time == null)
      return null;

    Date date = df.parse(time);
    return date;
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
}
