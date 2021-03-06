package loginpages;

import android.content.Intent;
import android.graphics.Typeface;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import baseclasses.MyActionBarActivity;
import trumplab.textslate.R;
import trumplabs.schoolapp.Constants;
import tutorial.ParentTutorial;
import tutorial.TeacherTutorial;

/**
 * Created by Dhanesh on 1/12/2015.
 */
public class Signup extends MyActionBarActivity {

    protected void onCreate(android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);



        setContentView(R.layout.signup);
        getSupportActionBar().hide();

        //Intializing variables
        LinearLayout teacher = (LinearLayout) findViewById(R.id.teacherLayout);
        LinearLayout parent = (LinearLayout) findViewById(R.id.parent_layout);
        LinearLayout student = (LinearLayout) findViewById(R.id.student_layout);
        TextView login = (TextView) findViewById(R.id.logiinView);
       /* progressLayout = (LinearLayout) findViewById(R.id.progresslayout);
        loginlayout = (LinearLayout) findViewById(R.id.loginlayout);*/
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
                Intent intent = new Intent(Signup.this, ParentTutorial.class);
                intent.putExtra("role", Constants.PARENT);
                startActivity(intent);
            }
        });

        student.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Signup.this, ParentTutorial.class);
                intent.putExtra("role", Constants.STUDENT);
                startActivity(intent);
            }
        });


        teacher.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                Intent intent = new Intent(Signup.this, TeacherTutorial.class);
                intent.putExtra("role", Constants.TEACHER);
                startActivity(intent);
            }
        });

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Signup.this, PhoneLoginPage.class);
                intent.putExtra("login", true); //just a dummy extra for proper behaviour of empty text on forward, filled text on back
                //Intent intent = new Intent(Signup.this, Test.class);
                startActivity(intent);
            }
        });




    }




    @Override
    public void onBackPressed() {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);

    }
}