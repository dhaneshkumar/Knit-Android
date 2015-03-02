package trumplabs.schoolapp;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.PopupMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseUser;

import java.util.HashMap;

import additionals.InviteParents;
import joinclasses.School;
import library.UtilString;
import notifications.EventCheckerAlarmReceiver;
import notifications.NotificationGenerator;
import trumplab.textslate.R;
import utility.Queries;
import utility.SessionManager;
import utility.Utility;


/**
 * Created by Dhanesh on 19/2/15.
 */
public class CreateClassDialog extends DialogFragment{
    private Dialog dialog;
    private TextView schoolButton;
    private TextView standardButton;
    private TextView divisonButton;
    private TextView createclassbtn;
    private String selectedSchool;
    private String selectedStandard;
    private String selectedDivison;
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

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View view =
                getActivity().getLayoutInflater().inflate(R.layout.create_class_popup, null);
        builder.setView(view);
        dialog = builder.create();
        dialog.show();

        query = new Queries();
        schoolButton = (TextView) view.findViewById(R.id.school);
        standardButton = (TextView) view.findViewById(R.id.standard);
        divisonButton = (TextView) view.findViewById(R.id.division);
        createclassbtn = (TextView) view.findViewById(R.id.create_button);
        classView = (EditText) view.findViewById(R.id.classnameid);
        progressLayout = (LinearLayout) view.findViewById(R.id.progresslayout);
        contentLayout = (LinearLayout) view.findViewById(R.id.createclasslayout);
        codeViewLayout = (LinearLayout) view.findViewById(R.id.codeViewLayout);
        codeTV = (TextView) view.findViewById(R.id.codeTV);
        seeHowTV = (TextView) view.findViewById(R.id.seeHow);
        TextView classHeading = (TextView) view.findViewById(R.id.heading);


       // School school = new School();
        user = ParseUser.getCurrentUser();
       /* selectedSchool = school.getSchoolName(user.getString("school"));
        if (selectedSchool != null)
            schoolButton.setText(selectedSchool);
        else
            selectedSchool ="Other";

        selectedDivison = "NA";
        selectedStandard = "NA";*/

        //signup check.
        if(getArguments() != null) {
            String flag = getArguments().getString("flag");
            if(flag.equals("SIGNUP"))
            {
                classHeading.setText("Create your First Classroom");
            }
        }


        createclassbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //typed class name
                className = classView.getText().toString().trim();

                String updatedName = className;

            /*    if(!UtilString.isBlank(selectedStandard) && !selectedStandard.equals("NA"))
                    updatedName += " "+selectedStandard;

                if(!UtilString.isBlank(selectedDivison) && !selectedDivison.equals("NA"))
                    updatedName += selectedDivison;
*/

                if (!UtilString.isBlank(className)) {

                    if(query.checkClassNameExist(updatedName))
                    {
                        Utility.toast(updatedName + " class already exist");
                        return;
                    }

                    if (Utility.isInternetOn(getActivity())) {
                        createGroup jg = new createGroup();
                        jg.execute();


                        /*
                         * Hidding the keyboard from screen
                         */

                        /*if(getActivity() != null)
                            Tools.hideKeyboard(getActivity());*/

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


         /*
     * school select options
     */
        schoolButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                School school1 = new School();
                String school = school1.getSchoolName(user.getString("school"));

        /*
         * Creating pop-menu for selecting schools
         */
                PopupMenu menu = new PopupMenu(getActivity(), v);

                if (!UtilString.isBlank(school))
                    menu.getMenu().add(school);

                menu.getMenu().add("Other");
                menu.show();

                // setting menu click functionality
                menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(MenuItem item) {

                        selectedSchool = item.getTitle().toString();
                        schoolButton.setText(selectedSchool);
                        return false;
                    }
                });
            }
        });


        /*
     * school select options
     */
        standardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

        /*
         * Creating popmenu for selecting schools
         */
                PopupMenu menu = new PopupMenu(getActivity(), v);
                menu.getMenuInflater().inflate(R.menu.standard, menu.getMenu());
                menu.show();


                /** Defining menu item click listener for the popup menu */

                // setting menu click functionality
                menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(MenuItem item) {


                        selectedStandard = item.getTitle().toString();
                        standardButton.setText(selectedStandard);
                        standardButton.setTextColor(Color.parseColor("#000000"));
                        return false;
                    }
                });
            }
        });

        divisonButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


        /*
         * Creating popmenu for selecting schools
         */
                PopupMenu menu = new PopupMenu(getActivity(), v);
                menu.getMenuInflater().inflate(R.menu.division, menu.getMenu());
                menu.show();


                /** Defining menu item click listener for the popup menu */

                // setting menu click functionality
                menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(MenuItem item) {

                        selectedDivison = item.getTitle().toString();
                        divisonButton.setText(selectedDivison);
                        divisonButton.setTextColor(Color.parseColor("#000000"));
                        return false;
                    }
                });
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
                   Intent intent = new Intent(getActivity(), InviteParents.class);

                    intent.putExtra("classCode", classCode);
                    intent.putExtra("className", className);
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

            //setting schoolId, classname, standard and division
           /* String schoolId = user.getString("school");
            if (!selectedSchool.trim().equals("Other"))
                params.put("school", schoolId);*/

      //      params.put("division", selectedDivison);
      //      params.put("standard", selectedStandard);
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
                    EventCheckerAlarmReceiver.generateLocalMessage(Constants.CLASS_CREATION_MESSAGE_TEACHER, Constants.DEFAULT_NAME, user);
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

               // dialog.dismiss();

            } /*else if (classNameCheckFlag) {
        createclasslayout.setVisibility(View.VISIBLE);
        progressLayout.setVisibility(View.GONE);
        Utility.toast("Sorry. Can't create classes with same name");
        classNameCheckFlag = false;
      }*/
            else
            {
                contentLayout.setVisibility(View.VISIBLE);
                progressLayout.setVisibility(View.GONE);
                Utility.toast("Oops! Something went wrong. Can't create your class");
            }


      /*
       * else if (internetFlag){ createclasslayout.setVisibility(View.VISIBLE);
       * progressLayout.setVisibility(View.GONE); Utility.toast("Check your Internet Connection.");
       * internetFlag = false;
       *
       * }
       */

            super.onPostExecute(result);
        }
    }
}
