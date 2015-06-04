
package additionals;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.parse.ParseAnalytics;

import java.util.HashMap;
import java.util.Map;

import baseclasses.MyActionBarActivity;
import library.UtilString;
import trumplab.textslate.R;
import trumplabs.schoolapp.Constants;

/*
    common class to invite users
    Intent must contain : inviteType, -> see Constants.java
           optional :   source  -> landed here from either in-app click or notification,
                        classCode,
                        className

    Mode(email/phone/etc) will be decided here on this screen on click
 */
public class InviteParents extends MyActionBarActivity{
    public static final String LOGTAG = "DEBUG_INVITE";
    private String classCode = "";
    private String className = "";
    private String source = Constants.SOURCE_APP;

    private int inviteType = -1; //1, 2, 3, 4 (see Constants.java)

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.invite_parent);

      final TextView inviteHeading = (TextView) findViewById(R.id.invite_heading);
      final TextView howToJoin = (TextView) findViewById(R.id.howToJoin);
      RelativeLayout whatsapp = (RelativeLayout) findViewById(R.id.whatsapp);
      RelativeLayout email = (RelativeLayout) findViewById(R.id.email);
      RelativeLayout phonebook = (RelativeLayout) findViewById(R.id.phonebook);


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

          //override source if in intent extras
          if(!UtilString.isBlank(bundle.getString("source"))){
              source = bundle.getString("source");
          }
      }

      getSupportActionBar().setDisplayHomeAsUpEnabled(true);

      //TODO set title & invite heading according to inviteType and hide/show extra details
      getSupportActionBar().setTitle("Invite Parents & Students");

      switch (inviteType){
          case Constants.INVITATION_T2P:
              inviteHeading.setText("Invite Parents using -");
              break;
          case Constants.INVITATION_P2T:
              inviteHeading.setText("Invite Teachers using -");
              break;
          case Constants.INVITATION_P2P:
              inviteHeading.setText("Invite fellow parents using - ");
              break;
          case Constants.INVITATION_SPREAD:
              inviteHeading.setText("Tell others about Knit using - ");
              break;
      }

      //click on phonebook icon
      phonebook.setOnClickListener(new OnClickListener() {
          @Override
          public void onClick(View v) {
              Intent intent = new Intent(InviteParents.this, InviteParentViaPhonebook.class);
              intent.putExtra("classCode", classCode);
              intent.putExtra("inviteType", inviteType);
              startActivity(intent);
          }
      });

    /*  recommendedTv.setOnClickListener(new OnClickListener() {
          @Override
          public void onClick(View v) {
              FragmentManager fm = getSupportFragmentManager();
              RecommendationDialog recommendationDialog = new RecommendationDialog();
              Bundle args = new Bundle();
              args.putString("classCode", classCode);
              args.putString("className", className);

              recommendationDialog.setArguments(args);
              recommendationDialog.show(fm, "Join Class");
          }
      });*/

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
