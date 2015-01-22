package tutorial;

import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.parse.ParseUser;

import trumplab.textslate.R;

/**
 * Created by Dhanesh on 1/17/2015.
 */
public class OneWay extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layoutview = inflater.inflate(R.layout.tutorial_oneway, container, false);
        return layoutview;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        //setting light font
        TextView heading = (TextView) getActivity().findViewById(R.id.heading_oneway);
        Typeface typeFace = Typeface.createFromAsset(getActivity().getAssets(), "fonts/Roboto-Light.ttf");
        heading.setTypeface(typeFace);
    }
}