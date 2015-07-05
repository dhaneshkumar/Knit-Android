package trumplabs.schoolapp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.webkit.WebView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.parse.ParseException;
import com.parse.ParseUser;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import trumplab.textslate.R;
import utility.Config;
import utility.Queries;
import utility.Tools;
import utility.Utility;

/**
 * Created by dhanesh on 16/6/15.
 */
public class ComposeMessage extends ActionBarActivity implements ChooserDialog.CommunicatorInterface{
    public static final String LOGTAG = "DEBUG_COMPOSE_MESSAGE";
    private RelativeLayout sendTo;
    private List<List<String>> classList;
    private final String TRUE = "true";
    private final String FALSE = "false";
    private String selectedClasses="";
    private WebView selectedClassTV;
    private TextView classTextView;
    private List<String> selectedClassNames;
    private ImageView doneImageView;
    public static LinearLayout sendimgpreview;
    public static LinearLayout picProgressBarLayout;
    public static ImageView sendimgview;
    private ImageView removebutton;
    private Typeface typeface;

    public static String source = Constants.ComposeSource.INSIDE;
                                //i.e inside the particular class page
                                //"OUTSIDE" i.e from MainActivity
    //will be used by ComposeMessageHelper to decide what all things to update on sending a message


    /* offline msg flags*/
    public static boolean sendButtonClicked = false; //to show status 'sending' in msg items when background sender job is NOT running & is about to get start(i.e jobRunning flag not set)
    //this is a quick hack because thread is spawned only when message has been pinned locally(after slight delay), however message is shown in the list immediately.
    //So we need to show the status of msg as 'sending' even though jobRunning flag is not yet set
    //It is cleared when either msg is added to queue or new job started

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.compose_message);

        sendTo = (RelativeLayout) findViewById(R.id.sendTo);
        final ListView classeslistview = (ListView) findViewById(R.id.classeslistview);
        selectedClassTV = (WebView) findViewById(R.id.selectedClass);
        selectedClassTV.loadUrl("file:///android_asset/selectClass.html");
        selectedClassTV.getSettings().setJavaScriptEnabled(true);
        doneImageView = (ImageView) findViewById(R.id.done);
        classTextView = (TextView) findViewById(R.id.classTV);
        sendimgpreview = (LinearLayout) findViewById(R.id.imgpreview);
        picProgressBarLayout = (LinearLayout) findViewById(R.id.progressBarLayout);
        sendimgview = (ImageView) findViewById(R.id.attachedimg);
        removebutton = (ImageView) findViewById(R.id.removebutton);
        typeface = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Light.ttf");

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("New Message");

        selectedClassNames = new ArrayList<>();
        classList = ParseUser.getCurrentUser().getList(Constants.CREATED_GROUPS);
        if(classList == null)
            classList = new ArrayList<List<String>>();

        String selectedClassCode = null;
        boolean selectAllClasses = false;

        if(getIntent() != null && getIntent().getExtras() != null)
        {

            if(getIntent().getExtras().getBoolean("SELECT_ALL"))
            {
                selectAllClasses = true;
                for(int i=0; i < classList.size();i++) {
                    selectedClassNames.add(classList.get(i).get(1));

                    selectedClasses +="<span style=\" border: 1px solid #0288D1;display:inline-block; padding: 8px 16px;  font-size: 14px; background: #03A9F4; color:#ffffff; margin-bottom:5px; margin-right:5px; border-radius: 25px;\">"
                            + classList.get(i).get(1) + "</span>";
                }
            }
            else {
                if(getIntent().getExtras().getString("CLASS_CODE") != null)
                {
                    selectedClassCode = getIntent().getExtras().getString("CLASS_CODE");
                    String selectedClassName = getIntent().getExtras().getString("CLASS_NAME");

                    selectedClassNames.add(selectedClassName);

                    selectedClasses ="<span style=\" border: 1px solid #0288D1;display:inline-block; padding: 8px 16px;  font-size: 14px; background: #03A9F4; color:#ffffff; margin-bottom:5px; margin-right:5px; border-radius: 25px;\">"
                            + selectedClassName + "</span>";
                }
            }

            if(getIntent().getExtras().getString(Constants.ComposeSource.KEY) != null){
                source = getIntent().getExtras().getString(Constants.ComposeSource.KEY);
            }

            selectedClassTV.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    selectedClassTV.loadUrl("javascript:replace( '" + selectedClasses + "')");
                }
            });
        }

        classTextView.setText(displayText(selectedClassNames));

        if (selectAllClasses) {
            for (int i = 0; i < classList.size(); i++) {
                classList.get(i).add(2, TRUE);
            }
        } else {
            for (int i = 0; i < classList.size(); i++) {
                List<String> item = classList.get(i);

                item.add(2, FALSE);

                if (selectedClassCode != null) {
                    if (item.get(0).equals(selectedClassCode))
                        item.add(2, TRUE);
                }
            }
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

                   if(classList.size() > 0) {
                       Tools.hideKeyboard(ComposeMessage.this);
                       classeslistview.setVisibility(View.VISIBLE);
                       selectedClassTV.setVisibility(View.VISIBLE);
                       classTextView.setVisibility(View.GONE);
                       doneImageView.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_action_tick));

                   }
                    else {
                       classTextView.setText("Sorry! No Created Classrooms");
                       classTextView.setTextColor(Color.RED);
                   }

                }
            }
        });


        //setting adapter

        SelectClassAdapter selectClassAdapter = new SelectClassAdapter();
        classeslistview.setAdapter(selectClassAdapter);

        //on click show full image
        sendimgview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent imgintent = new Intent();
                imgintent.setAction(Intent.ACTION_VIEW);
                imgintent.setDataAndType(Uri.parse("file://" + (String) sendimgpreview.getTag()), "image/*");
                startActivity(imgintent);
            }
        });


        // remove the image ready to be sent
        removebutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendimgpreview.setTag("");
                sendimgview.setImageBitmap(null);
                sendimgpreview.setVisibility(View.GONE);

               /* if (typedmsg.getText() != null) {
                    if (typedmsg.getText().length() < 1)
                        sendmsgbutton.setImageResource(R.drawable.send_grey);
                }*/
            }
        });
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

            case R.id.attachment:
                FragmentManager fm = getSupportFragmentManager();
                ChooserDialog openchooser = new ChooserDialog();
                openchooser.show(fm, "Add Image");
                break;

            case R.id.send:
                Tools.hideKeyboard(ComposeMessage.this);
                checkTime();
                break;

            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    public void  checkTime()
    {
        int hourOfDay = -1;

        //using local time instead of session.getCurrentTime
        Calendar cal = Calendar.getInstance();
        hourOfDay = cal.get(Calendar.HOUR_OF_DAY);

        Log.d(ComposeMessage.LOGTAG, "send() : hourOfDay=" + hourOfDay);

        if (hourOfDay != -1) {

            //If current message time is not sutaible <9PM- 6AM> then show this warning as popup to users
            if (hourOfDay >= Config.messageNormalEndTime || hourOfDay < Config.messageNormalStartTime) {
                //note >= and < respectively because disallowed are [ >= EndTime and < StartTime]
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                LinearLayout warningView = new LinearLayout(this);
                warningView.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams nameParams =
                        new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT);
                nameParams.setMargins(30, 30, 30, 30);

                final TextView nameInput = new TextView(this);
                nameInput.setTextSize(16);
                nameInput.setText(Config.messageTimeWarning);
                nameInput.setGravity(Gravity.CENTER_HORIZONTAL);
                warningView.addView(nameInput, nameParams);
                builder.setView(warningView);


                builder.setPositiveButton("SEND", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        sendNow();
                    }
                });
                builder.setNegativeButton("CANCEL", null);
                AlertDialog dialog = builder.create();
                dialog.setCanceledOnTouchOutside(true);
                dialog.show();

            } else {
                sendNow();
            }
        } else {
            sendNow();
        }
    }

    public void sendNow(){
        //get the selected classes from classlist here
        List<List<String>> selectedClassList = new ArrayList<>();
        for(List<String> c : classList){
            if(c.size() > 2 && c.get(2).equals(TRUE)){
                selectedClassList.add(c);
            }
        }

        Log.d(LOGTAG, "selectedClassList size=" + selectedClassList.size());

        ComposeMessageHelper composeMessageHelper = new ComposeMessageHelper(this, selectedClassList);
        composeMessageHelper.sendFunction();

        if(source.equals(Constants.ComposeSource.OUTSIDE)){
            MainActivity.goToOutboxFlag = true;
        }

        finish(); //activity over. Return to parent(MainActivity or SendMessage - whatsoever it may be)
    }

    @Override
    public void sendImagePic(String imgname) {

        // The image was brought into the App folder hence only name was passed
        ComposeMessage.sendimgpreview.setVisibility(View.VISIBLE);
        ComposeMessage.sendimgpreview.setTag(Utility.getWorkingAppDir() + "/media/" + imgname);
        File thumbnailFile = new File(Utility.getWorkingAppDir() + "/thumbnail/" + imgname);

        // The thumbnail is already created
        Bitmap myBitmap = BitmapFactory.decodeFile(thumbnailFile.getAbsolutePath());
        sendimgview.setImageBitmap(myBitmap);
    }


    /**
     * Adapter to show created class lists while selecting classrooms to send msg
     */
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
            TextView memberCountTV = (TextView) row.findViewById(R.id.memberCount);

            final List<String> item = classList.get(position);
            className.setText(item.get(1));

            try {
                Queries memberQuery = new Queries();
                int memberCount = memberQuery.getMemberCount(item.get(0));

                memberCountTV.setText(memberCount+" Members");

            } catch (ParseException e) {
                e.printStackTrace();
            }

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
                    headerText.setTypeface(typeface);
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
                            headerText.setTypeface(typeface);
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
                        classTextView.setText(displayText(selectedClassNames));

                    }
                }
            });


            return row;
        }
    }


    /**
     *
     * @param selectedClassNames
     * @return string to display in format of < & -- more>
     */
    private String displayText(List<String> selectedClassNames)
    {
        String start = "";
        String end = "";

        if(selectedClassNames == null)
            return "";

        int size = selectedClassNames.size();

        if(size ==1)
            start = selectedClassNames.get(0);
        else if(size > 1)
            start = selectedClassNames.get(0) + ", " + selectedClassNames.get(1);

        if(size >2)
        {
            end = " & " + (size - 2) +" more";
        }

        return start+end;
    }
}
