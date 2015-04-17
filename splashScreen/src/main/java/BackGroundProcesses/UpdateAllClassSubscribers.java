package BackGroundProcesses;

import android.os.AsyncTask;
import android.util.Log;

import com.parse.ParseUser;

import java.util.List;

import trumplabs.schoolapp.Application;
import trumplabs.schoolapp.Classrooms;
import trumplabs.schoolapp.Constants;
import trumplabs.schoolapp.MainActivity;
import utility.SessionManager;

/**
 * It updates subscribers list of all classes of a user.
 */
public class UpdateAllClassSubscribers  {

    static void updateMembers()
    {
        ParseUser user = ParseUser.getCurrentUser();
        if(user != null)
        {
            if(user.getString("role").equals(Constants.TEACHER))
            {
                        MemberList memberList = new MemberList();
                        memberList.doInBackgroundCore();

                if(MainActivity.mHeaderProgressBar!=null){
                    MainActivity.mHeaderProgressBar.post(new Runnable() {
                        @Override
                        public void run() {
                                Classrooms.createdClassAdapter.notifyDataSetChanged();
                        }
                    });
                }

                Log.d("SUBSCRIBER", "updating subscriber in a thread");
            }
        }
    }



    /* @Override
        protected Void doInBackground(Void... params) {

            ParseUser user = ParseUser.getCurrentUser();
            if(user != null)
            {
                if(user.getString("role").equals(Constants.TEACHER))
                {
                    List<List<String>> createdClassroom = user.getList(Constants.CREATED_GROUPS);
                    if(createdClassroom != null)
                    {
                        for(int i=0; i<createdClassroom.size();i++)
                        {
                            MemberList memberList = new MemberList(createdClassroom.get(i).get(0));
                            memberList.execute();

                            Log.d("SUBSCRIBER_UPDATE", createdClassroom.get(i).get(0));
                        }
                    }
                }
            }

            return null;
        }*/


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
