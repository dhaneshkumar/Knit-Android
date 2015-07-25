package trumplabs.schoolapp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.util.LruCache;
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
    private static RecyclerView listv;
    private LinearLayoutManager mLayoutManager;
    public static RecyclerView.Adapter myadapter;
    public static SwipeRefreshLayout mPullToRefreshLayout;
    private LinearLayout inemptylayout;
    private Queries query  ;
    private LruCache<String, Bitmap> mMemoryCache;
    String userId;
    public static int totalInboxMessages; //total pinned messages in inbox
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
                        Log.d("DEBUG_MESSAGES", "calling Inbox async task because of pushOpen flag");
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
                    Log.d("DEBUG_MESSAGES", "pushOpen flag false");
                }
            }
        }


        if(!refreshServerMessage) {//if pushOpen flag not set in intent
            if (Refresher.isSufficientGapInbox() && Utility.isInternetExistWithoutPopup()) {
                Log.d("DEBUG_MESSAGES", "calling Inbox async task since sufficient gap");
                refreshServerMessage = true; //we are calling inbox if gap is sufficient
            } else {
                Log.d("DEBUG_MESSAGES", "skipping Inbox async : gap " + Refresher.isSufficientGapInbox());
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
     * Setting up LRU Cache for images
     */
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

        // Use 1/8th of the available memory for this memory cache.
        final int cacheSize = maxMemory / 8;


        //Utility.toast("RAM: "+ maxMemory );

        mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
        /*
         * The cache size will be measured in kilobytes rather than number of items.
         */

                return bitmap.getRowBytes() * bitmap.getHeight() / 1024;
            }
        };


        /*
        Recycler view handling
         */
        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(getActivity());
        listv.setLayoutManager(mLayoutManager);


        ParseUser currentParseUser = ParseUser.getCurrentUser();

        if (currentParseUser == null)
            {
                Utility.LogoutUtility.logout(); return;}

        userId = currentParseUser.getUsername();


        //fetch local messages in background
        query = new Queries();

        //initialize msgs as it is static and can be persistent from previous user who logged out
        msgs = new ArrayList<>();

        /*
        If user is a teacher then load data in background (since there are 3 tabs to load) else load directly
         */
        String role = currentParseUser.getString("role");
        if(role.equals("teacher"))
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
                Log.d("DEBUG_MESSAGES", "showing mPullToRefreshLayout");
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

        super.onActivityCreated(savedInstanceState);

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

                if (visibleItemCount + pastVisibleItems >= totalItemCount - 2) {
                    if (totalItemCount >= totalInboxMessages) {
                        //Now no more available locally, now if not all inbox messages have been fetched, fetch more using showOldMessages cloud function and update adapter and totalInboxMessages
                        checkAndFetchOldMessages();
                        Log.d("DEBUG_MESSAGES", "[" + (visibleItemCount + pastVisibleItems) + " out of" + totalInboxMessages + "]all messages loaded. Saving unnecessary query");
                        return; //nothing to do as all messages have been loaded
                    }
                    Log.d("DEBUG_MESSAGES", "Loading more local messages");

                    try {
                        msgs = query.getExtraLocalInboxMsgs(msgs);
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
                Log.d("DEBUG_MESSAGES_REFRESH", "On refresh called through pull down listener");
                if (MainActivity.mHeaderProgressBar != null)
                    MainActivity.mHeaderProgressBar.setVisibility(View.GONE);


                Utility.ls(" pull to refresh starting ... ");
                // mHeaderProgressBar.setVisibility(View.GONE);


                if (Utility.isInternetExist()) {
                    Utility.ls(" inbox has to sstart ... ");
                    Log.d("DEBUG_MESSAGES", "calling Inbox execute() pull to refresh");

                    if(!Inbox.isQueued) {
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

    /*
     * LRU Functions *************************************************
     */
    public void addBitmapToMemoryCache(String key, Bitmap bitmap) {

        if (key != null && bitmap != null) {
            if (getBitmapFromMemCache(key) == null) {
                mMemoryCache.put(key, bitmap);
            }
        }
    }

    public Bitmap getBitmapFromMemCache(String key) {
        return mMemoryCache.get(key);
    }


    public void loadBitmap(String imageKey, ImageView mImageView) {

        final Bitmap bitmap = getBitmapFromMemCache(imageKey);
        if (bitmap != null) {
            mImageView.setImageBitmap(bitmap);
        } else {
            Bitmap myBitmap = BitmapFactory.decodeFile(imageKey);
            mImageView.setImageBitmap(myBitmap);
            addBitmapToMemoryCache(imageKey, myBitmap);
        }
    }

    /**
     * ********************* LRU END ******************************
     */

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
                //Log.d("DEBUG_MESSAGES", msgObject.getObjectId() + " newLS" + newState.likeStatus + " curLS" + currentState.likeStatus + " likeCount"  + likeCount + " diff" + diff);
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
                //Log.d("DEBUG_MESSAGES", msgObject.getObjectId() + " newCS" + newState.confusedStatus + " curCS" + currentState.confusedStatus + " conCount"  + confusedCount + " diff" + diff);

                int newConfusedCount = confusedCount + diff;
                if(newConfusedCount < 0 ) newConfusedCount = 0;

                holder.confused.setText(newConfusedCount + "");

                msgObject.put(Constants.GroupDetails.CONFUSED_COUNT, newConfusedCount);
            }

            Log.d("DEBUG_MESSAGES", "new status L/C = " + newState.likeStatus + "/" + newState.confusedStatus +
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

            //Log.d("DEBUG_MESSAGES_DISPLAYING", "profile pic " + filePath);
            File senderThumbnailFile = new File(filePath);

            // ///////////////////////////////////////////////////////////
            final String imagepath;
            if (msgObject.containsKey("attachment_name"))
                imagepath = msgObject.getString("attachment_name");
            else
                imagepath = "";

      /*
       * Setting group name and sender name
       */

            String Str = null;

            if(!UtilString.isBlank(msgObject.getString("name")))
                 Str = msgObject.getString("name").toUpperCase();
            holder.groupName.setText(Str);


            if (msgObject.getCreatedAt() != null) {
                holder.startTime.setText(Utility.convertTimeStamp(msgObject.getCreatedAt()));

                SessionManager sessionManager = new SessionManager(Application.getAppContext());
                //Log.d("INBOX", "message : " + msgObject.getString("title"));
                //Log.d("INBOX", "createdAt : " + msgObject.getCreatedAt().toString());
                //Log.d("INBOX", "current time : " + sessionManager.getCurrentTime().toString());

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

            if (!imagepath.equals(""))
            {
                holder.imgframelayout.setVisibility(View.VISIBLE);
                holder.imgframelayout.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        Intent imgintent = new Intent();
                        imgintent.setAction(Intent.ACTION_VIEW);
                        imgintent.setDataAndType(
                                Uri.parse("file://" + Utility.getWorkingAppDir() + "/media/" + imagepath),
                                "image/*");
                        startActivity(imgintent);
                    }
                });

                File imgFile = new File(Utility.getWorkingAppDir() + "/media/" + imagepath);
                final File thumbnailFile = new File(Utility.getWorkingAppDir() + "/thumbnail/" + imagepath);
                if (imgFile.exists() && !thumbnailFile.exists())
                    Utility.createThumbnail(getActivity(), imagepath);

                // Utility.toast(Utility.getWorkingAppDir() + "/thumbnail/" +
                // imagepath);

                if (imgFile.exists()) {
                    // image file present locally

                    holder.uploadprogressbar.setVisibility(View.GONE);
                    holder.faildownload.setVisibility(View.GONE);
                    holder.imgframelayout.setTag(imgFile.getAbsolutePath());

                    loadBitmap(thumbnailFile.getAbsolutePath(), holder.imgmsgview);

                } else {
                    if(Utility.isInternetExist()) {
                        // Have to download image from server
                        ParseFile imagefile = (ParseFile) msgObject.get("attachment");
                        holder.uploadprogressbar.setVisibility(View.VISIBLE);
                        holder.faildownload.setVisibility(View.GONE);
                        imagefile.getDataInBackground(new GetDataCallback() {
                            public void done(byte[] data, ParseException e) {
                                if (e == null) {
                                    // ////Image download successful
                                    FileOutputStream fos;
                                    try {
                                        fos = new FileOutputStream(Utility.getWorkingAppDir() + "/media/" + imagepath);
                                        try {
                                            fos.write(data);

                                            // Utility.toast("images downloaded in media folder");
                                            // Utility.toast(Utility.getWorkingAppDir()
                                            // + "/media/" + imagepath);

                                            Utility.createThumbnail(getActivity(), imagepath);
                                            Bitmap mynewBitmap =
                                                    BitmapFactory.decodeFile(thumbnailFile.getAbsolutePath());
                                            holder.imgmsgview.setImageBitmap(mynewBitmap);
                                            holder.uploadprogressbar.setVisibility(View.GONE);
                                            holder.faildownload.setVisibility(View.GONE);
                                            myadapter.notifyDataSetChanged();
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

                                    // might be problem
                                } else {
                                    Utility.LogoutUtility.checkAndHandleInvalidSession(e);
                                    // Image not downloaded
                                    holder.uploadprogressbar.setVisibility(View.GONE);
                                    holder.faildownload.setVisibility(View.VISIBLE);
                                }
                            }
                        });

                        holder.imgframelayout.setTag(Utility.getWorkingAppDir() + "/media/" + imagepath);
                        holder.imgmsgview.setImageBitmap(null);
                    } else {
                        holder.uploadprogressbar.setVisibility(View.GONE);
                        holder.faildownload.setVisibility(View.VISIBLE);
                    }
                }
            } else

            {
                holder.imgframelayout.setVisibility(View.GONE);
            }

            //if a) first msg, b) already not shown, c) either non-teacher or fragment # visible is 2 (ie. Messages tab)

            ParseUser currentParseUser = ParseUser.getCurrentUser();
            if(currentParseUser == null){
                return;
            }

            String role = currentParseUser.getString(Constants.ROLE);

            Log.d(ShowcaseCreator.LOGTAG, "(parent)checking response tutorial, location=" + position + ", flag=" + responseTutorialShown
                    + ", role=" + role + ", fragVisible=" + MainActivity.fragmentVisible);

            if(Application.mainActivityVisible && position == 0 && !responseTutorialShown && (!role.equals(Constants.TEACHER) || MainActivity.fragmentVisible == 2) && !ShowcaseView.isVisible){
                String tutorialId = currentParseUser.getUsername() + Constants.TutorialKeys.PARENT_RESPONSE;
                SessionManager mgr = new SessionManager(Application.getAppContext());
                Log.d(ShowcaseCreator.LOGTAG, "(parent)tutorialId=" + tutorialId + " isSignUpAccount=" + mgr.getSignUpAccount() + " tutState=" + mgr.getTutorialState(tutorialId));
                if(mgr.getSignUpAccount() && !mgr.getTutorialState(tutorialId)) { //only if signup account
                    mgr.setTutorialState(tutorialId, true);
                    Log.d(ShowcaseCreator.LOGTAG, "(parent) creating response tutorial");

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

                    Log.d("DEBUG_MESSAGES", "calling Inbox execute() on refresh option click");

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

        if (MainActivity.mHeaderProgressBar != null)
            MainActivity.mHeaderProgressBar.setVisibility(View.GONE);

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

        if (user != null) {
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
        else
        {
            Utility.LogoutUtility.logout(); return;}
    }

    class GetLocalInboxMsgInBackground extends AsyncTask<Void, Void, Void>
    {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                msgs = query.getLocalInboxMsgs();
                updateInboxTotalCount(); //update total inbox count required to manage how/when scrolling loads more messages
            } catch (ParseException e) {
            }

            if (msgs == null)
                msgs = new ArrayList<ParseObject>();


            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {

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
            if(SessionManager.getBooleanValue(key)){//if true set
                Log.d("_FETCH_OLD", "already set in shared prefs");
                oldInboxFetched = true;
                return;
            }

            if(!isGetMoreOldMessagesRunning){
                Log.d("_FETCH_OLD", "spawning GetMoreOldMessages");
                GetMoreOldMessages getMoreOldMessages = new GetMoreOldMessages();
                getMoreOldMessages.execute();
                isGetMoreOldMessagesRunning = true;
            }
            else{
                //Log.d("_FETCH_OLD", "already running GetMoreOldMessages");
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
                    Log.d("_FETCH_OLD", "oldInboxFetched set to true - we're done");
                    oldInboxFetched = true;
                }
            }
            else{
                Log.d("_FETCH_OLD", "extraMessages null - connection failure or other error");
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
