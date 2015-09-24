package trumplabs.schoolapp;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.github.amlcurran.showcaseview.ShowcaseView;
import com.parse.GetDataCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import BackGroundProcesses.Refresher;
import BackGroundProcesses.SendPendingMessages;
import BackGroundProcesses.SyncMessageDetails;
import library.UtilString;
import trumplab.textslate.R;
import tutorial.ShowcaseCreator;
import utility.Config;
import utility.ImageCache;
import utility.Queries;
import utility.SessionManager;
import utility.Utility;

/**
 * Outbox Activity showing Outbox fragment of homepage
 */
public class Outbox extends Fragment {
    protected LayoutInflater layoutinflater;
    public static RecycleAdapter myadapter;
    private static RecyclerView outboxListv;
    private Queries query;
    public static List<ParseObject> groupDetails; // List of group messages
    Activity myActivity;
    private LinearLayoutManager mLayoutManager;
    private static SwipeRefreshLayout outboxRefreshLayout;
    static LinearLayout outboxLayout;
    public static int totalOutboxMessages = 15; //total pinned outbox messages(across all classes)
    private Typeface typeface;
    ImageView emptyBackground;
    ProgressBar loadingBar;

    private static int selectedMsgIndex = -1;

    //handle notification
    private static String action; //LIKE/CONFUSE
    private static String id; //msg object id

    public static boolean responseTutorialShown = false; //show in shared prefs

    public boolean isGetExtraLocalOutboxMsgsRunning = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        layoutinflater = inflater;
        View layoutview = inflater.inflate(R.layout.outbox, container, false);
        return layoutview;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        //intializing variables
        outboxListv = (RecyclerView) getActivity().findViewById(R.id.outboxlistview);
        myActivity = getActivity();
        query = new Queries();
        outboxRefreshLayout = (SwipeRefreshLayout) getActivity().findViewById(R.id.ptr_outbox);
        outboxRefreshLayout.setColorSchemeColors(Color.RED, Color.GREEN, Color.BLUE, Color.MAGENTA);
        outboxLayout = (LinearLayout) getActivity().findViewById(R.id.outboxmsg);
        typeface = Typeface.createFromAsset(getActivity().getAssets(), "fonts/Roboto-Light.ttf");
        loadingBar = (ProgressBar) getActivity().findViewById(R.id.sent_messages_pb);
        emptyBackground = (ImageView) getActivity().findViewById(R.id.sent_messages_bg);

        //handle receive notification action - LIKE/CONFUSE
        if(getActivity().getIntent() != null){
            Bundle bundle = getActivity().getIntent().getExtras();
            if(bundle != null){
                action = bundle.getString("action");
                id = bundle.getString("id");
                getActivity().getIntent().removeExtra("action"); //we must handle it one time only
            }
        }

        //initialize msgs as it is static and can be persistent from previous user who logged out
        groupDetails = new ArrayList<>();

        //fetching locally stored outbox messages
        GetLocalOutboxMsgInBackground getLocalOutboxMsg = new GetLocalOutboxMsgInBackground();
        getLocalOutboxMsg.execute();

        //setting recycle view & layout
        mLayoutManager = new LinearLayoutManager(getActivity());
        outboxListv.setLayoutManager(mLayoutManager);
        myadapter = new RecycleAdapter();
        outboxListv.setAdapter(myadapter);

        emptyBackground.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), ComposeMessage.class);
                intent.putExtra(Constants.ComposeSource.KEY, Constants.ComposeSource.OUTSIDE); //i.e not from within a particular classroom's page
                startActivity(intent);
            }
        });

     /*
     * On scrolling down the list view display extra messages.
     */
        outboxListv.setOnScrollListener(new RecyclerView.OnScrollListener() {
            int lastCount = 0;

            @Override
            public void onScrolled(RecyclerView view, int dx, int dy) {

                int visibleItemCount = mLayoutManager.getChildCount();
                int totalItemCount = mLayoutManager.getItemCount();
                int pastVisibleItems = mLayoutManager.findFirstVisibleItemPosition();

                if (visibleItemCount + pastVisibleItems >= totalItemCount - 1) {
                    if(Config.SHOWLOG) Log.d("D_OUTBOX_SCROLL", "showing " + totalItemCount + " totalpinnned " + totalOutboxMessages);
                    if (totalItemCount >= totalOutboxMessages) {
                        if(Config.SHOWLOG) Log.d("D_OUTBOX_SCROLL", "[" + (visibleItemCount + pastVisibleItems) + " out of" + totalOutboxMessages + "]all messages loaded. Saving unnecessary query");
                        return; //nothing to do as all messages have been loaded
                    }

                    if(!isGetExtraLocalOutboxMsgsRunning){
                        if(Config.SHOWLOG) Log.d("_FETCH_OLD", "spawning GetExtraLocalOutboxMsgs");
                        GetExtraLocalOutboxMsgs getExtraLocalOutboxMsgs = new GetExtraLocalOutboxMsgs(totalItemCount);
                        isGetExtraLocalOutboxMsgsRunning = true;
                        getExtraLocalOutboxMsgs.execute();
                    }
                }
            }

            @Override
            public void onScrollStateChanged(RecyclerView view, int newState) {
            }
        });


        /*
        Refreshing Messages on pulling refresh layout
         */
        outboxRefreshLayout.setColorSchemeColors(Color.RED, Color.GREEN, Color.BLUE, Color.MAGENTA);
        outboxRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {

                // mHeaderProgressBar.setVisibility(View.GONE);

                if (Utility.isInternetExist()) {

                    //code to refresh outbox
                    refreshCountInBackground();

                    //stop the refresh in refresCountInBackground() method in the view.post

                    //start handler for 10 secs.  <to stop refreshbar>
                    final Handler h = new Handler() {
                        @Override
                        public void handleMessage(Message message) {

                            outboxRefreshLayout.setRefreshing(false);
                        }
                    };
                    h.sendMessageDelayed(new Message(), 10000);
                } else {

                    //start handler for 2 secs.  <to stop refreshbar>
                    final Handler h = new Handler() {
                        @Override
                        public void handleMessage(Message message) {

                            outboxRefreshLayout.setRefreshing(false);
                        }
                    };
                    h.sendMessageDelayed(new Message(), 2000);
                }

            }
        });

        if(Refresher.isSufficientGapOutbox() && Utility.isInternetExistWithoutPopup()){
            if(Config.SHOWLOG) Log.d("DEBUG_MESSAGES", "calling Outbox update since sufficient gap");

            if(outboxRefreshLayout!=null){
                outboxRefreshLayout.postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        if (outboxRefreshLayout != null) {
                            runSwipeRefreshLayout(outboxRefreshLayout, 10);
                        }
                        outboxRefreshLayout.setRefreshing(true);
                    }
                }, 1000);
            }
            //update outbox in background
            refreshCountInBackground();
        }
        else{
            if(Config.SHOWLOG) Log.d("DEBUG_MESSAGES", "skipping outbox update : gap " + Refresher.isSufficientGapOutbox());
        }
    }

    public static void notifyAdapter(){
        notifyAdapter(null);
    }

    //used if called in background thread because groupDetails should not change(atleast msgs removed) without notifyDataSetChanged
    public static void notifyAdapter(final List<ParseObject> msgsToRemove){
        //applicationHandler.post
        if(Application.applicationHandler != null) {
            Application.applicationHandler.post(new Runnable() {
                @Override
                public void run() {
                    updateOutboxTotalMessages();
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //validating option menu
        setHasOptionsMenu(true);
    }

    class RunnableBundle{
        public Runnable openRunnable;
        public Runnable onImageSuccessRunnable;
        public Runnable onFileSuccessRunnable;
        public Runnable onFailRunnable;
        public Runnable retryRunnable;
    }

    /**
     * Holder class to hold all elements of an item
     */
    class ViewHolder extends RecyclerView.ViewHolder {
        TextView timestampmsg;
        ImageView pendingClockIcon;
        TextView classimage;
        FrameLayout imgframelayout;
        ImageView imgmsgview;
        TextView attachmentNameTV;
        TextView faildownload;
        ProgressBar uploadprogressbar;
        TextView msgtxtcontent;
        TextView classname;
        TextView likes;
        TextView confused;
        TextView seen;
        RelativeLayout root;
        LinearLayout head;
        TextView retryButton;

        //constructor
        public ViewHolder(View row) {
            super(row);

            classname = (TextView) row.findViewById(R.id.classname1);
            classimage = (TextView) row.findViewById(R.id.classimage1);
            timestampmsg = (TextView) row.findViewById(R.id.cctimestamp);
            pendingClockIcon = (ImageView) row.findViewById(R.id.pendingClock);
            imgframelayout = (FrameLayout) row.findViewById(R.id.imagefrmlayout);
            imgmsgview = (ImageView) row.findViewById(R.id.ccimgmsg);
            attachmentNameTV = (TextView) row.findViewById(R.id.attachment_name);

            faildownload = (TextView) row.findViewById(R.id.faildownload);
            uploadprogressbar = (ProgressBar) row.findViewById(R.id.msgprogressbar);
            msgtxtcontent = (TextView) row.findViewById(R.id.ccmsgtext);
            likes = (TextView) row.findViewById(R.id.like);
            confused = (TextView) row.findViewById(R.id.confusion);
            seen = (TextView) row.findViewById(R.id.seen);
            root = (RelativeLayout) row.findViewById(R.id.rootLayout);
            head = (LinearLayout) row.findViewById(R.id.headLayout);
            retryButton = (TextView) row.findViewById(R.id.retry);

        }
    }


    /**
     * Adapter for recycleview of outbox
     */
    class RecycleAdapter extends RecyclerView.Adapter<ViewHolder> {


        @Override
        public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int position) {

            View row = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.outbox_item, viewGroup, false);
            ViewHolder holder = new ViewHolder(row);
            return holder;
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, final int position) {
            final ParseObject msgObject = groupDetails.get(position);

            if (msgObject == null) return;

            //setting message in view
            String msg = msgObject.getString("title");
            if(msg != null){
                if(msg.length() >= Config.attachmentMessage.length()) {
                    int candidateIndex = msg.length()-Config.attachmentMessage.length();
                    String candidate = msg.substring(candidateIndex);
                    if(candidate.equals(Config.attachmentMessage)){
                        msg = msg.substring(0, candidateIndex);
                        //msgObject.put("title", msg); don't do this o/w pending message content would be changed
                        //msgObject.pinInBackground(); //Remove the extra note
                    }
                }
            }
            if (msg == null || msg.trim().equals(""))
                holder.msgtxtcontent.setVisibility(View.GONE);
            else
                holder.msgtxtcontent.setVisibility(View.VISIBLE);
            holder.msgtxtcontent.setText(msg);

            String className = null;
            //setting class name
            if (msgObject.getString("name") != null) {
                holder.classname.setText(msgObject.getString("name"));
                className = msgObject.getString("name");
            } else {
                //previous version support < in the version from now onwards storing class name also>
                String groupCode = msgObject.getString("code");

                ParseObject codegroup = Queries.getCodegroupObject(groupCode);

                if(codegroup != null ) {
                    String name = codegroup.getString(Constants.Codegroup.NAME);
                    if(!UtilString.isBlank(name))
                    {
                        className = name;
                        holder.classname.setText(className);
                    }
                }
            }


            int likeCount = Utility.nonNegative(msgObject.getInt(Constants.GroupDetails.LIKE_COUNT));
            int confusedCount = Utility.nonNegative(msgObject.getInt(Constants.GroupDetails.CONFUSED_COUNT));
            int seenCount = Utility.nonNegative(msgObject.getInt(Constants.GroupDetails.SEEN_COUNT));
            if (seenCount < likeCount + confusedCount) {//for consistency(SC >= LC + CC) - might not be correct though
                seenCount = likeCount + confusedCount;
            }

            holder.likes.setText("" + likeCount);
            holder.confused.setText("" + confusedCount);
            holder.seen.setText("seen by " + seenCount);

            //setting background color of circular image
            GradientDrawable gradientdrawable = (GradientDrawable) holder.classimage.getBackground();
            gradientdrawable.setColor(Color.parseColor(Utility.classColourCode(className.toUpperCase())));
            holder.classimage.setText(className.substring(0, 1).toUpperCase());    //setting front end of circular image
            holder.classimage.setTypeface(typeface);
            /*
            Retrieving timestamp
             */
            String timestampmsg = "";
            Date cdate = msgObject.getCreatedAt();

            if (cdate == null)
                cdate = (Date) msgObject.get("creationTime");

            //finding difference of current & createdAt timestamp
            timestampmsg = Utility.MyDateFormatter.getInstance().convertTimeStampNew(cdate);

            //setting cardview for higher api using elevation

            final int currentapiVersion = android.os.Build.VERSION.SDK_INT;
            if (currentapiVersion >= Build.VERSION_CODES.LOLLIPOP) {

                if (selectedMsgIndex != -1) {

                if (position == selectedMsgIndex) {
                    holder.head.setBackgroundDrawable(getResources().getDrawable(R.drawable.greyoutline_selected));

                    holder.root.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            selectedMsgIndex = -1;
                            holder.head.setBackgroundDrawable(getResources().getDrawable(R.drawable.greyoutline));
                        }
                    });
                } else
                    holder.head.setBackgroundDrawable(getResources().getDrawable(R.drawable.greyoutline));
                }
            }
            else {
                if (selectedMsgIndex != -1) {
                    if (position == selectedMsgIndex) {
                        holder.head.setBackgroundDrawable(getResources().getDrawable(R.drawable.outbox_item_selected));
                        holder.head.setPadding(0, 24, 0, 24);

                        holder.root.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                selectedMsgIndex = -1;
                                holder.head.setBackgroundDrawable(getResources().getDrawable(R.drawable.outbox_item_shadow));
                                holder.head.setPadding(0, 24, 0, 24);
                            }
                        });

                    } else {
                        holder.head.setBackgroundDrawable(getResources().getDrawable(R.drawable.outbox_item_shadow));
                        holder.head.setPadding(0, 24, 0, 24);
                    }
                }
            }

            boolean pending = msgObject.getBoolean("pending"); //if this key is not available (for older messages)
            if (pending) {
                holder.timestampmsg.setVisibility(View.GONE);
                holder.pendingClockIcon.setVisibility(View.VISIBLE);
            }
            else{
                holder.timestampmsg.setVisibility(View.VISIBLE);
                holder.pendingClockIcon.setVisibility(View.GONE);
                holder.timestampmsg.setText(timestampmsg);
            }





            //retry button handle
            if (pending) {//this message is not yet sent
                holder.seen.setVisibility(View.GONE);
                holder.retryButton.setVisibility(View.VISIBLE);
                if (SendPendingMessages.isJobRunning() || ComposeMessage.sendButtonClicked) {
                    holder.retryButton.setClickable(false);
                    holder.retryButton.setText("sending..");
                    holder.retryButton.setTextColor(getResources().getColor(R.color.grey_light));
                } else {
                    holder.retryButton.setClickable(true);
                    holder.retryButton.setText(" Retry ");
                    holder.retryButton.setTextColor(getResources().getColor(R.color.buttoncolor));
                    holder.retryButton.setOnClickListener(new View.OnClickListener() {
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
            } else {
                holder.seen.setVisibility(View.VISIBLE);
                holder.retryButton.setVisibility(View.GONE);
            }



            /*
            Retrieving image attachment if exist
             */
            final String imageName;
            if (msgObject.containsKey("attachment_name"))
                imageName = msgObject.getString("attachment_name");
            else
                imageName = "";


            //for text messages when recycled
            holder.uploadprogressbar.setVisibility(View.GONE);

            //If image attachment exist, display image
            if (!UtilString.isBlank(imageName)) {
                final boolean isFileAnImage = Utility.isFileImageType(imageName);

                Log.d("__file_picker", imageName);
                final String imageFilePath = Utility.getFileLocationInAppFolder(imageName);
                final File imgFile = new File(imageFilePath);
                holder.imgmsgview.setTag(imgFile.getAbsolutePath());

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
                        if(! Utility.isTagSame(holder.imgmsgview, imgFile.getAbsolutePath())){
                            Log.d("__sleep", "onImageSuccessRunnable skip different tag " + imageName);
                            return;
                        }
                        holder.uploadprogressbar.setVisibility(View.GONE);
                        holder.attachmentNameTV.setVisibility(View.GONE);
                        holder.faildownload.setVisibility(View.GONE);

                        holder.imgframelayout.setOnClickListener(new View.OnClickListener() {
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
                        if(! Utility.isTagSame(holder.imgmsgview, imgFile.getAbsolutePath())){
                            Log.d("__sleep", "onFileSuccessRunnable skip different tag " + imageName);
                            return;
                        }
                        holder.uploadprogressbar.setVisibility(View.GONE);
                        holder.attachmentNameTV.setText(imageName);
                        holder.attachmentNameTV.setVisibility(View.VISIBLE);
                        holder.faildownload.setVisibility(View.GONE);

                        int resId = Utility.getMessageIconResource(imageName);
                        holder.imgmsgview.setImageResource(resId);
                        holder.imgframelayout.setOnClickListener(new View.OnClickListener() {
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
                        if(! Utility.isTagSame(holder.imgmsgview, imgFile.getAbsolutePath())){
                            Log.d("__sleep", "onFailRunnable skip different tag " + imageName);
                            return;
                        }
                        holder.uploadprogressbar.setVisibility(View.GONE);
                        holder.attachmentNameTV.setVisibility(View.GONE);
                        holder.faildownload.setVisibility(View.VISIBLE);
                        holder.imgframelayout.setOnClickListener(new View.OnClickListener() {
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
                        holder.imgframelayout.setVisibility(View.VISIBLE);
                        holder.imgframelayout.setOnClickListener(null); //override always

                        holder.imgmsgview.setImageBitmap(null);//because in recycleview is reused, hence need to initialize properly
                        holder.uploadprogressbar.setVisibility(View.VISIBLE);
                        holder.faildownload.setVisibility(View.GONE);
                        holder.attachmentNameTV.setVisibility(View.GONE);


                        if(ImageCache.showIfInCache(imageName, holder.imgmsgview)){
                            if(Config.SHOWLOG) Log.d(ImageCache.LOGTAG, "(m) already cached : " + imageName);
                            rb.onImageSuccessRunnable.run();
                        }
                        else if (imgFile.exists()) {
                            // image file present locally
                            if(isFileAnImage) {
                                ImageCache.WriteLoadAndShowTask writeLoadAndShowTask = new ImageCache.WriteLoadAndShowTask(null, imageName, holder.imgmsgview, getActivity(), rb.onImageSuccessRunnable);
                                writeLoadAndShowTask.execute();
                            }
                            else{
                                Log.d("__file_picker", "m) exists " + imageName);
                                //set file icon and run onSuccessRunnable
                                rb.onFileSuccessRunnable.run();
                            }
                        } else if(Utility.isInternetExistWithoutPopup()) {
                            Log.d("__file_picker", "m) downloading " + imageName);
                            if(Config.SHOWLOG) Log.d(ImageCache.LOGTAG, "(m) downloading data : " + imageName);

                            // Have to download image from server
                            final ParseFile imagefile = msgObject.getParseFile("attachment");

                            if (imagefile != null) {
                                imagefile.getDataInBackground(new GetDataCallback() {
                                    public void done(byte[] data, ParseException e) {
                                        if (e == null) {
                                            if (isFileAnImage) {
                                                ImageCache.WriteLoadAndShowTask writeLoadAndShowTask = new ImageCache.WriteLoadAndShowTask(data, imageName, holder.imgmsgview, getActivity(), rb.onImageSuccessRunnable);
                                                writeLoadAndShowTask.execute();
                                            } else {
                                                ImageCache.WriteDocTask writeDocTask = new ImageCache.WriteDocTask(data, imageName, holder.imgmsgview, getActivity(), rb.onFileSuccessRunnable);
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
                                                        ImageCache.WriteLoadAndShowTask writeLoadAndShowTask = new ImageCache.WriteLoadAndShowTask(data, imageName, holder.imgmsgview, getActivity(), rb.onImageSuccessRunnable);
                                                        writeLoadAndShowTask.execute();
                                                    }
                                                    else{
                                                        ImageCache.WriteDocTask writeDocTask = new ImageCache.WriteDocTask(data, imageName, holder.imgmsgview, getActivity(), rb.onFileSuccessRunnable);
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
                holder.imgframelayout.setVisibility(View.GONE);
                holder.imgmsgview.setTag(""); //reset to empty
                holder.attachmentNameTV.setVisibility(View.GONE);
            }

            //if a) first msg, b) is a teacher & c) already not shown
            ParseUser currentParseUser = ParseUser.getCurrentUser();

            if(currentParseUser == null){
                return;
            }

            if(Application.mainActivityVisible && position == 0 && !responseTutorialShown && MainActivity.fragmentVisible == 0 && currentParseUser != null && currentParseUser.getString(Constants.ROLE).equals(Constants.TEACHER) && !ShowcaseView.isVisible){
                if(Config.SHOWLOG) Log.d("_TUTORIAL_", "outbox response tutorial entered");
                String tutorialId = currentParseUser.getUsername() + Constants.TutorialKeys.TEACHER_RESPONSE;
                SessionManager mgr = SessionManager.getInstance();
                if(mgr.getSignUpAccount() && !mgr.getTutorialState(tutorialId)) {//only if signup account
                    mgr.setTutorialState(tutorialId, true);
                    ShowcaseCreator.teacherHighlightResponseButtonsNew(getActivity(), holder.likes);
                }
                responseTutorialShown = true;
            }
        }


        @Override
        public int getItemCount() {

            if (groupDetails == null) {
                groupDetails = new ArrayList<ParseObject>();
            }

            if (groupDetails.size() == 0)
                outboxLayout.setVisibility(View.VISIBLE);
            else
                outboxLayout.setVisibility(View.GONE);

            return groupDetails.size();
        }
    }


       /* @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            switch (item.getItemId()) {

                //on refresh option selected from options menu
                case R.id.refresh:

                    if (Utility.isInternetExist()) {

                        if (outboxRefreshLayout != null) {
                            runSwipeRefreshLayout(outboxRefreshLayout, 10);
                        }
                        outboxRefreshLayout.setRefreshing(true);

                        //update outbox in background
                        refreshCountInBackground();
                        //stop refreshing in above method inside view.post

                    }
                    break;
                default:
                    break;
            }
            return super.onOptionsItemSelected(item);
        }*/


    //Refresh the layout. For e.g if outbox messages have changed
    public static void refreshSelf() {
        if (Application.applicationHandler != null && outboxRefreshLayout != null && outboxLayout != null) {
            Application.applicationHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (Config.SHOWLOG) Log.d("D_AFTER_OUTBOX_REFRESH", "Updating outbox messages");
                    if(outboxRefreshLayout == null || outboxLayout == null){
                        return;
                    }

                    outboxRefreshLayout.setRefreshing(false);

                    if (groupDetails == null || groupDetails.size() == 0) {
                        outboxLayout.setVisibility(View.VISIBLE);
                    } else {
                        outboxLayout.setVisibility(View.GONE);
                    }

                    if (myadapter != null) {
                        myadapter.notifyDataSetChanged();
                    }
                }
            });
        }
    }

    public static void refreshCountCore() {
        //set lastTimeOutboxSync
        Application.lastTimeOutboxSync = Calendar.getInstance().getTime();

        if(Config.SHOWLOG) Log.d("DEBUG_OUTBOX", "running fetchLikeConfusedCountOutbox and setting lastTimeOutboxSync");
        SyncMessageDetails.fetchLikeConfusedCountOutbox();
        //following is the onpostexecute thing
        refreshSelf();
    }

    //update like/confused/seen count for sent messages in a background thread
    public static void refreshCountInBackground() {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                refreshCountCore();
            }
        };

        Thread t = new Thread(r);
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }

    /*
    stop swipe refreshlayout
    */
    public static void runSwipeRefreshLayout(final SwipeRefreshLayout outboxRefreshLayout, final int seconds) {

        if (outboxRefreshLayout == null)
            return;

        outboxRefreshLayout.setRefreshing(true);

        //start handler for 10 secs.  <to stop refreshbar>
        final Handler h = new Handler() {
            @Override
            public void handleMessage(Message message) {

                outboxRefreshLayout.setRefreshing(false);
            }
        };
        h.sendMessageDelayed(new Message(), seconds * 1000);
    }

    public static void updateOutboxTotalMessages() {

        if(Config.SHOWLOG) Log.d("DEBUG_OUTBOX_UPDATE_TOTAL_COUNT", "updating total outbox count");

        //update totalOutboxMessages
        ParseUser user = ParseUser.getCurrentUser();

        if (user == null) {
            Utility.LogoutUtility.logout();
            return;
        }

        ParseQuery<ParseObject> query = ParseQuery.getQuery(Constants.SENT_MESSAGES_TABLE);
        query.fromLocalDatastore();
        query.whereEqualTo("userId", user.getUsername());
        try {
            totalOutboxMessages = query.count();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        if(Config.SHOWLOG) Log.d("DEBUG_OUTBOX_UPDATE_TOTAL_COUNT", "count is " + totalOutboxMessages);
    }


    class GetLocalOutboxMsgInBackground extends AsyncTask<Void, Void, Void> {
        List<ParseObject> tempMsgs;

        @Override
        protected Void doInBackground(Void... params) {
            //retrieving lcoally stored outbox messges
            tempMsgs = Queries.getLocalOutbox();

            updateOutboxTotalMessages();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (tempMsgs == null) {
                tempMsgs = new ArrayList<ParseObject>();
            }

            groupDetails = tempMsgs;

            if (myadapter != null) {
                myadapter.notifyDataSetChanged();
            }

            if (outboxLayout != null && emptyBackground != null && loadingBar != null) {
                if (groupDetails.size() == 0){
                    outboxLayout.setVisibility(View.VISIBLE);
                    emptyBackground.setVisibility(View.VISIBLE);
                    loadingBar.setVisibility(View.GONE);
                }
                else {
                    outboxLayout.setVisibility(View.GONE);
                    emptyBackground.setVisibility(View.GONE);
                    loadingBar.setVisibility(View.VISIBLE);
                }
            }
            super.onPostExecute(aVoid);

            if (action != null && id != null) {
                //handle the notification in asynctask
                NotificationHandler notificationHandler = new NotificationHandler();
                notificationHandler.execute();
            }
        }
    }

    //called when all local messages shown
    class GetExtraLocalOutboxMsgs extends AsyncTask<Void, Void, Void>
    {
        int totalItemCount;
        List<ParseObject> extraMessages = null;

        GetExtraLocalOutboxMsgs(int totalItemCount){
            this.totalItemCount = totalItemCount;
        }

        @Override
        protected Void doInBackground(Void... params) {
            extraMessages = query.getExtraLocalOutbox(groupDetails);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            isGetExtraLocalOutboxMsgsRunning = false;

            if(extraMessages == null){
                totalOutboxMessages = totalItemCount;
            }

            if(extraMessages != null && groupDetails != null && myadapter != null){
                groupDetails.addAll(extraMessages);
                myadapter.notifyDataSetChanged();
            }
            super.onPostExecute(aVoid);
        }
    }

    class NotificationHandler extends AsyncTask<Void, Void, Void> {
        int msgIndex = -1;

        @Override
        protected Void doInBackground(Void... params) {
            if (groupDetails == null) return null;

            if (action != null && id != null &&
                    (action.equals(Constants.Actions.LIKE_ACTION) || action.equals(Constants.Actions.CONFUSE_ACTION))) {
                action = null; //action not used hereafter. Avoid duplicate asynctask invocations

                for (int i = 0; i < groupDetails.size(); i++) {
                    ParseObject msg = groupDetails.get(i);
                    if (msg.getString("objectId") != null && msg.getString("objectId").equals(id)) {
                        msgIndex = i;
                        break;
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (outboxListv == null || outboxListv.getAdapter() == null) return;
            if (msgIndex >= 0 && msgIndex < outboxListv.getAdapter().getItemCount()) {
                if(Config.SHOWLOG) Log.d("DEBUG_OUTBOX", "scrolling to position " + msgIndex);

                selectedMsgIndex = msgIndex;
                myadapter.notifyDataSetChanged();
                outboxListv.smoothScrollToPosition(msgIndex);
                action = null;
                id = null; //do not repeat
            }
        }
    }
}
