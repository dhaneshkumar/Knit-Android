
package additionals;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.parse.ParseAnalytics;

import java.util.HashMap;
import java.util.Map;

import baseclasses.MyActionBarActivity;
import library.UtilString;
import trumplab.textslate.R;
import trumplabs.schoolapp.Constants;
import utility.Utility;

/*
    common class to invite users
    Intent must contain : inviteType, -> see Constants.java
           optional :   source  -> landed here from either in-app click or notification,
                        classCode,
                        className

    Mode(email/phone/etc) will be decided here on this screen on click
 */
public class Invite extends MyActionBarActivity{
    public static final String LOGTAG = "DEBUG_INVITE";
    private String classCode = "";
    private String className = "";
    private String source = Constants.SOURCE_APP;
    private String teacherName = "";

    private int inviteType = -1; //1, 2, 3, 4 (see Constants.java)

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.invite_parent);

      final TextView inviteHeading = (TextView) findViewById(R.id.invite_heading);
      final TextView sms = (TextView) findViewById(R.id.sms);
      final TextView app = (TextView) findViewById(R.id.app);
      final TextView instructions = (TextView) findViewById(R.id.instructions);
      RelativeLayout whatsapp = (RelativeLayout) findViewById(R.id.whatsapp);
      RelativeLayout email = (RelativeLayout) findViewById(R.id.email);
      RelativeLayout phonebook = (RelativeLayout) findViewById(R.id.phonebook);

      final LinearLayout seeHow = (LinearLayout) findViewById(R.id.seeHow);

      if(getIntent()!= null &&  getIntent().getExtras() != null)
      {
          Bundle bundle = getIntent().getExtras();

          if(bundle.getInt("inviteType", -1000) != -1000){
              inviteType = bundle.getInt("inviteType");
          }

          if(!UtilString.isBlank(bundle.getString("classCode"))) {
              classCode = bundle.getString("classCode");
              className = bundle.getString("className");
          }

          if(!UtilString.isBlank(bundle.getString("teacherName"))){
              teacherName = bundle.getString("teacherName");
          }
          //override source if in intent extras
          if(!UtilString.isBlank(bundle.getString("source"))){
              source = bundle.getString("source");
          }
      }

      getSupportActionBar().setDisplayHomeAsUpEnabled(true);

      Typeface typeFace = Typeface.createFromAsset(getAssets(), "fonts/roboto-condensed.bold.ttf");
      inviteHeading.setTypeface(typeFace);

      //TODO set title & invite heading according to inviteType and hide/show extra details
      String whatsAppContent = Constants.spreadWordContent;

      if(inviteType == Constants.INVITATION_T2P){
          //show the extra fields
          instructions.setVisibility(View.VISIBLE);
          seeHow.setVisibility(View.VISIBLE);
      }
      else{
          instructions.setVisibility(View.GONE);
          seeHow.setVisibility(View.GONE);
      }

      switch (inviteType){
          case Constants.INVITATION_T2P:
              inviteHeading.setText("Invite Parents");
              getSupportActionBar().setTitle("Invite Parents & Students");
              whatsAppContent = "Hi! I have recently started using 'Knit Messaging' app to send updates for my '"+ className +"' class. Download the app from "+ "goo.gl/cormDk" +" and use code '"+classCode+"' to join my class. To join via SMS, send '" + classCode + "  <Student's Name>' to 9243000080";
              break;
          case Constants.INVITATION_P2T:
              inviteHeading.setText("Invite Teachers");
              getSupportActionBar().setTitle("Invite Teachers");
              whatsAppContent = "Dear teacher, I found an awesome app, 'Knit Messaging', for teachers to communicate with parents and students. You can download the app from " + "goo.gl/FmydzU ";
              break;
          case Constants.INVITATION_P2P:
              inviteHeading.setText("Invite other parents");
              getSupportActionBar().setTitle("Invite other parents");
              whatsAppContent = "Hi! I just joined '" + className + "' class of " + teacherName + " on 'Knit Messaging' app.  Download the app from " + "goo.gl/Q2yeE3" +  " and use code '" + classCode + "' to join this class. To join via SMS, send '" + classCode + "  <Student's Name>' to 9243000080";
              break;
          case Constants.INVITATION_SPREAD:
              inviteHeading.setText("Tell your friends about Knit");
              getSupportActionBar().setTitle("Spread the word");
              whatsAppContent = "Yo! I just started using 'Knit Messaging' app. It's an awesome app for teachers, parents and students to connect with each other. Download the app from " + "goo.gl/GLkQ57 ";
              break;
      }

      final String whatsAppContentFinal = whatsAppContent;

      //click on phonebook icon
      phonebook.setOnClickListener(new OnClickListener() {
          @Override
          public void onClick(View v) {
              Intent intent = new Intent(Invite.this, InviteVia.class);
              intent.putExtra("classCode", classCode);
              intent.putExtra("inviteType", inviteType);
              intent.putExtra("inviteMode", Constants.MODE_PHONE);
              startActivity(intent);
          }
      });

      //click on email icon
      email.setOnClickListener(new OnClickListener() {
          @Override
          public void onClick(View v) {
              Intent intent = new Intent(Invite.this, InviteVia.class);
              intent.putExtra("classCode", classCode);
              intent.putExtra("inviteType", inviteType);
              intent.putExtra("inviteMode", Constants.MODE_EMAIL);
              startActivity(intent);
          }
      });

      //share via whatsapp
      whatsapp.setOnClickListener(new OnClickListener() {
          @Override
          public void onClick(View v) {
              PackageManager pm = getPackageManager();
              try {
                  pm.getPackageInfo("com.whatsapp", PackageManager.GET_ACTIVITIES);

                  //track this event
                  Map<String, String> dimensions = new HashMap<String, String>();
                  dimensions.put("Invite Type", "type" + Integer.toString(inviteType));
                  dimensions.put("Invite Mode", Constants.MODE_WHATSAPP);
                  ParseAnalytics.trackEventInBackground("inviteMode", dimensions);
                  Log.d(LOGTAG, "tracking inviteMode type=" + inviteType + ", mode=" + Constants.MODE_WHATSAPP);

                  Intent sendIntent = new Intent(Intent.ACTION_SEND);
                  sendIntent.setPackage("com.whatsapp");
                  sendIntent.setType("text/plain");

                  sendIntent.putExtra(Intent.EXTRA_TEXT, whatsAppContentFinal);
                  startActivity(sendIntent);

              } catch (PackageManager.NameNotFoundException e) {
                  e.printStackTrace();
                  Utility.toast("WhatsApp not installed !");
              }
          }
      });

      instructions.setOnClickListener(new OnClickListener() {
          @Override
          public void onClick(View v) {
              //track this event
              Map<String, String> dimensions = new HashMap<String, String>();
              dimensions.put("Invite Type", "type" + Integer.toString(inviteType));
              dimensions.put("Invite Mode", Constants.MODE_RECEIVE_INSTRUCTIONS);
              ParseAnalytics.trackEventInBackground("inviteMode", dimensions);
              Log.d(LOGTAG, "tracking inviteMode type=" + inviteType + ", mode=" + Constants.MODE_RECEIVE_INSTRUCTIONS);

              FragmentManager fm = getSupportFragmentManager();
              RecommendationDialog recommendationDialog = new RecommendationDialog();
              Bundle args = new Bundle();
              args.putString("classCode", classCode);
              args.putString("className", className);

              recommendationDialog.setArguments(args);
              recommendationDialog.show(fm, "Join Class");
          }
      });



      sms.setOnClickListener(new OnClickListener() {
          @Override
          public void onClick(View view) {

              FragmentManager fm = getSupportFragmentManager();
              HowToJoinDialog howToJoinDialog= new HowToJoinDialog();

              // Supply num input as an argument.
              Bundle args = new Bundle();
              args.putString("flag", "SMS");
              args.putString("classCode", classCode);
              howToJoinDialog.setArguments(args);

              howToJoinDialog.show(fm, "how to join");
          }
      });

      app.setOnClickListener(new OnClickListener() {
          @Override
          public void onClick(View view) {

              FragmentManager fm = getSupportFragmentManager();
              HowToJoinDialog howToJoinDialog= new HowToJoinDialog();

              // Supply num input as an argument.
              Bundle args = new Bundle();
              args.putString("flag", "APP");
              args.putString("classCode", classCode);
              howToJoinDialog.setArguments(args);

              howToJoinDialog.show(fm, "how to join");
          }
      });


      //track this event
      if(inviteType > 0) {
          Map<String, String> dimensions = new HashMap<String, String>();
          dimensions.put("Invite Type", "type" + Integer.toString(inviteType));
          dimensions.put("Source", source);
          ParseAnalytics.trackEventInBackground("invitePageOpenings", dimensions);
          Log.d(LOGTAG, "tracking invitePageOpenings type=" + inviteType + ",source=" + source);
      }
  }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        // Save UI state changes to the savedInstanceState.
        // This bundle will be passed to onCreate if the process is
        // killed and restarted.

        savedInstanceState.putString("classCode", classCode);
        savedInstanceState.putString("className", className);
        savedInstanceState.putString("source", source);
        savedInstanceState.putInt("inviteType", inviteType);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // Restore UI state from the savedInstanceState.
        // This bundle has also been passed to onCreate.

        if(UtilString.isBlank(classCode))
            classCode = savedInstanceState.getString("classCode");
        if(UtilString.isBlank(className))
            className = savedInstanceState.getString("className");
        if(UtilString.isBlank(source))
            source = savedInstanceState.getString("source");
        if(inviteType == -1)
            inviteType = savedInstanceState.getInt("inviteType");
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
