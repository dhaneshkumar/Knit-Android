package tutorial;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import loginpages.PhoneSignUpName;
import trumplab.textslate.R;
import trumplabs.schoolapp.Constants;

/**
 * Fragment showing free icons
 *
 * Created by Dhanesh on 1/17/2015.
 */
public class Free extends Fragment {
    LinearLayout progressLayout;
    RelativeLayout loginlayout;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layoutview = inflater.inflate(R.layout.tutorial_free, container, false);
        return layoutview;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        TextView free_done = (TextView) getActivity().findViewById(R.id.free_done);
        final String role = getActivity().getIntent().getExtras().getString("role");
        TextView free_details = (TextView) getActivity().findViewById(R.id.free_details);

        if(!role.equals(Constants.TEACHER))
            free_details.setText("Knit is absolutely free.");


        free_done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(getActivity(), PhoneSignUpName.class);
                intent.putExtra("role", role);
                startActivity(intent);
            }
        });

    }
}
