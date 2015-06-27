package trumplabs.schoolapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import BackGroundProcesses.SendPendingMessages;
import library.UtilString;
import trumplab.textslate.R;
import utility.Config;
import utility.SessionManager;
import utility.Utility;

/**
 * Created by dhanesh on 26/6/15.
 */
public class ComposeMessageHelper {
    private EditText typedmsg;
    private Context context;
    private String typedtxt;

    private String groupCode;
    private String grpName;
    private String sender;
    private String userId;        //really needed to store ???????????????
    private ParseUser user;
    private SessionManager session;

    ComposeMessageHelper(Activity context, String grpName, String groupCode)
    {
        this.context = context;
        this.grpName = grpName;
        this.groupCode = groupCode;
        typedmsg = (EditText) context.findViewById(R.id.typedmsg);

        user = ParseUser.getCurrentUser();
        userId = user.getUsername();
        sender = user.getString(Constants.NAME);
        session = new SessionManager(Application.getAppContext());
    }

    public void  send()
    {
        int hourOfDay = -1;
        if (session != null) {
            //using local time instead of session.getCurrentTime
            //Date now = session.getCurrentTime();
            Calendar cal = Calendar.getInstance();
            //cal.setTime(now);
            hourOfDay = cal.get(Calendar.HOUR_OF_DAY);
        }


        if (hourOfDay != -1) {

            //If current message time is not sutaible <9PM- 6AM> then show this warning as popup to users
            if (hourOfDay >= Config.messageNormalEndTime || hourOfDay < Config.messageNormalStartTime) {
                //note >= and < respectively because disallowed are [ >= EndTime and < StartTime]
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                LinearLayout warningView = new LinearLayout(context);
                warningView.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams nameParams =
                        new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT);
                nameParams.setMargins(30, 30, 30, 30);

                final TextView nameInput = new TextView(context);
                nameInput.setTextSize(16);
                nameInput.setText(Config.messageTimeWarning);
                nameInput.setGravity(Gravity.CENTER_HORIZONTAL);
                warningView.addView(nameInput, nameParams);
                builder.setView(warningView);


                builder.setPositiveButton("SEND", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        sendFunction();
                    }
                });
                builder.setNegativeButton("CANCEL", null);
                AlertDialog dialog = builder.create();
                dialog.setCanceledOnTouchOutside(true);
                dialog.show();

            } else {
                sendFunction();
            }
        } else {
            sendFunction();
        }
    }

    /*
    Send messages to subscribers
     */
    public void sendFunction() {

        typedtxt = typedmsg.getText().toString().trim();  //message to send

        //check internet connection - NOT REQUIRED - as offline messaging support

                /*if(!Utility.isInternetExist(SendMessage.this)) {
                    return;
                }*/
        if (!UtilString.isBlank(typedtxt) && ComposeMessage.sendimgpreview.getVisibility() == View.GONE) {

            // when its not an image message******************
            sendTxtMsgtoSubscribers(typedtxt);

        } else if (ComposeMessage.sendimgpreview.getVisibility() == View.VISIBLE) {
            // Sending an image file
            // passing image file path and message content as
            // parameters
            sendPic((String) ComposeMessage.sendimgpreview.getTag(), typedtxt);

            // for image we try to keep track of progress
            typedmsg.setText("");
            ComposeMessage.sendimgpreview.setTag("");
            ComposeMessage.sendimgview.setImageBitmap(null);
            ComposeMessage.sendimgpreview.setVisibility(View.GONE);
        }
    }



    private void sendTxtMsgtoSubscribers(final String typedtxt) {

        final ParseObject sentMsg = new ParseObject(Constants.SENT_MESSAGES_TABLE);
        sentMsg.put("Creator", sender);
        sentMsg.put("code", groupCode);
        sentMsg.put("title", typedtxt);
        sentMsg.put("name", grpName);
        sentMsg.put("creationTime", session.getCurrentTime()); //needs to be updated once sent
        sentMsg.put("senderId", userId);
        sentMsg.put("userId", userId);

        typedmsg.setText(""); //for reuse

        sentMsg.pinInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    SendPendingMessages.addMessageToQueue(sentMsg);
                } else {
                    e.printStackTrace();
                    Utility.toastDone("Sorry! Can't send your message");
                }
            }
        });

        //TODO Notify and update "Outbox" page messages and count also
        Outbox.needLoading = true; //handle in MainActivity when tab is changed

        //updProgressBar.setVisibility(View.VISIBLE); not needed now as immediately showing the offline message
    }

    /*always called in thread for background sending
     return values
        0 : success,
        100 : network error (so abort queue),
        -1 : failure due to other error(won't happen usually, hence safe to ignore and continue with other pending messsages)
    */
    public static int sendTextMessageCloud(final ParseObject msg, final boolean isLive){
        //if live, then only show "sent" toast
        //sending message using parse cloud function
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("classcode", msg.getString("code"));
        params.put("classname", msg.getString("name"));
        params.put("message", msg.getString("title"));

        try{
            HashMap obj = ParseCloud.callFunction("sendTextMessage", params);
            if (obj != null) {
                Date createdAt = (Date) obj.get("createdAt");
                String objectId = (String) obj.get("messageId");

                //updating local time
                SessionManager sm = new SessionManager(Application.getAppContext());
                if (createdAt != null) {
                    sm.setCurrentTime(createdAt);
                }

                //update msg and pin
                msg.put("objectId", objectId);
                msg.put("pending", false);
                msg.put("creationTime", createdAt);

                try {
                    msg.pin();
                } catch (ParseException err) {
                    err.printStackTrace();
                }

                //just notify outbox - no new query or updating count (since an old message just got new status)
                Outbox.refreshSelf();
                return 0; //success
            }
            return -1; //unexpected error
        }
        catch (ParseException e){
            e.printStackTrace();
            if(e.getCode() == ParseException.CONNECTION_FAILED){
                return 100; //network error
            }
            return -1;
        }
    }



    // Send Image Pic
    private void sendPic(String filepath, String txtmsg){

        if (filepath == null) return;

        final ParseObject sentMsg = new ParseObject(Constants.SENT_MESSAGES_TABLE);
        sentMsg.put("Creator", sender);
        sentMsg.put("code", groupCode);
        sentMsg.put("title", txtmsg);
        sentMsg.put("name", grpName);
        SessionManager session = new SessionManager(Application.getAppContext());
        sentMsg.put("creationTime", session.getCurrentTime()); //needs to be updated once sent
        sentMsg.put("senderId", userId);
        sentMsg.put("userId", userId);
        sentMsg.put("pending", true);

        // /Creating ParseFile (Not yet uploaded)
        int slashindex = filepath.lastIndexOf("/");
        final String fileName = filepath.substring(slashindex + 1);// image file //

        if (fileName != null)
            sentMsg.put("attachment_name", fileName);

        typedmsg.setText(""); //for reuse

        sentMsg.pinInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    SendPendingMessages.addMessageToQueue(sentMsg);
                } else {
                    e.printStackTrace();
                    Utility.toast("Unable to send message!");
                }
            }
        });

        //TODO Notify and update "Outbox" page messages and count also
        Outbox.needLoading = true; //handle in MainActivity when tab is changed
    }

    //refer to sendTextMessageCloud
    public static int sendPicMessageCloud(final ParseObject msg, final boolean isLive) {
        //don't have attachement, objectid
        //update creationTime, pending
        String imageName = null;
        if (msg.containsKey("attachment_name"))
            imageName = msg.getString("attachment_name");

        if (imageName == null) return -1; //no attachment

        byte[] data = null;
        try {
            RandomAccessFile f = new RandomAccessFile(Utility.getWorkingAppDir() + "/media/" + imageName, "r");
            data = new byte[(int) f.length()];
            f.read(data);
        } catch (IOException e) {
            e.printStackTrace();
            return -1; //io exception
        }

        final ParseFile file = new ParseFile(imageName, data);

        Log.d(SendPendingMessages.LOGTAG, "sendPicMessageCloud : data size " + data.length + " bytes");

        try {
            file.save();
            Log.d(SendPendingMessages.LOGTAG, "sendPicMessageCloud : file save success");
            //sending message using parse cloud function
            HashMap<String, Object> params = new HashMap<String, Object>();
            params.put("classcode", msg.getString("code"));
            params.put("classname", msg.getString("name"));
            params.put("message", msg.getString("title")); //won't be null
            params.put("filename", msg.getString("attachment_name"));
            params.put("parsefile", file);

            HashMap obj = ParseCloud.callFunction("sendPhotoTextMessage", params);

            Log.d(SendPendingMessages.LOGTAG, "sendPicMessageCloud : calling cloud function success");
            if (obj != null) {
                Date createdAt = (Date) obj.get("createdAt");
                String objectId = (String) obj.get("messageId");

                //update msg and pin
                msg.put("objectId", objectId);
                msg.put("pending", false);
                msg.put("creationTime", createdAt);
                msg.put("attachment", file);

                //saving locally
                try {
                    msg.pin();
                } catch (ParseException e1) {
                    e1.printStackTrace();
                }

                //showing popup if live
                if (isLive) {
                    Utility.toastDone("Notification Sent");
                }

                //just notify outbox - no new query or updating count (since an old message just got new status)
                Outbox.refreshSelf();
                return 0;
            }
            return -1;
        }
        catch(ParseException esave){
            esave.printStackTrace();
            if(esave.getCode() == ParseException.CONNECTION_FAILED){
                return 100;
            }
            return -1;
            //Utility.toast("Sorry, sending failed now. We'll send it next time you're online");
        }
    }

}
