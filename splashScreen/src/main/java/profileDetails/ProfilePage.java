package profileDetails;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
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

import com.parse.FunctionCallback;
import com.parse.GetDataCallback;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseUser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;

import baseclasses.MyActionBarActivity;
import library.UtilString;
import trumplab.textslate.R;
import trumplabs.schoolapp.Constants;
import trumplabs.schoolapp.FeedBackClass;
import trumplabs.schoolapp.MainActivity;
import utility.Utility;

public class ProfilePage extends MyActionBarActivity implements OnClickListener {
    private ImageView profileimgview;
    private TextView name_textView;
    private TextView phone_textView;
    private String userId;
    private String filePath;
    private String name;
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

        if (!UtilString.isBlank(name))
            name_textView.setText(name);

        phone_textView.setText(userId);

    /*
     * set Profile Pic.
     */
        setProfilePic();

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

                if(Utility.isInternetExistWithoutPopup()) {
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

      //  FacebookSdk.sdkInitialize(getApplicationContext());
    }

    void setProfilePic(){
        String userString = userId.replaceAll("@", "");
        filePath = Utility.getWorkingAppDir() + "/thumbnail/" + userString + "_PC.jpg";

        try {
            final File thumbnailFile = new File(filePath);
            if (thumbnailFile.exists()) {
                Log.d("__K", "displaying available locally : " + filePath);
                // image file present locally
                Bitmap myBitmap = BitmapFactory.decodeFile(thumbnailFile.getAbsolutePath());
                profileimgview.setImageBitmap(myBitmap);
            } else if (ParseUser.getCurrentUser() != null && ParseUser.getCurrentUser().has("pid")) {
                Log.d("__K", "displaying fetching :" + filePath);
                ParseFile imagefile = (ParseFile) ParseUser.getCurrentUser().get("pid");
                imagefile.getDataInBackground(new GetDataCallback() {
                    public void done(byte[] data, ParseException e) {
                        Log.d("__K", "displaying fetch over :" + filePath);
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
         * Setting profile pic (don't consider sex, just the role)
         */
                profileimgview.setImageResource(R.drawable.dp_common);
            }
        } catch (Exception e) {
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        //facebook ad tracking
        // Logs 'install' and 'app activate' App Events.
       // AppEventsLogger.activateApp(this, Config.FB_APP_ID);
    }

    @Override
    protected void onPause() {
        super.onPause();

        //facebook tracking : time spent on app by people
        // Logs 'app deactivate' App Event.
       // AppEventsLogger.deactivateApp(this, Config.FB_APP_ID);
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

                    //update the profile picture on server
                    UpdateProfilePicTask task = new UpdateProfilePicTask(filePath);
                    task.execute();

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

                            if (Utility.isInternetExist()) {
                                InputMethodManager imm =
                                        (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                                imm.hideSoftInputFromWindow(nameInput.getWindowToken(), 0);

                                final ParseUser user = ParseUser.getCurrentUser();
                                if(user == null){
                                    return;
                                }

                                HashMap<String, Object> parameters = new HashMap<String, Object>();
                                parameters.put("name", value);

                                ParseCloud.callFunctionInBackground("updateProfileName", parameters,
                                    new FunctionCallback<Boolean>() {
                                        @Override
                                        public void done(Boolean result, ParseException e) {
                                            if (e == null && result) {
                                                Log.d("__N", "all success : now just pinning");
                                                name = value;
                                                name_textView.setText(name);
                                                Utility.toast("Name updated !");
                                                user.put("name", value);
                                                try {
                                                    user.pin();
                                                } catch (ParseException e2) {
                                                    e2.printStackTrace();
                                                }
                                            } else {
                                                Log.d("__N", "cloud call failed");
                                                Utility.toast("Name update failed !");
                                            }
                                        }
                                    }
                                );
                            }
                        }
                        else{
                            Utility.toast("Name can't be empty !");
                        }
                    }
                });

                nameDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.dismiss();
                        Log.d("action", getSupportActionBar().getHeight() + " ");

                    }
                });
                Dialog dialog = nameDialog.create();
                dialog.setCanceledOnTouchOutside(true);
                dialog.show();

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
                if(Utility.isInternetExist()) {

                    {
                        Utility.logoutProfilePage();
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

                if(Utility.isInternetExist()) {

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
    
    public void onBackPressed() {
        Intent intent = new Intent(ProfilePage.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    class UpdateProfilePicTask extends AsyncTask<Void, Void, Void>
    {
        String filepath;
        Boolean success = false;

        public UpdateProfilePicTask(final String fp) {
            filepath = fp;
        }

        @Override
        protected Void doInBackground(Void... params) {
            Log.d("__K", "pic path=" + filepath);

            if(!Utility.isInternetExistWithoutPopup()){
                return null; //return immediately if internet not connected
            }

            int slashindex = (filepath).lastIndexOf("/");
            String fileName = (filepath).substring(slashindex + 1);// image file //
            // name

            if (ParseUser.getCurrentUser() != null) {
                byte[] data = null;
                try {
                    RandomAccessFile f = new RandomAccessFile(filepath, "r");
                    data = new byte[(int) f.length()];
                    f.read(data);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (data == null) {
                    Log.d("__K", "file read error");
                    return null;
                }

                final ParseFile file = new ParseFile(fileName, data);
                try {
                    file.save();
                    HashMap<String, Object> parameters = new HashMap<String, Object>();
                    parameters.put("pid", file);
                    boolean result = ParseCloud.callFunction("updateProfilePic", parameters);

                    if (result) {
                        Log.d("__K", "all success : now just pinning");
                        //TODO call cloud function updateProfilePic
                        ParseUser.getCurrentUser().put("pid", file);
                        ParseUser.getCurrentUser().pin();
                        success = true;
                    } else {
                        Log.d("__K", "cloud code returned false :(");
                    }
                } catch (ParseException e) {
                    Log.d("__K", "parse file save, cloud code call OR pinning failure");
                    e.printStackTrace();
                }
            }
            else{
                Log.d("__K", "parse user null");
            }

            return null;
        }


        void deletePicFileOnFailure(){
            File file=new File(filepath);

            boolean check=file.delete();//file <username>_PC.jpg will be deleted. But the pid in User object is not updated and the corresponding
                                        //parsefile's data is already present. So next time when pic file is not present in sdcard,
                                        //we won't need to fetch the data for parsefile. So consistent even if net not present ;)
        }


        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if(success) {
                Utility.toast("Profile Pic Updated!!");
            }
            else{
                Utility.toast("Profile Pic Not Updated!!");
                deletePicFileOnFailure();
                setProfilePic();
            }
        }
    }
}
