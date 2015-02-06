package loginpages;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;

import java.util.ArrayList;

import additionals.SchoolAutoComplete;
import baseclasses.MyActionBarActivity;
import library.DelayAutoCompleteTextView;
import library.UtilString;
import trumplab.textslate.R;
import trumplabs.schoolapp.Constants;
import utility.Utility;

public class Signup1Class extends MyActionBarActivity {
  Spinner mrspinner;
  EditText displayname_etxt;
  AutoCompleteTextView schoolNameView;
  DelayAutoCompleteTextView locationInput;
  ProgressBar loadingSchools;
  Button join_btn;
  String role;

  private ArrayAdapter schoolsAdapter;
  private Context activityContext;

  protected void onCreate(android.os.Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.signup1_layout);
    activityContext = this;

    mrspinner = (Spinner) findViewById(R.id.mr_spinner);
    displayname_etxt = (EditText) findViewById(R.id.displaynameid);
    schoolNameView = (AutoCompleteTextView) findViewById(R.id.schoolName);
    locationInput = (DelayAutoCompleteTextView) findViewById(R.id.school_location);
    loadingSchools = (ProgressBar) findViewById(R.id.loading_schools);
    join_btn = (Button) findViewById(R.id.join_button);

    Intent intent = getIntent();
    role = intent.getStringExtra("role");

    if (role.equals(Constants.PARENT) || role.equals(Constants.STUDENT)) {
        schoolNameView.setVisibility(View.GONE);
        locationInput.setVisibility(View.GONE);
        loadingSchools.setVisibility(View.GONE);
    }
    else {
        locationInput.setVisibility(View.VISIBLE);
        schoolNameView.setVisibility(View.GONE); //for now. show when school list fetched depending on location
        loadingSchools.setVisibility(View.GONE); //for now show while fetching school location
      /*
       * get school name from local storage
       */

        //setting adapter to autocomplete textview
        /*ArrayAdapter adapter;
          try {

              ReadSchoolFile readSchoolFile = new ReadSchoolFile();
              adapter =
                  new ArrayAdapter(this, android.R.layout.simple_list_item_1, readSchoolFile.getSchoolsList().toArray());
              schoolNameView.setAdapter(adapter);
          } catch (IOException e) {
              e.printStackTrace();
          }*/

        locationInput.setAdapter(new SchoolAutoComplete.PlacesAutoCompleteAdapter(this, R.layout.school_autocomplete_list_item, R.id.school_location));
        locationInput.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final String value = (String) parent.getItemAtPosition(position);
                Log.d("DEBUG_PROFILE_PAGE", "item clicked " + value);
                schoolNameView.setVisibility(View.GONE);
                loadingSchools.setVisibility(View.VISIBLE); //show progress bar

                new AsyncTask<Void, Void, Void>() {
                    ArrayList<String> schools;
                    @Override
                    protected Void doInBackground( Void... voids ) {
                        schools = SchoolAutoComplete.schoolsNearby(value);
                        return null;
                    }
                    @Override
                    protected void onPostExecute(Void result){
                        schoolsAdapter =
                                new ArrayAdapter(activityContext, android.R.layout.simple_list_item_1, schools);
                        schoolNameView.setAdapter(schoolsAdapter);

                        loadingSchools.setVisibility(View.GONE); //hide progress bar
                        schoolNameView.setText("");
                        schoolNameView.setVisibility(View.VISIBLE); //finally show school list box
                        return;
                    }
                }.execute();
            }
        });
    }

      //next button click response
    join_btn.setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View v) {
        String displayname = displayname_etxt.getText().toString();
        String schoolName = schoolNameView.getText().toString();
        if (UtilString.isBlank(displayname))
          Utility.toast("Incorrect Display Name");
        else if (UtilString.isBlank(schoolName) && role.equals(Constants.TEACHER))
          Utility.toast("Incorrect School Location/Name");
        else if (mrspinner.getSelectedItemPosition() == 0)
          Utility.toast("Choose a title!");
        else {


          Intent intent = new Intent(getBaseContext(), Signup1_5Class.class);
          intent.putExtra("MR", mrspinner.getSelectedItem().toString());

          String userName = UtilString.changeFirstToCaps(displayname);

          intent.putExtra("name", userName.trim());
          
          if (role.equals(Constants.TEACHER))
            intent.putExtra("school", schoolName.trim());
          intent.putExtra("role", role);
          startActivity(intent);

        }
      }
    });
  };


    @Override
    public void onBackPressed() {
            Intent intent = new Intent(this, Signup.class);
            startActivity(intent);
    }



}
