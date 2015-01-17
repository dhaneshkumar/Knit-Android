package tutorial;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.List;

import loginpages.Signup1Class;
import loginpages.signUp0Class;
import trumplab.textslate.R;
import trumplabs.schoolapp.Constants;

/**
 * Fragment showing Secure icon
 *
 * Created by Dhanesh on 1/17/2015.
 */
public class Free extends Fragment {
    LinearLayout progressLayout;
    LinearLayout loginlayout;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layoutview = inflater.inflate(R.layout.tutorial_free, container, false);
        return layoutview;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        TextView signup = (TextView) getActivity().findViewById(R.id.signup);
        progressLayout = (LinearLayout) getActivity().findViewById(R.id.progresslayout);
        loginlayout = (LinearLayout) getActivity().findViewById(R.id.loginlayout);

        final String role = getActivity().getIntent().getExtras().getString("role");

        //setting light font
        TextView heading = (TextView) getActivity().findViewById(R.id.heading_free);
        Typeface typeFace = Typeface.createFromAsset(getActivity().getAssets(), "fonts/Roboto-Light.ttf");
        heading.setTypeface(typeFace);

        //sign up button clicked..
        signup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(role.equals(Constants.TEACHER))
                {

                    loginlayout.setVisibility(View.GONE);
                    progressLayout.setVisibility(View.VISIBLE);

                    GetSchools getSchools = new GetSchools();
                    getSchools.execute();
                }
                else
                {
                    Intent intent = new Intent(getActivity(),Signup1Class.class);
                    intent.putExtra("role", role);
                    startActivity(intent);
                }
            }
        });




    }



    /**
     * Retrieve school list from server
     */
    private class GetSchools extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            ParseQuery<ParseObject> query = ParseQuery.getQuery("SCHOOLS");
            query.orderByAscending("school_name");
            try {
                List<ParseObject> schoolList = query.find();
                if (schoolList != null)
                    ParseObject.pinAll(schoolList);
            } catch (ParseException e) {
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {

            if(getActivity() != null) {
                Intent intent = new Intent(getActivity(), Signup1Class.class);
                intent.putExtra("role", Constants.TEACHER);
                startActivity(intent);
            }

        }
    }
}
