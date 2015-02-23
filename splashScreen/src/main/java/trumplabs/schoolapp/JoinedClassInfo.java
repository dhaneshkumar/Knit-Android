package trumplabs.schoolapp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.io.File;
import java.util.HashMap;
import java.util.List;

import baseclasses.MyActionBarActivity;
import library.UtilString;
import trumplab.textslate.R;
import utility.Utility;

/**
 * Created by ashish on 22/2/15.
 */
public class JoinedClassInfo extends MyActionBarActivity {
    Context activityContext;
    ImageView profileImageView;
    TextView classNameTV;
    TextView teacherNameTV;
    TextView classCodeTV;
    ImageView whatsAppImageView;
    TextView schoolNameTV;
    TextView assignedNameTV;
    LinearLayout assignedNameContainer;

    String className;
    String classCode;
    String schoolName;
    String assignedName;
    String teacherName;

    String defaultSchoolName = "Unknown School";

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.joined_class_info);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        activityContext = this;

        //get extras
        className = getIntent().getExtras().getString("className");
        classCode = getIntent().getExtras().getString("classCode");
        assignedName = getIntent().getExtras().getString("assignedName");
        if(assignedName == null){
            assignedName = "";
        }

        //get ui elements
        profileImageView = (ImageView) findViewById(R.id.profileImage);
        classNameTV = (TextView) findViewById(R.id.className);
        teacherNameTV = (TextView) findViewById(R.id.teacherName);
        classCodeTV = (TextView) findViewById(R.id.classCode);
        whatsAppImageView = (ImageView) findViewById(R.id.whatsAppIcon);
        schoolNameTV = (TextView) findViewById(R.id.schoolName);
        assignedNameTV = (TextView) findViewById(R.id.assignedName);
        assignedNameContainer = (LinearLayout) findViewById(R.id.assignedNameContainer);

        //get details(schoolName, profile pic, assigned name) from Codegroup and User table
        ParseQuery<ParseObject> classQuery = new ParseQuery<ParseObject>("Codegroup");
        classQuery.fromLocalDatastore();
        classQuery.whereEqualTo("code", classCode);

        String teacherSenderId = null;
        String teacherSex = "";

        try{
            ParseObject codegroup = classQuery.getFirst();
            teacherName = codegroup.getString("Creator");
            teacherSenderId = codegroup.getString("senderId");
            teacherSex = codegroup.getString("sex");
            schoolName = codegroup.getString("schoolName"); //this is a new field. If not present, fetch and store locally
            if(schoolName == null){
                //fetch school name from id using asynctask
                Log.d("DEBUG_JOINED_CLASS_INFO", "schoolName not in codegroup. Fetching...");
                GetSchoolName getSchoolNameTask = new GetSchoolName(codegroup);
                getSchoolNameTask.execute();
            }
            else{
                Log.d("DEBUG_JOINED_CLASS_INFO", "schoolName already there " + schoolName);
                schoolNameTV.setText(schoolName);
            }
        }
        catch (ParseException e){
            Log.d("DEBUG_JOINED_CLASS_INFO", "local query into Codegroup failed");
            teacherName = "Unknown";
            schoolName = defaultSchoolName;
            schoolNameTV.setText(schoolName);
            e.printStackTrace();
        }

        if(teacherSenderId != null){//set profile pic
            teacherSenderId = teacherSenderId.replaceAll("@", "");
            String filePath = Utility.getWorkingAppDir() + "/thumbnail/" + teacherSenderId + "_PC.jpg";

            //Log.d("DEBUG_MESSAGES_DISPLAYING", "profile pic " + filePath);
            File teacherThumbnailFile = new File(filePath);

            if(teacherThumbnailFile.exists()){
                // image file present locally
                Bitmap mySenderBitmap = BitmapFactory.decodeFile(teacherThumbnailFile.getAbsolutePath());
                profileImageView.setImageBitmap(mySenderBitmap);
            }
            else{
                if (!UtilString.isBlank(teacherSex)) {
                    if (teacherSex.equals("M"))
                        profileImageView.setImageResource(R.drawable.maleteacherdp);
                    else if (teacherSex.equals("F"))
                        profileImageView.setImageResource(R.drawable.femaleteacherdp);
                } else {
                    // if sex is not stored. Determine using title(first word) in name
                    if (!UtilString.isBlank(teacherName)) {
                        String[] names = teacherName.split("\\s");

                        if (names != null && names.length > 1) {
                            String title = names[0].trim();

                            if (title.equals("Mr")) {
                                profileImageView.setImageResource(R.drawable.maleteacherdp);
                            } else if (title.equals("Mrs")) {
                                profileImageView.setImageResource(R.drawable.femaleteacherdp);
                            } else if (title.equals("Ms")) {
                                profileImageView.setImageResource(R.drawable.femaleteacherdp);
                            }
                        }
                    }
                }
            }
        }

        //set ui elements
        setTitle(className);
        classNameTV.setText(className);
        classCodeTV.setText(classCode);
        assignedNameTV.setText(assignedName);
        teacherNameTV.setText(teacherName);

        //on click code, copy code to clipboard
        classCodeTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Utility.copyToClipBoard(activityContext, "Class code", classCode);
            }
        });

        assignedNameContainer.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                AlertDialog.Builder alert = new AlertDialog.Builder(activityContext);

                alert.setTitle("Update your child name");

                LinearLayout layout = new LinearLayout(activityContext);
                layout.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams params =
                        new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT);
                params.setMargins(30, 30, 30, 30);

                final EditText input = new EditText(activityContext);
                input.setText(assignedName);
                layout.addView(input, params);
                alert.setView(layout);
                alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String newAssignedName = input.getText().toString();

                        if (!UtilString.isBlank(newAssignedName)) {
                            assignedName = newAssignedName;

                            if (Utility.isInternetOn(JoinedClassInfo.this)) {
                                InputMethodManager imm =
                                        (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                                imm.hideSoftInputFromWindow(input.getWindowToken(), 0);

                                ParseUser user = ParseUser.getCurrentUser();
                                if (user != null) {
                                    //update assigned name for this class
                                    List<List<String>> joinedGroups = user.getList(Constants.JOINED_GROUPS);
                                    if(joinedGroups != null){
                                        for(int i=0; i<joinedGroups.size(); i++){
                                            List<String> group = joinedGroups.get(i);
                                            if(group.get(0).equals(classCode)){
                                                Log.d("DEBUG_JOINED_CLASS_INFO", "changing assigned name to " + newAssignedName);
                                                //remove the last entry(child name) of group and put new name there
                                                if(group.size() >= 3){
                                                    group.remove(2);
                                                    group.add(newAssignedName);
                                                    user.saveEventually();
                                                    assignedNameTV.setText(newAssignedName);

                                                    Utility.toast("Child name update successful");

                                                    //notify Classrooms joined groups adapter
                                                    Classrooms.joinedGroups = joinedGroups;
                                                    Classrooms.joinedClassAdapter.notifyDataSetChanged();

                                                    return;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            else{
                                Utility.toast("Check you internet connection !");
                            }
                            Log.d("DEBUG_JOINED_CLASS_INFO", "FAIL : Updating child name. Something wrong happened");
                        }
                    }
                });

                alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.dismiss();
                    }
                });

                alert.show();
            }
        });

        whatsAppImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(activityContext);
                builder.setMessage("Share via WhatsApp ?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {

                            PackageManager pm = getPackageManager();
                            try {
                                pm.getPackageInfo("com.whatsapp", PackageManager.GET_ACTIVITIES);
                                Intent sendIntent = new Intent(Intent.ACTION_SEND);
                                sendIntent.setPackage("com.whatsapp");
                                sendIntent.setType("text/plain");
                                String trimSchoolName = schoolName.substring(0, 50);
                                trimSchoolName = trimSchoolName + "...";
                                sendIntent.putExtra(Intent.EXTRA_TEXT, "I have joined " + className +
                                        " class(code " + classCode + ") on KNIT App by " + teacherName + " of " + trimSchoolName + ". Please join this class ! ");
                                startActivity(sendIntent);

                            } catch (PackageManager.NameNotFoundException e) {
                                e.printStackTrace();
                                Utility.toast("WhatsApp not installed !");
                            }
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                        }
                    });
                builder.create().show();
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();

        inflater.inflate(R.menu.joined_class_info_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.unsubscribe:
                unSubscribe();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    void unSubscribe(){
        if (Utility.isInternetOn(JoinedClassInfo.this)) {

            ParseUser user = ParseUser.getCurrentUser();
            if (user != null) {
                //update assigned name for this class
                List<List<String>> joinedGroups = user.getList(Constants.JOINED_GROUPS);
                if(joinedGroups != null){
                    int targetIndex = -1; //group index to remove from joined_groups
                    for(int i=0; i<joinedGroups.size(); i++){
                        List<String> group = joinedGroups.get(i);
                        if(group.get(0).equals(classCode)){
                            targetIndex = i;
                        }
                    }
                    if(targetIndex != -1){
                        joinedGroups.remove(targetIndex); //remove this class
                        user.saveEventually();

                        Utility.toast("Successfully unsubscribed");
                        //notify Classrooms joined groups adapter
                        Classrooms.joinedGroups = joinedGroups;
                        Classrooms.joinedClassAdapter.notifyDataSetChanged();

                        finish(); //close this activity to go back to MainActivity
                        return;
                    }
                }
            }
        }
        else{
            Utility.toast("Check you internet connection !");
        }
    }

    class GetSchoolName extends AsyncTask<Void, Void, Void>{
        ParseObject codegroup;
        public GetSchoolName(ParseObject inputCodegroup){
            this.codegroup = inputCodegroup;
        }

        @Override
        protected Void doInBackground(Void... params) {
            String schoolId = codegroup.getString("school");
            if(schoolId == null) {
                Log.d("DEBUG_JOINED_CLASS_INFO", "schoolId null");
                //store default school name and pin
                schoolName = defaultSchoolName;
                codegroup.put("schoolName", schoolName);
                codegroup.pinInBackground();
                return null;
            }

            if(schoolId.equalsIgnoreCase("Others") || schoolId.equalsIgnoreCase("Other")){//school-id is Other. No need to fetch anything. Just put "Other" in schoolName
                Log.d("DEBUG_JOINED_CLASS_INFO", "schoolId Others");
                schoolName = defaultSchoolName;
                codegroup.put("schoolName", schoolName);
                codegroup.pinInBackground();
                return null;
            }

            HashMap<String, Object> parameters = new HashMap<String, Object>();

            parameters.put("schoolId", schoolId);
            try{
                String name = ParseCloud.callFunction("getSchoolName", parameters);
                if(name != null){ //store and pin
                    Log.d("DEBUG_JOINED_CLASS_INFO", "getSchoolName() success with name "+ name);
                    schoolName = name;
                    codegroup.put("schoolName", schoolName);
                    codegroup.pinInBackground();
                }
                else{
                    Log.d("DEBUG_JOINED_CLASS_INFO", "getSchoolName() success with name NULL");
                    schoolName = defaultSchoolName; //don't pin this
                }
            }
            catch (ParseException e){
                Log.d("DEBUG_JOINED_CLASS_INFO", "getSchoolName() failed ParseException");
                schoolName = defaultSchoolName; //don't pin this
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result){//schoolName has been set to a non-null value
            schoolNameTV.setText(schoolName);
        }
    }
}
