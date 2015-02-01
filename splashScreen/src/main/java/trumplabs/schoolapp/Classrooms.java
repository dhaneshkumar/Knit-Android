package trumplabs.schoolapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.parse.ParseException;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;

import BackGroundProcesses.CreatedClassRooms;
import library.UtilString;
import trumplab.textslate.R;
import utility.Queries;
import utility.Tools;
import utility.Utility;

public class Classrooms extends Fragment {
    private Activity getactivity;
    public static ListView listv;
    private LinearLayout emptylayout;
    public static List<List<String>> createdGroups;
    protected LayoutInflater layoutinflater;
    public static BaseAdapter myadapter;
    private String groupCode;
    private ParseUser user;
    public static int members;
    private String userId;
    private Queries query;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        layoutinflater = inflater;
        // getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        View layoutview = inflater.inflate(R.layout.createdclasses_layout, container, false);
        return layoutview;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        query = new Queries();
        getactivity = getActivity();
        listv = (ListView) getactivity.findViewById(R.id.listview);
        emptylayout = (LinearLayout) getactivity.findViewById(R.id.ccemptymsg);

        getactivity.findViewById(R.id.ccemptylink).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getactivity, CreateClass.class));
            }
        });


        ParseUser parseObject = ParseUser.getCurrentUser();

        if (parseObject == null)
            {Utility.logout(); return;}

        userId = parseObject.getUsername();

        createdGroups = parseObject.getList(Constants.CREATED_GROUPS);

        Log.d("stop", "class rooms : ---------");

        if (createdGroups == null) {
            createdGroups = new ArrayList<List<String>>();
        }

        myadapter = new myBaseAdapter();
        // Log.d("FragmentB", "intial grp size : " + createdGroups.size());

        listv.setAdapter(myadapter);
        View emptyrow = layoutinflater.inflate(R.layout.createdclasses_classview, listv, false);
        listv.setEmptyView(emptyrow);

        initialiseListViewMethods();
        super.onActivityCreated(savedInstanceState);

    /*
     * //Refreshing options if (Utility.isInternetOn()) { GetDataFromServer gf = new
     * GetDataFromServer(); gf.execute(); } else { Utility.toast("Check your Internet connection");
     * }
     */
    }

    @Override
    public void onResume() {
        super.onResume();
        myadapter.notifyDataSetChanged();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.refresh:
                if (Utility.isInternetOn(getActivity())) {

                    if (MainActivity.mHeaderProgressBar != null) {
                        Tools.runSmoothProgressBar(MainActivity.mHeaderProgressBar, 10);
                    }

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

    class myBaseAdapter extends BaseAdapter {

        @Override
        public int getCount() {

            if (createdGroups == null)
                createdGroups = new ArrayList<List<String>>();

            if (createdGroups.size() == 0) {
                listv.setVisibility(View.GONE);
                emptylayout.setVisibility(View.VISIBLE);
            } else {
                listv.setVisibility(View.VISIBLE);
                emptylayout.setVisibility(View.GONE);
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
                row = layoutinflater.inflate(R.layout.createdclasses_classview, parent, false);
            }

            TextView classimage1 = (TextView) row.findViewById(R.id.classimage1);
            TextView classname1 = (TextView) row.findViewById(R.id.classname1);
            TextView classmembers1 = (TextView) row.findViewById(R.id.classmembers1);
            TextView classcode1 = (TextView) row.findViewById(R.id.classcode1);

            int memberCount = 0;

            try {
                memberCount = query.getMemberCount(createdGroups.get(position).get(0));
            } catch (ParseException e) {

            }

            classmembers1.setText(memberCount + " members");
            classcode1.setText(createdGroups.get(position).get(0));
            String classnamestr = createdGroups.get(position).get(1);


            String Str = null;
            if (!UtilString.isBlank(classnamestr)) {
                Str = classnamestr.trim();
                classnamestr = Str;
            }

            //setting background color of circular image
            GradientDrawable gradientdrawable = (GradientDrawable) classimage1.getBackground();
            gradientdrawable.setColor(Color.parseColor(Utility.classColourCode(classnamestr.toUpperCase())));
            classname1.setText(classnamestr.toUpperCase());                 //setting class name
            classimage1.setText(classnamestr.substring(0, 1).toUpperCase());    //setting front end of circular image

            return row;
        }
    }

    public void initialiseListViewMethods() {

        listv.setOnItemLongClickListener(new OnItemLongClickListener() {

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


        listv.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getactivity, ClassContainer.class);

                intent.putExtra("selectedclass", createdGroups.get(position).get(0));
                intent.putExtra("selectedclassName", createdGroups.get(position).get(1));
                Log.d("FragmentB", "user details : " + createdGroups.get(position).get(0)
                        + createdGroups.get(position).get(1));
                startActivity(intent);
            }
        });
    }
}
