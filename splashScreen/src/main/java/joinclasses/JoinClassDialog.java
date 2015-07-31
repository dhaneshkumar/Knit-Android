package joinclasses;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;

import additionals.Invite;
import additionals.InviteToClassDialog;
import library.UtilString;
import trumplab.textslate.R;
import trumplabs.schoolapp.Application;
import trumplabs.schoolapp.Classrooms;
import trumplabs.schoolapp.Constants;
import trumplabs.schoolapp.MainActivity;
import trumplabs.schoolapp.Messages;
import utility.Config;
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
    private TextView joinButton;
    private TextView inviteButton;
    private String code;
    private String role;
    private LinearLayout progressLayout;
    private RelativeLayout contentLayout;
    private String childName;
    private String userId;
    private Point p;
    private int height;
    private boolean callerflag= false;  // true if its called from "join suggestion" class.
    private Queries query;
    private ImageView codeHead;
    private TextView codePopupText;
    private ImageView childInfo;
    private ImageView childHead;
    private TextView childPopupText;

    ParseUser currentParseUser;

    final String WRONG_CLASS_CODE_MSG = "Wrong class code !";

    public Dialog onCreateDialog(Bundle savedInstanceState) {

        //Creating new dialog box
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final View view =
                getActivity().getLayoutInflater().inflate(R.layout.join_class, null);
        builder.setView(view);
        dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();


        //Initializing variables
        codeET = (EditText) view.findViewById(R.id.code);
        childET = (EditText) view.findViewById(R.id.child);
        codeHelp = (ImageView) view.findViewById(R.id.nameHelp);
        joinButton = (TextView) view.findViewById(R.id.join);
        inviteButton = (TextView) view.findViewById(R.id.invite);
        progressLayout = (LinearLayout) view.findViewById(R.id.progresslayout);
        contentLayout = (RelativeLayout) view.findViewById(R.id.createclasslayout);
        LinearLayout inviteLayout = (LinearLayout) view.findViewById(R.id.inviteLayout);

        codeHead = (ImageView) view.findViewById(R.id.popup_up);
        codePopupText = (TextView) view.findViewById(R.id.popup_text);
        childInfo = (ImageView) view.findViewById(R.id.childHelp);
        childHead = (ImageView) view.findViewById(R.id.child_popup_up);
        childPopupText = (TextView) view.findViewById(R.id.child_popup_text);

        currentParseUser = ParseUser.getCurrentUser();
        if(currentParseUser == null){
            Utility.LogoutUtility.logout();
            return dialog;
        }

        //checking role and setting child name according to that
        role = currentParseUser.getString(Constants.ROLE);
        if(role.equals(Constants.STUDENT)) {
            childName = currentParseUser.getString("name");
            childET.setVisibility(View.GONE);
            childInfo.setVisibility(View.GONE);
        }

        userId = currentParseUser.getUsername();
        query = new Queries();

        callerflag = false;

        if(getArguments() != null) {
            code = getArguments().getString("classCode");

            callerflag = true;

            codeET.setVisibility(View.GONE);
            codeHelp.setVisibility(View.GONE);
            childInfo.setVisibility(View.GONE);
            inviteLayout.setVisibility(View.GONE);


            //if user is a student and joining class from suggestions, then directly join the class
            if(role.equals(Constants.STUDENT)) {

                //checking for internet connection
                if(Utility.isInternetExist()) {

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

                ParseUser user = ParseUser.getCurrentUser();
                if(user == null){
                    Utility.LogoutUtility.logout();
                    return;
                }

                if(! role.equals(Constants.STUDENT))
                    childName = childET.getText().toString();

                if(! callerflag)
                    code = codeET.getText().toString().trim();

                //validating class code and child name
                if ((!UtilString.isBlank(code))   && (! UtilString.isBlank(childName)) ) {

                    childName = childName.trim();

                    //validating code format
                    if (code.length() != 7) {
                        Utility.toast(WRONG_CLASS_CODE_MSG);
                        return;
                    }

                    //hiding keyboard
                    if(getActivity() != null)
                    {
                        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(joinButton.getWindowToken(), 0);
                    }

                    //to hide keyboard when showing dialog fragment
                    getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);


                    //Check CAN_JOIN_OWN_CLASS flag. If set nothing to check.
                    //Otherwise if attempt to join a created class, then deny.
                    if(!Config.CAN_JOIN_OWN_CLASS && role.equals(Constants.TEACHER)){
                        List<ArrayList<String>> createdGroups = user.getList(Constants.CREATED_GROUPS);
                        if (createdGroups != null && !createdGroups.isEmpty()) {
                            for (int i = 0; i < createdGroups.size(); i++) {
                                if (createdGroups.get(i).get(0).equalsIgnoreCase(code)) {
                                    Utility.toast("You can't join your own class");
                                    return;
                                }
                            }
                        }
                    }

                    //checking for internet connection
                    if(Utility.isInternetExist()) {

                        //calling background function to join clas
                        AddChild_Background rcb = new AddChild_Background();
                        rcb.execute();

                        //showing progress bar
                        progressLayout.setVisibility(View.VISIBLE);
                        contentLayout.setVisibility(View.GONE);

                    }
                }
                else if(UtilString.isBlank(codeET.getText().toString())) {
                    Utility.toast(WRONG_CLASS_CODE_MSG);
                }
                else {
                    Utility.toast("Enter child name");
                }
            }
        });


        inviteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Go to the common invite screen
                Intent intent = new Intent(getActivity(), Invite.class);
                intent.putExtra("inviteType", Constants.INVITATION_P2T);
                startActivity(intent);

                dialog.dismiss();
            }
        });


        //setting help button clicked functionality
        codeHelp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (codeHead.getVisibility() == View.VISIBLE) {
                    // Its visible
                    codeHead.setVisibility(View.GONE);
                    codePopupText.setVisibility(View.GONE);
                } else {
                    // Either gone or invisible
                    codeHead.setVisibility(View.VISIBLE);
                    codePopupText.setVisibility(View.VISIBLE);
                }


                childHead.setVisibility(View.GONE);
                childPopupText.setVisibility(View.GONE);



            }
        });

        //setting help button clicked functionality
        childInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (childHead.getVisibility() == View.VISIBLE) {
                    // Its visible
                    childHead.setVisibility(View.GONE);
                    childPopupText.setVisibility(View.GONE);
                }
                else
                {
                    childHead.setVisibility(View.VISIBLE);
                    childPopupText.setVisibility(View.VISIBLE);
                }

                codeHead.setVisibility(View.GONE);
                codePopupText.setVisibility(View.GONE);

            }
        });


        return dialog;
    }




    /**
     * joining class using code in background
     */
    class AddChild_Background extends AsyncTask<Void, Void, Boolean> {
        boolean classExist; //flag to test whether class already added in user's joined-group or not
        boolean classCodeNotExist;

        @Override
        protected Boolean doInBackground(Void... params) {
            if (userId != null && (!UtilString.isBlank(childName))) {

                /*
                * Retrieving user details
                */
                childName = childName.trim();
                childName = UtilString.changeFirstToCaps(childName);
                childName = UtilString.parseString(childName);

                classExist = false; //setting flag initially false
                classCodeNotExist = false; //Assuming class code exist
                /*
                * Change first letter to caps
                */
                SessionManager session = new SessionManager(Application.getAppContext());
                ParseUser user = ParseUser.getCurrentUser();
                if (user != null) {
                    int result = JoinedHelper.joinClass(code, childName);

                    if (result == 1) {
                        return true;      //successfully joined class
                    }
                    else if (result == 2) {
                        classExist = true;    //already joined
                        return false;
                    }
                    else if(result ==3) {
                        classCodeNotExist = true;
                        return false;
                    }
                    else
                        return false;       //failed to join

                } else
                    return false;
            }

            return false;
        }
        @Override
        protected void onPostExecute(Boolean result) {

            if (result) {
                if(getActivity()!=null)
                    Utility.toast("Classroom Joined");

                //Refreshing joined class adapter
                Classrooms.joinedGroups = currentParseUser.getList(Constants.JOINED_GROUPS);

                SessionManager sessionManager = new SessionManager(Application.getAppContext());

                if(!sessionManager.getHasUserJoinedClass() && Classrooms.joinedGroups != null && Classrooms.joinedGroups.size() > 0)
                {
                    //USEr has joined class atleast once : setting flag for that
                    sessionManager.setHasUserJoinedClass();

                    //Adding an extra tab
                    MainActivity.tab3Icon.setVisibility(View.VISIBLE);
                    MainActivity.tab1Icon.setText("SENT");
                    LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams( 0, LinearLayout.LayoutParams.WRAP_CONTENT, 3);
                    MainActivity.tab1Icon.setLayoutParams(layoutParams);
                    MainActivity.myAdapter.notifyDataSetChanged();
                }

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

                if(getActivity() != null){
                    //show if signup account, and not set in sharedprefs
                    String tutorialId = currentParseUser.getUsername() + Constants.TutorialKeys.JOIN_INVITE;
                    if(sessionManager.getSignUpAccount() && !sessionManager.getTutorialState(tutorialId)) {
                        sessionManager.setTutorialState(tutorialId, true);

                        if(Messages.msgs != null && Messages.msgs.size() > 0){
                            ParseObject msgObject = Messages.msgs.get(0);

                            Bundle bundle = new Bundle();
                            bundle.putString("classCode", msgObject.getString("code"));
                            bundle.putString("className", msgObject.getString("name"));
                            bundle.putString("teacherName", msgObject.getString("Creator"));

                            FragmentManager fm = getActivity().getSupportFragmentManager(); //MyActionBarActivity (our base class) is FragmentActivity derivative
                            InviteToClassDialog inviteToClassDialog = new InviteToClassDialog();
                            inviteToClassDialog.setArguments(bundle);
                            inviteToClassDialog.show(fm, "Invite others");
                        }
                    }
                }

                if( Messages.myadapter != null)
                    Messages.myadapter.notifyDataSetChanged();

                dialog.dismiss();

            } else {
                if (classExist)
                    Utility.toast("Classroom already joined.");
                else if(classCodeNotExist)
                    Utility.toast(WRONG_CLASS_CODE_MSG);
                else
                    Utility.toast("Sorry, Something went wrong. Try Again.");
            }

            progressLayout.setVisibility(View.GONE);
            contentLayout.setVisibility(View.VISIBLE);
        }
    }

}
