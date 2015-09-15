package trumplabs.schoolapp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.github.amlcurran.showcaseview.ShowcaseView;
import com.parse.GetDataCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import BackGroundProcesses.Inbox;
import BackGroundProcesses.Refresher;
import joinclasses.JoinClassDialog;
import library.UtilString;
import trumplab.textslate.R;
import tutorial.ShowcaseCreator;
import utility.Config;
import utility.ImageCache;
import utility.Queries;
import utility.SessionManager;
import utility.Utility;

/**
 * Class for Inbox's functions & activity
 */
public class Messages extends Fragment {
    private static Activity getactivity;

    public static List<ParseObject> msgs;
    protected LayoutInflater layoutinflater;
    RecyclerView listv;
    private LinearLayoutManager mLayoutManager;
    public static RecyclerView.Adapter myadapter;
    public static SwipeRefreshLayout mPullToRefreshLayout;
    private LinearLayout inemptylayout;
    private Queries query  ;
    String userId;
    static int totalInboxMessages; //total pinned messages in inbox
    LinearLayout mainLayout;
    boolean refreshServerMessage;
    private ImageView inbox_messages_bg;
    private ProgressBar inbox_messages_pb;

    public static boolean responseTutorialShown = false;

    public boolean oldInboxFetched = false;
    public boolean isGetMoreOldMessagesRunning = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        layoutinflater = inflater   ;
        setHasOptionsMenu(true);
        getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        return inflater.inflate(R.layout.msgcontainer, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getactivity = getActivity();

        mPullToRefreshLayout = (SwipeRefreshLayout) getActivity().findViewById(R.id.ptr_layout);
        mPullToRefreshLayout.setColorSchemeColors(Color.RED, Color.GREEN, Color.BLUE, Color.MAGENTA);

        listv = (RecyclerView) getActivity().findViewById(R.id.msg_list);
        inemptylayout = (LinearLayout) getActivity().findViewById(R.id.inemptymsg);
        mainLayout = (LinearLayout)  getActivity().findViewById(R.id.msgCntLayout);
        inbox_messages_bg= (ImageView) getActivity().findViewById(R.id.inbox_messages_bg);
        inbox_messages_pb = (ProgressBar) getActivity().findViewById(R.id.inbox_messages_pb);

      /*
      Check for push open
       */

        refreshServerMessage = false;
        if(getActivity().getIntent() != null) {
            if (getActivity().getIntent().getExtras() != null) {
                Intent intent = getActivity().getIntent();
                boolean pushOpen = getActivity().getIntent().getExtras().getBoolean("pushOpen", false);
                if (pushOpen) {
                    intent.putExtra("pushOpen", false);
                    getActivity().setIntent(intent);

                    if(Utility.isInternetExistWithoutPopup()) {
                        if(Config.SHOWLOG) Log.d("DEBUG_MESSAGES", "calling Inbox async task because of pushOpen flag");
                        refreshServerMessage = true;
                        /*
                        flag set to fetch messages from server.
                        In case of teacher, fetching locally and from server both are async task, which causing problem(index outofbound exception)
                         so need to call them serially.
                         Hence first call local async task then server async task.
                         */
                    }
                }
                else{
                    if(Config.SHOWLOG) Log.d("DEBUG_MESSAGES", "pushOpen flag false");
                }
            }
        }


        if(!refreshServerMessage) {//if pushOpen flag not set in intent
            if (Refresher.isSufficientGapInbox() && Utility.isInternetExistWithoutPopup()) {
                if(Config.SHOWLOG) Log.d("DEBUG_MESSAGES", "calling Inbox async task since sufficient gap");
                refreshServerMessage = true; //we are calling inbox if gap is sufficient
            } else {
                if(Config.SHOWLOG) Log.d("DEBUG_MESSAGES", "skipping Inbox async : gap " + Refresher.isSufficientGapInbox());
            }
        }


        getActivity().findViewById(R.id.inbox_messages_bg).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fm = getActivity().getSupportFragmentManager();
                JoinClassDialog joinClassDialog = new JoinClassDialog();
                joinClassDialog.show(fm, "Join Class");
            }
        });

        /*
        Recycler view handling
         */
        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(getActivity());
        listv.setLayoutManager(mLayoutManager);


        ParseUser currentParseUser = ParseUser.getCurrentUser();

        if (currentParseUser == null) {
            Utility.LogoutUtility.logout();
            return;
        }

        userId = currentParseUser.getUsername();


        //fetch local messages in background
        query = new Queries();

        //initialize msgs as it is static and can be persistent from previous user who logged out
        msgs = new ArrayList<>();

        /*
        If user is a teacher then load data in background (since there are 3 tabs to load) else load directly
         */
        String role = currentParseUser.getString(Constants.ROLE);
        if(role.equals(Constants.TEACHER))
        {
            GetLocalInboxMsgInBackground  getLocalInboxMsg = new GetLocalInboxMsgInBackground();
            getLocalInboxMsg.execute();
        }
        else
        {
            inbox_messages_pb.setVisibility(View.GONE);
            inbox_messages_bg.setVisibility(View.VISIBLE);

            try {
                msgs = query.getLocalInboxMsgs();
                updateInboxTotalCount(); //update total inbox count required to manage how/when scrolling loads more messages
            } catch (ParseException e) {
            }

            if (msgs == null)
                msgs = new ArrayList<>();

            if (msgs.size() == 0)
                inemptylayout.setVisibility(View.VISIBLE);
            else
                inemptylayout.setVisibility(View.GONE);


            if(refreshServerMessage) {
                if(Config.SHOWLOG) Log.d("DEBUG_MESSAGES", "showing mPullToRefreshLayout");
                if (mPullToRefreshLayout != null) {
                    runSwipeRefreshLayout(mPullToRefreshLayout, 10);
                }

                if(!Inbox.isQueued){
                    Inbox newInboxMsg = new Inbox();
                    newInboxMsg.execute();
                }

                refreshServerMessage = false;
            }
        }


        // Collections.reverse(msgs);
        myadapter = new RecycleAdapter();
        listv.setAdapter(myadapter);

    /*
     * On scrolling down the list view display extra messages.
     */
        listv.setOnScrollListener(new RecyclerView.OnScrollListener() {
            int lastCount = 0;

            @Override
            public void onScrolled(RecyclerView view, int dx, int dy) {

                int visibleItemCount = mLayoutManager.getChildCount();
                int totalItemCount = mLayoutManager.getItemCount();
                int pastVisibleItems = mLayoutManager.findFirstVisibleItemPosition();

                //if(Config.SHOWLOG) Log.d("DEBUG_MESSAGES", "onScrolled : v=" + visibleItemCount + ", p=" + pastVisibleItems + ", t=" + totalItemCount + ", i=" + totalInboxMessages);
                if (visibleItemCount + pastVisibleItems >= totalItemCount - 2) {
                    if (totalItemCount >= totalInboxMessages) {
                        //Now no more available locally, now if not all inbox messages have been fetched, fetch more using showOldMessages cloud function and update adapter and totalInboxMessages
                        checkAndFetchOldMessages();
                        //if(Config.SHOWLOG) Log.d("DEBUG_MESSAGES", "[" + (visibleItemCount + pastVisibleItems) + " out of" + totalInboxMessages + "]all messages loaded. Saving unnecessary query");
                        return; //nothing to do as all messages have been loaded
                    }
                    if (Config.SHOWLOG) Log.d("DEBUG_MESSAGES", "Loading more local messages");

                    try {
                        boolean gotExtraMsgs = query.getExtraLocalInboxMsgs(msgs);
                        //if no extra msgs added, then (just for safety), set totalInboxMessages to totalItemCount
                        //otherwise repeatedly getExtraLocalInboxMsgs will be called unnecessarily
                        if (!gotExtraMsgs) {
                            if (Config.SHOWLOG) Log.d("DEBUG_MESSAGES", "gotExtraMsgs=false, setting totalInboxMessges");
                            totalInboxMessages = totalItemCount;
                        }
                        myadapter.notifyDataSetChanged();
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onScrollStateChanged(RecyclerView view, int newState) {
            }
        });


        mPullToRefreshLayout.setColorSchemeColors(Color.RED, Color.GREEN, Color.BLUE, Color.MAGENTA);
        mPullToRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (Config.SHOWLOG) Log.d("DEBUG_MESSAGES_REFRESH", "On refresh called through pull down listener");

                Utility.ls(" pull to refresh starting ... ");
                // mHeaderProgressBar.setVisibility(View.GONE);


                if (Utility.isInternetExist()) {
                    Utility.ls(" inbox has to sstart ... ");
                    if (Config.SHOWLOG) Log.d("DEBUG_MESSAGES", "calling Inbox execute() pull to refresh");

                    if (!Inbox.isQueued) {
                        Inbox newInboxMsg = new Inbox();
                        newInboxMsg.execute();
                    }

                    //start handler for 10 secs.  <to stop refreshbar>
                    final Handler h = new Handler() {
                        @Override
                        public void handleMessage(Message message) {

                            mPullToRefreshLayout.setRefreshing(false);
                        }
                    };
                    h.sendMessageDelayed(new Message(), 10000);
                } else {

                    //start handler for 2 secs.  <to stop refreshbar>
                    final Handler h = new Handler() {
                        @Override
                        public void handleMessage(Message message) {

                            mPullToRefreshLayout.setRefreshing(false);
                        }
                    };
                    h.sendMessageDelayed(new Message(), 2000);
                }
            }
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }


    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView groupName;
        TextView startTime;
        TextView msgslist;
        FrameLayout imgframelayout;
        ImageView imgmsgview;
        TextView attachmentNameTV;

        TextView faildownload;
        ProgressBar uploadprogressbar;
        LinearLayout likeButton;
        LinearLayout confuseButton;
        TextView likes;
        TextView confused;
        ImageView likeIcon;
        ImageView confusingIcon;
        LinearLayout copyLayout;
        LinearLayout root;
        LinearLayout head;

        public ViewHolder(View row) {
            super(row);

            groupName = (TextView) row.findViewById(R.id.groupName);
            startTime = (TextView) row.findViewById(R.id.startTime);
            msgslist = (TextView) row.findViewById(R.id.msgs);
            imgframelayout = (FrameLayout) row.findViewById(R.id.imagefrmlayout);
            imgmsgview = (ImageView) row.findViewById(R.id.imgmsgcontent);
            attachmentNameTV = (TextView) row.findViewById(R.id.attachment_name);

            faildownload = (TextView) row.findViewById(R.id.faildownload);
            uploadprogressbar = (ProgressBar) row.findViewById(R.id.msgprogressbar);
            likeButton = (LinearLayout) row.findViewById(R.id.likeButton);
            confuseButton = (LinearLayout) row.findViewById(R.id.confuseButton);
            likes = (TextView) row.findViewById(R.id.like);
            confused = (TextView) row.findViewById(R.id.confusion);
            likeIcon = (ImageView) row.findViewById(R.id.likeIcon);
            confusingIcon = (ImageView) row.findViewById(R.id.confusionIcon);
            copyLayout = (LinearLayout) row.findViewById(R.id.copyMessage);
            root = (LinearLayout) row.findViewById(R.id.rootLayout);
            head = (LinearLayout) row.findViewById(R.id.headLayout);
        }
    }

    public class RecycleAdapter extends RecyclerView.Adapter<ViewHolder> {
        public class MessageStatePair{
            public int likeStatus;
            public int confusedStatus;
            public MessageStatePair(int like, int confused){
                likeStatus = like;
                confusedStatus = confused;
            }
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int position) {

            View row = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.messages_likes, viewGroup, false);
            ViewHolder holder = new ViewHolder(row);
            return holder;
        }


        @Override
        public int getItemCount() {

            if(msgs == null){
                msgs = new ArrayList<ParseObject>();
            }

            if (msgs.size() == 0)
                inemptylayout.setVisibility(View.VISIBLE);
            else
                inemptylayout.setVisibility(View.GONE);

            return msgs.size();
        }

        protected void handleStateChange(final ViewHolder holder, MessageStatePair currentState, MessageStatePair newState, ParseObject msgObject){
            //update button displays if any change
            if(newState.likeStatus - currentState.likeStatus !=0){
                //means like status has changed
                if(newState.likeStatus == 1) {
                    liked(holder.likeIcon, holder.likes, holder.likeButton);
                    msgObject.put(Constants.GroupDetails.LIKE, true);
                }
                else {
                    unLiked(holder.likeIcon, holder.likes, holder.likeButton);
                    msgObject.put(Constants.GroupDetails.LIKE, false);
                }

                int likeCount = msgObject.getInt(Constants.GroupDetails.LIKE_COUNT);
                int diff = newState.likeStatus - currentState.likeStatus;
                //if(Config.SHOWLOG) Log.d("DEBUG_MESSAGES", msgObject.getObjectId() + " newLS" + newState.likeStatus + " curLS" + currentState.likeStatus + " likeCount"  + likeCount + " diff" + diff);
                int newLikeCount = likeCount + diff;
                if(newLikeCount < 0 ) newLikeCount = 0;

                holder.likes.setText(newLikeCount + "");

                msgObject.put(Constants.GroupDetails.LIKE_COUNT, newLikeCount);
            }

            if(newState.confusedStatus - currentState.confusedStatus !=0){
                if (newState.confusedStatus == 1) {
                    confused(holder.confusingIcon, holder.confused, holder.confuseButton);
                    msgObject.put(Constants.GroupDetails.CONFUSING, true);
                }
                else {
                    unConfused(holder.confusingIcon, holder.confused, holder.confuseButton);
                    msgObject.put(Constants.GroupDetails.CONFUSING, false);
                }

                int confusedCount = msgObject.getInt(Constants.GroupDetails.CONFUSED_COUNT);
                int diff = newState.confusedStatus - currentState.confusedStatus;
                //if(Config.SHOWLOG) Log.d("DEBUG_MESSAGES", msgObject.getObjectId() + " newCS" + newState.confusedStatus + " curCS" + currentState.confusedStatus + " conCount"  + confusedCount + " diff" + diff);

                int newConfusedCount = confusedCount + diff;
                if(newConfusedCount < 0 ) newConfusedCount = 0;

                holder.confused.setText(newConfusedCount + "");

                msgObject.put(Constants.GroupDetails.CONFUSED_COUNT, newConfusedCount);
            }

            if(Config.SHOWLOG) Log.d("DEBUG_MESSAGES", "new status L/C = " + newState.likeStatus + "/" + newState.confusedStatus +
                    "|| old status L/C = " + currentState.likeStatus + "/" + currentState.confusedStatus +
                    "|| new count L/C = " + msgObject.getInt(Constants.GroupDetails.CONFUSED_COUNT) + "/" + msgObject.getInt(Constants.GroupDetails.CONFUSED_COUNT) +
                    "|| synced status L/C = " + msgObject.getBoolean(Constants.GroupDetails.SYNCED_LIKE) + "/" + msgObject.getBoolean(Constants.GroupDetails.SYNCED_CONFUSING));

            //Set the dirty bit in message according to current and synced states
            boolean like = msgObject.getBoolean(Constants.GroupDetails.LIKE);
            boolean confusing = msgObject.getBoolean(Constants.GroupDetails.CONFUSING);
            boolean synced_like = msgObject.getBoolean(Constants.GroupDetails.SYNCED_LIKE);
            boolean synced_confusing = msgObject.getBoolean(Constants.GroupDetails.SYNCED_CONFUSING);

            if(like == synced_like && confusing == synced_confusing){ //no changes at all.
                msgObject.put(Constants.GroupDetails.DIRTY_BIT, false);
            }
            else {
                msgObject.put(Constants.GroupDetails.DIRTY_BIT, true);
            }
            //updating msgObject locally
            msgObject.pinInBackground();
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, final int position) {

            final ParseObject msgObject = msgs.get(position);

      /*
       * Set likes and confused count
       */
            int likeCount = msgObject.getInt(Constants.GroupDetails.LIKE_COUNT);
            if(likeCount < 0) likeCount = 0;
            int confusedCount = msgObject.getInt(Constants.GroupDetails.CONFUSED_COUNT);
            if(confusedCount < 0) confusedCount = 0;

            holder.likes.setText(likeCount + "");
            holder.confused.setText(confusedCount + "");


            if (msgObject.getBoolean(Constants.GroupDetails.LIKE))
                liked(holder.likeIcon, holder.likes, holder.likeButton);
            else
                unLiked(holder.likeIcon, holder.likes, holder.likeButton);

            if (msgObject.getBoolean(Constants.GroupDetails.CONFUSING))
                confused(holder.confusingIcon, holder.confused, holder.confuseButton);
            else
                unConfused(holder.confusingIcon, holder.confused, holder.confuseButton);

      /*
       * Setting likes and confused button functionality
       */

            holder.likeButton.setOnClickListener(new OnClickListener() {

                                                     @Override
                                                     public void onClick(View v) {
                                                         final MessageStatePair currentState = new MessageStatePair(0, 0);

                                                         if(msgObject.getBoolean(Constants.GroupDetails.LIKE)){
                                                             currentState.likeStatus = 1;
                                                         }
                                                         if(msgObject.getBoolean(Constants.GroupDetails.CONFUSING)){
                                                             currentState.confusedStatus = 1;
                                                         }

                                                         MessageStatePair newState = null;
                                                         if(currentState.likeStatus == 1){ //old liked(10) => new 00
                                                             newState = new MessageStatePair(0, 0);
                                                         }
                                                         else{ //new 10 (liked)
                                                             newState = new MessageStatePair(1, 0);
                                                         }

                                                         handleStateChange(holder, currentState, newState, msgObject);
                                                     }
                                                 }
            );


            // Setting like and confusing button functionality


            holder.confuseButton.setOnClickListener(new OnClickListener() {

                                                        @Override
                                                        public void onClick(View v) {
                                                            final MessageStatePair currentState = new MessageStatePair(0, 0);

                                                            if(msgObject.getBoolean(Constants.GroupDetails.LIKE)){
                                                                currentState.likeStatus = 1;
                                                            }
                                                            if(msgObject.getBoolean(Constants.GroupDetails.CONFUSING)){
                                                                currentState.confusedStatus = 1;
                                                            }

                                                            MessageStatePair newState = null;
                                                            if(currentState.confusedStatus == 1){ //old confused(10) => new 00
                                                                newState = new MessageStatePair(0, 0);
                                                            }
                                                            else{ //new 01 (confused)
                                                                newState = new MessageStatePair(0, 1);
                                                            }

                                                            handleStateChange(holder, currentState, newState, msgObject);
                                                        }
                                                    }

            );


            holder.uploadprogressbar.setVisibility(View.GONE);

            //setting cardview for higher api using elevation

            int currentapiVersion = android.os.Build.VERSION.SDK_INT;
            if (currentapiVersion >= Build.VERSION_CODES.LOLLIPOP){
                holder.head.setBackground(getResources().getDrawable(R.drawable.greyoutline));
            }

            String senderId = msgObject.getString("senderId");

            // senderId = senderId.replaceAll(".", "");
            senderId = senderId.replaceAll("@", "");
            String filePath = Utility.getWorkingAppDir() + "/thumbnail/" + senderId + "_PC.jpg";

            //if(Config.SHOWLOG) Log.d("DEBUG_MESSAGES_DISPLAYING", "profile pic " + filePath);
            File senderThumbnailFile = new File(filePath);

            // ///////////////////////////////////////////////////////////
            final String imageName;
            if (msgObject.containsKey("attachment_name"))
                imageName = msgObject.getString("attachment_name");
            else
                imageName = "";

      /*
       * Setting group name and sender name
       */

            String Str = null;

            if(!UtilString.isBlank(msgObject.getString("name")))
                 Str = msgObject.getString("name").toUpperCase();
            holder.groupName.setText(Str);


            if (msgObject.getCreatedAt() != null) {
                holder.startTime.setText(Utility.convertTimeStamp(msgObject.getCreatedAt()));
            }
            else if (msgObject.get("creationTime") != null)
                holder.startTime.setText(Utility.convertTimeStamp((Date) msgObject.get("creationTime")));

            final String message = msgObject.getString("title");
            if (UtilString.isBlank(message)) {
                holder.msgslist.setVisibility(View.GONE);
                holder.copyLayout.setVisibility(View.GONE);
            }
            else
            {
                holder.msgslist.setVisibility(View.VISIBLE);
                holder.msgslist.setText(message);
                holder.copyLayout.setVisibility(View.VISIBLE);

                holder.copyLayout.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        /*
                         * Creating popmenu for copying text
                         */
                        //Defualt copy text menu
                        PopupMenu menu = new PopupMenu(getActivity(), v);

                        menu.getMenuInflater().inflate(R.menu.copy_text, menu.getMenu());
                        menu.show();

                        // setting menu click functionality
                        menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {

                            @Override
                            public boolean onMenuItemClick(MenuItem item) {

                                switch (item.getItemId()) {
                                    case R.id.copy:

                                        Utility.copyToClipBoard(getActivity(), "Message", message);
                                       break;

                                    default:
                                        break;
                                }
                                return true;
                            }
                        });

                    }
                });
            }

            if (!UtilString.isBlank(imageName))
            {
                final boolean isFileAnImage = Utility.isFileImageType(imageName);
                final String imageFilePath = Utility.getFileLocationInAppFolder(imageName);

                holder.imgframelayout.setVisibility(View.VISIBLE);
                holder.imgframelayout.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        if(isFileAnImage){
                            Intent imgintent = new Intent();
                            imgintent.setAction(Intent.ACTION_VIEW);
                            imgintent.setDataAndType(Uri.parse("file://" + imageFilePath), "image/*");
                            startActivity(imgintent);
                        }
                        else {
                            //assume any kind of file. only teacher has restriction on what kind of file he can send(currently pdf)
                            //while opening, assume any type of file
                            String mimeType = Utility.getMimeType(imageName); //non null return value
                            Utility.toast(imageName + " with mime=" + mimeType, false, 15);
                            File file = new File(imageFilePath);
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setDataAndType(Uri.fromFile(file), mimeType);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                            startActivity(intent);
                        }
                    }
                });

                final Runnable onImageSuccessRunnable = new Runnable() {
                    @Override
                    public void run() {
                        holder.uploadprogressbar.setVisibility(View.GONE);
                        holder.attachmentNameTV.setVisibility(View.GONE);
                        holder.faildownload.setVisibility(View.GONE);
                    }
                };

                final Runnable onFileSuccessRunnable = new Runnable() {
                    @Override
                    public void run() {
                        holder.uploadprogressbar.setVisibility(View.GONE);
                        holder.attachmentNameTV.setText(imageName);
                        holder.attachmentNameTV.setVisibility(View.VISIBLE);
                        holder.faildownload.setVisibility(View.GONE);
                    }
                };

                final Runnable onFailRunnable = new Runnable() {
                    @Override
                    public void run() {
                        holder.uploadprogressbar.setVisibility(View.GONE);
                        holder.attachmentNameTV.setVisibility(View.GONE);
                        holder.faildownload.setVisibility(View.VISIBLE);
                    }
                };

                //show progress bar
                holder.uploadprogressbar.setVisibility(View.VISIBLE);
                holder.faildownload.setVisibility(View.GONE);

                File imgFile = new File(imageFilePath);

                holder.imgmsgview.setImageBitmap(null);//because in recycleview is reused, hence need to initialize properly
                holder.imgframelayout.setTag(imgFile.getAbsolutePath());
                holder.imgmsgview.setTag(imgFile.getAbsolutePath());

                if(ImageCache.showIfInCache(imageName, holder.imgmsgview)){
                    if(Config.SHOWLOG) Log.d(ImageCache.LOGTAG, "(m) already cached : " + imageName);
                    onImageSuccessRunnable.run();
                }
                else if (imgFile.exists()) {
                    // image file present locally
                    if(isFileAnImage) {
                        ImageCache.WriteLoadAndShowTask writeLoadAndShowTask = new ImageCache.WriteLoadAndShowTask(null, imageName, holder.imgmsgview, getActivity(), onImageSuccessRunnable);
                        writeLoadAndShowTask.execute();
                    }
                    else{
                        Log.d("__file_picker", "m) exists " + imageName);
                        //set file icon and run onSuccessRunnable
                        holder.imgmsgview.setImageResource(R.drawable.general_file_icon);
                        onFileSuccessRunnable.run();
                    }
                } else if(Utility.isInternetExistWithoutPopup()) {
                    Log.d("__file_picker", "m) downloading " + imageName);
                    if(Config.SHOWLOG) Log.d(ImageCache.LOGTAG, "(m) downloading data : " + imageName);

                    // Have to download image from server
                    ParseFile imagefile = msgObject.getParseFile("attachment");
                    if(imagefile != null) {
                        imagefile.getDataInBackground(new GetDataCallback() {
                            public void done(byte[] data, ParseException e) {
                                if (e == null) {
                                    if(isFileAnImage) {
                                        ImageCache.WriteLoadAndShowTask writeLoadAndShowTask = new ImageCache.WriteLoadAndShowTask(data, imageName, holder.imgmsgview, getActivity(), onImageSuccessRunnable);
                                        writeLoadAndShowTask.execute();
                                    }
                                    else{
                                        ImageCache.WriteDocTask writeDocTask = new ImageCache.WriteDocTask(data, imageName, holder.imgmsgview, getActivity(), onFileSuccessRunnable);
                                        writeDocTask.execute();
                                    }

                                } else {
                                    //ParseException check for invalid session
                                    Utility.LogoutUtility.checkAndHandleInvalidSession(e);
                                    onFailRunnable.run();
                                }
                            }
                        });
                    }
                    else{
                        onFailRunnable.run();
                    }
                }
                else {
                    onFailRunnable.run();
                }
            } else
            {
                holder.imgframelayout.setVisibility(View.GONE);
                holder.attachmentNameTV.setVisibility(View.GONE);
                holder.imgmsgview.setTag(""); //reset to empty
            }

            //if a) first msg, b) already not shown, c) either non-teacher or fragment # visible is 2 (ie. Messages tab)

            ParseUser currentParseUser = ParseUser.getCurrentUser();
            if(currentParseUser == null){
                return;
            }

            String role = currentParseUser.getString(Constants.ROLE);

            //if(Config.SHOWLOG) Log.d(ShowcaseCreator.LOGTAG, "(parent)checking response tutorial, location=" + position + ", flag=" + responseTutorialShown
            //        + ", role=" + role + ", fragVisible=" + MainActivity.fragmentVisible);

            if(Application.mainActivityVisible && position == 0 && !responseTutorialShown && (!role.equals(Constants.TEACHER) || MainActivity.fragmentVisible == 2) && !ShowcaseView.isVisible){
                String tutorialId = currentParseUser.getUsername() + Constants.TutorialKeys.PARENT_RESPONSE;
                SessionManager mgr = SessionManager.getInstance();
                if(Config.SHOWLOG) Log.d(ShowcaseCreator.LOGTAG, "(parent)tutorialId=" + tutorialId + " isSignUpAccount=" + mgr.getSignUpAccount() + " tutState=" + mgr.getTutorialState(tutorialId));
                if(mgr.getSignUpAccount() && !mgr.getTutorialState(tutorialId)) { //only if signup account
                    mgr.setTutorialState(tutorialId, true);
                    if(Config.SHOWLOG) Log.d(ShowcaseCreator.LOGTAG, "(parent) creating response tutorial");

                    ShowcaseCreator.parentHighlightResponseButtonsNew(getactivity, holder.likeButton);
                }
                responseTutorialShown = true;
            }
        }
    }

   /* @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.refresh:

                if(Utility.isInternetExist()) {
                    Utility.ls(" option selected  ... ");

                    if (mPullToRefreshLayout != null) {
                        Utility.ls(" option selected pull to refresh starting ... ");
                        if(mPullToRefreshLayout.isRefreshing())
                            mPullToRefreshLayout.setRefreshing(false);
                        runSwipeRefreshLayout(mPullToRefreshLayout, 10);
                    }
                    else
                        Utility.ls(" option selected  ...null ");

                    if(Config.SHOWLOG) Log.d("DEBUG_MESSAGES", "calling Inbox execute() on refresh option click");

                    Inbox newInboxMsg = new Inbox(msgs);
                    newInboxMsg.execute();
                }

                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }*/

    /********** showcase ***********/


    void liked(ImageView likeIcon, TextView likes, LinearLayout likeButton) {
        likeIcon.setImageResource(R.drawable.ic_action_like1);
        likes.setTextColor(getResources().getColor(R.color.white));

        int sdk = android.os.Build.VERSION.SDK_INT;
        if(sdk < android.os.Build.VERSION_CODES.JELLY_BEAN) {
            likeButton.setBackgroundDrawable( getResources().getDrawable(R.drawable.inbox_button_blue_color) );
        } else {
            likeButton.setBackground( getResources().getDrawable(R.drawable.inbox_button_blue_color));
        }

    }

    void unLiked(ImageView likeIcon, TextView likes, LinearLayout likeButton) {
        likeIcon.setImageResource(R.drawable.ic_action_like);
        likes.setTextColor(getResources().getColor(R.color.grey));
        likeButton.setBackgroundColor(getResources().getColor(R.color.white));

        int sdk = android.os.Build.VERSION.SDK_INT;
        if(sdk < android.os.Build.VERSION_CODES.JELLY_BEAN) {
            likeButton.setBackgroundDrawable( getResources().getDrawable(R.drawable.round_corner_grey_color) );
        } else {
            likeButton.setBackground( getResources().getDrawable(R.drawable.round_corner_grey_color));
        }
    }

    void confused(ImageView confusingIcon, TextView confused, LinearLayout confuseButton) {
        confusingIcon.setImageResource(R.drawable.ic_action_help1);
        confused.setTextColor(getResources().getColor(R.color.white));

        int sdk = android.os.Build.VERSION.SDK_INT;
        if(sdk < android.os.Build.VERSION_CODES.JELLY_BEAN) {
            confuseButton.setBackgroundDrawable( getResources().getDrawable(R.drawable.inbox_button_blue_color) );
        } else {
            confuseButton.setBackground( getResources().getDrawable(R.drawable.inbox_button_blue_color));
        }
    }

    void unConfused(ImageView confusingIcon, TextView confused, LinearLayout confuseButton) {
        confusingIcon.setImageResource(R.drawable.ic_action_help);
        confused.setTextColor(getResources().getColor(R.color.grey));

        int sdk = android.os.Build.VERSION.SDK_INT;
        if(sdk < android.os.Build.VERSION_CODES.JELLY_BEAN) {
            confuseButton.setBackgroundDrawable( getResources().getDrawable(R.drawable.round_corner_grey_color) );
        } else {
            confuseButton.setBackground( getResources().getDrawable(R.drawable.round_corner_grey_color));
        }
    }


    /*
   stop swipe refreshlayout
    */
    private void runSwipeRefreshLayout(final SwipeRefreshLayout mPullToRefreshLayout, final int seconds) {

        if (mPullToRefreshLayout == null)
            return;

        if(mPullToRefreshLayout!=null){
            mPullToRefreshLayout.postDelayed(new Runnable() {

                @Override
                public void run() {
                    mPullToRefreshLayout.setRefreshing(true);
                }
            }, 1000);
        }

        //start handler for 10 secs.  <to stop refreshbar>
        final Handler h = new Handler() {
            @Override
            public void handleMessage(Message message) {

                mPullToRefreshLayout.setRefreshing(false);
            }
        };
        h.sendMessageDelayed(new Message(), seconds * 1000);
    }

    //update total number of messages in inbox (normal + locally generated)
    //update Messages.totalInboxMessages
    public static void updateInboxTotalCount(){
        ParseUser user = ParseUser.getCurrentUser();

        if (user == null) {
            Utility.LogoutUtility.logout();
            return;
        }

        int totalMessages = 0;
        ParseQuery<ParseObject> query = ParseQuery.getQuery(Constants.GroupDetails.TABLE);
        query.fromLocalDatastore();
        query.whereEqualTo("userId", user.getUsername());
        try {
            totalMessages += query.count();
        } catch (ParseException e) {
            e.printStackTrace();
            return;
        }

        ParseQuery<ParseObject> queryLocal = ParseQuery.getQuery("LocalMessages");
        queryLocal.fromLocalDatastore();
        queryLocal.whereEqualTo("userId", user.getUsername());
        try {
            totalMessages += queryLocal.count();
        } catch (ParseException e) {
            e.printStackTrace();
            return;
        }

        Messages.totalInboxMessages = totalMessages;
    }

    class GetLocalInboxMsgInBackground extends AsyncTask<Void, Void, Void>
    {
        List<ParseObject> tempMsgs;
        @Override
        protected Void doInBackground(Void... params) {
            try {
                tempMsgs = query.getLocalInboxMsgs();
                updateInboxTotalCount(); //update total inbox count required to manage how/when scrolling loads more messages
            } catch (ParseException e) {
            }

            if (tempMsgs == null)
                tempMsgs = new ArrayList<>();


            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            msgs = tempMsgs;
            Messages.myadapter.notifyDataSetChanged();

            if (msgs.size() == 0) {
                inemptylayout.setVisibility(View.VISIBLE);
                inbox_messages_pb.setVisibility(View.GONE);
                inbox_messages_bg.setVisibility(View.VISIBLE);
            }
            else {
                inemptylayout.setVisibility(View.GONE);
                inbox_messages_pb.setVisibility(View.VISIBLE);
                inbox_messages_bg.setVisibility(View.GONE);
            }


            if(refreshServerMessage)
            {
                refreshServerMessage = false;

                if (mPullToRefreshLayout != null) {
                    runSwipeRefreshLayout(mPullToRefreshLayout, 10);
                }

                if(!Inbox.isQueued) {
                    Inbox newInboxMsg = new Inbox();
                    newInboxMsg.execute();
                }
            }

            super.onPostExecute(aVoid);
        }
    }

    void checkAndFetchOldMessages(){
        //check if flag set
        //if not - run GetMoreOldMessages task
        //set flag if #msgs returned non null and less than 20
        if(!oldInboxFetched){
            ParseUser currentParseUser = ParseUser.getCurrentUser();
            if(currentParseUser == null){
                return;
            }

            String username = currentParseUser.getUsername();
            String key = username + Constants.SharedPrefsKeys.SERVER_INBOX_FETCHED;
            if(SessionManager.getInstance().getBooleanValue(key)){//if true set
                if(Config.SHOWLOG) Log.d("_FETCH_OLD", "already set in shared prefs");
                oldInboxFetched = true;
                return;
            }

            if(!isGetMoreOldMessagesRunning){
                if(Config.SHOWLOG) Log.d("_FETCH_OLD", "spawning GetMoreOldMessages");
                GetMoreOldMessages getMoreOldMessages = new GetMoreOldMessages();
                getMoreOldMessages.execute();
                isGetMoreOldMessagesRunning = true;
            }
            else{
                //if(Config.SHOWLOG) Log.d("_FETCH_OLD", "already running GetMoreOldMessages");
            }
        }
    }

    //called when all local messages shown
    class GetMoreOldMessages extends AsyncTask<Void, Void, Void>
    {
        List<ParseObject> extraMessages = null;
        @Override
        protected Void doInBackground(Void... params) {
            extraMessages = query.getOldServerInboxMsgs();
            if(extraMessages != null){
                if(extraMessages.size() < Config.oldMessagesPagingSize){
                    if(Config.SHOWLOG) Log.d("_FETCH_OLD", "oldInboxFetched set to true - we're done");
                    oldInboxFetched = true;
                }
            }
            else{
                if(Config.SHOWLOG) Log.d("_FETCH_OLD", "extraMessages null - connection failure or other error");
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            isGetMoreOldMessagesRunning = false;

            if(extraMessages != null && Messages.msgs != null && Messages.myadapter != null){
                Messages.msgs.addAll(extraMessages);
                Messages.myadapter.notifyDataSetChanged();
                totalInboxMessages += extraMessages.size();
            }

            super.onPostExecute(aVoid);
        }
    }
}
