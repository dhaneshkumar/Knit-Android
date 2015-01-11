package trumplabs.schoolapp;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import BackGroundProcesses.CreatedClassRooms;
import library.UtilString;
import trumplab.textslate.R;
import utility.Queries;
import utility.SessionManager;
import utility.Tools;
import utility.Utility;

/**
 * Outbox Activity showing Outbox fragment of homepage
 */
public class Outbox extends Fragment {
    protected LayoutInflater layoutinflater;
    RecycleAdapter myadapter;
    RecyclerView listv;
    Queries query;
    List<ParseObject> groupDetails; // List of group messages
    Activity myActivity;
    private LinearLayoutManager mLayoutManager;
    SessionManager session;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        layoutinflater = inflater;
        View layoutview = inflater.inflate(R.layout.outbox, container, false);
        return layoutview;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        //intializing variables
        listv = (RecyclerView) getActivity().findViewById(R.id.classmsglistview);
        myActivity = getActivity();
        session = new SessionManager(Application.getAppContext());
        query = new Queries();

        //retrieving lcoally stored outbox messges
        groupDetails = query.getLocalOutbox();
        if (groupDetails == null)
            groupDetails = new ArrayList<ParseObject>();

        //setting recycle view & layout
        mLayoutManager = new LinearLayoutManager(getActivity());
        listv.setLayoutManager(mLayoutManager);
        myadapter = new RecycleAdapter();
        listv.setAdapter(myadapter);

        super.onActivityCreated(savedInstanceState);
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
            holder.msgtxtcontent.setText(groupdetails1.getString("title"));

            //setting class name
            if(groupdetails1.getString("name") != null)
                holder.classname.setText(groupdetails1.getString("name"));
            else
            {
                //previous version support < in the version from now onwards storing class name also>
                String groupCode = groupdetails1.getString("code");

                //Retrieving from shared preferences to access fast
                holder.classname.setText(session.getClassName(groupCode));

            }

            holder.likes.setText("" + Utility.nonNegative(groupdetails1.getInt(Constants.LIKE_COUNT)));
            holder.confused.setText("" + Utility.nonNegative(groupdetails1.getInt(Constants.CONFUSED_COUNT)));
            holder.seen.setText("seen by " + Utility.nonNegative(groupdetails1.getInt(Constants.SEEN_COUNT)));

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

                    //show progress bar
                    if (MainActivity.mHeaderProgressBar != null) {
                        Tools.runSmoothProgressBar(MainActivity.mHeaderProgressBar, 10);
                    }

                   //update outbox in background


                } else {
                    Utility.toast("Check your Internet connection");
                }
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }


}
