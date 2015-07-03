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

/**
 * Created by Dhanesh on 1/17/2015.
 */
public class ConnectClass extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layoutview = inflater.inflate(R.layout.tutorial_connect_class, container, false);
        return layoutview;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        TextView connect_skip = (TextView) getActivity().findViewById(R.id.connect_skip);


        LinearLayout connect_next = (LinearLayout) getActivity().findViewById(R.id.connect_next);

        connect_next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fragmentmanager = getActivity().getSupportFragmentManager();
                if (ParentTutorial.myAdapter == null)
                    ParentTutorial.myAdapter = new ParentTutorial.MyAdapter(fragmentmanager);
                ParentTutorial.viewpager.setAdapter(ParentTutorial.myAdapter);
                ParentTutorial.viewpager.setCurrentItem(1);
            }
        });


        connect_skip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String role = getActivity().getIntent().getExtras().getString("role");

                Intent intent = new Intent(getActivity(), PhoneSignUpName.class);
                intent.putExtra("role", role);
                startActivity(intent);
            }
        });
    }
}
