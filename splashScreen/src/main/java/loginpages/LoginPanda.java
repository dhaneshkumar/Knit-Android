package loginpages;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.TextView;

import trumplab.textslate.R;
import trumplabs.schoolapp.MainActivity;

/**
 * Created by dhanno on 21/1/15.
 */
public class LoginPanda extends ActionBarActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_panda);


        TextView heading = (TextView) findViewById(R.id.textView);
        Typeface typeFace = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Light.ttf");
        heading.setTypeface(typeFace);

        TextView next = (TextView) findViewById(R.id.next);

        //sign up button clicked..
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                    Intent intent = new Intent(LoginPanda.this, MainActivity.class);
                    startActivity(intent);
            }
        });


    }
}
