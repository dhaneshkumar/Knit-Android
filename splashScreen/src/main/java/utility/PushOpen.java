package utility;

import profileDetails.ProfilePage;
import trumplabs.schoolapp.Constants;
import trumplabs.schoolapp.MainActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

import com.parse.ParseUser;

public class PushOpen extends ActionBarActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MyReceiver myreceiver = null;
        for (int i = 0; i < 10; i++)
            myreceiver.events[i] = "";
        myreceiver.count = 0;
        Intent i = new Intent(this, MainActivity.class);

        ParseUser user = ParseUser.getCurrentUser();
        if (user != null && user.getString("role").equals(Constants.TEACHER))
            i.putExtra("VIEWPAGERINDEX", 1);

        i.putExtra("pushOpen", true);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        this.startActivity(i);
    }
}