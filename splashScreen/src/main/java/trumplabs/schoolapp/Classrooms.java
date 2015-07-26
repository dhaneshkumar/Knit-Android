package trumplabs.schoolapp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;

import BackGroundProcesses.SendPendingMessages;
import joinclasses.JoinClassDialog;
import joinclasses.JoinedClassInfo;
import library.ExpandableListView;
import library.UtilString;
import trumplab.textslate.R;
import utility.Config;
import utility.Utility;


/**
 * Shows all the created classrooms
 */
public class Classrooms extends Fragment  {
    private static Activity getactivity;
    private library.ExpandableListView createdClassListView;
    private library.ExpandableListView joinedClassListView;
    public static List<List<String>> createdGroups;
    public static List<List<String>> joinedGroups;
    protected LayoutInflater layoutinflater;
    public static BaseAdapter createdClassAdapter;
    public static BaseAdapter joinedClassAdapter;
    public static int members;
    Typeface lightTypeFace;
    public static TextView createdClassTV;
    private TextView joinedClassTV;
    private LinearLayout blank_classroom;
    private Typeface typeface;
    private boolean isTeacher;
    private ImageView classroom_empty_background;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        layoutinflater = inflater;

        View layoutview = inflater.inflate(R.layout.classrooms, container, false);
        return layoutview;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getactivity = getActivity();
        createdClassListView = (ExpandableListView) getactivity.findViewById(R.id.createdclasseslistview);
        joinedClassListView = (ExpandableListView) getactivity.findViewById(R.id.joinedclasseslistview);
        createdClassTV = (TextView) getActivity().findViewById(R.id.createdClassTextView);
        joinedClassTV = (TextView) getActivity().findViewById(R.id.joinedClassTextView);
        blank_classroom = (LinearLayout) getActivity().findViewById(R.id.classroom_blank);
        typeface = Typeface.createFromAsset(getActivity().getAssets(), "fonts/Roboto-Light.ttf");
        classroom_empty_background  = (ImageView) getActivity().findViewById(R.id.classroom_empty_background);

        //Setting condensed font
        Typeface typeFace = Typeface.createFromAsset(getactivity.getAssets(), "fonts/roboto-condensed.bold.ttf");
        createdClassTV.setTypeface(typeFace);
        joinedClassTV.setTypeface(typeFace);


        ParseUser currentParseUser = ParseUser.getCurrentUser();
        if(currentParseUser == null){
            Utility.LogoutUtility.logout();
            return;
        }

        isTeacher = currentParseUser.getString(Constants.ROLE).equals(Constants.TEACHER);

        //signup check
        if(isTeacher && getActivity().getIntent() != null)
        {
            if(getActivity().getIntent().getExtras() != null)
            {
                String signup = getActivity().getIntent().getExtras().getString("flag");
                if(!UtilString.isBlank(signup))
                {
                   if(signup.equals("CREATE_CLASS")) {
                        FragmentManager fm = getActivity().getSupportFragmentManager();
                        CreateClassDialog createClassDialog = new CreateClassDialog();
                        createClassDialog.show(fm, "create Class");
                    }
                    getActivity().getIntent().putExtra("flag", "false"); // resetting flag
                }
            }
        }


        //Setting light font
        lightTypeFace = Typeface.createFromAsset(getactivity.getAssets(), "fonts/Roboto-Light.ttf");


        ParseUser parseObject = ParseUser.getCurrentUser();
        if (parseObject == null)
            {
                Utility.LogoutUtility.logout(); return;}


        //show create class option only for teachers
        if(!isTeacher) {

            createdClassTV.setVisibility(View.GONE);
            createdClassListView.setVisibility(View.GONE);
            classroom_empty_background.setVisibility(View.VISIBLE);
            classroom_empty_background.setBackgroundDrawable(getResources().getDrawable(R.drawable.empty_join_classroom_bg));

            classroom_empty_background.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {

                    //showing dialog to join class
                    FragmentManager fm = getActivity().getSupportFragmentManager();
                    JoinClassDialog joinClassDialog = new JoinClassDialog();
                    joinClassDialog.show(fm, "Join Class");
                }
            });

        }
        else {
            classroom_empty_background.setBackgroundDrawable(getResources().getDrawable(R.drawable.empty_classroom_bg));
            classroom_empty_background.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {

                    //showing dialog to create class

                    FragmentManager fm = getActivity().getSupportFragmentManager();
                    CreateClassDialog createClassDialog = new CreateClassDialog();
                    createClassDialog.show(fm, "create Class");
                }
            });

            /*
            Initializing created class list and it's Adapter
         */
            createdGroups = parseObject.getList(Constants.CREATED_GROUPS);
            if (createdGroups == null) {
                createdGroups = new ArrayList<List<String>>();
            }

            createdClassAdapter = new CreatedClassAdapter();
            createdClassListView.setAdapter(createdClassAdapter);
            createdClassListView.setExpanded(true);
        }

      /*
       Initializing joined class list and it's Adapter
       */
        joinedGroups = getJoinedGroups(parseObject);

        joinedClassAdapter = new JoinedClassAdapter();
        joinedClassListView.setAdapter(joinedClassAdapter);
        joinedClassListView.setExpanded(true);

        initialiseListViewMethods();

    }

    /********** showcase ***********/

    /*
        returns non-null list containing joined groups(removing Kio class as a quick hack)
     */
    public static List<List<String>> getJoinedGroups(ParseUser user){
        if(user == null) return new ArrayList<>();

        List<List<String>> groups = user.getList(Constants.JOINED_GROUPS);
        if(groups == null) return new ArrayList<>();

        for(List<String> group : groups){
            String code = group.get(0);
            if(code != null && (code.equals(Config.defaultParentGroupCode) || code.equals(Config.defaultTeacherGroupCode))){
                groups.remove(group);
                Log.d("DEBUG_CLASSROOMS", "removing default group " + code + " from joined groups(Quick Hack)");
                break;
            }
        }
        return groups;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    class CreatedClassAdapter extends BaseAdapter {

        @Override
        public int getCount() {

            if (createdGroups == null)
                createdGroups = new ArrayList<List<String>>();

            if(joinedGroups == null)
                joinedGroups = new ArrayList<>();

            if (createdGroups.size() == 0) {
                createdClassTV.setVisibility(View.GONE);
                if(joinedGroups.size() ==0)
                   blank_classroom.setVisibility(View.VISIBLE);
                else
                    blank_classroom.setVisibility(View.GONE);
            }
            else {
                blank_classroom.setVisibility(View.GONE);
            }

            return createdGroups.size();
        }

        @Override
        public Object getItem(int position) {
            return createdGroups.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }


        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            if (row == null) {
                row = layoutinflater.inflate(R.layout.classroom_created_item, parent, false);
            }

            TextView classname1 = (TextView) row.findViewById(R.id.classname1);
            TextView headerText = (TextView) row.findViewById(R.id.headerText);

            String classnamestr = createdGroups.get(position).get(1);

            //setting background color of circular image
            GradientDrawable gradientdrawable = (GradientDrawable) headerText.getBackground();
            gradientdrawable.setColor(Color.parseColor(Utility.classColourCode(classnamestr.toUpperCase())));
            headerText.setText(classnamestr.substring(0, 1).toUpperCase());    //setting front end of circular image
            headerText.setTypeface(typeface);

            classname1.setText(classnamestr.toUpperCase());                 //setting class name

            return row;
        }
    }

    public void initialiseListViewMethods() {

            /**
             * setting list item clicked functionality
             */
            createdClassListView.setOnItemClickListener(new OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                    Intent intent = new Intent(getactivity, SendMessage.class);

                    intent.putExtra("classCode", createdGroups.get(position).get(0));
                    intent.putExtra("className", createdGroups.get(position).get(1));
                    startActivity(intent);
                }
            });

        /**
         * setting list item clicked functionality
         */
        joinedClassListView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Intent intent = new Intent(getactivity, JoinedClassInfo.class);

                intent.putExtra("classCode", joinedGroups.get(position).get(0));
                intent.putExtra("className", joinedGroups.get(position).get(1));
                if (joinedGroups.get(position).size() > 2 && joinedGroups.get(position).get(2) != null) {
                    intent.putExtra("assignedName", joinedGroups.get(position).get(2));
                } else {
                    ParseUser currentParseUser = ParseUser.getCurrentUser();
                    if (currentParseUser == null) {
                        Utility.LogoutUtility.logout();
                        return;
                    }
                    intent.putExtra("assignedName", currentParseUser.getString("name"));
                }
                startActivity(intent);
            }
        });
    }


    class JoinedClassAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            if (joinedGroups == null)
                joinedGroups = new ArrayList<List<String>>();

            if(createdGroups == null)
                createdGroups = new ArrayList<>();

            if (joinedGroups.size() == 0) {
                joinedClassTV.setVisibility(View.GONE);

                if(createdGroups.size() ==0)
                    blank_classroom.setVisibility(View.VISIBLE);
                else
                    blank_classroom.setVisibility(View.GONE);
            }
            else {
                joinedClassTV.setVisibility(View.VISIBLE);

                blank_classroom.setVisibility(View.GONE);
            }


            return joinedGroups.size();
        }

        @Override
        public Object getItem(int position) {

            return joinedGroups.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View row = convertView;
            if (row == null) {
                row = layoutinflater.inflate(R.layout.classrom_joined_item, parent, false);
            }

            TextView classcreator = (TextView) row.findViewById(R.id.classcreator);
            TextView classname = (TextView) row.findViewById(R.id.classname);

          /*
           * Setting class name, code & child name
           */
            String Str = joinedGroups.get(position).get(1).toUpperCase();
            classname.setText(Str);

            final List<String> group = new ArrayList<String>();
            group.add(joinedGroups.get(position).get(0));
            group.add(Str);

            classcreator.setVisibility(View.VISIBLE);

          /*
           * Setting creator name
           */
            ParseQuery<ParseObject> delquery1 = new ParseQuery<ParseObject>(Constants.CODE_GROUP);
            delquery1.fromLocalDatastore();
            delquery1.whereEqualTo("code", joinedGroups.get(position).get(0));

            String senderId = null;
            try {
                ParseObject obj = delquery1.getFirst();
                if (obj != null) {
                    String creatorName = obj.get("Creator").toString();

                    if (!UtilString.isBlank(creatorName)) {
                        Str = creatorName.trim();
                        classcreator.setText(Str);
                    }
                }
            } catch (ParseException e) {
            }

            return row;
        }
    }

    //can be called from anywhere
    public static void refreshCreatedClassrooms(final List<String> deletedCodes){
        if(getactivity != null && createdClassAdapter != null && MainActivity.floatOptionsAdapter != null){
            getactivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d(SendPendingMessages.LOGTAG, "refreshCreatedClassrooms called with = " + deletedCodes);

                    ParseUser currentUser = ParseUser.getCurrentUser();
                    if (currentUser == null) {
                        return;
                    }
                    createdGroups = currentUser.getList(Constants.CREATED_GROUPS);
                    if (createdGroups == null) {
                        createdGroups = new ArrayList<>();
                    }
                    createdClassAdapter.notifyDataSetChanged();

                    //remove this class from MainActivity's floating classList
                    if (MainActivity.classList != null && deletedCodes != null){
                        for(int i=0; i<MainActivity.classList.size(); i++){
                            List<String> cls = MainActivity.classList.get(i);
                            for(int j=0; j<deletedCodes.size(); j++) {
                                String deletedClassCode = deletedCodes.get(j);

                                if (cls != null && cls.size() > 1 && cls.get(0).equalsIgnoreCase(deletedClassCode)) {
                                    MainActivity.classList.remove(i);
                                    break;
                                }
                            }
                        }

                        MainActivity.floatOptionsAdapter.notifyDataSetChanged();
                    }
                }
            });
        }
    }
}