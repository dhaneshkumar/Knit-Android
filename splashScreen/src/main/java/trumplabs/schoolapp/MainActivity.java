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
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.software.shell.fab.ActionButton;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import BackGroundProcesses.Refresher;
import additionals.Invite;
import additionals.RateAppDialog;
import additionals.SpreadWordDialog;
import baseclasses.MyActionBarActivity;
import fr.castorflex.android.smoothprogressbar.SmoothProgressBar;
import joinclasses.JoinClassDialog;
import joinclasses.JoinClassesContainer;
import notifications.AlarmTrigger;
import profileDetails.ProfilePage;
import trumplab.textslate.R;
import tutorial.ShowcaseCreator;
import utility.Config;
import utility.Queries;
import utility.SessionManager;
import utility.Utility;

/**
 * This Activity shows home page of our app. It contains three fragments outbox, inbox and classrooms.
 */
public class MainActivity extends MyActionBarActivity implements TabListener {
    ViewPager viewpager;
    public static int numTabsToShow; //in case of teacher, no of tabs to show (2 if none joined, 3 otherwise)
    public static TextView tab1Icon;
    public static TextView tab2Icon;
    public static TextView tab3Icon;
    private TextView tabcolor;
    private LinearLayout.LayoutParams params;
    private int screenwidth;
    private static String role = ""; //it should not be null
    public static LinearLayout progressBarLayout;
    SmoothProgressBar mHeaderProgressBar;
    public static MyAdapter myAdapter;

    SessionManager sessionManager;

    private ListView action_menu_list;
    private RelativeLayout action_menu;
    public static List<List<String >> classList;
    public static FloatOptionsAdapter floatOptionsAdapter;
    private boolean isFloatingButtonCliked = false;
    private Typeface lighttypeFace;
    private ActionButton actionButton;
    private static ParseUser user;

    //flag telling whether alarm for event checker has been triggered or not
    static boolean isEventCheckerAlarmTriggered = false;

    public static boolean signUpShowcaseShown = false;
    public static boolean optionsShowcaseShown = false;

    public static int fragmentVisible = 0; //which fragment is visible, changed in viewpager's PageChangeListener

    public static boolean goToOutboxFlag = false; //when returns from Message Composer, go directly to Outbox
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if(Config.SHOWLOG) Log.d("__AA","onCreate MainActivity");

        //testing tutorial TODO comment following line before release
        //TestingUtililty.testingTutorial();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.homepage_layout);

        user = ParseUser.getCurrentUser();

        //check for current user loggedin or not
        if (user == null) {
            Utility.LogoutUtility.logout();
            return;
        }

        // Initialize elements
        viewpager = (ViewPager) findViewById(R.id.pager);
        tabcolor = (TextView) findViewById(R.id.tabcolor);
        tab1Icon = (TextView) findViewById(R.id.tab1Icon);
        tab2Icon = (TextView) findViewById(R.id.tab2Icon);
        tab3Icon = (TextView) findViewById(R.id.tab3Icon);
        progressBarLayout = (LinearLayout) findViewById(R.id.progressBarLayout);

        //todo remove this from layout also as NOT used
        mHeaderProgressBar = (SmoothProgressBar) findViewById(R.id.ptr_progress);

        action_menu = (RelativeLayout) findViewById(R.id.action_menu);
        action_menu_list = (ListView) findViewById(R.id.action_menu_list);

        floatOptionsAdapter = new FloatOptionsAdapter();
        action_menu_list.setAdapter(floatOptionsAdapter);

        Typeface typeFace = Typeface.createFromAsset(getAssets(), "fonts/RobotoCondensed-Regular.ttf");
        lighttypeFace = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Light.ttf");
        tab1Icon.setTypeface(typeFace);
        tab2Icon.setTypeface(typeFace);
        tab3Icon.setTypeface(typeFace);

        //setting list for float button options
        setClassListOptions();

        /*
        Check for app re-installation. In case of reinstallation or delete data appOpeningCount set to zero.
        So, we retrieve all data from server.
         */

        sessionManager = SessionManager.getInstance();

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

        List<String> testingList = new ArrayList<>();

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

        final FragmentManager fragmentmanager = getSupportFragmentManager();

        if(sessionManager.getHasUserJoinedClass()){
            numTabsToShow = 3;
        }
        else{
            numTabsToShow = 2;
        }

        myAdapter = new MyAdapter(fragmentmanager);
        viewpager.setAdapter(myAdapter);

        //checking internet connection
        Utility.isInternetExist();

        //Set the number of pages that should be retained to either side of the current page
        //if(sessionManager.getHasUserJoinedClass())
            viewpager.setOffscreenPageLimit(2);
        //else
        //    viewpager.setOffscreenPageLimit(1);


        //Initializing compose button
        actionButton = (ActionButton) findViewById(R.id.action_button);

        if(role.equals(Constants.TEACHER)) {

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

                    if (classList.size() == 0) {
                        Intent intent = new Intent(MainActivity.this, ComposeMessage.class);
                        intent.putExtra(Constants.ComposeSource.KEY, Constants.ComposeSource.OUTSIDE); //i.e not from within a particular classroom's page
                        startActivity(intent);
                    } else if (classList.size() == 1) {
                        Intent intent = new Intent(MainActivity.this, ComposeMessage.class);
                        intent.putExtra(Constants.ComposeSource.KEY, Constants.ComposeSource.OUTSIDE); //i.e not from within a particular classroom's page
                        intent.putExtra("CLASS_CODE", classList.get(0).get(0));
                        intent.putExtra("CLASS_NAME", classList.get(0).get(1));
                        startActivity(intent);
                    } else {

                        action_menu.setVisibility(View.VISIBLE);
                        action_menu_list.setVisibility(View.VISIBLE);

                        if (isFloatingButtonCliked) {
                            Intent intent = new Intent(MainActivity.this, ComposeMessage.class);
                            intent.putExtra(Constants.ComposeSource.KEY, Constants.ComposeSource.OUTSIDE); //i.e not from within a particular classroom's page
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


            //setting tab click functionality
            tab1Icon.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    highLightTab1();
                    viewpager.setCurrentItem(0);
                    actionButton.setShowAnimation(ActionButton.Animations.JUMP_FROM_DOWN);
                    actionButton.show();
                }
            });
            tab2Icon.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    highLightTab2();
                    viewpager.setCurrentItem(1);
                    actionButton.setShowAnimation(ActionButton.Animations.JUMP_FROM_DOWN);
                    actionButton.show();
                }
            });

            tab3Icon.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    highLightTab3();
                    viewpager.setCurrentItem(2);

                    actionButton.setHideAnimation(ActionButton.Animations.JUMP_TO_DOWN);
                    actionButton.hide();

                }
            });


            if(numTabsToShow == 3) {
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


            //swipe feature implementation
            viewpager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                @Override
                public void onPageSelected(int arg0) {
                    supportInvalidateOptionsMenu();
                    fragmentVisible = arg0;

                    if(Config.SHOWLOG) Log.d("__TEMP__", "onPageSelected " + fragmentVisible + "["+Outbox.responseTutorialShown + "," + Messages.responseTutorialShown + "]");
                    if(role.equals(Constants.TEACHER) && fragmentVisible == 0 && !Outbox.responseTutorialShown && Outbox.myadapter != null){
                        Outbox.myadapter.notifyDataSetChanged(); //so as to show the tutorial if not shown
                    }
                    if(role.equals(Constants.TEACHER) && fragmentVisible == 2 && !Messages.responseTutorialShown && Messages.myadapter != null){
                        Messages.myadapter.notifyDataSetChanged(); //so as to show the tutorial if not shown
                    }
                }

                @Override
                public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

                /*
                scrolling from one tab to other
                 */
                    if(numTabsToShow == 3) {

                        if (position == 0) {
                            params.width = screenwidth * 5 / 13;

                            params.setMargins(positionOffsetPixels * 3 / 13, 0, 0, 0);  // added " positionOffsetPixels/3" for smooth transition
                            tabcolor.setLayoutParams(params);
                            highLightTab1();
                            actionButton.setShowAnimation(ActionButton.Animations.JUMP_FROM_DOWN);
                            actionButton.show();

                        } else if (position == 1) {

                            params.width = screenwidth * 5 / 13;
                            params.setMargins((screenwidth * 3 / 13) + (positionOffsetPixels * 5 / 13), 0, 0, 0); // added " positionOffsetPixels/3" for smooth transition
                            tabcolor.setLayoutParams(params);

                            highLightTab2();

                            if(positionOffset < 0.3) {
                                actionButton.setShowAnimation(ActionButton.Animations.JUMP_FROM_DOWN);
                                actionButton.show();
                            }else {
                                actionButton.setHideAnimation(ActionButton.Animations.JUMP_TO_DOWN);
                                actionButton.hide();
                            }

                        } else {
                            params.width = screenwidth * 5 / 13;
                            params.setMargins((8 * screenwidth / 13), 0, 0, 0);
                            tabcolor.setLayoutParams(params);
                            highLightTab3();
                            if(Messages.myadapter != null){
                                Messages.myadapter.notifyDataSetChanged(); //we're in gui now
                            }

                            actionButton.setHideAnimation(ActionButton.Animations.JUMP_TO_DOWN);
                            actionButton.hide();
                        }
                    }
                    else {
                        params.width = screenwidth  / 2;
                        if (position == 0) {
                            params.setMargins(positionOffsetPixels / 2, 0, 0, 0);  // added " positionOffsetPixels/3" for smooth transition
                            tabcolor.setLayoutParams(params);
                            highLightTab1();
                        } else {
                            params.setMargins((screenwidth / 2), 0, 0, 0); // added " positionOffsetPixels/3" for smooth transition
                            tabcolor.setLayoutParams(params);
                            highLightTab2();
                        }

                    }
                }

                @Override
                public void onPageScrollStateChanged(int arg0) {

                }
            });
        } else {
            LinearLayout parentLayout = (LinearLayout) findViewById(R.id.tabviewer);
            parentLayout.setVisibility(View.GONE);
            actionButton.setVisibility(View.GONE);
        }

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

        //show recommend app dialog
        if(appOpeningCount == 6 || appOpeningCount == 21) {

            FragmentManager fm = getSupportFragmentManager();
            SpreadWordDialog spreadWordDialog = new SpreadWordDialog();
            spreadWordDialog.show(fm, "recommend app");

            sessionManager.setAppOpeningCount();
        }

        //show rate app dialog after using 10 times app
        if(sessionManager.getSignUpAccount() && appOpeningCount == 11) {
            if( user != null) {
                FragmentManager fm = getSupportFragmentManager();
                RateAppDialog rateAppDialog = new RateAppDialog();
                rateAppDialog.show(fm, "rate app");
            }
        }

        FacebookSdk.sdkInitialize(getApplicationContext());

    }


    /**
     * create list of options to show on selecting action button
     * @How : if size <=4 then shows all options
     *          else, It picks up 4 least recently used classes (LRU)
     *          Method to pick : take date of last message sent message from all classes and which class
     *          don't have any sent message, take their creation date, then sort them and display 4 options
     */
    public static void setClassListOptions()
    {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                setClassListOptionsWork();
            }
        };

        new Thread(r).start();
    }

    //called inside worker thread
    static void setClassListOptionsWork(){
        ParseUser currentParseUser = ParseUser.getCurrentUser();
        if(currentParseUser == null){
            return;
        }

        List<List<String>> tempClassList = currentParseUser.getList(Constants.CREATED_GROUPS);

        if(tempClassList == null)
            tempClassList = new ArrayList<>();
        else if(tempClassList.size()>4)
        {
            List<List<String>> newList = new ArrayList<>();

            List<Date> LRU_list = new ArrayList<>();

            for(int i=0; i< tempClassList.size(); i++)
            {
                ParseQuery<ParseObject> query = ParseQuery.getQuery(Constants.SENT_MESSAGES_TABLE);
                query.fromLocalDatastore();
                query.orderByDescending("creationTime");
                query.whereEqualTo("userId", user.getUsername());
                query.whereEqualTo("code", tempClassList.get(i).get(0));

                try {
                    ParseObject obj = query.getFirst();

                    if(obj != null){
                        if(obj.getDate("creationTime") != null)
                            LRU_list.add(obj.getDate("creationTime"));

                    }
                }
                catch (ParseException e) {
                    e.printStackTrace();

                    ParseObject codeGroup = Queries.getCodegroupObject(tempClassList.get(i).get(0));

                    if(codeGroup != null)
                    {
                        if(codeGroup.getCreatedAt()!= null)
                            LRU_list.add(codeGroup.getCreatedAt());
                    }
                }
            }

            //making a new copy of lru_list
            List<Date> lruCopy = new ArrayList<>();
            lruCopy.addAll(LRU_list);

            Collections.sort(LRU_list, new Comparator<Date>() {

                @Override
                public int compare(Date d1, Date d2) {
                    return d2.compareTo(d1);
                }
            });

            for(int i=0; LRU_list.size() >=4 && i<4;i++)
            {
                for(int j=0; j<lruCopy.size(); j++)
                {
                    if(LRU_list.get(i).compareTo(lruCopy.get(j)) == 0 ) {
                        newList.add(0, tempClassList.get(j));
                    }
                }
            }

            tempClassList = newList;
        }

        final List<List<String>> finalClassList = tempClassList;

        if(Application.applicationHandler != null && MainActivity.floatOptionsAdapter != null){
            Application.applicationHandler.post(new Runnable() {
                @Override
                public void run() {
                    MainActivity.classList = finalClassList;
                    if(MainActivity.floatOptionsAdapter != null) {
                        MainActivity.floatOptionsAdapter.notifyDataSetChanged();
                    }
                }
            });
        }
    }

    @Override
    protected void onResume() {

        super.onResume();
        Application.mainActivityVisible = true;
        AppEventsLogger.activateApp(this, Config.FB_APP_ID);
        if(Config.SHOWLOG) Log.d(ComposeMessage.LOGTAG, "MainActivity onResume() : goToOutboxFlag=" + goToOutboxFlag);
        if(goToOutboxFlag){
            if(Config.SHOWLOG) Log.d(ComposeMessage.LOGTAG, "MainActivity onResume() : going to outbox tab");
            goToOutboxFlag = false;
            if(viewpager != null) {
                viewpager.setCurrentItem(0, false); //0th is the outbox tab
            }
        }
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

        if (role.equals(Constants.TEACHER))
            inflater.inflate(R.menu.mainactivity_for_teachers, menu);
        else
            inflater.inflate(R.menu.mainactivity_for_parents, menu);

        ParseUser currentParseUser = ParseUser.getCurrentUser();
        if(currentParseUser == null){
            return true;
        }

        if(role.equals(Constants.TEACHER)){
            //prepare action views for menu items - create and join
            final ImageView createClassActionView = (ImageView) menu.findItem(R.id.createclass).getActionView();
            final ImageView joinClassActionView = (ImageView) menu.findItem(R.id.joinclass).getActionView();


            int pixels = Utility.dpiToPixels(56); //56 dpi is the default spacing between action bar items

            createClassActionView.setMinimumWidth(pixels);
            joinClassActionView.setMinimumWidth(pixels);

            createClassActionView.setImageResource(R.drawable.ic_action_add);
            joinClassActionView.setImageResource(R.drawable.ic_action_import);

            createClassActionView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    FragmentManager fm = getSupportFragmentManager();
                    CreateClassDialog createClassDialog = new CreateClassDialog();
                    createClassDialog.show(fm, "create class");
                }
            });

            joinClassActionView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    FragmentManager fm1 = getSupportFragmentManager();
                    JoinClassDialog joinClassDialog = new JoinClassDialog();
                    joinClassDialog.show(fm1, "Join Class");
                }
            });

            //if(Config.SHOWLOG) Log.d(ShowcaseCreator.LOGTAG, "teacher create: flag=" + signUpShowcaseShown + ", signup flag=" + Constants.IS_SIGNUP);

            if(Application.mainActivityVisible && !MainActivity.signUpShowcaseShown && Constants.IS_SIGNUP && !ShowcaseView.isVisible) {
                MainActivity.signUpShowcaseShown = true;
                if(Config.SHOWLOG) Log.d(ShowcaseCreator.LOGTAG, "teacher create: creating showcase");
                ShowcaseCreator.teacherHighlightCreate(this, createClassActionView, joinClassActionView); //show now
            }

            if(Application.mainActivityVisible && !MainActivity.optionsShowcaseShown && !Constants.IS_SIGNUP && !ShowcaseView.isVisible){//signup flag is not set i.e next time app is opened after signup
                String tutorialId = currentParseUser.getUsername() + Constants.TutorialKeys.OPTIONS;
                SessionManager mgr = SessionManager.getInstance();
                if(mgr.getSignUpAccount() && !mgr.getTutorialState(tutorialId)) { //only if signup account
                    mgr.setTutorialState(tutorialId, true);
                    ShowcaseCreator.highlightOptions(this, joinClassActionView, false);
                }
                MainActivity.optionsShowcaseShown = true;
            }
        }
        else{
            //prepare action views for menu items - join and joined
            final ImageView joinClassActionView = (ImageView) menu.findItem(R.id.joinclass).getActionView();
            final ImageView joinedClassesActionView = (ImageView) menu.findItem(R.id.joinedclasses).getActionView();

            int pixels = Utility.dpiToPixels(56); //56 dpi is the default spacing between action bar items

            joinClassActionView.setMinimumWidth(pixels);
            joinedClassesActionView.setMinimumWidth(pixels);

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

            if(Config.SHOWLOG) Log.d(ShowcaseCreator.LOGTAG, "parent join: flag=" + signUpShowcaseShown + ", signup flag=" + Constants.IS_SIGNUP);
            if(Application.mainActivityVisible && !MainActivity.signUpShowcaseShown && Constants.IS_SIGNUP && !ShowcaseView.isVisible) {
                MainActivity.signUpShowcaseShown = true;
                if(Config.SHOWLOG) Log.d(ShowcaseCreator.LOGTAG, "parent join:  creataing the showcase");
                ShowcaseCreator.parentHighlightJoin(this, joinClassActionView, joinedClassesActionView); //show now
            }

            if(Application.mainActivityVisible && !MainActivity.optionsShowcaseShown && !Constants.IS_SIGNUP && !ShowcaseView.isVisible){//signup flag is not set i.e next time app is opened after signup
                String tutorialId = currentParseUser.getUsername() + Constants.TutorialKeys.OPTIONS;
                SessionManager mgr = SessionManager.getInstance();
                if(mgr.getSignUpAccount() && !mgr.getTutorialState(tutorialId)) { //only if signup account
                    mgr.setTutorialState(tutorialId, true);
                    ShowcaseCreator.highlightOptions(this, joinedClassesActionView, true);
                }
                MainActivity.optionsShowcaseShown = true;
            }
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
                //not used here. set in onCreateOptionsMenu
                FragmentManager fm = getSupportFragmentManager();
                CreateClassDialog createClassDialog = new CreateClassDialog();
                createClassDialog.show(fm,"create class");
                break;

            case R.id.joinclass:
                //not used here. set in onCreateOptionsMenu
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

    public class MyAdapter extends FragmentPagerAdapter {

        public MyAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int arg0) {

            Fragment fragment = null;
            if (role.equals(Constants.TEACHER)) {

                if(numTabsToShow == 3) {
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
            if (role.equals(Constants.TEACHER)) {
                return numTabsToShow;
            } else
                return 1;
        }
    }

    /**
     * setting back button functionality
     * on double click, you can exit from app
     */
    public void onBackPressed() {

        if(action_menu.getVisibility() == View.VISIBLE)
        {
            action_menu.setVisibility(View.GONE);
            action_menu_list.setVisibility(View.GONE);
            isFloatingButtonCliked = false;
        }
        else {

            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
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

                    intent.putExtra(Constants.ComposeSource.KEY, Constants.ComposeSource.OUTSIDE); //i.e not from within a particular classroom's page

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