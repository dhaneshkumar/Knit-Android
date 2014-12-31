package loginpages.test;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.test.InstrumentationTestCase;
import android.test.TouchUtils;
import android.test.suitebuilder.annotation.MediumTest;
import android.widget.Button;

import trumplab.textslate.R;
import loginpages.SplashScreen;
import loginpages.LoginPage;


/**
 * Created by Dhanesh on 12/22/2014.
 */


class SplashScreenTest extends InstrumentationTestCase {
    public void test() throws Exception {
        final int expected = 1;
        final int reality = -1;
        assertEquals(expected, reality);
    }


    @MediumTest
    public void testValidLayout () {
        Instrumentation inst = getInstrumentation();
        Instrumentation.ActivityMonitor monitor = inst.addMonitor(LoginPage.class.getName(), null, false);

        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setClassName(inst.getTargetContext(), LoginPage.class.getName());
        inst.startActivitySync(intent);

        Activity CurrentActivity = getInstrumentation().waitForMonitorWithTimeout(monitor, 5);

        assertNotNull(CurrentActivity);
        assertEquals("LoginPage1", CurrentActivity.getClass().getName());

        inst.removeMonitor(monitor);
        monitor = inst.addMonitor(SplashScreen.class.getName(), null, false);


        final int expected = 1;
        final int reality = -1;
        assertEquals(expected, reality);

       /* Button btnBack = (Button) CurrentActivity.findViewById(R.id.backbutton);
        TouchUtils.clickView(this, btnBack);

        Activity CurrentActivity1 = getInstrumentation().waitForMonitorWithTimeout(monitor, 5);
        assertNotNull(CurrentActivity);
        assertEquals("loginpages.SplashScreen", CurrentActivity1.getClass().getName());*/
    }

}

