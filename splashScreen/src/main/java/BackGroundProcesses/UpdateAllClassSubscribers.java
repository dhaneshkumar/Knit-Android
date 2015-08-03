package BackGroundProcesses;

import android.util.Log;

import com.parse.ParseUser;

import trumplabs.schoolapp.Constants;
import utility.Config;

/**
 * It updates subscribers list of all classes of a user.
 */
public class UpdateAllClassSubscribers  {

    //call from THREAD only
    public static void updateMembers()
    {
        ParseUser user = ParseUser.getCurrentUser();
        if(user == null){
            return;
        }

        if(user.getString("role").equals(Constants.TEACHER))
        {
            MemberList memberList = new MemberList();
            memberList.doInBackgroundCore();

            if(Config.SHOWLOG) Log.d("SUBSCRIBER", "updating subscriber in a thread");
        }
    }

    public static void update(){
        Runnable r = new Runnable() {
            @Override
            public void run() {
                updateMembers();
            }
        };

        Thread t = new Thread(r);
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }
}
