package school;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.parse.FunctionCallback;
import com.parse.ParseAnalytics;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseUser;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import baseclasses.MyActionBarActivity;
import loginpages.PhoneSignUpName;
import loginpages.PhoneSignUpVerfication;
import profileDetails.ProfilePage;
import trumplab.textslate.R;
import utility.Tools;
import utility.Utility;

/**
 * Created by ashish on 10/9/15.
 */
public class PhoneInputActivity extends MyActionBarActivity{

    final static String LOGTAG = "__pi_activity";

    ProgressDialog pdialog;
    EditText phoneNumberET;
    public String phoneNumber = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.phone_input_activity);

        phoneNumberET = (EditText) findViewById(R.id.phone_id);
        phoneNumber = "";

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        phoneNumberET.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    next();
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.phone_signup_name_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.next:
                next();
                break;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    void next(){
        phoneNumber = phoneNumberET.getText().toString();
        if (!Utility.isNumberValid(phoneNumber)) {
            Utility.toast("Incorrect Mobile Number", true);
        }
        else if(Utility.isInternetExist()) {
            Intent nextIntent = new Intent(this, PhoneSignUpVerfication.class);
            nextIntent.putExtra("purpose", PhoneSignUpVerfication.UPDATE_PHONE);
            nextIntent.putExtra("phoneNumber", phoneNumber);

            PhoneSignUpName.GenerateVerificationCode generateVerificationCode = new PhoneSignUpName.GenerateVerificationCode(phoneNumber);
            startActivity(nextIntent);

            generateVerificationCode.execute();
        }
    }
}
