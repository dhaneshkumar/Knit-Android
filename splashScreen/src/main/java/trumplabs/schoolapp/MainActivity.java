package trumplabs.schoolapp;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.Tab;
import android.support.v7.app.ActionBar.TabListener;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.parse.ParseUser;
import com.software.shell.fab.ActionButton;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

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
    public static ViewPager viewpager;
    public static TextView tab1Icon;
    public static TextView tab2Icon;
    public static TextView tab3Icon;
    private TextView tabcolor;
    private LinearLayout.LayoutParams params;
    private int screenwidth;
    private static String role;
    private int backCount = 0;
    public static LinearLayout progressBarLayout;
    public static SmoothProgressBar mHeaderProgressBar;
    public static MyAdapter myAdapter;
    public static SessionManager sessionManager;
    private ListView action_menu_list;
    private RelativeLayout action_menu;
    private List<List<String >> classList;
    private boolean isFloatingButtonCliked = false;
    Typeface lighttypeFace;

    //flag telling whether alarm for event checker has been triggered or not
    static boolean isEventCheckerAlarmTriggered = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.homepage_layout);

        ParseUser user = ParseUser.getCurrentUser();

        //check for current user loggedin or not
        if (user == null)
        {Utility.logout(); return;}

        // Initialize elements
        viewpager = (ViewPager) findViewById(R.id.pager);
        tabcolor = (TextView) findViewById(R.id.tabcolor);
        tab1Icon = (TextView) findViewById(R.id.tab1Icon);
        tab2Icon = (TextView) findViewById(R.id.tab2Icon);
        tab3Icon = (TextView) findViewById(R.id.tab3Icon);
        progressBarLayout = (LinearLayout) findViewById(R.id.progressBarLayout);
        mHeaderProgressBar = (SmoothProgressBar) findViewById(R.id.ptr_progress);
        action_menu = (RelativeLayout) findViewById(R.id.action_menu);
        action_menu_list = (ListView) findViewById(R.id.action_menu_list);

        FloatOptionsAdapter floatOptionsAdapter = new FloatOptionsAdapter();
        action_menu_list.setAdapter(floatOptionsAdapter);

        Typeface typeFace = Typeface.createFromAsset(getAssets(), "fonts/RobotoCondensed-Regular.ttf");
        lighttypeFace = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Light.ttf");
        tab1Icon.setTypeface(typeFace);
        tab2Icon.setTypeface(typeFace);
        tab3Icon.setTypeface(typeFace);

        classList = user.getList(Constants.CREATED_GROUPS);

        if(classList == null)
            classList = new ArrayList<>();

        /*
        Check for app re-installation. In case of reinstallation or delete data appOpeningCount set to zero.
        So, we retrieve all data from server.
         */
        if(sessionManager == null)
            sessionManager = new SessionManager(Application.getAppContext());

        final int appOpeningCount = sessionManager.getAppOpeningCount();
        if (appOpeningCount == 0) {
            sessionManager.setAppOpeningCount();
            if (!sessionManager.getSignUpAccount()) {
                progressBarLayout.setVisibility(View.VISIBLE);

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

        role = user.getString(Constants.ROLE);

        // setting layout params for tab color
        params = (LinearLayout.LayoutParams) tabcolor.getLayoutParams();
        Display mDisplay = this.getWindowManager().getDefaultDisplay();
        screenwidth = mDisplay.getWidth();

         /*
            Setting custom view in action bar <Increased font size and bold>
         */
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);

        LayoutInflater inflator = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflator.inflate(R.layout.action_bar_view, null);
        actionBar.setCustomView(v);

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

        FragmentManager fragmentmanager = getSupportFragmentManager();

        myAdapter = new MyAdapter(fragmentmanager);
        viewpager.setAdapter(myAdapter);



        //Set the number of pages that should be retained to either side of the current page
        if(sessionManager.getHasUserJoinedClass())
            viewpager.setOffscreenPageLimit(2);
        else
            viewpager.setOffscreenPageLimit(1);


        //setting tab click functionality
        tab1Icon.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                highLightTab1();
                viewpager.setCurrentItem(0);
            }
        });
        tab2Icon.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                highLightTab2();
                viewpager.setCurrentItem(1);
            }
        });

        tab3Icon.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                highLightTab3();
                viewpager.setCurrentItem(2);
            }
        });


        //gui setup according to ROLE
        if (role.equals("teacher")) {

            if(sessionManager.getHasUserJoinedClass()) {
                tab3Icon.setVisibility(View.VISIBLE);
            }
            else {
                tab3Icon.setVisibility(View.GONE);
                tab1Icon.setText("MESSAGES");
                tab1Icon.setTextSize(16);
                tab1Icon.setGravity(Gravity.CENTER);

                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams( 0, LinearLayout.LayoutParams.WRAP_CONTENT, 5);
                tab1Icon.setLayoutParams(layoutParams);
            }
        } else {
            LinearLayout parentLayout = (LinearLayout) findViewById(R.id.tabviewer);
            parentLayout.setVisibility(View.GONE);
        }

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
                if(sessionManager.getHasUserJoinedClass()) {

                    if (position == 0) {
                        params.width = screenwidth * 5 / 13;

                        params.setMargins(positionOffsetPixels * 3 / 13, 0, 0, 0);  // added " positionOffsetPixels/3" for smooth transition
                        tabcolor.setLayoutParams(params);
                        highLightTab1();
                        hideButttonContainer(Classrooms.buttonContainer);
                      /*  if(Outbox.needLoading){
                            Log.d(SendPendingMessages.LOGTAG, "lazy loading outbox");
                            Outbox.GetLocalOutboxMsgInBackground outboxAT = new Outbox.GetLocalOutboxMsgInBackground();
                           8 outboxAT.execute();
                        }*/
                    } else if (position == 1) {

                        params.width = screenwidth * 5 / 13;
                        params.setMargins((screenwidth * 3 / 13) + (positionOffsetPixels * 5 / 13), 0, 0, 0); // added " positionOffsetPixels/3" for smooth transition
                        tabcolor.setLayoutParams(params);

                        highLightTab2();
                        showButttonContainer(Classrooms.buttonContainer);

                    } else {
                        params.width = screenwidth * 5 / 13;
                        params.setMargins((8 * screenwidth / 13), 0, 0, 0);
                        tabcolor.setLayoutParams(params);
                        highLightTab3();
                        hideButttonContainer(Classrooms.buttonContainer);
                    }
                }else
                {
                    params.width = screenwidth  / 2;
                    if (position == 0) {
                        params.setMargins(positionOffsetPixels / 2, 0, 0, 0);  // added " positionOffsetPixels/3" for smooth transition
                        tabcolor.setLayoutParams(params);
                        highLightTab1();
                        hideButttonContainer(Classrooms.buttonContainer);
                    } else {
                        params.setMargins((screenwidth /2), 0, 0, 0); // added " positionOffsetPixels/3" for smooth transition
                        tabcolor.setLayoutParams(params);
                        highLightTab2();
                        showButttonContainer(Classrooms.buttonContainer);
                    }
                }
            }

            @Override
            public void onPageScrollStateChanged(int arg0) {

            }
        });

        if(getIntent() != null && getIntent().getExtras() != null) {
            int currentItem = getIntent().getExtras().getInt("VIEWPAGERINDEX", -1);
            if (currentItem != -1) {
                viewpager.setCurrentItem(currentItem, true);
            }
        }

        if(!isEventCheckerAlarmTriggered){
            AlarmTrigger.triggerEventCheckerAlarm(Application.getAppContext());
            isEventCheckerAlarmTriggered = true;
        }
/*************+++++++++++++++++++++++++++++++++++++++++++++++============================================*/
        /**
         * setting action bar height to display no internet connection bar
         */
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

        /*******************************************************************************************************/

        //show recommend app dialog
        if(appOpeningCount == 6 || appOpeningCount == 21) {

            FragmentManager fm = getSupportFragmentManager();
            SpreadWordDialog spreadWordDialog = new SpreadWordDialog();
            spreadWordDialog.show(fm, "recommend app");

            sessionManager.setAppOpeningCount();
        }

        //show rate app dialog after using 10 times app
        if(appOpeningCount == 11) {

            if( user!= null && !user.getBoolean("APP_RATED")) { //checking whether already app rated or not
                FragmentManager fm = getSupportFragmentManager();
                RateAppDialog rateAppDialog = new RateAppDialog();
                rateAppDialog.show(fm, "rate app");

                user.put("APP_RATED", true);
                user.saveEventually();
            }
        }

        //Check if parent/student has just signed up and show join class dialog
        Intent intent = getIntent();
        if(intent != null && intent.getExtras() != null) {
            String signup = intent.getExtras().getString("flag");

            if(!UtilString.isBlank(signup) && signup.equalsIgnoreCase("SIGNUP")) {
                intent.putExtra("flag", "false"); //set flag to something else
                setIntent(intent);

                if (role.equalsIgnoreCase("student") || role.equalsIgnoreCase("parent")) {
                    FragmentManager fm = getSupportFragmentManager();
                    JoinClassDialog joinClassDialog = new JoinClassDialog();
                    joinClassDialog.show(fm, "Join Class");
                }

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


        //Initializing compose button
        final ActionButton actionButton = (ActionButton) findViewById(R.id.action_button);

        // To set button color for normal state:
        actionButton.setButtonColor(Color.parseColor("#039BE5"));

        //#E53935 -  red(600)
        // To set button color for pressed state:
        actionButton.setButtonColorPressed(Color.parseColor("#01579B"));

        //Setting image in floating button
        actionButton.setImageResource(R.drawable.ic_edit);

        // To enable or disable Ripple Effect:
        actionButton.setRippleEffectEnabled(true);



        actionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(classList.size() == 0)
                {
                    Intent intent = new Intent(MainActivity.this, ComposeMessage.class);
                    startActivity(intent);
                }
                else if(classList.size() == 1)
                {
                    Intent intent = new Intent(MainActivity.this, ComposeMessage.class);
                    intent.putExtra("CLASS_CODE", classList.get(0).get(0));
                    intent.putExtra("CLASS_NAME", classList.get(0).get(1));
                    startActivity(intent);
                }
                else {

                    action_menu.setVisibility(View.VISIBLE);
                    action_menu_list.setVisibility(View.VISIBLE);

                    if (isFloatingButtonCliked) {
                        Intent intent = new Intent(MainActivity.this, ComposeMessage.class);
                        startActivity(intent);
                        isFloatingButtonCliked = false;
                        action_menu.setVisibility(View.GONE);
                        action_menu_list.setVisibility(View.GONE);
                    } else
                        isFloatingButtonCliked = true;
                }
            }
        });


        action_menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                action_menu.setVisibility(View.GONE);
                action_menu_list.setVisibility(View.GONE);
                isFloatingButtonCliked = false;
            }
        });

       /* action_menu_list.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                action_menu.setVisibility(View.VISIBLE);
                action_menu_list.setVisibility(View.VISIBLE);
            }
        });*/

    }


    @Override
    protected void onResume() {
        super.onResume();
        Application.mainActivityVisible = true;
        AppEventsLogger.activateApp(this, Config.FB_APP_ID);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Application.mainActivityVisible = false;
        AppEventsLogger.deactivateApp(this, Config.FB_APP_ID);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();

        if (role.equals("teacher"))
            inflater.inflate(R.menu.mainactivity_for_teachers, menu);
        else
            inflater.inflate(R.menu.mainactivity_for_parents, menu);

        if (!role.equals("teacher")) {
            //prepare action views for menu items - join and joined
            final ImageView joinClassActionView = (ImageView) menu.findItem(R.id.joinclass).getActionView();
            final ImageView joinedClassesActionView = (ImageView) menu.findItem(R.id.joinedclasses).getActionView();

            joinClassActionView.setImageResource(R.drawable.ic_action_import);
            joinedClassesActionView.setImageResource(R.drawable.ic_action_document);

            joinClassActionView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    FragmentManager fm = getSupportFragmentManager();
                    JoinClassDialog joinClassDialog = new JoinClassDialog();
                    joinClassDialog.show(fm, "Join Class");
                }
            });

            joinedClassesActionView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(MainActivity.this, JoinClassesContainer.class));
                }
            });

            //setting up views to highlight
            Log.d("_TEMP_", "setting up action bar views to showcase");
            Messages.t1 = joinClassActionView;
            Messages.t2 = joinedClassesActionView;
        }

        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {



            case R.id.joinedclasses:
                startActivity(new Intent(this, JoinClassesContainer.class));
                break;

            case R.id.createclass:
                FragmentManager fm = getSupportFragmentManager();
                CreateClassDialog createClassDialog = new CreateClassDialog();
                createClassDialog.show(fm,"create class");
                break;

            case R.id.joinclass:
                FragmentManager fm1 = getSupportFragmentManager();
                JoinClassDialog joinClassDialog = new JoinClassDialog();
                joinClassDialog.show(fm1, "Join Class");
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

    public static class MyAdapter extends FragmentPagerAdapter {

        public MyAdapter(FragmentManager fm) {
            super(fm);

            if(sessionManager == null)
                sessionManager = new SessionManager(Application.getAppContext());
        }

        @Override
        public Fragment getItem(int arg0) {

            Fragment fragment = null;
            if (role.equals("teacher")) {

                if(sessionManager.getHasUserJoinedClass()) {
                    switch (arg0) {
                        case 0:
                            fragment = new Outbox();
                            break;
                        case 1:
                            fragment = new Classrooms();
                            break;
                        case 2:
                            fragment = new Messages();
                            break;
                        default:
                            break;
                    }
                }
                else {
                    switch (arg0) {
                        case 0:
                            fragment = new Outbox();
                            break;
                        case 1:
                            fragment = new Classrooms();
                            break;
                        default:
                            break;
                    }
                }
            } else
                fragment = new Messages();

            return fragment;
        }

        @Override
        public int getCount() {
            if (role.equals("teacher")) {

                if(sessionManager.getHasUserJoinedClass())
                    return 3;
                else
                    return 2;
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
    }

    @Override
    public void onTabSelected(Tab arg0, FragmentTransaction arg1) {
    }

    @Override
    public void onTabUnselected(Tab arg0, FragmentTransaction arg1) {
    }


    //highlight outbox icon
    private void highLightTab2() {
        tab2Icon.setTextColor(getResources().getColor(R.color.white));
        tab1Icon.setTextColor(getResources().getColor(R.color.light_button_color));
        tab3Icon.setTextColor(getResources().getColor(R.color.light_button_color));
    }

    //highlight inbox icon
    private void highLightTab3() {
        tab3Icon.setTextColor(getResources().getColor(R.color.white));
        tab1Icon.setTextColor(getResources().getColor(R.color.light_button_color));
        tab2Icon.setTextColor(getResources().getColor(R.color.light_button_color));
    }

    //highlight classroom icon
    private void highLightTab1() {
        tab1Icon.setTextColor(getResources().getColor(R.color.white));
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

    class FloatOptionsAdapter extends BaseAdapter {

        @Override
        public int getCount() {

            if (classList == null)
                classList = new ArrayList<List<String>>();

            if (classList.size() > 1)
                return classList.size() + 1;
            else
                return classList.size();
        }

        @Override
        public Object getItem(int position) {
            return classList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public boolean isEnabled (int position) {
            return false;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View row = convertView;
            if (row == null) {
                LayoutInflater inflater = (LayoutInflater) MainActivity.this
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                row = inflater.inflate(R.layout.float_button_options, parent, false);
            }

            TextView headerText = (TextView) row.findViewById(R.id.optionItem);
            TextView emptyBox = (TextView) row.findViewById(R.id.emptyBox);
            LinearLayout option = (LinearLayout) row.findViewById(R.id.option);
            TextView headerIcon = (TextView) row.findViewById(R.id.headerText);
            headerIcon.setTypeface(lighttypeFace);

            if (classList.size() > 1) {
                if (position == 0) {
                    headerText.setText("SELECT ALL");
                    headerIcon.setText("*");
                } else {
                    headerText.setText(classList.get(position - 1).get(1));
                    headerIcon.setText(classList.get(position - 1).get(1).substring(0, 1).toUpperCase());
                }
            } else {
                headerText.setText(classList.get(position).get(1));
                headerIcon.setText(classList.get(position).get(1).substring(0, 1).toUpperCase());
            }

            GradientDrawable gradientdrawable = (GradientDrawable) headerIcon.getBackground();
            gradientdrawable.setColor(getResources().getColor(R.color.color_secondary));

            emptyBox.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    action_menu.setVisibility(View.GONE);
                    action_menu_list.setVisibility(View.GONE);
                    isFloatingButtonCliked = false;

                }
            });

            option.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(MainActivity.this, ComposeMessage.class);

                    if(position ==0)
                        intent.putExtra("SELECT_ALL", true);
                    else {
                        intent.putExtra("CLASS_CODE", classList.get(position - 1).get(0));
                        intent.putExtra("CLASS_NAME", classList.get(position - 1).get(1));
                    }

                    startActivity(intent);
                    isFloatingButtonCliked = false;
                    action_menu.setVisibility(View.GONE);
                    action_menu_list.setVisibility(View.GONE);
                }
            });

            return row;
        }
    }
}
