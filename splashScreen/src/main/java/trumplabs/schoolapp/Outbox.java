package trumplabs.schoolapp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

import BackGroundProcesses.SyncMessageDetails;
import library.UtilString;
import trumplab.textslate.R;
import utility.Queries;
import utility.SessionManager;
import utility.Utility;

/**
 * Outbox Activity showing Outbox fragment of homepage
 */
public class Outbox extends Fragment {
    protected LayoutInflater layoutinflater;
    public static RecycleAdapter myadapter;
    private RecyclerView outboxListv;
    Queries query;
    public static List<ParseObject> groupDetails; // List of group messages
    Activity myActivity;
    private LinearLayoutManager mLayoutManager;
    SessionManager session;
    private static SwipeRefreshLayout outboxRefreshLayout;
    public static LinearLayout outboxLayout;
    public static int totalOutboxMessages = 15; //total pinned outbox messages(across all classes)

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        layoutinflater = inflater;
        View layoutview = inflater.inflate(R.layout.outbox, container, false);
        return layoutview;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        //intializing variables
        outboxListv = (RecyclerView) getActivity().findViewById(R.id.outboxlistview);
        myActivity = getActivity();
        session = new SessionManager(Application.getAppContext());
        query = new Queries();
        outboxRefreshLayout = (SwipeRefreshLayout) getActivity().findViewById(R.id.ptr_outbox);
        outboxRefreshLayout.setColorSchemeColors(Color.RED, Color.GREEN, Color.BLUE, Color.MAGENTA);
        outboxLayout = (LinearLayout) getActivity().findViewById(R.id.outboxmsg);

        //retrieving lcoally stored outbox messges
        groupDetails = query.getLocalOutbox();
        if (groupDetails == null) {
            groupDetails = new ArrayList<ParseObject>();
            outboxLayout.setVisibility(View.VISIBLE);
        } else if (groupDetails.size() == 0)
            outboxLayout.setVisibility(View.VISIBLE);
        else
            outboxLayout.setVisibility(View.GONE);
        if (groupDetails == null)
            groupDetails = new ArrayList<ParseObject>();

        //setting recycle view & layout
        mLayoutManager = new LinearLayoutManager(getActivity());
        outboxListv.setLayoutManager(mLayoutManager);
        myadapter = new RecycleAdapter();
        outboxListv.setAdapter(myadapter);

        super.onActivityCreated(savedInstanceState);


        getActivity().findViewById(R.id.outboxlink).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fragmentmanager = getActivity().getSupportFragmentManager();
                getActivity().setTitle("Classrooms"); //set the title explicitly. o/w it shows partial text

                MainActivity.viewpager.setAdapter(new MainActivity.MyAdapter(fragmentmanager));
                MainActivity.viewpager.setCurrentItem(0);
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

                if(visibleItemCount + pastVisibleItems >= totalItemCount-1){
                    //Log.d("DEBUG_OUTBOX_MESSAGES_SCROLL", "showing " + totalItemCount + " totalpinnned " + totalOutboxMessages);
                    if(totalItemCount >= totalOutboxMessages){
                        Log.d("DEBUG_OUTBOX_MESSAGES_SCROLL", "[" + (visibleItemCount + pastVisibleItems) + " out of" + totalOutboxMessages + "]all messages loaded. Saving unnecessary query");
                        return; //nothing to do as all messages have been loaded
                    }

                    try {
                        groupDetails = query.getExtraLocalOutbox(groupDetails);
                        myadapter.notifyDataSetChanged();
                    } catch (ParseException e) {
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

                //hiding main activity's progress bar
                if (MainActivity.mHeaderProgressBar != null)
                    MainActivity.mHeaderProgressBar.setVisibility(View.GONE);

                // mHeaderProgressBar.setVisibility(View.GONE);
                if (Utility.isInternetOn(getActivity())) {

                    //code to refresh outbox
                    refreshCountInBackground();

                    //stop the refresh in refresCountInBackground() method in the view.post

                    /*//start handler for 10 secs.  <to stop refreshbar>
                    final Handler h = new Handler() {
                        @Override
                        public void handleMessage(Message message) {

                            outboxRefreshLayout.setRefreshing(false);
                        }
                    };
                    h.sendMessageDelayed(new Message(), 10000);*/
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

    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //validating option menu
        setHasOptionsMenu(true);
    }


    /**
     * Holder class to hold all elements of an item
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView timestampmsg;
        TextView classimage;
        ImageView imgmsgview;
        ProgressBar uploadprogressbar;
        TextView msgtxtcontent;
        TextView classname;
        TextView likes;
        TextView confused;
        TextView seen;

        //constructor
        public ViewHolder(View row) {
            super(row);

            classname = (TextView) row.findViewById(R.id.classname1);
            classimage = (TextView) row.findViewById(R.id.classimage1);
            timestampmsg = (TextView) row.findViewById(R.id.cctimestamp);
            imgmsgview = (ImageView) row.findViewById(R.id.ccimgmsg);
            uploadprogressbar = (ProgressBar) row.findViewById(R.id.msgprogressbar);
            msgtxtcontent = (TextView) row.findViewById(R.id.ccmsgtext);
            likes = (TextView) row.findViewById(R.id.like);
            confused = (TextView) row.findViewById(R.id.confusion);
            seen = (TextView) row.findViewById(R.id.seen);
        }
    }


    /**
     * Adapter for recycleview of outbox
     */
    public class RecycleAdapter extends RecyclerView.Adapter<ViewHolder> {


        @Override
        public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int position) {

            View row = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.outbox_item, viewGroup, false);
            ViewHolder holder = new ViewHolder(row);
            return holder;
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            ParseObject groupdetails1 = groupDetails.get(position);

            if (groupdetails1 == null) return;

            //setting message in view
            String msg = groupdetails1.getString("title");
            if(msg == null || msg.trim().equals(""));
            else
                holder.msgtxtcontent.setVisibility(View.VISIBLE);
            holder.msgtxtcontent.setText(msg);

            String className= null;
            //setting class name
            if(groupdetails1.getString("name") != null) {
                holder.classname.setText(groupdetails1.getString("name"));
                className = groupdetails1.getString("name");
            }
            else
            {
                //previous version support < in the version from now onwards storing class name also>
                String groupCode = groupdetails1.getString("code");


                //Retrieving from shared preferences to access fast
                className =session.getClassName(groupCode);
                holder.classname.setText(className);


            }

            holder.likes.setText("" + Utility.nonNegative(groupdetails1.getInt(Constants.LIKE_COUNT)));
            holder.confused.setText("" + Utility.nonNegative(groupdetails1.getInt(Constants.CONFUSED_COUNT)));
            holder.seen.setText("seen by " + Utility.nonNegative(groupdetails1.getInt(Constants.SEEN_COUNT)));

            //setting background color of circular image
            GradientDrawable gradientdrawable = (GradientDrawable) holder.classimage.getBackground();
            gradientdrawable.setColor(Color.parseColor(Utility.classColourCode(className.toUpperCase())));
            holder.classimage.setText(className.substring(0, 1).toUpperCase());    //setting front end of circular image
            /*
            Retrieving timestamp
             */
            String timestampmsg = null;
            try {
                Date cdate = groupdetails1.getCreatedAt();

                if (cdate == null)
                    cdate = (Date) groupdetails1.get("creationTime");

                //finding difference of current & createdAt timestamp
                timestampmsg = Utility.convertTimeStamp(cdate);

                //setting timestamp in view
                holder.timestampmsg.setText(timestampmsg);
            } catch (java.text.ParseException e) {
            }

            /*
            Retrieving image attachment if exist
             */
            final String imagepath;
            if (groupdetails1.containsKey("attachment_name"))
                imagepath = groupdetails1.getString("attachment_name");
            else
                imagepath = "";

            holder.uploadprogressbar.setVisibility(View.GONE);

            //If image attachment exist, display image
            if (!UtilString.isBlank(imagepath)) {
                holder.imgmsgview.setVisibility(View.VISIBLE);

                holder.uploadprogressbar.setTag("Progress");
                File imgFile = new File(Utility.getWorkingAppDir() + "/media/" + imagepath);
                final File thumbnailFile = new File(Utility.getWorkingAppDir() + "/thumbnail/" + imagepath);
                if (imgFile.exists() && !thumbnailFile.exists())
                    Utility.createThumbnail(getActivity(), imagepath);
                if (imgFile.exists()) {
                    // if image file present locally
                    Bitmap myBitmap = BitmapFactory.decodeFile(thumbnailFile.getAbsolutePath());
                    holder.imgmsgview.setTag(imgFile.getAbsolutePath());
                    holder.imgmsgview.setImageBitmap(myBitmap);
                } else {
                    // else we Have to download image from server
                    ParseFile imagefile = (ParseFile) groupdetails1.get("attachment");
                    holder.uploadprogressbar.setVisibility(View.VISIBLE);
                    imagefile.getDataInBackground(new GetDataCallback() {
                        public void done(byte[] data, ParseException e) {
                            if (e == null) {
                                // ////Image download successful
                                FileOutputStream fos;
                                try {
                                    //store image
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

                                Utility.createThumbnail(myActivity, imagepath);
                                Bitmap mynewBitmap = BitmapFactory.decodeFile(thumbnailFile.getAbsolutePath());
                                holder.imgmsgview.setImageBitmap(mynewBitmap);
                                holder.uploadprogressbar.setVisibility(View.GONE);
                                // Might be a problem when net is too slow :/
                            } else {
                                // Image not downloaded
                                holder.uploadprogressbar.setVisibility(View.GONE);
                            }
                        }
                    });

                    holder.imgmsgview.setTag(Utility.getWorkingAppDir() + "/media/" + imagepath);
                    holder.imgmsgview.setImageBitmap(null);

                    // imgmsgview.setVisibility(View.GONE);


                }

                holder.imgmsgview.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                            Intent imgintent = new Intent();
                            imgintent.setAction(Intent.ACTION_VIEW);
                            imgintent.setDataAndType(Uri.parse("file://" + (String) holder.imgmsgview.getTag()), "image/*");
                            startActivity(imgintent);
                    }
                });


            } else {
                holder.imgmsgview.setVisibility(View.GONE);
            }
        }


        @Override
        public int getItemCount() {
            return groupDetails.size();
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            //on refresh option selected from options menu
            case R.id.refresh:
                if (Utility.isInternetOn(getActivity())) {

                    /*if (outboxRefreshLayout != null) {
                        runSwipeRefreshLayout(outboxRefreshLayout, 10);
                    }*/
                    outboxRefreshLayout.setRefreshing(true);
                   //update outbox in background
                    refreshCountInBackground();
                    //stop refreshing in above method inside view.post

                } else {
                    Utility.toast("Check your Internet connection");
                }
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    //Refresh the layout. For e.g if outbox messages have changed
    public static void refreshSelf(){
        if (Outbox.outboxRefreshLayout != null){
            Outbox.outboxRefreshLayout.post(new Runnable() {
                @Override
                public void run() {
                    Log.d("DEBUG_AFTER_OUTBOX_COUNT_REFRESH", "Updating outbox messages");
                    outboxRefreshLayout.setRefreshing(false);
                    if (groupDetails == null || groupDetails.size() == 0) {
                        outboxLayout.setVisibility(View.VISIBLE);
                    } else {
                        outboxLayout.setVisibility(View.GONE);
                    }

                    if(Outbox.myadapter != null){
                        Outbox.myadapter.notifyDataSetChanged();
                    }
                }
            });
        }
    }

    public static void refreshCountCore(){
        Log.d("DEBUG_OUTBOX", "running fetchLikeConfusedCountOutbox");
        SyncMessageDetails.fetchLikeConfusedCountOutbox();
        //following is the onpostexecute thing
        refreshSelf();
    }

    //update like/confused/seen count for sent messages in a background thread
    public static void refreshCountInBackground(){
        Runnable r = new Runnable() {
            @Override
            public void run(){
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
        if (MainActivity.mHeaderProgressBar != null)
            MainActivity.mHeaderProgressBar.setVisibility(View.GONE);

        //start handler for 10 secs.  <to stop refreshbar>
        final Handler h = new Handler() {
            @Override
            public void handleMessage(Message message) {

                outboxRefreshLayout.setRefreshing(false);
            }
        };
        h.sendMessageDelayed(new Message(), seconds * 1000);
    }

    public static void updateOutboxTotalMessages(){

        Log.d("DEBUG_OUTBOX_UPDATE_TOTAL_COUNT", "updating total outbox count");

        //update totalOutboxMessages
        ParseUser user = ParseUser.getCurrentUser();

        if (user == null)
            {Utility.logout(); return;}

        ParseQuery<ParseObject> query = ParseQuery.getQuery("SentMessages");
        query.fromLocalDatastore();
        query.whereEqualTo("userId", user.getUsername());
        try{
            totalOutboxMessages = query.count();
        }
        catch(ParseException e){
            e.printStackTrace();
        }
        Log.d("DEBUG_OUTBOX_UPDATE_TOTAL_COUNT", "count is " + totalOutboxMessages);
    }


}
