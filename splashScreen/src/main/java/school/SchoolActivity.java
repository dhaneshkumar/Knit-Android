package school;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Filter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.parse.FunctionCallback;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseUser;

import org.slf4j.helpers.Util;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import baseclasses.MyActionBarActivity;
import profileDetails.ProfilePage;
import trumplab.textslate.R;
import utility.Tools;
import utility.Utility;

/**
 * Created by ashish on 10/9/15.
 */
public class SchoolActivity extends MyActionBarActivity{

    final static String LOGTAG = "__sv_activity";

    CustomSearchView locSV;
    ListView locLV;
    LocationAdapter locAdapter;

    CustomSearchView schoolSV;
    ListView schoolLV;
    SchoolAdapter schoolAdapter;

    ProgressBar loadingSchoolsPB;

    TextView selectedSchoolTV;
    LinearLayout schoolHolderLL;
    Button updateSchoolButton;

    String selectedSchoolName;
    String selectedSchoolPlaceId;
    String selectedSchoolArea;

    ProgressDialog pdialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.school_activity);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        loadingSchoolsPB = (ProgressBar) findViewById(R.id.loading_schools);

        locSV = (CustomSearchView) findViewById(R.id.locSV);
        locSV.setQueryHint("Where is the school?");

        locLV = (ListView) findViewById(R.id.locLV);
        locAdapter = new LocationAdapter(this);
        locLV.setAdapter(locAdapter);

        locSV.setParameters(500L, 3, locLV, locAdapter);

        final WeakReference<SearchViewAdapterInterface> locAdapterRef = new WeakReference<SearchViewAdapterInterface>(locAdapter);
        final WeakReference<ProgressBar> progressBarRef = new WeakReference<ProgressBar>(loadingSchoolsPB);
        locSV.setHandler(locAdapterRef, progressBarRef);

        locSV.setListItemOnClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                Tools.hideKeyboard(SchoolActivity.this);
                final String value = (String) locAdapter.getItem(position);
                //Utility.toast(locAdapter.getStringDescription(position), 15);

                hideSchoolHolder();

                loadingSchoolsPB.setVisibility(View.VISIBLE);

                new AsyncTask<Void, Void, Void>() {
                    ArrayList<SchoolUtils.SchoolItem> schools;
                    boolean error = false;

                    @Override
                    protected Void doInBackground(Void... voids) {
                        schools = SchoolUtils.schoolsNearby(value);
                        if (schools == null) {
                            error = true;
                        }
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void result) {
                        loadingSchoolsPB.setVisibility(View.GONE); //hide progress bar

                        if (error) {
                            Utility.toast("Error fetching schools...");
                        }
                        else if(schools.size() == 0) {
                            Utility.toast("No results to display");
                        }
                        else{
                            schoolAdapter.setOriginalItemList(schools);
                            schoolSV.setQuery("", false);
                            schoolSV.setVisibility(View.VISIBLE); //finally show school list box
                        }
                        return;
                    }
                }.execute();
            }
        });

        locSV.setQueryTextChangeRunnable(new Runnable() {
            @Override
            public void run() {
                hideSchoolHolder();
            }
        });


        schoolSV = (CustomSearchView) findViewById(R.id.schoolSV);
        schoolSV.setQueryHint("Find your school");

        schoolLV = (ListView) findViewById(R.id.schoolLV);
        schoolAdapter = new SchoolAdapter(this, new ArrayList<SchoolUtils.SchoolItem>());
        schoolLV.setAdapter(schoolAdapter);

        schoolSV.setParameters(200L, 2, schoolLV, schoolAdapter);

        WeakReference<SearchViewAdapterInterface> schoolAdapterRef = new WeakReference<SearchViewAdapterInterface>(schoolAdapter);
        schoolSV.setHandler(schoolAdapterRef, null);

        schoolSV.setListItemOnClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                //Utility.toast(schoolAdapter.getStringDescription(position), 15);
                selectedSchoolTV.setText("You selected :\n" + schoolAdapter.getStringDescription(position));

                selectedSchoolName = ((SchoolUtils.SchoolItem) schoolAdapter.getItem(position)).name;
                selectedSchoolArea = ((SchoolUtils.SchoolItem) schoolAdapter.getItem(position)).area;
                selectedSchoolPlaceId = ((SchoolUtils.SchoolItem) schoolAdapter.getItem(position)).placeId;

                schoolHolderLL.setVisibility(View.VISIBLE);
                Tools.hideKeyboard(SchoolActivity.this);
            }
        });

        selectedSchoolTV = (TextView) findViewById(R.id.selectedSchoolTV);
        schoolHolderLL = (LinearLayout) findViewById(R.id.schoolHolder);
        updateSchoolButton = (Button) findViewById(R.id.updateSchoolButton);
        updateSchoolButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!Utility.isInternetExistWithoutPopup()){
                    Utility.toast("No Internet Connection", false, 17, true);
                    return;
                }

                pdialog = new ProgressDialog(SchoolActivity.this);
                pdialog.setCancelable(false);
                pdialog.setCanceledOnTouchOutside(false);
                pdialog.setMessage("Updating school...");
                pdialog.show();

                Map<String, String> params = new HashMap<>();
                params.put("place_id", selectedSchoolPlaceId);
                ParseCloud.callFunctionInBackground("updateSchoolId", params, new FunctionCallback<Boolean>() {
                    @Override
                    public void done(Boolean result, ParseException e) {
                        if(pdialog != null){
                            pdialog.dismiss();
                            pdialog = null;
                        }

                        if (e == null && result) {
                            ParseUser currentUser = ParseUser.getCurrentUser();
                            if(currentUser != null){
                                currentUser.put("place_id", selectedSchoolPlaceId);
                                currentUser.put("place_name", selectedSchoolName);
                                currentUser.put("place_area", selectedSchoolArea);
                                currentUser.pinInBackground();
                                Utility.toast("School updated !");
                                if(ProfilePage.schoolNameTV != null){
                                    ProfilePage.schoolNameTV.setText(selectedSchoolName + "\n" + selectedSchoolArea);
                                }
                            }
                            else{
                                Utility.LogoutUtility.logout();
                            }

                            if(SchoolActivity.this != null){
                                SchoolActivity.this.finish();
                            }
                        }
                        else {
                            if(!Utility.LogoutUtility.checkAndHandleInvalidSession(e)) {
                                Utility.toast("School update failed ! Try again");
                            }
                        }
                    }
                });
            }
        });
    }

    void hideSchoolHolder(){
        schoolSV.setVisibility(View.GONE);
        schoolLV.setVisibility(View.GONE);
    }

    public class LocationAdapter extends SearchViewAdapterInterface{
        private List<String> itemList = new ArrayList<>();
        private Context context;
        private List<String> tempItemList;
        //private ArrayList<String> placeIdList;

        public LocationAdapter(Context context){
            this.context = context;
        }

        @Override
        public int getCount() {
            return itemList.size();
        }

        @Override
        public Object getItem(int index) {
            return itemList.get(index);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater vi = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View row = vi.inflate(R.layout.school_item, parent, false);

            TextView nameTV = (TextView) row.findViewById(R.id.name);
            TextView areaTV = (TextView) row.findViewById(R.id.area);

            String[] tokens = itemList.get(position).split(",", 2);
            nameTV.setText(tokens[0]);
            if(tokens.length > 1) {
                areaTV.setText(tokens[1].trim());
            }
            else{
                areaTV.setText("");
            }
            return row;
        }

        @Override
        public Filter getFilter() {
            Filter filter = new Filter() {
                boolean error = false;
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    tempItemList = null;
                    Log.d(LOGTAG, "loc performFiltering");
                    FilterResults filterResults = new FilterResults();
                    if (constraint != null) {
                        // Retrieve the autocomplete results.
                        tempItemList = SchoolUtils.areaAutoComplete(constraint.toString());
                        if(tempItemList == null){
                            error = true;
                        }
                        else {
                            // Assign the data to the FilterResults
                            filterResults.values = tempItemList;
                            filterResults.count = tempItemList.size();
                        }
                    }
                    return filterResults;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    loadingSchoolsPB.setVisibility(View.GONE);
                    if(error){
                        Utility.toast("Error fetching areas");
                        return;
                    }

                    if (results != null && results.count >= 0) {
                        if(results.count == 0){
                            Utility.toast("No results to display");
                        }
                        itemList = tempItemList;
                        Log.d(LOGTAG, "loc results.count=" + results.count);
                        notifyDataSetChanged();
                    }
                    else {
                        notifyDataSetInvalidated();
                    }

                    if(!locSV.getItemJustSelected()) {//don't show list again if some item just selected before and no further text typed
                        locLV.setVisibility(View.VISIBLE);
                    }
                }};
            return filter;
        }

        public String getStringDescription(int position){
            if(position >= 0 && position < itemList.size()){
                return itemList.get(position);
            }
            return "Unknown";
        }
    }

    public class SchoolAdapter extends SearchViewAdapterInterface{
        private List<SchoolUtils.SchoolItem> originalItemList = new ArrayList<>();
        private List<SchoolUtils.SchoolItem> itemList = new ArrayList<>();
        private List<SchoolUtils.SchoolItem> tempItemList;
        private Context context;
        //private ArrayList<String> placeIdList;

        public SchoolAdapter(Context context, ArrayList<SchoolUtils.SchoolItem> originalItemList){
            this.context = context;
            this.originalItemList = originalItemList;
        }

        @Override
        public int getCount() {
            return itemList.size();
        }

        @Override
        public Object getItem(int index) {
            if(index < 0 || index >= itemList.size()){
                return new SchoolUtils.SchoolItem("unknown", "unknown", "unknown");
            }
            return itemList.get(index);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        public void setOriginalItemList(List<SchoolUtils.SchoolItem> originalItemList){
            Log.d(LOGTAG, "setOriginalItemList size=" + originalItemList.size());
            this.originalItemList = originalItemList;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater vi = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View row = vi.inflate(R.layout.school_item, parent, false);

            TextView nameTV = (TextView) row.findViewById(R.id.name);
            TextView areaTV = (TextView) row.findViewById(R.id.area);

            SchoolUtils.SchoolItem item = itemList.get(position);
            nameTV.setText(item.name);
            areaTV.setText(item.area);
            return row;
        }

        @Override
        public Filter getFilter() {
            Filter filter = new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    tempItemList = null;
                    Log.d(LOGTAG, "school performFiltering among " + originalItemList.size() + " items");
                    FilterResults filterResults = new FilterResults();
                    if (constraint != null) {
                        // Retrieve the autocomplete results.
                        tempItemList = new ArrayList<>();
                        for(SchoolUtils.SchoolItem val : originalItemList){
                            String schoolDesc = val.name + ", " + val.area;

                            if(schoolDesc.toLowerCase().contains(constraint.toString().toLowerCase())){
                                tempItemList.add(val);
                            }
                        }

                        filterResults.values = tempItemList;
                        filterResults.count = tempItemList.size();
                    }

                    return filterResults;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    if (results != null && results.count > 0) {
                        itemList = tempItemList;
                        Log.d(LOGTAG, "school results.count=" + results.count);
                        notifyDataSetChanged();
                    }
                    else {
                        notifyDataSetInvalidated();
                    }

                    if(!schoolSV.getItemJustSelected()) {
                        schoolLV.setVisibility(View.VISIBLE);
                    }
                }};
            return filter;
        }

        public String getStringDescription(int position){
            if(position >= 0 && position < itemList.size()){
                return itemList.get(position).name + ", " + itemList.get(position).area;
            }
            return "Unknown";
        }
    }

    @Override
    public void onBackPressed(){
        super.onBackPressed();
        Log.d(LOGTAG, "onBackPressed called");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case android.R.id.home:
                onBackPressed();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
