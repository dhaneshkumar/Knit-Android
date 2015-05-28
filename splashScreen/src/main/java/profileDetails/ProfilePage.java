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
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.parse.FindCallback;
import com.parse.GetDataCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;

import baseclasses.MyActionBarActivity;
import library.UtilString;
import trumplab.textslate.R;
import trumplabs.schoolapp.Constants;
import trumplabs.schoolapp.FeedBackClass;
import trumplabs.schoolapp.MainActivity;
import utility.Config;
import utility.Utility;

public class ProfilePage extends MyActionBarActivity implements OnClickListener {
    private ImageView profileimgview;
    private TextView name_textView;
    private TextView phone_textView;
    private String userId;
    private String picName;
    private String filePath;
    private String name;
    private String phone;
    public static LinearLayout progressBarLayout;
    public static LinearLayout profileLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile);

        name_textView = (TextView) findViewById(R.id.name);
        phone_textView = (TextView) findViewById(R.id.phone);
        profileimgview = (ImageView) findViewById(R.id.profileimg);

        TextView profile = (TextView) findViewById(R.id.profile);
        TextView account = (TextView) findViewById(R.id.account);
        TextView about = (TextView) findViewById(R.id.about);
        LinearLayout editName = (LinearLayout) findViewById(R.id.editName);
        LinearLayout editPhone = (LinearLayout) findViewById(R.id.editPhone);
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

                if(Utility.isInternetExist(this)) {
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
        FacebookSdk.sdkInitialize(getApplicationContext());
    }


    @Override
    protected void onResume() {
        super.onResume();

        //facebook ad tracking
        // Logs 'install' and 'app activate' App Events.
        AppEventsLogger.activateApp(this, Config.FB_APP_ID);
    }

    @Override
    protected void onPause() {
        super.onPause();

        //facebook tracking : time spent on app by people
        // Logs 'app deactivate' App Event.
        AppEventsLogger.deactivateApp(this, Config.FB_APP_ID);
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
        //RESULT_CANCELED is 0, RESULT_OK is -1
        if (requestCode == 301) {
            Log.d("DEBUG_PROFILE_PAGE", "request code 301 with resultCode=" + resultCode);
            switch (resultCode) {
                case Activity.RESULT_OK:
                    Uri selectedImg = intent.getData();
                    doCrop(selectedImg);
                    break;
                case Activity.RESULT_CANCELED:
                    break;
            }
        } else if (requestCode == 302) {
            Log.d("DEBUG_PROFILE_PAGE", "request code 302 with resultCode=" + resultCode);
            switch (resultCode) {
                case Activity.RESULT_OK:
                    File thumbnailFile = new File(filePath);
                    Bitmap myBitmap = BitmapFactory.decodeFile(thumbnailFile.getAbsolutePath());
                    profileimgview.setImageBitmap(myBitmap);
                    // //update the profile picture on server
                    try {
                        updateProfilePic(filePath, userId);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case Activity.RESULT_CANCELED:
                    break;
            }
        }
    }

    public void doCrop(Uri uriOfImageToCrop) {
        Log.d("DEBUG_PROFILE_PAGE", "into doCrop");
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

               /* FragmentManager fm = getSupportFragmentManager();
                ChooserDialog openchooser = new ChooserDialog();
                Bundle args = new Bundle();
                args.putString("flag", "PROFILE");

                openchooser.setArguments(args);

                openchooser.show(fm, "Add Image");*/
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
                nameInput.setTextColor(Color.BLACK);
                nameLayout.addView(nameInput, nameParams);
                nameDialog.setView(nameLayout);
                nameDialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        final String value = nameInput.getText().toString();

                        if (!UtilString.isBlank(value)) {

                            if(Utility.isInternetExist(ProfilePage.this)) {
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

            case R.id.editPhone: //Not used right now
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

                            if(Utility.isInternetExist(ProfilePage.this)) {
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

      /*
       * Setting feedback
       */
            case R.id.feedback:
                FeedBackClass feedBack = new FeedBackClass();
                FragmentManager fm1 = getSupportFragmentManager();
                feedBack.show(fm1, "FeedBackClass");

                break;

      /*
       * sign out
       */
            case R.id.signOut:
                if(Utility.isInternetExist(ProfilePage.this)) {

                    {
                        Utility.logoutProfilePage(ProfilePage.this);
                        return;
                    }
                }
                break;

      /*
       * Rate our app
       */
            case R.id.rateOurApp:
                Uri uri = Uri.parse("market://details?id=" + getPackageName());
                Intent myAppLinkToMarket = new Intent(Intent.ACTION_VIEW, uri);

                if(Utility.isInternetExist(ProfilePage.this)) {

                    try {
                        startActivity(myAppLinkToMarket);
                    } catch (ActivityNotFoundException e) {
                    }
                }
                break;

      /*
       * FAQs
       */
            case R.id.faq:

                Intent intent = new Intent(this, FAQs.class);
                startActivity(intent);

                break;

            default:
                break;
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

    public void onBackPressed() {

        Intent intent = new Intent(ProfilePage.this, MainActivity.class);
        startActivity(intent);
    }
}
