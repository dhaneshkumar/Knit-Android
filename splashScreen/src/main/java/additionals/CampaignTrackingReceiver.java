package additionals;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.parse.ParseAnalytics;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseUser;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import trumplabs.schoolapp.Application;
import utility.SessionManager;

/**
 * Receiver to track installation source (fb, google)
 *  Created by dhanesh on 17/8/15.
 */

public class CampaignTrackingReceiver extends BroadcastReceiver {
    public static final String REFERRER = "REF";
    public static final String BUNDLE = "bundle";

    public static final String CAMPAIGN_NAME = "utm_campaign";
    public static final String CAMPAIGN_SOURCE = "utm_source";
    public static final String CAMPAIGN_MEDIUM = "utm_medium";
    public static final String CAMPAIGN_TERM = "utm_term";
    public static final String CAMPAIGN_CONTENT = "utm_content";

    public static final String[] sources = {
            CAMPAIGN_NAME, CAMPAIGN_SOURCE, CAMPAIGN_MEDIUM, CAMPAIGN_TERM, CAMPAIGN_CONTENT
    };


    private String bundleData;
    private String referrerData;

    @Override
    public void onReceive(Context context, Intent intent) {

        Bundle extras = intent.getExtras();

        if(extras == null){
            return;
        }

        String referrerString = extras.getString("referrer");
        if(referrerString != null) {
            Log.d(REFERRER, "Query String : " + referrerString);

            referrerData = referrerString;

            //SessionManager.getInstance().setCompaignDetails(REFERRER, referrerString);

           /* try {
                Map<String, String> getParams = getHashMapFromQuery(referrerString);

                for (String sourceType : sources) {
                    String source = getParams.get(sourceType);

                    if (source != null) {

                        Log.d(REFERRER, sourceType + " : " + source);

                        //Storing compaign details in shared preferences
                        SessionManager.getInstance().setCompaignDetails(sourceType, source);
                    } else
                        Log.d(REFERRER, sourceType + " : null");
                }

                //sending compaign info to analytics
                sendCompaignDetailsToServer(null);

            } catch (UnsupportedEncodingException e) {

                Log.e(REFERRER, e.getMessage());
            }*/
        }


        /*******************************************TO REMOVE***********************************/

        Bundle bundle = extras;


        if (bundle != null) {

            String data = "";

            for (String key : bundle.keySet()) {
                data += "  " + key + " -> " + bundle.get(key) + ";";
            }

            bundleData = data;
            // sessionManager.setCompaignDetails(BUNDLE, data);
        }

        //setting parameters
        final HashMap<String, Object> params = new HashMap<String, Object>();

        if(referrerData != null)
            params.put("Referrer", referrerData);

        if(bundleData != null)
            params.put("Bundle", bundleData);



        if(bundleData != null || referrerData != null)
        {
            //sending data to server
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    try {
                        ParseCloud.callFunction("saveAdCampaign", params);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
            };
            Thread t = new Thread(r);
            t.setPriority(Thread.MIN_PRIORITY);
            t.start();

        }
    }


    /**
     * Decoding string into different compaign parameters
     * @param query
     * @return
     * @throws UnsupportedEncodingException
     */
    public static Map<String, String> getHashMapFromQuery(String query)
            throws UnsupportedEncodingException {

        Map<String, String> query_pairs = new LinkedHashMap<String, String>();

        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"),
                    URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
        }
        return query_pairs;
    }

    /**
     * sending compaign info for analytics
     * if user null : app installed
     * else user signuped into app
     * @param user
     */
    public static void sendCompaignDetailsToServer(ParseUser user)
    {
        Map<String, String> dimensions = new HashMap<String, String>();
        if(user == null)
        {
            for (String key : sources) {
                String value = SessionManager.getInstance().getCompaignDetails(key);
                if(value != null)
                    dimensions.put(key, value);
            }

            if(dimensions.size()>0)
                ParseAnalytics.trackEvent("Installation_Source", dimensions);
        }
        else
        {
            for (String key : sources) {
                String value = SessionManager.getInstance().getCompaignDetails(key);
                if(value != null)
                    dimensions.put(key, value);
            }

            if(dimensions.size()>0)
                ParseAnalytics.trackEvent("Signup_Source", dimensions);
        }
    }

    /**
     * Deleting data from shared preferences
     */
    public static void reSetCompaignDetails(){
        for (String key : sources) {
            SessionManager.getInstance().reSetCompaignDetails(key);
        }
    }
}
