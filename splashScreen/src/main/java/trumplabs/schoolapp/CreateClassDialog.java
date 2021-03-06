package trumplabs.schoolapp;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.text.InputFilter;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
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
import java.util.List;
import java.util.Map;

import additionals.Invite;
import library.UtilString;
import trumplab.textslate.R;
import utility.Config;
import utility.Queries;
import utility.Utility;


/**
 * Show dialog to create a new classrooom
 */

public class CreateClassDialog extends DialogFragment{
    private Dialog dialog;
    private TextView createclassbtn;
    private Queries query;
    private String className;
    private EditText classNameET;
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
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();

        //Initializing gui elements
        query = new Queries();
        createclassbtn = (TextView) view.findViewById(R.id.create_button);
        classNameET = (EditText) view.findViewById(R.id.classnameid);
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

        classNameET.setFilters(new InputFilter[] {new InputFilter.AllCaps()});
        classNameET.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    createAction();
                    return true;
                }
                return false;
            }
        });

        //setting create button click functionality
        createclassbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createAction();
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

    void createAction(){
        //typed class name
        className = classNameET.getText().toString().trim().toUpperCase();

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
            if(Utility.isInternetExist()) {
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
            }
        }
        else
            Utility.toast("Enter Class Name");
    }

    private class createGroup extends AsyncTask<Void, Void, Void> {
        boolean success = false;
        boolean networkError = false;

        @Override
        protected Void doInBackground(Void... param) {

            //setting parameters
            HashMap<String, Object> params = new HashMap<String, Object>();
            params.put("classname", className);

            //calling parse cloud function to create class
            HashMap<String, Object> result = null;
            if(Config.SHOWLOG) Log.d("__A", "createGroup : calling createClass3");
            try {
                result = ParseCloud.callFunction("createClass3", params);
            } catch (ParseException e) {
                if(Config.SHOWLOG) Log.d("__A", "createClass3 parseexception, code=" + e.getCode() + " msg=" + e.getMessage());
                Utility.LogoutUtility.checkAndHandleInvalidSession(e);
                if(e.getCode() == ParseException.CONNECTION_FAILED){
                    networkError = true;
                }
                e.printStackTrace();
                return null;
            }

            if (result == null)
                return null;

            ParseObject codeGroupObject = (ParseObject) result.get("codegroup");
            List<List<String>> updatedCreatedGroups = (List<List<String>>) result.get(Constants.CREATED_GROUPS);

            ParseUser currentUser = ParseUser.getCurrentUser();
            if(codeGroupObject == null || updatedCreatedGroups == null || currentUser == null) {
                return null;
            }

            //successfully created your class
            //locally saving codegroup(of that class) and updated user object
            codeGroupObject.put("userId", currentUser.getUsername());
            currentUser.put(Constants.CREATED_GROUPS, updatedCreatedGroups);
            try {
                currentUser.pin();
                codeGroupObject.pin();
            } catch (ParseException e) {
                e.printStackTrace();
            }

            //retrieving class-code
            classCode = codeGroupObject.getString(Constants.Codegroup.CODE);

            //retrieving class name
            className = codeGroupObject.getString(Constants.Codegroup.NAME);
            success = true;
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);

            if(Config.SHOWLOG) Log.d("__A", "createGroup : onPostExecute()");
            if (success) {
                if(user == null){
                    return;
                }

                codeTV.setText(classCode);

                Classrooms.createdGroups = user.getList(Constants.CREATED_GROUPS);
                MainActivity.setClassListOptions();

                if(Classrooms.createdGroups != null && Classrooms.createdGroups.size()> 0)
                {
                    Classrooms.createdClassTV.setVisibility(View.VISIBLE);
                }

                if(Classrooms.createdClassAdapter != null)
                    Classrooms.createdClassAdapter.notifyDataSetChanged();

                if(MainActivity.floatOptionsAdapter != null)
                    MainActivity.floatOptionsAdapter.notifyDataSetChanged();


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
                if(networkError) {
                    Utility.toast("Unable to connect");
                }
                else{
                    Utility.toast("Oops! Something went wrong. Can't create your class");
                }
            }
        }
    }
}
