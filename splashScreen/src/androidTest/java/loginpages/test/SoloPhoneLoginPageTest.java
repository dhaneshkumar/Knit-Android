package loginpages.test;

import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.EditText;

import com.robotium.solo.Solo;

import baseclasses.MyActionBarActivity;
import loginpages.LoginPage;
import loginpages.PhoneLoginPage;
import trumplab.textslate.R;

/**
 * Created by Dhanesh on 1/3/2015.
 */
public class SoloPhoneLoginPageTest extends ActivityInstrumentationTestCase2<PhoneLoginPage> {
    private Solo solo;
    public SoloPhoneLoginPageTest(){
        super(PhoneLoginPage.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        solo = new Solo(getInstrumentation(), getActivity());
    }

    public void testSoloIncorrectNumber(){
        Log.d("TEST", "running main activity test");
        EditText phoneNumberET = (EditText) solo.getView(R.id.phone_id);
        solo.clearEditText(phoneNumberET);
        solo.clickOnView(phoneNumberET);

        this.sendKeys(KeyEvent.KEYCODE_9);
        this.sendKeys(KeyEvent.KEYCODE_8);
        this.sendKeys(KeyEvent.KEYCODE_7);

        String phoneNum = phoneNumberET.getText().toString();
        assertEquals("987", phoneNum);

        solo.clickOnText("Next"); //NOTE THAT THIS IS CASE SENSITIVE
        assertTrue(solo.waitForText("Incorrect Mobile Number", 0, 5000)); //Should toast this since only 3 digit in mobile number
    }

    public void testSoloOldLogin(){
        Log.d("TEST", "running main activity test");
        solo.clickOnText("Are you an old user");
        solo.assertCurrentActivity("Expected Old login page activity", LoginPage.class);
    }
}