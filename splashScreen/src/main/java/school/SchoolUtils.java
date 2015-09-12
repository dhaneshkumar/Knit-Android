package school;

import android.util.Log;

import com.parse.ParseCloud;
import com.parse.ParseException;

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

/**
 * Created by ashish on 7/9/15.
 */
public class SchoolUtils {
    public static ArrayList<String> areaAutoComplete(String input) {
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
}
