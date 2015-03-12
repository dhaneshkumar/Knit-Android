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

import com.parse.ParseUser;

import java.lang.reflect.Field;

import BackGroundProcesses.Refresher;
import baseclasses.MyActionBarActivity;
import fr.castorflex.android.smoothprogressbar.SmoothProgressBar;
import joinclasses.JoinClassDialog;
import joinclasses.JoinClassesContainer;
import notifications.AlarmTrigger;
import profileDetails.ProfilePage;
import trumplab.textslate.R;
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


        /*
        Check for app reinstallation. In case of reinstallation or delete data appOpeningCount set to zero.
        So, we retrieve all data from server.
         */
        final SessionManager session = new SessionManager(Application.getAppContext());
        int appOpeningCount = session.getAppOpeningCount();
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
                        new Refresher(session.getAppOpeningCount());
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

    }

    @Override
    protected void onResume() {
        super.onResume();
        //Utility.isInternetOn(this);
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

                String link = "Hey, You must try this great parent-teacher communication app. https://play.google.com/store/apps/details?id=trumplab.textslate";
                Intent i=new Intent(android.content.Intent.ACTION_SEND);
                i.setType("text/plain");
                i.putExtra(android.content.Intent.EXTRA_SUBJECT,"Knit");
                i.putExtra(android.content.Intent.EXTRA_TEXT, link);
                startActivity(Intent.createChooser(i,"Share via"));

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
