package trumplabs.schoolapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;

import baseclasses.MyActionBarActivity;
import trumplab.textslate.R;
import utility.Queries;
import utility.SessionManager;
import utility.Utility;

/**
 * Send message to class and also show sent messages
 */
public class SendMessage extends MyActionBarActivity{
    private ListView listv;                   //listview to show sent messages
    protected LayoutInflater layoutinflater;
 //   private myBaseAdapter myadapter;        //Adapter for listview
    private int ACTION_MODE_NO;
    private ArrayList<ParseObject> selectedlistitems; // To delete selected messages
    public static String groupCode;      //class-code
    private List<ParseObject> groupDetails;     // List of group messages
    public static String grpName;        //class-name
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
    public static LinearLayout contentLayout;
    public static Activity currentActivity;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ccmsging_layout);

        groupCode = getIntent().getExtras().getString("classCode");
        grpName = getIntent().getExtras().getString("className");

        selectedlistitems = new ArrayList<ParseObject>();
     //   myadapter = new myBaseAdapter();
        ACTION_MODE_NO = 0;
        query = new Queries();

        listv = (ListView) findViewById(R.id.classmsglistview);   //list view
        listv.setStackFromBottom(true);         //show message from bottom

        contentLayout = (LinearLayout) findViewById(R.id.contentLayout);
        progressLayout = (LinearLayout) findViewById(R.id.progresslayout);

        session = new SessionManager(Application.getAppContext());

        ParseUser userObject = ParseUser.getCurrentUser();
        //checking parse user null or not
        if (userObject == null)
        {
            Utility.logout(); return;}

        sender = userObject.getString(Constants.NAME);
        userId = userObject.getUsername();

        // retrieving sent messages of given class from local database
        try {
            groupDetails = query.getLocalCreateMsgs(ClassContainer.classuid, groupDetails, false);
        } catch (ParseException e) {
        }

        if (groupDetails == null)
            groupDetails = new ArrayList<ParseObject>();

  //      sendMsgMethod();
  //      initialiseListViewMethods();

        //setting action bar title as class name
        //((ActionBarActivity) this).getSupportActionBar().setTitle(grpName);

        //setting listview adapter
       // listv.setAdapter(myadapter);



        /*
        Setting custom view in action bar
         */
        ActionBar actionBar = getSupportActionBar();

        //   actionBar.setTitle("science");
        actionBar.setDisplayShowTitleEnabled(false);

        LayoutInflater inflator = (LayoutInflater) this .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflator.inflate(R.layout.classmsg_action_view, null);

        TextView className = (TextView) v.findViewById(R.id.className);
        className.setText(grpName);

        actionBar.setCustomView(v);

        //setting click action on action bar
        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SendMessage.this, Subscribers.class);
                intent.putExtra("className", grpName);
                intent.putExtra("classCode", groupCode);

                startActivity(intent);
            }
        });

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
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
        super.onCreateOptionsMenu(menu);
        return true;
    }
}
