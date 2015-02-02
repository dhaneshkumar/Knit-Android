package trumplabs.schoolapp;

import android.content.Intent;
import android.util.Log;

import com.parse.ParseException;
import com.parse.ParseObject;

import org.json.JSONException;
import org.json.JSONObject;

import baseclasses.MyActivity;

public class ReceiveMsg extends MyActivity{

	
	private void examineIntent(Intent intent) throws ParseException
    {
        String v= "1111";
        
        try 
        {
            String action = intent.getAction();
            String channel = intent.getExtras().getString("com.parse.Channel");
            JSONObject json = new JSONObject(intent.getExtras().getString("com.parse.Data"));
       
            //v= "got action " + action + " on channel " + channel + " with:\n";
            
          /*  Iterator itr = json.keys();
            while (itr.hasNext()) {
              String key = (String) itr.next();
              v += "..." + key + " => " + json.getString(key)+"\n";
            }*/
            
            //storing parse object in local database
            ParseObject messages =  new ParseObject("messages");
            messages.put("groupName",  json.getString("groupName"));
            messages.put("sender", json.getString("sender"));
            messages.put("timStamp", json.getString("timeStamp"));
            messages.put("msgs", json.getString("msg"));
            
            messages.pin();
            
            //Refresh the list adapter
            
          	} 
        	catch (JSONException e) {
        	  v+="**********ERROR**********";
          
          }
     
        Log.d("mainActivity", "JSONException: ");
    }
	
	
}
