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

import trumplabs.schoolapp.Classrooms;
import trumplabs.schoolapp.Constants;
import trumplabs.schoolapp.MemberDetails;
import trumplabs.schoolapp.Subscribers;
import utility.Queries;

/**
 * Fetch updated group members from server in background
 * @param-groupCode
 */
public class MemberList extends AsyncTask<Void, Void, String[]> {

    private String groupCode;
    private Queries queries;

    public MemberList(String groupCode) {
        this.groupCode = groupCode;
        this.queries = new Queries();
    }

    @Override
    protected String[] doInBackground(Void... params) {

        Date updatedTime = null;  //last updated time of members
        ParseUser user = ParseUser.getCurrentUser();


        //retrieving last updated time of app members
        ParseQuery<ParseObject> appQuery = ParseQuery.getQuery(Constants.GROUP_MEMBERS);
        appQuery.fromLocalDatastore();
        appQuery.whereEqualTo("code", groupCode);
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
        appQuery.whereEqualTo("cod", groupCode);
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

      /*
      Checking whether updatedTime is null or not.
      If it's null then set it to createdAt of this codegroup entry
       */
        if (updatedTime == null) {
            //retrieving codegroup entry of given class
            ParseQuery<ParseObject> codeQuery = ParseQuery.getQuery(Constants.CODE_GROUP);
            codeQuery.fromLocalDatastore();
            codeQuery.whereEqualTo("code", groupCode);
            codeQuery.whereEqualTo("userId", user.getUsername());

            ParseObject obj = null;
            try {
                obj = codeQuery.getFirst();
            } catch (ParseException e) {
                e.printStackTrace();
            }

            if (obj != null) {
                updatedTime = obj.getCreatedAt();
            }


        }


       // Log.d("MEMBER", "members " + updatedTime.toString());

        //calling parse cloud functions to fetch new member updates
        HashMap<String, Object> param = new HashMap<String, Object>();
        param.put("classcode", groupCode);

        if (updatedTime != null)
            param.put("date", updatedTime);

        HashMap<String, Object> memberList = null;

        try {
            memberList = ParseCloud.callFunction("showSubscribers", param);
        } catch (ParseException e) {
            e.printStackTrace();
        }


        // storing updated members locally
        if (memberList != null) {
            List<ParseObject> appMembersList = (List<ParseObject>) memberList.get("app");
            List<ParseObject> smsMembersList = (List<ParseObject>) memberList.get("sms");


            //storing app members
            if (appMembersList != null) {

                Log.d("MEMBER", "members " + appMembersList.size() );

                for (int i = 0; i < appMembersList.size(); i++) {
                    ParseObject appMembers = appMembersList.get(i);
                    appMembers.put("userId", user.getUsername());
                    Log.d("MEMBER", "members " + appMembers.getString("name") + "  :  " + appMembers.getString("status") );
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

        return null;
    }


    @Override
    protected void onPostExecute(String[] strings) {

        //updating listview items of member list.
        List<MemberDetails> updatedLocalMemberList = queries.getLocalClassMembers(groupCode);
        if (updatedLocalMemberList != null) {
            Subscribers.memberDetails = updatedLocalMemberList;
        }

        if (Subscribers.mHeaderProgressBar != null)
            Subscribers.mHeaderProgressBar.setVisibility(View.GONE);

        if (Subscribers.myadapter != null)
            Subscribers.myadapter.notifyDataSetChanged();

        if (Classrooms.createdClassAdapter != null)
            Classrooms.createdClassAdapter.notifyDataSetChanged();

        super.onPostExecute(strings);
    }
}



