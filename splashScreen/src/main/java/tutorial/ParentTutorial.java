package tutorial;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;

import baseclasses.MyActionBarActivity;
import trumplab.textslate.R;
import trumplabs.schoolapp.Constants;

/**
 * Created by Dhanesh on 1/17/2015.
 */
public class ParentTutorial extends MyActionBarActivity{
    public static ViewPager viewpager;
    static String role;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tutorial);


        viewpager = (ViewPager) findViewById(R.id.pager);

        getSupportActionBar().hide();
        viewpager.setAdapter(new MyAdapter(getSupportFragmentManager()));
        viewpager.setOffscreenPageLimit(3);

        role = getIntent().getExtras().getString(Constants.ROLE);
    }


    public static class MyAdapter extends FragmentPagerAdapter {

        public MyAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int arg0) {
            Fragment fragment = null;
            switch (arg0) {
                case 0:
                    fragment = new ConnectClass();
                    break;
                case 1:
                    fragment = new NoChaos();
                    break;
                case 2:

                    if(role.equals(Constants.PARENT))
                        fragment = new NM();
                    else
                        fragment = new SNM();
                    break;
                case 3:
                    fragment = new Free();
                    break;
                default:
                    break;
            }
            return fragment;
        }

        @Override
        public int getCount() {
            return 4;
        }
    }

}
