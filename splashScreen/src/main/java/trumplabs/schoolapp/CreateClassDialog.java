package trumplabs.schoolapp;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.parse.ParseAnalytics;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseUser;

import java.util.HashMap;
import java.util.Map;

import additionals.Invite;
import library.UtilString;
import notifications.NotificationGenerator;
import trumplab.textslate.R;
import utility.Queries;
import utility.SessionManager;
import utility.Utility;


/**
 * Show dialog to create a new classrooom
 */

public class CreateClassDialog extends DialogFragment{
    private Dialog dialog;
    private TextView createclassbtn;
    private Queries query;
    private String className;
    private EditText classView;
    private ParseUser user;
    private String classCode;
    private LinearLayout progressLayout;
    private LinearLayout contentLayout;
    private LinearLayout codeViewLayout;
    private TextView codeTV;
    private TextView seeHowTV;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        //creating new alertdialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View view =
                getActivity().getLayoutInflater().inflate(R.layout.create_class_popup, null);
        builder.setView(view);
        dialog = builder.create();
        dialog.show();

        //Initializing gui elements
        query = new Queries();
        createclassbtn = (TextView) view.findViewById(R.id.create_button);
        classView = (EditText) view.findViewById(R.id.classnameid);
        progressLayout = (LinearLayout) view.findViewById(R.id.progresslayout);
        contentLayout = (LinearLayout) view.findViewById(R.id.createclasslayout);
        codeViewLayout = (LinearLayout) view.findViewById(R.id.codeViewLayout);
        codeTV = (TextView) view.findViewById(R.id.codeTV);
        seeHowTV = (TextView) view.findViewById(R.id.seeHow);
        TextView classHeading = (TextView) view.findViewById(R.id.heading);


        user = ParseUser.getCurrentUser();


        //flag to tell whether user has signup or not
        if(getArguments() != null) {
            String flag = getArguments().getString("flag");
            if(flag.equals("SIGNUP"))
            {
                classHeading.setText("Create your First Classroom");
            }
        }


        //setting create button click functionality
        createclassbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //typed class name
                className = classView.getText().toString().trim();

                String updatedName = className;

                //Checking newly created class already exist or not
                if (!UtilString.isBlank(className)) {
                    if(query.checkClassNameExist(updatedName))
                    {
                        Utility.toast(updatedName + " class already exist");
                        return;
                    }

                    //to hide keyboard when showing dialog fragment
                    getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);


                    //calling creteGroup class to create new class in background
                    if(Utility.isInternetExist(getActivity())) {
                        createGroup jg = new createGroup();
                        jg.execute();


                        /*
                         * Hidding the keyboard from screen
                         */

                        if(getActivity() != null)
                        {
                            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(createclassbtn.getWindowToken(), 0);
                        }

                        progressLayout.setVisibility(View.VISIBLE);
                        contentLayout.setVisibility(View.GONE);
                    } else {
                        Utility.toast("Check your Internet connection");
                    }
                }
                else
                    Utility.toast("Enter Class Name");
            }

        });



        //on click code, copy code to clipboard
        codeTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Utility.copyToClipBoard(getActivity(), "Class code", classCode);

            }
        });


        //on click on "seeHow" button go to invite parent class.
        seeHowTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                   Intent intent = new Intent(getActivity(), Invite.class);

                    intent.putExtra("classCode", classCode);
                    intent.putExtra("className", className);
                    intent.putExtra("inviteType", Constants.INVITATION_T2P);
                    startActivity(intent);

                    dialog.dismiss();

            }
        });

        return dialog;
    }

    private class createGroup extends AsyncTask<Void, Void, Boolean> {


        @Override
        protected Boolean doInBackground(Void... param) {

            //setting parameters
            HashMap<String, Object> params = new HashMap<String, Object>();
            params.put("classname", className);

            ParseObject codeGroupObject = null;

            //calling parse cloud function to create class
            try {
                codeGroupObject = ParseCloud.callFunction("createClass", params);
            } catch (ParseException e) {
                e.printStackTrace();
                return false;
            }


            if (codeGroupObject == null)
                return false;
            else {
                //successfully created your class

                //locally saving codegroup entry corresponding to that class
                codeGroupObject.put("userId", user.getUsername());
                try {
                    codeGroupObject.pin();
                } catch (ParseException e) {
                    e.printStackTrace();
                }

                //retrieving class-code
                classCode = codeGroupObject.getString("code");


                //retrieving class name
                className = codeGroupObject.getString("name");


                //fetching changes made to created groups of user
                try {
                    user.fetch();
                } catch (ParseException e) {
                    e.printStackTrace();
                }

                return true;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {





//                Utility.toast("Group Creation successful");

                if(getActivity() != null) {
                    //create class creation messages and notification
                    SessionManager session = new SessionManager(getActivity().getApplicationContext());
                    NotificationGenerator.generateNotification(getActivity().getApplicationContext(), Constants.CLASS_CREATION_MESSAGE_TEACHER, Constants.DEFAULT_NAME, Constants.NORMAL_NOTIFICATION, Constants.INBOX_ACTION);
                    //EventCheckerAlarmReceiver.generateLocalMessage(Constants.CLASS_CREATION_MESSAGE_TEACHER, Constants.DEFAULT_NAME, user);
                }

                codeTV.setText(classCode);

                Classrooms.createdGroups = user.getList(Constants.CREATED_GROUPS);

                if(Classrooms.createdGroups != null && Classrooms.createdGroups.size()> 0)
                {
                    Classrooms.createdClassTV.setVisibility(View.VISIBLE);
                }

                if(Classrooms.createdClassAdapter != null)
                    Classrooms.createdClassAdapter.notifyDataSetChanged();


                // Setting layouts visibility
                codeViewLayout.setVisibility(View.VISIBLE);
                progressLayout.setVisibility(View.GONE);


                if(Classrooms.createdGroups != null && Classrooms.createdGroups.size() ==1)
                {
                    if(Constants.IS_SIGNUP) {

                        //Analytics to measure created classrooms on first time use
                        Map<String, String> dimensions = new HashMap<String, String>();
                        dimensions.put("First created class", "First created class on FTU");
                        ParseAnalytics.trackEvent("Signup", dimensions);
                    }

                    //Analytics to measure created classrooms on first time use
                    Map<String, String> dimensions = new HashMap<String, String>();
                    dimensions.put("First created class", "First created class");
                    ParseAnalytics.trackEvent("Signup", dimensions);

                }


               // dialog.dismiss();
            }
            else
            {
                contentLayout.setVisibility(View.VISIBLE);
                progressLayout.setVisibility(View.GONE);
                Utility.toast("Oops! Something went wrong. Can't create your class");
            }
            super.onPostExecute(result);
        }
    }
}
