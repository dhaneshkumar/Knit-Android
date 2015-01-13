package trumplabs.schoolapp;

import android.content.Intent;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.ParseUser;

import java.lang.reflect.Field;

import BackGroundProcesses.Refresher;
import baseclasses.MyActionBarActivity;
import fr.castorflex.android.smoothprogressbar.SmoothProgressBar;
import joinclasses.JoinClassesContainer;
import profileDetails.ProfilePage;
import trumplab.textslate.R;
import utility.SessionManager;
import utility.Utility;

/**
 * This Activity shows home page of our app. It contains three fragments outbox, inbox and classrooms.
 */
public class MainActivity extends MyActionBarActivity implements TabListener {
    ViewPager viewpager;
    LinearLayout tabviewer;
    LinearLayout tab1;
    LinearLayout tab2;
    LinearLayout tab3;
    private ImageView tab1Icon;
    private ImageView tab2Icon;
    private ImageView tab3Icon;
    TextView tabcolor;
    LinearLayout.LayoutParams params;
    int screenwidth;
    android.support.v7.app.ActionBar actionbar;
    private String role;
    int backCount = 0;
    boolean signInFlag = false;
    public static LinearLayout progressBarLayout;
    public static LinearLayout editLayout;
    public static SmoothProgressBar mHeaderProgressBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.homepage_layout);
        signInFlag = false;
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out); //setting animation
        ParseUser parseObject = ParseUser.getCurrentUser();

        //check for current user loggedin
        if (parseObject == null)
            Utility.logout();

        progressBarLayout = (LinearLayout) findViewById(R.id.progressBarLayout);
        editLayout = (LinearLayout) findViewById(R.id.editLayout);
        mHeaderProgressBar = (SmoothProgressBar) findViewById(R.id.ptr_progress);


        /*
        Check for app reinstallation. In case of reinstallation or delete data appOpeningCount set to zero.
        So, we retrieve all data from server.
         */
        SessionManager session = new SessionManager(Application.getAppContext());
        int appOpeningCount = session.getAppOpeningCount();
        if (appOpeningCount == 0 || appOpeningCount == 1) {
            session.setAppOpeningCount();

            if (!session.getSignUpAccount()) {
                progressBarLayout.setVisibility(View.VISIBLE);
                editLayout.setVisibility(View.GONE);
                new Refresher(0);
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
        tab1Icon = (ImageView) findViewById(R.id.tab1Icon);
        tab2Icon = (ImageView) findViewById(R.id.tab2Icon);
        tab3Icon = (ImageView) findViewById(R.id.tab3Icon);

        // setting layout params for tab color
        params = (LinearLayout.LayoutParams) tabcolor.getLayoutParams();
        params.height = getResources().getDimensionPixelSize(R.dimen.dimen4);
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

        //setting tab click functionality
        tab1.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                highLightOutbox();
                viewpager.setCurrentItem(0);
            }
        });
        tab2.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                highLightInbox();
                viewpager.setCurrentItem(1);
            }
        });

        tab3.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                highLightClassrooms();
                viewpager.setCurrentItem(2);
            }
        });


        //gui setup according to ROLE
        if (role.equals("teacher")) {
            params.width = screenwidth / 3;
        } else {
            LinearLayout parentLayout = (LinearLayout) findViewById(R.id.tabviewer);
            parentLayout.setVisibility(View.GONE);
            actionbar.setTitle("Inbox");
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
                    setTitle("Outbox");
                    params.setMargins(positionOffsetPixels / 3, 0, 0, 0);  // added " positionOffsetPixels/3" for smooth transition
                    tabcolor.setLayoutParams(params);
                    highLightOutbox();
                } else if (position == 1) {
                    setTitle("Inbox");

                    params.setMargins((screenwidth / 3) + (positionOffsetPixels / 3), 0, 0, 0); // added " positionOffsetPixels/3" for smooth transition
                    tabcolor.setLayoutParams(params);

                    highLightInbox();
                } else {
                    setTitle("Classrooms");
                    params.setMargins((2 * screenwidth / 3), 0, 0, 0);
                    tabcolor.setLayoutParams(params);
                    highLightClassrooms();
                }
            }

            @Override
            public void onPageScrollStateChanged(int arg0) {

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Utility.isInternetOn(this);
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
        // inflater.inflate(R.menu.messages, menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case R.id.addclass:
                startActivity(new Intent(this, CreateClass.class));
                break;
            case R.id.joinedclasses:
                startActivity(new Intent(this, JoinClassesContainer.class));
                break;

            case R.id.joinclass:
                Intent intent = new Intent(this, JoinClassesContainer.class);
                intent.putExtra("VIEWPAGERINDEX", 1);
                startActivity(intent);
                break;

            case R.id.profile:
                this.startActivity(new Intent(this, ProfilePage.class));
                break;

            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    class MyAdapter extends FragmentPagerAdapter {

        public MyAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int arg0) {

            Fragment fragment = null;
            if (role.equals("teacher")) {

                switch (arg0) {
                    case 0:
                        fragment = new Outbox();
                        break;
                    case 1:
                        fragment = new Messages();
                        break;
                    case 2:
                        fragment = new Classrooms();
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
        tab1Icon.setBackgroundDrawable(getResources().getDrawable(R.drawable.outbox));
        tab2Icon.setBackgroundDrawable(getResources().getDrawable(R.drawable.inbox_grey));
        tab3Icon.setBackgroundDrawable(getResources().getDrawable(R.drawable.classroom_grey));
    }

    //highlight inbox icon
    private void highLightInbox() {
        tab1Icon.setBackgroundDrawable(getResources().getDrawable(R.drawable.outbox_grey));
        tab2Icon.setBackgroundDrawable(getResources().getDrawable(R.drawable.inbox));
        tab3Icon.setBackgroundDrawable(getResources().getDrawable(R.drawable.classroom_grey));
    }

    //highlight classroom icon
    private void highLightClassrooms() {
        tab1Icon.setBackgroundDrawable(getResources().getDrawable(R.drawable.outbox_grey));
        tab2Icon.setBackgroundDrawable(getResources().getDrawable(R.drawable.inbox_grey));
        tab3Icon.setBackgroundDrawable(getResources().getDrawable(R.drawable.classroom));
    }
}
