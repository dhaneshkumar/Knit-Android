package trumplabs.schoolapp;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;

import trumplab.textslate.R;
import utility.Tools;
import utility.Utility;

/**
 * Created by dhanesh on 16/6/15.
 */
public class ComposeMessage extends ActionBarActivity {

    private RelativeLayout sendTo;
    private List<List<String>> classList;
    private final String TRUE = "true";
    private final String FALSE = "false";
    private String selectedClasses="";
    private WebView selectedClassTV;
    private TextView classTextView;
    private List<String> selectedClassNames;
    private ImageView doneImageView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.compose_message);

        sendTo = (RelativeLayout) findViewById(R.id.sendTo);
        final ListView classeslistview = (ListView) findViewById(R.id.classeslistview);
        selectedClassTV = (WebView) findViewById(R.id.selectedClass);
        selectedClassTV.getSettings().setJavaScriptEnabled(true);
        selectedClassTV.loadUrl("file:///android_asset/selectClass.html");
        doneImageView = (ImageView) findViewById(R.id.done);

        classTextView = (TextView) findViewById(R.id.classTV);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("New Message");

        classList = ParseUser.getCurrentUser().getList(Constants.CREATED_GROUPS);
        if(classList == null)
            classList = new ArrayList<List<String>>();

        selectedClassNames = new ArrayList<>();


        //adding false string as 3rd column to array <Class-code, class-name, "false">
        for(int i=0; i<classList.size(); i++)
        {
            List<String> item = classList.get(i);
            if(item.size()<3)
                item.add(FALSE);
            else
                item.add(2, FALSE);
        }


        sendTo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(classeslistview.getVisibility() == View.VISIBLE) {
                    classeslistview.setVisibility(View.GONE);
                    selectedClassTV.setVisibility(View.GONE);
                    classTextView.setVisibility(View.VISIBLE);
                    doneImageView.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_mode_edit));

                }
                else {
                    Tools.hideKeyboard(ComposeMessage.this);
                    classeslistview.setVisibility(View.VISIBLE);
                    selectedClassTV.setVisibility(View.VISIBLE);
                    classTextView.setVisibility(View.GONE);
                    doneImageView.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_action_tick));

                }
            }
        });


        //setting adapter

        SelectClassAdapter selectClassAdapter = new SelectClassAdapter();
        classeslistview.setAdapter(selectClassAdapter);
    }


        @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
                inflater.inflate(R.menu.compose_message_menu, menu);
        super.onCreateOptionsMenu(menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                Tools.hideKeyboard(ComposeMessage.this);
                onBackPressed();
                break;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    class SelectClassAdapter extends BaseAdapter {

        @Override
        public int getCount() {

            if (classList == null)
                classList = new ArrayList<List<String>>();

            return classList.size();
        }

        @Override
        public Object getItem(int position) {
            return classList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }


        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            if (row == null) {
                LayoutInflater inflater = (LayoutInflater) ComposeMessage.this
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                row = inflater.inflate(R.layout.select_class_item, parent, false);
            }

            final LinearLayout headerLayout = (LinearLayout) row.findViewById(R.id.header);
            final TextView headerText = (TextView) row.findViewById(R.id.headerText);
            TextView className = (TextView) row.findViewById(R.id.classname);
            final ImageView headerImage = (ImageView) row.findViewById(R.id.headerImage);
            RelativeLayout rootLayout = (RelativeLayout) row.findViewById(R.id.root);

            final List<String> item = classList.get(position);
            className.setText(item.get(1));

            if(item.size()>2)
            {
                if(item.get(2).equals(FALSE))
                {
                    //setting background color of circular image
                    GradientDrawable gradientdrawable = (GradientDrawable) headerLayout.getBackground();
                    gradientdrawable.setColor(Color.parseColor(Utility.classColourCode(item.get(1).toUpperCase())));

                    headerText.setVisibility(View.VISIBLE);
                    headerImage.setVisibility(View.GONE);
                    headerText.setText(item.get(1).substring(0, 1).toUpperCase());    //setting front end of circular image
                }
                else
                {
                    //setting background color of circular image - blue
                    GradientDrawable gradientdrawable = (GradientDrawable) headerLayout.getBackground();
                    gradientdrawable.setColor(getResources().getColor(R.color.color_primary));

                    headerText.setVisibility(View.GONE);
                    headerImage.setVisibility(View.VISIBLE);
                }
            }

            rootLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(item.size()>2)
                    {
                        final String mimeType = "text/html";
                        final String encoding = "UTF-8";
                        String content ="<span style=\" border: 1px solid #0288D1;display:inline-block; padding: 8px 16px;  font-size: 14px; background: #03A9F4; color:#ffffff; margin-bottom:5px; margin-right:5px; border-radius: 25px;\">"
                                + item.get(1) + "</span>";

                        String tvContent =  item.get(1).trim();

                        if(item.get(2).equals(TRUE))
                        {
                            item.add(2, FALSE); //removing selection
                            selectedClasses = selectedClasses.replace(content, "");

                            selectedClassNames.remove(tvContent);

                            //setting background color of circular image
                            GradientDrawable gradientdrawable = (GradientDrawable) headerLayout.getBackground();
                            gradientdrawable.setColor(Color.parseColor(Utility.classColourCode(item.get(1).toUpperCase())));

                            headerText.setVisibility(View.VISIBLE);
                            headerImage.setVisibility(View.GONE);
                            headerText.setText(item.get(1).substring(0, 1).toUpperCase());    //setting front end of circular image
                        }
                        else
                        {
                            item.add(2, TRUE); //adding to selection

                            selectedClasses += content;
                            selectedClassNames.add(tvContent);

                            //setting background color of circular image - blue
                            GradientDrawable gradientdrawable = (GradientDrawable) headerLayout.getBackground();
                            gradientdrawable.setColor(getResources().getColor(R.color.color_primary));

                            headerText.setVisibility(View.GONE);
                            headerImage.setVisibility(View.VISIBLE);
                        }

                        selectedClassTV.loadUrl("javascript:replace( '" + selectedClasses + "')");

                       int size = selectedClassNames.size();

                        String start = "";
                        String end = "";

                        if(size ==1)
                            start = selectedClassNames.get(0);
                        else if(size > 1)
                            start = selectedClassNames.get(0) + ", " + selectedClassNames.get(1);

                        if(size >2)
                        {
                            end = " & " + (size - 2) +" more";
                        }

                        classTextView.setText(start + end);

                    }
                }
            });


            return row;
        }
    }
}
