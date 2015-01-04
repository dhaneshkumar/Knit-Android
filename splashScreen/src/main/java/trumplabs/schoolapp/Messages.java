package trumplabs.schoolapp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import joinclasses.JoinClassesContainer;
import library.ExpandableListView;
import library.UtilString;
import trumplab.textslate.R;
import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;
import utility.Queries;
import utility.Queries2;
import utility.Tools;
import utility.Utility;
import BackGroundProcesses.Inbox;

import android.R.color;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.util.LruCache;
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

/**
 * Class for Inbox's functions & activity
 */
public class Messages extends Fragment implements OnRefreshListener {
    public static List<ParseObject> msgs;
    protected LayoutInflater layoutinflater;
    public static ListView listv;
    public static PullToRefreshLayout mPullToRefreshLayout;
    private LinearLayout inemptylayout;
    private Queries query;
    public static BaseAdapter myadapter;
    private boolean checkInternet = false;
    // public static LinearLayout progressbar;
    ImageView imgmsgview;
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
        mPullToRefreshLayout = (PullToRefreshLayout) getActivity().findViewById(R.id.ptr_layout);
        listv = (ListView) getActivity().findViewById(R.id.msg_list);
        inemptylayout = (LinearLayout) getActivity().findViewById(R.id.inemptymsg);

      /*
      Check for push open
       */
        if (getActivity().getIntent().getExtras() != null) {
            boolean pushOpen = getActivity().getIntent().getExtras().getBoolean("pushOpen", false);
            if (pushOpen) {

                if (Utility.isInternetOn(getActivity())) {
                    if (MainActivity.mHeaderProgressBar != null) {
                        Tools.runSmoothProgressBar(MainActivity.mHeaderProgressBar, 10);
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


        myadapter = new myBaseAdapter();

        ParseUser parseObject = ParseUser.getCurrentUser();

        if (parseObject == null)
            Utility.logout();

        String userId = parseObject.getUsername();

        query = new Queries();
        try {
            msgs = query.getLocalInboxMsgs();
        } catch (ParseException e) {
        }

        if (msgs == null)
            msgs = new ArrayList<ParseObject>();

        // Collections.reverse(msgs);
        listv.setAdapter(myadapter);


        super.onActivityCreated(savedInstanceState);

    /*
     * setup the action bar with pull to refresh layout
     */
        mPullToRefreshLayout = (PullToRefreshLayout) getActivity().findViewById(R.id.ptr_layout);
        ActionBarPullToRefresh.from(getActivity()).allChildrenArePullable().listener(this)
                .setup(mPullToRefreshLayout);

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
        listv.setOnScrollListener(new OnScrollListener() {
            int lastCount = 0;

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
                                 int totalItemCount) {

                if (firstVisibleItem + visibleItemCount >= totalItemCount) {

                    if (lastCount != totalItemCount || lastCount == 0) {

                        lastCount = totalItemCount;
                        // mHeaderProgressBar.setVisibility(View.GONE);
                        try {
                            msgs = query.getExtraLocalInboxMsgs(msgs);
                        } catch (ParseException e) {
                        }

                        // lastCount = msgs.size();
                        myadapter.notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
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
            imgmsgview.setImageBitmap(myBitmap);
            addBitmapToMemoryCache(imageKey, myBitmap);
        }
    }

    /**
     * ********************* LRU END ******************************
     */


    private class GetDataFromLocalDatabase extends AsyncTask<Void, Void, String[]> {

        String[] mStrings;

        @Override
        protected String[] doInBackground(Void... params) {

            try {
                msgs = query.getExtraLocalInboxMsgs(msgs);
            } catch (ParseException e) {
            }
            return mStrings;
        }

        @Override
        protected void onPostExecute(String[] result) {
            myadapter.notifyDataSetChanged();

            // Call onRefreshComplete when the list has been refreshed.
            mPullToRefreshLayout.setRefreshComplete();

            super.onPostExecute(result);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    class myBaseAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            if (msgs.size() == 0) {
                listv.setVisibility(View.VISIBLE);
                inemptylayout.setVisibility(View.VISIBLE);
            } else {
                listv.setVisibility(View.VISIBLE);
                inemptylayout.setVisibility(View.GONE);
            }
            return msgs.size();
        }


        @Override
        public Object getItem(int position) {

            if (position >= msgs.size())
                return null;
            else
                return msgs.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View row = convertView;
            if (row == null) {
                row = layoutinflater.inflate(R.layout.messages_likes, parent, false);
            }

            // on select list item, set background color white
            row.setBackgroundColor(getResources().getColor(color.white));

      /*
       * Intializing variables
       */
            TextView groupName = (TextView) row.findViewById(R.id.groupName);
            TextView sender = (TextView) row.findViewById(R.id.sender);
            TextView startTime = (TextView) row.findViewById(R.id.startTime);
            TextView msgslist = (TextView) row.findViewById(R.id.msgs);
            FrameLayout imgframelayout = (FrameLayout) row.findViewById(R.id.imagefrmlayout);
            imgmsgview = (ImageView) row.findViewById(R.id.imgmsgcontent);
            final TextView faildownload = (TextView) row.findViewById(R.id.faildownload);
            final ProgressBar uploadprogressbar = (ProgressBar) row.findViewById(R.id.msgprogressbar);


            final LinearLayout likeButton = (LinearLayout) row.findViewById(R.id.likeButton);
            final LinearLayout confuseButton = (LinearLayout) row.findViewById(R.id.confuseButton);
            final TextView likes = (TextView) row.findViewById(R.id.like);
            final TextView confused = (TextView) row.findViewById(R.id.confusion);
            final ImageView likeIcon = (ImageView) row.findViewById(R.id.likeIcon);
            final ImageView confusingIcon = (ImageView) row.findViewById(R.id.confusionIcon);

            // Setting type face
            //likes.setTypeface(typeFace);
            //confused.setTypeface(typeFace);


            final ParseObject object = (ParseObject) getItem(position);

      /*
       * Setting likes and confused count
       */


            if (object.getInt(Constants.LIKE_COUNT) > 0)
                likes.setText(object.getInt(Constants.LIKE_COUNT) + "");
            if (object.getInt(Constants.CONFUSED_COUNT) > 0)
                confused.setText(object.getInt(Constants.CONFUSED_COUNT) + "");


            if (object.getBoolean(Constants.LIKE))
                liked(likeIcon, likes, likeButton);
            else
                unLiked(likeIcon, likes, likeButton);

            if (object.getBoolean(Constants.CONFUSING))
                confused(confusingIcon, confused, confuseButton);
            else
                unConfused(confusingIcon, confused, confuseButton);



      /*
       * Setting likes and confused button functionality
       */


            likeButton.setOnClickListener(new OnClickListener() {

                                              @Override
                                              public void onClick(View v) {

                                                  ParseObject object1 = object;

                                                  boolean likeFlag = object1.getBoolean(Constants.LIKE);
                                                  boolean confusingFlag =
                                                          object1.getBoolean(Constants.CONFUSING);

                                                  if (likeFlag) {
                                                      likeFlag = false;

                                                      unLiked(likeIcon, likes, likeButton);

                                                      object1.put(Constants.LIKE, likeFlag);

                                                      int likeCount = object1.getInt(Constants.LIKE_COUNT);

                                                      if (likeCount > 0) {
                                                          object1.put(Constants.LIKE_COUNT, --likeCount);
                                                          likes.setText(likeCount + "");

                                                          //updating locally
                                                          object1.pinInBackground();
                                                          //updating globally
                                                          MessagesHelper.DecreaseLikeCount decreaseLikeCount = new MessagesHelper.DecreaseLikeCount(object1.getObjectId());
                                                          decreaseLikeCount.execute();
                                                      }
                                                  } else {


                                                      likeFlag = true;
                                                      liked(likeIcon, likes, likeButton);

                                                      object1.put(Constants.LIKE, likeFlag);
                                                      int likeCount = object1.getInt(Constants.LIKE_COUNT);
                                                      object1.put(Constants.LIKE_COUNT, ++likeCount);
                                                      likes.setText(likeCount + "");

                                                      //updating globally
                                                      MessagesHelper.IncreaseLikeCount increaseLikeCount = new MessagesHelper.IncreaseLikeCount(object1);
                                                      increaseLikeCount.execute();

                                                      //Ensuring that both buttons are not clicked simultaneously

                                                      if (!confusingFlag) {
                                                          //storing locally
                                                          try {
                                                              object1.pin();
                                                          } catch (ParseException e) {
                                                              e.printStackTrace();
                                                          }
                                                      } else {
                                                          confusingFlag = false;
                                                          unConfused(confusingIcon, confused, confuseButton);
                                                          object1.put(Constants.CONFUSING, false);

                                                          int confusionedCount = object1.getInt(Constants.CONFUSED_COUNT);
                                                          if (confusionedCount > 0) {
                                                              confusionedCount--;
                                                              object1.put(Constants.CONFUSED_COUNT, confusionedCount);
                                                              confused.setText(confusionedCount + "");

                                                              //updating locally
                                                              object1.pinInBackground();

                                                              MessagesHelper.DecreaseConfusedCount decreaseConfusedCount = new MessagesHelper.DecreaseConfusedCount(object1.getObjectId());
                                                              decreaseConfusedCount.execute();

                                                          }
                                                      }

                                                  }
                                              }
                                          }
            );


            // Setting like and confusing button functionality


            confuseButton.setOnClickListener(new OnClickListener() {

                                                 @Override
                                                 public void onClick(View v) {

                                                     ParseObject object1 = (ParseObject) getItem(position);

                                                     boolean likeFlag = object1.getBoolean(Constants.LIKE);
                                                     boolean confusingFlag =
                                                             object1.getBoolean(Constants.CONFUSING);
                                                     if (confusingFlag) {
                                                         confusingFlag = false;

                                                         unConfused(confusingIcon, confused, confuseButton);

                                                         object1.put(Constants.CONFUSING, false);
                                                         int confusionedCount = object1.getInt(Constants.CONFUSED_COUNT);
                                                         if (confusionedCount > 0) {
                                                             confusionedCount--;
                                                             object1.put(Constants.CONFUSED_COUNT, confusionedCount);
                                                             confused.setText(confusionedCount + "");

                                                             //updating locally
                                                             object1.pinInBackground();

                                                             MessagesHelper.DecreaseConfusedCount decreaseConfusedCount = new MessagesHelper.DecreaseConfusedCount(object1.getObjectId());
                                                             decreaseConfusedCount.execute();
                                                         }

                                                     } else {

                                                         confusingFlag = true;

                                                         confused(confusingIcon, confused, confuseButton);

                                                         object1.put(Constants.CONFUSING, true);
                                                         int confusions =
                                                                 object1.getInt(Constants.CONFUSED_COUNT);
                                                         object1.put(Constants.CONFUSED_COUNT,
                                                                 ++confusions);
                                                         confused.setText(confusions + "");
                                                         MessagesHelper.IncreaseCounfusedCount increaseCounfusedCount = new MessagesHelper.IncreaseCounfusedCount(object1.getObjectId());
                                                         increaseCounfusedCount.execute();

                                                         if (!likeFlag) {

                                                             object1.pinInBackground();
                                                         } else {
                                                             likeFlag = false;

                                                             unLiked(likeIcon, likes, likeButton);

                                                             object1.put(Constants.LIKE, false);

                                                             int likeCount = object1.getInt(Constants.LIKE_COUNT);

                                                             if (likeCount > 0) {
                                                                 object1.put(Constants.LIKE_COUNT, --likeCount);
                                                                 likes.setText(likeCount + "");

                                                                 //updating globally
                                                                 MessagesHelper.DecreaseLikeCount decreaseLikeCount = new MessagesHelper.DecreaseLikeCount(object1.getObjectId());
                                                                 decreaseLikeCount.execute();

                                                                 //updating locally
                                                                 object1.pinInBackground();
                                                             }

                                                         }
                                                     }
                                                 }
                                             }

            );


            uploadprogressbar.setVisibility(View.GONE);

            ImageView senderImg = (ImageView) row.findViewById(R.id.image);
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
            groupName.setText(Str);

            Str = object.getString("Creator");
            sender.setText(Str);

            if (senderThumbnailFile.exists())

            {

                // image file present locally
                Bitmap mySenderBitmap = BitmapFactory.decodeFile(senderThumbnailFile.getAbsolutePath());
                senderImg.setImageBitmap(mySenderBitmap);
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
                                senderImg.setImageResource(R.drawable.maleteacherdp);
                            else if (sex.equals("F"))
                                senderImg.setImageResource(R.drawable.femaleteacherdp);
                        } else {

                            // if sex is not stored
                            if (!UtilString.isBlank(Str)) {
                                String[] names = Str.split("\\s");

                                if (names != null && names.length > 1) {
                                    String title = names[0].trim();

                                    if (title.equals("Mr")) {
                                        senderImg.setImageResource(R.drawable.maleteacherdp);
                                        obj.put("sex", "M");
                                        obj.pin();
                                    } else if (title.equals("Mrs")) {
                                        senderImg.setImageResource(R.drawable.femaleteacherdp);
                                        obj.put("sex", "F");
                                        obj.pin();
                                    } else if (title.equals("Ms")) {
                                        senderImg.setImageResource(R.drawable.femaleteacherdp);
                                        obj.put("sex", "F");
                                        obj.pin();
                                    } else
                                        senderImg.setImageResource(R.drawable.logo);
                                } else
                                    senderImg.setImageResource(R.drawable.logo);
                            } else
                                senderImg.setImageResource(R.drawable.logo);
                        }
                    } catch (ParseException e) {
                    }
                }
            }

            try

            {
                if (object.getCreatedAt() != null)
                    startTime.setText(Utility.convertTimeStamp(object.getCreatedAt()));
                else if (object.get("creationTime") != null)
                    startTime.setText(Utility.convertTimeStamp((Date) object.get("creationTime")));
            } catch (
                    java.text.ParseException e
                    )

            {
            }

            if (object.getString("title").

                    equals("")

                    )
                msgslist.setVisibility(View.GONE);
            else

            {
                msgslist.setVisibility(View.VISIBLE);
                msgslist.setText(object.getString("title"));
            }

            if (!imagepath.equals(""))

            {
                imgframelayout.setVisibility(View.VISIBLE);
                imgframelayout.setOnClickListener(new OnClickListener() {

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

                    uploadprogressbar.setVisibility(View.GONE);
                    faildownload.setVisibility(View.GONE);
                    imgframelayout.setTag(imgFile.getAbsolutePath());

                    loadBitmap(thumbnailFile.getAbsolutePath(), imgmsgview);


                } else {
                    if (Utility.isInternetOn(getActivity())) {
                        // Have to download image from server
                        ParseFile imagefile = (ParseFile) object.get("attachment");
                        uploadprogressbar.setVisibility(View.VISIBLE);
                        faildownload.setVisibility(View.GONE);
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
                                            imgmsgview.setImageBitmap(mynewBitmap);
                                            uploadprogressbar.setVisibility(View.GONE);
                                            faildownload.setVisibility(View.GONE);
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
                                    uploadprogressbar.setVisibility(View.GONE);
                                    faildownload.setVisibility(View.VISIBLE);
                                }
                            }
                        });

                        imgframelayout.setTag(Utility.getWorkingAppDir() + "/media/" + imagepath);
                        imgmsgview.setImageBitmap(null);
                    } else {
                        uploadprogressbar.setVisibility(View.GONE);
                        faildownload.setVisibility(View.VISIBLE);
                    }
                }
            } else

            {
                imgframelayout.setVisibility(View.GONE);
            }

            return row;
        }
    }

    @Override
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

    /*
     * stop refreshing bar after some certain interval
     */
        final Handler h = new Handler() {
            @Override
            public void handleMessage(Message message) {

                if (mPullToRefreshLayout != null)
                    mPullToRefreshLayout.setRefreshComplete();
            }
        };
        h.sendMessageDelayed(new Message(), 10000);

    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.refresh:
                if (Utility.isInternetOn(getActivity())) {

                    if (MainActivity.mHeaderProgressBar != null) {
                        Tools.runSmoothProgressBar(MainActivity.mHeaderProgressBar, 10);
                    }

                    Inbox newInboxMsg = new Inbox(msgs);
                    newInboxMsg.execute();
                } else {
                    Utility.toast("Check your Internet connection");
                }
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

}
