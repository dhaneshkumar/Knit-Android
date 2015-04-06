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
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.facebook.appevents.AppEventsLogger;
import com.parse.FunctionCallback;
import com.parse.GetDataCallback;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import BackGroundProcesses.MemberList;
import additionals.InviteParents;
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
public class SendMessage extends MyActionBarActivity implements ChooserDialog.CommunicatorInterface {
    private ListView listv;                   //listview to show sent messages
    private myBaseAdapter myadapter;        //Adapter for listview
    private int ACTION_MODE_NO;
    private ArrayList<ParseObject> selectedlistitems; // To delete selected messages
    public static String groupCode;      //class-code
    private List<ParseObject> groupDetails;     // List of group messages
    public static String grpName;        //class-name
    private String sender, userId;
    private Queries query;
    private String typedtxt;        //message to sent
    private TextView countview;
    private EditText typedmsg;
    public static LinearLayout sendimgpreview;
    private ImageView sendimgview;
    private ProgressBar updProgressBar;
    private ImageView attachView;
    public static LinearLayout progressLayout;
    private SessionManager session;
    public static int totalClassMessages; //total messages sent from this class
    public static LinearLayout contentLayout;
    public static Activity currentActivity;
    private LinearLayout inviteLayout;
    public static LinearLayout picProgressBarLayout;


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
        listv.setStackFromBottom(true);         //show message from bottom

        contentLayout = (LinearLayout) findViewById(R.id.contentLayout);
        progressLayout = (LinearLayout) findViewById(R.id.progresslayout);
        inviteLayout = (LinearLayout) findViewById(R.id.inviteLayout);
        picProgressBarLayout = (LinearLayout) findViewById(R.id.progressBarLayout);

        session = new SessionManager(Application.getAppContext());

        ParseUser userObject = ParseUser.getCurrentUser();
        //checking parse user null or not
        if (userObject == null)
        {
            Utility.logout(); return;}

        sender = userObject.getString(Constants.NAME);
        userId = userObject.getUsername();

        // retrieving sent messages of given class from local database
        try {
            groupDetails = query.getLocalCreateMsgs(groupCode, groupDetails, false);
        } catch (ParseException e) {
        }

        if (groupDetails == null)
            groupDetails = new ArrayList<ParseObject>();

        sendMsgMethod();
        initialiseListViewMethods();

        //setting action bar title as class name
        //((ActionBarActivity) this).getSupportActionBar().setTitle(grpName);

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
        className.setText(grpName);

        actionBar.setCustomView(v);

        //setting click action on action bar
        v.setOnClickListener(new View.OnClickListener() {
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
                Intent intent = new Intent(SendMessage.this, InviteParents.class);
                intent.putExtra("classCode", groupCode);
                intent.putExtra("className", grpName);
                startActivity(intent);
            }
        });

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

            final ParseObject groupdetails1 = groupDetails.get(position);  //selected object
            String stringmsg = (String) getItem(position);      //selected messages

            //retrieving the message sent time
            String timestampmsg = null;
            try {
                Date cdate = (Date) groupdetails1.get("creationTime");
                timestampmsg = Utility.convertTimeStamp(cdate);
            } catch (java.text.ParseException e) {
            }

            final String imagepath;
            if (groupdetails1.containsKey("attachment_name"))
                imagepath = groupdetails1.getString("attachment_name");
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


            int likeCount = Utility.nonNegative(groupdetails1.getInt(Constants.LIKE_COUNT));
            int confusedCount = Utility.nonNegative(groupdetails1.getInt(Constants.CONFUSED_COUNT));
            int seenCount = Utility.nonNegative(groupdetails1.getInt(Constants.SEEN_COUNT));
            if(seenCount < likeCount + confusedCount){ //for consistency(SC >= LC + CC) - might not be correct though
                seenCount = likeCount + confusedCount;
            }

            likeCountArea.setText("" + likeCount);
            confusedCountArea.setText("" + confusedCount);
            seenCountArea.setText("seen by " + seenCount);

            String className= null;
            //setting class name
            if(groupdetails1.getString("name") != null) {
                classNameTV.setText(groupdetails1.getString("name"));
                className = groupdetails1.getString("name");
            }
            else
            {
                //previous version support < in the version from now onwards storing class name also>
                String groupCode = groupdetails1.getString("code");


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
                    msgtxtcontent.setText(stringmsg);
                    imgmsgview.setTag(imgFile.getAbsolutePath());
                    imgmsgview.setImageBitmap(myBitmap);
                    timestampview.setText(timestampmsg);
                } else {
                    // else download image from server and then display it
                    ParseFile imagefile = (ParseFile) groupdetails1.get("attachment");
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
                                // myadapter.notifyDataSetChanged();
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

                    msgtxtcontent.setText(stringmsg);
                    imgmsgview.setTag(Utility.getWorkingAppDir() + "/media/" + imagepath);
                    imgmsgview.setImageBitmap(null);
                    // imgmsgview.setVisibility(View.GONE);
                    timestampview.setText(timestampmsg);

                }
            } else {
                imgmsgview.setVisibility(View.GONE);
                msgtxtcontent.setText(stringmsg);
                timestampview.setText(timestampmsg);
            }

            row.setBackgroundColor(getResources().getColor(R.color.transparent));
            if (ACTION_MODE_NO == 1 && selectedlistitems.contains(groupdetails1)) {
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

                if (firstVisibleItem < 2) {
                    if(lastCount >= totalClassMessages){
                        return;
                    }

                    try {
                        if (lastCount >= Config.createMsgCount) {
                            if(totalItemCount - firstVisibleItem >= lastCount) {
                                groupDetails = query.getLocalCreateMsgs(groupCode, groupDetails, true);
                                myadapter.notifyDataSetChanged();
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


    private void sendMsgMethod() {
        // Initializing all the views related to sending message view
        final ImageButton sendmsgbutton = (ImageButton) findViewById(R.id.sendmsgbttn);
        typedmsg = (EditText) findViewById(R.id.typedmsg);
        countview = (TextView) findViewById(R.id.lettercount);
        sendimgpreview = (LinearLayout) findViewById(R.id.imgpreview);
        sendimgview = (ImageView) findViewById(R.id.attachedimg);
        Button viewbutton = (Button) findViewById(R.id.viewbutton);
        Button removebutton = (Button) findViewById(R.id.removebutton);
        updProgressBar = (ProgressBar) findViewById(R.id.updprogressbar);
        updProgressBar.setVisibility(View.GONE);
        attachView = (ImageView) findViewById(R.id.gallery);

        typedmsg.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View arg0, MotionEvent arg1) {
                scrollMyListViewToBottom(); //show messages from bottom
                return false;
            }
        });

        /*
        Change send button color on entering some text and update the entered text counter
         */
        typedmsg.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count){}
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void afterTextChanged(Editable s) {
                countview.setText(300 - s.length() + "");
                if (s.length() >= 300)
                    countview.setTextColor(getResources().getColor(R.color.secondarycolor));
                else
                    countview.setTextColor(getResources().getColor(R.color.buttoncolor));

        /*
         * Changing sending button color
         */
                if (s.length() > 0) {
                    sendmsgbutton.setImageResource(R.drawable.send);

                } else
                    sendmsgbutton.setImageResource(R.drawable.send_grey);
            }

        });


        /*
        Current model is MI then hide attachview option
         */
        if (android.os.Build.MODEL != null)
        {
            String[] models = new String[]{"MI 3W", "MI 3", "MI 3S", "MI 3SW", "MI 4", "MI 4W",
                    "HM 1SW", "MI 1S", "MI 1SW", "MI 2", "MI 2W", "MI 2S", "MI 2SW", "MI 2A", "MI 2AW", "HM 1S"};

            if (Arrays.asList(models).contains(android.os.Build.MODEL.trim()))
                attachView.setVisibility(View.GONE);
            else
                Utility.ls(android.os.Build.MODEL);
        }

        attachView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                FragmentManager fm = getSupportFragmentManager();
                ChooserDialog openchooser = new ChooserDialog();
                openchooser.show(fm, "Add Image");
            }
        });


        //setting send message button clicked functionality
        sendmsgbutton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                /*
                For sending a message, you need atleast 1 subscriber
                 */
                int memberCount = 0;

                try {
                    Queries memberQuery = new Queries();
                    memberCount = memberQuery.getMemberCount(groupCode);
                } catch (ParseException e) {
                    e.printStackTrace();
                }

                if(memberCount < Config.SUBSCRIBER_MIN_LIMIT )
                {
                    Utility.toastLong("You don't have any subscriber right now. Invite subscribers to start messaging.");
                    return;
                }


                int hourOfDay = -1;
                if(session != null) {
                    //using local time instead of session.getCurrentTime
                    //Date now = session.getCurrentTime();
                    Calendar cal = Calendar.getInstance();
                    //cal.setTime(now);
                    hourOfDay = cal.get(Calendar.HOUR_OF_DAY);
                }


                if(hourOfDay != -1){

                    //If current message time is not sutaible <9PM- 6AM> then show this warning as popup to users
                    if(hourOfDay >= Config.messageNormalEndTime || hourOfDay < Config.messageNormalStartTime){
                        //note >= and < respectively because disallowed are [ >= EndTime and < StartTime]
                        AlertDialog.Builder builder = new AlertDialog.Builder(SendMessage.this);
                        LinearLayout warningView = new LinearLayout(SendMessage.this);
                        warningView.setOrientation(LinearLayout.VERTICAL);
                        LinearLayout.LayoutParams nameParams =
                                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,
                                        LinearLayout.LayoutParams.WRAP_CONTENT);
                        nameParams.setMargins(30, 30, 30, 30);

                        final TextView nameInput = new TextView(SendMessage.this);
                        nameInput.setTextSize(18);
                        nameInput.setText(Config.messageTimeWarning);
                        nameInput.setGravity(Gravity.CENTER_HORIZONTAL);
                        warningView.addView(nameInput, nameParams);
                        builder.setView(warningView);

                        builder.setPositiveButton("Send", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                sendFunction();
                            }
                        });
                        builder.setNegativeButton("Cancel", null);
                        AlertDialog dialog = builder.create();
                        dialog.show();
                    }
                    else{
                        sendFunction();
                    }
                }
                else{
                    sendFunction();
                }

            }

            /*
            Send messages to subscribers
             */
            public void sendFunction(){
                scrollMyListViewToBottom();  //show sent messages from bottom on clicking send button
                typedtxt = typedmsg.getText().toString().trim();  //message to send

                //check internet connection

                if(!Utility.isInternetExist(SendMessage.this)) {
                    return;
                }
                if (!typedtxt.equals("") && sendimgpreview.getVisibility() == View.GONE) {
                    // when its not an image message******************
                    sendTxtMsgtoSubscribers(typedtxt);

                } else if (sendimgpreview.getVisibility() == View.VISIBLE) {

                    // Sending an image file
                    try {
                        // passing image file path and message content as
                        // parameters
                        sendPic((String) sendimgpreview.getTag(), typedtxt);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    // for image we try to keep track of progress
                    typedmsg.setText("");
                    sendimgpreview.setTag("");
                    sendimgview.setImageBitmap(null);
                    sendimgpreview.setVisibility(View.GONE);
                }
            }
        });

        // View the image ready to be sent
        viewbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent imgintent = new Intent();
                imgintent.setAction(Intent.ACTION_VIEW);
                imgintent.setDataAndType(Uri.parse("file://" + (String) sendimgpreview.getTag()), "image/*");
                startActivity(imgintent);
            }
        });

        // remove the image ready to be sent
        removebutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendimgpreview.setTag("");
                sendimgview.setImageBitmap(null);
                sendimgpreview.setVisibility(View.GONE);
            }
        });
    }

    private void sendTxtMsgtoSubscribers(final String typedtxt) {

        //adding item to the listview
        final ParseObject groupDetails1 = new ParseObject("GroupDetails");
        groupDetails1.put("code", groupCode);
        groupDetails1.put("title", typedtxt);
        groupDetails1.put("Creator", sender);
        groupDetails1.put("name", grpName);
        groupDetails1.put("senderId", userId);

        groupDetails.add(groupDetails1);
        typedmsg.setText("");
        myadapter.notifyDataSetChanged();
        updProgressBar.setVisibility(View.VISIBLE);


        //sending message using parse cloud function
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("classcode", groupCode);
        params.put("classname", grpName);
        params.put("message", typedtxt);


        ParseCloud.callFunctionInBackground("sendTextMessage", params, new FunctionCallback<HashMap>() {
            @Override
            public void done(HashMap obj, ParseException e) {

                if (e == null) {
                    if (obj != null) {

                        Date createdAt = (Date) obj.get("createdAt");
                        String objectId = (String) obj.get("messageId");

                        updProgressBar.setVisibility(View.GONE);

                        ParseObject sentMsg = new ParseObject("SentMessages");
                        sentMsg.put("objectId", objectId);
                        sentMsg.put("Creator", groupDetails1.getString("Creator"));
                        sentMsg.put("code", groupDetails1.getString("code"));
                        sentMsg.put("title", groupDetails1.getString("title"));
                        sentMsg.put("name", groupDetails1.getString("name"));
                        sentMsg.put("creationTime", createdAt);
                        sentMsg.put("senderId", userId);
                        sentMsg.put("userId", userId);
                        try {
                            sentMsg.pin();
                        } catch (ParseException e1) {
                            e1.printStackTrace();
                        }


                        //update outbox message count
                        Outbox.updateOutboxTotalMessages();

                        ParseQuery<ParseObject> query = ParseQuery.getQuery("SentMessages");
                        query.fromLocalDatastore();
                        query.orderByDescending("creationTime");
                        query.whereEqualTo("userId", userId);
                        query.whereEqualTo("code", groupCode);

                        List<ParseObject> msgList1;
                        try {
                            msgList1 = query.find();

                            groupDetails.clear();
                            if (msgList1 != null) {
                                for (int i = 0; i < msgList1.size(); i++) {
                                    groupDetails.add(0, msgList1.get(i));
                                }
                            }
                        } catch (ParseException e1) {
                        }

                        //updating local time
                        SessionManager sm = new SessionManager(Application.getAppContext());
                        if (createdAt != null) {
                            sm.setCurrentTime(createdAt);
                        }

                        //showing popup
                        Utility.toastDone("Notification Sent");

                        //updating outbox
                        Queries outboxQuery = new Queries();

                        List<ParseObject> outboxItems = outboxQuery.getLocalOutbox();
                        if (outboxItems != null) {
                            Outbox.groupDetails = outboxItems;

                            if (Outbox.myadapter != null)
                                Outbox.myadapter.notifyDataSetChanged();

                            if (Outbox.outboxLayout != null && Outbox.groupDetails.size() > 0)
                                Outbox.outboxLayout.setVisibility(View.GONE);
                        }

                    }
                } else {
                    // message was not sent
                    groupDetails.remove(groupDetails1); //removing entry from list view
                    myadapter.notifyDataSetChanged();
                    Utility.toast("Oops! Message wasn't sent. Try Again!");
                    updProgressBar.setVisibility(View.GONE);
                    e.printStackTrace();

                }
            }
        });
    }

    @Override
    public void sendImagePic(String imgname) {

        // The image was brought into the App folder hence only name was passed
        sendimgpreview.setVisibility(View.VISIBLE);
        sendimgpreview.setTag(Utility.getWorkingAppDir() + "/media/" + imgname);
        File thumbnailFile = new File(Utility.getWorkingAppDir() + "/thumbnail/" + imgname);

        // The thumbnail is already created
        Bitmap myBitmap = BitmapFactory.decodeFile(thumbnailFile.getAbsolutePath());
        sendimgview.setImageBitmap(myBitmap);
    }

    // Send Image Pic
    private void sendPic(String filepath, String txtmsg) throws IOException {

        // /Creating ParseFile (Not yet uploaded)
        int slashindex = ((String) sendimgpreview.getTag()).lastIndexOf("/");
        final String fileName = ((String) sendimgpreview.getTag()).substring(slashindex + 1);// image file //

        RandomAccessFile f = new RandomAccessFile(filepath, "r");
        byte[] data = new byte[(int) f.length()];
        f.read(data);
        final ParseFile file = new ParseFile(fileName, data);

        // /saving the sent image message details on App//////////////////////
        final ParseObject groupDetails1 = new ParseObject("GroupDetails");

        //Adding this object to list view
        groupDetails1.put("code", groupCode);
        groupDetails1.put("title", txtmsg);
        groupDetails1.put("Creator", sender);
        groupDetails1.put("name", grpName);
        groupDetails1.put("senderId", userId);
        groupDetails1.put("attachment_name", fileName);
        groupDetails.add(groupDetails1);
        myadapter.notifyDataSetChanged();
        // Now the image is shown in the list view

        // //Uploading the image file/////////////////////
        updProgressBar.setVisibility(View.VISIBLE);

        file.saveInBackground(new SaveCallback() {
            public void done(ParseException e) {

                if (e == null) {
                    //file uploading completed
                    updProgressBar.setVisibility(View.GONE);

                    //sending the message details to server since file is uploaded
                    int index = groupDetails.indexOf(groupDetails1);
                    if (index >= 0)
                        groupDetails.set(index, groupDetails1);// replacing with the


                    //sending message using parse cloud function
                    HashMap<String, Object> msg = new HashMap<String, Object>();
                    msg.put("classcode", groupCode);
                    msg.put("classname", grpName);
                    msg.put("message", typedtxt);
                    msg.put("filename", fileName);
                    msg.put("parsefile", file);


                    ParseCloud.callFunctionInBackground("sendPhotoTextMessage", msg, new FunctionCallback<HashMap>() {
                        @Override
                        public void done(HashMap obj, ParseException e) {

                            if (e == null) {
                                if (obj != null) {

                                    Date createdAt = (Date) obj.get("createdAt");
                                    String objectId = (String) obj.get("messageId");

                                    updProgressBar.setVisibility(View.GONE);

                                    ParseObject sentMsg = new ParseObject("SentMessages");
                                    sentMsg.put("objectId", objectId);
                                    sentMsg.put("Creator", groupDetails1.getString("Creator"));
                                    sentMsg.put("code", groupDetails1.getString("code"));
                                    sentMsg.put("title", groupDetails1.getString("title"));
                                    sentMsg.put("name", groupDetails1.getString("name"));
                                    sentMsg.put("creationTime", createdAt);
                                    sentMsg.put("senderId", userId);
                                    sentMsg.put("userId", userId);
                                    if (file != null)
                                        sentMsg.put("attachment", file);
                                    if (fileName != null)
                                        sentMsg.put("attachment_name", fileName);

                                    //saving locally
                                    try {
                                        sentMsg.pin();
                                    } catch (ParseException e1) {
                                        e1.printStackTrace();
                                    }

                                    // Removing old object
                                    groupDetails.remove(groupDetails1);

                                    //update outbox message count
                                    Outbox.updateOutboxTotalMessages();

                                    ParseQuery<ParseObject> query = ParseQuery.getQuery("SentMessages");
                                    query.fromLocalDatastore();
                                    query.orderByDescending("creationTime");
                                    query.whereEqualTo("userId", userId);
                                    query.whereEqualTo("code", groupCode);

                                    List<ParseObject> msgList1;
                                    try {
                                        msgList1 = query.find();

                                        groupDetails.clear();
                                        if (msgList1 != null) {
                                            for (int i = 0; i < msgList1.size(); i++) {
                                                groupDetails.add(0, msgList1.get(i));
                                            }
                                        }
                                    } catch (ParseException e1) {
                                    }

                                    //showing popup
                                    Utility.toastDone("Notification Sent");

                                    //updating outbox
                                    Queries outboxQuery = new Queries();

                                    List<ParseObject> outboxItems = outboxQuery.getLocalOutbox();
                                    if (outboxItems != null) {
                                        Outbox.groupDetails = outboxItems;

                                        if (Outbox.myadapter != null)
                                            Outbox.myadapter.notifyDataSetChanged();

                                        if (Outbox.outboxLayout != null && Outbox.groupDetails.size() > 0)
                                            Outbox.outboxLayout.setVisibility(View.GONE);
                                    }
                                }
                            }
                        }
                    });


                } else {
                    updProgressBar.setVisibility(View.GONE);
                    Utility.toast("Sorry, Can't sent this image.");
                    groupDetails.remove(groupDetails1);
                    myadapter.notifyDataSetChanged();
                }
            }
        });
    }

    /*
     * scroll to bottom
     */
    private void scrollMyListViewToBottom() {
        listv.post(new Runnable() {
            @Override
            public void run() {
                // Select the last row so it will scroll into view...
                listv.setSelection(myadapter.getCount() - 1);
            }
        });
    }





}
