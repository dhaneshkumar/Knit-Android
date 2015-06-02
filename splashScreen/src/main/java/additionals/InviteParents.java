
package additionals;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.text.Html;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import baseclasses.MyActionBarActivity;
import library.UtilString;
import trumplab.textslate.R;
import utility.Utility;

public class InviteParents extends MyActionBarActivity{
    private String classCode;
    private String className;


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.invite_parent);

      final TextView inviteHeading = (TextView) findViewById(R.id.invite_heading);
      final TextView howToJoin = (TextView) findViewById(R.id.howToJoin);
      RelativeLayout whatsapp = (RelativeLayout) findViewById(R.id.whatsapp);
      RelativeLayout email = (RelativeLayout) findViewById(R.id.email);
      RelativeLayout phonebook = (RelativeLayout) findViewById(R.id.phonebook);


      if(getIntent()!= null && getIntent().getExtras() != null)
      {
          if(!UtilString.isBlank(getIntent().getExtras().getString("classCode"))) {
              classCode = getIntent().getExtras().getString("classCode");
              className = getIntent().getExtras().getString("className");
          }
      }

      getSupportActionBar().setDisplayHomeAsUpEnabled(true);
      getSupportActionBar().setTitle("Invite Parents & Students");



      //click on phonebook icon
      phonebook.setOnClickListener(new OnClickListener() {
          @Override
          public void onClick(View v) {

              Intent intent = new Intent(InviteParents.this, InviteParentViaPhonebook.class);
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

  }


    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        // Save UI state changes to the savedInstanceState.
        // This bundle will be passed to onCreate if the process is
        // killed and restarted.

        savedInstanceState.putString("classCode", classCode);
        savedInstanceState.putString("className", className);
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
