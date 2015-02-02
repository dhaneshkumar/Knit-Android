package joinclasses;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.MenuItem;

import baseclasses.MyActionBarActivity;
import joinclasses.JoinedClasses.ViewPagerCommunicator;
import trumplab.textslate.R;
import trumplabs.schoolapp.MainActivity;
import utility.Utility;

public class JoinClassesContainer extends MyActionBarActivity implements ViewPagerCommunicator {
  android.support.v7.app.ActionBar actionbar;
  public ViewPager viewpager;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.joiningclasses_viewpager_layout);
    actionbar = getSupportActionBar();
    viewpager = (ViewPager) findViewById(R.id.joiningclassespager);
    actionbar.setDisplayHomeAsUpEnabled(true);
    viewpager.setAdapter(new MyAdapter(getSupportFragmentManager()));
    viewpager.setCurrentItem(getIntent().getIntExtra("VIEWPAGERINDEX", 0));
  }

  @Override
  protected void onResume() {
    super.onResume();
    Utility.isInternetOn(this);
    
   /* Intent intent = getIntent();
    finish();
    startActivity(intent);*/
  }

  /*@Override
  protected void onRestart() {
    super.onRestart();
    
    Utility.toast("restarted");
    Intent intent = getIntent();
    finish();
    startActivity(intent);
  }*/
  
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        onBackPressed();
        break;
      case R.id.addclass:
        viewpager.setCurrentItem(1);
        break;
      case R.id.joinedclassesmenu:
        viewpager.setCurrentItem(0);
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
      supportInvalidateOptionsMenu();
      switch (arg0) {
        case 0:
          fragment = new JoinedClasses();
          break;
        case 1:
          fragment = new JoinClass();
          break;
        default:
          break;
      }
      return fragment;
    }

    @Override
    public int getCount() {
      return 2;
    }
  }

  @Override
  public void viewPagerSet(int i) {
    viewpager.setCurrentItem(i);
  }



  @Override
  public void onBackPressed() {
    // Write your code here
    Intent intent = new Intent(this, MainActivity.class);
    startActivity(intent);

    super.onBackPressed();
  }
}
