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
import android.view.Gravity;
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
import java.util.Calendar;
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


/**
 * This class shows sent messages of a particular class and show option to send a new message.
 */
public class ClassMsg extends Fragment implements CommunicatorInterface {
    private ListView listv;                   //listview to show sent messages
    protected LayoutInflater layoutinflater;
    private myBaseAdapter myadapter;        //Adapter for listview
    private int ACTION_MODE_NO;
    private ArrayList<ParseObject> selectedlistitems; // To delete selected messages
    public static String groupCode = ClassContainer.classuid;       //class-code
    private List<ParseObject> groupDetails;     // List of group messages
    public static String grpName = ClassContainer.className;        //class-name
    private String sender, userId;
    private Queries query;
    private String typedtxt;        //message to sent
    private Activity myActivity;
    private TextView countview;
    private EditText typedmsg;
    public static LinearLayout sendimgpreview;
    private ImageView sendimgview;
    private ProgressBar updProgressBar;
    private ImageView attachView;
    public static LinearLayout progressLayout;
    private boolean createMsgFlag; // A flag to stop continuous request coming from create msgs in background on scrolling
    private SessionManager session;
    public static int totalClassMessages; //total messages sent from this class

    //calling constructor to refresh class-code and group name.
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


        listv = (ListView) getActivity().findViewById(R.id.classmsglistview);   //list view
        listv.setStackFromBottom(true);         //show message from bottom

        progressLayout = (LinearLayout) getActivity().findViewById(R.id.progressBarLayout);
        session = new SessionManager(Application.getAppContext());

        ParseUser userObject = ParseUser.getCurrentUser();
        myActivity = getActivity();

        //checking parse user null or not
        if (userObject == null)
            {Utility.logout(); return;}

        sender = userObject.getString(Constants.NAME);
        userId = userObject.getUsername();

        // retrieving sent messages of given class from local database
        try {
            groupDetails = query.getLocalCreateMsgs(ClassContainer.classuid, groupDetails, false);
        } catch (ParseException e) {
        }

        if (groupDetails == null)
            groupDetails = new ArrayList<ParseObject>();

        sendMsgMethod();
        initialiseListViewMethods();

        //setting action bar title as class name
        ((ActionBarActivity) getActivity()).getSupportActionBar().setTitle(grpName);

        //setting listview adapter
        listv.setAdapter(myadapter);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
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
                                    {Utility.logout(); return;}


                               // user.put(Constants.JOINED_GROUPS, Utility.removeItemFromJoinedGroups(user, group));
                              //  ParseUser.getCurrentUser().saveEventually();

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

                              /*  ParseQuery<ParseObject> delquery11 = new ParseQuery<ParseObject>("Codegroup");
                                delquery11.whereEqualTo("code", groupCode);
                                delquery11.fromLocalDatastore();
                                try {
                                    ParseObject.unpinAll(delquery11.find());
                                } catch (ParseException e1) {
                                    e1.printStackTrace();
                                }*/

                             /*   ParseQuery<ParseObject> delquery22 = new ParseQuery<ParseObject>("GroupDetails");
                                delquery22.whereEqualTo("code", groupCode);
                                delquery22.whereEqualTo("name", grpName);
                                delquery22.fromLocalDatastore();
                                try {
                                    ParseObject.unpinAll(delquery22.find());
                                } catch (ParseException e1) {
                                    e1.printStackTrace();
                                }
*/
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


                                /*
                                locally remove sent messages
                                 */

                                userObject.getList(Constants.CREATED_GROUPS).remove(group);
                                userObject.saveEventually();
                                Classrooms.createdGroups.remove(group);
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

    /*
    Setting adapter to show sent messages in list view
     */
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

            final ParseObject groupdetails1 = groupDetails.get(position);  //selected object
            String stringmsg = (String) getItem(position);      //selected messages

            //retrieving the message sent time
            String timestampmsg = null;
            try {
                Date  cdate = (Date) groupdetails1.get("creationTime");
                timestampmsg = Utility.convertTimeStamp(cdate);
            } catch (java.text.ParseException e) {
            }

            final String imagepath;
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

                /*
                Showoing the attached image
                 */
                imgmsgview.setVisibility(View.VISIBLE);
                uploadprogressbar.setTag("Progress");
                File imgFile = new File(Utility.getWorkingAppDir() + "/media/" + imagepath);
                final File thumbnailFile = new File(Utility.getWorkingAppDir() + "/thumbnail/" + imagepath);
                if (imgFile.exists() && !thumbnailFile.exists())
                    Utility.createThumbnail(getActivity(), imagepath);
                if (imgFile.exists()) {
                    // image file present locally then display it
                    Bitmap myBitmap = BitmapFactory.decodeFile(thumbnailFile.getAbsolutePath());
                    msgtxtcontent.setText(stringmsg);
                    imgmsgview.setTag(imgFile.getAbsolutePath());
                    imgmsgview.setImageBitmap(myBitmap);
                    timestampview.setText(timestampmsg);
                } else {
                    // else download image from server and then display it
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


        /*
        On scrolling list view, load more messages from local storage
         */
        listv.setOnScrollListener(new OnScrollListener() {

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
                                 int totalItemCount) {

                int lastCount = groupDetails.size();

                if (firstVisibleItem < 2) {
                    if(lastCount >= totalClassMessages){
                        return;
                    }

                    try {
                        if (lastCount >= Config.createMsgCount) {
                            if(totalItemCount - firstVisibleItem >= lastCount) {
                                groupDetails = query.getLocalCreateMsgs(ClassContainer.classuid, groupDetails, true);
                                 myadapter.notifyDataSetChanged();
                            }
                        }
                    } catch (ParseException e) {
                    }
                }
            }
        });


        /*
        Setting options on clicking a list view item
         */
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

    // delete the messages
    public void menuDeleteBinMethod() {
        ACTION_MODE_NO = 0;

        if (myActivity != null) {
            ((ActionBarActivity) myActivity).supportInvalidateOptionsMenu();
            Iterator<ParseObject> it = selectedlistitems.iterator();
            Integer count = 0;


            //write parse cloud function to delete selected msg, if needed
          /*  ParseQuery<ParseObject> query4 = ParseQuery.getQuery("GroupDetails");
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
            myadapter.notifyDataSetChanged();*/
        }
    }

    private void sendMsgMethod() {
        // Initializing all the views related to sending message view
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

        typedmsg.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View arg0, MotionEvent arg1) {
                scrollMyListViewToBottom(); //show messages from bottom
                return false;
            }
        });

        /*
        Change send button color on entering some text and update the entered text counter
         */
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
                    "HM 1SW", "MI 1S", "MI 1SW", "MI 2", "MI 2W", "MI 2S", "MI 2SW", "MI 2A", "MI 2AW", "HM 1S"};

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


        //setting send message button clicked functionality
        sendmsgbutton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                int hourOfDay = -1;
                if(session != null) {
                    //using local time instead of session.getCurrentTime
                    //Date now = session.getCurrentTime();
                    Calendar cal = Calendar.getInstance();
                    //cal.setTime(now);
                    hourOfDay = cal.get(Calendar.HOUR_OF_DAY);
                }
                Log.d("DEBUG_CLASS_MSG", "time hour of day " + hourOfDay);
                if(hourOfDay != -1){

                    //If current message time is not sutaible <9PM- 6AM> then show this warning as popup to users
                    if(hourOfDay >= Config.messageNormalEndTime || hourOfDay < Config.messageNormalStartTime){
                        //note >= and < respectively because disallowed are [ >= EndTime and < StartTime]
                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        LinearLayout warningView = new LinearLayout(getActivity());
                        warningView.setOrientation(LinearLayout.VERTICAL);
                        LinearLayout.LayoutParams nameParams =
                                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,
                                        LinearLayout.LayoutParams.WRAP_CONTENT);
                        nameParams.setMargins(30, 30, 30, 30);

                        final TextView nameInput = new TextView(getActivity());
                        nameInput.setTextSize(18);
                        nameInput.setText(Config.messageTimeWarning);
                        nameInput.setGravity(Gravity.CENTER_HORIZONTAL);
                        warningView.addView(nameInput, nameParams);
                        builder.setView(warningView);

                        builder.setPositiveButton("Send", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                sendFunction();
                            }
                        });
                        builder.setNegativeButton("Cancel", null);
                        AlertDialog dialog = builder.create();
                        dialog.show();
                    }
                    else{
                        sendFunction();
                    }
                }
                else{
                    sendFunction();
                }

            }

            /*
            Send messages to subscribers
             */
            public void sendFunction(){
                scrollMyListViewToBottom();  //show sent messages from bottom on clicking send button
                typedtxt = typedmsg.getText().toString().trim();  //message to send

                //check internet connection
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


                    //updating outbox
                    Queries outboxQuery = new Queries();

                    List<ParseObject> outboxItems = outboxQuery.getLocalOutbox();
                    if( outboxItems != null)
                    {
                        Outbox.groupDetails = outboxItems;

                        if( Outbox.myadapter != null)
                            Outbox.myadapter.notifyDataSetChanged();

                        if(Outbox.outboxLayout != null && Outbox.groupDetails.size()>0)
                            Outbox.outboxLayout.setVisibility(View.GONE);
                    }

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


                                //updating outbox adapter
                                Queries outboxQuery = new Queries();

                                List<ParseObject> outboxItems = outboxQuery.getLocalOutbox();
                                if( outboxItems != null)
                                {
                                    Outbox.groupDetails = outboxItems;

                                    if( Outbox.myadapter != null)
                                        Outbox.myadapter.notifyDataSetChanged();

                                    if(Outbox.outboxLayout != null && Outbox.groupDetails.size()>0)
                                        Outbox.outboxLayout.setVisibility(View.GONE);
                                }
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

    //Updating total sent messages count of this class
    public static void updateTotalClassMessages(){

        Log.d("DEBUG_CLASS_MSG_UPDATE_TOTAL_COUNT", "updating total outbox count");

        //update ClassMsg.totalClassMessages
        ParseUser user = ParseUser.getCurrentUser();

        if (user == null)
            {Utility.logout(); return;}

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
