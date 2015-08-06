package trumplabs.schoolapp;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;

import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import BackGroundProcesses.MemberList;
import BackGroundProcesses.SendPendingMessages;
import library.UtilString;
import utility.Config;
import utility.SessionManager;
import utility.Utility;

/**
 * Created by dhanesh on 26/6/15.
 */
public class ComposeMessageHelper {
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

        user = ParseUser.getCurrentUser();
        if(user == null){
            Utility.LogoutUtility.logout();
            return;
        }

        userId = user.getUsername();
        sender = user.getString(Constants.NAME);
        session = new SessionManager(Application.getAppContext());
    }

    /*
    Send messages to subscribers
     */
    public void sendFunction() {
        if(user == null){
            Utility.LogoutUtility.logout();
            return;
        }

        if(Config.SHOWLOG) Log.d(ComposeMessage.LOGTAG, "helper : sendFunction()");
        typedtxt = ComposeMessage.typedmsg.getText().toString().trim();  //message to send

        if (!UtilString.isBlank(typedtxt) && ComposeMessage.sendimgpreview.getVisibility() == View.GONE) {

            // when its not an image message******************
            sendTxtMsgtoSubscribers(typedtxt);

        } else if (ComposeMessage.sendimgpreview.getVisibility() == View.VISIBLE) {
            // Sending an image file
            // passing image file path and message content as
            // parameters
            sendPic((String) ComposeMessage.sendimgpreview.getTag(), typedtxt);

            // for image we try to keep track of progress
            ComposeMessage.typedmsg.setText("");
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
                    Utility.toast("Sorry! Can't send your message");
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
            if(Config.SHOWLOG) Log.d(ComposeMessage.LOGTAG, "source outside - current # outbox msgs=" + Outbox.groupDetails.size());
            for(ParseObject msg : messagesToSend){
                Outbox.groupDetails.add(0, msg);
            }

            Outbox.refreshSelf();//not just adapter notify but also make sure that the layout changes(No sent messages -> msg list)

            Outbox.totalOutboxMessages += messagesToSend.size(); //totalOutboxMessages would have a proper value since source is MainActivity

            if(Config.SHOWLOG) Log.d(ComposeMessage.LOGTAG, "source outside - added to Outbox.groupdetails #=" + messagesToSend.size() +
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

            if(Config.SHOWLOG) Log.d(ComposeMessage.LOGTAG, "source inside - current # sendmessage msgs=" + SendMessage.groupDetails.size());
            for(ParseObject msg : messagesToSend){
                if(msg.getString("code") != null && SendMessage.groupCode != null && msg.getString("code").equals(SendMessage.groupCode)) {
                    SendMessage.groupDetails.add(0, msg);
                    SendMessage.totalClassMessages++; //increment one by one only for the concerned messages
                }
            }

            SendMessage.notifyAdapter(); //just notify, as the content(new msg) has been added

            if(Config.SHOWLOG) Log.d(ComposeMessage.LOGTAG, "source inside - added to SendMessage.groupdetails #=" + messagesToSend.size() +
                    " total SendMessage count=" + SendMessage.totalClassMessages + ", #visible outbox msgs=" + SendMessage.groupDetails.size());
        }

        ComposeMessage.sendButtonClicked = true; //Quick hack to compensate delayed(after pinning of msgs) spawning of pending msg thread
    }


    private void sendTxtMsgtoSubscribers(final String typedtxt) {
        final List<ParseObject> messagesToSend = new ArrayList<>();

        Long batchId = Calendar.getInstance().getTimeInMillis();

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
            sentMsg.put(Constants.BATCH_ID, batchId);

            ComposeMessage.typedmsg.setText(""); //for reuse

            messagesToSend.add(sentMsg);
        }

        pinAndNotify(messagesToSend);
    }

    /*always called in thread for background sending
     return values
        0 : success,
        100 : network error
        ParseException.INVALID_SESSION_TOKEN(209) : invalid session token
        -1 : failure due to other error(won't happen usually, hence safe to ignore and continue with other pending messsages)
    */
    public static int sendMultiTextMessageCloud(final List<ParseObject> batch){
        if(!Utility.isInternetExistWithoutPopup()){
            if(Config.SHOWLOG) Log.d(SendPendingMessages.LOGTAG, "send text cloud : saving cloud call when offline");
            return 100; //not connected to internet
        }

        ParseObject master = batch.get(0);

        List<String> classcodes = new ArrayList<>();
        List<String> classnames = new ArrayList<>();
        List<Boolean> checkmemberFlagArray = new ArrayList<>();

        boolean CHECK_MEMBER_FLAG = false;
        if(batch.size() == 1){
            CHECK_MEMBER_FLAG = true; //only when sending to one class
        }

        for(int i=0; i<batch.size(); i++){
            String code = batch.get(i).getString("code");
            classcodes.add(code);
            classnames.add(batch.get(i).getString("name"));
            checkmemberFlagArray.add(CHECK_MEMBER_FLAG && MemberList.getMemberCount(code) < 1);
        }

        //if live, then only show "sent" toast
        //sending message using parse cloud function
        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("classcode", classcodes);
        params.put("classname", classnames);
        params.put("checkmember", checkmemberFlagArray);

        params.put("message", master.getString("title"));

        try{
            HashMap result = ParseCloud.callFunction("sendMultiTextMessage", params);
            if (result != null) {
                int retVal = 0; //success
                List<Date> createdAtList = (List<Date>) result.get("createdAt");
                List<String> objectIdList = (List<String>) result.get("messageId");

                if(createdAtList == null || objectIdList == null){
                    return -1; //unexpected error
                }

                if(Config.SHOWLOG) Log.d(SendPendingMessages.LOGTAG, "sendMultiTextMessage response objectIdList=" + objectIdList);

                List<ParseObject> failedMessages = new ArrayList<>();

                for(int i=0; i<createdAtList.size(); i++){
                    String objectId = objectIdList.get(i);
                    ParseObject msg = batch.get(i);

                    if(UtilString.isBlank(objectId)){
                        failedMessages.add(msg);
                        continue;
                    }

                    Date createdAt = createdAtList.get(i);

                    //update msg and pin
                    msg.put("objectId", objectId);
                    msg.put("pending", false);
                    msg.put("creationTime", createdAt);

                    msg.pinInBackground();
                }

                List<List<String>> updatedCreatedGroups = (List<List<String>>) result.get(Constants.CREATED_GROUPS);

                if(failedMessages.size() == batch.size()){
                    //only if all fail
                    if(updatedCreatedGroups != null){
                        //class has been deleted
                        retVal = 200;
                    }
                    else {
                        //no members error hence
                        retVal = 201;
                    }
                }

                if(failedMessages.size() > 0) {
                    //first update created groups of user
                    if(Config.SHOWLOG) Log.d(SendPendingMessages.LOGTAG, "FAILED #classes=" + failedMessages.size());

                    try {
                        ParseUser user = ParseUser.getCurrentUser();
                        if (user != null && updatedCreatedGroups != null) {
                            if(Config.SHOWLOG) Log.d(SendPendingMessages.LOGTAG, "setting Created_groups in the user");
                            user.put(Constants.CREATED_GROUPS, updatedCreatedGroups);
                            user.pin();

                            List<String> deletedClasses = new ArrayList<>();
                            for(ParseObject failedMsg : failedMessages){
                                deletedClasses.add(failedMsg.getString(Constants.GroupDetails.CODE));
                            }

                            //Notify created classrooms adapter
                            Classrooms.refreshCreatedClassrooms(deletedClasses);
                        }
                    } catch (ParseException err) {
                        err.printStackTrace();
                    }

                    //unpin the message,
                    ParseObject.unpinAll(failedMessages);
                    //delete the message from lists, moved to SendMessage.notifyAdapter & Outbox.notifyAdapter

                }

                //notify outbox and class page adapter
                SendMessage.notifyAdapter(failedMessages);
                Outbox.notifyAdapter(failedMessages);
                return retVal; //success
            }
            return -1; //unexpected error
        }
        catch (ParseException e){
            if(Utility.LogoutUtility.checkAndHandleInvalidSession(e)){
                return ParseException.INVALID_SESSION_TOKEN;
            }
            else if(e.getCode() == ParseException.CONNECTION_FAILED){
                return 100; //network error
            }
            e.printStackTrace();
            return -1;
        }
    }

    // Send Image Pic
    private void sendPic(String filepath, String txtmsg){
        final List<ParseObject> messagesToSend = new ArrayList<>();

        Long batchId = Calendar.getInstance().getTimeInMillis();

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
            sentMsg.put(Constants.BATCH_ID, batchId);

            int slashindex = filepath.lastIndexOf("/");
            final String fileName = filepath.substring(slashindex + 1);// image file //

            if (fileName != null)
                sentMsg.put("attachment_name", fileName);

            ComposeMessage.typedmsg.setText(""); //for reuse

            messagesToSend.add(sentMsg);
        }

        pinAndNotify(messagesToSend);
    }


    /*always called in thread for background sending
    return values
       0 : success,
       100 : network error
       ParseException.INVALID_SESSION_TOKEN(209) : invalid session token
       -1 : failure due to other error(won't happen usually, hence safe to ignore and continue with other pending messsages)
   */
    public static int sendMultiPicMessageCloud(final List<ParseObject> batch){
        if(!Utility.isInternetExistWithoutPopup()){
            if(Config.SHOWLOG) Log.d(SendPendingMessages.LOGTAG, "send text cloud : saving cloud call when offline");
            return 100; //not connected to internet
        }

        ParseObject master = batch.get(0);

        List<String> classcodes = new ArrayList<>();
        List<String> classnames = new ArrayList<>();
        List<Boolean> checkmemberFlagArray = new ArrayList<>();

        boolean CHECK_MEMBER_FLAG = false;
        if(batch.size() == 1){
            CHECK_MEMBER_FLAG = true; //only when sending to one class
        }

        for(int i=0; i<batch.size(); i++){
            String code = batch.get(i).getString("code");
            classcodes.add(code);
            classnames.add(batch.get(i).getString("name"));
            checkmemberFlagArray.add(CHECK_MEMBER_FLAG && MemberList.getMemberCount(code) < 1);
        }

        /* image file work */
        //don't have attachement, objectid
        //update creationTime, pending
        String imageName = null;
        if (master.containsKey("attachment_name"))
            imageName = master.getString("attachment_name");

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

        String oldName = imageName;
        imageName = imageName.replaceAll("[^a-zA-Z0-9_\\.]", "");
        imageName = "i" + imageName;

        final ParseFile file = new ParseFile(imageName, data);

        if(Config.SHOWLOG) Log.d(SendPendingMessages.LOGTAG, "sendPicMessageCloud : data size=" + data.length + " bytes, name=" + imageName + ", old=" + oldName);

        try{
            file.save();
            if(Config.SHOWLOG) Log.d(SendPendingMessages.LOGTAG, "sendPicMessageCloud : file save success");

            //sending message using parse cloud function
            HashMap<String, Object> params = new HashMap<String, Object>();
            params.put("classcode", classcodes);
            params.put("classname", classnames);
            params.put("checkmember", checkmemberFlagArray);

            params.put("message", master.getString("title"));
            params.put("filename", master.getString("attachment_name"));
            params.put("parsefile", file);

            HashMap result = ParseCloud.callFunction("sendMultiPhotoTextMessage", params);
            if (result != null) {
                int retVal = 0; //success
                List<Date> createdAtList = (List<Date>) result.get("createdAt");
                List<String> objectIdList = (List<String>) result.get("messageId");

                if(createdAtList == null || objectIdList == null){
                    return -1; //unexpected error
                }

                if(Config.SHOWLOG) Log.d(SendPendingMessages.LOGTAG, "sendMultiTextMessage response objectIdList=" + objectIdList);

                List<ParseObject> failedMessages = new ArrayList<>();

                for(int i=0; i<createdAtList.size(); i++){
                    String objectId = objectIdList.get(i);
                    ParseObject msg = batch.get(i);

                    if(UtilString.isBlank(objectId)){
                        failedMessages.add(msg);
                        continue;
                    }

                    Date createdAt = createdAtList.get(i);

                    //update msg and pin
                    msg.put("objectId", objectId);
                    msg.put("pending", false);
                    msg.put("creationTime", createdAt);
                    msg.put("attachment", file);

                    msg.pinInBackground();
                }

                List<List<String>> updatedCreatedGroups = (List<List<String>>) result.get(Constants.CREATED_GROUPS);
                if(failedMessages.size() == batch.size()){
                    //only if all fail
                    if(updatedCreatedGroups != null){
                        //class has been deleted
                        retVal = 200;
                    }
                    else {
                        //no members error hence
                        retVal = 201;
                    }
                }

                if(failedMessages.size() > 0) {
                    //first update created groups of user
                    if(Config.SHOWLOG) Log.d(SendPendingMessages.LOGTAG, "FAILED #classes=" + failedMessages.size());

                    try {
                        ParseUser user = ParseUser.getCurrentUser();
                        if (user != null && updatedCreatedGroups != null) {
                            user.put(Constants.CREATED_GROUPS, updatedCreatedGroups);
                            user.pin();

                            List<String> deletedClasses = new ArrayList<>();
                            for(ParseObject failedMsg : failedMessages){
                                deletedClasses.add(failedMsg.getString(Constants.GroupDetails.CODE));
                            }
                            //Notify created classrooms adapter
                            Classrooms.refreshCreatedClassrooms(deletedClasses);
                        }
                    } catch (ParseException err) {
                        err.printStackTrace();
                    }

                    //unpin the message,
                    ParseObject.unpinAll(failedMessages);
                    //delete the message from lists, moved to SendMessage.notifyAdapter & Outbox.notifyAdapter
                }

                //notify outbox and class page adapter
                SendMessage.notifyAdapter(failedMessages);
                Outbox.notifyAdapter(failedMessages);
                return retVal; //success
            }
            return -1; //unexpected error
        }
        catch (ParseException e){
            if(Utility.LogoutUtility.checkAndHandleInvalidSession(e)){
                return ParseException.INVALID_SESSION_TOKEN;
            }
            else if(e.getCode() == ParseException.CONNECTION_FAILED){
                return 100; //network error
            }
            e.printStackTrace();
            return -1;
        }
    }
}
