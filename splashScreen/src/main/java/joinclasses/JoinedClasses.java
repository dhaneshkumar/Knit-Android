package joinclasses;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.PopupMenu.OnMenuItemClickListener;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import BackGroundProcesses.DeleteJoinedGroup;
import BackGroundProcesses.JoinedClassRooms;
import BackGroundProcesses.UpdateSuggestions;
import fr.castorflex.android.smoothprogressbar.SmoothProgressBar;
import library.ExpandableListView;
import library.UtilString;
import trumplab.textslate.R;
import trumplabs.schoolapp.Application;
import trumplabs.schoolapp.Constants;
import utility.Config;
import utility.Queries2;
import utility.SessionManager;
import utility.Tools;
import utility.Utility;

/**
 * Displays list of all joined classes and suggested classes
 */
public class JoinedClasses extends Fragment {

    private LayoutInflater layoutinflater;
    private library.ExpandableListView listv;
    public static List<List<String>> joinedGroups;
    private TextView classname;
    private boolean checkInternet = false;
    public static BaseAdapter joinedadapter;
    private String userId;
    public static LinearLayout progressBarLayout;
    public static LinearLayout editProfileLayout;
    private Typeface typeFace;
    private Context context;
    public static SmoothProgressBar mHeaderProgressBar;
    private SessionManager session;
    private String DefaultClassCode;
    private PopupWindow popupMessage;
    private library.ExpandableListView suggestionList;
    public static List<List<String>> suggestedGroups;
    public static BaseAdapter suggestionAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        layoutinflater = inflater;
        getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        return inflater.inflate(R.layout.joinedclasses_layout, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        // retrieving userid
        ParseUser parseObject = ParseUser.getCurrentUser();

        if (parseObject == null)
            {Utility.logout(); return;}

        userId = parseObject.getUsername();
        progressBarLayout = (LinearLayout) getActivity().findViewById(R.id.progressBarLayout);
        editProfileLayout = (LinearLayout) getActivity().findViewById(R.id.editLayout);
        mHeaderProgressBar = (SmoothProgressBar) getActivity().findViewById(R.id.ptr_progress);
        TextView suggestionText = (TextView) getActivity().findViewById(R.id.suggestion);

        typeFace =
                Typeface.createFromAsset(getActivity().getAssets(), "fonts/RobotoCondensed-Italic.ttf");

          /*
          Fetching local joined list
           */
        joinedGroups = parseObject.getList("joined_groups");
        context = getActivity();

        if (joinedGroups == null)
            joinedGroups = new ArrayList<List<String>>();

        if (joinedadapter == null)
            joinedadapter = new myBaseAdapter();

      /*
      Fetching suggestion list
       */
        suggestedGroups = JoinedHelper.getSuggestionList(userId);

        if (suggestedGroups == null)
            suggestedGroups = new ArrayList<List<String>>();

        if (suggestedGroups.size() == 0)
            suggestionText.setVisibility(View.GONE);
        else
            suggestionText.setVisibility(View.VISIBLE);

        if (suggestionAdapter == null)
            suggestionAdapter = new suggestionAdapter();

        /*
         * Check whether it has default group or not
         */
        session = new SessionManager(Application.getAppContext());
        if (!session.getDefaultClassExtst()) {
            try {
                defaultClassJoined();
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        if (ParseUser.getCurrentUser() != null) {
            if (ParseUser.getCurrentUser().getString("role").equals("teacher"))
                DefaultClassCode = Config.defaultTeacherGroupCode;
            else
                DefaultClassCode = Config.defaultParentGroupCode;
        } else
            {Utility.logout(); return;}

        listv = (ExpandableListView) getActivity().findViewById(R.id.joinedclasseslistview);
        suggestionList = (ExpandableListView) getActivity().findViewById(R.id.suggestionclasseslistview);
        final ViewPagerCommunicator vpc = (ViewPagerCommunicator) getActivity();

        super.onActivityCreated(savedInstanceState);
        listv.setAdapter(joinedadapter);
        listv.setExpanded(true);
        suggestionList.setAdapter(suggestionAdapter);
        suggestionList.setExpanded(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu8, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.refresh:
                if (Utility.isInternetOn(getActivity())) {

                    if (mHeaderProgressBar != null) {
                        Tools.runSmoothProgressBar(mHeaderProgressBar, 10);
                    }

                    //refreshing join clases
                    JoinedClassRooms joinClass = new JoinedClassRooms();
                    joinClass.execute();

                    //refreshing classroom suggestions
                  //  UpdateSuggestions updateSuggestions = new UpdateSuggestions(true);
                   // updateSuggestions.execute();
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

    class myBaseAdapter extends BaseAdapter {

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
                row = layoutinflater.inflate(R.layout.joinedclasses_classview, parent, false);
            }
            ImageView classimg = (ImageView) row.findViewById(R.id.classimage);
            TextView classcreator = (TextView) row.findViewById(R.id.classcreator);
            TextView child_textView = (TextView) row.findViewById(R.id.childName);
            final ImageView option_imageView = (ImageView) row.findViewById(R.id.joinOpt);
            classname = (TextView) row.findViewById(R.id.classname);
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
                child_textView.setTypeface(typeFace);
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
                option_imageView.setVisibility(View.GONE);
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

                    if (!UtilString.isBlank(senderId)) {
                        senderId = senderId.replaceAll("@", "");

                        String filePath = Utility.getWorkingAppDir() + "/thumbnail/" + senderId + "_PC.jpg";
                        final File senderThumbnailFile = new File(filePath);
                        if (senderThumbnailFile.exists()) {
                            // image file present locally

                            Bitmap mySenderBitmap =
                                    BitmapFactory.decodeFile(senderThumbnailFile.getAbsolutePath());
                            classimg.setImageBitmap(mySenderBitmap);
                        } else {

                            String sex = obj.getString("sex");

                            if (!UtilString.isBlank(sex)) {

                                if (sex.equals("M"))
                                    classimg.setImageResource(R.drawable.maleteacherdp);
                                else if (sex.equals("F"))
                                    classimg.setImageResource(R.drawable.femaleteacherdp);
                            } else {

                                // if sex is not stored
                                if (!UtilString.isBlank(creatorName)) {
                                    String[] names = creatorName.split("\\s");

                                    if (names != null && names.length > 1) {
                                        String title = names[0].trim();

                                        if (title.equals("Mr")) {
                                            classimg.setImageResource(R.drawable.maleteacherdp);
                                            obj.put("sex", "M");
                                            obj.pin();
                                        } else if (title.equals("Mrs")) {
                                            classimg.setImageResource(R.drawable.femaleteacherdp);
                                            obj.put("sex", "F");
                                            obj.pin();
                                        } else if (title.equals("Ms")) {
                                            classimg.setImageResource(R.drawable.femaleteacherdp);
                                            obj.put("sex", "F");
                                            obj.pin();
                                        } else
                                            classimg.setImageResource(R.drawable.logo);
                                    } else
                                        classimg.setImageResource(R.drawable.logo);
                                } else
                                    classimg.setImageResource(R.drawable.logo);
                            }
                        }
                    }
                }
            } catch (ParseException e) {
            }


          /*
           * Setting options for items
           */
            option_imageView.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {

                    final View popView = v;

                    if (context == null)
                        return;

                    /** Instantiating PopupMenu class */
                    final PopupMenu popup = new PopupMenu(context, v);


                    /** Adding menu items to the popumenu */

                    if(role.equals(Constants.STUDENT))
                        popup.getMenuInflater().inflate(R.menu.joined_groups_student_popup, popup.getMenu());
                    else
                        popup.getMenuInflater().inflate(R.menu.joined_groups_popup, popup.getMenu());


                    /** Defining menu item click listener for the popup menu */
                    popup.setOnMenuItemClickListener(new OnMenuItemClickListener() {


                        @Override
                        public boolean onMenuItemClick(MenuItem item) {

                            popup.dismiss();


                            switch (item.getItemId()) {
                                case R.id.action1:

                      /*
                       * Updating child name
                       */

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

                    /** Showing the popup menu */
                    popup.show();

                }
            });

            return row;
        }
    }


    class suggestionAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            if (suggestedGroups == null)
                suggestedGroups = new ArrayList<List<String>>();

            return suggestedGroups.size();
        }

        @Override
        public Object getItem(int position) {

            return suggestedGroups.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View row = convertView;
            if (row == null) {
                row = layoutinflater.inflate(R.layout.joinedclasses_suggestionview, parent, false);
            }
            ImageView classimg = (ImageView) row.findViewById(R.id.classimage);
            TextView classcreator = (TextView) row.findViewById(R.id.classcreator);
            final ImageView option_imageView = (ImageView) row.findViewById(R.id.joinOpt);
            classname = (TextView) row.findViewById(R.id.classname);
            TextView joinButton = (TextView) row.findViewById(R.id.joinButton);
            final TextView code = (TextView) row.findViewById(R.id.childName);



          /*
           * Setting class name, code & child name
           */
            String className = suggestedGroups.get(position).get(1).toUpperCase();
            classname.setText(className);

            final String classCode = suggestedGroups.get(position).get(0);
            code.setText(classCode);

          /*
           * Setting creator name
           */
            ParseQuery<ParseObject> delquery1 = new ParseQuery<ParseObject>("Codegroup");
            delquery1.fromLocalDatastore();
            delquery1.whereEqualTo("code", suggestedGroups.get(position).get(0));

            String senderId = null;
            try {
                ParseObject obj = delquery1.getFirst();
                if (obj != null) {
                    String creatorName = obj.get("Creator").toString();
                    senderId = obj.getString("senderId");

                    if (!UtilString.isBlank(creatorName)) {
                        classcreator.setText(creatorName.trim());
                    }

                    if (!UtilString.isBlank(senderId)) {
                        senderId = senderId.replaceAll("@", "");

                        String filePath = Utility.getWorkingAppDir() + "/thumbnail/" + senderId + "_PC.jpg";
                        final File senderThumbnailFile = new File(filePath);
                        if (senderThumbnailFile.exists()) {
                            // image file present locally

                            Bitmap mySenderBitmap =
                                    BitmapFactory.decodeFile(senderThumbnailFile.getAbsolutePath());
                            classimg.setImageBitmap(mySenderBitmap);
                        } else {

                            String sex = obj.getString("sex");

                            if (!UtilString.isBlank(sex)) {

                                if (sex.equals("M"))
                                    classimg.setImageResource(R.drawable.maleteacherdp);
                                else if (sex.equals("F"))
                                    classimg.setImageResource(R.drawable.femaleteacherdp);
                            } else {

                                // if sex is not stored
                                if (!UtilString.isBlank(creatorName)) {
                                    String[] names = creatorName.split("\\s");

                                    if (names != null && names.length > 1) {
                                        String title = names[0].trim();

                                        if (title.equals("Mr")) {
                                            classimg.setImageResource(R.drawable.maleteacherdp);
                                            obj.put("sex", "M");
                                            obj.pin();
                                        } else if (title.equals("Mrs")) {
                                            classimg.setImageResource(R.drawable.femaleteacherdp);
                                            obj.put("sex", "F");
                                            obj.pin();
                                        } else if (title.equals("Ms")) {
                                            classimg.setImageResource(R.drawable.femaleteacherdp);
                                            obj.put("sex", "F");
                                            obj.pin();
                                        } else
                                            classimg.setImageResource(R.drawable.logo);
                                    } else
                                        classimg.setImageResource(R.drawable.logo);
                                } else
                                    classimg.setImageResource(R.drawable.logo);
                            }
                        }
                    }
                }
            } catch (ParseException e) {
            }


          /*
           * Setting options for items
           */
            option_imageView.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    final View popView = v;

                    if (context == null)
                        return;

                    /** Instantiating PopupMenu class */
                    final PopupMenu popup = new PopupMenu(context, v);


                    /** Adding menu items to the popumenu */
                    popup.getMenuInflater().inflate(R.menu.joined_suggestions, popup.getMenu());

                    /** Defining menu item click listener for the popup menu */
                    popup.setOnMenuItemClickListener(new OnMenuItemClickListener() {


                        @Override
                        public boolean onMenuItemClick(MenuItem item) {

                            popup.dismiss();


                            switch (item.getItemId()) {
                                case R.id.action1:

                      /*
                        Putting this group in removed list
                       */
                                    if (context != null) {

                                        List<List<String>> removedList = ParseUser.getCurrentUser().getList(Constants.REMOVED_GROUPS);

                                        if (removedList == null)
                                            removedList = new ArrayList<List<String>>();
                                        removedList.add(suggestedGroups.get(position));
                                        ParseUser.getCurrentUser().put(Constants.REMOVED_GROUPS, removedList);
                                        ParseUser.getCurrentUser().saveEventually();

                                        // updating adapter
                                        suggestedGroups.remove(position);
                                        suggestionAdapter.notifyDataSetChanged();

                                    }
                                    break;

                                default:
                                    break;
                            }

                            return true;
                        }
                    });

                    /** Showing the popup menu */
                    popup.show();

                }
            });


           /*
                Joining suggested class
                 */
            joinButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {


                    String dispayText = "Are you sure want to join this Class ?";
                    showDeleteClassPopUp(v, context, classCode, null, false, dispayText);
                }
            });

            return row;
        }
    }


    public interface ViewPagerCommunicator {
        void viewPagerSet(int i);
    }


    class joinDefaultGroup extends AsyncTask<Void, Void, Void> {
        private String code;
        private String grpName;
        private boolean classExist;

        public joinDefaultGroup(String code) throws ParseException {
            this.code = code;
        }

        @Override
        protected Void doInBackground(Void... params) {

            if (userId != null && ParseUser.getCurrentUser() != null) {

            /*
             * Retrieving user details
             */
                classExist = false;
                String childName = ParseUser.getCurrentUser().getString("name");
                childName = UtilString.parseString(childName);

                JoinedHelper.joinClass(code, childName, true);
            }
            return null;
        }
    }



    private void defaultClassJoined() throws ParseException {


        boolean defaultClassExist = false;

        if (ParseUser.getCurrentUser().getString("role").equals("teacher")) {
            for (int i = 0; i < joinedGroups.size(); i++) {
                if (joinedGroups.get(i).get(0).equals(Config.defaultTeacherGroupCode)) {
                    defaultClassExist = true;
                    session.setDefaultClassExtst();
                    break;
                }
            }

            if (!defaultClassExist) {
                joinDefaultGroup jd = new joinDefaultGroup(Config.defaultTeacherGroupCode);
                jd.execute();

            }
        } else {
            for (int i = 0; i < joinedGroups.size(); i++) {
                if (joinedGroups.get(i).get(0).equals(Config.defaultParentGroupCode)) {
                    defaultClassExist = true;
                    session.setDefaultClassExtst();
                    break;
                }
            }

            if (!defaultClassExist) {
                joinDefaultGroup jd = new joinDefaultGroup(Config.defaultParentGroupCode);
                jd.execute();
            }
        }
    }

    /*
     * pop up to update name
     */
    @SuppressLint("NewApi")
    private void showPopUp(final View popupView, Context context, String hint,
                           final String classcode) {


        /*
         * Setting parent linear layout
         */
        LinearLayout layout = new LinearLayout(context);
        LinearLayout parentLayout = new LinearLayout(context);
        LinearLayout layout0 = new LinearLayout(context);
        LinearLayout layout2 = new LinearLayout(context);
        LinearLayout editTextLayout = new LinearLayout(context);

        parentLayout.setOrientation(LinearLayout.VERTICAL);


        LinearLayout.LayoutParams parentParams =
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 300);

        LinearLayout.LayoutParams parentParams2 =
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT);


        LinearLayout.LayoutParams techerParams =
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);

        layout0.setLayoutParams(parentParams);
        layout.setLayoutParams(techerParams);
        layout2.setLayoutParams(parentParams2);


        layout0.setBackgroundColor(Color.BLACK);
        layout2.setBackgroundColor(Color.BLACK);
        layout0.setAlpha(new Float(0.6));
        layout2.setAlpha(new Float(0.6));


        layout.setPadding(50, 40, 50, 20);
        layout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);

        String backgroudColor = "#f1f1f1";
        layout.setBackgroundColor(Color.parseColor(backgroudColor));

        /*
         * setting heading text in layout
         */
        TextView popupText = new TextView(context);
        popupText.setText("Update Associated Name");
        // popupText.setGravity(Gravity.CENTER );
        popupText.setTextSize(20);
        popupText.setPadding(15, 0, 15, 20);
        // popupText.setTypeface(null, Typeface.BOLD);
        layout.addView(popupText, params);

        String buttonColor = "#0099cc";

        /*
         * Setting edittext inside layout
         */
        LinearLayout.LayoutParams editParams =
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
        editParams.setMargins(1, 1, 1, 1);

        final EditText input = new EditText(context);

        if (!UtilString.isBlank(hint))
            input.setText(hint);
        input.requestFocus();
        input.setTextSize(18);
        input.setTextColor(Color.parseColor(buttonColor));
        // input.setGravity(Gravity.CENTER );
        input.setPadding(20, 25, 20, 25);
        input.setBackgroundColor(Color.WHITE);
        editTextLayout.setBackgroundColor(Color.parseColor("#B7B6B6"));
        editTextLayout.setLayoutParams(params);

        editTextLayout.addView(input, editParams);
        layout.addView(editTextLayout);


        /*
         * Adding layout containg ok and cancel button
         */
        LinearLayout subLayout = new LinearLayout(context);
        subLayout.setLayoutParams(params);
        subLayout.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout.LayoutParams buttonParams =
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);

        buttonParams.setMargins(0, 10, 0, 0);

        /*
         * ok button
         */

        TextView ok = new TextView(context);
        ok.setText("OK");
        ok.setGravity(Gravity.CENTER);
        ok.setTextSize(20);
        ok.setPadding(15, 15, 15, 15);
        ok.setTextColor(Color.parseColor(buttonColor));
        subLayout.addView(ok, buttonParams);


        /*
         * Cancel Button
         */
        TextView cancel = new TextView(context);
        cancel.setText("Cancel");
        cancel.setGravity(Gravity.CENTER);
        cancel.setTextSize(20);
        cancel.setPadding(15, 15, 15, 15);

        cancel.setTextColor(Color.parseColor(buttonColor));
        subLayout.addView(cancel, buttonParams);

        layout.addView(subLayout);


        parentLayout.addView(layout0);
        parentLayout.addView(layout);
        parentLayout.addView(layout2);

        popupMessage =
                new PopupWindow(parentLayout, LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, true);
        popupMessage.setContentView(parentLayout);
        popupMessage.setFocusable(true);
        popupMessage.update();

        popupMessage.showAtLocation(popupView, Gravity.CENTER, 0, 0);


        /*
         * Setting button click listener
         */
        ok.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                String value = input.getText().toString();

                if (!UtilString.isBlank(value)) {

                    //showing progress bar
                    progressBarLayout.setVisibility(View.VISIBLE);
                    editProfileLayout.setVisibility(View.GONE);

                    JoinedHelper.UpdateAssociatedName updateAssociatedName = new JoinedHelper.UpdateAssociatedName();
                    String[] params = new String[]{classcode, value};
                    updateAssociatedName.execute(params);

                    popupMessage.dismiss();
                }
            }
        });

        cancel.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                popupMessage.dismiss();
            }
        });

        layout0.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                popupMessage.dismiss();
            }
        });

        layout2.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                popupMessage.dismiss();
            }
        });
    }


      /*
       * Delete class popup
       */

    private void showDeleteClassPopUp(final View popupView, final Context context, final String classCode,
                                      final String className, final boolean deletionFlag, String displayText) {


        /*
         * Setting parent linear layout
         */
        LinearLayout layout = new LinearLayout(context);
        LinearLayout parentLayout = new LinearLayout(context);
        LinearLayout layout0 = new LinearLayout(context);
        LinearLayout layout2 = new LinearLayout(context);
        LinearLayout editTextLayout = new LinearLayout(context);

        parentLayout.setOrientation(LinearLayout.VERTICAL);


        LinearLayout.LayoutParams parentParams =
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 300);

        LinearLayout.LayoutParams parentParams2 =
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT);


        LinearLayout.LayoutParams techerParams =
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);

        layout0.setLayoutParams(parentParams);
        layout.setLayoutParams(techerParams);
        layout2.setLayoutParams(parentParams2);


        layout0.setBackgroundColor(Color.BLACK);
        layout2.setBackgroundColor(Color.BLACK);
        layout0.setAlpha(new Float(0.6));
        layout2.setAlpha(new Float(0.6));


        layout.setPadding(50, 40, 50, 20);
        layout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);

        String backgroudColor = "#f1f1f1";
        layout.setBackgroundColor(Color.parseColor(backgroudColor));

        /*
         * setting heading text in layout
         */
        TextView popupText = new TextView(context);
        popupText.setText(displayText);
        // popupText.setGravity(Gravity.CENTER );
        popupText.setTextSize(20);
        popupText.setPadding(15, 0, 15, 20);
        // popupText.setTypeface(null, Typeface.BOLD);
        layout.addView(popupText, params);

        String buttonColor = "#0099cc";

        /*
         * Setting edittext inside layout
         */
        LinearLayout.LayoutParams editParams =
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, 1);

        editTextLayout.setBackgroundColor(Color.parseColor("#949494"));
        editTextLayout.setLayoutParams(editParams);

        layout.addView(editTextLayout);


        /*
         * Adding layout containg ok and cancel button
         */
        LinearLayout subLayout = new LinearLayout(context);
        subLayout.setLayoutParams(params);
        subLayout.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout.LayoutParams buttonParams =
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);

        buttonParams.setMargins(0, 10, 0, 0);

        /*
         * ok button
         */

        TextView ok = new TextView(context);
        ok.setText("No");
        ok.setGravity(Gravity.CENTER);
        ok.setTextSize(20);
        ok.setPadding(15, 15, 15, 15);
        ok.setTextColor(Color.parseColor(buttonColor));
        subLayout.addView(ok, buttonParams);


        /*
         * Cancel Button
         */
        TextView cancel = new TextView(context);
        cancel.setText("Yes");
        cancel.setGravity(Gravity.CENTER);
        cancel.setTextSize(20);
        cancel.setPadding(15, 15, 15, 15);

        cancel.setTextColor(Color.parseColor(buttonColor));
        subLayout.addView(cancel, buttonParams);

        layout.addView(subLayout);


        parentLayout.addView(layout0);
        parentLayout.addView(layout);
        parentLayout.addView(layout2);

        popupMessage =
                new PopupWindow(parentLayout, LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, true);
        popupMessage.setContentView(parentLayout);
        popupMessage.setFocusable(true);
        popupMessage.update();

        popupMessage.showAtLocation(popupView, Gravity.CENTER, 0, 0);


        /*
         * Setting button click listner
         */
        cancel.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                if (deletionFlag) {
                    DeleteJoinedGroup deleteGroup = new DeleteJoinedGroup(classCode, className);
                    deleteGroup.execute();

                    popupMessage.dismiss();
                    progressBarLayout.setVisibility(View.VISIBLE);
                    editProfileLayout.setVisibility(View.GONE);
                } else {
                    popupMessage.dismiss();
                    if (context != null) {
                        Intent intent = new Intent(context, AddChildToClass.class);
                        intent.putExtra("code", classCode);
                        intent.putExtra("backFlag", "1");   // if user return from addChildToClass class, then come back to this class rather than joinclass
                        //startActivity(intent);


                        if (context != null)
                            startActivityForResult(intent, 0);
                    }

                }
            }
        });

        ok.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                popupMessage.dismiss();
            }
        });

        layout0.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                popupMessage.dismiss();
            }
        });

        layout2.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                popupMessage.dismiss();
            }
        });


    }


}
