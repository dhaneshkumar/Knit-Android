package loginpages;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.List;

import trumplab.textslate.R;
import trumplabs.schoolapp.Constants;

/**
 * Created by Dhanesh on 1/12/2015.
 */
public class Signup extends ActionBarActivity {
    int backCount =0 ;
    LinearLayout progressLayout;
    LinearLayout loginlayout;

    protected void onCreate(android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signup);

        getSupportActionBar().hide();

        //Intializing variables
        LinearLayout teacher = (LinearLayout) findViewById(R.id.teacherLayout);
        LinearLayout parent = (LinearLayout) findViewById(R.id.parent_layout);
        LinearLayout student = (LinearLayout) findViewById(R.id.student_layout);
        TextView login = (TextView) findViewById(R.id.logiinView);
        progressLayout = (LinearLayout) findViewById(R.id.progresslayout);
        loginlayout  =(LinearLayout) findViewById(R.id.loginlayout);
        TextView signup = (TextView) findViewById(R.id.signup);
        TextView ttext = (TextView) findViewById(R.id.ttext);
        TextView ptext = (TextView) findViewById(R.id.ptext);
        TextView stext = (TextView) findViewById(R.id.stext);
        TextView member = (TextView) findViewById(R.id.member);


        //Setting the font
        Typeface typeFace = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Light.ttf");
        Typeface typeFaceItalic = Typeface.createFromAsset(getAssets(), "fonts/Roboto-LightItalic.ttf");
        login.setTypeface(typeFace);
        signup.setTypeface(typeFace);
        ttext.setTypeface(typeFace);
        ptext.setTypeface(typeFace);
        stext.setTypeface(typeFace);
        member.setTypeface(typeFaceItalic);



        //parent button clicked
        parent.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent=new Intent(Signup.this, Signup1Class.class);
                intent.putExtra("role", Constants.PARENT);
                startActivity(intent);
            }
        });


        student.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                Intent intent=new Intent(Signup.this,Signup1Class.class);
                intent.putExtra("role", Constants.STUDENT);
                startActivity(intent);
            }
        });

        teacher.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                backCount =0;
                loginlayout.setVisibility(View.GONE);
                progressLayout.setVisibility(View.VISIBLE);

                //loading school list in background
                GetSchools getSchools = new GetSchools();
                getSchools.execute();


            }
        });


        login.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                Intent intent=new Intent(Signup.this,LoginPage.class);
                startActivity(intent);
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

           /* loginlayout.setVisibility(View.VISIBLE);
            progressLayout.setVisibility(View.GONE);*/

            Intent intent=new Intent(Signup.this,Signup1Class.class);
            intent.putExtra("role", Constants.TEACHER);
            startActivity(intent);

            loginlayout.setVisibility(View.VISIBLE);
            progressLayout.setVisibility(View.GONE);
        }


    }


    @Override
    public void onBackPressed() {

        if (backCount == 1) {

            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            startActivity(intent);
        } else {
            backCount++;

            loginlayout.setVisibility(View.VISIBLE);
            progressLayout.setVisibility(View.GONE);

        }
    }



}
