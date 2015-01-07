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
    public static RecyclerView listv;
    private RecyclerView.LayoutManager mLayoutManager;
    public static RecyclerView.Adapter myadapter;
    public static SwipeRefreshLayout mPullToRefreshLayout;
    private LinearLayout inemptylayout;
    private Queries query;
    private boolean checkInternet = false;
    // public static LinearLayout progressbar;
    static Button notifCount;
    static int mNotifCount = 0;
    private LruCache<String, Bitmap> mMemoryCache;
    private Typeface typeFace;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        layoutinflater = inflater;
        getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        return inflater.inflate(R.layout.msgcontainer, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        mPullToRefreshLayout = (SwipeRefreshLayout) getActivity().findViewById(R.id.ptr_layout);
        mPullToRefreshLayout.setColorSchemeColors(Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW);

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

        String userId = parseObject.getUsername();

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

                if (dy > 5) {

                    try {
                        msgs = query.getExtraLocalInboxMsgs(msgs);
                    } catch (ParseException e) {
                    }

                    // lastCount = msgs.size();
                    myadapter.notifyDataSetChanged();
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

                // mHeaderProgressBar.setVisibility(View.GONE);
                checkInternet = Utility.isInternetOn(getActivity());
                if (Utility.isInternetOn(getActivity())) {
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

        @Override
        public void onBindViewHolder(final ViewHolder holder, final int position) {

            final ParseObject object = msgs.get(position);

      /*
       * Setting likes and confused count
       */
            if (object.getInt(Constants.LIKE_COUNT) > 0)
                (holder.likes).setText(object.getInt(Constants.LIKE_COUNT) + "");
            if (object.getInt(Constants.CONFUSED_COUNT) > 0)
                holder.confused.setText(object.getInt(Constants.CONFUSED_COUNT) + "");


            if (object.getBoolean(Constants.LIKE))
                liked(holder.likeIcon, holder.likes, holder.likeButton);
            else
                unLiked(holder.likeIcon, holder.likes, holder.likeButton);

            if (object.getBoolean(Constants.CONFUSING))
                confused(holder.confusingIcon, holder.confused, holder.confuseButton);
            else
                unConfused(holder.confusingIcon, holder.confused, holder.confuseButton);



      /*
       * Setting likes and confused button functionality
       */


            holder.likeButton.setOnClickListener(new OnClickListener() {

                                                     @Override
                                                     public void onClick(View v) {


                                                         boolean likeFlag = object.getBoolean(Constants.LIKE);
                                                         boolean confusingFlag =
                                                                 object.getBoolean(Constants.CONFUSING);

                                                         if (likeFlag) {
                                                             likeFlag = false;

                                                             unLiked(holder.likeIcon, holder.likes, holder.likeButton);

                                                             object.put(Constants.LIKE, likeFlag);

                                                             int likeCount = object.getInt(Constants.LIKE_COUNT);

                                                             if (likeCount > 0) {
                                                                 object.put(Constants.LIKE_COUNT, --likeCount);
                                                                 holder.likes.setText(likeCount + "");

                                                                 //updating locally
                                                                 object.pinInBackground();
                                                                 //updating globally
                                                                 MessagesHelper.DecreaseLikeCount decreaseLikeCount = new MessagesHelper.DecreaseLikeCount(object.getObjectId());
                                                                 decreaseLikeCount.execute();
                                                             }
                                                         } else {


                                                             likeFlag = true;
                                                             liked(holder.likeIcon, holder.likes, holder.likeButton);

                                                             object.put(Constants.LIKE, likeFlag);
                                                             int likeCount = object.getInt(Constants.LIKE_COUNT);
                                                             object.put(Constants.LIKE_COUNT, ++likeCount);
                                                             holder.likes.setText(likeCount + "");

                                                             //updating globally
                                                             MessagesHelper.IncreaseLikeCount increaseLikeCount = new MessagesHelper.IncreaseLikeCount(object);
                                                             increaseLikeCount.execute();

                                                             //Ensuring that both buttons are not clicked simultaneously

                                                             if (!confusingFlag) {
                                                                 //storing locally
                                                                 try {
                                                                     object.pin();
                                                                 } catch (ParseException e) {
                                                                     e.printStackTrace();
                                                                 }
                                                             } else {
                                                                 confusingFlag = false;
                                                                 unConfused(holder.confusingIcon, holder.confused, holder.confuseButton);
                                                                 object.put(Constants.CONFUSING, false);

                                                                 int confusionedCount = object.getInt(Constants.CONFUSED_COUNT);
                                                                 if (confusionedCount > 0) {
                                                                     confusionedCount--;
                                                                     object.put(Constants.CONFUSED_COUNT, confusionedCount);
                                                                     holder.confused.setText(confusionedCount + "");

                                                                     //updating locally
                                                                     object.pinInBackground();

                                                                     MessagesHelper.DecreaseConfusedCount decreaseConfusedCount = new MessagesHelper.DecreaseConfusedCount(object.getObjectId());
                                                                     decreaseConfusedCount.execute();

                                                                 }
                                                             }

                                                         }
                                                     }
                                                 }
            );


            // Setting like and confusing button functionality


            holder.confuseButton.setOnClickListener(new OnClickListener() {

                                                        @Override
                                                        public void onClick(View v) {


                                                            boolean likeFlag = object.getBoolean(Constants.LIKE);
                                                            boolean confusingFlag =
                                                                    object.getBoolean(Constants.CONFUSING);
                                                            if (confusingFlag) {
                                                                confusingFlag = false;

                                                                unConfused(holder.confusingIcon, holder.confused, holder.confuseButton);

                                                                object.put(Constants.CONFUSING, false);
                                                                int confusionedCount = object.getInt(Constants.CONFUSED_COUNT);
                                                                if (confusionedCount > 0) {
                                                                    confusionedCount--;
                                                                    object.put(Constants.CONFUSED_COUNT, confusionedCount);
                                                                    holder.confused.setText(confusionedCount + "");

                                                                    //updating locally
                                                                    object.pinInBackground();

                                                                    MessagesHelper.DecreaseConfusedCount decreaseConfusedCount = new MessagesHelper.DecreaseConfusedCount(object.getObjectId());
                                                                    decreaseConfusedCount.execute();
                                                                }

                                                            } else {

                                                                confusingFlag = true;

                                                                confused(holder.confusingIcon, holder.confused, holder.confuseButton);

                                                                object.put(Constants.CONFUSING, true);
                                                                int confusions =
                                                                        object.getInt(Constants.CONFUSED_COUNT);
                                                                object.put(Constants.CONFUSED_COUNT,
                                                                        ++confusions);
                                                                holder.confused.setText(confusions + "");
                                                                MessagesHelper.IncreaseCounfusedCount increaseCounfusedCount = new MessagesHelper.IncreaseCounfusedCount(object.getObjectId());
                                                                increaseCounfusedCount.execute();

                                                                if (!likeFlag) {

                                                                    object.pinInBackground();
                                                                } else {
                                                                    likeFlag = false;

                                                                    unLiked(holder.likeIcon, holder.likes, holder.likeButton);

                                                                    object.put(Constants.LIKE, false);

                                                                    int likeCount = object.getInt(Constants.LIKE_COUNT);

                                                                    if (likeCount > 0) {
                                                                        object.put(Constants.LIKE_COUNT, --likeCount);
                                                                        holder.likes.setText(likeCount + "");

                                                                        //updating globally
                                                                        MessagesHelper.DecreaseLikeCount decreaseLikeCount = new MessagesHelper.DecreaseLikeCount(object.getObjectId());
                                                                        decreaseLikeCount.execute();

                                                                        //updating locally
                                                                        object.pinInBackground();
                                                                    }

                                                                }
                                                            }
                                                        }
                                                    }

            );


            holder.uploadprogressbar.setVisibility(View.GONE);

            String senderId = object.getString("senderId");

            // senderId = senderId.replaceAll(".", "");
            senderId = senderId.replaceAll("@", "");
            String filePath = Utility.getWorkingAppDir() + "/thumbnail/" + senderId + "_PC.jpg";

            File senderThumbnailFile = new File(filePath);

            // ///////////////////////////////////////////////////////////
            final String imagepath;
            if (object.containsKey("attachment_name"))
                imagepath = object.getString("attachment_name");
            else
                imagepath = "";

      /*
       * Setting group name and sender name
       */

            String Str = object.getString("name").toUpperCase();
            holder.groupName.setText(Str);

            Str = object.getString("Creator");
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

                String code = object.getString("code");
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
                if (object.getCreatedAt() != null)
                    holder.startTime.setText(Utility.convertTimeStamp(object.getCreatedAt()));
                else if (object.get("creationTime") != null)
                    holder.startTime.setText(Utility.convertTimeStamp((Date) object.get("creationTime")));
            } catch (
                    java.text.ParseException e
                    )

            {
            }

            if (object.getString("title").

                    equals("")

                    )
                holder.msgslist.setVisibility(View.GONE);
            else

            {
                holder.msgslist.setVisibility(View.VISIBLE);
                holder.msgslist.setText(object.getString("title"));
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
                        ParseFile imagefile = (ParseFile) object.get("attachment");
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

                    if (mPullToRefreshLayout != null) {
                        runSwipeRefreshLayout(mPullToRefreshLayout, 10);
                    }

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
        likeButton.setBackgroundColor(getResources().getColor(R.color.buttoncolor));
    }

    void unLiked(ImageView likeIcon, TextView likes, LinearLayout likeButton) {
        likeIcon.setImageResource(R.drawable.ic_action_like);
        likes.setTextColor(getResources().getColor(R.color.buttoncolor));
        likeButton.setBackgroundColor(getResources().getColor(R.color.white));
    }

    void confused(ImageView confusingIcon, TextView confused, LinearLayout confuseButton) {
        confusingIcon.setImageResource(R.drawable.ic_action_help1);
        confused.setTextColor(getResources().getColor(R.color.white));
        confuseButton.setBackgroundColor(getResources().getColor(R.color.buttoncolor));
    }

    void unConfused(ImageView confusingIcon, TextView confused, LinearLayout confuseButton) {
        confusingIcon.setImageResource(R.drawable.ic_action_help);
        confused.setTextColor(getResources().getColor(R.color.buttoncolor));
        confuseButton.setBackgroundColor(getResources().getColor(R.color.white));
    }


    /*
   stop swipe refreshlayout
    */
    private void runSwipeRefreshLayout(final SwipeRefreshLayout mPullToRefreshLayout, final int seconds) {

        if (mPullToRefreshLayout == null)
            return;

        mPullToRefreshLayout.setRefreshing(true);
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
