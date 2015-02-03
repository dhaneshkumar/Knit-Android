package joinclasses;

import library.UtilString;
import notifications.AlarmReceiver;
import notifications.NotificationGenerator;
import trumplab.textslate.R;
import trumplabs.schoolapp.Application;
import trumplabs.schoolapp.Constants;
import trumplabs.schoolapp.InviteTeacher;
import trumplabs.schoolapp.Messages;
import utility.Popup;
import utility.Queries;
import utility.Queries2;
import utility.SessionManager;
import utility.Tools;
import utility.Utility;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParsePush;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * This fragment show layout to enter the class-code
 */
public class JoinClass extends Fragment {
    private EditText classCode;
    private String groupName = "";
    private LinearLayout joinLayout;
    private String code = "";
    private ParseInstallation pi;
    private Point p;
    private int height;
    private LinearLayout progressBarLayout;
    private LinearLayout editProfileLayout;
    private String role;
    private String childName;
    private String userId;
    private Queries textQuery;
    private Queries2 memberQuery;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layoutview = inflater.inflate(R.layout.joinclass_layout, container, false);
        return layoutview;
    }
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // retrieving userID
        ParseUser user = ParseUser.getCurrentUser();
        if (user == null)
            {Utility.logout(); return;}

        //initializing variables
        textQuery = new Queries();
        userId = user.getUsername();
        childName = user.getString("name");
        role = user.getString(Constants.ROLE);
        new Queries();
        Button join_btn = (Button) getActivity().findViewById(R.id.Join_btn);
        classCode = (EditText) getActivity().findViewById(R.id.classcodevalue);
        joinLayout = (LinearLayout) getActivity().findViewById(R.id.joinlayout);
        progressBarLayout = (LinearLayout) getActivity().findViewById(R.id.progresslayout);
        editProfileLayout = (LinearLayout) getActivity().findViewById(R.id.joinlayout);
        memberQuery= new Queries2();
        final ImageView help = (ImageView) getActivity().findViewById(R.id.help);


        //removing focus from editText and setting it to parent layout
        getActivity().findViewById(R.id.joinLinearLayout).requestFocus();


        // Get the x, y location and store it in the location[] array
        // location[0] = x, location[1] = y.
        ViewTreeObserver vto = help.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int[] location = new int[2];
                help.getLocationOnScreen(location);
                height = help.getHeight();

                // Initialize the Point with x, and y positions
                p = new Point();
                p.x = location[0];
                p.y = location[1];
            }
        });

        //changing button's text depending on user's role
        if(role.equals(Constants.STUDENT))
            join_btn.setText("Join");

        final String txt =
                "You need a class-code to join the class-room. If you don't have any, ask to teacher for it.";

        //setting help button clicked functionality
        help.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (p != null) {
                    Popup popup = new Popup();
                    popup.showPopup(getActivity(), p, true, -300, txt, height, 15, 400);
                    InputMethodManager inputMethodManager =
                            (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);

                    //hiding keyboard
                    if (getActivity().getCurrentFocus() != null) {
                        inputMethodManager.hideSoftInputFromWindow(getActivity().getCurrentFocus()
                                .getApplicationWindowToken(), 0);
                    }
                }
            }
        });

        //Setting join button clicked button functionality
        join_btn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!UtilString.isBlank(classCode.getText().toString())) {

                    code = classCode.getText().toString().trim();

                    //validating code format
                    if (code.length() != 7) {
                        Utility.toast("Enter Correct Class Code");
                        return;
                    }

                    //hiding keyboard
                    Tools.hideKeyboard(getActivity());

                    //checking for internet connection
                    if (Utility.isInternetOn(getActivity())) {

                        /*
                        If user is not a student then it will goto next activity to take input child-name else
                        It will directly join this classroom.
                         */
                        if(! role.equals(Constants.STUDENT)) {
                            //going to next activity
                            Intent intent = new Intent(getActivity(), AddChildToClass.class);
                            intent.putExtra("code", code);
                            getActivity().overridePendingTransition(R.anim.animation_leave, R.anim.animation_enter);
                            startActivityForResult(intent, 0);
                        }
                        else {

                            //joining this class, since user is a student
                            AddChild_Background rcb = new AddChild_Background();
                            rcb.execute();
                            progressBarLayout.setVisibility(View.VISIBLE);
                            editProfileLayout.setVisibility(View.GONE);
                        }
                    }
                    else {
                        Utility.toast("Check your Internet connection");
                    }
                }
            }
        });

        //Setting invite button functionality
        Button inviteButton = (Button) getActivity().findViewById(R.id.inviteButton);
        inviteButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                //go to invite teacher activity
                Intent intent = new Intent(getActivity(), InviteTeacher.class);
                startActivity(intent);
            }
        });
    }

    /**
     * joining class using code in background
     */
    class AddChild_Background extends AsyncTask<Void, Void, Boolean> {
        boolean classExist; //flag to test whether class already added in user's joined-group or not

        @Override
        protected Boolean doInBackground(Void... params) {
            if (userId != null && childName != null) {

                /*
                * Retrieving user details
                */
                childName = childName.trim();
                childName = UtilString.parseString(childName);

                classExist = false; //setting flag initially false
                /*
                * Change first letter to caps
                */
                SessionManager session = new SessionManager(Application.getAppContext());
                session.addChildName(childName);
                ParseUser user = ParseUser.getCurrentUser();
                if (user != null) {

                    int result = JoinedHelper.joinClass(code, childName, false);


                    if (result == 1)
                        return true;      //successfully joined class
                    else if (result == 2) {
                        classExist = true;    //already joined
                        return false;
                    } else
                        return false;       //failed to join

                } else
                    return false;
            }

            return false;
        }
        @Override
        protected void onPostExecute(Boolean result) {

            if (result) {
                Utility.toast("ClassRoom Added.");
                if( Messages.myadapter != null)
                    Messages.myadapter.notifyDataSetChanged();
                Intent intent = new Intent(getActivity(), joinclasses.JoinClassesContainer.class);
                startActivity(intent);
            } else {
                if (classExist) {
                    Utility.toast("Class room Already added.");
                } else
                    Utility.toast("Entered Class doesn't exist");
                progressBarLayout.setVisibility(View.GONE);
                editProfileLayout.setVisibility(View.VISIBLE);
            }
        }
    }
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu6, menu);
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }
}