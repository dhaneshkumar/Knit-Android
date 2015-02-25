package joinclasses;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseUser;

import java.util.ArrayList;

import library.UtilString;
import trumplab.textslate.R;
import trumplabs.schoolapp.Application;
import trumplabs.schoolapp.Classrooms;
import trumplabs.schoolapp.Constants;
import trumplabs.schoolapp.InviteTeacher;
import trumplabs.schoolapp.MainActivity;
import trumplabs.schoolapp.Messages;
import utility.Popup;
import utility.Queries;
import utility.SessionManager;
import utility.Utility;

/**
 * This class shows popup to join a classroom
 */
public class JoinClassDialog extends DialogFragment {
    private Dialog dialog;
    private EditText codeET;
    private EditText childET;
    private ImageView codeHelp;
    private ImageView childHelp;
    private TextView joinButton;
    private TextView inviteButton;
    private String code;
    private String role;
    private LinearLayout progressLayout;
    private LinearLayout contentLayout;
    private String childName;
    private String userId;
    private Point p;
    private int height;
    private boolean callerflag= false;  // true if its called from "join suggestion" class.
    private Queries query;


    public Dialog onCreateDialog(Bundle savedInstanceState) {

        //Creating new dialog box
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View view =
                getActivity().getLayoutInflater().inflate(R.layout.join_class, null);
        builder.setView(view);
        dialog = builder.create();
        dialog.show();


        //Initializing variables
        codeET = (EditText) view.findViewById(R.id.code);
        childET = (EditText) view.findViewById(R.id.child);
        codeHelp = (ImageView) view.findViewById(R.id.nameHelp);
        childHelp = (ImageView) view.findViewById(R.id.childHelp);
        joinButton = (TextView) view.findViewById(R.id.join);
        inviteButton = (TextView) view.findViewById(R.id.invite);
        progressLayout = (LinearLayout) view.findViewById(R.id.progresslayout);
        contentLayout = (LinearLayout) view.findViewById(R.id.createclasslayout);
        LinearLayout inviteLayout = (LinearLayout) view.findViewById(R.id.inviteLayout);
        TextView codeHint = (TextView) view.findViewById(R.id.codeHelper);

        //checking role and setting child name according to that
        role = ParseUser.getCurrentUser().getString(Constants.ROLE);
        if(role.equals(Constants.STUDENT)) {
            childName = ParseUser.getCurrentUser().getString("name");
            childET.setVisibility(View.GONE);
            childHelp.setVisibility(View.GONE);
        }

        userId = ParseUser.getCurrentUser().getUsername();
        query = new Queries();

        callerflag = false;

        if(getArguments() != null) {
            code = getArguments().getString("classCode");

            callerflag = true;

            codeET.setVisibility(View.GONE);
            codeHelp.setVisibility(View.GONE);
            codeHint.setVisibility(View.GONE);
            inviteLayout.setVisibility(View.GONE);


            //if user is a student and joining class from suggestions, then directly join the class
            if(role.equals(Constants.STUDENT)) {
                //checking for internet connection
                if (Utility.isInternetOn(getActivity())) {

                    //calling background function to join clas
                    AddChild_Background rcb = new AddChild_Background();
                    rcb.execute();

                    //showing progress bar
                    progressLayout.setVisibility(View.VISIBLE);
                    contentLayout.setVisibility(View.GONE);

                }
                else {
                    Utility.toast("Check your Internet connection");
                }
            }

        }

        //Setting join button click functionality
        joinButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(! role.equals(Constants.STUDENT))
                    childName = childET.getText().toString();

                if(! callerflag)
                    code = codeET.getText().toString().trim();

                //validating class code and child name
                if ((!UtilString.isBlank(code))   && (! UtilString.isBlank(childName)) ) {

                    childName = childName.trim();

                    //validating code format
                    if (code.length() != 7) {
                        Utility.toast("Enter Correct Class Code");
                        return;
                    }

                    //hiding keyboard
                   // if(getActivity() != null)
                     //   Tools.hideKeyboard(getActivity());

                    //checking for internet connection
                    if (Utility.isInternetOn(getActivity())) {

                        //calling background function to join clas
                        AddChild_Background rcb = new AddChild_Background();
                        rcb.execute();

                        //showing progress bar
                        progressLayout.setVisibility(View.VISIBLE);
                        contentLayout.setVisibility(View.GONE);

                    }
                    else {
                        Utility.toast("Check your Internet connection");
                    }
                }
                else if(UtilString.isBlank(codeET.getText().toString())) {
                    Utility.toast("Enter correct class-code");
                }
                else {
                    Utility.toast("Enter correct child name");
                }
            }
        });


        inviteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), InviteTeacher.class);
                startActivity(intent);

                dialog.dismiss();
            }
        });

        //get parameter "classCode" from caller to know if called to join a suggested class. If that is the case
        //don't show class code, invite teacher details



        // Get the x, y location and store it in the location[] array
        // location[0] = x, location[1] = y.
        ViewTreeObserver vto = codeHelp.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int[] location = new int[2];
                codeHelp.getLocationOnScreen(location);
                height = codeHelp.getHeight();

                // Initialize the Point with x, and y positions
                p = new Point();
                p.x = location[0];
                p.y = location[1];
            }
        });

        final String txt =
                "You need a class-code to join the class-room. If you don't have any, ask to teacher for it.";


        //setting help button clicked functionality
        codeHelp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (p != null) {

                    Utility.toast("yo clicked");

                    Popup popup = new Popup();
                    //popup.showPopup(getActivity(), p, true, 0, txt, height, 15, 400);
                    //popup.showPopup();

                   /* InputMethodManager inputMethodManager =
                            (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);

                    //hiding keyboard
                    if (getActivity().getCurrentFocus() != null) {
                        inputMethodManager.hideSoftInputFromWindow(getActivity().getCurrentFocus()
                                .getApplicationWindowToken(), 0);
                    }*/
                }
                else
                    Utility.toast("yo null clicked");
            }
        });



        return dialog;
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
               // Utility.toast("ClassRoom Added.");


                //Refreshing joined class adapter
                Classrooms.joinedGroups = ParseUser.getCurrentUser().getList(Constants.JOINED_GROUPS);

                if(Classrooms.joinedClassAdapter != null)
                    Classrooms.joinedClassAdapter.notifyDataSetChanged();

                if(callerflag && getActivity()!= null)
                {
                    Intent intent = new Intent(getActivity(), MainActivity.class);
                    startActivity(intent);
                }

                //Refreshing inbox fetched messages

                try {
                    Messages.msgs = query.getLocalInboxMsgs();
                    Messages.updateInboxTotalCount(); //update total inbox count required to manage how/when scrolling loads more messages

                    if(Messages.msgs == null)
                        Messages.msgs = new ArrayList<ParseObject>();

                } catch (ParseException e) {
                    e.printStackTrace();
                }

                if( Messages.myadapter != null)
                    Messages.myadapter.notifyDataSetChanged();


                //Refreshing suggestion adapter
                Classrooms.suggestedGroups = JoinedHelper.getSuggestionList(ParseUser.getCurrentUser().getUsername());
                if(Classrooms.suggestedGroups == null)
                    Classrooms.suggestedGroups = new ArrayList<ParseObject>();

                if(Classrooms.suggestedClassAdapter != null)
                    Classrooms.suggestedClassAdapter.notifyDataSetChanged();


                dialog.dismiss();

            } else {
                if (classExist) {
                    Utility.toast("Class room Already added.");
                } else
                    Utility.toast("Sorry, Something went wrong. Try Again.");

                progressLayout.setVisibility(View.GONE);
                contentLayout.setVisibility(View.VISIBLE);
            }
        }
    }

}
