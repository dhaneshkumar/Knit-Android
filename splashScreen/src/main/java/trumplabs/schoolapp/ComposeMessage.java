package trumplabs.schoolapp;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;

import trumplab.textslate.R;
import utility.Utility;

/**
 * Created by dhanesh on 16/6/15.
 */
public class ComposeMessage extends ActionBarActivity {

    private RelativeLayout sendTo;
    private TableLayout selectedLayout;
    private List<List<String>> classList;
    private List<List<String>> selectedClassList;
    private int displayWidth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.compose_message);

        sendTo = (RelativeLayout) findViewById(R.id.sendTo);
        selectedLayout = (TableLayout) findViewById(R.id.selectedClass);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("New Message");

        classList = ParseUser.getCurrentUser().getList(Constants.CREATED_GROUPS);
        selectedClassList = new ArrayList<>();

        sendTo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(classList == null || classList.size() ==0)
                    Utility.toast("Sorry! You have not created any classroom.");
                else
                    showSelectColoursDialog();
            }
        });

        //measuring screen width
        WindowManager w = getWindowManager();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            Point size = new Point();
            w.getDefaultDisplay().getSize(size);
            displayWidth = (int) (size.x * 0.95);   //taking 0.95 of screen to use
        } else {
            Display d = w.getDefaultDisplay();
            displayWidth = (int) (d.getWidth() * 0.95);
        }
    }

    protected void showSelectColoursDialog() {

        if(classList == null || classList.size() ==0)
            return;

        if(selectedClassList == null)
            selectedClassList = new ArrayList<>();

        boolean[] checkedClasses = new boolean[classList.size()];

        int count = classList.size();

        String[] classNamesArray = new String[count];

        for(int i = 0; i < count; i++) {
            classNamesArray[i] = classList.get(i).get(1);

            for(int j=0; j<selectedClassList.size(); j++)
            {
                if(selectedClassList.get(j).get(0).equals(classList.get(i).get(0))) {
                    checkedClasses[i] = true;
                    break;
                }
                else
                    checkedClasses[i] =false;
            }
        }

        DialogInterface.OnMultiChoiceClickListener classesDialogListener = new DialogInterface.OnMultiChoiceClickListener() {

            @Override

            public void onClick(DialogInterface dialog, int postion, boolean isChecked) {

                if(isChecked)
                    selectedClassList.add(classList.get(postion));

                else
                    selectedClassList.remove(classList.get(postion));

                onChangeSelectedColours();

            }

        };



        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Classrooms");

        builder.setMultiChoiceItems(classNamesArray, checkedClasses, classesDialogListener);

        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();

    }


    protected void onChangeSelectedColours() {

        String str= "";
        int totalwidth = 0;

        //remove old views
        selectedLayout.removeAllViews();

        for(int i=0; i<selectedClassList.size(); i++)
        {
            str += selectedClassList.get(i).get(1);

            TextView textview = new TextView(this);
            textview.setText(selectedClassList.get(i).get(1));
            textview.setPadding(16, 8, 16, 8);
            textview.setTextSize(16);
            textview.setTextColor(Color.BLUE);

            int count = selectedLayout.getChildCount();
            if( count > 0)
            {
                View view = selectedLayout.getChildAt(count-1);

                if (view instanceof TableRow) {
                    TableRow row = (TableRow) view;
                    row.addView(textview);
                }

                view.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
                    @Override
                    public void onSystemUiVisibilityChange(int visibility) {

                    }
                });
            }
            else
            {
                TableRow row = new TableRow(this);
                row.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                row.addView(textview);
                selectedLayout.addView(row);
            }
        }

        Utility.toast(str);

    }

    @Override
    public void onWindowFocusChanged (boolean hasFocus) {
        // the height will be set at this point

        Log.d("compose", "0000 width = "  + " - " + displayWidth);

        int count = selectedLayout.getChildCount();
        if( count !=0)
        {
            View view = selectedLayout.getChildAt(count-1);

            if (view instanceof TableRow) {
                TableRow row = (TableRow) view;

                int lastColumn = row.getChildCount()-1;

                if(row.getWidth() > displayWidth && lastColumn >=0)
                {
                    View tv = row.getChildAt(lastColumn);
                    TextView textView = (TextView) tv;

                    row.removeViewAt(lastColumn);

                    //creat a new row and put there last element

                    Log.d("compose", "0000 width = " +  textView.getWidth() + " - " + displayWidth);
                    //adding to new row

                    TableRow new_row = new TableRow(this);
                    new_row.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                    new_row.addView(textView);
                    selectedLayout.addView(new_row);
                }

            }
        }

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
                onBackPressed();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }




}
