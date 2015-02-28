package joinclasses;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import baseclasses.MyActionBarActivity;
import library.UtilString;
import trumplab.textslate.R;
import trumplabs.schoolapp.Classrooms;
import trumplabs.schoolapp.Constants;
import utility.Utility;

/**
 *
 * Show Information of suggested classrooms
 */
public class JoinSuggestedClass extends MyActionBarActivity {
    private TextView classNameView;
    private TextView teacherNameView;
    private TextView classCodeView;
    private Button joinButton;
    private TextView ignoreView;
    private ImageView profileImageView;
    public static
    TextView  schoolNameTV;
    private LinearLayout joinLayout;

    private String teacherName;
    private String className;
    private String classCode;
    private String schoolName;
    private String defaultSchoolName="";

    protected void onCreate(android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.suggested_class_info);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        profileImageView = (ImageView) findViewById(R.id.profileImage);
        classNameView = (TextView) findViewById(R.id.classname);
        classCodeView = (TextView) findViewById(R.id.classcode);
        teacherNameView = (TextView) findViewById(R.id.teacher);
        joinButton = (Button) findViewById(R.id.join_button);
        ignoreView = (TextView) findViewById(R.id.ignore);
        schoolNameTV = (TextView) findViewById(R.id.schoolName);
        joinLayout = (LinearLayout) findViewById(R.id.join);

        teacherName = getIntent().getExtras().getString("teacherName");
        className = getIntent().getExtras().getString("className");
        classCode = getIntent().getExtras().getString("classCode");

        teacherNameView.setText(teacherName);
        classNameView.setText(className);
        classCodeView.setText(classCode);

        TextView profile = (TextView) findViewById(R.id.profile);
        TextView classDetails = (TextView) findViewById(R.id.classDetails);

        //setting font in subheaders
        Typeface typeFace = Typeface.createFromAsset(getAssets(), "fonts/roboto-condensed.bold.ttf");
        profile.setTypeface(typeFace);
        classDetails.setTypeface(typeFace);


        //copy class code
        classCodeView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Utility.copyToClipBoard(JoinSuggestedClass.this, "Class Code", classCode);
            }
        });


        //opening join suggestion dialog
        joinLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fm = getSupportFragmentManager();
                JoinClassDialog joinClassDialog = new JoinClassDialog();
                Bundle args = new Bundle();
                args.putString("classCode", classCode);

                joinClassDialog.setArguments(args);
                joinClassDialog.show(fm, "Join Class");
            }
        });

        //ignore suggestion
        ignoreView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ParseUser user = ParseUser.getCurrentUser();
                if(user != null){
                    List<List<String>> removedList = ParseUser.getCurrentUser().getList(Constants.REMOVED_GROUPS);

                    if (removedList == null) {
                        removedList = new ArrayList<List<String>>();
                    }

                    for(int i=0; i<removedList.size(); i++){
                        Log.d("DEBUG_JOIN_SUGGESTED", "removed list " + i + " " + removedList.get(i).get(0));
                    }

                    ArrayList<String> removedGroup = new ArrayList<String>();
                    removedGroup.add(classCode);
                    removedGroup.add(className);
                    removedList.add(removedGroup);
                    user.put(Constants.REMOVED_GROUPS, removedList);
                    user.getCurrentUser().saveEventually();

                  /*  // updating suggestions adapter in Classrooms fragment
                    Classrooms.suggestedGroups = JoinedHelper.getSuggestionList(user.getUsername());

                    if(Classrooms.suggestedClassAdapter != null)
                        Classrooms.suggestedClassAdapter.notifyDataSetChanged();*/
                }
                finish(); //close the activity
            }
        });



        //get details(schoolName, profile pic, assigned name) from Codegroup and User table
        ParseQuery<ParseObject> classQuery = new ParseQuery<ParseObject>("Codegroup");
        classQuery.fromLocalDatastore();
        classQuery.whereEqualTo("code", classCode);

        String teacherSenderId = null;
        String teacherSex = "";

        try{
            ParseObject codegroup = classQuery.getFirst();
            teacherSenderId = codegroup.getString("senderId");
            teacherSex = codegroup.getString("sex");
            schoolName = codegroup.getString("schoolName"); //this is a new field. If not present, fetch and store locally
            if(schoolName == null){
                //fetch school name from id using asynctask
                Log.d("DEBUG_JOINED_CLASS_INFO", "schoolName not in codegroup. Fetching...");
                JoinedClassInfo.GetSchoolName getSchoolNameTask = new JoinedClassInfo.GetSchoolName(codegroup, 1);
                getSchoolNameTask.execute();
            }
            else{
                Log.d("DEBUG_JOINED_CLASS_INFO", "schoolName already there " + schoolName);
                schoolNameTV.setText(schoolName);
            }
        }
        catch (ParseException e){
            Log.d("DEBUG_JOINED_CLASS_INFO", "local query into Codegroup failed");
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