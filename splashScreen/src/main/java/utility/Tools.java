package utility;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import com.parse.ParseUser;

import fr.castorflex.android.smoothprogressbar.SmoothProgressBar;

import trumplabs.schoolapp.MainActivity;

import BackGroundProcesses.Inbox;
import BackGroundProcesses.Refresher;
import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ProgressBar;

public class Tools {

  /*
   * add a new group to jsonArray and stores again in string format
   */
  public static String addToJSONArry(String groups, String code, String name) {
    // Utility.ls("intial group : " + groups);

    JSONArray groupArray;
    if (groups == null) {
      groupArray = new JSONArray();
    } else
      groupArray = (JSONArray) JSONValue.parse(groups);
    // convert to jsonArray


    if (groupArray == null)
      groupArray = new JSONArray();
    // Creating a new json object
    JSONObject json = new JSONObject();
    json.put("code", code.trim());
    json.put("name", name.trim());


    for (int i = 0; i < groupArray.size(); i++) {
      if (((JSONObject) groupArray.get(i)).get("code").equals(code.trim()))
        return groupArray.toJSONString();

    }

    groupArray.add(json);

    // Utility.ls("final group : " + groupArray.toJSONString());
    // returning as string
    return groupArray.toJSONString();
  }


  /*
   * add a child/school to jsonArray and stores again in string format
   */
  public static String addchildToJSON(String groups, List<String> childList) {

    if (childList == null)
      return groups;

    JSONArray groupArray;
    if (groups == null) {
      groupArray = new JSONArray();
    } else
      groupArray = (JSONArray) JSONValue.parse(groups);
    // convert to jsonArray


    if (groupArray == null)
      groupArray = new JSONArray();
    // Creating a new json object
    JSONObject json = new JSONObject();
    json.put("childName", childList.get(0).trim());

    if (childList.size() > 1)
      json.put("schoolName", childList.get(1).trim());


    for (int i = 0; i < groupArray.size(); i++) {
      if (((JSONObject) groupArray.get(i)).get("childName").equals(childList.get(0).trim()))
        return groupArray.toJSONString();

    }

    groupArray.add(json);

    // Utility.ls("final group : " + groupArray.toJSONString());
    // returning as string
    return groupArray.toJSONString();
  }



  /*
   * add a new group to jsonArray and stores again in string format
   */
  public static String addChildToJSONArry(String groups, String code, String name,
      String[] childList) {
    // Utility.ls("intial group : " + groups);

    JSONArray groupArray;
    if (groups == null) {
      groupArray = new JSONArray();
    } else
      groupArray = (JSONArray) JSONValue.parse(groups);
    // convert to jsonArray


    if (groupArray == null)
      groupArray = new JSONArray();
    // Creating a new json object
    JSONObject json = new JSONObject();
    json.put("code", code.trim());
    json.put("name", name.trim());

    if (childList != null) {
      for (int k = 0; k < childList.length; k++) {
        String childno = "child" + k;
        json.put(childno, childList[k]);
      }
    }

    Log.d("tools", "child name added :  --- " + json.toString());

    /*
     * for (int i = 0; i < groupArray.size(); i++) { if (((JSONObject)
     * groupArray.get(i)).get("code").equals(code.trim())) return groupArray.toJSONString();
     * 
     * }
     */

    groupArray.add(json);

    // Utility.ls("final group : " + groupArray.toJSONString());
    // returning as string
    return groupArray.toJSONString();
  }


  /*
   * remove a group to jsonArray and stores again in string format
   */
  public static String deleteFromJSONArry(String groups, String code) {
    // Utility.ls("intial group : " + groups);

    // convert to jsonArray
    JSONArray groupArray = (JSONArray) JSONValue.parse(groups);

    if (groupArray == null)
      return groups;

    int index = -1;
    for (int i = 0; i < groupArray.size(); i++) {

      JSONObject obj = (JSONObject) groupArray.get(i);

      if (obj.get("code").equals(code)) {
        index = i;
        break;
      }
    }

    if (index != -1) {
      groupArray.remove(index);
    }

    // Utility.ls("final group : " + groupArray.toJSONString());
    // returning as string
    return groupArray.toJSONString();
  }


  /*
   * remove a group to jsonArray and stores again in string format
   */
  public static String deleteChildFromJSON(String groups, String childName) {
    // Utility.ls("intial group : " + groups);

    // convert to jsonArray
    JSONArray groupArray = (JSONArray) JSONValue.parse(groups);

    if (groupArray == null)
      return groups;

    int index = -1;
    for (int i = 0; i < groupArray.size(); i++) {

      JSONObject obj = (JSONObject) groupArray.get(i);

      if (obj.get("childName").equals(childName)) {
        index = i;
        break;
      }
    }

    if (index != -1) {
      groupArray.remove(index);
    }

    // Utility.ls("final group : " + groupArray.toJSONString());
    // returning as string
    return groupArray.toJSONString();
  }

  /*
   * convert json array to list
   */
  public static List<List<String>> convertJsonArrayToList(String groups) {
    List<List<String>> groupList = new ArrayList<List<String>>();

    if (groups == null)
      return groupList;


    // convert to jsonArray
    JSONArray groupArray = (JSONArray) JSONValue.parse(groups);

    if (groupArray == null) {
      groupArray = new JSONArray();

    }

    int index = -1;
    for (int i = 0; i < groupArray.size(); i++) {

      JSONObject obj = (JSONObject) groupArray.get(i);

      List<String> groupData = new ArrayList<String>();
      groupData.add(obj.get("code").toString());
      groupData.add(obj.get("name").toString());
      int length = obj.size();

      if (length > 2) {
        for (int k = 0; k < length; k++) {
          String child = "child" + k;

          if (obj.get(child) != null)
            groupData.add(obj.get(child).toString());
        }
      }

      groupList.add(groupData);
    }

    return groupList;

  }



  /*
   * convert json array to list
   */
  public static List<List<String>> convertJsonToChildList(String groups) {
    List<List<String>> groupList = new ArrayList<List<String>>();

    if (groups == null)
      return groupList;

    Log.d("profile", "group : " + groups);

    // convert to jsonArray
    JSONArray groupArray = (JSONArray) JSONValue.parse(groups);

    if (groupArray == null) {
      groupArray = new JSONArray();

    }

    int index = -1;
    for (int i = 0; i < groupArray.size(); i++) {

      JSONObject obj = (JSONObject) groupArray.get(i);

      List<String> groupData = new ArrayList<String>();
      groupData.add(obj.get("childName").toString());

      if (obj.get("schoolName") != null)
        groupData.add(obj.get("schoolName").toString());


      groupList.add(groupData);
    }

    return groupList;

  }

  /*
   * convert chilren array to list
   */
  public static List<List<String>> convertChildrenStringToList(String groups) {
    List<List<String>> groupList = new ArrayList<List<String>>();

    if (groups == null)
      return groupList;

    // convert to jsonArray
    JSONArray groupArray = (JSONArray) JSONValue.parse(groups);

    if (groupArray == null) {
      groupArray = new JSONArray();

    }

    int index = -1;
    for (int i = 0; i < groupArray.size(); i++) {


      JSONObject obj = (JSONObject) groupArray.get(i);

      List<String> groupData = new ArrayList<String>();
      groupData.add(obj.get("childName").toString());
      if (obj.get("schoolName") != null)
        groupData.add(obj.get("schoolName").toString());

      groupList.add(groupData);
    }

    return groupList;

  }


  /*
   * hide keyboard
   */

  public static void hideKeyboard(Activity currentActiviry) {
    InputMethodManager inputManager =
        (InputMethodManager) currentActiviry.getSystemService(Context.INPUT_METHOD_SERVICE);

    inputManager.hideSoftInputFromWindow(currentActiviry.getCurrentFocus().getWindowToken(),
        InputMethodManager.HIDE_NOT_ALWAYS);

    currentActiviry.getWindow()
        .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
  }


  /*
   * Setting counter to update
   */
  public static void updateMsgs() {
    final Handler handler = new Handler();
    Timer timer = new Timer();
    TimerTask doAsynchronousTask = new TimerTask() {
      @Override
      public void run() {
        handler.post(new Runnable() {
          public void run() {
            try {

              new Refresher(2);
            } catch (Exception e) {
            }
          }
        });
      }
    };
    timer.schedule(doAsynchronousTask, 0, 900000); // execute in every 15min
  }

  
  
  /*
   * make visible smooth progress bar for certain interval
   */
  public static void runSmoothProgressBar(final SmoothProgressBar progressBar, final int seconds) {

    if(progressBar == null)
      return ;
    
    progressBar.setVisibility(View.VISIBLE);
    progressBar.setIndeterminate(true);
    
    /*
     * stop refreshing progress bar
     */
    final Handler h = new Handler() {
      @Override
      public void handleMessage(Message message) {
        progressBar.setVisibility(View.GONE);
      }     
    };
    h.sendMessageDelayed(new Message(), seconds*1000);

  }

  /*
   * make visible  progress bar for certain interval
   */
  public static void runProgressBar(final ProgressBar progressBar, final int seconds) {

    if(progressBar == null)
      return ;
    
    progressBar.setVisibility(View.VISIBLE);
    progressBar.setIndeterminate(true);
    
    /*
     * stop refreshing progress bar after some interval
     */
    final Handler h = new Handler() {
      @Override
      public void handleMessage(Message message) {
        progressBar.setVisibility(View.GONE);
      }     
    };
    h.sendMessageDelayed(new Message(), seconds * 1000);

  }

}
