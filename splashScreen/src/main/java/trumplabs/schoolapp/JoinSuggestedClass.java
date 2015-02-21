package trumplabs.schoolapp;

import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;

import baseclasses.MyActionBarActivity;
import joinclasses.JoinedHelper;
import trumplab.textslate.R;

/**
 * Created by ashish on 21/2/15.
 */
public class JoinSuggestedClass extends MyActionBarActivity {
    TextView classNameView;
    TextView teacherNameView;
    TextView classCodeView;
    Button joinButton;
    TextView ignoreView;

    String teacherName;
    String className;
    String classCode;

    protected void onCreate(android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.join_suggested_class_layout);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        classNameView = (TextView) findViewById(R.id.classname);
        classCodeView = (TextView) findViewById(R.id.classcode);
        teacherNameView = (TextView) findViewById(R.id.teacher);
        joinButton = (Button) findViewById(R.id.join_button);
        ignoreView = (TextView) findViewById(R.id.ignore);

        teacherName = getIntent().getExtras().getString("teacherName");
        className = getIntent().getExtras().getString("className");
        classCode = getIntent().getExtras().getString("classCode");

        teacherNameView.setText("Teacher : " + teacherName);
        classNameView.setText(className);
        classCodeView.setText("Code : " + classCode);

        ignoreView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ParseUser user = ParseUser.getCurrentUser();
                if(user != null){
                    List<List<String>> removedList = ParseUser.getCurrentUser().getList(Constants.REMOVED_GROUPS);

                    if (removedList == null) {
                        removedList = new ArrayList<List<String>>();
                    }

                    for(int i=0; i<removedList.size(); i++){
                        Log.d("DEBUG_JOIN_SUGGESTED", "removed list " + i + " " + removedList.get(i).get(0));
                    }

                    ArrayList<String> removedGroup = new ArrayList<String>();
                    removedGroup.add(classCode);
                    removedGroup.add(className);
                    removedList.add(removedGroup);
                    user.put(Constants.REMOVED_GROUPS, removedList);
                    user.getCurrentUser().saveEventually();

                    // updating suggestions adapter in Classrooms fragment
                    Classrooms.suggestedGroups = JoinedHelper.getSuggestionList(user.getUsername());

                    if(Classrooms.suggestedClassAdapter != null)
                        Classrooms.suggestedClassAdapter.notifyDataSetChanged();
                }
                finish(); //close the activity
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}