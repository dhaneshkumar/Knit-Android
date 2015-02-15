package joinclasses;

import android.content.Context;
import android.content.Intent;
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

import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import baseclasses.MyActionBarActivity;
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

/**
 * Activity to show layout to enter the child-name
 */
public class AddChildToClass extends MyActionBarActivity {
  public static List<String> group;
  private TextView okButton;
  private AutoCompleteTextView child_editText;
  private static String userId;
  private String code;
  public static LinearLayout progressBarLayout;
  public static LinearLayout editProfileLayout;
  private String childName;
  private Point p ;
  private int height;
  private Typeface typeFace;
  private ParseUser user;
  private String backFlag;
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.joinclass_addchild);

    //enabling home button
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    //validating current user
    ParseUser user = ParseUser.getCurrentUser();
    if (user == null)
      {Utility.logout(); return;}

    typeFace =
        Typeface.createFromAsset(getAssets(), "fonts/RobotoCondensed-BoldItalic.ttf");

    //Initializing variables
    child_editText = (AutoCompleteTextView) findViewById(R.id.child);
    TextView child_textView = (TextView) findViewById(R.id.child_textView);
    child_textView.setTypeface(typeFace);
    okButton = (TextView) findViewById(R.id.done);
    progressBarLayout = (LinearLayout) findViewById(R.id.progressBarLayout);
    editProfileLayout = (LinearLayout) findViewById(R.id.profileEditLayout);
    userId = user.getUsername();
    code = getIntent().getExtras().getString("code");
    backFlag = getIntent().getExtras().getString("backFlag");
    SessionManager session = new SessionManager(Application.getAppContext());
    
   
    //setting adapter for autocomplete child-name form
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

    //setting help button functionality
    help.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {

          //show popup
        if (p != null) {
          Popup popup = new Popup();
          popup.showPopup(AddChildToClass.this, p, true, -300, txt, height, 15, 400);

          InputMethodManager inputMethodManager =
              (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

            //hide keyboard
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

    /**
     * joining class in background using class-code and child-name
     */
  class AddChild_Background extends AsyncTask<Void, Void, Boolean> {
      private boolean classExist = false;

    @Override
    protected Boolean doInBackground(Void... params) {

        Log.d("join", "class joining -----------");

        if (userId != null && childName != null) {

        /*
         * Retrieving user details
         */
            Log.d("join", "class joining -----------1111");

            classExist = false; //setting flag initially false

            childName = childName.trim();
            childName = UtilString.parseString(childName);
        
        /*
         * Storing child name for autocomplete suggestions
         */
            SessionManager session = new SessionManager(Application.getAppContext());
            session.addChildName(childName);

            Log.d("join", "class joining -----------1155");



            user = ParseUser.getCurrentUser();
            if (user != null) {
                Log.d("join", "class joining -----------22222");


                int result = JoinedHelper.joinClass(code, childName, false);


                if (result == 1)
                    return true;      //successfully joined class
                else if (result == 2) {
                    classExist = true;    //already joined
                    return false;
                } else
                    return false;       //failed to join

            } else
                return false;
        }

        return false;
    }



    @Override
    protected void onPostExecute(Boolean result) {

      if (result) {
        Utility.toast("ClassRoom Added.");
        
        if( Messages.myadapter != null)
          Messages.myadapter.notifyDataSetChanged();
        
        Intent intent = new Intent(AddChildToClass.this, joinclasses.JoinClassesContainer.class);
        startActivity(intent);

      } else {
        if (classExist) {
          Utility.toast("Class room Already added.");
        } else
          Utility.toast("Failed to join this class. Try again.");

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
