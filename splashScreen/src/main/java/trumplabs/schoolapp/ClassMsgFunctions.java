package trumplabs.schoolapp;

import java.util.HashMap;

import library.UtilString;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;

import com.parse.FunctionCallback;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParsePush;

public class ClassMsgFunctions {
  
  public static void sendMessageAsData(String groupCode, String msg, int attachmentFlag, String sender,
      String groupName) {

    JSONObject data = new JSONObject();
    try {
      
      if(UtilString.isBlank(msg)) {
          data.put("msg", "Image...");
      }
        else
          data.put("msg", msg);
      /*  data.put("alert", "Image");
      else
        data.put("alert", msg);*/
      
      if (UtilString.isBlank(groupName))
        data.put("title", "Knit");
      else
        data.put("title", groupName);


      data.put("flag", attachmentFlag);
      data.put("sender", sender);
      data.put("groupName", groupName);
    } catch (JSONException x) {
      throw new RuntimeException("Something wrong with JSON", x);
    }

    ParsePush push = new ParsePush();
    push.setChannel(groupCode);
    push.setData(data);
    push.sendInBackground();
    
    
    
    HashMap<String, String> params = new HashMap<String, String>();
    params.put("classcode", groupCode);
    params.put("message", msg);
    ParseCloud.callFunctionInBackground("messagecc", params, new FunctionCallback<String>() {
      @Override
      public void done(String result, ParseException e) {

        
      }
    });
  }

  

/*
    typedmsg.addTextChangedListener(new TextWatcher() {

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {}

      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

      @Override
      public void afterTextChanged(Editable s) {
        countview.setText(s.length() + "");
        if (s.length() == 140)
          countview.setTextColor(getResources().getColor(R.color.secondarycolor));
        else
          countview.setTextColor(getResources().getColor(R.color.buttoncolor));
      }
    });*/

  /*public void showTemplateDialog(Activity activity)
  {
    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
    builder.setTitle("Enter Template");
    
    final EditText input = new EditText(MainActivity.this);  
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                          LinearLayout.LayoutParams.MATCH_PARENT,
                          LinearLayout.LayoutParams.MATCH_PARENT);
    input.setLayoutParams(lp);
    
  }*/
  
  

}
