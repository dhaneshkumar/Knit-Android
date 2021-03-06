package trumplabs.schoolapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
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
import android.widget.FrameLayout;
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
import utility.ImageCache;
import utility.Queries;
import utility.Utility;

/**
 * Send message to class and also show sent messages
 */
public class SendMessage extends MyActionBarActivity  {
    private ListView listv;                   //listview to show sent messages
    private static myBaseAdapter myadapter;        //Adapter for listview
    private int ACTION_MODE_NO;
    private ArrayList<ParseObject> selectedlistitems; // To delete selected messages
    public static String groupCode;      //class-code
    public static List<ParseObject> groupDetails;     // List of group messages

    String grpName;        //class-name

    private Queries query;
    public static LinearLayout progressLayout;
    public static int totalClassMessages; //total messages sent from this class
    public static LinearLayout contentLayout;
    public static Activity currentActivity;
    private RelativeLayout inviteLayout;
    private RelativeLayout memberLayout;
    public static LinearLayout picProgressBarLayout;

    public static TextView memberCountTV;
    public static TextView memberLabelTV;

    private Typeface typeface;
    private ImageView empty_class_bg;

    boolean pushOpen = false; //set to true when directly opened through notification click,

    public boolean  isGetLocalCreateMessagesRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.ccmsging_layout);

        if(getIntent()!= null && getIntent().getExtras() != null)
        {
            pushOpen = getIntent().getExtras().getBoolean("pushOpen", false); //optional when opened via push
            getIntent().removeExtra("pushOpen");

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
        //todo remove this as not used
        picProgressBarLayout = (LinearLayout) findViewById(R.id.progressBarLayout);
        memberCountTV = (TextView) findViewById(R.id.memberCount);
        memberLabelTV = (TextView) findViewById(R.id.memberLabel);

        typeface = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Light.ttf");
        empty_class_bg = (ImageView) findViewById(R.id.sent_class_bg);

        ParseUser userObject = ParseUser.getCurrentUser();

        //checking parse user null or not
        if (userObject == null) {
            Utility.LogoutUtility.logout();
            return;
        }

        groupDetails = new ArrayList<>(); //important since now its static variable so need to reset


        if(!isGetLocalCreateMessagesRunning){
            if(Config.SHOWLOG) Log.d("_FETCH_OLD", "spawning GetLocalCreateMessages");
            GetLocalCreateMessages getLocalCreateMessages = new GetLocalCreateMessages(0, false);
            isGetLocalCreateMessagesRunning = true;
            getLocalCreateMessages.execute();
        }

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

                Intent intent = new Intent(SendMessage.this, Subscribers.class);
                intent.putExtra("className", grpName);
                intent.putExtra("classCode", groupCode);

                startActivity(intent);
            }
        });

        inviteLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(SendMessage.this, Invite.class);
                intent.putExtra("classCode", groupCode);
                intent.putExtra("className", grpName);
                intent.putExtra("inviteType", Constants.INVITATION_T2P);
                startActivity(intent);
            }
        });

        //FacebookSdk.sdkInitialize(getApplicationContext());

        int memberCount = MemberList.getMemberCount(groupCode);
        memberCountTV.setText(memberCount+"");
        memberLabelTV.setText("Member" + Utility.getPluralSuffix(memberCount));


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

        /*if(Application.getCurrentActivity() != null) {
            if(Config.SHOWLOG) Log.d("__A", "currentActivity=" + Application.getCurrentActivity().getClass().getSimpleName() + "; SendMessage's name=" + SendMessage.class.getSimpleName());
        }*/

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
        notifyAdapter(null);
    }

    //used if called in background thread because groupDetails should not change without notifyDataSetChanged
    public static void notifyAdapter(final List<ParseObject> msgsToRemove){
        //applicationHandler.post
        if(Application.applicationHandler != null) {
            Application.applicationHandler.post(new Runnable() {
                @Override
                public void run() {
                    if(groupCode != null){
                        ClassMsgFunctions.updateTotalClassMessages(groupCode);
                    }

                    if (groupDetails != null && myadapter != null) {
                        if(msgsToRemove != null){
                            groupDetails.removeAll(msgsToRemove);
                        }
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

                                if(! Utility.isInternetExist()) {
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


    class RunnableBundle{
        public Runnable openRunnable;
        public Runnable onImageSuccessRunnable;
        public Runnable onFileSuccessRunnable;
        public Runnable onFailRunnable;
        public Runnable retryRunnable;
    }

    /*
    Setting adapter to show sent messages in list view
     */
    class myBaseAdapter extends BaseAdapter {
        @Override
        public int getCount() {

            int size = groupDetails.size();

            if(size ==0)
                empty_class_bg.setVisibility(View.VISIBLE);
            else
                empty_class_bg.setVisibility(View.GONE);

            return size;
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
                row = layoutinflater.inflate(R.layout.created_class_messages_item, parent, false);
            } else {
            }

            final ParseObject msgObject = groupDetails.get(position);  //selected object
            String stringmsg = (String) getItem(position);      //selected messages

            //retrieving the message sent time
            String timestampmsg = "";
            Date cdate = (Date) msgObject.get("creationTime");
            timestampmsg = Utility.MyDateFormatter.getInstance().convertTimeStampNew(cdate);

            boolean pending = msgObject.getBoolean("pending"); //if this key is not available (for older messages)
            //get pending "false" & that's what we want
            if(pending){
                timestampmsg = "pending..";
            }


            final String imageName;
            if (msgObject.containsKey("attachment_name"))
                imageName = msgObject.getString("attachment_name");
            else
                imageName = "";

            // initialize all the view components
            final TextView timestampview = (TextView) row.findViewById(R.id.cctimestamp);
            final ImageView pendingClockIcon = (ImageView) row.findViewById(R.id.pendingClock);

            final FrameLayout imgframelayout = (FrameLayout) row.findViewById(R.id.imagefrmlayout);
            final ImageView imgmsgview = (ImageView) row.findViewById(R.id.ccimgmsg);
            final TextView attachmentNameTV = (TextView) row.findViewById(R.id.attachment_name);
            final TextView faildownload = (TextView) row.findViewById(R.id.faildownload);

            final ProgressBar uploadprogressbar = (ProgressBar) row.findViewById(R.id.msgprogressbar);
            TextView msgtxtcontent = (TextView) row.findViewById(R.id.ccmsgtext);
            TextView classimage = (TextView) row.findViewById(R.id.classimage1);
            TextView classNameTV = (TextView) row.findViewById(R.id.classname1);

            //set like,seen and confused counts
            TextView likeCountArea = (TextView) row.findViewById(R.id.like);
            TextView confusedCountArea = (TextView) row.findViewById(R.id.confusion);
            TextView seenCountArea = (TextView) row.findViewById(R.id.seen);
            TextView retryButton = (TextView) row.findViewById(R.id.retry);
            LinearLayout root = (LinearLayout) row.findViewById(R.id.rootLayout);
            LinearLayout head = (LinearLayout) row.findViewById(R.id.headLayout);

            int currentapiVersion = android.os.Build.VERSION.SDK_INT;
            if (currentapiVersion >= Build.VERSION_CODES.LOLLIPOP) {
                root.setBackground(getResources().getDrawable(R.drawable.messages_item_background));
                head.setBackground(getResources().getDrawable(R.drawable.greyoutline));
            }

            if(pending){//this message is not yet sent
                timestampview.setVisibility(View.GONE);
                pendingClockIcon.setVisibility(View.VISIBLE);

                //do other stuff
                seenCountArea.setVisibility(View.GONE);
                retryButton.setVisibility(View.VISIBLE);
                if(SendPendingMessages.isJobRunning() || ComposeMessage.sendButtonClicked){
                    retryButton.setClickable(false);
                    retryButton.setText("Sending..");
                    retryButton.setTextColor(getResources().getColor(R.color.grey_light));
                }
                else{
                    retryButton.setClickable(true);
                    retryButton.setText(" Retry ");
                    retryButton.setTextColor(getResources().getColor(R.color.buttoncolor));

                    retryButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if(Config.SHOWLOG) Log.d(SendPendingMessages.LOGTAG, "retry button clicked");
                            if(Utility.isInternetExist()) {
                                SendPendingMessages.spawnThread(true);
                                myadapter.notifyDataSetChanged();
                            }
                        }
                    });
                }
            }
            else{
                timestampview.setVisibility(View.VISIBLE);
                pendingClockIcon.setVisibility(View.GONE);

                seenCountArea.setVisibility(View.VISIBLE);
                retryButton.setVisibility(View.GONE);
            }

            int likeCount = Utility.nonNegative(msgObject.getInt(Constants.GroupDetails.LIKE_COUNT));
            int confusedCount = Utility.nonNegative(msgObject.getInt(Constants.GroupDetails.CONFUSED_COUNT));
            int seenCount = Utility.nonNegative(msgObject.getInt(Constants.GroupDetails.SEEN_COUNT));
            if(seenCount < likeCount + confusedCount){ //for consistency(SC >= LC + CC) - might not be correct though
                seenCount = likeCount + confusedCount;
            }

            likeCountArea.setText("" + likeCount);
            confusedCountArea.setText("" + confusedCount);
            seenCountArea.setText("seen by " + seenCount);

            String className= null;
            //setting class name
            if(msgObject.getString("name") != null) {
                classNameTV.setText(msgObject.getString("name"));
                className = msgObject.getString("name");
            }
            else
            {
                //previous version support < in the version from now onwards storing class name also>
                String groupCode = msgObject.getString("code");

                ParseObject codegroup = Queries.getCodegroupObject(groupCode);
                if(codegroup != null ) {
                    String name = codegroup.getString(Constants.Codegroup.NAME);
                    if(!UtilString.isBlank(name))
                    {
                        className = name;
                        classNameTV.setText(className);
                    }
                }
            }


            //setting background color of circular image
            GradientDrawable gradientdrawable = (GradientDrawable) classimage.getBackground();
            gradientdrawable.setColor(Color.parseColor(Utility.classColourCode(className.toUpperCase())));
            classimage.setText(className.substring(0, 1).toUpperCase());    //setting front end of circular image
            classimage.setTypeface(typeface);


            //ImageView tickview = (ImageView) row.findViewById(R.id.tickmark);
            row.setVisibility(View.VISIBLE);


            if(stringmsg != null){
                if(stringmsg.length() >= Config.attachmentMessage.length()) {
                    int candidateIndex = stringmsg.length()-Config.attachmentMessage.length();
                    String candidate = stringmsg.substring(candidateIndex);
                    if(candidate.equals(Config.attachmentMessage)){
                        stringmsg = stringmsg.substring(0, candidateIndex);
                        if(!pending){
                            msgObject.put("title", stringmsg); //alert pending message content would be changed
                            msgObject.pinInBackground(); //Remove the extra note
                        }
                    }
                }
            }

            if(UtilString.isBlank(stringmsg))
                msgtxtcontent.setVisibility(View.GONE);
            else {
                msgtxtcontent.setText(stringmsg);
                msgtxtcontent.setVisibility(View.VISIBLE);
            }
            timestampview.setText(timestampmsg);

            //for text messages when recycled
            uploadprogressbar.setVisibility(View.GONE);

            // /////////////////////////////////////////////

            if (!UtilString.isBlank(imageName)) {
                final boolean isFileAnImage = Utility.isFileImageType(imageName);
                final String imageFilePath = Utility.getFileLocationInAppFolder(imageName);
                final File imgFile = new File(imageFilePath);
                imgmsgview.setTag(imgFile.getAbsolutePath());

                final RunnableBundle rb = new RunnableBundle();

                rb.openRunnable = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (isFileAnImage) {
                                Intent imgintent = new Intent();
                                imgintent.setAction(Intent.ACTION_VIEW);
                                imgintent.setDataAndType(Uri.parse("file://" + imageFilePath), "image/*");
                                startActivity(imgintent);
                            } else {
                                //assume any kind of file. only teacher has restriction on what kind of file he can send(currently pdf)
                                //while opening, assume any type of file
                                String mimeType = Utility.getMimeType(imageName); //non null return value
                                //Utility.toast(imageName + " with mime=" + mimeType, false, 15);
                                File file = new File(imageFilePath);
                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                intent.setDataAndType(Uri.fromFile(file), mimeType);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                                startActivity(intent);
                            }
                        }
                        catch (ActivityNotFoundException e){
                            String extension = Utility.getExtension(imageName);
                            Utility.toast("No app installed to open file type " + extension, 15);
                            e.printStackTrace();
                        }
                    }
                };

                rb.onImageSuccessRunnable = new Runnable() {
                    @Override
                    public void run() {
                        if(! Utility.isTagSame(imgmsgview, imgFile.getAbsolutePath())){
                            Log.d("__sleep", "onImageSuccessRunnable skip different tag " + imageName);
                            return;
                        }
                        uploadprogressbar.setVisibility(View.GONE);
                        attachmentNameTV.setVisibility(View.GONE);
                        faildownload.setVisibility(View.GONE);

                        imgframelayout.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                rb.openRunnable.run();
                            }
                        });
                    }
                };

                rb.onFileSuccessRunnable = new Runnable() {
                    @Override
                    public void run() {
                        if(! Utility.isTagSame(imgmsgview, imgFile.getAbsolutePath())){
                            Log.d("__sleep", "onFileSuccessRunnable skip different tag " + imageName);
                            return;
                        }
                        uploadprogressbar.setVisibility(View.GONE);
                        attachmentNameTV.setText(imageName);
                        attachmentNameTV.setVisibility(View.VISIBLE);
                        faildownload.setVisibility(View.GONE);

                        int resId = Utility.getMessageIconResource(imageName);
                        imgmsgview.setImageResource(resId);

                        imgframelayout.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                rb.openRunnable.run();
                            }
                        });
                    }
                };

                rb.onFailRunnable = new Runnable() {
                    @Override
                    public void run() {
                        if(! Utility.isTagSame(imgmsgview, imgFile.getAbsolutePath())){
                            Log.d("__sleep", "onFailRunnable skip different tag " + imageName);
                            return;
                        }
                        uploadprogressbar.setVisibility(View.GONE);
                        attachmentNameTV.setVisibility(View.GONE);
                        faildownload.setVisibility(View.VISIBLE);
                        imgframelayout.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                rb.retryRunnable.run();
                            }
                        });
                    }
                };

                rb.retryRunnable = new Runnable() {
                    @Override
                    public void run() {
                        //to override previous recycled view
                        imgframelayout.setVisibility(View.VISIBLE);
                        imgframelayout.setOnClickListener(null); //override always

                        imgmsgview.setImageBitmap(null);//because in recycleview is reused, hence need to initialize properly
                        uploadprogressbar.setVisibility(View.VISIBLE);
                        faildownload.setVisibility(View.GONE);
                        attachmentNameTV.setVisibility(View.GONE);

                        if(ImageCache.showIfInCache(imageName, imgmsgview)){
                            if(Config.SHOWLOG) Log.d(ImageCache.LOGTAG, "(m) already cached : " + imageName);
                            rb.onImageSuccessRunnable.run();
                        }
                        else if (imgFile.exists()) {
                            // image file present locally
                            if(isFileAnImage) {
                                ImageCache.WriteLoadAndShowTask writeLoadAndShowTask = new ImageCache.WriteLoadAndShowTask(null, imageName, imgmsgview, currentActivity, rb.onImageSuccessRunnable);
                                writeLoadAndShowTask.execute();
                            }
                            else{
                                if(Config.SHOWLOG) Log.d("__file_picker", "m) exists " + imageName);
                                //set file icon and run onSuccessRunnable
                                rb.onFileSuccessRunnable.run();
                            }
                        } else if(Utility.isInternetExistWithoutPopup()) {
                            if(Config.SHOWLOG) Log.d("__file_picker", "m) downloading " + imageName);
                            if(Config.SHOWLOG) Log.d(ImageCache.LOGTAG, "(m) downloading data : " + imageName);

                            // Have to download image from server
                            final ParseFile imagefile = msgObject.getParseFile("attachment");

                            if(imagefile != null) {
                                imagefile.getDataInBackground(new GetDataCallback() {
                                    public void done(byte[] data, ParseException e) {
                                        if (e == null) {
                                            if (isFileAnImage) {
                                                ImageCache.WriteLoadAndShowTask writeLoadAndShowTask = new ImageCache.WriteLoadAndShowTask(data, imageName, imgmsgview, currentActivity, rb.onImageSuccessRunnable);
                                                writeLoadAndShowTask.execute();
                                            } else {
                                                ImageCache.WriteDocTask writeDocTask = new ImageCache.WriteDocTask(data, imageName, imgmsgview, currentActivity, rb.onFileSuccessRunnable);
                                                writeDocTask.execute();
                                            }

                                        } else {
                                            //ParseException check for invalid session
                                            Utility.LogoutUtility.checkAndHandleInvalidSession(e);
                                            rb.onFailRunnable.run();
                                        }
                                    }
                                });

                                /*Runnable r = new Runnable() {
                                    @Override
                                    public void run() {
                                        imagefile.getDataInBackground(new GetDataCallback() {
                                            public void done(byte[] data, ParseException e) {
                                                e = new ParseException(1000, "dummy parse exception");

                                                if (e == null) {
                                                    if(isFileAnImage) {
                                                        ImageCache.WriteLoadAndShowTask writeLoadAndShowTask = new ImageCache.WriteLoadAndShowTask(data, imageName, imgmsgview, currentActivity, rb.onImageSuccessRunnable);
                                                        writeLoadAndShowTask.execute();
                                                    }
                                                    else{
                                                        ImageCache.WriteDocTask writeDocTask = new ImageCache.WriteDocTask(data, imageName, imgmsgview, currentActivity, rb.onFileSuccessRunnable);
                                                        writeDocTask.execute();
                                                    }

                                                } else {
                                                    //ParseException check for invalid session
                                                    Utility.LogoutUtility.checkAndHandleInvalidSession(e);
                                                    rb.onFailRunnable.run();
                                                }
                                            }
                                        });
                                    }
                                };

                                TestingUtililty.runAfterUiThread(r, 5000, "__sleep" + imageName);*/
                            }
                            else{
                                rb.onFailRunnable.run();
                            }
                        }
                        else {
                            rb.onFailRunnable.run();
                        }
                    }
                };

                rb.retryRunnable.run();
            } else {
                imgframelayout.setVisibility(View.GONE);
                attachmentNameTV.setVisibility(View.GONE);
                imgmsgview.setTag(""); //reset to empty
            }

            row.setBackgroundColor(getResources().getColor(R.color.transparent));
            if (ACTION_MODE_NO == 1 && selectedlistitems.contains(msgObject)) {
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
                    if(Config.SHOWLOG) Log.d("D_SEND_SCROLL", "showing " + totalItemCount + " totalpinnned " + totalClassMessages);
                    if(lastCount >= totalClassMessages){
                        if(Config.SHOWLOG) Log.d("D_SEND_SCROLL", "[" + (visibleItemCount + firstVisibleItem) + " out of" + totalClassMessages + "]all messages loaded. Saving unnecessary query");
                        return;
                    }

                    if(!isGetLocalCreateMessagesRunning){
                        if(Config.SHOWLOG) Log.d("_FETCH_OLD", "spawning GetLocalCreateMessages onScroll");
                        GetLocalCreateMessages getLocalCreateMessages = new GetLocalCreateMessages(totalItemCount, true);
                        isGetLocalCreateMessagesRunning = true;
                        getLocalCreateMessages.execute();
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

    //called when all local messages shown
    class GetLocalCreateMessages extends AsyncTask<Void, Void, Void>
    {
        int totalItemCount;
        List<ParseObject> extraMessages = null;
        boolean calledOnScroll = false;

        GetLocalCreateMessages(int totalItemCount, boolean onScroll){
            this.totalItemCount = totalItemCount;
            this.calledOnScroll = onScroll;
        }

        @Override
        protected Void doInBackground(Void... params) {

            if(!calledOnScroll) {//i.e called in onCreate
                extraMessages = query.getLocalCreateMsgs(groupCode, groupDetails, false);
                ClassMsgFunctions.updateTotalClassMessages(groupCode);
            }
            else{
                extraMessages = query.getLocalCreateMsgs(groupCode, groupDetails, true);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            isGetLocalCreateMessagesRunning = false;

            if(extraMessages == null && calledOnScroll){
                totalClassMessages = totalItemCount;
            }

            if(extraMessages != null){
                if(groupDetails == null){
                    groupDetails = extraMessages;
                }
                else{
                    groupDetails.addAll(extraMessages);
                }
            }

            if(myadapter != null) {
                myadapter.notifyDataSetChanged();
            }

            super.onPostExecute(aVoid);
        }
    }
}
