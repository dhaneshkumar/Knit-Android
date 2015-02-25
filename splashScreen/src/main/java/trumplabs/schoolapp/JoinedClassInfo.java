package trumplabs.schoolapp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
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
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import baseclasses.MyActionBarActivity;
import library.UtilString;
import trumplab.textslate.R;
import utility.Config;
import utility.Utility;

/**
 * Show joined classroom information
 * Created by ashish on 22/2/15.
 */
public class JoinedClassInfo extends MyActionBarActivity {
    Context activityContext;
    ImageView profileImageView;
    TextView classNameTV;
    TextView teacherNameTV;
    TextView classCodeTV;
    ImageView whatsAppImageView;
    static TextView schoolNameTV;
    TextView assignedNameTV;
    TextView subCodeTV;
    LinearLayout assignedNameContainer;
    RelativeLayout whatsappLayout;
    LinearLayout classInfoLayout;
    LinearLayout progressBarLayout;

    String className;
    String classCode;
    static String schoolName;
    String assignedName;
    String teacherName;

    static String defaultSchoolName = "";

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
        whatsappLayout = (RelativeLayout) findViewById(R.id.whatsappLayout);
        subCodeTV = (TextView) findViewById(R.id.subCode);
        classInfoLayout = (LinearLayout) findViewById(R.id.classInfoLayout);
        progressBarLayout = (LinearLayout) findViewById(R.id.progressBarLayout);

        TextView profile = (TextView) findViewById(R.id.profile);
        TextView classDetails = (TextView) findViewById(R.id.classDetails);
        TextView share = (TextView) findViewById(R.id.share);

        //get details(schoolName, profile pic, assigned name) from Codegroup and User table
        ParseQuery<ParseObject> classQuery = new ParseQuery<ParseObject>("Codegroup");
        classQuery.fromLocalDatastore();
        classQuery.whereEqualTo("code", classCode);

        String teacherSenderId = null;
        String teacherSex = "";

        Typeface typeFace = Typeface.createFromAsset(getAssets(), "fonts/roboto-condensed.bold.ttf");
        profile.setTypeface(typeFace);
        classDetails.setTypeface(typeFace);
        share.setTypeface(typeFace);

        try{
            ParseObject codegroup = classQuery.getFirst();
            teacherName = codegroup.getString("Creator");
            teacherSenderId = codegroup.getString("senderId");
            teacherSex = codegroup.getString("sex");
            schoolName = codegroup.getString("schoolName"); //this is a new field. If not present, fetch and store locally
            if(schoolName == null){
                //fetch school name from id using asynctask
                Log.d("DEBUG_JOINED_CLASS_INFO", "schoolName not in codegroup. Fetching...");
                GetSchoolName getSchoolNameTask = new GetSchoolName(codegroup, 0);
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

                            if (title.equals("Mr") || title.equals("Mr.")) {
                                profileImageView.setImageResource(R.drawable.maleteacherdp);
                            } else if (title.equals("Mrs") || title.equals("Mrs.")) {
                                profileImageView.setImageResource(R.drawable.femaleteacherdp);
                            } else if (title.equals("Ms") || title.equals("Ms.")) {
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

        if(classCode.equals(Config.defaultParentGroupCode) || classCode.equals(Config.defaultTeacherGroupCode))
        {
            classCodeTV.setVisibility(View.GONE);
            subCodeTV.setVisibility(View.GONE);
        }
        classCodeTV.setText(classCode);
        assignedNameTV.setText(assignedName);
        teacherNameTV.setText(teacherName);

        //on click code, copy code to clipboard
        classCodeTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Utility.copyToClipBoard(activityContext, "Class Code", classCode);
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
                            if (Utility.isInternetOn(JoinedClassInfo.this)) {
                                InputMethodManager imm =
                                        (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                                imm.hideSoftInputFromWindow(input.getWindowToken(), 0);

                                UpdateAssignedName updateAssignedName = new UpdateAssignedName(newAssignedName);
                                updateAssignedName.execute();
                                progressBarLayout.setVisibility(View.VISIBLE);
                                classInfoLayout.setVisibility(View.GONE);
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

        whatsappLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PackageManager pm = getPackageManager();
                try {
                    pm.getPackageInfo("com.whatsapp", PackageManager.GET_ACTIVITIES);
                    Intent sendIntent = new Intent(Intent.ACTION_SEND);
                    sendIntent.setPackage("com.whatsapp");
                    sendIntent.setType("text/plain");
                    String trimSchoolName = schoolName;
                    if(trimSchoolName.length() > 50 ){
                        trimSchoolName = schoolName.substring(0, 50);
                    }

                    trimSchoolName = trimSchoolName + "...";
                    sendIntent.putExtra(Intent.EXTRA_TEXT, "I have joined " + className +
                            " class(code " + classCode + ") on KNIT App by " + teacherName + " of " + trimSchoolName + ". Please join this class ! ");
                    startActivity(sendIntent);

                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                    Utility.toast("WhatsApp not installed !");
                }
            }
        });
    }


    Boolean isDefaultGroup(String code){
        if(code == null) return false;
        if(code.equals(Config.defaultParentGroupCode) || code.equals(Config.defaultTeacherGroupCode)){
            return true;
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        if(classCode != null) {

            if(isDefaultGroup(classCode)){
                return true;
            }

            //inflate menu containing unsubscribe option only if class is not the default group
            MenuInflater inflater = getMenuInflater();

            inflater.inflate(R.menu.joined_class_info_menu, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.unsubscribe:
                unSubscribeAction();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    void unSubscribeAction(){
        AlertDialog.Builder builder = new AlertDialog.Builder(activityContext);
        builder.setMessage("Unsubscribe from this class ?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        progressBarLayout.setVisibility(View.VISIBLE);
                        classInfoLayout.setVisibility(View.GONE);
                        UnSubscribeTask unSubscribeTask = new UnSubscribeTask();
                        unSubscribeTask.execute();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        builder.create().show();
    }

    public class UnSubscribeTask extends AsyncTask<Void, Void, Void>{
        List<List<String>> joinedGroups; //this will contain updated joined_groups
        Boolean success = false;
        public UnSubscribeTask(){
        }

        @Override
        protected Void doInBackground(Void... params) {
            //setting parameters
            HashMap<String, Object> param = new HashMap<String, Object>();
            param.put("classcode", classCode);
            param.put("installationObjectId", ParseInstallation.getCurrentInstallation().getObjectId());

            try {
                success = ParseCloud.callFunction("leaveClass", param);
                ParseUser user = ParseUser.getCurrentUser();
                user.fetch();
            } catch (ParseException e) {
                e.printStackTrace();
                return null;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result){
            progressBarLayout.setVisibility(View.GONE);
            classInfoLayout.setVisibility(View.VISIBLE);
            if(success) {
                //notify Classrooms joined groups adapter
                joinedGroups = ParseUser.getCurrentUser().getList("joined_groups");
                if (joinedGroups == null) {
                    joinedGroups = new ArrayList<List<String>>();
                }
                Classrooms.joinedGroups = joinedGroups;
                if(Classrooms.joinedClassAdapter != null) {
                    Classrooms.joinedClassAdapter.notifyDataSetChanged();
                }

                Utility.toast("Successfully unsubscribed");
                finish(); //close the activity
            }
            else{
                Utility.toast("Oops ! Failed to unsubscribe");
            }
        }
    }

    public class UpdateAssignedName extends AsyncTask<Void, Void, Void>{
        String newAssignedName;
        List<List<String>> joinedGroups; //this will contain updated joined_groups
        Boolean success = false;
        public UpdateAssignedName(String newName){
            newAssignedName = newName;
        }

        @Override
        protected Void doInBackground(Void... params) {

            HashMap<String, Object> parameters = new HashMap<String, Object>();

            parameters.put("classCode", classCode);
            parameters.put("childName", newAssignedName);

            try{
                ParseCloud.callFunction("changeAssociateName", parameters);
                Log.d("DEBUG_JOINED_CLASS_INFO", "changeAssociateName() success with new asso name "+ newAssignedName);
                ParseUser user = ParseUser.getCurrentUser();
                user.fetch();
                success = true;
            }
            catch (ParseException e){
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result){
            progressBarLayout.setVisibility(View.GONE);
            classInfoLayout.setVisibility(View.VISIBLE);
            if(success) {
                assignedName = newAssignedName;
                assignedNameTV.setText(assignedName); //assignedName reflects correct name(new when success, old when fail)

                //notify Classrooms joined groups adapter
                joinedGroups = ParseUser.getCurrentUser().getList("joined_groups");
                if (joinedGroups == null) {
                    joinedGroups = new ArrayList<List<String>>();
                }
                Classrooms.joinedGroups = joinedGroups;
                if(Classrooms.joinedClassAdapter != null) {
                    Classrooms.joinedClassAdapter.notifyDataSetChanged();
                }

                Utility.toast("Successfully updated Associated name.");
            }
            else{
                Utility.toast("Oops ! Failed to update Associated name.");
            }
        }
    }

    public static class GetSchoolName extends AsyncTask<Void, Void, Void>{
        ParseObject codegroup;
        private String school;
        int caller;  //  caller class  -- 0: joinedClassInfo & 1: JoinSuggestedClass
        public GetSchoolName(ParseObject inputCodegroup, int caller){
            this.codegroup = inputCodegroup;
            this.caller = caller;
        }

        @Override
        protected Void doInBackground(Void... params) {
            String schoolId = codegroup.getString("school");

            school = "";
            if(schoolId == null) {

                //store default school name and pin
                codegroup.put("schoolName", school);
                codegroup.pinInBackground();
                return null;
            }

            if(schoolId.equalsIgnoreCase("Others") || schoolId.equalsIgnoreCase("Other")){//school-id is Other. No need to fetch anything. Just put "Other" in schoolName
                Log.d("DEBUG_JOINED_CLASS_INFO", "schoolId Others");

                codegroup.put("schoolName", school);
                codegroup.pinInBackground();
                return null;
            }

            HashMap<String, Object> parameters = new HashMap<String, Object>();

            parameters.put("schoolId", schoolId);
            try{
                String name = ParseCloud.callFunction("getSchoolName", parameters);
                if(name != null){ //store and pin
                    Log.d("DEBUG_JOINED_CLASS_INFO", "getSchoolName() success with name "+ name);
                    school = name;
                    codegroup.put("schoolName", school);
                    codegroup.pinInBackground();
                }
            }
            catch (ParseException e){
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result){//schoolName has been set to a non-null value

            if(caller ==0) {
                JoinedClassInfo.schoolNameTV.setText(school);
                JoinedClassInfo.schoolName = school; //used for whats app messages. So need to update
            }
            else if(caller == 1)
                JoinSuggestedClass.schoolNameTV.setText(school);
            else if(caller == 2)
                Subscribers.schoolNameTV.setText(school);
        }
    }
}
