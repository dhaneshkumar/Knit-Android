package tutorial;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;

import baseclasses.MyActionBarActivity;
import trumplab.textslate.R;

/**
 * Created by Dhanesh on 1/17/2015.
 */
public class ParentTutorial extends MyActionBarActivity{
    public static ViewPager viewpager;
    public static MyAdapter myAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tutorial);


        viewpager = (ViewPager) findViewById(R.id.pager);

        getSupportActionBar().hide();
        myAdapter = new MyAdapter(getSupportFragmentManager());
        viewpager.setAdapter(myAdapter);
        viewpager.setOffscreenPageLimit(3);
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
