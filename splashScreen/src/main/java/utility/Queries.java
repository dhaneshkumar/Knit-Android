package utility;

import android.os.AsyncTask;
import android.util.Log;

import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import library.UtilString;
import trumplabs.schoolapp.Application;
import trumplabs.schoolapp.Classrooms;
import trumplabs.schoolapp.Constants;
import trumplabs.schoolapp.MemberDetails;

public class Queries {
    String userId;
    List<ParseObject> msgs;
    int messageCount = Config.inboxMsgCount;
    int createMsgCount = Config.createMsgCount;
    Date newTimeStamp;
    Date oldTimeStamp;
    int membersCount = Config.membersCount;
    ParseUser currentUser;
    ParseUser user;

    public Queries() {
        ParseUser userObject = ParseUser.getCurrentUser();
        if (userObject == null) {
            Utility.logout();
            return;
        }
        userId = userObject.getUsername();
    }

    public List<ParseObject> getLocalInboxMsgs() throws ParseException {

        ParseQuery<ParseObject> query = ParseQuery.getQuery("GroupDetails");
        query.fromLocalDatastore();
        query.orderByDescending(Constants.TIMESTAMP);
        query.whereEqualTo("userId", userId);
        query.setLimit(messageCount);


        List<ParseObject> msgs = query.find();

        if (msgs != null && msgs.size() != 0) {
            newTimeStamp = msgs.get(0).getCreatedAt();
            oldTimeStamp = msgs.get(msgs.size() - 1).getCreatedAt();
        }

    /*
     * Query for retrieving local msg
     */
        ParseQuery<ParseObject> query1 = ParseQuery.getQuery("LocalMessages");
        query1.fromLocalDatastore();
        query1.orderByDescending("creationTime");
        query1.whereEqualTo("userId", userId);
        query1.setLimit(messageCount);

        List<ParseObject> localMsgList = query1.find();

    /*
     * Mixing local and global message
     */

        if (msgs != null && localMsgList != null) {
            // Utility.toast("not null");

            if (msgs.size() == 0) {
                msgs.addAll(localMsgList);
            } else if (localMsgList.size() == 0) {
            } else if (msgs.size() < messageCount) {

                Log.d("msg", "0000");

                if (msgs.get(msgs.size() - 1).getCreatedAt() != null
                        && localMsgList.get(0).getDate("creationTime") != null) {
                    if (msgs.get(msgs.size() - 1).getCreatedAt()
                            .compareTo(localMsgList.get(0).getDate("creationTime")) == 1) {
                        msgs.addAll(localMsgList);
                    } else {
                        msgs = mergeAllOnDate(msgs, localMsgList, true);
                    }
                }
            } else if (msgs.size() >= messageCount) {
                Log.d("msg", "000022222222222");
                if (msgs.get(msgs.size() - 1).getCreatedAt() != null
                        && localMsgList.get(0).getDate("creationTime") != null) {

                    Log.d("msg", "0000232323");
                    if (msgs.get(msgs.size() - 1).getCreatedAt()
                            .compareTo(localMsgList.get(0).getDate("creationTime")) == 1) {
                        // do nothing

                        Log.d("msg", "323232");
                    } else {

                        Log.d("msg", "2222");

            /*
             * merge only those local msg whose creation time is greater than last cretedAt global
             * time
             */

                        msgs = mergeAllOnDate(msgs, localMsgList, false);
                    }
                } else
                    Log.d("msg", "0000 local - null");
            }
        }

    /*
     * for(int i =0; i<msgs.size(); i++) { if(msgs.get(i).getCreatedAt()!= null)
     * Log.d("localMessage", msgs.get(i).getString("title") + "  -- "+ msgs.get(i).getCreatedAt() );
     * else Log.d("localMessage", msgs.get(i).getString("title") + "  -- "+
     * msgs.get(i).getDate("creationTime") ); }
     */

        return msgs;
    }

    /*
     * merging both list based on created time
     *
     * flag used to denote specific add(flag : false)/ all object addition(true)
     */
    public List<ParseObject> mergeAllOnDate(List<ParseObject> globalMsg, List<ParseObject> localMsg,
                                            boolean flag) {
        int m = globalMsg.size();
        int n = localMsg.size();
        int i = 0, j = 0;


        List<ParseObject> result = new ArrayList<ParseObject>();

        while (i < m && j < n) {
            Date globalDate = globalMsg.get(i).getCreatedAt();
            Date localDate = localMsg.get(j).getDate("creationTime");



            if (globalDate.compareTo(localDate) == 1 || globalDate.compareTo(localDate) == 0) {
                result.add(globalMsg.get(i));
                i++;
            } else {
                result.add(localMsg.get(j));
                j++;
            }
        }

        if (i == m) {
            while (j < n && flag) {
                result.add(localMsg.get(j));
                j++;
            }
        } else {
            while (i < m) {
                result.add(globalMsg.get(i));
                i++;
            }
        }

        return result;
    }


    //fetch messages from server
    public List<ParseObject> getServerInboxMsgs() {
        List<ParseObject> msgList = null;
        try {
            msgList = getLocalInboxMsgs();

        } catch (ParseException e3) {
        }


        if (msgList == null)
            msgList = new ArrayList<ParseObject>();

        SessionManager sessionManager = new SessionManager(Application.getAppContext());
        ParseUser user = ParseUser.getCurrentUser();

        if(user == null) return msgList; //won't happen in general


        //newTimeStamp has been set in getLocalInboxMsgs
        //If no local messages :
        //      if sign up mode, set timestamp as user's creation time.
        //      Otherwise use showLatestMessagesWithLimit for login mode

        if(newTimeStamp == null && sessionManager.getSignUpAccount()){
            Log.d("DEBUG_QUERIES_SERVER_MSGS", "timestamp null with SIGNUP mode. Using user's creation time as timestamp");
            newTimeStamp = user.getCreatedAt();
        }

        //newTimeStamp has been appropriately set handling cases
        //if newTimeStamp is null this means that (no local messages + login mode) and we're fetching old+new messages
        //if newTimeStamp is NOT null, fetch all new messages with timestamp > newTimeStamp

        if(newTimeStamp == null){
            Log.d("DEBUG_QUERIES_SERVER_MSGS", "timestamp null. So no messages stored. Fetching first batch of messages");
            HashMap<String, Object> parameters = new HashMap<String, Object>();

            parameters.put("limit", 50);
            parameters.put("classtype", "j");
            try {
                HashMap<String, List<ParseObject> > resultMap = ParseCloud.callFunction("showLatestMessagesWithLimit", parameters);
                List<ParseObject> allMessages = resultMap.get("message");
                List<ParseObject> allStates = resultMap.get("states");

                //since old messages, need to update their MessageState(like_status, confused_status)
                if(allMessages != null) {
                    Log.d("DEBUG_QUERIES_SERVER_MSGS", "[limit] : fetched msgs " + allMessages.size());
                    Log.d("DEBUG_QUERIES_SERVER_MSGS", "[limit] : fetched states " + allStates.size());
                    //use allStates to set appropriate state for the each received message
                    for(int m=0; m < allMessages.size(); m++){
                        ParseObject msg = allMessages.get(m);
                        for(int s=0; s < allStates.size(); s++){
                            ParseObject msgState = allStates.get(s);
                            if(msgState.getString("message_id").equals(msg.getObjectId())){
                                msg.put(Constants.LIKE, msgState.getBoolean(Constants.LIKE_STATUS));
                                msg.put(Constants.CONFUSING, msgState.getBoolean(Constants.CONFUSED_STATUS));
                                msg.put(Constants.SYNCED_LIKE, msgState.getBoolean(Constants.LIKE_STATUS));
                                msg.put(Constants.SYNCED_CONFUSING, msgState.getBoolean(Constants.CONFUSED_STATUS));
                                break;
                            }
                            //default
                            msg.put(Constants.LIKE, false);
                            msg.put(Constants.CONFUSING, false);
                            msg.put(Constants.SYNCED_LIKE, false);
                            msg.put(Constants.SYNCED_CONFUSING, false);
                        }
                        msg.put(Constants.USER_ID, userId);
                        msg.put(Constants.DIRTY_BIT, false);
                        msg.put(Constants.SEEN_STATUS, 0); // we assume that if msg downloaded, then must have seen
                    }

                    Log.d("DEBUG_QUERIES_SERVER_MSGS", "[limit] pinning all together");
                    ParseObject.pinAll(allMessages); //pin all the messages
                    msgList.addAll(0, allMessages);
                }
            }
            catch (ParseException e){
                e.printStackTrace();
            }
        }
        else{
            Log.d("DEBUG_QUERIES_SERVER_MSGS", "fetch messages greater than newTimeStamp");
            //fetch messages greater than newTimeStamp
            HashMap<String, Date> parameters = new HashMap<String, Date>();

            parameters.put("date", newTimeStamp);

            try {
                //just fetch, set default state(like, confused = false, false)
                List<ParseObject> allMessages= ParseCloud.callFunction("showLatestMessages", parameters);
                if(allMessages != null) {
                    Log.d("DEBUG_QUERIES_SERVER_MSGS", "[time] fetched " + allMessages.size());
                    for(int i=0; i<allMessages.size(); i++){
                        ParseObject msg = allMessages.get(i);
                        msg.put(Constants.LIKE, false);
                        msg.put(Constants.CONFUSING, false);
                        msg.put(Constants.SYNCED_LIKE, false);
                        msg.put(Constants.SYNCED_CONFUSING, false);

                        msg.put(Constants.USER_ID, userId);
                        msg.put(Constants.DIRTY_BIT, false);
                        msg.put(Constants.SEEN_STATUS, 0); // we assume that if msg downloaded, then must have seen
                    }
                    Log.d("DEBUG_QUERIES_SERVER_MSGS", "[time] pinning all together");
                    ParseObject.pinAll(allMessages); //pin all the messages
                    msgList.addAll(0, allMessages); //in the beginning so that [newMessages ... followed by ... original_msgList]
                }
            }
            catch (ParseException e){
                e.printStackTrace();
            }
        }

        return msgList;
    }


    public List<ParseObject> getExtraLocalInboxMsgs(List<ParseObject> msgs) throws ParseException {

        if (msgs == null)
            return null;

        if (msgs.size() > 0 && msgs.get(msgs.size() - 1) != null) {

            if (msgs.get(msgs.size() - 1).getCreatedAt() != null)
                oldTimeStamp = msgs.get(msgs.size() - 1).getCreatedAt();
            else
                oldTimeStamp = msgs.get(msgs.size() - 1).getDate("creationTime");
            // To make infinite inbox , remove above line and join a new class :P

        }

        ParseQuery<ParseObject> query = ParseQuery.getQuery("GroupDetails");
        query.fromLocalDatastore();
        query.orderByDescending(Constants.TIMESTAMP);
        query.whereEqualTo("userId", userId);
        query.setLimit(messageCount);

        if (oldTimeStamp != null)
            query.whereLessThan(Constants.TIMESTAMP, oldTimeStamp);

        List<ParseObject> msgList1 = query.find();

        // appending extra objects to the end of list
        if (msgList1 != null) {
            msgs.addAll(msgList1);
            // Utility.toast(msgList1.size()+"");
        }
        return msgs;

    }

    // **********************CREATE CLASS MESSAGES QUERY******************


    public List<ParseObject> getLocalCreateMsgs(String groupCode, List<ParseObject> groupDetails,
                                                boolean flag) throws ParseException {
        Date oldTime = null;

        if (groupDetails != null && groupDetails.size() > 0) {
            if (groupDetails.get(0).get("creationTime") != null)
                oldTime = (Date) groupDetails.get(0).get("creationTime");
        } else
            groupDetails = new ArrayList<ParseObject>();

        ParseQuery<ParseObject> query = ParseQuery.getQuery("SentMessages");
        query.fromLocalDatastore();
        query.orderByDescending("creationTime");
        query.whereEqualTo("userId", userId);
        query.whereEqualTo("code", groupCode);

        if (flag)
            query.setLimit(2 * createMsgCount);
        else
            query.setLimit(createMsgCount);

        if (oldTime != null)
            query.whereLessThan("creationTime", oldTime);

        List<ParseObject> msgList1 = query.find();

        // appending extra objects to the end of list

        if (msgList1 != null) {
            for (int i = 0; i < msgList1.size(); i++) {
                groupDetails.add(0, msgList1.get(i));


            }


        }

        return groupDetails;
    }

    /*
     * Retreiving created messages from server
     */
    public List<ParseObject> getServerCreateMsgs(final String groupCode,
                                                 List<ParseObject> groupDetails, boolean extra) throws ParseException {


        // Setting latest and old timestamp

        if (Constants.serverMsgCounter == null)
            Constants.serverMsgCounter = new HashMap<String, Integer>();

        if (Constants.serverMsgCounter.get(groupCode) != null
                && Constants.serverMsgCounter.get(groupCode) < 3 * createMsgCount)
            ;
        else {

            Date oldTime = null;

            if (groupDetails != null && groupDetails.size() > 0) {
                // newTime = msgs.get(0).getString("timeStamp");

                if ((Date) groupDetails.get(0).get("creationTime") != null)
                    oldTime = (Date) groupDetails.get(0).get("creationTime");
            }

            ParseQuery<ParseObject> query = ParseQuery.getQuery("GroupDetails");
            query.orderByDescending(Constants.TIMESTAMP);
            query.whereEqualTo("code", groupCode);
            query.setLimit(3 * createMsgCount);

            if (oldTime != null) {
                query.whereLessThan(Constants.TIMESTAMP, oldTime);

            }

            try {
                List<ParseObject> newmsgs = query.find();

                if (newmsgs != null) {

          /*
           * If all obj are fetched then no request should be sent to server , that's why we are
           * keeping the count
           */
                    Constants.serverMsgCounter.put(groupCode, newmsgs.size());

                    Queries2 listQuery = new Queries2();


                    for (int k = 0; k < newmsgs.size(); k++) {

            /*
             * check item already stored or not comparing usring title and date
             */
                        if (listQuery.isItemExist(groupDetails, newmsgs.get(k)))
                            continue;

                        if (!extra) {
                            // Adding new msgs to msg list

                            // Storing new msgs to local database

                            ParseObject messages = newmsgs.get(k);

                            ParseObject sentMsg = new ParseObject("SentMessages");
                            sentMsg.put("objectId", messages.getObjectId());
                            sentMsg.put("Creator", messages.getString("Creator"));
                            sentMsg.put("code", messages.getString("code"));
                            sentMsg.put("title", messages.getString("title"));
                            sentMsg.put("name", messages.getString("name"));
                            sentMsg.put("creationTime", messages.getCreatedAt());
                            sentMsg.put("senderId", messages.getString("senderId"));
                            sentMsg.put("userId", userId);


                            Utility.ls(messages.getString("code") +  "  :  " + messages.getString("title"));

                            if (messages.get("attachment") != null)
                                sentMsg.put("attachment", messages.get("attachment"));
                            if (messages.getString("attachment_name") != null)
                                sentMsg.put("attachment_name", messages.getString("attachment_name"));
                            if (messages.get("senderpic") != null)
                                sentMsg.put("senderpic", messages.get("senderpic"));
                            sentMsg.pin();

                            messages.put("creationTime", messages.getCreatedAt());
                            groupDetails.add(0, messages);


                        }
                    }

                } else
                    Constants.serverMsgCounter.put(groupCode, 0);

            } catch (ParseException e) {
            }
        }

        return groupDetails;
    }

    // ****************************CREATE CLASSS*************************************

    public boolean checkClassNameExist(String className) {
        ParseUser user = ParseUser.getCurrentUser();

        if (user != null) {
            List<List<String>> createdGroups = new ArrayList<List<String>>();
            createdGroups = user.getList("Created_groups");
            if (createdGroups != null)
                for (int i = 0; i < createdGroups.size(); i++) {
                    if (className.equals(createdGroups.get(i).get(1).toString())) {
                        return true;
                    }
                }
        }

        return false;
    }


    // ****************************CLASS MEMBERS********************************

    public List<MemberDetails> getLocalClassMembers(String groupCode) {

        List<MemberDetails> memberList = new ArrayList<MemberDetails>();

        // Retrieving local messages from group members locally
        ParseQuery<ParseObject> query = ParseQuery.getQuery("GroupMembers");
        query.fromLocalDatastore();
        query.whereEqualTo("userId", userId);
        query.whereEqualTo("code", groupCode);
        query.whereEqualTo("status", null);


        try {
            List<ParseObject> appMembers = query.find();

            if (appMembers != null && appMembers.size() > 0) {
                for (int i = 0; i < appMembers.size(); i++) {
                    ParseObject members = appMembers.get(i);

                 /*   if(members.getString("status") != null)
                        Log.d("STATUS",members.getObjectId() + "-" + members.getString("status") +  "-");
                    else
                        Log.d("STATUS", members.getObjectId() + "  :  null status");*/


                    List<String> childList = members.getList("children_names");
                    if (childList != null && childList.size() > 0) {

                        for (int j = 0; j < childList.size(); j++) {

                            String child = childList.get(j);

                            if (!UtilString.isBlank(child)) {

                                child = UtilString.changeFirstToCaps(child);
                                MemberDetails member =
                                        new MemberDetails(members.getObjectId(), MemberDetails.APP_MEMBER, child);
                                memberList.add(member);
                            }
                        }
                    } else {

                        String parent = members.getString("name");
                        MemberDetails member =
                                new MemberDetails(members.getObjectId(), MemberDetails.APP_MEMBER, parent);
                        memberList.add(member);
                    }
                }
            }
        } catch (ParseException e) {
        }


    /*
     * Retrieve sms subscribed users list locally
     */
        ParseQuery<ParseObject> smsQuery = ParseQuery.getQuery(Constants.MESSAGE_NEEDERS);
        smsQuery.fromLocalDatastore();
        smsQuery.addAscendingOrder("subscriber");
        smsQuery.whereEqualTo("userId", userId);
        smsQuery.whereEqualTo("cod", groupCode); // "cod" - as written in table
        smsQuery.whereEqualTo("status", null);

        List<ParseObject> smsUsersList = null;
        try {
            smsUsersList = smsQuery.find();

            if (smsUsersList != null) {

        /*
         * Storing new entries locally
         */
                for (int i = 0; i < smsUsersList.size(); i++) {

                    ParseObject smsMembers = smsUsersList.get(i);

          /*
           * Adding members in memberlist
           */
                    String child = smsMembers.getString("subscriber");
                    if (!UtilString.isBlank(child)) {
                        child = UtilString.changeFirstToCaps(child);
                        MemberDetails member =
                                new MemberDetails(smsMembers.getObjectId(), MemberDetails.SMS_MEMBER, child);
                        memberList.add(member);
                    }
                    else
                    {
                        if(!UtilString.isBlank(smsMembers.getString("number"))) {
                            child = smsMembers.getString("number");
                            MemberDetails member =
                                    new MemberDetails(smsMembers.getObjectId(), MemberDetails.SMS_MEMBER, child);
                            memberList.add(member);
                        }
                    }

                }
            }
        } catch (ParseException e) {
        }

        return sortMemberList(memberList);
    }


    /**
     * Sorting memberlist in alphabetical order
     *
     * @param memberList
     * @return
     */
    public List<MemberDetails> sortMemberList(List<MemberDetails> memberList) {
        if (memberList == null || memberList.size() < 1)
            return memberList;

        List<MemberDetails> sortedList = new ArrayList<MemberDetails>();

        for (int i = 0; i < memberList.size(); i++) {

            String minMember = memberList.get(i).getChildName();
            int k = i;
            for (int j = i + 1; j < memberList.size(); j++) {

                String child = memberList.get(j).getChildName();
                if (minMember.compareTo(child) > 0) {
                    k = j;
                    minMember = child;
                }
            }

            sortedList.add(memberList.get(k));

            // swap items

            if (i != k) {
                MemberDetails tempMember = memberList.get(i);
                memberList.set(i, memberList.get(k));
                memberList.set(k, tempMember);
            }

        }

        return sortedList;
    }

    /**
     * Gives member count of a class
     * @param groupCode
     * @return total member count (app + sms members)
     * @throws ParseException
     */
    public int getMemberCount(String groupCode) throws ParseException {
        int appCount = 0, smsCount = 0;
        ParseQuery<ParseObject> query1 = ParseQuery.getQuery("GroupMembers");
        query1.fromLocalDatastore();
        query1.whereEqualTo("code", groupCode);
        query1.whereEqualTo("userId", userId);
        query1.whereEqualTo("status", null);
        appCount = query1.count();

        ParseQuery<ParseObject> smsQuery = ParseQuery.getQuery(Constants.MESSAGE_NEEDERS);
        smsQuery.fromLocalDatastore();
        smsQuery.whereEqualTo("userId", userId);
        smsQuery.whereEqualTo("cod", groupCode);
        smsQuery.whereEqualTo("status", null);

        smsCount = smsQuery.count();

        return appCount + smsCount;
    }

    public ParseObject getClassObject(String groupCode) throws ParseException{
        ParseQuery query = ParseQuery.getQuery("Codegroup");
        query.fromLocalDatastore();
        //query.whereEqualTo("userId", userId); // Not needed as it doesn't matter. All will have same Timestamp
        query.whereEqualTo("code", groupCode);
        query.setLimit(1);

        List<ParseObject> objects = query.find();
        if(objects != null && objects.size() > 0){
            return objects.get(0);
        }

        Log.d("DEBUG_QUERIES_GET_CLASS_OBJECT", "[ ^" + groupCode + "^ ] Not found locally. Fetching from Server");

        //not found locally
        ParseQuery serverQuery = ParseQuery.getQuery("Codegroup");
        serverQuery.whereEqualTo("code", groupCode);
        serverQuery.setLimit(1);

        List<ParseObject> serverObjects = serverQuery.find();
        if(serverObjects != null && serverObjects.size() > 0){
            /*for(int i=0; i<objects.size(); i++){
                Log.d("DEBUG_QUERIES_GET_CLASS_OBJECT", "[ ^" + objects.get(i).getString("code") + "^ ]");
            }*/
            serverObjects.get(0).pinInBackground();
            return serverObjects.get(0);
        }

        Log.d("DEBUG_QUERIES_GET_CLASS_OBJECT", "[ ^" + groupCode + "^ ] Not found even on server");

        return null;
    }


  /*
   * Searches for given class in joined classrooms
   */

    public static boolean isJoinedClassExist(String classCode) {
        ParseUser user = ParseUser.getCurrentUser();
        List<List<String>> joinedGroups;

        if (user != null) {
            joinedGroups = Classrooms.getJoinedGroups(user); //won't be null

            for (List<String> group : joinedGroups) {
                if (classCode.equals(group.get(0)))
                    return true;
            }
        }

        return false;
    }

    /*
     * get FAQs locally
     */
    public List<ParseObject> getLocalFAQs(String role) throws ParseException {

        ParseQuery<ParseObject> query = ParseQuery.getQuery("FAQs");
        query.fromLocalDatastore();
        query.orderByAscending(Constants.TIMESTAMP);
        query.whereEqualTo("userId", userId);

        if (role.equals("parent"))
            query.whereEqualTo("role", "Parent"); // role stored in faq is "Parent" where in user table -
        // "parent"

        List<ParseObject> msgs = query.find();

        return msgs;
    }

    /**
     * Get locally sent messages in combined way
     *
     * @return list of sent messages
     * @how query locally from sentMessage table and return first 20 messages list
     */

    public List<ParseObject> getLocalOutbox() {

        ParseQuery<ParseObject> query = ParseQuery.getQuery("SentMessages");
        query.fromLocalDatastore();
        query.orderByDescending("creationTime");
        query.whereEqualTo("userId", userId);
        query.setLimit(Config.outboxMsgCount);

        List<ParseObject> outboxList = null;
        try {
            outboxList = query.find();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return outboxList;
    }

    public List<ParseObject> getExtraLocalOutbox(List<ParseObject> msgs) throws ParseException {

        if (msgs == null)
            return null;
        Date lastTimeStamp = null;

        if (msgs.size() > 0 && msgs.get(msgs.size() - 1) != null) {
            lastTimeStamp = msgs.get(msgs.size() - 1).getDate("creationTime");
        }

        ParseQuery<ParseObject> query = ParseQuery.getQuery("SentMessages");
        query.fromLocalDatastore();
        query.orderByDescending("creationTime");
        query.whereEqualTo("userId", userId);
        query.setLimit(Config.outboxMsgCount);

        if (lastTimeStamp != null)
            query.whereLessThan("creationTime", lastTimeStamp);

        List<ParseObject> msgList1 = query.find();

        // appending extra objects to the end of list
        if (msgList1 != null) {
            msgs.addAll(msgList1);
            // Utility.toast(msgList1.size()+"");
        }
        return msgs;
    }

}
