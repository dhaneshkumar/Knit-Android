package joinclasses;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import library.UtilString;
import notifications.AlarmReceiver;
import notifications.NotificationGenerator;
import trumplab.textslate.R;
import trumplabs.schoolapp.Application;
import trumplabs.schoolapp.Constants;
import trumplabs.schoolapp.Messages;
import utility.Popup;
import utility.Queries;
import utility.Queries2;
import utility.SessionManager;
import utility.Utility;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap.Config;
import android.graphics.Point;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import baseclasses.MyActionBarActivity;

import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParsePush;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import org.json.JSONException;
import org.json.JSONObject;

public class AddChildToClass extends MyActionBarActivity {
  public static List<String> group;
  private TextView okButton;
  private AutoCompleteTextView child_editText;
  private static String userId;
  private String code;
  private String grpName;
  public static LinearLayout progressBarLayout;
  public static LinearLayout editProfileLayout;
  private String childName;
  private Point p ;
  private int height;
  private boolean joinFlag = false;
  private boolean classExist = false;
  private Queries textQuery;
  private Queries2 memberQuery;
  private Typeface typeFace;
  private ParseUser user;
  private String schoolId ;
  private String standard;
  private String division;
  private String backFlag;
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.joinclass_addchild);

    typeFace =
        Typeface.createFromAsset(getAssets(), "fonts/RobotoCondensed-BoldItalic.ttf");
   
    child_editText = (AutoCompleteTextView) findViewById(R.id.child);
    TextView child_textView = (TextView) findViewById(R.id.child_textView);
    child_textView.setTypeface(typeFace);
    okButton = (TextView) findViewById(R.id.done);
    // skipButton = (TextView) findViewById(R.id.skip);
    progressBarLayout = (LinearLayout) findViewById(R.id.progressBarLayout);
    editProfileLayout = (LinearLayout) findViewById(R.id.profileEditLayout);

    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    ParseUser user = ParseUser.getCurrentUser();

    if (user == null)
      {Utility.logout(); return;}

    memberQuery = new Queries2();
    textQuery = new Queries();
    userId = user.getUsername();
    code = getIntent().getExtras().getString("code");
      backFlag = getIntent().getExtras().getString("backFlag");

    SessionManager session = new SessionManager(Application.getAppContext());
    
   
    
    ArrayAdapter adapter;
    if(session.getChildList()!= null)
    {
    adapter = new ArrayAdapter
        (this,android.R.layout.simple_list_item_1,session.getChildList().toArray());
    }
    else
    {
      String[] names = {user.getString("name")};
      
      adapter = new ArrayAdapter<String>
          (this,android.R.layout.simple_list_item_1,names);
    }
    child_editText.setAdapter(adapter);
    
    final ImageView help = (ImageView) findViewById(R.id.help);

    ViewTreeObserver vto = help.getViewTreeObserver();
    vto.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
      @Override
      public void onGlobalLayout() {
        int[] location = new int[2];
        help.getLocationOnScreen(location);
        height = help.getHeight();

        // Initialize the Point with x, and y positions
        p = new Point();
        p.x = location[0];
        p.y = location[1];

      }
    });



    final String txt =
        "Entered Name will be associated with this class. It may be your name or your child's name";

    help.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {

        if (p != null) {
          Popup popup = new Popup();
          popup.showPopup(AddChildToClass.this, p, true, -300, txt, height, 15, 400);

          InputMethodManager inputMethodManager =
              (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
          // inputMethodManager.showSoftInput(viewToEdit, 0);
          if (getCurrentFocus() != null) {
            inputMethodManager.hideSoftInputFromWindow(getCurrentFocus()
                .getApplicationWindowToken(), 0);
          }
        }
      }
    });



    /*
     * Setting click listener on OK button
     */
    okButton.setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View v) {

        if (child_editText.getText() == null)
          return;

        childName = child_editText.getText().toString();
        childName = childName.trim();
        childName = UtilString.changeFirstToCaps(childName);

        if (Utility.isInternetOn(AddChildToClass.this)) {

          if (!UtilString.isBlank(childName)) {

            InputMethodManager imm =
                (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(child_editText.getWindowToken(), 0);


            AddChild_Background rcb = new AddChild_Background();
            rcb.execute();

            progressBarLayout.setVisibility(View.VISIBLE);
            editProfileLayout.setVisibility(View.GONE);
          }

        }
      }
    });


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


  /******************************************************************/


  class AddChild_Background extends AsyncTask<Void, Void, Void> {

    ParseInstallation pi;
    @Override
    protected Void doInBackground(Void... params) {

      if (userId != null && childName != null) {

        /*
         * Retrieving user details
         */

        childName = childName.trim();
        childName = UtilString.parseString(childName);
        
        /*
         * Change first letter to caps
         */
        
        SessionManager session = new SessionManager(Application.getAppContext());
        session.addChildName(childName);
        

        user = ParseUser.getCurrentUser();
        if (user != null) {

          /********************* < joining class room > *************************/

          joinFlag = false;
          classExist = false;
          // check whether group exist or not
          ParseQuery<ParseObject> query = ParseQuery.getQuery("Codegroup");

          String grpCode = code;
          grpCode = grpCode.replace("TS", "");
          code = "TS" + grpCode.trim();


          query.whereEqualTo("code", code);

          if (!textQuery.isJoinedClassExist(code)) {

            ParseObject a;
            try {
              a = query.getFirst();

              if (a != null) {
                if (a.get("name") != null && a.getBoolean("classExist")) {

                  String senderId = a.getString("senderId");
                  final String grpSenderId =  senderId;
                  ParseFile senderPic = a.getParseFile("senderPic");

                  if (!UtilString.isBlank(a.get("name").toString())) {
                    grpName = a.get("name").toString();

                   schoolId = a.getString("school");
                      standard = a.getString("standard");
                      division = a.getString(Constants.DIVISION);

                    // Enable to receive push

                    pi = ParseInstallation.getCurrentInstallation();
                    if (pi != null) {
                      
                      pi.addUnique("channels", code);
                      pi.saveInBackground(new SaveCallback() {
                        
                        @Override
                        public void done(ParseException e) {
                         if(e != null)
                         {
                           pi.saveEventually();
                           Utility.ls("saved not installation in back");
                           
                           e.printStackTrace();
                         }
                         else
                         {
                           Utility.ls("saved installation in back");
                         }
                        }
                      });
                      
                    }
                    else
                      Utility.ls("parse installation --  null");

                    user.addUnique("joined_groups", Arrays.asList(code, grpName, childName));
                    user.saveEventually();

                    // Adding this user as member in GroupMembers table
                    final ParseObject groupMembers = new ParseObject("GroupMembers");
                    groupMembers.put("code", code);
                    groupMembers.put("name", user.getString("name"));
                    List<String> boys = new ArrayList<String>();
                    boys.add(childName.trim());
                    groupMembers.put("children_names", boys);


                    if (user.getEmail() != null)
                      groupMembers.put("emailId", user.getEmail());
                    groupMembers.saveInBackground(new SaveCallback() {
                      
                      @Override
                      public void done(ParseException e) {
                        if(e == null)
                        {
                          try {
                            memberQuery.storeGroupMember(code, userId, true);
                            
                          } catch(ParseException e1)
                          {
                           e1.printStackTrace(); 
                          }
                        }
                      }
                    });
                  }
                  

                  /*
                   * Saving locally in Codegroup table
                   */
                  a.put("userId", userId);
                  a.pin();

                  /*
                   * download pic locally
                   */
                  senderId = senderId.replaceAll("@", "");
                  String filePath =
                      Utility.getWorkingAppDir() + "/thumbnail/" + senderId + "_PC.jpg";
                  final File senderThumbnailFile = new File(filePath);

                  if (!senderThumbnailFile.exists()) {

                    Queries2 imageQuery = new Queries2();

                    if (senderPic != null)
                      imageQuery.downloadProfileImage(senderId, senderPic);
                  } else {
                    // Utility.toast("image already exist ");
                  }

                  joinFlag = true;
                  
                  
                  
               // Create our Installation query
                /*  ParseQuery pushQuery = ParseInstallation.getQuery();
                  pushQuery.whereEqualTo("installationId", ParseInstallation.getCurrentInstallation().getInstallationId());
                   
                  // Send push notification to query
                    ParsePush push = new ParsePush();
                    push.setQuery(pushQuery);             // Set our Installation query
                    JSONObject data = new JSONObject();
                    try {
                        data.put("msg", utility.Config.welcomeMsg);
                        data.put("groupName", grpName);
                        push.setData(data);
                        push.sendInBackground();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }*/


                    //locally generating joiining notification and inbox msg
                    NotificationGenerator.generateNotification(getApplicationContext(), utility.Config.welcomeMsg, grpName, Constants.NORMAL_NOTIFICATION, Constants.INBOX_ACTION);
                    AlarmReceiver.generateLocalMessage(utility.Config.welcomeMsg, code, a.getString("Creator"), a.getString("senderId"), grpName, user);


                  /*
                  Retrieve suggestion classes and  store them in locally
                   */


                    School.storeSuggestions(schoolId, standard, division, userId);
                }
              }
            } catch (ParseException e) {
              joinFlag = false;
            }
          } else
            classExist = true;
        }
      }
      return null;
    }



    @Override
    protected void onPostExecute(Void result) {

      if (joinFlag) {
        Utility.toast("ClassRoom Added.");
        
        if( Messages.myadapter != null)
          Messages.myadapter.notifyDataSetChanged();
        
        Intent intent = new Intent(AddChildToClass.this, joinclasses.JoinClassesContainer.class);
        startActivity(intent);

      } else {
        if (classExist) {
          Utility.toast("Class room Already added.");
        } else
          Utility.toast("Entered Class doesn't exist");

        Intent intent = new Intent(AddChildToClass.this, joinclasses.JoinClassesContainer.class);
        intent.putExtra("VIEWPAGERINDEX", 1);
        startActivity(intent);
      }

    }
  }



  @Override
  public void onBackPressed() {

      if (backFlag == null)
      {
        Intent intent = new Intent(this, joinclasses.JoinClassesContainer.class);
        intent.putExtra("VIEWPAGERINDEX", 1);
        startActivity(intent);
     }
     else {
          Intent intent = new Intent(this, joinclasses.JoinClassesContainer.class);
          startActivity(intent);

      }
    super.onBackPressed();
  }




}
