package loginpages;

import android.content.Intent;
import android.graphics.Typeface;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.facebook.CallbackManager;
import com.parse.LogInCallback;
import com.parse.ParseException;
import com.parse.ParseFacebookUtils;
import com.parse.ParseUser;

import java.util.Collection;

import baseclasses.MyActionBarActivity;
import trumplab.textslate.R;
import trumplabs.schoolapp.Constants;
import tutorial.ParentTutorial;
import tutorial.TeacherTutorial;

/**
 * Created by Dhanesh on 1/12/2015.
 */
public class Signup extends MyActionBarActivity {
    CallbackManager callbackManager;

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


        Button loginButton = (Button) findViewById(R.id.login_button);

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


        // Callback registration
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Collection<String> permissions = null;

                ParseFacebookUtils.logInWithReadPermissionsInBackground(Signup.this, permissions, new LogInCallback() {
                    @Override
                    public void done(ParseUser user, ParseException err) {
                        if (user == null) {
                            Log.d("MyApp", "Uh oh. The user cancelled the Facebook login.");
                        } else if (user.isNew()) {
                            Log.d("MyApp", "User signed up and logged in through Facebook!");
                        } else {
                            Log.d("MyApp", "User logged in through Facebook!");
                        }
                    }
                });
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        ParseFacebookUtils.onActivityResult(requestCode, resultCode, data);
    }


    @Override
    public void onBackPressed() {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);

    }
}