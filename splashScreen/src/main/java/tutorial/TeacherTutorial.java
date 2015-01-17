package tutorial;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.view.View;

import trumplab.textslate.R;
import trumplabs.schoolapp.ClassMembers;
import trumplabs.schoolapp.ClassMsg;

/**
 * Created by Dhanesh on 1/17/2015.
 */
public class TeacherTutorial extends ActionBarActivity{
    ViewPager viewpager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tutorial);


        viewpager = (ViewPager) findViewById(R.id.pager);

        getSupportActionBar().hide();
        viewpager.setAdapter(new MyAdapter(getSupportFragmentManager()));

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
                    fragment = new OneWay();
                    break;
                case 1:
                    fragment = new Secure();
                    break;
                case 2:
                    fragment = new PNM();
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
