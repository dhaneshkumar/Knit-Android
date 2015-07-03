package tutorial;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import loginpages.PhoneSignUpName;
import trumplab.textslate.R;
import trumplabs.schoolapp.Constants;

/**
 * Created by Dhanesh on 1/17/2015.
 */
public class PNM extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layoutview = inflater.inflate(R.layout.tutorial_pnm, container, false);
        return layoutview;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        TextView pnm_skip = (TextView) getActivity().findViewById(R.id.pnm_skip);
        LinearLayout pnm_next = (LinearLayout) getActivity().findViewById(R.id.pnm_next);

        TextView pnm_heading = (TextView) getActivity().findViewById(R.id.pnm_heading);
        TextView pnm_details = (TextView) getActivity().findViewById(R.id.pnm_details);
        ImageView pnm_image = (ImageView) getActivity().findViewById(R.id.pnm_image);

        final String role = getActivity().getIntent().getExtras().getString("role");

        if(role.equals(Constants.PARENT))
        {
            pnm_heading.setText("Never miss Anything");
            pnm_details.setText("Forgot to check diary? No problem, find all information with a click.");
            pnm_image.setBackgroundDrawable(getResources().getDrawable(R.drawable.tut_snm));
        }
        else if(role.equals(Constants.STUDENT))
        {
            pnm_heading.setText("Keep Track");
            pnm_image.setBackgroundDrawable(getResources().getDrawable(R.drawable.tut_snm));
            pnm_details.setText("Forgot about an assignment deadline? Don’t fret, always stay updated with reminders on Knit.");
        }


        pnm_next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fragmentmanager = getActivity().getSupportFragmentManager();


                if(role.equals(Constants.TEACHER)) {
                    if (TeacherTutorial.myAdapter == null)
                        TeacherTutorial.myAdapter = new TeacherTutorial.MyAdapter(fragmentmanager);
                    TeacherTutorial.viewpager.setAdapter(TeacherTutorial.myAdapter);
                    TeacherTutorial.viewpager.setCurrentItem(3);
                }else
                {
                    if (ParentTutorial.myAdapter == null)
                        ParentTutorial.myAdapter = new ParentTutorial.MyAdapter(fragmentmanager);
                    ParentTutorial.viewpager.setAdapter(ParentTutorial.myAdapter);
                    ParentTutorial.viewpager.setCurrentItem(3);
                }
            }
        });

        pnm_skip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(getActivity(), PhoneSignUpName.class);
                intent.putExtra("role", role);
                startActivity(intent);
            }
        });

    }
}
