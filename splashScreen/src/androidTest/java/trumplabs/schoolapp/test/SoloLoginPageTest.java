package trumplabs.schoolapp.test;

import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.robotium.solo.Solo;

import loginpages.LoginPage;
import loginpages.PhoneLoginPage;
import loginpages.Signup;
import profileDetails.ProfilePage;
import trumplab.textslate.R;
import trumplabs.schoolapp.MainActivity;

/**
 * Created by Dhanesh on 1/3/2015.
 */
public class SoloLoginPageTest extends ActivityInstrumentationTestCase2<LoginPage> {
    private Solo solo;
    public SoloLoginPageTest(){
        super(LoginPage.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        solo = new Solo(getInstrumentation(), getActivity());
    }

    //Note that functions are executed in order of their names(alphabetically increasing)
    public void test0_ForgotCorrectEmail(){
        solo.clickOnText("Forgot?");
        //forgot password dialog shown now
        EditText emailTV = (EditText) solo.getView(R.id.emailid_forgot);
        TextView sendButton = (TextView) solo.getView(R.id.button_forgotpass);

        solo.enterText(emailTV, "ashson07@gmail.com");
        solo.clickOnView(sendButton);
        //look for positive response toast
        assertTrue(solo.waitForText("Password Reset Link is sent to your", 0, 10000)); //wait for 5 seconds for the toast to appear
    }

    public void test1_ForgotWrongEmail(){
        solo.clickOnText("Forgot?");
        //forgot password dialog shown now
        EditText emailTV = (EditText) solo.getView(R.id.emailid_forgot);
        TextView sendButton = (TextView) solo.getView(R.id.button_forgotpass);
        solo.enterText(emailTV, "ash");
        solo.clickOnView(sendButton);
        //look for positive response toast
        assertTrue(solo.waitForText("Failed to send link to your Email", 0, 10000));
    }

    public void test2_SignInWrongCredentials(){
        EditText usernameET = (EditText) solo.getView(R.id.usernameid);
        EditText passwordET = (EditText) solo.getView(R.id.passwordid);
        Button signinButton = (Button) solo.getView(R.id.signin_button);
        solo.enterText(usernameET, "wrong@wrong.com");
        solo.enterText(passwordET, "w");
        solo.clickOnView(signinButton);

        assertTrue(solo.waitForText("Log in failed", 0, 10000));
    }

    public void test3_SignInCorrectCredentials(){
        EditText usernameET = (EditText) solo.getView(R.id.usernameid);
        EditText passwordET = (EditText) solo.getView(R.id.passwordid);
        Button signinButton = (Button) solo.getView(R.id.signin_button);
        solo.enterText(usernameET, "tom@t.com");
        solo.enterText(passwordET, "t");
        solo.clickOnView(signinButton);

        //On sign-in success, MainActivity is loaded. So assert and wait for MainActivity
        assertTrue(solo.waitForActivity(MainActivity.class, 10000));

        //Wait for textview Created Classes to load before proceeding to logout
        TextView createdClassTV = (TextView) solo.getView(R.id.createdClassTextView);

        assertTrue(solo.waitForView(createdClassTV, 30000, false));

        solo.sendKey(Solo.MENU);
        solo.clickOnText("Profile");
        assertTrue(solo.waitForActivity(ProfilePage.class, 5000));


        TextView signOutButton = (TextView) solo.getView(R.id.signOut);
        solo.clickOnView(signOutButton);
        assertTrue(solo.waitForActivity(Signup.class, 10000));
    }
}