package BackGroundProcesses;

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

import trumplabs.schoolapp.Application;
import trumplabs.schoolapp.Constants;
import trumplabs.schoolapp.MemberDetails;
import trumplabs.schoolapp.SendMessage;
import trumplabs.schoolapp.Subscribers;
import utility.Config;
import utility.Queries;
import utility.Utility;

/**
 * Fetch updated group members from server of a class in background
 * @param-groupCode
 */
public class MemberList extends AsyncTaskProxy<Void, Void, String[]> {

    private Queries queries;
    private String groupCode;
    private List<MemberDetails> updatedLocalMemberList;
    private final int defaultMemberCount = -111;
    private int memberCount = defaultMemberCount;


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
        //do this first so as to populate Application.globalCodegroupMap
        Queries.fillCodegroupMap();

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
            if(Config.SHOWLOG) Log.d("SUBSCRIBER_DEBUG", updatedTime.toString());
        }
        else
            param.put("date", user.getCreatedAt());



        HashMap<String, Object> memberList = null;

        try {
            memberList = ParseCloud.callFunction("showAllSubscribers", param);
            if(Config.SHOWLOG) Log.d("SUBSCRIBER_DEBUG", "calling show all subscribers");
        } catch (ParseException e) {
            e.printStackTrace();
        }


        // storing updated members locally
        if (memberList != null) {
            List<ParseObject> appMembersList = (List<ParseObject>) memberList.get("app");
            List<ParseObject> smsMembersList = (List<ParseObject>) memberList.get("sms");


            //storing app members
            if (appMembersList != null) {

                if(Config.SHOWLOG) Log.d("DEBUG_MEMBER", "members " + appMembersList.size() );

                for (int i = 0; i < appMembersList.size(); i++) {
                    ParseObject appMembers = appMembersList.get(i);
                    appMembers.put("userId", user.getUsername());
                    if(Config.SHOWLOG) Log.d("DEBUG_MEMBER", "members " + appMembers.getString("name") + "  :  " + appMembers.getString("status"));
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

            //updating listview items of member list & codegroup entry

            if(groupCode != null) {
                updatedLocalMemberList = queries.getLocalClassMembers(groupCode);

                if(updatedLocalMemberList != null)
                {
                    memberCount = updatedLocalMemberList.size();
                    setMemberCount(groupCode, memberCount);  //updating codegroup entry
                }
            }

            //updating other class member counts in thread to stop delay in display
            setMemberCountInThread();

        }
        else
            if(Config.SHOWLOG) Log.d("SUBSCRIBER_DEBUG", "got zero members");




    }

    @Override
    protected void onPostExecute(String[] strings) {

        if(groupCode != null)
        {
            if (updatedLocalMemberList != null)
                Subscribers.memberDetails = updatedLocalMemberList;

            if(memberCount != defaultMemberCount) {
                if (Subscribers.subscriberTV != null)
                    Subscribers.subscriberTV.setText(memberCount + " Member" + Utility.getPluralSuffix(memberCount));

                if (SendMessage.memberCountTV != null && SendMessage.memberLabelTV != null) {
                    SendMessage.memberCountTV.setText(memberCount + "");
                    SendMessage.memberLabelTV.setText("Member" + Utility.getPluralSuffix(memberCount));
                }
            }
        }

        if (Subscribers.mHeaderProgressBar != null)
            Subscribers.mHeaderProgressBar.setVisibility(View.GONE);

        if (Subscribers.myadapter != null)
            Subscribers.myadapter.notifyDataSetChanged();

        super.onPostExecute(strings);
    }


    public static int getMemberCount(String groupCode){
        ParseUser currentParseUser = ParseUser.getCurrentUser();
        if(currentParseUser == null){
            return 0;
        }

        ParseObject codegroup = Queries.getCodegroupObject(groupCode);

        if(codegroup != null)
        {
            if(Config.SHOWLOG) Log.d("MEMBER", codegroup.getString(Constants.Codegroup.CODE) + ", member count="+codegroup.getInt(Constants.Codegroup.COUNT));
            return codegroup.getInt(Constants.Codegroup.COUNT);
        }
        else
            if(Config.SHOWLOG) Log.d("MEMBER", "query null");

        return 0;
    }


    public static void setMemberCount(String groupCode, int count) {
        ParseObject codegroup = Queries.getCodegroupObject(groupCode);

        if(codegroup != null) {
            codegroup.put(Constants.Codegroup.COUNT, count);
            codegroup.pinInBackground();
            if(Config.SHOWLOG) Log.d("MEMBER", "setMemberCount " + groupCode + " : success : count=" + count);
        }
        else{
            if(Config.SHOWLOG) Log.d("MEMBER", "setMemberCount " + groupCode + " : error");
        }
    }


    public void setMemberCountInThread()
    {
        Runnable r = new Runnable() {
            @Override
            public void run(){
                //updating all classroom's member entries

                ParseUser currentParseUser = ParseUser.getCurrentUser();
                if(currentParseUser == null){
                    return;
                }

                List<List<String>> createdGroups = currentParseUser.getList(Constants.CREATED_GROUPS);
                if(createdGroups != null )
                {
                    int classCount = createdGroups.size();

                    for(int i=0; i<classCount ; i++)
                    {
                        try {
                            memberCount = queries.getMemberCount(createdGroups.get(i).get(0));
                            setMemberCount(createdGroups.get(i).get(0), memberCount);  //updating codegroup entry

                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        };

        Thread t = new Thread(r);
        t.start();
    }
}