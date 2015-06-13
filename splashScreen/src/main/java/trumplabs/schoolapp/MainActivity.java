package trumplabs.schoolapp;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar.Tab;
import android.support.v7.app.ActionBar.TabListener;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewConfiguration;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.parse.ParseAnalytics;
import com.parse.ParseUser;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import BackGroundProcesses.Refresher;
import additionals.Invite;
import additionals.RateAppDialog;
import additionals.SpreadWordDialog;
import baseclasses.MyActionBarActivity;
import fr.castorflex.android.smoothprogressbar.SmoothProgressBar;
import joinclasses.JoinClassDialog;
import joinclasses.JoinClassesContainer;
import library.UtilString;
import notifications.AlarmTrigger;
import profileDetails.ProfilePage;
import trumplab.textslate.R;
import utility.Config;
import utility.SessionManager;
import utility.Utility;

/**
 * This Activity shows home page of our app. It contains three fragments outbox, inbox and classrooms.
 */
public class MainActivity extends MyActionBarActivity implements TabListener {
    static ViewPager viewpager;
    LinearLayout tabviewer;
    LinearLayout tab1;
    LinearLayout tab2;
    LinearLayout tab3;
    private TextView tab1Icon;
    private TextView tab2Icon;
    private TextView tab3Icon;
    TextView tabcolor;
    LinearLayout.LayoutParams params;
    int screenwidth;
    android.support.v7.app.ActionBar actionbar;
    private static String role;
    int backCount = 0;
    boolean signInFlag = false;
    public static LinearLayout progressBarLayout;
    public static LinearLayout editLayout;
    public static SmoothProgressBar mHeaderProgressBar;

    static boolean isEventCheckerAlarmTriggered = false; //flag telling whether alarm for event checker has been triggered or not

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("DEBUG_TEMPORARY", "onCreate Called");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.homepage_layout);
        signInFlag = false;
       // overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out); //setting animation
        ParseUser parseObject = ParseUser.getCurrentUser();

        //check for current user loggedin
        if (parseObject == null)
            {Utility.logout(); return;}


        progressBarLayout = (LinearLayout) findViewById(R.id.progressBarLayout);
        editLayout = (LinearLayout) findViewById(R.id.editLayout);
        mHeaderProgressBar = (SmoothProgressBar) findViewById(R.id.ptr_progress);



        Map<String, String> dimensions = new HashMap<String, String>();
// Define ranges to bucket data points into meaningful segments
        dimensions.put("priceRange", "1000-1500");
// Did the user filter the query?
        dimensions.put("source", "craigslist");
// Do searches happen more often on weekdays or weekends?
        dimensions.put("dayType", "weekday");
// Send the dimensions to Parse along with the 'search' event
        ParseAnalytics.trackEvent("search", dimensions);

        /*
        Check for app reinstallation. In case of reinstallation or delete data appOpeningCount set to zero.
        So, we retrieve all data from server.
         */
        final SessionManager session = new SessionManager(Application.getAppContext());
        final int appOpeningCount = session.getAppOpeningCount();
        if (appOpeningCount == 0) {
            session.setAppOpeningCount();
            if (!session.getSignUpAccount()) {
                //Log.d("MAINACTIVITY_CALLING_REFRESHER", "showing progress");
                progressBarLayout.setVisibility(View.VISIBLE);
                editLayout.setVisibility(View.GONE);

                //call refresher
                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        new Refresher(appOpeningCount);
                    }
                };
                Thread t = new Thread(r);
                t.setPriority(Thread.MIN_PRIORITY);
                t.start();
            }
        }

        String userId = parseObject.getUsername();
        role = parseObject.getString(Constants.ROLE);


        // initialize elements
        viewpager = (ViewPager) findViewById(R.id.pager);
        tabviewer = (LinearLayout) findViewById(R.id.tabviewer);
        tab1 = (LinearLayout) findViewById(R.id.tab1);
        tab2 = (LinearLayout) findViewById(R.id.tab2);
        tab3 = (LinearLayout) findViewById(R.id.tab3);
        tabcolor = (TextView) findViewById(R.id.tabcolor);
        tab1Icon = (TextView) findViewById(R.id.tab1Icon);
        tab2Icon = (TextView) findViewById(R.id.tab2Icon);
        tab3Icon = (TextView) findViewById(R.id.tab3Icon);

        Typeface typeFace = Typeface.createFromAsset(getAssets(), "fonts/RobotoCondensed-Regular.ttf");
        tab1Icon.setTypeface(typeFace);
        tab2Icon.setTypeface(typeFace);
        tab3Icon.setTypeface(typeFace);

        // setting layout params for tab color
        params = (LinearLayout.LayoutParams) tabcolor.getLayoutParams();
        Display mDisplay = this.getWindowManager().getDefaultDisplay();
        screenwidth = mDisplay.getWidth();
        //
        // Setting action bar properties
        actionbar = getSupportActionBar();
        // To disable the hardware menu button and display the menu in action bar
        try {
            ViewConfiguration config = ViewConfiguration.get(this);
            Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
            if (menuKeyField != null) {
                menuKeyField.setAccessible(true);
                menuKeyField.setBoolean(config, false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        actionbar.setDisplayShowCustomEnabled(true);
        FragmentManager fragmentmanager = getSupportFragmentManager();
        viewpager.setAdapter(new MyAdapter(fragmentmanager));
        viewpager.setOffscreenPageLimit(2);

        //setting tab click functionality
        tab1.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                highLightClassrooms();
                viewpager.setCurrentItem(0);
            }
        });
        tab2.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                highLightOutbox();
                viewpager.setCurrentItem(1);
            }
        });

        tab3.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                highLightInbox();
                viewpager.setCurrentItem(2);
            }
        });


        //gui setup according to ROLE
        if (role.equals("teacher")) {
            params.width = screenwidth / 3;
        } else {
            LinearLayout parentLayout = (LinearLayout) findViewById(R.id.tabviewer);
            parentLayout.setVisibility(View.GONE);
            //actionbar.setTitle("Inbox");
            // params.width = screenwidth;
        }
        params.setMargins(0, 0, 0, 0);
        tabcolor.setLayoutParams(params);

        //swipe feature implementation
        viewpager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageSelected(int arg0) {
                supportInvalidateOptionsMenu();
            }

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

                /*
                scrolling from one tab to other
                 */
                if (position == 0) {
                   // setTitle("Outbox");
                    params.setMargins(positionOffsetPixels / 3, 0, 0, 0);  // added " positionOffsetPixels/3" for smooth transition
                    tabcolor.setLayoutParams(params);
                    highLightClassrooms();
                    showButttonContainer(Classrooms.buttonContainer);
                } else if (position == 1) {
                    //setTitle("Inbox");

                    params.setMargins((screenwidth / 3) + (positionOffsetPixels / 3), 0, 0, 0); // added " positionOffsetPixels/3" for smooth transition
                    tabcolor.setLayoutParams(params);

                    highLightOutbox();
                    hideButttonContainer(Classrooms.buttonContainer);

                } else {
                    //setTitle("Classrooms");
                    params.setMargins((2 * screenwidth / 3), 0, 0, 0);
                    tabcolor.setLayoutParams(params);
                    highLightInbox();
                    hideButttonContainer(Classrooms.buttonContainer);
                }
            }

            @Override
            public void onPageScrollStateChanged(int arg0) {

            }
        });

        //ViewPager.setCurrentItem(int pageIndex, boolean isSmoothScroll);

        if(getIntent() != null && getIntent().getExtras() != null) {
            int currentItem = getIntent().getExtras().getInt("VIEWPAGERINDEX", -1);
            if (currentItem != -1) {
                viewpager.setCurrentItem(currentItem, true);
            }
        }

        if(!isEventCheckerAlarmTriggered){
            //Log.d("DEBUG_MAIN_ACTIVITY_ALARM", "triggering alarm on app opening");
            AlarmTrigger.triggerEventCheckerAlarm(Application.getAppContext());
            isEventCheckerAlarmTriggered = true;
        }
        else{
            //Log.d("DEBUG_MAIN_ACTIVITY_ALARM", "alarm already triggered");
        }


        final SessionManager sessionManager = new SessionManager(Application.getAppContext());
        int actionBarHeight = sessionManager.getActionBarHeight();

        if(actionBarHeight == 0) {
            //Storing action bar height locally
            Thread thread = new Thread() {
                @Override
                public void run() {
                    try {
                        while (true) {
                            sleep(1000);
                            sessionManager.setActionBarHeight(getSupportActionBar().getHeight());
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            };

            thread.start();
        }

        //show recommend app dialog
        if(appOpeningCount == 6 || appOpeningCount == 20) {

                FragmentManager fm = getSupportFragmentManager();
                SpreadWordDialog spreadWordDialog = new SpreadWordDialog();
                spreadWordDialog.show(fm, "recommend app");

                session.setAppOpeningCount();
        }

        //show rate app dialog after using 10 times app
        if(appOpeningCount == 11) {

            ParseUser user = ParseUser.getCurrentUser() ;
            if( user!= null && !user.getBoolean("APP_RATED")) { //checking whether already app rated or not
                FragmentManager fm = getSupportFragmentManager();
                RateAppDialog rateAppDialog = new RateAppDialog();
                rateAppDialog.show(fm, "rate app");

                user.put("APP_RATED", true);
                user.saveEventually();

            }
        }

        //check if parent/student has just signed up and show join class dialog
        Intent intent = getIntent();
        if(intent != null && intent.getExtras() != null) {
            Log.d("DEBUG_MAIN", "extras non null");
            String signup = intent.getExtras().getString("flag");
            if(signup != null){
                Log.d("DEBUG_MAIN", "signup flag is " + signup + "; role " + role);
            }
            else{
                Log.d("DEBUG_MAIN", "signup flag is null; role is" + role);
            }

            if(!UtilString.isBlank(signup) && signup.equalsIgnoreCase("SIGNUP")) {
                intent.putExtra("flag", "false"); //set flag to something else
                setIntent(intent);

              /*  if (role.equalsIgnoreCase("student") || role.equalsIgnoreCase("parent")) {
                    Log.d("DEBUG_MAIN", "creating join class dialog");
                    FragmentManager fm = getSupportFragmentManager();
                    JoinClassDialog joinClassDialog = new JoinClassDialog();
                    joinClassDialog.show(fm, "Join Class");
                }*/
                if(role.equalsIgnoreCase("teacher"))
                {
                    FragmentManager fm = getSupportFragmentManager();
                    CreateClassDialog createClassDialog = new CreateClassDialog();
                    Bundle args = new Bundle();
                    args.putString("flag", "SIGNUP");
                    createClassDialog.setArguments(args);
                    createClassDialog.show(fm, "create Class");
                }

               Constants.signup_classrooms= true;
               Constants.signup_inbox= true;
               Constants.signup_outbox = true;
            }
        }
        FacebookSdk.sdkInitialize(getApplicationContext());

//        testing notification actions :
//        development_knit - User : 0000001017

//        if(showNot){
//
//            NotificationGenerator.generateNotification(this, EventCheckerAlarmReceiver.parentTip1Content , Constants.DEFAULT_NAME, Constants.NORMAL_NOTIFICATION, Constants.INBOX_ACTION);
//            NotificationGenerator.generateNotification(this, EventCheckerAlarmReceiver.parentNoActivityContent, Constants.DEFAULT_NAME, Constants.TRANSITION_NOTIFICATION, Constants.INVITE_TEACHER_ACTION);
//
//            NotificationGenerator.generateNotification(this, EventCheckerAlarmReceiver.teacherNoActivityContent, Constants.DEFAULT_NAME, Constants.TRANSITION_NOTIFICATION, Constants.CREATE_CLASS_ACTION);
//            NotificationGenerator.generateNotification(this, EventCheckerAlarmReceiver.teacherTip1Content, Constants.DEFAULT_NAME, Constants.TRANSITION_NOTIFICATION, Constants.OUTBOX_ACTION);
//
//            Bundle extras = new Bundle();
//            String className = "CUP";
//            String classCode = "ASH8636";
//            extras.putString("grpCode", classCode);
//            extras.putString("grpName", className);
//            NotificationGenerator.generateNotification(this, "Your classroom " + className + EventCheckerAlarmReceiver.teacherNoSubContent, Constants.DEFAULT_NAME, Constants.TRANSITION_NOTIFICATION, Constants.INVITE_PARENT_ACTION, extras);
//            NotificationGenerator.generateNotification(this, EventCheckerAlarmReceiver.teacherNoMsgContent + className + ". Send a message now !", Constants.DEFAULT_NAME, Constants.TRANSITION_NOTIFICATION, Constants.SEND_MESSAGE_ACTION, extras);
//            NotificationGenerator.generateNotification(this, "10" + EventCheckerAlarmReceiver.teacherConfusingMsgContent + className, Constants.DEFAULT_NAME, Constants.TRANSITION_NOTIFICATION, Constants.OUTBOX_ACTION);
//
//            /*test merging in case of normal notification*/
//            NotificationGenerator.generateNotification(this, "Message 1 = " + EventCheckerAlarmReceiver.parentTip1Content , "Mr XYZ", Constants.NORMAL_NOTIFICATION, Constants.INBOX_ACTION);
//            NotificationGenerator.generateNotification(this, "Message 2 = " +  EventCheckerAlarmReceiver.teacherTip1Content, "Miss PQR", Constants.NORMAL_NOTIFICATION, Constants.INBOX_ACTION);
//
//            showNot = false;
//        }
    }

//    static boolean showNot = true; //temporary hack to show notification once on app start

    @Override
    protected void onResume() {
        super.onResume();
        Application.mainActivityVisible = true;
        AppEventsLogger.activateApp(this, Config.FB_APP_ID);
        Log.d("DEBUG_MAIN_ACTIVITY", "visibility TRUE");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Application.mainActivityVisible = false;
        AppEventsLogger.deactivateApp(this, Config.FB_APP_ID);
        Log.d("DEBUG_MAIN_ACTIVITY", "visibility FALSE");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();

        if (role.equals("teacher"))
            inflater.inflate(R.menu.mainactivity_for_teachers, menu);
        else
            inflater.inflate(R.menu.mainactivity_for_parents, menu);

        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case R.id.joinedclasses:
                startActivity(new Intent(this, JoinClassesContainer.class));
                break;

            case R.id.joinclass:
                FragmentManager fm = getSupportFragmentManager();
                JoinClassDialog joinClassDialog = new JoinClassDialog();
                joinClassDialog.show(fm, "Join Class");
                break;

            case R.id.profile:
                this.startActivity(new Intent(this, ProfilePage.class));
                break;
            case R.id.spread:
                //show the common Invite screen
                Intent intent = new Intent(getBaseContext(), Invite.class);
                intent.putExtra("inviteType", Constants.INVITATION_SPREAD);
                startActivity(intent);

                /*Intent i=new Intent(android.content.Intent.ACTION_SEND);
                i.setType("text/plain");
                i.putExtra(android.content.Intent.EXTRA_SUBJECT,"Knit");
                i.putExtra(android.content.Intent.EXTRA_TEXT, Constants.spreadWordContent);
                startActivity(Intent.createChooser(i,"Share via"));*/

                break;

            case R.id.feedback:
                FeedBackClass feedBack = new FeedBackClass();
                FragmentManager fmr = getSupportFragmentManager();
                feedBack.show(fmr, "FeedBackClass");

                break;

            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    static class MyAdapter extends FragmentPagerAdapter {

        public MyAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int arg0) {

            Fragment fragment = null;
            if (role.equals("teacher")) {
                switch (arg0) {
                    case 0:

                        fragment = new Classrooms();
                        break;
                    case 1:
                        fragment = new Outbox();
                        break;
                    case 2:
                        fragment = new Messages();
                        break;
                    default:
                        break;
                }
            } else
                fragment = new Messages();

            return fragment;
        }

        @Override
        public int getCount() {
            if (role.equals("teacher")) {
                return 3;
            } else
                return 1;
        }
    }

    /**
     * setting back button functionality
     * on double click, you can exit from app
     */
    public void onBackPressed() {

        if (backCount == 1) {

            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            startActivity(intent);
        } else {
            backCount++;

            Toast.makeText(getApplicationContext(), "Press again to Exit", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onTabReselected(Tab arg0, FragmentTransaction arg1) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onTabSelected(Tab arg0, FragmentTransaction arg1) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onTabUnselected(Tab arg0, FragmentTransaction arg1) {
        // TODO Auto-generated method stub

    }


    //highlight outbox icon
    private void highLightOutbox() {
        tab2Icon.setTextColor(getResources().getColor(R.color.buttoncolor));
        tab1Icon.setTextColor(getResources().getColor(R.color.light_button_color));
        tab3Icon.setTextColor(getResources().getColor(R.color.light_button_color));
    }

    //highlight inbox icon
    private void highLightInbox() {
        tab3Icon.setTextColor(getResources().getColor(R.color.buttoncolor));
        tab1Icon.setTextColor(getResources().getColor(R.color.light_button_color));
        tab2Icon.setTextColor(getResources().getColor(R.color.light_button_color));
    }

    //highlight classroom icon
    private void highLightClassrooms() {
        tab1Icon.setTextColor(getResources().getColor(R.color.buttoncolor));
        tab2Icon.setTextColor(getResources().getColor(R.color.light_button_color));
        tab3Icon.setTextColor(getResources().getColor(R.color.light_button_color));
    }



    private void showButttonContainer(LinearLayout buttonContainer)
    {

        if(buttonContainer == null)
            return;

        Animation bottomUp = AnimationUtils.loadAnimation(this,
                R.anim.bottom_up);
        buttonContainer.setAnimation(bottomUp);
        bottomUp.setDuration(100);
        bottomUp.start();
        buttonContainer.setVisibility(View.VISIBLE);
    }


    private void hideButttonContainer(LinearLayout buttonContainer)
    {

        if(buttonContainer == null)
            return;

        Animation bottomUp = AnimationUtils.loadAnimation(this,
                R.anim.bottom_down);
        buttonContainer.setAnimation(bottomUp);
       // bottomUp.setDuration(100);
        bottomUp.start();
        buttonContainer.setVisibility(View.GONE);
    }
}
