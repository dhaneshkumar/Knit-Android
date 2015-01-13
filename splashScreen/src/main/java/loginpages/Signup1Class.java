package loginpages;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.List;

import baseclasses.MyActionBarActivity;
import joinclasses.School;
import library.UtilString;
import trumplab.textslate.R;
import trumplabs.schoolapp.Constants;
import utility.Utility;

public class Signup1Class extends MyActionBarActivity {
  Spinner mrspinner;
  EditText displayname_etxt;
  AutoCompleteTextView schoolNameView;
  Button join_btn;
  String role;

  protected void onCreate(android.os.Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.signup1_layout);
    mrspinner = (Spinner) findViewById(R.id.mr_spinner);
    displayname_etxt = (EditText) findViewById(R.id.displaynameid);
    schoolNameView = (AutoCompleteTextView) findViewById(R.id.schoolName);
    join_btn = (Button) findViewById(R.id.join_button);

    Intent intent = getIntent();
    role = intent.getStringExtra("role");

    if (role.equals(Constants.PARENT) || role.equals(Constants.STUDENT))
      schoolNameView.setVisibility(View.GONE);
    else {
      /*
       * get school name from local storage
       */

        //setting adapter to autocomplete textview
        School school = new School();
      ArrayAdapter adapter;
      if (school.getSchoolList() != null && school.getSchoolList().size() > 0) {
        adapter =
            new ArrayAdapter(this, android.R.layout.simple_list_item_1, school.getSchoolList().toArray());

        schoolNameView.setAdapter(adapter);
      }
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
          Utility.toast("Incorrect School Name");
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



}
