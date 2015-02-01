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

        if(role.equals(Constants.STUDENT))
            join_btn.setText("Join");
        final String txt =
                "You need a class-code to join the class-room. If you don't have any, ask to teacher for it.";
        help.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (p != null) {
                    Popup popup = new Popup();
                    popup.showPopup(getActivity(), p, true, -300, txt, height, 15, 400);
                    InputMethodManager inputMethodManager =
                            (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            // inputMethodManager.showSoftInput(viewToEdit, 0);
                    if (getActivity().getCurrentFocus() != null) {
                        inputMethodManager.hideSoftInputFromWindow(getActivity().getCurrentFocus()
                                .getApplicationWindowToken(), 0);
                    }
                }
            }
        });

        join_btn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!UtilString.isBlank(classCode.getText().toString())) {
                    code = classCode.getText().toString().trim();
                    if (code.length() != 5) {
                        Utility.toast("Enter Correct Class Code");
                        return;
                    }
            // subscribe user to this group.
                    Tools.hideKeyboard(getActivity());
                    Utility.ls("ready to subscrible");
                    if (Utility.isInternetOn(getActivity())) {
                        if(! role.equals(Constants.STUDENT)) {
                            Intent intent = new Intent(getActivity(), AddChildToClass.class);
                            intent.putExtra("code", code);
            //startActivity(intent);
                            getActivity().overridePendingTransition(R.anim.animation_leave, R.anim.animation_enter);
                            startActivityForResult(intent, 0);
                        }
                        else {
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

        Button inviteButton = (Button) getActivity().findViewById(R.id.inviteButton);
        inviteButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), InviteTeacher.class);
                startActivity(intent);
            }
        });
    }




    class AddChild_Background extends AsyncTask<Void, Void, Void> {
        boolean joinFlag;
        boolean classExist;
        String grpName;
        String schoolId;
        String division;
        String standard;
        ParseInstallation pi;
        @Override
        protected Void doInBackground(Void... params) {
            if (userId != null && childName != null) {

                /*
                * Retrieving user details
                */
                childName = childName.trim();
                childName = UtilString.parseString(childName);

                /*
                * Change first letter to caps
                */
                SessionManager session = new SessionManager(Application.getAppContext());
                session.addChildName(childName);
                ParseUser user = ParseUser.getCurrentUser();
                if (user != null) {

                    /********************* < joining class room > *************************/
                    joinFlag = false;
                    classExist = false;

                    // check whether group exist or not
                    ParseQuery<ParseObject> query = ParseQuery.getQuery("Codegroup");
                    String grpCode = code;
                    grpCode = grpCode.replace("TS", "");
                    code = "TS" + grpCode.trim();
                    query.whereEqualTo("code", code);
                    if (!textQuery.isJoinedClassExist(code)) {
                        ParseObject a;
                        try {
                            a = query.getFirst();
                            if (a != null) {
                                if (a.get("name") != null && a.getBoolean("classExist")) {
                                    String senderId = a.getString("senderId");
                                    final String grpSenderId = senderId;
                                    ParseFile senderPic = a.getParseFile("senderPic");
                                    if (!UtilString.isBlank(a.get("name").toString())) {
                                        grpName = a.get("name").toString();
                                        schoolId = a.getString("school");
                                        standard = a.getString("standard");
                                        division = a.getString(Constants.DIVISION);

                                        // Enable to receive push
                                        pi = ParseInstallation.getCurrentInstallation();
                                        if (pi != null) {
                                            pi.addUnique("channels", code);
                                            pi.saveInBackground(new SaveCallback() {
                                                @Override
                                                public void done(ParseException e) {
                                                    if(e != null)
                                                    {
                                                        pi.saveEventually();
                                                        Utility.ls("saved not installation in back");
                                                        e.printStackTrace();
                                                    }
                                                    else
                                                    {
                                                        Utility.ls("saved installation in back");
                                                    }
                                                }
                                            });
                                        }
                                        else
                                            Utility.ls("parse installation -- null");
                                        user.addUnique("joined_groups", Arrays.asList(code, grpName, childName));
                                        user.saveEventually();

                                        // Adding this user as member in GroupMembers table
                                        final ParseObject groupMembers = new ParseObject("GroupMembers");
                                        groupMembers.put("code", code);
                                        groupMembers.put("name", user.getString("name"));
                                        List<String> boys = new ArrayList<String>();
                                        boys.add(childName.trim());
                                        groupMembers.put("children_names", boys);
                                        if (user.getEmail() != null)
                                            groupMembers.put("emailId", user.getEmail());
                                        groupMembers.saveInBackground(new SaveCallback() {
                                            @Override
                                            public void done(ParseException e) {
                                                if(e == null)
                                                {
                                                    try {
                                                        memberQuery.storeGroupMember(code, userId, true);
                                                    } catch(ParseException e1)
                                                    {
                                                        e1.printStackTrace();
                                                    }
                                                }
                                            }
                                        });
                                    }

                                    /*
                                    * Saving locally in Codegroup table
                                    */
                                    a.put("userId", userId);
                                    a.pin();

                                    /*
                                    * download pic locally
                                    */
                                    senderId = senderId.replaceAll("@", "");
                                    String filePath =
                                            Utility.getWorkingAppDir() + "/thumbnail/" + senderId + "_PC.jpg";
                                    final File senderThumbnailFile = new File(filePath);
                                    if (!senderThumbnailFile.exists()) {
                                        Queries2 imageQuery = new Queries2();
                                        if (senderPic != null)
                                            imageQuery.downloadProfileImage(senderId, senderPic);
                                    } else {

                                      // Utility.toast("image already exist ");
                                    }
                                    joinFlag = true;



                                    //locally generating joiining notification and inbox msg
                                    NotificationGenerator.generateNotification(getActivity().getApplicationContext(), utility.Config.welcomeMsg, grpName, Constants.NORMAL_NOTIFICATION, Constants.INBOX_ACTION);
                                    AlarmReceiver.generateLocalMessage(utility.Config.welcomeMsg, code, a.getString("Creator"), a.getString("senderId"), grpName, user);



/*
Retrieve suggestion classes and store them in locally
*/
                                    School.storeSuggestions(schoolId, standard, division, userId);
                                }
                            }
                        } catch (ParseException e) {
                            joinFlag = false;
                        }
                    } else
                        classExist = true;
                }
            }
            return null;
        }
        @Override
        protected void onPostExecute(Void result) {
            if (joinFlag) {
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