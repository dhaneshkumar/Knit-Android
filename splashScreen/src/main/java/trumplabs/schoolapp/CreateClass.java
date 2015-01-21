package trumplabs.schoolapp;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import joinclasses.School;
import library.UtilString;
import notifications.AlarmReceiver;
import notifications.NotificationGenerator;
import trumplab.textslate.R;
import utility.Queries;
import utility.Queries2;
import utility.SessionManager;
import utility.Tools;
import utility.Utility;
import additionals.ClassInstructions;
import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.PopupMenu.OnMenuItemClickListener;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import baseclasses.MyActionBarActivity;

import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

public class CreateClass extends MyActionBarActivity {
  private android.support.v7.app.ActionBar actionbar;
  private LinearLayout progressLayout;
  private TextView codetxtview;
  private TextView okbtn;
  private ScrollView createclasslayout;
  private LinearLayout codeviewlayout;
  private EditText classnameview;
  private String typedtxt;
  private Queries query12;
  private String userId;
  private String codevalue;
  private boolean classNameCheckFlag = false;
  private boolean internetFlag = false;
  Activity activity;
  private ParseUser user;
  private LinearLayout schoolButton;
  private LinearLayout standardButton;
  private LinearLayout divisonButton;
  private TextView school_txt;
  private TextView standard_txt;
  private TextView division_txt;
  private String selectedSchool;
  private String selectedStandard;
  private String selectedDivison;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.create_class_new);

    query12 = new Queries();
    actionbar = getSupportActionBar();
    actionbar.setDisplayHomeAsUpEnabled(true);
    createclasslayout = (ScrollView) findViewById(R.id.createclasslayout);
    codeviewlayout = (LinearLayout) findViewById(R.id.codeviewlayout);
    progressLayout = (LinearLayout) findViewById(R.id.progresslayout);
    ProgressBar progressbar = (ProgressBar) findViewById(R.id.progressBar);

    schoolButton = (LinearLayout) findViewById(R.id.schoolButton);
    standardButton = (LinearLayout) findViewById(R.id.standardButton);
    divisonButton = (LinearLayout) findViewById(R.id.divisionButton);
    school_txt = (TextView) findViewById(R.id.school);
    standard_txt = (TextView) findViewById(R.id.standard);
    division_txt = (TextView) findViewById(R.id.division);
    user = ParseUser.getCurrentUser();
    activity = this;


    if (user == null)
      Utility.logout();

    userId = user.getUsername();

    codetxtview = (TextView) findViewById(R.id.codetxtview);
    okbtn = (TextView) findViewById(R.id.okay_button);
    classnameview = (EditText) findViewById(R.id.classnameid);
    Button createclassbtn = (Button) findViewById(R.id.create_button);

      School school = new School();
    selectedSchool = school.getSchoolName(user.getString("school"));
    if (selectedSchool != null)
      school_txt.setText(selectedSchool);
    else
      selectedSchool ="Other";
    selectedDivison = "NA";
    selectedStandard = "NA";

    /*
     * convert typed edit-text letters to upper-case
     */
    // EditTextFunctions editTextModifier = new EditTextFunctions(classnameview);
    // editTextModifier.changeToUpperCase();

    createclassbtn.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {

        typedtxt = classnameview.getText().toString().trim();
        typedtxt = typedtxt.replaceAll("'", " ");
        typedtxt = typedtxt.replaceAll("`", " ");
        typedtxt = typedtxt.replaceAll("\"", " ");




        if (!UtilString.isBlank(typedtxt)) {


            if(!UtilString.isBlank(selectedStandard) && !selectedStandard.equals("NA"))
                 typedtxt += " "+selectedStandard;

            if(!UtilString.isBlank(selectedDivison) && !selectedDivison.equals("NA"))
            typedtxt += selectedDivison;

          if (Utility.isInternetOn(activity)) {
            createGroup jg = new createGroup();
            jg.execute();



            /*
             * Hidding the keyboard from screen
             */
            Tools.hideKeyboard(CreateClass.this);

            progressLayout.setVisibility(View.VISIBLE);
            createclasslayout.setVisibility(View.GONE);
          } else {
            Utility.toast("Check your Internet connection");
          }
        }
      }

    });

    okbtn.setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View v) {
        finish();
      }
    });


    /*
     * school select options
     */
    schoolButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {

          School school1 = new School();
        String school = school1.getSchoolName(user.getString("school"));

        /*
         * Creating popmenu for selecting schools
         */
        PopupMenu menu = new PopupMenu(CreateClass.this, v);

        if (!UtilString.isBlank(school))
          menu.getMenu().add(school);

        menu.getMenu().add("Other");
        menu.show();


        // setting menu click functionality
        menu.setOnMenuItemClickListener(new OnMenuItemClickListener() {

          @Override
          public boolean onMenuItemClick(MenuItem item) {

            selectedSchool = item.getTitle().toString();
            school_txt.setText(selectedSchool);
            return false;
          }
        });
      }
    });

    /*
     * Instruction button click response
     */
    TextView instructView = (TextView) findViewById(R.id.instructions);
    instructView.setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View v) {

        Intent intent = new Intent(CreateClass.this, ClassInstructions.class);
        intent.putExtra("grpCode", codevalue);
        intent.putExtra("grpName", typedtxt);
        startActivity(intent);

      }
    });


    /*
     * school select options
     */
    standardButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {

        Tools.hideKeyboard(CreateClass.this);
        /*
         * Creating popmenu for selecting schools
         */
        PopupMenu menu = new PopupMenu(CreateClass.this, v);
        menu.getMenuInflater().inflate(R.menu.standard, menu.getMenu());
        menu.show();


        /** Defining menu item click listener for the popup menu */

        // setting menu click functionality
        menu.setOnMenuItemClickListener(new OnMenuItemClickListener() {

          @Override
          public boolean onMenuItemClick(MenuItem item) {


            selectedStandard = item.getTitle().toString();
            standard_txt.setText(selectedStandard);
            standard_txt.setTextColor(Color.parseColor("#000000"));
            return false;
          }
        });
      }
    });

    divisonButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {

        Tools.hideKeyboard(CreateClass.this);
        /*
         * Creating popmenu for selecting schools
         */
        PopupMenu menu = new PopupMenu(CreateClass.this, v);
        menu.getMenuInflater().inflate(R.menu.division, menu.getMenu());
        menu.show();


        /** Defining menu item click listener for the popup menu */

        // setting menu click functionality
        menu.setOnMenuItemClickListener(new OnMenuItemClickListener() {

          @Override
          public boolean onMenuItemClick(MenuItem item) {


            selectedDivison = item.getTitle().toString();
            division_txt.setText(selectedDivison);
            division_txt.setTextColor(Color.parseColor("#000000"));
            return false;
          }
        });
      }
    });

  }

  @Override
  protected void onResume() {
    super.onResume();
    Utility.isInternetOn(this);
  }

  private class createGroup extends AsyncTask<Void, Void, String[]> {

    String[] mStrings;

    @Override
    protected String[] doInBackground(Void... params) {


      if (!query12.checkClassNameExist(typedtxt)) {

        codevalue = Utility.generateCode().trim();

        try {
          while (Queries.isGroupExist(codevalue)) {
            codevalue = Utility.generateCode().trim();
          }
        } catch (ParseException e2) {
        }

        codetxtview.setText(codevalue);

        /*
         * Pushing newly created entry to users table in "Created_groups" columns
         */
        user = ParseUser.getCurrentUser();
        
        
        if (user != null) {

          /*
           * pushing group informations in "group-details table"
           */


            Log.d("create class", "creating class .................");

          final ParseObject groupDetails = new ParseObject("Codegroup");
          groupDetails.put("code", codevalue);
          groupDetails.put("name", typedtxt);
          groupDetails.put("Creator", user.get("name").toString());
          groupDetails.put("senderId", userId);

          if (user.getString("sex") != null)
            groupDetails.put("sex", user.getString("sex"));

          if (ParseUser.getCurrentUser().get("picName") != null)
            groupDetails.put("picName", ParseUser.getCurrentUser().get("picName"));

          if (ParseUser.getCurrentUser().get("pid") != null)
            groupDetails.put("senderPic", ParseUser.getCurrentUser().getParseFile("pid"));

          groupDetails.put("classExist", true);
          
          String schoolId = user.getString("school");
          if(! selectedSchool.trim().equals("Other"))
          {
            groupDetails.put("school", schoolId);
            Utility.ls(schoolId);
          }
            
          if(!selectedDivison.equals("NA"))
            groupDetails.put("divison", selectedDivison);
          
          if(!selectedStandard.equals("NA"))
            groupDetails.put("standard", selectedStandard);


            Log.d("create class", "saving codegroup table .................");
          groupDetails.saveInBackground(new SaveCallback() {

            @Override
            public void done(ParseException e) {

              if (e == null) {
                user.addUnique("Created_groups", Arrays.asList(codevalue, typedtxt));
                user.saveEventually();

                  Log.d("create class", "creation groud  added .................");

                  try {
                  groupDetails.put("userId", userId);
                  groupDetails.pin();
                } catch (ParseException e1) {
                }


              }

            }
          });

          /*
           * save locally
           */



        } else
          Utility.logout();
      } else {
        classNameCheckFlag = true;
      }

      return mStrings;
    }

    @Override
    protected void onPostExecute(String[] result) {


      if (!classNameCheckFlag) {
        Utility.toast("Group Creation successful");

        List<String> newgroup = new ArrayList<String>();
        newgroup.add(codevalue);
        newgroup.add(typedtxt);

        // updating list view of created group
        if(Classrooms.createdGroups == null)
            Classrooms.createdGroups = new ArrayList<List<String>>();

        Classrooms.createdGroups.add(newgroup);
        Classrooms.members = 0;

        if(Classrooms.myadapter != null)
            Classrooms.myadapter.notifyDataSetChanged();

        // Setting layouts visibility
        codeviewlayout.setVisibility(View.VISIBLE);
        progressLayout.setVisibility(View.GONE);

        //create class creation messages and notification
        SessionManager session = new SessionManager(getApplicationContext());
        NotificationGenerator.generateNotification(getApplicationContext(), Constants.CLASS_CREATION_MESSAGE_TEACHER, Constants.DEFAULT_NAME, Constants.NORMAL_NOTIFICATION, Constants.INBOX_ACTION);
        AlarmReceiver.generateLocalMessage(Constants.CLASS_CREATION_MESSAGE_TEACHER, Constants.DEFAULT_NAME, user, session);


      } else if (classNameCheckFlag) {
        createclasslayout.setVisibility(View.VISIBLE);
        progressLayout.setVisibility(View.GONE);
        Utility.toast("Sorry. Can't create classes with same name");
        classNameCheckFlag = false;
      }

      /*
       * else if (internetFlag){ createclasslayout.setVisibility(View.VISIBLE);
       * progressLayout.setVisibility(View.GONE); Utility.toast("Check your Internet Connection.");
       * internetFlag = false;
       * 
       * }
       */
      else {
        createclasslayout.setVisibility(View.VISIBLE);
        progressLayout.setVisibility(View.GONE);
        Utility.toast("Check your Internet Connection.");
        internetFlag = false;

      }

      super.onPostExecute(result);
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
