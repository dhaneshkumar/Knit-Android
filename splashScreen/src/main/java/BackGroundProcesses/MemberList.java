package BackGroundProcesses;

import android.os.AsyncTask;
import android.util.Log;
import android.view.View;

import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

import trumplabs.schoolapp.Constants;
import trumplabs.schoolapp.MemberDetails;
import trumplabs.schoolapp.SendMessage;
import trumplabs.schoolapp.Subscribers;
import utility.Queries;

/**
 * Fetch updated group members from server of a class in background
 * @param-groupCode
 */
public class MemberList extends AsyncTask<Void, Void, String[]> {

    private Queries queries;
    private String groupCode;

    public MemberList(){
        this.groupCode =null;
        this.queries = new Queries();

    };

    public MemberList(String groupCode) {
        this.queries = new Queries();
        this.groupCode= groupCode;
    }

    @Override
    protected String[] doInBackground(Void... params) {
        doInBackgroundCore();
        return null;
    }

    public void doInBackgroundCore()
    {
        Date updatedTime = null;  //last updated time of members
        ParseUser user = ParseUser.getCurrentUser();

        //retrieving last updated time of app members
        ParseQuery<ParseObject> appQuery = ParseQuery.getQuery(Constants.GROUP_MEMBERS);
        appQuery.fromLocalDatastore();
        appQuery.whereEqualTo("userId", user.getUsername());
        appQuery.orderByDescending("updatedAt");

        ParseObject appMember = null;
        try {
            appMember = appQuery.getFirst();

            if (appMember != null)
                updatedTime = appMember.getUpdatedAt();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        //retrieving last updated time of sms members
        ParseQuery<ParseObject> smsQuery = ParseQuery.getQuery(Constants.MESSAGE_NEEDERS);
        appQuery.fromLocalDatastore();
        appQuery.whereEqualTo("userId", user.getUsername());
        appQuery.orderByDescending("updatedAt");

        ParseObject smsMember = null;
        try {
            smsMember = smsQuery.getFirst();

            if (appMember != null) {
                if (updatedTime == null)
                    updatedTime = smsMember.getUpdatedAt();
                else {
                    Date smsUpdatedTime = smsMember.getUpdatedAt();

                    if (smsUpdatedTime != null) {
                        if (updatedTime.compareTo(smsUpdatedTime) < 0)
                            updatedTime = smsUpdatedTime;  //updated last updated time
                    }
                }
            }

        } catch (ParseException e) {
            e.printStackTrace();
        }

        //calling parse cloud functions to fetch new member updates
        HashMap<String, Object> param = new HashMap<String, Object>();

        if (updatedTime != null) {
            param.put("date", updatedTime);
            Log.d("SUBSCRIBER_DEBUG", updatedTime.toString());
        }
        else
            param.put("date", user.getCreatedAt());



        HashMap<String, Object> memberList = null;

        try {
            memberList = ParseCloud.callFunction("showAllSubscribers", param);
            Log.d("SUBSCRIBER_DEBUG", "calling show all subscribers");
        } catch (ParseException e) {
            e.printStackTrace();
        }


        // storing updated members locally
        if (memberList != null) {
            List<ParseObject> appMembersList = (List<ParseObject>) memberList.get("app");
            List<ParseObject> smsMembersList = (List<ParseObject>) memberList.get("sms");


            //storing app members
            if (appMembersList != null) {

                Log.d("DEBUG_MEMBER", "members " + appMembersList.size() );

                for (int i = 0; i < appMembersList.size(); i++) {
                    ParseObject appMembers = appMembersList.get(i);
                    appMembers.put("userId", user.getUsername());
                    Log.d("DEBUG_MEMBER", "members " + appMembers.getString("name") + "  :  " + appMembers.getString("status") );
                }
                try {
                    ParseObject.pinAll(appMembersList);

                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }

            //storing sms members locally

            if (smsMembersList != null) {
                for (int i = 0; i < smsMembersList.size(); i++) {
                    ParseObject smsMembers = smsMembersList.get(i);
                    smsMembers.put("userId", user.getUsername());
                }
                try {
                    ParseObject.pinAll(smsMembersList);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        }
        else
            Log.d("SUBSCRIBER_DEBUG", "got zero members");


    }


    public static void onPostExecuteCore()
    {

        if (Subscribers.mHeaderProgressBar != null)
            Subscribers.mHeaderProgressBar.setVisibility(View.GONE);

        if (Subscribers.myadapter != null)
            Subscribers.myadapter.notifyDataSetChanged();
    }

    @Override
    protected void onPostExecute(String[] strings) {
        Log.d("DEBUG_MEMBER", "members onPostExecute() " + groupCode);


        if(groupCode != null)
        {
            //updating listview items of member list.
            List<MemberDetails> updatedLocalMemberList = queries.getLocalClassMembers(groupCode);
            if (updatedLocalMemberList != null) {
                Subscribers.memberDetails = updatedLocalMemberList;
            }


            int memberCount = 0;
            Queries memberQuery = new Queries();
            try {
                memberCount = memberQuery.getMemberCount(groupCode);
            } catch (ParseException e) {
                e.printStackTrace();
            }

            if(Subscribers.subscriberTV != null)
                Subscribers.subscriberTV.setText(memberCount + " subscribers");

            if(SendMessage.memberCountTV != null)
                SendMessage.memberCountTV.setText(memberCount + "");
        }

        onPostExecuteCore();

        super.onPostExecute(strings);
    }
}