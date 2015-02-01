package trumplabs.schoolapp.test;

import android.test.AndroidTestCase;

import trumplabs.schoolapp.MessagesHelper;

/**
 * Created by Dhanesh on 1/2/2015.
 */
public class MessagesHelperTest extends AndroidTestCase {

    MessagesHelperTest()
    {
      //  super(MessagesHelper.class);

    }

    @Override
    protected void setUp() {
        //   MessagesHelper.IncreaseLikeCount in = new IncreaseLikeCount();
    }

    public void testPrintHello() {

        MessagesHelper helper = new MessagesHelper();
      //  String t = helper.printHello(null);
    }

    @Override
    protected void tearDown() {
    }
}
