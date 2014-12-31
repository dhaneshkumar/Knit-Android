package utility.test;

import android.test.InstrumentationTestCase;


/**
 * Created by Dhanesh on 12/22/2014.
 */


public class Utility extends InstrumentationTestCase{
        public void test() throws Exception {
            final int expected = 1;
            final int reality = 1;
            assertEquals(expected, reality);
        }


}
