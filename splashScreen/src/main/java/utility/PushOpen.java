package utility;

import additionals.OpenURL;
import profileDetails.ProfilePage;
import trumplabs.schoolapp.Constants;
import trumplabs.schoolapp.MainActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;

import com.parse.ParseUser;

public class PushOpen extends ActionBarActivity {
    String type;
    String url;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        onNewIntent(getIntent());

        MyReceiver myreceiver = null;
        for (int i = 0; i < 10; i++)
            myreceiver.events[i] = "";
        myreceiver.count = 0;


        Intent i= null;

        if(type != null) {
            if (type.equals("TRANSITION")) {
                i = new Intent(this, ProfilePage.class);

            } else if (type.equals("ACTION")) {

                i = new Intent(this, OpenURL.class);
                i.putExtra("URL", url);
            }
            else
            {
                i = new Intent(this, MainActivity.class);
                ParseUser user = ParseUser.getCurrentUser();
                if (user != null && user.getString("role").equals(Constants.TEACHER))
                    i.putExtra("VIEWPAGERINDEX", 1);
                i.putExtra("pushOpen", true);
            }
        }
        else
        {
            i = new Intent(this, MainActivity.class);
            ParseUser user = ParseUser.getCurrentUser();
            if (user != null && user.getString("role").equals(Constants.TEACHER))
                i.putExtra("VIEWPAGERINDEX", 1);
            i.putExtra("pushOpen", true);

        }

        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        this.startActivity(i);
    }


    @Override
    public void onNewIntent(Intent intent) {

        setIntent(intent);
        type = getIntent().getExtras().getString("TYPE");
        url = getIntent().getExtras().getString("URL");

    }


}