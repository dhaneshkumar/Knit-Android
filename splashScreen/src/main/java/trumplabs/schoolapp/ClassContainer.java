package trumplabs.schoolapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.MenuItem;

import baseclasses.MyActionBarActivity;
import trumplab.textslate.R;
import utility.Utility;

public class ClassContainer extends MyActionBarActivity {
	android.support.v7.app.ActionBar actionbar;
	ViewPager viewpager;
	public static String classuid;
	public static String className;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.classroom_layout);
		if(getIntent().getExtras()!=null){
		classuid = getIntent().getExtras().getString("selectedclass");
		className = getIntent().getExtras().getString("selectedclassName");
		
		//Utility.toast("selectd class : " + classuid+"  " + className);
		}
		actionbar = getSupportActionBar();
		viewpager = (ViewPager) findViewById(R.id.classroompager);
		actionbar.setDisplayHomeAsUpEnabled(true);
		viewpager.setAdapter(new MyAdapter(getSupportFragmentManager()));
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
		case R.id.memberlist:
			viewpager.setCurrentItem(1);
			break;
		case R.id.classmsgmenu:
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
			//  Log.d("class container", "reaching here..");
			  //Utility.toast("selectd class : " + classuid+"  " + className);
	        
				fragment = new ClassMsg();
				break;
			case 1:
				fragment = new ClassMembers();
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
    protected void onRestart() {
        super.onRestart();  // Always call the superclass method first

        if(classuid == null)
        {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        }
        // Activity being restarted from stopped state
    }

}
