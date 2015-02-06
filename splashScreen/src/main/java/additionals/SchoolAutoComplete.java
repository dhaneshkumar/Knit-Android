package additionals;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Filter;
import android.widget.Filterable;

import com.parse.ParseCloud;
import com.parse.ParseException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by ashish on 24/1/15.
 */

public class SchoolAutoComplete {
    private static final String LOG_TAG = "ExampleApp";

    private static final String PLACES_API_BASE = "https://maps.googleapis.com/maps/api/place";

    private static final String TYPE_AUTOCOMPLETE = "/autocomplete";
    private static final String OUT_JSON = "/json";
    private static final String LOCATION_TYPE = "geocode";

    private static final String API_KEY = "AIzaSyCZ5_QxsDDJMaiCUCHDZp2A-OA_AnTYm74";

    private static ArrayList<String> areaAutoComplete(String input) {
        ArrayList<String> resultList = new ArrayList<>();
        HashMap<String, String> parameters = new HashMap<String, String>();

        parameters.put("partialAreaName", input);
        try {
            ArrayList<String> updatedmsg = ParseCloud.callFunction("areaAutoComplete", parameters);
            return  updatedmsg;
        }
        catch (ParseException e){
            e.printStackTrace();
        }

        return resultList;
    }

    public static ArrayList<String> schoolsNearby(String input) {
        ArrayList<String> resultList = new ArrayList<>();
        HashMap<String, String> parameters = new HashMap<String, String>();

        parameters.put("areaName", input);
        try {
            Log.d("DEBUG_SCHOOL_AUTOCOMPLETE", "calling schoolsNearby() with " + input);
            ArrayList<ArrayList<String>> schools = ParseCloud.callFunction("schoolsNearby", parameters);
            Log.d("DEBUG_SCHOOL_AUTOCOMPLETE", "No of schools fetched " + schools.size());
            for(int i=0; i<schools.size(); i++){
                String schoolFullAddress = schools.get(i).get(0) + " " + schools.get(i).get(1);
                Log.d("DEBUG_SCHOOL_AUTOCOMPLETE", "school " + i + " " + schoolFullAddress);
                resultList.add(schoolFullAddress);
            }
            return resultList;
        }

        catch (ParseException e){
            e.printStackTrace();
        }

        return resultList;
    }

    public static class PlacesAutoCompleteAdapter extends ArrayAdapter<String> implements Filterable {
        private ArrayList<String> resultList;

        public PlacesAutoCompleteAdapter(Context context, int layout, int textViewResourceId) {
            super(context, layout, textViewResourceId);
        }

        @Override
        public int getCount() {
            return resultList.size();
        }

        @Override
        public String getItem(int index) {
            return resultList.get(index);
        }

        @Override
        public Filter getFilter() {
            Filter filter = new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults filterResults = new Filter.FilterResults();
                    if (constraint != null) {
                        // Retrieve the autocomplete results.
                        resultList = areaAutoComplete(constraint.toString());

                        // Assign the data to the FilterResults
                        filterResults.values = resultList;
                        filterResults.count = resultList.size();
                    }
                    return filterResults;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    if (results != null && results.count > 0) {
                        notifyDataSetChanged();
                    }
                    else {
                        notifyDataSetInvalidated();
                    }
                }};
            return filter;
        }
    }

    public static class DelayAutoCompleteTextView extends AutoCompleteTextView {
        public DelayAutoCompleteTextView(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        private final Handler mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                DelayAutoCompleteTextView.super.performFiltering((CharSequence) msg.obj, msg.arg1);
            }
        };

        @Override
        protected void performFiltering(CharSequence text, int keyCode) {
            mHandler.removeMessages(0);
            mHandler.sendMessageDelayed(mHandler.obtainMessage(0, keyCode, 0, text), 750);
        }
    }
}
