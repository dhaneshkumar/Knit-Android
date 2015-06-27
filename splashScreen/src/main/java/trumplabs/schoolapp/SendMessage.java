package trumplabs.schoolapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.parse.GetDataCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseUser;
import com.software.shell.fab.ActionButton;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import BackGroundProcesses.MemberList;
import BackGroundProcesses.SendPendingMessages;
import additionals.Invite;
import baseclasses.MyActionBarActivity;
import library.UtilString;
import trumplab.textslate.R;
import utility.Config;
import utility.Queries;
import utility.SessionManager;
import utility.Tools;
import utility.Utility;

/**
 * Send message to class and also show sent messages
 */
public class SendMessage extends MyActionBarActivity  {
    private ListView listv;                   //listview to show sent messages
    private static myBaseAdapter myadapter;        //Adapter for listview
    private int ACTION_MODE_NO;
    private ArrayList<ParseObject> selectedlistitems; // To delete selected messages
    private static String groupCode;      //class-code
    public static List<ParseObject> groupDetails;     // List of group messages
    private static String grpName;        //class-name
    private Queries query;
    public static LinearLayout progressLayout;
    private SessionManager session;
    public static int totalClassMessages; //total messages sent from this class
    public static LinearLayout contentLayout;
    public static Activity currentActivity;
    private RelativeLayout inviteLayout;
    private RelativeLayout memberLayout;
    public static LinearLayout picProgressBarLayout;
    private TextView memberCountTV;
    private boolean isLoading = false;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ccmsging_layout);

        if(getIntent()!= null && getIntent().getExtras() != null)
        {
            if(!UtilString.isBlank(getIntent().getExtras().getString("classCode"))) {
                groupCode = getIntent().getExtras().getString("classCode");
                grpName = getIntent().getExtras().getString("className");
            }
        }

        selectedlistitems = new ArrayList<ParseObject>();
        myadapter = new myBaseAdapter();
        ACTION_MODE_NO = 0;
        query = new Queries();
        currentActivity = this;

        listv = (ListView) findViewById(R.id.classmsglistview);   //list view
        contentLayout = (LinearLayout) findViewById(R.id.contentLayout);
        progressLayout = (LinearLayout) findViewById(R.id.progresslayout);
        inviteLayout = (RelativeLayout) findViewById(R.id.inviteLayout);
        memberLayout = (RelativeLayout) findViewById(R.id.memberLayout);
        picProgressBarLayout = (LinearLayout) findViewById(R.id.progressBarLayout);
        memberCountTV = (TextView) findViewById(R.id.memberCount);

        session = new SessionManager(Application.getAppContext());

        ParseUser userObject = ParseUser.getCurrentUser();

        //checking parse user null or not
        if (userObject == null)
        {
            Utility.logout(); return;}

        // retrieving sent messages of given class from local database
        try {
            groupDetails = query.getLocalCreateMsgs(groupCode, groupDetails, false);
        } catch (ParseException e) {
        }

        ClassMsgFunctions.updateTotalClassMessages(groupCode);

        if (groupDetails == null)
            groupDetails = new ArrayList<ParseObject>();


        initialiseListViewMethods();

        //setting listview adapter
        listv.setAdapter(myadapter);

        /*
        Setting custom view in action bar
         */
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        //   actionBar.setTitle("science");
        actionBar.setDisplayShowTitleEnabled(false);

        LayoutInflater inflator = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflator.inflate(R.layout.classmsg_action_view, null);

        TextView className = (TextView) v.findViewById(R.id.className);
        TextView classCodeTV = (TextView) v.findViewById(R.id.class_code);
        className.setText(grpName);
        classCodeTV.setText(groupCode);

        actionBar.setCustomView(v);

        //setting click action on action bar
        memberLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //hide keyword before transition
                Tools.hideKeyboard(SendMessage.this);

                Intent intent = new Intent(SendMessage.this, Subscribers.class);
                intent.putExtra("className", grpName);
                intent.putExtra("classCode", groupCode);

                startActivity(intent);
            }
        });

        inviteLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //hide keyword before transition
                Tools.hideKeyboard(SendMessage.this);
                Intent intent = new Intent(SendMessage.this, Invite.class);
                intent.putExtra("classCode", groupCode);
                intent.putExtra("className", grpName);
                intent.putExtra("inviteType", Constants.INVITATION_T2P);
                startActivity(intent);
            }
        });

        //FacebookSdk.sdkInitialize(getApplicationContext());

        try {
            Queries memberQuery = new Queries();
            int memberCount = memberQuery.getMemberCount(groupCode);

            memberCountTV.setText(memberCount+"");

            if(memberCount == 0 )
            {
                MemberList memberList = new MemberList(groupCode);
                memberList.execute();
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }


        //Initializing compose button
        final ActionButton actionButton = (ActionButton) findViewById(R.id.action_button);

        // To set button color for normal state:
        actionButton.setButtonColor(Color.parseColor("#039BE5"));

        // To set button color for pressed state:
        actionButton.setButtonColorPressed(Color.parseColor("#01579B"));

        //Setting image in floating button
        actionButton.setImageResource(R.drawable.ic_edit);

        // To enable or disable Ripple Effect:
        actionButton.setRippleEffectEnabled(true);

        actionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(SendMessage.this, ComposeMessage.class);
                intent.putExtra(Constants.ComposeSource.KEY, Constants.ComposeSource.INSIDE);
                intent.putExtra("CLASS_CODE", groupCode);
                intent.putExtra("CLASS_NAME", grpName);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        //facebook ad tracking
        // Logs 'install' and 'app activate' App Events.
        //AppEventsLogger.activateApp(this, Config.FB_APP_ID);
    }

    @Override
    protected void onPause() {
        super.onPause();

        //facebook tracking : time spent on app by people
        // Logs 'app deactivate' App Event.
        //AppEventsLogger.deactivateApp(this, Config.FB_APP_ID);
    }

    public static void notifyAdapter(){
        //view.post
        if(SendMessage.contentLayout != null) {
            SendMessage.contentLayout.post(new Runnable() {
                @Override
                public void run() {
                    if (myadapter != null) {
                        myadapter.notifyDataSetChanged();//just notify the pending->sent change
                    }
                }
            });
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        // Save UI state changes to the savedInstanceState.
        // This bundle will be passed to onCreate if the process is
        // killed and restarted.

        savedInstanceState.putString("groupCode", groupCode);
        savedInstanceState.putString("grpName", grpName);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // Restore UI state from the savedInstanceState.
        // This bundle has also been passed to onCreate.

        if(UtilString.isBlank(groupCode))
            groupCode = savedInstanceState.getString("groupCode");

        if(UtilString.isBlank(grpName))
            grpName = savedInstanceState.getString("grpName");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        switch (ACTION_MODE_NO) {
            case 0:
                inflater.inflate(R.menu.classmsg, menu);
                break;
            case 1:
                if (selectedlistitems.size() == 1)
                    inflater.inflate(R.menu.menu4, menu);
                else
                    inflater.inflate(R.menu.menu7, menu);
                break;
            default:
                break;
        }
        super.onCreateOptionsMenu(menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

    /*
     * case R.id.attachfile: FragmentManager fm = getActivity().getSupportFragmentManager();
     * ChooserDialog openchooser = new ChooserDialog(); openchooser.setTargetFragment(this, 500);
     * openchooser.show(fm, "Chooser Dialog"); break;
     */
            case android.R.id.home:
                Tools.hideKeyboard(SendMessage.this);
                onBackPressed();
                break;
            case R.id.copyCode:
                Utility.copyToClipBoard(this, "Class Code", groupCode);
                break;

            case R.id.copyicon:
                String txtcont = selectedlistitems.get(0).getString("title");
                Utility.copyToClipBoard(this, "label", txtcont);
                ACTION_MODE_NO = 0;
                supportInvalidateOptionsMenu();
                int index = groupDetails.indexOf(selectedlistitems.get(0));
                myadapter.notifyDataSetChanged();
                selectedlistitems.clear();
                Utility.toast("Message copied");
                break;
            case R.id.shareicon:
                String sharemsg = selectedlistitems.get(0).getString("title");
                ACTION_MODE_NO = 0;
                supportInvalidateOptionsMenu();
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, sharemsg);
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Great Post!!");
                startActivity(Intent.createChooser(shareIntent, "Share..."));
                int index1 = groupDetails.indexOf(selectedlistitems.get(0));
                myadapter.notifyDataSetChanged();
                selectedlistitems.clear();
                // listv.setSelection(index1);
                break;
            case R.id.deleteclassmenu:
                // Use the Builder class for convenient dialog construction
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("Delete Class? Are you sure?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {

                                if(! Utility.isInternetExist(SendMessage.this)) {
                                    Utility.toast("No internet Connection! Can't delete your class");
                                    return;
                                }

                                //hide keyword before transition
                                Tools.hideKeyboard(SendMessage.this);

                                //showing progress bar
                                contentLayout.setVisibility(View.GONE);
                                progressLayout.setVisibility(View.VISIBLE);

                                //calling background function to delete class

                                String[] params = new String[]{groupCode};
                                ClassMsgFunctions.deleteCreatedClass deleteCreatedClass = new ClassMsgFunctions.deleteCreatedClass();
                                deleteCreatedClass.execute(params);

                            }
                        }).setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
                builder.create().show();
                break;

            default:
                break;
        }
        return super.onOptionsItemSelected(item);

    }




    /*
    Setting adapter to show sent messages in list view
     */
    class myBaseAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return groupDetails.size();
        }

        @Override
        public Object getItem(int position) {
            return groupDetails.get(position).getString("title");
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        public ParseObject getObjectItem(int position) {
            return groupDetails.get(position);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            if (row == null) {
                LayoutInflater layoutinflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                row = layoutinflater.inflate(R.layout.ccmsging_msgview, parent, false);
            } else {
            }

            final ParseObject msg = groupDetails.get(position);  //selected object
            String stringmsg = (String) getItem(position);      //selected messages

            //retrieving the message sent time
            String timestampmsg = "";
            try {
                Date cdate = (Date) msg.get("creationTime");
                timestampmsg = Utility.convertTimeStamp(cdate);
            } catch (java.text.ParseException e) {
            }

            boolean pending = msg.getBoolean("pending"); //if this key is not available (for older messages)
            //get pending "false" & that's what we want
            if(pending){
                timestampmsg = "pending..";
            }


            final String imagepath;
            if (msg.containsKey("attachment_name"))
                imagepath = msg.getString("attachment_name");
            else
                imagepath = "";

            // initialize all the view components
            final ImageView imgmsgview = (ImageView) row.findViewById(R.id.ccimgmsg);
            final ProgressBar uploadprogressbar = (ProgressBar) row.findViewById(R.id.msgprogressbar);
            TextView msgtxtcontent = (TextView) row.findViewById(R.id.ccmsgtext);
            TextView classimage = (TextView) row.findViewById(R.id.classimage1);
            TextView classNameTV = (TextView) row.findViewById(R.id.classname1);

            //set like,seen and confused counts
            TextView likeCountArea = (TextView) row.findViewById(R.id.like);
            TextView confusedCountArea = (TextView) row.findViewById(R.id.confusion);
            TextView seenCountArea = (TextView) row.findViewById(R.id.seen);
            TextView retryButton = (TextView) row.findViewById(R.id.retry);

            if(pending){//this message is not yet sent
                seenCountArea.setVisibility(View.GONE);
                retryButton.setVisibility(View.VISIBLE);
                if(SendPendingMessages.isJobRunning() || ComposeMessage.sendButtonClicked){
                    retryButton.setClickable(false);
                    retryButton.setText("Sending");
                    retryButton.setTextColor(getResources().getColor(R.color.grey_light));
                }
                else{
                    retryButton.setClickable(true);
                    retryButton.setText(" Retry ");
                    retryButton.setTextColor(getResources().getColor(R.color.buttoncolor));

                    retryButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Log.d(SendPendingMessages.LOGTAG, "retry button clicked");
                            SendPendingMessages.spawnThread(true);
                            myadapter.notifyDataSetChanged();
                        }
                    });
                }
            }
            else{
                seenCountArea.setVisibility(View.VISIBLE);
                retryButton.setVisibility(View.GONE);
            }

            int likeCount = Utility.nonNegative(msg.getInt(Constants.LIKE_COUNT));
            int confusedCount = Utility.nonNegative(msg.getInt(Constants.CONFUSED_COUNT));
            int seenCount = Utility.nonNegative(msg.getInt(Constants.SEEN_COUNT));
            if(seenCount < likeCount + confusedCount){ //for consistency(SC >= LC + CC) - might not be correct though
                seenCount = likeCount + confusedCount;
            }

            likeCountArea.setText("" + likeCount);
            confusedCountArea.setText("" + confusedCount);
            seenCountArea.setText("seen by " + seenCount);

            String className= null;
            //setting class name
            if(msg.getString("name") != null) {
                classNameTV.setText(msg.getString("name"));
                className = msg.getString("name");
            }
            else
            {
                //previous version support < in the version from now onwards storing class name also>
                String groupCode = msg.getString("code");

                //Retrieving from shared preferences to access fast
                className =session.getClassName(groupCode);
                classNameTV.setText(className);
            }


            //setting background color of circular image
            GradientDrawable gradientdrawable = (GradientDrawable) classimage.getBackground();
            gradientdrawable.setColor(Color.parseColor(Utility.classColourCode(className.toUpperCase())));
            classimage.setText(className.substring(0, 1).toUpperCase());    //setting front end of circular image



            //ImageView tickview = (ImageView) row.findViewById(R.id.tickmark);
            final TextView timestampview = (TextView) row.findViewById(R.id.cctimestamp);
            row.setVisibility(View.VISIBLE);


            if(UtilString.isBlank(stringmsg))
                msgtxtcontent.setVisibility(View.GONE);
            else {
                msgtxtcontent.setText(stringmsg);
                msgtxtcontent.setVisibility(View.VISIBLE);
            }
            timestampview.setText(timestampmsg);

            uploadprogressbar.setVisibility(View.GONE);
            // /////////////////////////////////////////////
            if (!UtilString.isBlank(imagepath)) {

                /*
                Showoing the attached image
                 */
                imgmsgview.setVisibility(View.VISIBLE);
                uploadprogressbar.setTag("Progress");
                File imgFile = new File(Utility.getWorkingAppDir() + "/media/" + imagepath);
                final File thumbnailFile = new File(Utility.getWorkingAppDir() + "/thumbnail/" + imagepath);
                if (imgFile.exists() && !thumbnailFile.exists())
                    Utility.createThumbnail(SendMessage.this, imagepath);
                if (imgFile.exists()) {
                    // image file present locally then display it
                    Bitmap myBitmap = BitmapFactory.decodeFile(thumbnailFile.getAbsolutePath());

                    imgmsgview.setTag(imgFile.getAbsolutePath());
                    imgmsgview.setImageBitmap(myBitmap);
                    timestampview.setText(timestampmsg);
                } else {
                    // else download image from server and then display it
                    ParseFile imagefile = (ParseFile) msg.get("attachment");
                    uploadprogressbar.setVisibility(View.VISIBLE);
                    imagefile.getDataInBackground(new GetDataCallback() {
                        public void done(byte[] data, ParseException e) {
                            if (e == null) {
                                // ////Image download successful
                                FileOutputStream fos;
                                try {
                                    fos = new FileOutputStream(Utility.getWorkingAppDir() + "/media/" + imagepath);
                                    try {
                                        fos.write(data);
                                    } catch (IOException e1) {
                                        e1.printStackTrace();
                                    } finally {
                                        try {
                                            fos.close();
                                        } catch (IOException e1) {
                                            e1.printStackTrace();
                                        }
                                    }

                                } catch (FileNotFoundException e2) {
                                    e2.printStackTrace();
                                }

                                // //////////////////////////////////////////
                                Utility.createThumbnail(SendMessage.this, imagepath);
                                Bitmap mynewBitmap = BitmapFactory.decodeFile(thumbnailFile.getAbsolutePath());
                                imgmsgview.setImageBitmap(mynewBitmap);
                                uploadprogressbar.setVisibility(View.GONE);
                                // Might be a problem when net is too slow :/
                            } else {
                                // Image not downloaded
                                uploadprogressbar.setVisibility(View.GONE);
                            }
                        }
                    });

                    imgmsgview.setTag(Utility.getWorkingAppDir() + "/media/" + imagepath);
                    imgmsgview.setImageBitmap(null);
                    // imgmsgview.setVisibility(View.GONE);

                }
            } else {
                imgmsgview.setVisibility(View.GONE);
            }

            row.setBackgroundColor(getResources().getColor(R.color.transparent));
            if (ACTION_MODE_NO == 1 && selectedlistitems.contains(msg)) {
                row.setBackgroundColor(getResources().getColor(R.color.highlightcolor));
            }
            return row;
        }
    }

    public void initialiseListViewMethods() {

        /*
        On scrolling list view, load more messages from local storage
         */
        listv.setOnScrollListener(new AbsListView.OnScrollListener() {

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
                                 int totalItemCount) {

                int lastCount = groupDetails.size();

                if (firstVisibleItem + visibleItemCount >= totalItemCount && totalItemCount != 0) {
                    if(lastCount >= totalClassMessages){
                        return;
                    }

                    try {
                        if (lastCount >= Config.createMsgCount) {

                            if(!isLoading) {
                                isLoading = true;


                                groupDetails = query.getLocalCreateMsgs(groupCode, groupDetails, true);
                                myadapter.notifyDataSetChanged();
                                isLoading = false;
                            }
                        }
                    } catch (ParseException e) {
                    }
                }
            }
        });


        /*
        Setting options on clicking a list view item
         */
        listv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (ACTION_MODE_NO == 1) {
                    if (selectedlistitems.contains(myadapter.getObjectItem(position))) {
                        view.setBackgroundColor(getResources().getColor(R.color.transparent));
                        selectedlistitems.remove(myadapter.getObjectItem(position));
                    } else {
                        view.setBackgroundColor(getResources().getColor(R.color.highlightcolor));
                        selectedlistitems.add(myadapter.getObjectItem(position));
                    }
                    if (selectedlistitems.size() == 0) {
                        ACTION_MODE_NO = 0;
                        supportInvalidateOptionsMenu();
                    } else if (selectedlistitems.size() == 1 || selectedlistitems.size() == 2) {
                        supportInvalidateOptionsMenu();
                    }
                } else if (ACTION_MODE_NO == 0) {

                    //On clicking an image, will show that in gallery
                    final ImageView imgframell = (ImageView) view.findViewById(R.id.ccimgmsg);
                    imgframell.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                            if (imgframell.getVisibility() == View.VISIBLE) {
                                Intent imgintent = new Intent();
                                imgintent.setAction(Intent.ACTION_VIEW);
                                imgintent.setDataAndType(Uri.parse("file://" + (String) imgframell.getTag()), "image/*");
                                startActivity(imgintent);
                            }
                        }
                    });
                }
            }
        });
    }
}
