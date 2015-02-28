package trumplabs.schoolapp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import BackGroundProcesses.MemberList;
import additionals.InviteParents;
import fr.castorflex.android.smoothprogressbar.SmoothProgressBar;
import joinclasses.JoinedClassInfo;
import library.ExpandableListView;
import library.UtilString;
import trumplab.textslate.R;
import utility.Queries;
import utility.Tools;
import utility.Utility;

/**
 * Show subscriber list of a class
 */
public class Subscribers extends ActionBarActivity {
    public static BaseAdapter myadapter;
    public static List<MemberDetails> memberDetails;
    private String classCode;
    private String className;
   // private String schoolName;
    private Queries memberQuery;
    public static SmoothProgressBar mHeaderProgressBar;
    public static LinearLayout progressBarLayout;
    public static LinearLayout editProfileLayout;
    private ExpandableListView listv;
   // public static TextView schoolNameTV;

    static String defaultSchoolName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.classmembers_layout);

        //setting action bar title
        getSupportActionBar().setTitle("Subscribers");

        //Adding home back button
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if(getIntent().getExtras() != null)
        {
            classCode = getIntent().getExtras().getString("classCode");
            className = getIntent().getExtras().getString("className");
        }

        intializeBackgroundParameters();

        //intializing variables
        listv = (ExpandableListView) findViewById(R.id.memberslistview);
        mHeaderProgressBar = (SmoothProgressBar) findViewById(R.id.ptr_progress);
        progressBarLayout = (LinearLayout) findViewById(R.id.progressBarLayout);
        editProfileLayout = (LinearLayout) findViewById(R.id.editLayout);
        TextView classNameTV = (TextView) findViewById(R.id.className);
      //  schoolNameTV = (TextView) findViewById(R.id.school);
        TextView subscriberTV = (TextView) findViewById(R.id.memberCount);
        final TextView classCodeTV = (TextView) findViewById(R.id.classcode);

        //setting class code
        if(!UtilString.isBlank(classCode))
            classCodeTV.setText(classCode);

        //setting class name
        if(!UtilString.isBlank(className))
            classNameTV.setText(className);

     /*   //setting school name
        ParseQuery<ParseObject> classQuery = new ParseQuery<ParseObject>("Codegroup");
        classQuery.fromLocalDatastore();
        classQuery.whereEqualTo("code", classCode);
        try{
            ParseObject codegroup = classQuery.getFirst();
            schoolName = codegroup.getString("schoolName"); //this is a new field. If not present, fetch and store locally
            if(schoolName == null){
                //fetch school name from id using asynctask
                Log.d("DEBUG_SUBSCRIBERS", "schoolName not in codegroup. Fetching...");
                JoinedClassInfo.GetSchoolName getSchoolNameTask = new JoinedClassInfo.GetSchoolName(codegroup, 2);
                getSchoolNameTask.execute();
            }
            else{
                Log.d("DEBUG_SUBSCRIBERS", "schoolName already there " + schoolName);
                schoolNameTV.setText(schoolName);
            }
        }
        catch (ParseException e){
            Log.d("DEBUG_SUBSCRIBERS", "local query into Codegroup failed");
            schoolName = defaultSchoolName;
            schoolNameTV.setText(schoolName);
            e.printStackTrace();
        }*/

        //setting member count
        int memberCount = 0;

        try {
            memberCount = memberQuery.getMemberCount(classCode);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        subscriberTV.setText(memberCount + " subscribers");

        classCodeTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Utility.copyToClipBoard(Subscribers.this, "Class Code", classCodeTV.getText().toString());
            }
        });

        listv.setAdapter(myadapter);
        listv.setExpanded(true);

        //moving to invite parent activity on click of "invite parent"
        LinearLayout inviteLayout = (LinearLayout) findViewById(R.id.inviteLayout);
        inviteLayout.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getBaseContext(), InviteParents.class);
                intent.putExtra("classCode", classCode);
                intent.putExtra("className", className);
                startActivity(intent);
            }
        });

        /*
        //setting school name
        //get details(schoolName, profile pic, assigned name) from Codegroup and User table
        ParseQuery<ParseObject> classQuery = new ParseQuery<ParseObject>("Codegroup");
        classQuery.fromLocalDatastore();
        classQuery.whereEqualTo("code", classCode);
        String schoolName = "";

        try{
            ParseObject codegroup = classQuery.getFirst();
            schoolName = codegroup.getString("schoolName"); //this is a new field. If not present, fetch and store locally
            if(schoolName == null){
                //fetch school name from id using asynctask
                JoinedClassInfo.GetSchoolName getSchoolNameTask = new JoinedClassInfo.GetSchoolName(codegroup, 2);
                getSchoolNameTask.execute();
            }
            else{
                Log.d("DEBUG_JOINED_CLASS_INFO", "schoolName already there " + schoolName);
                schoolNameTV.setText(schoolName);
            }
        }
        catch (ParseException e){
            schoolNameTV.setText(schoolName);
            e.printStackTrace();
        }*/
    }

    /*
        * Initializing variables required for calling background process
        */
    public void intializeBackgroundParameters() {
        memberQuery = new Queries();

        if (memberDetails == null)
            memberDetails = new ArrayList<MemberDetails>();

        LocalMembers localMembers = new LocalMembers(classCode);
        localMembers.execute();

        myadapter = new myBaseAdapter();

    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu5, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.refresh:
                if (Utility.isInternetOn(this)) {

                    //showing progress bar
                    if (mHeaderProgressBar != null) {
                        Tools.runSmoothProgressBar(mHeaderProgressBar, 10);
                    }

                    //refreshing member list in background
                    MemberList memberList = new MemberList(classCode);
                    memberList.execute();
                } else {
                    Utility.toast("Check your Internet connection");
                }
                break;

            case android.R.id.home:
                onBackPressed();
                break;

            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    /*
    Adapter to show member list
    */
    class myBaseAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return memberDetails.size();
        }

        @Override
        public Object getItem(int position) {

            return memberDetails.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View row = convertView;

            if (row == null) {
                LayoutInflater layoutinflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                row = layoutinflater.inflate(R.layout.classmember_memberview, parent, false);
            }

            TextView membername = (TextView) row.findViewById(R.id.membernameid);
            final String name = ((MemberDetails) getItem(position)).getChildName();
            membername.setText(name);
            ImageView option_imageView = (ImageView) row.findViewById(R.id.joinOpt);

            row.setBackgroundDrawable(getResources().getDrawable(R.drawable.greyoutline));

        /*
           * Setting options for items
           */
            option_imageView.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {

                    final Context context = Subscribers.this;
                    AlertDialog.Builder alert = new AlertDialog.Builder(context);

                    if(! UtilString.isBlank(className))
                        alert.setTitle("Remove " + name + " from this class");

                    String message = "If you remove this user from your class, they will no longer " +
                            "receive updates from you. Are you sure want to remove the user from your class?";

                    alert.setMessage(message);

                    alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {

                                if (Utility.isInternetOn(Subscribers.this)) {
                                    RemoveChild removeChild = new RemoveChild(memberDetails.get(position), name);
                                    removeChild.execute();

                                    progressBarLayout.setVisibility(View.VISIBLE);
                                    editProfileLayout.setVisibility(View.GONE);
                                    dialog.dismiss();

                                }
                            }
                    });

                    alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            dialog.dismiss();
                        }
                    });

                    alert.show();
                }
            });


            return row;
        }
    }

    class LocalMembers extends AsyncTask<Void, Void, Void> {

        private String code;

        LocalMembers(String groupCode) {
            this.code = groupCode;
        }

        @Override
        protected Void doInBackground(Void... params) {

            memberDetails = memberQuery.getLocalClassMembers(code);
            return null;
        }

        @Override
        protected void onPostExecute(Void res) {

            if (myadapter != null)
                myadapter.notifyDataSetChanged();

        }
    }


    class RemoveChild extends AsyncTask<Void, Void, Boolean> {
        private MemberDetails member;
        private String memberName;

        RemoveChild(MemberDetails member, String memberName) {
            this.member = member;
            this.memberName = memberName;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            Utility.ls("remove child starting in background....");

            //Steps :
            //remove groupCode from member's joined group
            // unsubscribe this channel from member's channels
            //remove entry of that class from subscriber's joined group
            // send notification to user about removing from this group
            // set status to "REMOVED" table entry of memberlist

            Log.d("REMOVE", "remove child starting in ");

            if (member != null) {
                String type = member.getType();
                String objectId = member.getObjectId();
                String memberId = null;


                Log.d("REMOVE", "removal starting..");

                if (type.equals(MemberDetails.APP_MEMBER)) {
                    ParseQuery<ParseObject> query = ParseQuery.getQuery(Constants.GROUP_MEMBERS);
                    query.fromLocalDatastore();
                    query.whereEqualTo("objectId", objectId);
                    query.whereEqualTo("userId", ParseUser.getCurrentUser().getUsername());
                    ParseObject obj = null;

                    Log.d("REMOVE", "APP Member");

                    try {

                        obj = query.getFirst();
                        if (obj != null) {
                            memberId = obj.getString("emailId");

                            Log.d("REMOVE", "obj not null");
                            boolean isRemoved = false;

                            HashMap<String, String> param = new HashMap<String, String>();
                            param.put("classcode", classCode);
                            param.put("classname", className);
                            if(memberId != null)
                                param.put("emailId", memberId);
                            param.put("usertype", "app");

                            Log.d("REMOVE", "calling  start remove function");

                            isRemoved = ParseCloud.callFunction("removeMember", param);

                            Log.d("REMOVE", "calling remove function");


                            if(isRemoved){

                                obj.fetch();        //retrieving updates from server

                                /*******    check whether fetched obj saved automatically or not ********/
                                memberDetails = memberQuery.getLocalClassMembers(classCode);
                                return true;
                            }
                        }
                        else
                        {
                            Log.d("REMOVE", "got null object");
                        }
                    } catch (ParseException e) {
                        e.printStackTrace();

                        Log.d("REMOVE", "local query failed object");
                    }
                }
                else
                {
                    ParseQuery<ParseObject> query = ParseQuery.getQuery(Constants.MESSAGE_NEEDERS);
                    query.fromLocalDatastore();
                    query.whereEqualTo("objectId", objectId);
                    query.whereEqualTo("userId", ParseUser.getCurrentUser().getUsername());
                    ParseObject obj = null;

                    try {

                        obj = query.getFirst();
                        if (obj != null) {
                            String number = obj.getString("number");

                            boolean isRemoved = false;

                            HashMap<String, String> param = new HashMap<String, String>();
                            param.put("classcode", classCode);
                            param.put("classname", className);
                            param.put("number", number);
                            param.put("usertype", "sms");
                            isRemoved = ParseCloud.callFunction("removechild", param);

                            if(isRemoved){
                                obj.fetch();        //retrieving updates from server
                                return true;
                            }
                        }
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
            }

            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                memberDetails.remove(member);
                myadapter.notifyDataSetChanged();

                if(Classrooms.createdClassAdapter != null)
                    Classrooms.createdClassAdapter.notifyDataSetChanged();


                Utility.toast(memberName +" successfully removed from your classroom.");
            }
            else
                Utility.toast("Sorry, something went wrong. Try Again");

            progressBarLayout.setVisibility(View.GONE);
            editProfileLayout.setVisibility(View.VISIBLE);
        }
    }

}

