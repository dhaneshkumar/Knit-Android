package trumplabs.schoolapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

import BackGroundProcesses.CreatedClassRooms;
import library.ExpandableListView;
import library.UtilString;
import trumplab.textslate.R;
import utility.Config;
import utility.Queries;
import utility.Tools;
import utility.Utility;


/**
 * Shows all the created classrooms
 */
public class Classrooms extends Fragment {
    private Activity getactivity;
    private library.ExpandableListView createdClassListView;
    private library.ExpandableListView joinedClassListView;
    private library.ExpandableListView suggestedClassListView;
   // private LinearLayout emptylayout;
    public static List<List<String>> createdGroups;
    public static List<List<String>> joinedGroups;
    public static List<List<String>> suggestedGroups;
    protected LayoutInflater layoutinflater;
    public static BaseAdapter createdClassAdapter;
    public static BaseAdapter joinedClassAdapter;
    public static BaseAdapter suggestedClassAdapter;
    public static int members;
    private Queries query;
    Typeface lightTypeFace;
    public static LinearLayout buttonContainer;
    private TextView createClassTV;
    private TextView joinClassTV;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        layoutinflater = inflater;

        View layoutview = inflater.inflate(R.layout.classrooms, container, false);
        return layoutview;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        buttonContainer = (LinearLayout) getActivity().findViewById(R.id.buttonContainer);

        query = new Queries();
        getactivity = getActivity();
        createdClassListView = (ExpandableListView) getactivity.findViewById(R.id.createdclasseslistview);
        joinedClassListView = (ExpandableListView) getactivity.findViewById(R.id.joinedclasseslistview);
        suggestedClassListView = (ExpandableListView) getactivity.findViewById(R.id.suggestedclasseslistview);
        TextView createdClassTV = (TextView) getActivity().findViewById(R.id.createdClassTextView);
        TextView joinedClassTV = (TextView) getActivity().findViewById(R.id.joinedClassTextView);
        TextView suggestedClassTV = (TextView) getActivity().findViewById(R.id.suggestedClassTextView);
        createClassTV = (TextView) getActivity().findViewById(R.id.createClassTV);
        joinClassTV = (TextView) getActivity().findViewById(R.id.joinClassTV);

        //Setting condensed font
        Typeface typeFace = Typeface.createFromAsset(getactivity.getAssets(), "fonts/roboto-condensed.bold.ttf");
        createdClassTV.setTypeface(typeFace);
        joinedClassTV.setTypeface(typeFace);
        suggestedClassTV.setTypeface(typeFace);


        //Setting light font
        lightTypeFace = Typeface.createFromAsset(getactivity.getAssets(), "fonts/Roboto-Light.ttf");


       // emptylayout = (LinearLayout) getactivity.findViewById(R.id.ccemptymsg);

        //No created classrooms, goto create class activity
      /*  getactivity.findViewById(R.id.ccemptylink).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getactivity, CreateClass.class));
            }
        });*/

        ParseUser parseObject = ParseUser.getCurrentUser();
        if (parseObject == null)
            {
                Utility.logout(); return;}


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

      /*
       Initializing joined class list and it's Adapter
       */
        joinedGroups = parseObject.getList("joined_groups");

        if (joinedGroups == null)
            joinedGroups = new ArrayList<List<String>>();

        if (joinedClassAdapter == null)
            joinedClassAdapter = new JoinedClassAdapter();
        joinedClassListView.setAdapter(joinedClassAdapter);
        joinedClassListView.setExpanded(true);



        /*
        On click create button , open up dialog box to crate class
         */
        createClassTV.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fm = getActivity().getSupportFragmentManager();
                CreateClassDialog createClassDialog = new CreateClassDialog();
                createClassDialog.show(fm, "create Class");
            }
        });


        initialiseListViewMethods();
        super.onActivityCreated(savedInstanceState);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.refresh:
                if (Utility.isInternetOn(getActivity())) {

                    //showing refreshing bar
                    if (MainActivity.mHeaderProgressBar != null) {
                        Tools.runSmoothProgressBar(MainActivity.mHeaderProgressBar, 10);
                    }

                    //refreshing class-list in background
                    CreatedClassRooms createdClassList = new CreatedClassRooms();
                    createdClassList.execute();
                } else {
                    Utility.toast("Check your Internet connection");
                }
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
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

          /*  if (createdGroups.size() == 0) {
                listv.setVisibility(View.GONE);
                emptylayout.setVisibility(View.VISIBLE);
            } else {
                listv.setVisibility(View.VISIBLE);
                emptylayout.setVisibility(View.GONE);
            }*/
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
            TextView classmembers1 = (TextView) row.findViewById(R.id.classmembers1);
            TextView classcode1 = (TextView) row.findViewById(R.id.classcode1);

            int memberCount = 0;

            try {
                memberCount = query.getMemberCount(createdGroups.get(position).get(0));
            } catch (ParseException e) {

            }

            classmembers1.setText(memberCount + " Members");
            classcode1.setText(createdGroups.get(position).get(0));

            String classnamestr = createdGroups.get(position).get(1);
            classname1.setText(classnamestr.toUpperCase());                 //setting class name

            String Str = null;
            if (!UtilString.isBlank(classnamestr)) {
                Str = classnamestr.trim();
                classnamestr = Str;
            }

            return row;
        }
    }

    public void initialiseListViewMethods() {


        /**
         * setting long pressed list item functionality
         */
        createdClassListView.setOnItemLongClickListener(new OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                final View finalview = view;

                final CharSequence[] items = {"Copy Code", "Copy Class Name"};

                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle("Make your selection");
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        // Do something with the selection
                        switch (item) {
                            case 0:
                                Utility.copyToClipBoard(getActivity(), "ClassCode",
                                        ((TextView) finalview.findViewById(R.id.classcode1)).getText().toString());
                                break;
                            case 1:
                                Utility.copyToClipBoard(getActivity(), "ClassName",
                                        ((TextView) finalview.findViewById(R.id.classname1)).getText().toString());
                                break;
                            default:
                                break;
                        }
                    }
                });
                AlertDialog alert = builder.create();
                alert.show();
                return true;
            }
        });


        /**
         * setting list item clicked functionality
         */
        createdClassListView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Intent intent = new Intent(getactivity, ClassContainer.class);

                intent.putExtra("selectedclass", createdGroups.get(position).get(0));
                intent.putExtra("selectedclassName", createdGroups.get(position).get(1));
                startActivity(intent);
            }
        });
    }


    class JoinedClassAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            if (joinedGroups == null)
                joinedGroups = new ArrayList<List<String>>();

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
            TextView child_textView = (TextView) row.findViewById(R.id.childName);
          //  final ImageView option_imageView = (ImageView) row.findViewById(R.id.joinOpt);
            TextView classname = (TextView) row.findViewById(R.id.classname);
            TextView classcode = (TextView) row.findViewById(R.id.classcode);



          /*
           * Setting class name, code & child name
           */
            classcode.setText(joinedGroups.get(position).get(0));
            String Str = joinedGroups.get(position).get(1).toUpperCase();
            classname.setText(Str);

            String grooupCode = joinedGroups.get(position).get(0);


            final List<String> group = new ArrayList<String>();
            group.add(joinedGroups.get(position).get(0));
            group.add(Str);

            final String classCode = joinedGroups.get(position).get(0);
            final String className = Str;

          /*
           * setting condensed font
           */
            final String role = ParseUser.getCurrentUser().getString(Constants.ROLE);

            if(! role.equals(Constants.STUDENT)) {
                child_textView.setTypeface(lightTypeFace);
                if (joinedGroups.get(position).size() > 2) {
                    String child = joinedGroups.get(position).get(2).toString().trim();
                    child_textView.setText("Assigned to : " + child);
                }
            }
            else
            {
                child_textView.setVisibility(View.GONE);
            }


          /*
           * special check for default group
           */
            if (grooupCode.equals(Config.defaultParentGroupCode)
                    || grooupCode.equals(Config.defaultTeacherGroupCode)) {
               // option_imageView.setVisibility(View.GONE);
                classcode.setVisibility(View.GONE);
                classcreator.setVisibility(View.GONE);

                child_textView.setText("Assigned to : " + ParseUser.getCurrentUser().getString("name"));
            }

          /*
           * Setting creator name
           */
            ParseQuery<ParseObject> delquery1 = new ParseQuery<ParseObject>("Codegroup");
            delquery1.fromLocalDatastore();
            delquery1.whereEqualTo("code", joinedGroups.get(position).get(0));

            String senderId = null;
            try {
                ParseObject obj = delquery1.getFirst();
                if (obj != null) {
                    String creatorName = obj.get("Creator").toString();
                    senderId = obj.getString("senderId");


                    if (!UtilString.isBlank(creatorName)) {
                        Str = creatorName.trim();
                        classcreator.setText(Str);
                    }

                }
            } catch (ParseException e) {
            }


          /*
           * Setting options for items
           */
          /*  option_imageView.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {

                    final View popView = v;

                    if (context == null)
                        return;

                    *//** Instantiating PopupMenu class *//*
                    final PopupMenu popup = new PopupMenu(context, v);


                    *//** Adding menu items to the popumenu *//*

                    if(role.equals(Constants.STUDENT))
                        popup.getMenuInflater().inflate(R.menu.joined_groups_student_popup, popup.getMenu());
                    else
                        popup.getMenuInflater().inflate(R.menu.joined_groups_popup, popup.getMenu());


                    *//** Defining menu item click listener for the popup menu *//*
                    popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {


                        @Override
                        public boolean onMenuItemClick(MenuItem item) {

                            popup.dismiss();


                            switch (item.getItemId()) {
                                case R.id.action1:

                      *//*
                       * Updating child name
                       *//*

                                    if (context != null) {

                                        String child = "";
                                        if (joinedGroups.get(position).size() > 2) {
                                            child = joinedGroups.get(position).get(2).toString().trim();
                                        }
                                        showPopUp(popView, context, child, joinedGroups.get(position).get(0));
                                    }
                                    break;
                                case R.id.action2:
                                    Utility.copyToClipBoard(context, "ClassCode", classCode);
                                    break;
                                case R.id.action3:
                                    String dispayText = "Unsubscribe from this class? Are you sure?";
                                    showDeleteClassPopUp(popView, context, classCode, className, true, dispayText);

                                    break;

                                default:
                                    break;
                            }

                            return true;
                        }
                    });

                    *//** Showing the popup menu *//*
                    popup.show();

                }
            });*/

            return row;
        }
    }




}
