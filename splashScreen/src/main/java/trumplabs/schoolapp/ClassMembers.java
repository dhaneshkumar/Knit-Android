package trumplabs.schoolapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.PopupMenu;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import BackGroundProcesses.MemberList;
import additionals.InviteParents;
import fr.castorflex.android.smoothprogressbar.SmoothProgressBar;
import trumplab.textslate.R;
import utility.Queries;
import utility.Tools;
import utility.Utility;

/**
 * Show subscriber list of a particular class
 */
public class ClassMembers extends Fragment {
    private Activity getactivity;
    private ListView listv;
    protected LayoutInflater layoutinflater;
    public static BaseAdapter myadapter;
    public static List<MemberDetails> memberDetails;
    private String groupCode = ClassContainer.classuid;
    private Queries query;
    public static SmoothProgressBar mHeaderProgressBar;
    private Context context;
    public static LinearLayout progressBarLayout;
    public static LinearLayout editProfileLayout;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        layoutinflater = inflater;
        View layoutview = inflater.inflate(R.layout.classmembers_layout, container, false);
        return layoutview;
    }

    /*
    * Initializing variables required for calling background process
    */
    public void intializeBackgroundParameters() {
        query = new Queries();

        if (memberDetails == null)
            memberDetails = new ArrayList<MemberDetails>();

        LocalMembers localMembers = new LocalMembers(groupCode);
        localMembers.execute();

        myadapter = new myBaseAdapter();

    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        intializeBackgroundParameters();

        getactivity = getActivity();

        //intializing variables
        listv = (ListView) getactivity.findViewById(R.id.memberslistview);
        mHeaderProgressBar = (SmoothProgressBar) getActivity().findViewById(R.id.ptr_progress);
        progressBarLayout = (LinearLayout) getActivity().findViewById(R.id.progressBarLayout);
        editProfileLayout = (LinearLayout) getActivity().findViewById(R.id.editLayout);

        context = getActivity();
        listv.setAdapter(myadapter);
        super.onActivityCreated(savedInstanceState);

        //moving to invite parent activity
        LinearLayout inviteLayout = (LinearLayout) getActivity().findViewById(R.id.inviteLayout);
        inviteLayout.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity().getBaseContext(), InviteParents.class);
                startActivity(intent);
            }
        });

    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu5, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.refresh:
                if (Utility.isInternetOn(getActivity())) {

                    //showing progress bar
                    if (mHeaderProgressBar != null) {
                        Tools.runSmoothProgressBar(mHeaderProgressBar, 10);
                    }

                    //refreshing member list in background
                    MemberList memberList = new MemberList(groupCode);
                    memberList.execute();
                } else {
                    Utility.toast("Check your Internet connection");
                }
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /*
    Adapter to show member list
     */
    class myBaseAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return memberDetails.size();
        }

        @Override
        public Object getItem(int position) {

            return memberDetails.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View row = convertView;
            if (row == null) {
                row = layoutinflater.inflate(R.layout.classmember_memberview, parent, false);
            }

            TextView membername = (TextView) row.findViewById(R.id.membernameid);
            membername.setText(((MemberDetails) getItem(position)).getChildName());
            ImageView option_imageView = (ImageView) row.findViewById(R.id.joinOpt);

            row.setBackgroundDrawable(getResources().getDrawable(R.drawable.greyoutline));

        /*
           * Setting options for items
           */
            option_imageView.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    final View popView = v;

                    if (context == null)
                        return;

                    /** Instantiating PopupMenu class */
                    final PopupMenu popup = new PopupMenu(context, v);


                    /** Adding menu items to the popumenu */
                    popup.getMenuInflater().inflate(R.menu.memberlist_remove_child, popup.getMenu());

                    /** Defining menu item click listener for the popup menu */
                    popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {


                        @Override
                        public boolean onMenuItemClick(MenuItem item) {

                            popup.dismiss();


                            switch (item.getItemId()) {
                                case R.id.action1:

                                  /*
                                    Remove this child from group
                                   */
                                    if (context != null) {
                                        showRemoveChildPopUp(popView, context, memberDetails.get(position));
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


            return row;
        }
    }

    class LocalMembers extends AsyncTask<Void, Void, Void> {

        private String code;

        LocalMembers(String groupCode) {
            this.code = groupCode;
        }

        @Override
        protected Void doInBackground(Void... params) {

            memberDetails = query.getLocalClassMembers(code);
            return null;
        }

        @Override
        protected void onPostExecute(Void res) {
            if (myadapter != null)
                myadapter.notifyDataSetChanged();

        }
    }


     /*
       * Delete class popup
       */

    private void showRemoveChildPopUp(final View popupView, final Context context, final MemberDetails member) {


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
        popupText.setText("Are you sure want to remove this child? ");
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
        ok.setText("Yes");
        ok.setGravity(Gravity.CENTER);
        ok.setTextSize(20);
        ok.setPadding(15, 15, 15, 15);
        ok.setTextColor(Color.parseColor(buttonColor));
        subLayout.addView(ok, buttonParams);


        /*
         * Cancel Button
         */
        TextView cancel = new TextView(context);
        cancel.setText("No");
        cancel.setGravity(Gravity.CENTER);
        cancel.setTextSize(20);
        cancel.setPadding(15, 15, 15, 15);

        cancel.setTextColor(Color.parseColor(buttonColor));
        subLayout.addView(cancel, buttonParams);

        layout.addView(subLayout);


        parentLayout.addView(layout0);
        parentLayout.addView(layout);
        parentLayout.addView(layout2);

        final PopupWindow popupMessage = new PopupWindow(parentLayout, LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.FILL_PARENT, true);
        popupMessage.setContentView(parentLayout);
        popupMessage.setFocusable(true);
        popupMessage.update();

        popupMessage.showAtLocation(popupView, Gravity.CENTER, 0, 0);


        /*
         * Setting button click listner
         */
        ok.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                popupMessage.dismiss();

                progressBarLayout.setVisibility(View.VISIBLE);
                editProfileLayout.setVisibility(View.GONE);

                Utility.ls("calling background");
                RemoveChild removeChild = new RemoveChild(member);
                removeChild.execute();


            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                popupMessage.dismiss();
            }
        });

        layout0.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                popupMessage.dismiss();
            }
        });

        layout2.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                popupMessage.dismiss();
            }
        });


    }


    class RemoveChild extends AsyncTask<Void, Void, Boolean> {
        private MemberDetails member;

        RemoveChild(MemberDetails member) {
            this.member = member;

        }

        @Override
        protected Boolean doInBackground(Void... params) {

            Utility.ls("remove child starting in background....");

            //Steps :
            //remove groupCode from member's joined group
            // unsubscribe this channel from member's channels
            //remove entry of that class from subscriber's joined group
            // send notification to user about removing from this group
            // set status to "REMOVED" table entry of memberlist

            if (member != null) {
                String type = member.getType();
                String objectId = member.getObjectId();
                String memberId = null;
                String groupCode = null;

                if (type.equals(MemberDetails.APP_MEMBER)) {
                    ParseQuery<ParseObject> query = ParseQuery.getQuery(Constants.GROUP_MEMBERS);
                    query.fromLocalDatastore();
                    query.whereEqualTo("objectId", objectId);
                    query.whereEqualTo("userId", ParseUser.getCurrentUser());
                    ParseObject obj = null;

                    try {

                        obj = query.getFirst();
                        if (obj != null) {
                            memberId = obj.getString("emailId");
                            groupCode = obj.getString("code");

                            boolean isRemoved = false;

                            HashMap<String, String> param = new HashMap<String, String>();
                            param.put("classcode", groupCode);
                            param.put("emailId", memberId);
                            param.put("type", "APP");
                            isRemoved = ParseCloud.callFunction("removechild", param);

                            if(isRemoved){

                                obj.fetch();        //retrieving updates from server
                                return true;
                            }


                        }
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }

                }
                else
                {
                        ParseQuery<ParseObject> query = ParseQuery.getQuery(Constants.MESSAGE_NEEDERS);
                        query.fromLocalDatastore();
                        query.whereEqualTo("objectId", objectId);
                        query.whereEqualTo("userId", ParseUser.getCurrentUser());
                        ParseObject obj = null;

                        try {

                            obj = query.getFirst();
                            if (obj != null) {
                                memberId = obj.getString("number");
                                groupCode = obj.getString("code");


                                //Retrieving class-name
                                ParseQuery<ParseObject> codeGroupQuery = ParseQuery.getQuery(Constants.CODE_GROUP);
                                codeGroupQuery.fromLocalDatastore();
                                codeGroupQuery.whereEqualTo("code", groupCode);

                                ParseObject codeObject = codeGroupQuery.getFirst();

                                String className = "Knit";
                                if(codeObject != null)
                                {
                                    className = codeObject.getString("name");
                                }



                                boolean isRemoved = false;

                                HashMap<String, String> param = new HashMap<String, String>();
                                param.put("classcode", groupCode);
                                param.put("emailId", memberId);
                                param.put("usertype", "SMS");
                                param.put("classname", className);
                                isRemoved = ParseCloud.callFunction("removechild", param);

                                if(isRemoved){
                                    obj.fetch();        //retrieving updates from server
                                return true;
                            }

                            }
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                }
            }

            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                memberDetails.remove(member);
                myadapter.notifyDataSetChanged();
            }

            progressBarLayout.setVisibility(View.GONE);
            editProfileLayout.setVisibility(View.VISIBLE);
        }
    }
}
