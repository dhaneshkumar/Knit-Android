package loginpages;

import library.UtilString;
import trumplab.textslate.R;
import trumplabs.schoolapp.Constants;
import utility.Utility;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class Signup1_5Class extends ActionBarActivity {

  EditText email_etxt;
  EditText phone_etxt;
  Button nextButton;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.signup1_5_layout);

    email_etxt = (EditText) findViewById(R.id.emailinput);
    phone_etxt = (EditText) findViewById(R.id.phoneid);
    nextButton = (Button) findViewById(R.id.create_button);

    nextButton.setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View v) {
        String email = email_etxt.getText().toString();
        String phone = phone_etxt.getText().toString();
        if (UtilString.isBlank(phone) || phone.length() != 10)
          Utility.toast("Incorrect Mobile Number");
        else {
          Intent intent = new Intent(getBaseContext(), Signup2Class.class);

          intent.putExtra("MR", getIntent().getExtras().getString("MR"));
          intent.putExtra("email", email.trim());
          intent.putExtra("phone", phone.trim());
          intent.putExtra("role", getIntent().getExtras().getString("role"));
          intent.putExtra("name", getIntent().getExtras().getString("name"));
          if (getIntent().getExtras().getString("role").equals(Constants.TEACHER))
            intent.putExtra("school", getIntent().getExtras().getString("school"));
          startActivity(intent);
        }
      }
    });

  }

}
