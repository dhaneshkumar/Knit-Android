package trumplabs.schoolapp;

import java.lang.reflect.Field;

import joinclasses.JoinClassesContainer;

import org.json.JSONException;
import org.json.JSONObject;

import profileDetails.ProfilePage;
import trumplab.textslate.R;
import utility.SessionManager;
import utility.Tools;
import utility.Utility;
import BackGroundProcesses.Inbox;
import BackGroundProcesses.Refresher;
import android.content.Intent;
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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import baseclasses.MyActionBarActivity;

import com.parse.ParseAnalytics;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseUser;

import fr.castorflex.android.smoothprogressbar.SmoothProgressBar;

public class MainActivity extends MyActionBarActivity implements TabListener {
  ViewPager viewpager;
  LinearLayout tabviewer;
  TextView tab1;
  TextView tab2;
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
    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    ParseUser parseObject = ParseUser.getCurrentUser();

    if (parseObject == null)
      Utility.logout();

    progressBarLayout = (LinearLayout) findViewById(R.id.progressBarLayout);
    editLayout = (LinearLayout) findViewById(R.id.editLayout);
    mHeaderProgressBar = (SmoothProgressBar) findViewById(R.id.ptr_progress);


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
    tab1 = (TextView) findViewById(R.id.tab1);
    tab2 = (TextView) findViewById(R.id.tab2);
    tabcolor = (TextView) findViewById(R.id.tabcolor);
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
    
    


    // tab display according to teacher and parent. for parent display only inbox tab
    tab2.setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View v) {
        viewpager.setCurrentItem(1);
      }
    });

    /*
     * if(getIntent().getExtras() != null) {
     * signInFlag=getIntent().getExtras().getBoolean("signInFlag");
     * 
     * if(signInFlag) { ProgressDialog p = ProgressDialog.show(this, "", "Loading Data...", true); }
     * }
     */



    if (role.equals("teacher")) {
      tab1.setVisibility(View.VISIBLE);
      tab1.setOnClickListener(new OnClickListener() {

        @Override
        public void onClick(View v) {
          viewpager.setCurrentItem(0);
        }
      });
      params.width = screenwidth / 2;
    } else {
      LinearLayout parentLayout = (LinearLayout) findViewById(R.id.tabviewer);
      parentLayout.setVisibility(View.GONE);
      actionbar.setTitle("Inbox");
      // params.width = screenwidth;
    }
    params.setMargins(0, 0, 0, 0);
    tabcolor.setLayoutParams(params);

    viewpager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
      @Override
      public void onPageSelected(int arg0) {
        supportInvalidateOptionsMenu();
      }

      @Override
      public void onPageScrolled(int arg0, float arg1, int arg2) {
        if (arg0 != 1) {
          params.setMargins(arg2 / 2, 0, 0, 0);
          tabcolor.setLayoutParams(params);
        } else if (arg0 == 1) {
          params.setMargins(screenwidth / 2, 0, 0, 0);
          tabcolor.setLayoutParams(params);
        }
        Log.d("trumplab", "" + arg0 + " " + arg2);
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
    
    MenuInflater inflater=getMenuInflater();

    if (role.equals("teacher"))
      inflater.inflate(R.menu.mainactivity_for_teachers,menu);
    else
      inflater.inflate(R.menu.mainactivity_for_parents,menu);

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
        startActivity(new Intent(this, JoinClassesContainer.class ));
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
      if(role.equals("teacher"))
      {
      
      switch (arg0) {
        case 0:
          fragment = new Classrooms();
          break;
        case 1:

          fragment = new Messages ();
          break;
        default:
          break;
      }
      }
      else
        fragment = new Messages ();
        
      return fragment;
    }

    @Override
    public int getCount() {
      if (role.equals("teacher")) {
        return 2;
      } else
        return 1;
    }
  }

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



}
