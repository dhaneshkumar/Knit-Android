package joinclasses;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import baseclasses.MyActionBarActivity;
import trumplab.textslate.R;
import trumplabs.schoolapp.Classrooms;
import trumplabs.schoolapp.MainActivity;
import utility.Utility;

public class JoinClassesContainer extends MyActionBarActivity  {
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
  }

  
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        onBackPressed();
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
      supportInvalidateOptionsMenu();
      switch (arg0) {
        case 0:
          fragment = new Classrooms();
          break;
        default:
          break;
      }
      return fragment;
    }

    @Override
    public int getCount() {
      return 1;
    }
  }


  @Override
  public void onBackPressed() {
    // Write your code here
    Intent intent = new Intent(this, MainActivity.class);
    startActivity(intent);

    super.onBackPressed();
  }
}
