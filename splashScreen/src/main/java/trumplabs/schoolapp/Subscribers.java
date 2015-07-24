package trumplabs.schoolapp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
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

import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import BackGroundProcesses.MemberList;
import additionals.Invite;
import baseclasses.MyActionBarActivity;
import fr.castorflex.android.smoothprogressbar.SmoothProgressBar;
import library.ExpandableListView;
import library.UtilString;
import trumplab.textslate.R;
import utility.Config;
import utility.Queries;
import utility.Tools;
import utility.Utility;

/**
 * Show subscriber list of a class
 */
public class Subscribers extends MyActionBarActivity {
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
    private TextView emptyTV;
    public static TextView subscriberTV;
    private int memberCount = 0;
    private LinearLayout memberProgressBar;

    static String defaultSchoolName = "";

    boolean pushOpen = false; //set to true when directly opened through notification click,

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.classmembers_layout);

        //setting action bar title
        getSupportActionBar().setTitle("Members");

        //Adding home back button
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if(getIntent()!= null && getIntent().getExtras() != null)
        {
            pushOpen = getIntent().getExtras().getBoolean("pushOpen", false); //optional when opened via push
            getIntent().removeExtra("pushOpen");

            if(!UtilString.isBlank(getIntent().getExtras().getString("classCode"))) {
                classCode = getIntent().getExtras().getString("classCode");
                className = getIntent().getExtras().getString("className");
            }
        }

        intializeBackgroundParameters();

        //intializing variables
        listv = (ExpandableListView) findViewById(R.id.memberslistview);
        mHeaderProgressBar = (SmoothProgressBar) findViewById(R.id.ptr_progress);
        progressBarLayout = (LinearLayout) findViewById(R.id.progressBarLayout);
        editProfileLayout = (LinearLayout) findViewById(R.id.editLayout);
        TextView classNameTV = (TextView) findViewById(R.id.className);
      //  schoolNameTV = (TextView) findViewById(R.id.school);
         subscriberTV = (TextView) findViewById(R.id.memberCount);
        final TextView classCodeTV = (TextView) findViewById(R.id.classcode);
        emptyTV = (TextView) findViewById(R.id.emptyTV);
        memberProgressBar = (LinearLayout) findViewById(R.id.memberBarLayout);

        //setting class code
        if(!UtilString.isBlank(classCode))
            classCodeTV.setText(classCode);

        //setting class name
        if(!UtilString.isBlank(className))
            classNameTV.setText(className);

        //setting member count
        int memberCount = 0;

        try {
            memberCount = MemberList.getMemberCount(classCode);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        if(memberCount == 0)
            emptyTV.setVisibility(View.VISIBLE);
        else
            emptyTV.setVisibility(View.GONE);

        subscriberTV.setText(memberCount + " Member" + Utility.getPluralSuffix(memberCount));

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
                Intent intent = new Intent(getBaseContext(), Invite.class);
                intent.putExtra("classCode", classCode);
                intent.putExtra("className", className);
                intent.putExtra("inviteType", Constants.INVITATION_T2P);
                startActivity(intent);
            }
        });

        FacebookSdk.sdkInitialize(getApplicationContext());

        if(pushOpen){
            refresh(); //fetch subscribers for the class, classCode has now been set, gui also created
        }
    }



    @Override
    protected void onResume() {
        super.onResume();

        //facebook ad tracking
        // Logs 'install' and 'app activate' App Events.
        AppEventsLogger.activateApp(this, Config.FB_APP_ID);
    }

    @Override
    protected void onPause() {
        super.onPause();

        //facebook tracking : time spent on app by people
        // Logs 'app deactivate' App Event.
        AppEventsLogger.deactivateApp(this, Config.FB_APP_ID);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        // Save UI state changes to the savedInstanceState.
        // This bundle will be passed to onCreate if the process is
        // killed and restarted.

        savedInstanceState.putString("classCode", classCode);
        savedInstanceState.putString("className", className);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // Restore UI state from the savedInstanceState.
        // This bundle has also been passed to onCreate.

        if(UtilString.isBlank(classCode))
            classCode = savedInstanceState.getString("classCode");

        if(UtilString.isBlank(className))
            className = savedInstanceState.getString("className");
    }

    /*
        * Initializing variables required for calling background process
        */
    public void intializeBackgroundParameters() {
        memberQuery = new Queries();

       // if (memberDetails == null)  //no need to check null otherwise it causes one memberlist is loaded in other classroom in case of high volumes
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
                refresh();
                break;

            case android.R.id.home:
                onBackPressed();
                break;

            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if(pushOpen){
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
        else{
            super.onBackPressed();
        }
    }

    void refresh(){
        if(Utility.isInternetExist()) {

            //showing progress bar
            if (mHeaderProgressBar != null) {
                Tools.runSmoothProgressBar(mHeaderProgressBar, 10);
            }

            //refreshing member list in background
            MemberList memberList = new MemberList(classCode);
            memberList.execute();
        }
    }

    /*
    Adapter to show member list
    */
    class myBaseAdapter extends BaseAdapter {

        @Override
        public int getCount() {

            if(memberDetails.size() == 0)
                emptyTV.setVisibility(View.VISIBLE);
            else
                emptyTV.setVisibility(View.GONE);

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
            ImageView memberIcon = (ImageView) row.findViewById(R.id.memberIcon);

            row.setBackgroundDrawable(getResources().getDrawable(R.drawable.greyoutline));


            MemberDetails member = memberDetails.get(position);
            if (member != null) {
                String type = member.getType();

                if (type.equals(MemberDetails.APP_MEMBER)) {
                    int sdk = android.os.Build.VERSION.SDK_INT;
                    if(sdk < android.os.Build.VERSION_CODES.JELLY_BEAN) {
                        memberIcon.setBackgroundDrawable( getResources().getDrawable(R.drawable.android) );
                    } else {
                        memberIcon.setBackground( getResources().getDrawable(R.drawable.android));
                    }
                }
                else
                {
                    int sdk = android.os.Build.VERSION.SDK_INT;
                    if(sdk < android.os.Build.VERSION_CODES.JELLY_BEAN) {
                        memberIcon.setBackgroundDrawable( getResources().getDrawable(R.drawable.sms) );
                    } else {
                        memberIcon.setBackground( getResources().getDrawable(R.drawable.sms));
                    }
                }
                }


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

                            if(Utility.isInternetExist()) {
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

            //hide progressbar
            memberProgressBar.setVisibility(View.GONE);

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

                            Log.d("REMOVE", "calling : removeMember function");

                            isRemoved = ParseCloud.callFunction("removeMember", param);

                            Log.d("REMOVE", "call over : removeMember function");

                            if(isRemoved){
                                Log.d("REMOVE", "success - just unpin it");
                                obj.unpin(); //just unpin for now. Next time if it is fetched using MemberList task, it will have updated value for status column

                                /*******    check whether fetched obj saved automatically or not ********/
                                memberDetails = memberQuery.getLocalClassMembers(classCode);

                                if(memberDetails != null)
                                    memberCount = memberDetails.size();

                                MemberList.setMemberCount(classCode, memberCount);  //updating codegroup
                                return true;
                            }
                        }
                        else
                        {
                            Log.d("REMOVE", "got null object");
                        }
                    } catch (ParseException e) {
                        Utility.checkAndHandleInvalidSession(e);
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
                            isRemoved = ParseCloud.callFunction("removeMember", param);

                            if(isRemoved){
                                obj.unpin(); //just unpin for now. Next time if it is fetched using MemberList task, it will have updated field for status

                                /*******    check whether fetched obj saved automatically or not ********/
                                memberDetails = memberQuery.getLocalClassMembers(classCode);

                                if(memberDetails != null) {
                                    memberCount = memberDetails.size();
                                    MemberList.setMemberCount(classCode, memberCount);  //updating codegroup
                                }
                                return true;
                            }
                        }
                    } catch (ParseException e) {
                        Utility.checkAndHandleInvalidSession(e);
                        e.printStackTrace();
                    }
                }
            }

            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                if(memberDetails != null)
                    memberDetails.remove(member);

                if(myadapter != null)
                    myadapter.notifyDataSetChanged();

                //updating member count display view

                if(memberCount == 0)
                    emptyTV.setVisibility(View.VISIBLE);
                else
                    emptyTV.setVisibility(View.GONE);

                subscriberTV.setText(memberCount + " Member" + Utility.getPluralSuffix(memberCount));


                if(SendMessage.memberCountTV != null && SendMessage.memberLabelTV != null) {
                    SendMessage.memberCountTV.setText(memberCount + "");
                    SendMessage.memberLabelTV.setText("Member" + Utility.getPluralSuffix(memberCount));
                }

                Utility.toast(memberName + " successfully removed from your classroom.");
            }
            else
                Utility.toast("Sorry, something went wrong.\n Try Again");

            progressBarLayout.setVisibility(View.GONE);
            editProfileLayout.setVisibility(View.VISIBLE);
        }
    }
}

