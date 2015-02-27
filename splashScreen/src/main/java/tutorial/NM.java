package tutorial;

import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import trumplab.textslate.R;

/**
 * Created by Dhanesh on 1/17/2015.
 */
public class NM extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layoutview = inflater.inflate(R.layout.tutorial_nm, container, false);
        return layoutview;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        //setting light font
        TextView heading = (TextView) getActivity().findViewById(R.id.heading_nm);
        Typeface typeFace = Typeface.createFromAsset(getActivity().getAssets(), "fonts/Roboto-Light.ttf");
        heading.setTypeface(typeFace);


        LinearLayout back = (LinearLayout) getActivity().findViewById(R.id.nm_back);
        LinearLayout next = (LinearLayout) getActivity().findViewById(R.id.nm_next);

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fragmentmanager = getActivity().getSupportFragmentManager();
                ParentTutorial.viewpager.setAdapter(new ParentTutorial.MyAdapter(fragmentmanager));
                ParentTutorial.viewpager.setCurrentItem(1);
            }
        });

        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fragmentmanager = getActivity().getSupportFragmentManager();
                ParentTutorial.viewpager.setAdapter(new ParentTutorial.MyAdapter(fragmentmanager));
                ParentTutorial.viewpager.setCurrentItem(3);
            }
        });
    }
}
