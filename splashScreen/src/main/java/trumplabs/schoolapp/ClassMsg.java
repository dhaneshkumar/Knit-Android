package trumplabs.schoolapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.parse.DeleteCallback;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.GetDataCallback;
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
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import library.UtilString;
import trumplab.textslate.R;
import trumplabs.schoolapp.ChooserDialog.CommunicatorInterface;
import utility.Config;
import utility.Queries;
import utility.SessionManager;
import utility.Utility;

public class ClassMsg extends Fragment implements CommunicatorInterface {
    ListView listv;
    protected LayoutInflater layoutinflater;
    myBaseAdapter myadapter;
    int ACTION_MODE_NO;
    ArrayList<ParseObject> selectedlistitems; // To delete selected messages
    public static String groupCode = ClassContainer.classuid;
    List<ParseObject> groupDetails; // List of group messages
    public static String grpName = ClassContainer.className;
    String sender, userId;
    Queries query;
    boolean localCheck = false;
    int msgCount = 0;
    boolean extra = true;
    int prevCount = 0;
    String typedtxt;
    Date timeStamp;
    Activity myActivity;
    TextView countview;
    EditText typedmsg;
    public static LinearLayout sendimgpreview;
    ImageView sendimgview;
    ProgressBar updProgressBar;
    ImageView attachView;
    public static LinearLayout progressLayout;
    boolean createMsgFlag; // A flag to stop continuous request coming from create msgs in background on scrolling
    SessionManager session;
    boolean overflow;  // A flag to stop continuous request coming from create msgs on scrolling
    public static int extraMessages; //extra retrieved msgs on scrolling
    // Handler handler = new Handler();;
    public static int totalClassMessages; //total messages sent from this class

    public ClassMsg() {
        groupCode = ClassContainer.classuid;
        grpName = ClassContainer.className;
        updateTotalClassMessages();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        layoutinflater = inflater;
        getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

        View layoutview = inflater.inflate(R.layout.ccmsging_layout, container, false);
        return layoutview;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        selectedlistitems = new ArrayList<ParseObject>();
        myadapter = new myBaseAdapter();
        ACTION_MODE_NO = 0;
        query = new Queries();
        listv = (ListView) getActivity().findViewById(R.id.classmsglistview);
        listv.setStackFromBottom(true);
        progressLayout = (LinearLayout) getActivity().findViewById(R.id.progressBarLayout);
        session = new SessionManager(Application.getAppContext());
        overflow = false;

        ParseUser userObject = ParseUser.getCurrentUser();
        myActivity = getActivity();
        if (userObject == null)
            Utility.logout();

        sender = userObject.getString(Constants.NAME);
        userId = userObject.getUsername();

        // retrieving top messages from local database

        localCheck = false;
        // GetDataFromLocalDatabase gf = new GetDataFromLocalDatabase();
        // gf.execute();
        try {
            groupDetails = query.getLocalCreateMsgs(ClassContainer.classuid, groupDetails, false);
        } catch (ParseException e) {
        }

        if (groupDetails == null)
            groupDetails = new ArrayList<ParseObject>();


        if (groupDetails.size() < Config.createMsgCount) {
            GetDataFromServer gf = new GetDataFromServer();
            gf.execute();
        }

        // if(groupDetails.size())

        sendMsgMethod();
        initialiseListViewMethods();
        ((ActionBarActivity) getActivity()).getSupportActionBar().setTitle(grpName);

        listv.setAdapter(myadapter);
        super.onActivityCreated(savedInstanceState);
    }



    private class GetDataFromServer extends AsyncTask<Void, Void, String[]> {
        String[] mStrings;

        @Override
        protected String[] doInBackground(Void... params) {


      /*
       * Checking flag to stop continuous server query from scrolling create msgs page
       */
            if (createMsgFlag)
                return mStrings;

            createMsgFlag = true;
            Utility.ls("background thread started, sent messages");

            try {
                groupDetails = query.getServerCreateMsgs(ClassContainer.classuid, groupDetails, false);
            } catch (ParseException e) {
            }

            createMsgFlag = false;
            return mStrings;
        }

        @Override
        protected void onPostExecute(String[] result) {

            if (! createMsgFlag)
                myadapter.notifyDataSetChanged();// needs to change

            super.onPostExecute(result);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // handler.removeCallbacksAndMessages(null); // Remove all callbacks
        // attached to views
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
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
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

    /*
     * case R.id.attachfile: FragmentManager fm = getActivity().getSupportFragmentManager();
     * ChooserDialog openchooser = new ChooserDialog(); openchooser.setTargetFragment(this, 500);
     * openchooser.show(fm, "Chooser Dialog"); break;
     */

            case R.id.deletebin:
                menuDeleteBinMethod();
                break;
            case R.id.copyicon:
                String txtcont = selectedlistitems.get(0).getString("title");
                Utility.copyToClipBoard(getActivity(), "label", txtcont);
                ACTION_MODE_NO = 0;
                ((ActionBarActivity) getActivity()).supportInvalidateOptionsMenu();
                int index = groupDetails.indexOf(selectedlistitems.get(0));
                myadapter.notifyDataSetChanged();
                // listv.setSelection(index);
                selectedlistitems.clear();
                Utility.toast("Message copied");
                break;
            case R.id.shareicon:
                String sharemsg = selectedlistitems.get(0).getString("title");
                ACTION_MODE_NO = 0;
                ((ActionBarActivity) getActivity()).supportInvalidateOptionsMenu();
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
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setMessage("Delete Class? Are you sure?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                if (!Utility.isInternetOn(getActivity())) {
                                    Utility.toast("No internet!! Can't delete!");
                                    return;
                                }
                                sendTxtMsgtoSubscribers("Your Class " + grpName
                                        + " has been deleted by the Creator " + sender);

                                List<String> group = new ArrayList<String>();
                                group.add(groupCode);
                                group.add(grpName);

                                ParseUser user = ParseUser.getCurrentUser();

                                if (user == null)
                                    Utility.logout();


                                user.put(Constants.JOINED_GROUPS, Utility.removeItemFromJoinedGroups(user, group));
                                ParseUser.getCurrentUser().saveEventually();

                                ParseQuery<ParseObject> delquery1 = new ParseQuery<ParseObject>("Codegroup");
                                delquery1.whereEqualTo("code", groupCode);
                                delquery1.getFirstInBackground(new GetCallback<ParseObject>() {
                                    public void done(ParseObject object, ParseException e) {
                                        if (object == null) {
                                        } else {

                                            object.put("classExist", false);
                                            object.saveInBackground();
                                        }
                                    }
                                });

                                ParseQuery<ParseObject> delquery11 = new ParseQuery<ParseObject>("Codegroup");
                                delquery11.whereEqualTo("code", groupCode);
                                delquery11.fromLocalDatastore();
                                try {
                                    ParseObject.unpinAll(delquery11.find());
                                } catch (ParseException e1) {
                                    e1.printStackTrace();
                                }

                                ParseQuery<ParseObject> delquery22 = new ParseQuery<ParseObject>("GroupDetails");
                                delquery22.whereEqualTo("code", groupCode);
                                delquery22.whereEqualTo("name", grpName);
                                delquery22.fromLocalDatastore();
                                try {
                                    ParseObject.unpinAll(delquery22.find());
                                } catch (ParseException e1) {
                                    e1.printStackTrace();
                                }

                                ParseQuery<ParseObject> delquery3 = new ParseQuery<ParseObject>("GroupMembers");
                                delquery3.whereEqualTo("code", groupCode);
                                delquery3.findInBackground(new FindCallback<ParseObject>() {

                                    @Override
                                    public void done(List<ParseObject> objects, ParseException e) {
                                        if (e == null) {
                                            ParseObject.deleteAllInBackground(objects, new DeleteCallback() {

                                                @Override
                                                public void done(ParseException e) {
                                                    if (e == null)
                                                        Log.d("textslate", "Query3 success!!");
                                                }
                                            });
                                        }
                                    }
                                });

                                ParseQuery<ParseObject> delquery33 = new ParseQuery<ParseObject>("GroupMembers");
                                delquery33.whereEqualTo("code", groupCode);
                                delquery33.fromLocalDatastore();
                                try {
                                    ParseObject.unpinAll(delquery33.find());
                                    Log.d("textslate", "Local Query3 success!!");
                                } catch (ParseException e1) {
                                    e1.printStackTrace();
                                }

                                ParseUser userObject = ParseUser.getCurrentUser();
                                if (userObject == null)
                                    Utility.logout();

                                List<String> item = new ArrayList<String>();
                                item.add(groupCode);
                                item.add(grpName);

                                userObject.getList(Constants.CREATED_GROUPS).remove(item);
                                userObject.saveEventually();
                                Classrooms.createdGroups.remove(item);
                                getActivity().finish();
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
                row = layoutinflater.inflate(R.layout.ccmsging_msgview, parent, false);
            } else {
            }

            final ParseObject groupdetails1 = groupDetails.get(position);
            String stringmsg = (String) getItem(position);
            String timestampmsg = null;
            try {
                Date cdate = groupdetails1.getCreatedAt();

                if (cdate == null)
                    cdate = (Date) groupdetails1.get("creationTime");

                timestampmsg = Utility.convertTimeStamp(cdate);
            } catch (java.text.ParseException e) {
            }

            final String imagepath;// groupDetails.get(position).getString("attachment_name");
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


            likeCountArea.setText("" + Utility.nonNegative(groupdetails1.getInt(Constants.LIKE_COUNT)));
            confusedCountArea.setText("" + Utility.nonNegative(groupdetails1.getInt(Constants.CONFUSED_COUNT)));
            seenCountArea.setText("seen by " + Utility.nonNegative(groupdetails1.getInt(Constants.SEEN_COUNT)));

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
                imgmsgview.setVisibility(View.VISIBLE);
                uploadprogressbar.setTag("Progress");
                File imgFile = new File(Utility.getWorkingAppDir() + "/media/" + imagepath);
                final File thumbnailFile = new File(Utility.getWorkingAppDir() + "/thumbnail/" + imagepath);
                if (imgFile.exists() && !thumbnailFile.exists())
                    Utility.createThumbnail(getActivity(), imagepath);
                if (imgFile.exists()) {
                    // image file present locally
                    Bitmap myBitmap = BitmapFactory.decodeFile(thumbnailFile.getAbsolutePath());
                    msgtxtcontent.setText(stringmsg);
                    imgmsgview.setTag(imgFile.getAbsolutePath());
                    imgmsgview.setImageBitmap(myBitmap);
                    timestampview.setText(timestampmsg);
                } else {
                    // Have to download image from server
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
                                Utility.createThumbnail(myActivity, imagepath);
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


        listv.setOnScrollListener(new OnScrollListener() {

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
                                 int totalItemCount) {
                int lastInScreen = firstVisibleItem + visibleItemCount;

                int lastCount = groupDetails.size();



                Utility.ls(totalItemCount - firstVisibleItem+ " : first visible item");
                if (firstVisibleItem < 2) {
                    if(lastCount >= totalClassMessages){
                        Log.d("DEBUG_CLASS_MSG_ONSCROLL", "All loaded, no need to load more. Saving unnecessary query");
                        return;
                    }

                    try {


                        Utility.ls(lastCount + " : groupDetails size");

                        if (lastCount >= Config.createMsgCount) {

                            if(totalItemCount - firstVisibleItem >= lastCount) {

                                Utility.ls(groupDetails.size() + " : old groupDetails size");
                                groupDetails = query.getLocalCreateMsgs(ClassContainer.classuid, groupDetails, true);


                                Utility.ls(groupDetails.size() + " : new groupDetails size");

                                 myadapter.notifyDataSetChanged();

                                //commenting this, as we are fetching outbox messages from server on first launch of app
                                //and thereafter we won't need to do this

                                /*if (lastCount == groupDetails.size()) {
                                    GetDataFromServer gf = new GetDataFromServer();
                                    gf.execute();
                                }*/
                            }
                        }

                    } catch (ParseException e) {

                    }
                } else {

                }

            }
        });


        listv.setOnItemClickListener(new OnItemClickListener() {
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
                        ((ActionBarActivity) myActivity).supportInvalidateOptionsMenu();
                    } else if (selectedlistitems.size() == 1 || selectedlistitems.size() == 2) {
                        ((ActionBarActivity) myActivity).supportInvalidateOptionsMenu();
                    }
                } else if (ACTION_MODE_NO == 0) {
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

    // delete the messages
    public void menuDeleteBinMethod() {
        ACTION_MODE_NO = 0;

        if (myActivity != null) {
            ((ActionBarActivity) myActivity).supportInvalidateOptionsMenu();
            Iterator<ParseObject> it = selectedlistitems.iterator();
            Integer count = 0;
            ParseQuery<ParseObject> query4 = ParseQuery.getQuery("GroupDetails");
            query4.whereEqualTo("code", groupCode);
            while (it.hasNext()) {
                final ParseObject object = (ParseObject) it.next();
                object.unpinInBackground();
                object.deleteInBackground(new DeleteCallback() {

                    @Override
                    public void done(ParseException e) {
                        object.deleteEventually();
                    }
                });
                groupDetails.remove(object);
                count += 1;
            }
            Utility.toast("Deleted " + count + " messages");
            selectedlistitems.clear();
            myadapter.notifyDataSetChanged();
        }
    }

    private void sendMsgMethod() {
        // Initializing all the views related to sending message
        final ImageButton sendmsgbutton = (ImageButton) getActivity().findViewById(R.id.sendmsgbttn);
        typedmsg = (EditText) getActivity().findViewById(R.id.typedmsg);
        countview = (TextView) getActivity().findViewById(R.id.lettercount);
        sendimgpreview = (LinearLayout) getActivity().findViewById(R.id.imgpreview);
        sendimgview = (ImageView) getActivity().findViewById(R.id.attachedimg);
        Button viewbutton = (Button) getActivity().findViewById(R.id.viewbutton);
        Button removebutton = (Button) getActivity().findViewById(R.id.removebutton);
        updProgressBar = (ProgressBar) getActivity().findViewById(R.id.updprogressbar);
        updProgressBar.setVisibility(View.GONE);
        attachView = (ImageView) getActivity().findViewById(R.id.gallery);
        final ImageButton templetsList = (ImageButton) getActivity().findViewById(R.id.templets);

        typedmsg.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View arg0, MotionEvent arg1) {
                scrollMyListViewToBottom();
                return false;
            }
        });

        typedmsg.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

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
                    sendmsgbutton.setImageResource(R.drawable.ic_action_send1);
          /*
           * sendmsgbutton.setBackgroundDrawable(getResources()
           * .getDrawable(R.drawable.ic_action_send1));
           */
                } else
                    sendmsgbutton.setImageResource(R.drawable.ic_action_send);
            }

        });


        /*
        Current model is MI then hide attachview option
         */

        if (android.os.Build.MODEL != null)
        {
            String[] models = new String[]{"MI 3W", "MI 3", "MI 3S", "MI 3SW", "MI 4", "MI 4W",
                    "HM 1SW", "MI 1S", "MI 1SW", "MI 2", "MI 2W", "MI 2S", "MI 2SW", "MI 2A", "MI 2AW"};

            if (Arrays.asList(models).contains(android.os.Build.MODEL.trim()))
                attachView.setVisibility(View.GONE);
            else
                Utility.ls(android.os.Build.MODEL);
        }

        attachView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                FragmentManager fm = getActivity().getSupportFragmentManager();
                ChooserDialog openchooser = new ChooserDialog();
                openchooser.setTargetFragment(ClassMsg.this, 500);
                openchooser.show(fm, "Chooser Dialog");
            }
        });


        templetsList.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                final CharSequence[] items =
                        {"These are assignment for today", "Help your children in preparing for exams ",
                                "Parent-teacher meet tomorrow @9:30AM", "Exams will start from 27 Dec.",};

                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle("Suggestions");
        /*
         * LayoutInflater inflater = LayoutInflater.from(Application.getAppContext()); View
         * alertView = inflater.inflate(R.layout.template_msgs, null); builder.setView(alertView);
         */


                builder.setItems(items, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        // Utility.toast(items[item].toString());
                        typedmsg.setText(items[item].toString());
                    }
                });
        /*
         * 
         * builder.setPositiveButton("Create new template", new DialogInterface.OnClickListener() {
         * public void onClick(DialogInterface dialog, int id) { // do things
         * 
         * Utility.toast("ok clicked");
         * 
         * 
         * Create a new alert dialog..
         * 
         * 
         * 
         * } });
         */

                AlertDialog alert = builder.create();
                alert.show();

        /*
         * Button bq = alert.getButton(DialogInterface.BUTTON_POSITIVE); if (bq != null) {
         * bq.setBackgroundColor(getResources().getColor(R.color.buttoncolor));
         * bq.setTextColor(getResources().getColor(R.color.white)); bq.setTextSize(18);
         * 
         * }
         */
            }
        });

        sendmsgbutton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                scrollMyListViewToBottom();
                typedtxt = typedmsg.getText().toString().trim();
                if (!Utility.isInternetOn(getActivity())) {
                    Utility.toast("No internet Connection!");
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

        groupDetails1.saveInBackground(new SaveCallback() {

            @Override
            public void done(ParseException e) {
                if (e == null) {
                    // message was sent successfully
                    updProgressBar.setVisibility(View.GONE);

          /*
           * storing locally
           */
                    ParseObject sentMsg = new ParseObject("SentMessages");
                    sentMsg.put("objectId", groupDetails1.getObjectId());
                    sentMsg.put("Creator", groupDetails1.getString("Creator"));
                    sentMsg.put("code", groupDetails1.getString("code"));
                    sentMsg.put("title", groupDetails1.getString("title"));
                    sentMsg.put("name", groupDetails1.getString("name"));
                    sentMsg.put("creationTime", groupDetails1.getCreatedAt());
                    sentMsg.put("senderId", groupDetails1.getString("senderId"));
                    sentMsg.put("userId", userId);

                    if (groupDetails1.get("attachment") != null)
                        sentMsg.put("attachment", groupDetails1.get("attachment"));
                    if (groupDetails1.getString("attachment_name") != null)
                        sentMsg.put("attachment_name", groupDetails1.getString("attachment_name"));
                    if (groupDetails1.get("senderpic") != null)
                        sentMsg.put("senderpic", groupDetails1.get("senderpic"));

                    try {
                        sentMsg.pin();

                    } catch (ParseException e2) {
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


                    SessionManager sm = new SessionManager(Application.getAppContext());

                    if (groupDetails1.getUpdatedAt() != null)
                        sm.setCurrentTime(groupDetails1.getUpdatedAt());

                    // pushing notification to this group
                    ClassMsgFunctions.sendMessageAsData(groupCode, typedtxt, 0, sender, grpName);
                    Utility.toast("Notification Sent");

                } else {
                    // message was not sent
                    groupDetails.remove(groupDetails1);
                    myadapter.notifyDataSetChanged();
                    Utility.toast("Message wasn't sent! Check Internet!");
                    updProgressBar.setVisibility(View.GONE);
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

        // Utility.toast(Utility.getWorkingAppDir() + "/thumbnail/" + imgname);
        // The thumbnail is already created
        Bitmap myBitmap = BitmapFactory.decodeFile(thumbnailFile.getAbsolutePath());
        sendimgview.setImageBitmap(myBitmap);
    }

    // Send Image Pic
    private void sendPic(String filepath, String txtmsg) throws IOException {

        // /Creating ParseFile (Not yet uploaded)
        int slashindex = ((String) sendimgpreview.getTag()).lastIndexOf("/");
        String fileName = ((String) sendimgpreview.getTag()).substring(slashindex + 1);// image file //
        // name

        RandomAccessFile f = new RandomAccessFile(filepath, "r");
        byte[] data = new byte[(int) f.length()];
        f.read(data);
        final ParseFile file = new ParseFile(fileName, data);

        // /saving the sent image message details on App//////////////////////
        final ParseObject groupDetails1 = new ParseObject("GroupDetails");

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
                    updProgressBar.setVisibility(View.GONE);
                    // Utility.toast("uploading completed");

                    // //sending the message details to server since file is uploaded
                    int index = groupDetails.indexOf(groupDetails1);
                    groupDetails1.put("attachment", file);// updating the

                    if (index >= 0)
                        groupDetails.set(index, groupDetails1);// replacing with the

                    groupDetails1.saveInBackground(new SaveCallback() {
                        @Override
                        public void done(ParseException e) {
                            if (e == null) {
                                Utility.toast("Notification Sent");



                /*
                 * Sending notification and storing locally
                 */
                                ClassMsgFunctions.sendMessageAsData(groupCode, groupDetails1.getString("title"), 0,
                                        sender, grpName);

                                // Removing old object and replacing it with new
                                groupDetails.remove(groupDetails1);


                /*
                 * storing locally
                 */
                                ParseObject sentMsg = new ParseObject("SentMessages");
                                sentMsg.put("objectId", groupDetails1.getObjectId());
                                sentMsg.put("Creator", groupDetails1.getString("Creator"));
                                sentMsg.put("code", groupDetails1.getString("code"));
                                sentMsg.put("title", groupDetails1.getString("title"));
                                sentMsg.put("name", groupDetails1.getString("name"));
                                sentMsg.put("creationTime", groupDetails1.getCreatedAt());
                                sentMsg.put("senderId", groupDetails1.getString("senderId"));
                                sentMsg.put("userId", userId);

                                if (groupDetails1.get("attachment") != null)
                                    sentMsg.put("attachment", groupDetails1.get("attachment"));
                                if (groupDetails1.getString("attachment_name") != null)
                                    sentMsg.put("attachment_name", groupDetails1.getString("attachment_name"));
                                if (groupDetails1.get("senderpic") != null)
                                    sentMsg.put("senderpic", groupDetails1.get("senderpic"));

                                try {
                                    sentMsg.pin();
                                    groupDetails.add(groupDetails1);
                                    myadapter.notifyDataSetChanged();
                                } catch (ParseException e2) {
                                }

                                //update outbox message count
                                Outbox.updateOutboxTotalMessages();

                /*
                 * Updating time
                 */
                                SessionManager sm = new SessionManager(Application.getAppContext());
                                if (groupDetails1.getUpdatedAt() != null)
                                    sm.setCurrentTime(groupDetails1.getUpdatedAt());

                            }
                        }
                    });
                } else {
                    updProgressBar.setVisibility(View.GONE);
                    Utility.toast("uploading failed");
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

    public static void updateTotalClassMessages(){

        Log.d("DEBUG_CLASS_MSG_UPDATE_TOTAL_COUNT", "updating total outbox count");

        //update ClassMsg.totalClassMessages
        ParseUser user = ParseUser.getCurrentUser();

        if (user == null)
            Utility.logout();

        ParseQuery<ParseObject> query = ParseQuery.getQuery("SentMessages");
        query.fromLocalDatastore();
        query.whereEqualTo("userId", user.getUsername());
        query.whereEqualTo("code", groupCode);
        try{
            totalClassMessages = query.count();
        }
        catch(ParseException e){
            e.printStackTrace();
        }
        Log.d("DEBUG_CLASS_MSG_UPDATE_TOTAL_COUNT", "count is " + totalClassMessages);
    }

}
