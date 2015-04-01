package loginpages.test;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.test.ActivityInstrumentationTestCase2;
import android.test.ActivityUnitTestCase;
import android.test.InstrumentationTestCase;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.EditText;

import baseclasses.MyActionBarActivity;
import loginpages.PhoneLoginPage;
import trumplab.textslate.R;
import trumplabs.schoolapp.MainActivity;

/**
 * Created by Dhanesh on 1/3/2015.
 */
public class PhoneLoginPageTest extends ActivityInstrumentationTestCase2<PhoneLoginPage> {
    MyActionBarActivity mActivity;
    EditText phoneNumberET;
    public PhoneLoginPageTest(){
        super(PhoneLoginPage.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setActivityInitialTouchMode(false); //needed if you want to send key events
        mActivity = getActivity();
        phoneNumberET = (EditText) mActivity.findViewById(R.id.phone_id);
    }

    public void testInputOnlyDigits(){
        Log.d("TEST", "running main activity test");
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d("TEST", "setting text");
                phoneNumberET.performClick();
                //phoneNumberET.setText("123456789");
            }
        });

        getInstrumentation().waitForIdleSync();
        this.sendKeys(KeyEvent.KEYCODE_9);
        this.sendKeys(KeyEvent.KEYCODE_8);
        this.sendKeys(KeyEvent.KEYCODE_PLUS); //this would be ignored
        this.sendKeys(KeyEvent.KEYCODE_7);
        //sendKeys("1*23456789");

        String phoneNum = phoneNumberET.getText().toString();
        assertEquals("987", phoneNum);
    }
}