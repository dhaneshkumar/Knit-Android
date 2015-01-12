package trumplabs.schoolapp;

import android.R.color;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.util.LruCache;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

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
import joinclasses.JoinClassesContainer;
import library.UtilString;
import trumplab.textslate.R;
import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshLayout;
import utility.Queries;
import utility.Tools;
import utility.Utility;


/**
 * Class for Inbox's functions & activity
 */
public class Messages extends Fragment {
    public static List<ParseObject> msgs;
    protected LayoutInflater layoutinflater;
    private RecyclerView listv;
    private LinearLayoutManager mLayoutManager;
    public static RecyclerView.Adapter myadapter;
    public static SwipeRefreshLayout mPullToRefreshLayout;
    private LinearLayout inemptylayout;
    private Queries query  ;
    private boolean checkInternet = false;
    // public static LinearLayout progressbar;
    static Button notifCount;
    static int mNotifCount = 0;
    private LruCache<String, Bitmap> mMemoryCache;
    private Typeface typeFace;
    String userId;
    public static int totalInboxMessages; //total pinned messages in inbox

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        layoutinflater = inflater   ;
        getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        return inflater.inflate(R.layout.msgcontainer, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        mPullToRefreshLayout = (SwipeRefreshLayout) getActivity().findViewById(R.id.ptr_layout);
        mPullToRefreshLayout.setColorSchemeColors(Color.RED, Color.GREEN, Color.BLUE, Color.MAGENTA);

        listv = (RecyclerView) getActivity().findViewById(R.id.msg_list);
        inemptylayout = (LinearLayout) getActivity().findViewById(R.id.inemptymsg);

      /*
      Check for push open
       */
        if (getActivity().getIntent().getExtras() != null) {
            boolean pushOpen = getActivity().getIntent().getExtras().getBoolean("pushOpen", false);
            if (pushOpen) {

                if (Utility.isInternetOn(getActivity())) {
                    if (mPullToRefreshLayout != null) {
                        runSwipeRefreshLayout(mPullToRefreshLayout, 10);
                    }

                    Inbox newInboxMsg = new Inbox(msgs);
                    newInboxMsg.execute();
                }
            }
        }


        getActivity().findViewById(R.id.inemptylink).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), JoinClassesContainer.class);
                intent.putExtra("VIEWPAGERINDEX", 1);
                startActivity(intent);
            }
        });

        typeFace =
                Typeface.createFromAsset(getActivity().getAssets(), "fonts/RobotoCondensed-Bold.ttf");

    /*
     * Setting up LRU Cache for images
     */
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

        // Use 1/8th of the available memory for this memory cache.
        final int cacheSize = maxMemory / 8;

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


        ParseUser parseObject = ParseUser.getCurrentUser();

        if (parseObject == null)
            Utility.logout();

        userId = parseObject.getUsername();

        query = new Queries();
        try {
            msgs = query.getLocalInboxMsgs();
        } catch (ParseException e) {
        }

        if (msgs == null) {
            msgs = new ArrayList<ParseObject>();
            inemptylayout.setVisibility(View.VISIBLE);
        } else if (msgs.size() == 0)
            inemptylayout.setVisibility(View.VISIBLE);
        else
            inemptylayout.setVisibility(View.GONE);


        // Collections.reverse(msgs);
        myadapter = new RecycleAdapter();
        listv.setAdapter(myadapter);

        super.onActivityCreated(savedInstanceState);

    /*
     * setup the action bar with pull to refresh layout
     */
       /* mPullToRefreshLayout = (PullToRefreshLayout) getActivity().findViewById(R.id.ptr_layout);
        ActionBarPullToRefresh.from(getActivity()).allChildrenArePullable().listener(this)
                .setup(mPullToRefreshLayout);*/

        //
        // listv.setOnItemLongClickListener(new OnItemLongClickListener() {
        //
        // @Override
        // public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        // final View finalview = view;
        //
        // final CharSequence[] items = {"Copy Message", "Copy Class Name", "Copy Sender Name"};
        //
        // AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // builder.setTitle("Make your selection");
        // builder.setItems(items, new DialogInterface.OnClickListener() {
        // public void onClick(DialogInterface dialog, int item) {
        // // Do something with the selection
        // switch (item) {
        // case 0:
        // Utility.copyToClipBoard(getActivity(), "ClassMessage",
        // ((TextView) finalview.findViewById(R.id.msgs)).getText().toString());
        // break;
        // case 1:
        // Utility.copyToClipBoard(getActivity(), "ClassName",
        // ((TextView) finalview.findViewById(R.id.groupName)).getText().toString());
        // break;
        //
        // case 2:
        // Utility.copyToClipBoard(getActivity(), "SenderName",
        // ((TextView) finalview.findViewById(R.id.sender)).getText().toString());
        // break;
        // default:
        // break;
        // }
        // }
        // });
        // AlertDialog alert = builder.create();
        // alert.show();
        // return true;
        // }
        // });

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

                if(visibleItemCount + pastVisibleItems >= totalItemCount-2){
                    if(totalItemCount >= totalInboxMessages){
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
                if (MainActivity.mHeaderProgressBar != null)
                    MainActivity.mHeaderProgressBar.setVisibility(View.GONE);


                Utility.ls(" pull to refresh starting ... ");
                // mHeaderProgressBar.setVisibility(View.GONE);
                checkInternet = Utility.isInternetOn(getActivity());
                if (Utility.isInternetOn(getActivity())) {


                    Utility.ls(" inbox has to sstart ... ");
                    Inbox newInboxMsg = new Inbox(msgs);
                    newInboxMsg.execute();

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


                // stop refreshing bar after some certain interval


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
        TextView sender;
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
        ImageView senderImg;

        public ViewHolder(View row) {
            super(row);

            groupName = (TextView) row.findViewById(R.id.groupName);
            sender = (TextView) row.findViewById(R.id.sender);
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
            senderImg = (ImageView) row.findViewById(R.id.image);
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
            return msgs.size();
        }

        protected void handleStateChange(final ViewHolder holder, MessageStatePair currentState, MessageStatePair newState, ParseObject msgObject){
            //update button displays if any change
            if(newState.likeStatus - currentState.likeStatus !=0){
                //means like status has changed
                if(newState.likeStatus == 1) {
                    liked(holder.likeIcon, holder.likes, holder.likeButton);
                    msgObject.put(Constants.LIKE, true);
                }
                else {
                    unLiked(holder.likeIcon, holder.likes, holder.likeButton);
                    msgObject.put(Constants.LIKE, false);
                }

                int likeCount = msgObject.getInt(Constants.LIKE_COUNT);
                int diff = newState.likeStatus - currentState.likeStatus;
                Log.d("DEBUG_MESSAGES", "newLS" + newState.likeStatus + " curLS" + currentState.likeStatus + " likeCount"  + likeCount + " diff" + diff);
                int newLikeCount = likeCount + diff;
                if(newLikeCount < 0 ) newLikeCount = 0;

                holder.likes.setText(newLikeCount + "");

                msgObject.put(Constants.LIKE_COUNT, newLikeCount);
            }

            if(newState.confusedStatus - currentState.confusedStatus !=0){
                if (newState.confusedStatus == 1) {
                    confused(holder.confusingIcon, holder.confused, holder.confuseButton);
                    msgObject.put(Constants.CONFUSING, true);
                }
                else {
                    unConfused(holder.confusingIcon, holder.confused, holder.confuseButton);
                    msgObject.put(Constants.CONFUSING, false);
                }

                int confusedCount = msgObject.getInt(Constants.CONFUSED_COUNT);
                int diff = newState.confusedStatus - currentState.confusedStatus;
                Log.d("DEBUG_MESSAGES", "newLS" + newState.confusedStatus + " curLS" + currentState.confusedStatus + " conCount"  + confusedCount + " diff" + diff);

                int newConfusedCount = confusedCount + diff;
                if(newConfusedCount < 0 ) newConfusedCount = 0;

                holder.confused.setText(newConfusedCount + "");


                msgObject.put(Constants.CONFUSED_COUNT, newConfusedCount);
            }

            msgObject.put(Constants.DIRTY_BIT, true);
            //updating msgObject locally
            msgObject.pinInBackground();
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, final int position) {

            final ParseObject msgObject = msgs.get(position);


      /*
       * Set likes and confused count
       */
            int likeCount = msgObject.getInt(Constants.LIKE_COUNT);
            if(likeCount < 0) likeCount = 0;
            int confusedCount = msgObject.getInt(Constants.CONFUSED_COUNT);
            if(confusedCount < 0) confusedCount = 0;

            holder.likes.setText(likeCount + "");
            holder.confused.setText(confusedCount + "");


            if (msgObject.getBoolean(Constants.LIKE))
                liked(holder.likeIcon, holder.likes, holder.likeButton);
            else
                unLiked(holder.likeIcon, holder.likes, holder.likeButton);

            if (msgObject.getBoolean(Constants.CONFUSING))
                confused(holder.confusingIcon, holder.confused, holder.confuseButton);
            else
                unConfused(holder.confusingIcon, holder.confused, holder.confuseButton);


//            Boolean x = true;
//            if(x) return;
      /*
       * Setting likes and confused button functionality
       */


            holder.likeButton.setOnClickListener(new OnClickListener() {

                                                     @Override
                                                     public void onClick(View v) {
                                                         final MessageStatePair currentState = new MessageStatePair(0, 0);

                                                         if(msgObject.getBoolean(Constants.LIKE)){
                                                             currentState.likeStatus = 1;
                                                         }
                                                         if(msgObject.getBoolean(Constants.CONFUSING)){
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

                                                            if(msgObject.getBoolean(Constants.LIKE)){
                                                                currentState.likeStatus = 1;
                                                            }
                                                            if(msgObject.getBoolean(Constants.CONFUSING)){
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

            String senderId = msgObject.getString("senderId");

            // senderId = senderId.replaceAll(".", "");
            senderId = senderId.replaceAll("@", "");
            String filePath = Utility.getWorkingAppDir() + "/thumbnail/" + senderId + "_PC.jpg";

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

            String Str = msgObject.getString("name").toUpperCase();
            holder.groupName.setText(Str);

            Str = msgObject.getString("Creator");
            holder.sender.setText(Str);

            if (senderThumbnailFile.exists())

            {

                // image file present locally
                Bitmap mySenderBitmap = BitmapFactory.decodeFile(senderThumbnailFile.getAbsolutePath());
                holder.senderImg.setImageBitmap(mySenderBitmap);
            } else

            {

                ParseQuery<ParseObject> delquery1 = new ParseQuery<ParseObject>("Codegroup");
                delquery1.fromLocalDatastore();

                String code = msgObject.getString("code");
                if (code != null) {
                    delquery1.whereEqualTo("code", code.trim());
                    try {
                        ParseObject obj = delquery1.getFirst();

                        String sex = obj.getString("sex");

                        if (!UtilString.isBlank(sex)) {

                            if (sex.equals("M"))
                                holder.senderImg.setImageResource(R.drawable.maleteacherdp);
                            else if (sex.equals("F"))
                                holder.senderImg.setImageResource(R.drawable.femaleteacherdp);
                        } else {

                            // if sex is not stored
                            if (!UtilString.isBlank(Str)) {
                                String[] names = Str.split("\\s");

                                if (names != null && names.length > 1) {
                                    String title = names[0].trim();

                                    if (title.equals("Mr")) {
                                        holder.senderImg.setImageResource(R.drawable.maleteacherdp);
                                        obj.put("sex", "M");
                                        obj.pin();
                                    } else if (title.equals("Mrs")) {
                                        holder.senderImg.setImageResource(R.drawable.femaleteacherdp);
                                        obj.put("sex", "F");
                                        obj.pin();
                                    } else if (title.equals("Ms")) {
                                        holder.senderImg.setImageResource(R.drawable.femaleteacherdp);
                                        obj.put("sex", "F");
                                        obj.pin();
                                    } else
                                        holder.senderImg.setImageResource(R.drawable.logo);
                                } else
                                    holder.senderImg.setImageResource(R.drawable.logo);
                            } else
                                holder.senderImg.setImageResource(R.drawable.logo);
                        }
                    } catch (ParseException e) {
                    }
                }
            }

            try

            {
                if (msgObject.getCreatedAt() != null)
                    holder.startTime.setText(Utility.convertTimeStamp(msgObject.getCreatedAt()));
                else if (msgObject.get("creationTime") != null)
                    holder.startTime.setText(Utility.convertTimeStamp((Date) msgObject.get("creationTime")));
            } catch (
                    java.text.ParseException e
                    )

            {
            }

            if (msgObject.getString("title").

                    equals("")

                    )
                holder.msgslist.setVisibility(View.GONE);
            else

            {
                holder.msgslist.setVisibility(View.VISIBLE);
                holder.msgslist.setText(msgObject.getString("title"));
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
                    if (Utility.isInternetOn(getActivity())) {
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

        }
    }



  /*  @Override
    public void onRefreshStarted(View view) {

        if (MainActivity.mHeaderProgressBar != null)
            MainActivity.mHeaderProgressBar.setVisibility(View.GONE);

        // mHeaderProgressBar.setVisibility(View.GONE);
        checkInternet = Utility.isInternetOn(getActivity());
        if (Utility.isInternetOn(getActivity())) {
            Inbox newInboxMsg = new Inbox(msgs);
            newInboxMsg.execute();
        } else {
            // Utility.toast("Check your Internet connection");
            mPullToRefreshLayout.setRefreshComplete();
        }

    *//*
     * stop refreshing bar after some certain interval
     *//*
        final Handler h = new Handler() {
            @Override
            public void handleMessage(Message message) {

                if (mPullToRefreshLayout != null)
                    mPullToRefreshLayout.setRefreshComplete();
            }
        };
        h.sendMessageDelayed(new Message(), 10000);

    }*/


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.refresh:
                if (Utility.isInternetOn(getActivity())) {
                    Utility.ls(" option selected  ... ");

                    if (mPullToRefreshLayout != null) {
                        Utility.ls(" option selected pull to refresh starting ... ");
                        if(mPullToRefreshLayout.isRefreshing())
                            mPullToRefreshLayout.setRefreshing(false);
                        mPullToRefreshLayout.setRefreshing(true);
                        runSwipeRefreshLayout(mPullToRefreshLayout, 10);
                    }
                    else
                        Utility.ls(" option selected  ...null ");


                    Inbox newInboxMsg = new Inbox(msgs);
                    newInboxMsg.execute();
                }
                else
                    Utility.toast("Check your Internet Connection");

                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }


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


}
