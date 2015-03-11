package profileDetails;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.GetDataCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.RequestPasswordResetCallback;
import com.parse.SaveCallback;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import additionals.SchoolAutoComplete;
import baseclasses.MyActionBarActivity;
import joinclasses.School;
import library.DelayAutoCompleteTextView;
import library.UtilString;
import trumplab.textslate.R;
import trumplabs.schoolapp.Constants;
import trumplabs.schoolapp.FeedBackClass;
import trumplabs.schoolapp.MainActivity;
import utility.Utility;

public class ProfilePage extends MyActionBarActivity implements OnClickListener {
    private ImageView profileimgview;
    private TextView name_textView;
    public static TextView school_textView;
    private TextView phone_textView;
    private String userId;
    private String picName;
    private String filePath;
    private String name;
    private String phone;
    public static String school;
    private School school1;
    public static LinearLayout progressBarLayout;
    public static LinearLayout profileLayout;
    private ArrayAdapter schoolsAdapter;
    private Context actcontext;
    private Point p;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile);
        actcontext = this;

        name_textView = (TextView) findViewById(R.id.name);
        phone_textView = (TextView) findViewById(R.id.phone);
        school_textView = (TextView) findViewById(R.id.school);
        profileimgview = (ImageView) findViewById(R.id.profileimg);

        TextView profile = (TextView) findViewById(R.id.profile);
        TextView account = (TextView) findViewById(R.id.account);
        TextView about = (TextView) findViewById(R.id.about);
        LinearLayout editName = (LinearLayout) findViewById(R.id.editName);
        LinearLayout editPhone = (LinearLayout) findViewById(R.id.editPhone);
        LinearLayout editSchool = (LinearLayout) findViewById(R.id.editSchool);
      //  TextView changePassword = (TextView) findViewById(R.id.changePassword);
        TextView rateOurApp = (TextView) findViewById(R.id.rateOurApp);
        TextView faq = (TextView) findViewById(R.id.faq);
        TextView feedback = (TextView) findViewById(R.id.feedback);
        TextView signOut = (TextView) findViewById(R.id.signOut);
        progressBarLayout = (LinearLayout) findViewById(R.id.progressLayout);
        profileLayout = (LinearLayout) findViewById(R.id.profileLayout);
    /*
     * setting condensed font
     */
        Typeface typeFace = Typeface.createFromAsset(getAssets(), "fonts/roboto-condensed.bold.ttf");
        profile.setTypeface(typeFace);
        account.setTypeface(typeFace);
        about.setTypeface(typeFace);





        ParseUser user = ParseUser.getCurrentUser();

        if (user == null) {
            Utility.logout();
            return;
        }

        userId = user.getUsername();
        String role = user.getString(Constants.ROLE);

        //  if ( !role.equals(Constants.TEACHER))
        editSchool.setVisibility(View.GONE);


        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    /*
     * Setting local data
     */
        name = user.getString(Constants.NAME);
        phone = user.getString(Constants.PHONE);
        //school = user.getString(Constants.SCHOOL);

        if (!UtilString.isBlank(name))
            name_textView.setText(name);

        phone_textView.setText(userId);


        /*school1 = new School();

        if (!UtilString.isBlank(school)) {

            String schoolName = school1.getSchoolName(school);
            if (schoolName != null) {
                school_textView.setText(schoolName);
            } else
                school_textView.setText("");
        } else
            school_textView.setText("");*/

    /*
     * set Profile Pic.
     */
        String userString = userId;
        userString = userId.replaceAll("@", "");
        filePath = Utility.getWorkingAppDir() + "/thumbnail/" + userString + "_PC.jpg";

        try {
            final File thumbnailFile = new File(filePath);
            if (thumbnailFile.exists()) {
                // image file present locally
                Bitmap myBitmap = BitmapFactory.decodeFile(thumbnailFile.getAbsolutePath());
                profileimgview.setImageBitmap(myBitmap);
            } else if (ParseUser.getCurrentUser() != null && ParseUser.getCurrentUser().has("pid")) {

                ParseFile imagefile = (ParseFile) ParseUser.getCurrentUser().get("pid");
                imagefile.getDataInBackground(new GetDataCallback() {
                    public void done(byte[] data, ParseException e) {
                        if (e == null) {
                            // ////Image download successful
                            FileOutputStream fos;
                            try {
                                fos = new FileOutputStream(filePath);
                                try {
                                    fos.write(data);
                                } catch (IOException e1) {
                                    e1.printStackTrace();
                                } finally {
                                    try {
                                        fos.close();
                                    } catch (IOException e1) {
                                        e1.printStackTrace();
                                    }
                                }

                            } catch (FileNotFoundException e2) {
                                e2.printStackTrace();
                            }

                            Bitmap mynewBitmap = BitmapFactory.decodeFile(thumbnailFile.getAbsolutePath());
                            profileimgview.setImageBitmap(mynewBitmap);
                            // Might be a problem when net is too slow :/
                            // Utility.toast("Profile Image Downloaded");
                        } else {
                            // Image not downloaded
                            // Utility.toast("Profile Image not Downloaded");
                        }
                    }
                });
            } else {
        /*
         * Setting profile pic according to their sex
         */

                if (role.equals("parent")) {

                    if (user.getString("sex") != null && (!UtilString.isBlank(user.getString("sex")))) {
                        String sex = user.getString("sex");
                        if (sex.equals("M"))
                            profileimgview.setImageResource(R.drawable.maleparentdp);
                        else if (sex.equals("F"))
                            profileimgview.setImageResource(R.drawable.femaleparentdp);
                    } else {
                        // if sex is not stored
                        String username = user.getString("name");
                        if (!UtilString.isBlank(username)) {
                            String[] names = username.split("\\s");

                            if (names != null && names.length > 1) {
                                String title = names[0].trim();

                                if (title.equals("Mr") || title.equals("Mr.")) {
                                    profileimgview.setImageResource(R.drawable.maleparentdp);
                                    user.put("sex", "M");
                                    user.saveEventually();
                                } else if (title.equals("Mrs")|| title.equals("Mrs.")) {
                                    profileimgview.setImageResource(R.drawable.femaleparentdp);
                                    user.put("sex", "F");
                                    user.saveEventually();
                                } else if (title.equals("Ms")  || title.equals("Ms.")) {
                                    profileimgview.setImageResource(R.drawable.femaleparentdp);
                                    user.put("sex", "F");
                                    user.saveEventually();
                                }
                            }
                        }
                    }

                } else {

                    // in case of teacher role
                    if (user.getString("sex") != null && (!UtilString.isBlank(user.getString("sex")))) {
                        String sex = user.getString("sex");
                        if (sex.equals("M"))
                            profileimgview.setImageResource(R.drawable.maleteacherdp);
                        else if (sex.equals("F"))
                            profileimgview.setImageResource(R.drawable.femaleteacherdp);
                    } else {
                        // if sex is not stored
                        String username = user.getString("name");
                        if (!UtilString.isBlank(username)) {
                            String[] names = username.split("\\s");

                            if (names != null && names.length > 1) {
                                String title = names[0].trim();

                                if (title.equals("Mr")|| title.equals("Mr.")) {
                                    profileimgview.setImageResource(R.drawable.maleteacherdp);
                                    user.put("sex", "M");
                                    user.saveEventually();
                                } else if (title.equals("Mrs") ||  title.equals("Mrs.")) {
                                    profileimgview.setImageResource(R.drawable.femaleteacherdp);
                                    user.put("sex", "F");
                                    user.saveEventually();
                                } else if (title.equals("Ms") || title.equals("Ms.")) {
                                    profileimgview.setImageResource(R.drawable.femaleteacherdp);
                                    user.put("sex", "F");
                                    user.saveEventually();
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
        }

    /*
     * Setting clicklistner on each buttton
     */
        profileimgview.setOnClickListener(this);
        editName.setOnClickListener(this);
        //editPhone.setOnClickListener(this);
        editSchool.setOnClickListener(this);
        //changePassword.setOnClickListener(this);
        rateOurApp.setOnClickListener(this);
        faq.setOnClickListener(this);
        feedback.setOnClickListener(this);
        signOut.setOnClickListener(this);

        //following handles update app action from profile page
        if (getIntent().hasExtra("action")) {
            if (getIntent().getStringExtra("action").equals(Constants.PROFILE_PAGE_ACTION)) {
                //go to market
                Uri uri = Uri.parse("market://details?id=" + getPackageName());
                Intent myAppLinkToMarket = new Intent(Intent.ACTION_VIEW, uri);
                if (Utility.isInternetOn(this)) {
                    try {
                        startActivity(myAppLinkToMarket);
                    } catch (ActivityNotFoundException e) {
                    }
                } else {
                    Utility.toast("Check your Internet Connection.");

                    Intent intent = new Intent(this, MainActivity.class);
                    startActivity(intent);
                }
            }
        }
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == 301) {
            switch (resultCode) {
                case Activity.RESULT_OK:
                    Uri selectedImg = intent.getData();
                    doCrop(selectedImg);
                    break;
                case Activity.RESULT_CANCELED:
                    break;
            }
        } else if (requestCode == 302) {
            File thumbnailFile = new File(filePath);
            Bitmap myBitmap = BitmapFactory.decodeFile(thumbnailFile.getAbsolutePath());
            profileimgview.setImageBitmap(myBitmap);
            // //update the profile picture on server
            try {
                updateProfilePic(filePath, userId);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public void doCrop(Uri uriOfImageToCrop) {
        final Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setData(uriOfImageToCrop);
        intent.putExtra("outputX", 200);
        intent.putExtra("outputY", 200);
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        intent.putExtra("scale", true);
        intent.putExtra("noFaceDetection", true);

        File file = new File(filePath);
        if (file.exists())
            file.delete();
        intent.putExtra("output", Uri.fromFile(file));
        startActivityForResult(intent, 302);
    }


    @Override
    public void onClick(View v) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        switch (v.getId()) {
            case R.id.profileimg:
                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                photoPickerIntent.setType("image/*");
                startActivityForResult(photoPickerIntent, 301);
                break;

            case R.id.editName:
        /*
         * Updating user name ----------------------------------
         */
                AlertDialog.Builder nameDialog = new AlertDialog.Builder(this);

                nameDialog.setTitle("Update your Name");

                LinearLayout nameLayout = new LinearLayout(this);
                nameLayout.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams nameParams =
                        new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT);
                nameParams.setMargins(30, 30, 30, 30);

                final EditText nameInput = new EditText(this);
                nameInput.setText(name);
                nameLayout.addView(nameInput, nameParams);
                nameDialog.setView(nameLayout);
                nameDialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        final String value = nameInput.getText().toString();

                        if (!UtilString.isBlank(value)) {
                            if (Utility.isInternetOn(ProfilePage.this)) {
                                InputMethodManager imm =
                                        (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                                imm.hideSoftInputFromWindow(nameInput.getWindowToken(), 0);

                                ParseUser user = ParseUser.getCurrentUser();
                                if (user != null) {
                                    user.put("name", value);
                                    user.saveInBackground(new SaveCallback() {
                                        @Override
                                        public void done(ParseException e) {
                                            if (e == null) {
                                                name_textView.setText(value);
                                                Utility.toastDone("Name updated !");
                                            } else {
                                                e.printStackTrace();
                                                Utility.toast("Name update failed !");
                                            }
                                        }
                                    });

                  /*
                   * updatin creator in codegroup
                   */

                                    /*
                                    ParseQuery<ParseObject> query = ParseQuery.getQuery("Codegroup");
                                    query.whereEqualTo("senderId", userId);
                                    query.findInBackground(new FindCallback<ParseObject>() {
                                        public void done(List<ParseObject> itemList, ParseException e) {
                                            if (e == null) {
                                                if (itemList != null) {
                                                    for (int i = 0; i < itemList.size(); i++) {
                                                        itemList.get(i).put("Creator", value);
                                                        itemList.get(i).saveEventually();
                                                    }
                                                }
                                            }
                                        }
                                    });
                                    */

                  /*
                   * updating name in Group member class. Not needed as now using associated name instead of parent's name
                   */

                                    /*ParseQuery<ParseObject> memberQuery = ParseQuery.getQuery("GroupMembers");
                                    memberQuery.whereEqualTo("emailId", userId);
                                    memberQuery.findInBackground(new FindCallback<ParseObject>() {
                                        public void done(List<ParseObject> itemList, ParseException e) {
                                            if (e == null) {
                                                if (itemList != null) {
                                                    for (int i = 0; i < itemList.size(); i++) {
                                                        itemList.get(i).put("name", value);
                                                        itemList.get(i).saveEventually();
                                                    }
                                                }
                                            }
                                        }
                                    });*/

                  /*
                   * updating Creator in GroupDetails class
                   */

                                    // ParseQuery<ParseObject> msgQuery = ParseQuery.getQuery("GroupDetails");
                                    // msgQuery.whereEqualTo("senderId", userId);
                                    // msgQuery.findInBackground(new FindCallback<ParseObject>() {
                                    // public void done(List<ParseObject> itemList, ParseException e) {
                                    // if (e == null) {
                                    // if (itemList != null) {
                                    // for (int i = 0; i < itemList.size(); i++) {
                                    // itemList.get(i).put("Creator", value);
                                    // itemList.get(i).saveEventually();
                                    // }
                                    // }
                                    // }
                                    // }
                                    // });


                                }
                            }
                        }
                    }
                });

                nameDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.dismiss();
                        Log.d("action", getSupportActionBar().getHeight()+" ");

                    }
                });

                nameDialog.show();

                break;

            case R.id.editPhone:
        /*
         * Updating Phone Details ----------------------------------
         */
                AlertDialog.Builder alert = new AlertDialog.Builder(this);

                alert.setTitle("Update Phone Number");

                LinearLayout layout = new LinearLayout(this);
                layout.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams params =
                        new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT);
                params.setMargins(30, 30, 30, 30);

                final EditText input = new EditText(this);
                input.setText(phone);
                layout.addView(input, params);
                alert.setView(layout);
                input.setRawInputType(Configuration.KEYBOARD_QWERTY);
                alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String value = input.getText().toString();

                        if (!UtilString.isBlank(value)) {
                            if (Utility.isInternetOn(ProfilePage.this)) {
                                InputMethodManager imm =
                                        (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                                imm.hideSoftInputFromWindow(input.getWindowToken(), 0);

                                ParseUser user = ParseUser.getCurrentUser();
                                if (user != null) {
                                    user.put("phone", value);
                                    user.saveEventually();
                                    phone_textView.setText(value);
                                }
                            }
                        }
                    }
                });

                alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.dismiss();
                    }
                });

                alert.show();

                break;
            case R.id.editSchool:
        /*
         * Updating School Details ----------------------------------
         */
                AlertDialog.Builder schoolDialog = new AlertDialog.Builder(this);

                schoolDialog.setTitle("Update School Name");

                LinearLayout schoolLayout = new LinearLayout(this);
                schoolLayout.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams schoolParmas =
                        new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT);
                schoolParmas.setMargins(30, 30, 30, 30);

          /*
          Setting Autocomplete textview in popup and its adapter to it
           */
                //location input
                final DelayAutoCompleteTextView locationInput = new DelayAutoCompleteTextView(this, null);

                locationInput.setThreshold(1);
                locationInput.setHint("Your Location");
                locationInput.setAdapter(new SchoolAutoComplete.PlacesAutoCompleteAdapter(this, R.layout.school_autocomplete_list_item, R.id.school_location));

                /*String schoolName = school1.getSchoolName(school);
                if (schoolName != null) {
                    locationInput.setText(schoolName);
                } else
                    school_textView.setText("");*/


                //progress bar. Hidden by default
                final ProgressBar progressBar = new ProgressBar(this);
                progressBar.setVisibility(View.GONE);


                //school input. Hidden by default
                final AutoCompleteTextView schoolInput = new AutoCompleteTextView(this);
                schoolInput.setVisibility(View.GONE);
                schoolInput.setThreshold(1); //show suggestions on typing atleast one letter


                    /*ReadSchoolFile readSchoolFile = new ReadSchoolFile();
                    try {
                        schoolsAdapter =
                                new ArrayAdapter(this, android.R.layout.simple_list_item_1, readSchoolFile.getSchoolsList().toArray());
                                schoolInput.setAdapter(schoolsAdapter);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }*/


                locationInput.setSelectAllOnFocus(true);
                locationInput.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        final String value = (String) parent.getItemAtPosition(position);
                        Log.d("DEBUG_PROFILE_PAGE", "item clicked " + value);
                        schoolInput.setVisibility(View.GONE);
                        progressBar.setVisibility(View.VISIBLE); //show progress bar

                        new AsyncTask<Void, Void, Void>() {
                            ArrayList<String> schools;

                            @Override
                            protected Void doInBackground(Void... voids) {
                                schools = SchoolAutoComplete.schoolsNearby(value);
                                return null;
                            }

                            @Override
                            protected void onPostExecute(Void result) {
                                schoolsAdapter =
                                        new ArrayAdapter(actcontext, android.R.layout.simple_list_item_1, schools);
                                schoolInput.setAdapter(schoolsAdapter);
                                schoolInput.setText("");
                                progressBar.setVisibility(View.GONE); //hide progress bar
                                schoolInput.setVisibility(View.VISIBLE); //finally show school list box
                                return;
                            }
                        }.execute();

                    }
                });
                schoolInput.setHint("School name");
//                schoolInput.setDropDownHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
                schoolInput.setDropDownHeight(200);

                schoolLayout.addView(schoolInput, schoolParmas);
                schoolLayout.addView(progressBar, schoolParmas);
                schoolLayout.addView(locationInput, schoolParmas);
                ;

                schoolDialog.setView(schoolLayout);


                //button responses
                schoolDialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String value = schoolInput.getText().toString();
                        if (value.isEmpty()) {
                            showToast("please fill school name");
                            return;
                        }
                        if (!UtilString.isBlank(value)) {
                            value = value.trim();
                            if (Utility.isInternetOn(ProfilePage.this)) {
                                InputMethodManager imm =
                                        (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                                imm.hideSoftInputFromWindow(schoolInput.getWindowToken(), 0);

                                String schoolId = School.schoolIdExist(value);
                                if (schoolId == null) {

                                    profileLayout.setVisibility(View.GONE);
                                    progressBarLayout.setVisibility(View.VISIBLE);
                                    School.UpdateSchoolOnServer updateSchool = new School.UpdateSchoolOnServer(value);
                                    updateSchool.execute(new String[]{value});
                                } else {
                                    school_textView.setText(value);
                                    ParseUser user = ParseUser.getCurrentUser();
                                    user.put(Constants.SCHOOL, schoolId);  //updating school field of user
                                    user.saveEventually();
                                    school = schoolId;
                                }
                            }
                        }

                        dialog.dismiss();
                    }
                });

                schoolDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.dismiss();
                    }
                });

                AlertDialog dialog = schoolDialog.create();
                dialog.show();
                Button okButton = (Button) dialog.getButton(DialogInterface.BUTTON_POSITIVE);

                break;

      /*
       * Setting feedback
       */
            case R.id.feedback:
                FeedBackClass feedBack = new FeedBackClass();
                FragmentManager fm = getSupportFragmentManager();
                feedBack.show(fm, "FeedBackClass");

                break;

      /*
       * sign out
       */
            case R.id.signOut:
                if (Utility.isInternetOn(this)) {

                    {
                        Utility.logout();
                        return;
                    }
                } else {
                    Utility.toast("Check your Internet Connection.");
                }
                break;

      /*
       * Rate our app
       */
            case R.id.rateOurApp:
                Uri uri = Uri.parse("market://details?id=" + getPackageName());
                Intent myAppLinkToMarket = new Intent(Intent.ACTION_VIEW, uri);

                if (Utility.isInternetOn(this)) {

                    try {
                        startActivity(myAppLinkToMarket);
                    } catch (ActivityNotFoundException e) {
                    }
                } else {
                    Utility.toast("Check your Internet Connection.");
                }
                break;

      /*
       * FAQs
       */
            case R.id.faq:

                Intent intent = new Intent(this, FAQs.class);
                startActivity(intent);

                break;

      /*
       * Change Password
       */
           /* case R.id.changePassword:

                ParseUser.requestPasswordResetInBackground(userId, new RequestPasswordResetCallback() {
                    public void done(ParseException e) {
                        if (e == null) {
                            Utility
                                    .toastLong("Password resetting link with reset instructions was sent to your Email id.");
                            // An email was successfully sent with reset instructions.
                        } else {
                            Utility.toast("Check your Internet connection");
                            // Something went wrong. Look at the ParseException to see what's up.
                        }
                    }
                });

                break;
*/
            default:
                break;
        }
    }

    // **********************updating profile****************************************

    public void updateProfile(final String parentName, final String userId) {
        ParseUser user = ParseUser.getCurrentUser();


        if (user != null) {
            user.put("name", parentName);
            user.saveInBackground(new SaveCallback() {

                @Override
                public void done(com.parse.ParseException e) {
                    // TODO Auto-generated method stub
                    if (e == null) {
                        Utility.toastDone("Profile Updated");

                        // updating locally

                        ParseQuery<ParseObject> query1 = ParseQuery.getQuery("UserDetails");
                        query1.fromLocalDatastore();
                        query1.whereEqualTo("userId", userId);
                        try {
                            ParseObject object = query1.getFirst();
                            object.put("name", parentName);
                            object.pin();
                        } catch (ParseException e1) {
                        }
                    } else {
                        Utility.toast("Profile Update Failed");
                    }
                }
            });
        }
    }

    public void updateProfilePic(final String filepath, final String userId) throws IOException {
        int slashindex = (filepath).lastIndexOf("/");
        String fileName = (filepath).substring(slashindex + 1);// image file //
        // name

        if (ParseUser.getCurrentUser() != null) {
            RandomAccessFile f = new RandomAccessFile(filepath, "r");
            byte[] data = new byte[(int) f.length()];
            f.read(data);
            final ParseFile file = new ParseFile(fileName, data);
            file.saveInBackground(new SaveCallback() {

                @Override
                public void done(ParseException e) {
                    if (e == null) {
                        Utility.toastDone("Profile Pic Updated!!");


                        ParseUser.getCurrentUser().put("pid", file);

                        picName = ParseUser.getCurrentUser().getString("picName");
                        if (picName == null) {
                            // picName = userId.replaceAll("\\.", "");
                            picName = userId.replaceAll("@", "") + "___0";
                        } else {
                            String[] parts = picName.split("___");

                            int count = Integer.parseInt(parts[parts.length - 1]);

                            picName = "";

                            for (int i = 0; i < parts.length - 1; i++) {
                                picName += parts[i] + "___";
                            }
                            picName += Integer.toString(++count);
                        }
                        ParseUser.getCurrentUser().put("picName", picName);
                        ParseUser.getCurrentUser().saveInBackground();


            /*
             * Saving image details in code group
             */

                        ParseQuery<ParseObject> codeQuery = ParseQuery.getQuery("Codegroup");
                        codeQuery.whereEqualTo("senderId", userId);

                        codeQuery.findInBackground(new FindCallback<ParseObject>() {

                            @Override
                            public void done(List<ParseObject> arg0, ParseException arg1) {
                                if (arg1 == null) {
                                    if (arg0 != null) {
                                        for (int i = 0; i < arg0.size(); i++) {
                                            ParseObject obj = arg0.get(i);
                                            if (picName != null)
                                                obj.put("picName", picName);
                                            if (ParseUser.getCurrentUser().getParseFile("pid") != null)
                                                obj.put("senderPic", ParseUser.getCurrentUser().getParseFile("pid"));
                                            obj.saveInBackground();
                                        }
                                    }
                                }
                            }

                        });


                        // updating locally
                        ParseQuery<ParseObject> query1 = ParseQuery.getQuery("UserDetails");
                        query1.fromLocalDatastore();
                        query1.whereEqualTo("userId", userId);
                        try {
                            ParseObject object = query1.getFirst();
                            object.put("pid", file);
                        } catch (ParseException e1) {
                        }
                    } else {
                        Utility.toast("Profile Pic Not Updated!!");

                        File file = new File(filepath);

                        boolean check = file.delete();

                    }
                }
            });

        } else {
            Utility.toast("Profile Pic Not Updated!!");
        }
    }

    public void showToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    public void onBackPressed() {

        Intent intent = new Intent(ProfilePage.this, MainActivity.class);
        startActivity(intent);
    }



}
