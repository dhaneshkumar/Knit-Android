package school;

import android.util.Log;

import com.parse.FunctionCallback;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseUser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;

import library.UtilString;
import utility.Utility;

/**
 * Created by ashish on 7/9/15.
 */
public class SchoolUtils {
    public static ArrayList<String> areaAutoComplete(String input) {
        if(!Utility.isInternetExistWithoutPopup()){
            return null;
        }

        ArrayList<String> locationList = new ArrayList<>();
        HashMap<String, String> parameters = new HashMap<>();

        parameters.put("partialAreaName", input);
        try {
            locationList = ParseCloud.callFunction("areaAutoComplete", parameters);
            return  locationList;
        }
        catch (ParseException e){
            e.printStackTrace();
        }

        return null;
    }

    public static ArrayList<SchoolItem> schoolsNearby(String input) {
        if(!Utility.isInternetExistWithoutPopup()){
            return null;
        }

        ArrayList<SchoolItem> resultList = new ArrayList<>();
        HashMap<String, String> parameters = new HashMap<>();

        parameters.put("areaName", input);
        try {
            Log.d("__school_utils", "calling schoolsNearby() with " + input);
            ArrayList<ArrayList<String>> schools = ParseCloud.callFunction("schoolsNearby", parameters);
            Log.d("__school_utils", "No of schools fetched " + schools.size());
            for(int i=0; i<schools.size(); i++){
                if(schools.get(i) != null && schools.get(i).size() >= 3) {
                    String schoolFullAddress = schools.get(i).get(0) + ", " + schools.get(i).get(1);
                    SchoolItem item = new SchoolItem(schools.get(i).get(0), schools.get(i).get(1), schools.get(i).get(2));
                    Log.d("__school_utils", "school " + i + " " + schoolFullAddress);
                    resultList.add(item);
                }
            }
            return resultList;
        }

        catch (ParseException e){
            e.printStackTrace();
        }

        return null;
    }

    public static class SchoolItem{
        public String name;
        public String area;
        public String placeId;

        public SchoolItem(String name, String area, String placeId){
            this.name = name;
            this.area = area;
            this.placeId = placeId;
        }
    }

    //fetch details of placeId and store in currentUser
    public static void fetchSchoolInfoFromIdIfRequired(final ParseUser currentUser) {
        if(currentUser == null || !Utility.isInternetExistWithoutPopup()){
            return;
        }

        String placeId = currentUser.getString("place_id");
        String placeName = currentUser.getString("place_name");
        String placeArea = currentUser.getString("place_area");

        if(UtilString.isBlank(placeId)){
            Log.d("__school", "fetchSchoolInfo place_id null");
            return;
        }

        //if place name and area not null, no need to do anything
        if(!UtilString.isBlank(placeName) && !UtilString.isBlank(placeArea)){
            Log.d("__school", "fetchSchoolInfo place_name and place_area already present");
            return;
        }

        HashMap<String, String> parameters = new HashMap<>();

        parameters.put("place_id", placeId);
        Log.d("__school_utils", "calling placeInfo() with " + placeId);
        ParseCloud.callFunctionInBackground("getSchoolDetail", parameters, new FunctionCallback<Object>() {
            @Override
            public void done(Object response, ParseException e) {
                if (e == null && response != null && response instanceof ArrayList){
                    ArrayList<String> result = (ArrayList<String>) response;
                    if(result.size() >= 2 && !UtilString.isBlank(result.get(0)) && !UtilString.isBlank(result.get(1))){
                        currentUser.put("place_name", result.get(0));
                        currentUser.put("place_area", result.get(1));
                        currentUser.pinInBackground();
                        Log.d("__school", "fetchSchoolInfo success " + currentUser.getString("place_name") + ", " + currentUser.getString("place_area"));
                    }
                }
                else{
                    e.printStackTrace();
                }
            }
        });
    }
}
