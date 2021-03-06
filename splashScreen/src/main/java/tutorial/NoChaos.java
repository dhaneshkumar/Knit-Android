package tutorial;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import loginpages.PhoneSignUpName;
import trumplab.textslate.R;
import trumplabs.schoolapp.Constants;

/**
 * Created by Dhanesh on 1/17/2015.
 */
public class NoChaos extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layoutview = inflater.inflate(R.layout.tutorial_no_chaos, container, false);
        return layoutview;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);


        TextView no_chaos_skip = (TextView) getActivity().findViewById(R.id.no_chaos_skip);
        LinearLayout no_chaos_next = (LinearLayout) getActivity().findViewById(R.id.no_chaos_next);

        TextView no_chaos_content = (TextView) getActivity().findViewById(R.id.no_chaos_content);



        final String role = getActivity().getIntent().getExtras().getString("role");

        if(role.equals(Constants.PARENT))
            no_chaos_content.setText("Parents can either like the message or express confusion. That way, never lose track of real information.");
        else if(role.equals(Constants.STUDENT))
            no_chaos_content.setText("Students can either like the message or express confusion. That way, never lose track of real information.");

        no_chaos_skip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), PhoneSignUpName.class);
                intent.putExtra("role", role);
                startActivity(intent);
            }
        });

        no_chaos_next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fragmentmanager = getActivity().getSupportFragmentManager();
                if (ParentTutorial.myAdapter == null)
                    ParentTutorial.myAdapter = new ParentTutorial.MyAdapter(fragmentmanager);
                ParentTutorial.viewpager.setAdapter(ParentTutorial.myAdapter);
                ParentTutorial.viewpager.setCurrentItem(2);
            }
        });

    }
}
