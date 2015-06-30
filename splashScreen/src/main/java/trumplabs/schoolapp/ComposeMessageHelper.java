package trumplabs.schoolapp;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import BackGroundProcesses.SendPendingMessages;
import library.UtilString;
import trumplab.textslate.R;
import utility.SessionManager;
import utility.Utility;

/**
 * Created by dhanesh on 26/6/15.
 */
public class ComposeMessageHelper {
    private EditText typedmsg;
    private Context context;
    private String typedtxt;

    List<List<String>> selectedClassList;
    private String sender;
    private String userId;        //really needed to store ???????????????
    private ParseUser user;
    private SessionManager session;

    ComposeMessageHelper(Activity context, List<List<String>> classList)
    {
        this.context = context;
        this.selectedClassList = classList;
        typedmsg = (EditText) context.findViewById(R.id.typedmsg);

        user = ParseUser.getCurrentUser();
        userId = user.getUsername();
        sender = user.getString(Constants.NAME);
        session = new SessionManager(Application.getAppContext());
    }

    /*
    Send messages to subscribers
     */
    public void sendFunction() {

        Log.d(ComposeMessage.LOGTAG, "helper : sendFunction()");
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

    private void pinAndNotify(final List<ParseObject> messagesToSend){
        ParseObject.pinAllInBackground(messagesToSend, new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    SendPendingMessages.addMessageListToQueue(messagesToSend);
                } else {
                    e.printStackTrace();
                    Utility.toastDone("Sorry! Can't send your message");
                }
            }
        });

        if(true){//do it always as there is a delay in executing asynctask which queries and updates Outbox messages
            //just update Outbox msgs and notify. No need to worry about SendMessage page as from here
            // we will return to OUTSIDE and when we go to SendMessage page its onCreate will be called
            // By that time, new msgs would have been pinned and will appear when queried

            if(Outbox.groupDetails == null){
                Outbox.groupDetails = new ArrayList<>();
            }
            Log.d(ComposeMessage.LOGTAG, "source outside - current # outbox msgs=" + Outbox.groupDetails.size());
            for(ParseObject msg : messagesToSend){
                Outbox.groupDetails.add(0, msg);
            }

            Outbox.refreshSelf();//not just adapter notify but also make sure that the layout changes(No sent messages -> msg list)

            Outbox.totalOutboxMessages += messagesToSend.size(); //totalOutboxMessages would have a proper value since source is MainActivity

            Log.d(ComposeMessage.LOGTAG, "source outside - added to Outbox.groupdetails #=" + messagesToSend.size() +
                    " total outbox count=" + Outbox.totalOutboxMessages + ", #visible outbox msgs=" + Outbox.groupDetails.size());
        }

        if(ComposeMessage.source.equals(Constants.ComposeSource.INSIDE)){
            //source is INSIDE i.e
            //From here will return to SendMessage of the class
            //add to SendMessage, update total count, notify its adapter
            //No need to worry about Outbox page as it has already been updated and notified in above code fragment

            //Outbox.needLoading = true;

            if(SendMessage.groupDetails == null){
                SendMessage.groupDetails = new ArrayList<>();
            }

            Log.d(ComposeMessage.LOGTAG, "source inside - current # sendmessage msgs=" + SendMessage.groupDetails.size());
            for(ParseObject msg : messagesToSend){
                //TODO add only if its code equal to current SendMessage instance's codegroup
                if(msg.getString("code") != null && SendMessage.groupCode != null && msg.getString("code").equals(SendMessage.groupCode)) {
                    SendMessage.groupDetails.add(0, msg);
                    SendMessage.totalClassMessages++; //increment one by one only for the concerned messages
                }
            }

            SendMessage.notifyAdapter(); //just notify, as the content(new msg) has been added

            Log.d(ComposeMessage.LOGTAG, "source inside - added to SendMessage.groupdetails #=" + messagesToSend.size() +
                    " total SendMessage count=" + SendMessage.totalClassMessages + ", #visible outbox msgs=" + SendMessage.groupDetails.size());
        }

        ComposeMessage.sendButtonClicked = true; //Quick hack to compensate delayed(after pinning of msgs) spawning of pending msg thread
    }


    private void sendTxtMsgtoSubscribers(final String typedtxt) {
        final List<ParseObject> messagesToSend = new ArrayList<>();
        for(List<String> cls : selectedClassList){
            final ParseObject sentMsg = new ParseObject(Constants.SENT_MESSAGES_TABLE);
            sentMsg.put("Creator", sender);
            sentMsg.put("code", cls.get(0)); //code @ 0
            sentMsg.put("title", typedtxt);
            sentMsg.put("name", cls.get(1)); //name @ 1
            sentMsg.put("creationTime", session.getCurrentTime()); //needs to be updated once sent
            sentMsg.put("senderId", userId);
            sentMsg.put("userId", userId);
            sentMsg.put("pending", true);

            typedmsg.setText(""); //for reuse

            messagesToSend.add(sentMsg);
        }

        pinAndNotify(messagesToSend);
    }

    /*always called in thread for background sending
     return values
        0 : success,
        100 : network error (so abort queue),
        -1 : failure due to other error(won't happen usually, hence safe to ignore and continue with other pending messsages)
    */
    public static int sendTextMessageCloud(final ParseObject msg, final boolean isLive){
        if(!Utility.isInternetExistWithoutPopup()){
            Log.d(SendPendingMessages.LOGTAG, "send text cloud : saving cloud call when offline");
            return 100; //not connected to internet
        }

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

                SendMessage.notifyAdapter();
                //just notify outbox - no new query or updating count (since an old message just got new status)
                Outbox.notifyAdapter();
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
        final List<ParseObject> messagesToSend = new ArrayList<>();
        for(List<String> cls : selectedClassList) {
            if (filepath == null) return;

            final ParseObject sentMsg = new ParseObject(Constants.SENT_MESSAGES_TABLE);
            sentMsg.put("Creator", sender);
            sentMsg.put("code", cls.get(0));
            sentMsg.put("title", txtmsg);
            sentMsg.put("name", cls.get(1));
            SessionManager session = new SessionManager(Application.getAppContext());
            sentMsg.put("creationTime", session.getCurrentTime()); //needs to be updated once sent
            sentMsg.put("senderId", userId);
            sentMsg.put("userId", userId);
            sentMsg.put("pending", true);

            int slashindex = filepath.lastIndexOf("/");
            final String fileName = filepath.substring(slashindex + 1);// image file //

            if (fileName != null)
                sentMsg.put("attachment_name", fileName);

            typedmsg.setText(""); //for reuse

            messagesToSend.add(sentMsg);
        }

        pinAndNotify(messagesToSend);
    }

    //refer to sendTextMessageCloud
    public static int sendPicMessageCloud(final ParseObject msg, final boolean isLive) {
        if(!Utility.isInternetExistWithoutPopup()){
            Log.d(SendPendingMessages.LOGTAG, "send pic cloud : saving cloud call when offline");
            return 100; //not connected to internet
        }

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

                SendMessage.notifyAdapter();
                //just notify outbox - no new query or updating count (since an old message just got new status)
                Outbox.notifyAdapter();
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
