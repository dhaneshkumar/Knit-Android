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
public class Secure extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layoutview = inflater.inflate(R.layout.tutorial_secure, container, false);
        return layoutview;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        //setting light font
        TextView secure_skip = (TextView) getActivity().findViewById(R.id.secure_skip);
        LinearLayout secure_next = (LinearLayout) getActivity().findViewById(R.id.secure_next);

        secure_next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fragmentmanager = getActivity().getSupportFragmentManager();
                if(TeacherTutorial.myAdapter == null)
                    TeacherTutorial.myAdapter = new TeacherTutorial.MyAdapter(fragmentmanager);
                TeacherTutorial.viewpager.setAdapter(TeacherTutorial.myAdapter);
                TeacherTutorial.viewpager.setCurrentItem(2);
            }
        });

        secure_skip.setOnClickListener(new View.OnClickListener() {
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
