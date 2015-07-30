package utility;

import android.util.Log;

import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import library.UtilString;
import trumplabs.schoolapp.Application;
import trumplabs.schoolapp.Classrooms;
import trumplabs.schoolapp.Constants;
import trumplabs.schoolapp.MemberDetails;

public class Queries {
    static final boolean SHOWLOG = false;

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
            Utility.LogoutUtility.logout();
            return;
        }
        userId = userObject.getUsername();
    }

    public List<ParseObject> getLocalInboxMsgs() throws ParseException {

        ParseQuery<ParseObject> query = ParseQuery.getQuery(Constants.GroupDetails.TABLE);
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

                if(Config.SHOWLOG) Log.d("msg", "0000");

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
                if(Config.SHOWLOG) Log.d("msg", "000022222222222");
                if (msgs.get(msgs.size() - 1).getCreatedAt() != null
                        && localMsgList.get(0).getDate("creationTime") != null) {

                    if(Config.SHOWLOG) Log.d("msg", "0000232323");
                    if (msgs.get(msgs.size() - 1).getCreatedAt()
                            .compareTo(localMsgList.get(0).getDate("creationTime")) == 1) {
                        // do nothing

                        if(Config.SHOWLOG) Log.d("msg", "323232");
                    } else {

                        if(Config.SHOWLOG) Log.d("msg", "2222");

            /*
             * merge only those local msg whose creation time is greater than last cretedAt global
             * time
             */

                        msgs = mergeAllOnDate(msgs, localMsgList, false);
                    }
                } else
                    if(Config.SHOWLOG) Log.d("msg", "0000 local - null");
            }
        }

    /*
     * for(int i =0; i<msgs.size(); i++) { if(msgs.get(i).getCreatedAt()!= null)
     * if(Config.SHOWLOG) Log.d("localMessage", msgs.get(i).getString("title") + "  -- "+ msgs.get(i).getCreatedAt() );
     * else if(Config.SHOWLOG) Log.d("localMessage", msgs.get(i).getString("title") + "  -- "+
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
            if(Config.SHOWLOG) Log.d("DBG_QUERIES_SERVER_MSGS", "timestamp null with SIGNUP mode. Using user's creation time as timestamp");
            newTimeStamp = user.getCreatedAt();
        }

        //newTimeStamp has been appropriately set handling cases
        //if newTimeStamp is null this means that (no local messages + login mode) and we're fetching old+new messages
        //if newTimeStamp is NOT null, fetch all new messages with timestamp > newTimeStamp

        if(newTimeStamp == null){
            if(Config.SHOWLOG) Log.d("DBG_QUERIES_SERVER_MSGS", "timestamp null. So no messages stored. Fetching first batch of messages");
            HashMap<String, Object> parameters = new HashMap<String, Object>();

            parameters.put("limit", Config.firstTimeInboxFetchCount);
            parameters.put("classtype", "j");
            try {
                HashMap<String, Object > resultMap = ParseCloud.callFunction("showLatestMessagesWithLimit2", parameters);
                List<ParseObject> allMessages = (List<ParseObject>) resultMap.get("message");
                Map<String, List<Boolean>> allStates = (Map<String, List<Boolean>>) resultMap.get("states");

                //since old messages, need to update their MessageState(like_status, confused_status)
                if(allMessages != null) {

                    //if size is less than expected, then set flag in shared prefs
                    if(allMessages.size() < Config.firstTimeInboxFetchCount){
                        String key = userId + Constants.SharedPrefsKeys.SERVER_INBOX_FETCHED;
                        if(Config.SHOWLOG) Log.d("_FETCH_OLD", "getServerInboxMsgs() : setting shared prefs _server_inbox_fetched");
                        SessionManager.setBooleanValue(key, true);
                    }

                    if(Config.SHOWLOG) Log.d("DBG_QUERIES_SERVER_MSGS", "[limit] : fetched msgs=" + allMessages.size() + ", states=" + allStates.size());
                    //use allStates to set appropriate state for the each received message

                    for(int m=0; m < allMessages.size(); m++){
                        ParseObject msg = allMessages.get(m);
                        List<Boolean> msgState = allStates.get(msg.getObjectId());

                        if(msgState != null){
                            if(Config.SHOWLOG) Log.d("DBG_QUERIES_SERVER_MSGS", "[limit] : msg state for " + msg.getObjectId() + " l=" + msgState.get(0) + ", c=" + msgState.get(1));

                            msg.put(Constants.GroupDetails.LIKE, msgState.get(0));
                            msg.put(Constants.GroupDetails.CONFUSING, msgState.get(1));
                            msg.put(Constants.GroupDetails.SYNCED_LIKE, msgState.get(0));
                            msg.put(Constants.GroupDetails.SYNCED_CONFUSING, msgState.get(1));
                        }
                        else{
                            //default
                            msg.put(Constants.GroupDetails.LIKE, false);
                            msg.put(Constants.GroupDetails.CONFUSING, false);
                            msg.put(Constants.GroupDetails.SYNCED_LIKE, false);
                            msg.put(Constants.GroupDetails.SYNCED_CONFUSING, false);
                        }
                        msg.put(Constants.USER_ID, userId);
                        msg.put(Constants.GroupDetails.DIRTY_BIT, false);
                        msg.put(Constants.GroupDetails.SEEN_STATUS, 0); // we assume that if msg downloaded, then must have seen
                    }

                    if(Config.SHOWLOG) Log.d("DBG_QUERIES_SERVER_MSGS", "[limit] pinning all together");
                    ParseObject.pinAll(allMessages); //pin all the messages
                    msgList.addAll(0, allMessages);
                }
            }
            catch (ParseException e){
                Utility.LogoutUtility.checkAndHandleInvalidSession(e);
                e.printStackTrace();
            }
        }
        else{
            if(Config.SHOWLOG) Log.d("DBG_QUERIES_SERVER_MSGS", "fetch messages greater than newTimeStamp");
            //fetch messages greater than newTimeStamp
            HashMap<String, Date> parameters = new HashMap<String, Date>();

            parameters.put("date", newTimeStamp);

            try {
                //just fetch, set default state(like, confused = false, false)
                List<ParseObject> allMessages= ParseCloud.callFunction("showLatestMessages", parameters);
                if(allMessages != null) {
                    if(Config.SHOWLOG) Log.d("DBG_QUERIES_SERVER_MSGS", "[time] fetched " + allMessages.size());
                    for(int i=0; i<allMessages.size(); i++){
                        ParseObject msg = allMessages.get(i);
                        msg.put(Constants.GroupDetails.LIKE, false);
                        msg.put(Constants.GroupDetails.CONFUSING, false);
                        msg.put(Constants.GroupDetails.SYNCED_LIKE, false);
                        msg.put(Constants.GroupDetails.SYNCED_CONFUSING, false);

                        msg.put(Constants.USER_ID, userId);
                        msg.put(Constants.GroupDetails.DIRTY_BIT, false);
                        msg.put(Constants.GroupDetails.SEEN_STATUS, 0); // we assume that if msg downloaded, then must have seen
                    }
                    ParseObject.pinAll(allMessages); //pin all the messages
                    msgList.addAll(0, allMessages); //in the beginning so that [newMessages ... followed by ... original_msgList]
                }
            }
            catch (ParseException e){
                Utility.LogoutUtility.checkAndHandleInvalidSession(e);
                e.printStackTrace();
            }
        }

        return msgList;
    }

    //fetch extra old messages from server
    //Returns old messages. Fetches Config.oldMessagesPagingSize messages. If less than that, then caller should set the flag
    public static List<ParseObject> getOldServerInboxMsgs() {
        ParseUser user = ParseUser.getCurrentUser();
        if(user == null || user.getUsername() == null){
            return null;
        }

        String userId = user.getUsername();

        ParseQuery oldestInboxMsgQuery = new ParseQuery(Constants.GroupDetails.TABLE);
        oldestInboxMsgQuery.fromLocalDatastore();
        oldestInboxMsgQuery.orderByAscending(Constants.TIMESTAMP);

        ParseObject oldestMsg = null;
        try{
            oldestMsg = oldestInboxMsgQuery.getFirst();
        }
        catch (ParseException e){
            e.printStackTrace();
        }

        if(oldestMsg == null || oldestMsg.getCreatedAt() == null){
            return null;
        }

        Date oldestTimeStamp = oldestMsg.getCreatedAt();

        if(Config.SHOWLOG) Log.d("_FETCH_OLD", "entered input all correct. Now calling cloud fuction : showOldMessages");

        HashMap<String, Object> parameters = new HashMap<String, Object>();

        parameters.put("limit", Config.oldMessagesPagingSize);
        parameters.put("classtype", "j");
        parameters.put("date", oldestTimeStamp);

        try {
            HashMap<String, Object > resultMap = ParseCloud.callFunction("showOldMessages2", parameters);
            List<ParseObject> allMessages = (List<ParseObject>) resultMap.get("message");
            Map<String, List<Boolean>> allStates = (Map<String, List<Boolean>>) resultMap.get("states");


            //since old messages, need to update their MessageState(like_status, confused_status)
            if(allMessages != null) {
                if(Config.SHOWLOG) Log.d("_FETCH_OLD", "fetched msgs=" + allMessages.size() + " fetched states=" + allStates.size());

                //if size is less than expected, then set flag in shared prefs
                if(allMessages.size() < Config.oldMessagesPagingSize){
                    String key = userId + Constants.SharedPrefsKeys.SERVER_INBOX_FETCHED;
                    if(Config.SHOWLOG) Log.d("_FETCH_OLD", "setting shared prefs _server_inbox_fetched");
                    SessionManager.setBooleanValue(key, true);
                }

                //use allStates to set appropriate state for the each received message
                for(int m=0; m < allMessages.size(); m++){
                    ParseObject msg = allMessages.get(m);
                    List<Boolean> msgState = allStates.get(msg.getObjectId());

                    if(msgState != null){
                        if(Config.SHOWLOG) Log.d("_FETCH_OLD", "msg state for " + msg.getObjectId() + " l=" + msgState.get(0) + ", c=" + msgState.get(1));

                        msg.put(Constants.GroupDetails.LIKE, msgState.get(0));
                        msg.put(Constants.GroupDetails.CONFUSING, msgState.get(1));
                        msg.put(Constants.GroupDetails.SYNCED_LIKE, msgState.get(0));
                        msg.put(Constants.GroupDetails.SYNCED_CONFUSING, msgState.get(1));
                    }
                    else{
                        //default
                        msg.put(Constants.GroupDetails.LIKE, false);
                        msg.put(Constants.GroupDetails.CONFUSING, false);
                        msg.put(Constants.GroupDetails.SYNCED_LIKE, false);
                        msg.put(Constants.GroupDetails.SYNCED_CONFUSING, false);
                    }

                    msg.put(Constants.USER_ID, userId);
                    msg.put(Constants.GroupDetails.DIRTY_BIT, false);
                    msg.put(Constants.GroupDetails.SEEN_STATUS, 0); // we assume that if msg downloaded, then must have seen
                }

                ParseObject.pinAll(allMessages); //pin all the messages
                return allMessages; //non null
            }
        }
        catch (ParseException e){
            Utility.LogoutUtility.checkAndHandleInvalidSession(e);
            e.printStackTrace();
        }
        return null; //when cloud function fails or returns null
    }

    /*
        returns true if success (i.e atleast 1 new message added)
                false otherwise, in that case caller should update totalInboxMessages = item count
     */
    public boolean getExtraLocalInboxMsgs(List<ParseObject> msgs) throws ParseException {

        if (msgs == null)
            return false;

        if (msgs.size() > 0 && msgs.get(msgs.size() - 1) != null) {

            if (msgs.get(msgs.size() - 1).getCreatedAt() != null)
                oldTimeStamp = msgs.get(msgs.size() - 1).getCreatedAt();
            else
                oldTimeStamp = msgs.get(msgs.size() - 1).getDate("creationTime");
            // To make infinite inbox , remove above line and join a new class :P
        }

        ParseQuery<ParseObject> query = ParseQuery.getQuery(Constants.GroupDetails.TABLE);
        query.fromLocalDatastore();
        query.orderByDescending(Constants.TIMESTAMP);
        query.whereEqualTo("userId", userId);
        query.setLimit(messageCount);

        if (oldTimeStamp != null)
            query.whereLessThan(Constants.TIMESTAMP, oldTimeStamp);

        List<ParseObject> msgList1 = query.find();

        // appending extra objects to the end of list
        if (msgList1 != null && msgList1.size() > 0) {
            msgs.addAll(msgList1);
            return true;
        }

        return false; //error
    }

    // **********************CREATE CLASS MESSAGES QUERY******************


    public List<ParseObject> getLocalCreateMsgs(String groupCode, List<ParseObject> groupDetails,
                                                boolean flag) throws ParseException {

        Date oldTime = null;

        if (groupDetails != null && groupDetails.size() > 0 && flag /*important as append to older list only if flag is set*/) {
            int lastItem = groupDetails.size()-1;
            if (groupDetails.get(lastItem).get("creationTime") != null)
                oldTime = (Date) groupDetails.get(lastItem).get("creationTime");
        } else
            groupDetails = new ArrayList<ParseObject>();


        List<ParseObject> pendingMessages = new ArrayList<>();
        if(!flag) {//since getting first batch of messagess(not on scroll), so get all pending messages first and show them as latest

            ParseQuery<ParseObject> pendingQuery = ParseQuery.getQuery(Constants.SENT_MESSAGES_TABLE);
            pendingQuery.fromLocalDatastore();
            pendingQuery.orderByDescending("creationTime");
            pendingQuery.whereEqualTo("userId", userId);
            pendingQuery.whereEqualTo("pending", true);
            pendingQuery.whereEqualTo("code", groupCode);
            pendingQuery.setLimit(Config.outboxMsgCount);

            pendingMessages = pendingQuery.find();
        }

        ParseQuery<ParseObject> query = ParseQuery.getQuery(Constants.SENT_MESSAGES_TABLE);
        query.fromLocalDatastore();
        query.orderByDescending("creationTime");
        query.whereEqualTo("userId", userId);
        query.whereEqualTo("code", groupCode);
        query.whereNotEqualTo("pending", true);

        if (flag)
            query.setLimit(2 * createMsgCount);
        else
            query.setLimit(createMsgCount);

        if (oldTime != null)
            query.whereLessThan("creationTime", oldTime);

        List<ParseObject> msgList1 = query.find();

        // appending extra objects to the end of list

        groupDetails.addAll(pendingMessages);
        groupDetails.addAll(msgList1);

        return groupDetails;
    }

    // ****************************CREATE CLASSS*************************************

    public boolean checkClassNameExist(String className) {
        ParseUser user = ParseUser.getCurrentUser();

        if (user != null) {
            List<List<String>> createdGroups = new ArrayList<List<String>>();
            createdGroups = user.getList(Constants.CREATED_GROUPS);
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
        ParseQuery<ParseObject> query = ParseQuery.getQuery(Constants.GROUP_MEMBERS);
        query.fromLocalDatastore();
        query.whereEqualTo("userId", userId);
        query.whereEqualTo("code", groupCode);
        query.whereEqualTo("status", null);
        query.setLimit(1000); //set upper limit


        try {
            List<ParseObject> appMembers = query.find();

            if (appMembers != null && appMembers.size() > 0) {
                for (int i = 0; i < appMembers.size(); i++) {
                    ParseObject members = appMembers.get(i);

                 /*   if(members.getString("status") != null)
                        if(Config.SHOWLOG) Log.d("STATUS",members.getObjectId() + "-" + members.getString("status") +  "-");
                    else
                        if(Config.SHOWLOG) Log.d("STATUS", members.getObjectId() + "  :  null status");*/


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
        smsQuery.setLimit(1000);

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
        ParseQuery<ParseObject> query1 = ParseQuery.getQuery(Constants.GROUP_MEMBERS);
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

    /*
        The ultimate optimization to avoid Codegroup queries. This takes only 1 s.
        Fill the Application.globalCodegroupMap on start of app with all the codegroups
        (irrespective of whether they belong to that user or not - no filter makes it even faster)

        Currently being called in MemberList asynctask
     */
    public static void fillCodegroupMap() {
        if(Config.SHOWLOG) Log.d("__MG", "fillCodegroupMap : begin");

        if(Application.globalCodegroupMap == null){
            return;
        }

        ParseQuery query = ParseQuery.getQuery(Constants.Codegroup.TABLE);
        query.fromLocalDatastore();
        try{
            List<ParseObject> codegroupList = query.find();
            if(codegroupList != null){
                for(ParseObject codegroup : codegroupList){
                    Application.globalCodegroupMap.put(codegroup.getString(Constants.Codegroup.CODE), codegroup);
                }
            }
        }
        catch (ParseException e){
            e.printStackTrace();
        }
        if(Config.SHOWLOG) Log.d("__MG", "fillCodegroupMap : end,  mapsize=" + Application.globalCodegroupMap.size());
    }

    /*
        Optimization : Use this whenever you need codegroup object.
        If it is present in Application.globalCodegroupMap then we can avoid query in Codegroup table
     */
    public static ParseObject getCodegroupObject(String groupCode){
        if(groupCode == null){
            return null;
        }

        if(Application.globalCodegroupMap != null && Application.globalCodegroupMap.get(groupCode) != null){
            if(Config.SHOWLOG) Log.d("__M", "JoinedGroups getView codegroup " + groupCode + " found in map");
            return Application.globalCodegroupMap.get(groupCode);
        }

        //not found in map, now query Codegroup table
        ParseQuery query = ParseQuery.getQuery(Constants.Codegroup.TABLE);
        query.fromLocalDatastore();
        query.whereEqualTo(Constants.Codegroup.CODE, groupCode);

        try{
            ParseObject codegroup = query.getFirst();
            if(codegroup != null && Application.globalCodegroupMap != null){
                Application.globalCodegroupMap.put(groupCode, codegroup);
                if(Config.SHOWLOG) Log.d("__M", "JoinedGroups getView codegroup " + groupCode + " queried and put in map");
            }
            return codegroup;
        }
        catch (ParseException e){
            e.printStackTrace();
        }

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

    public static List<ParseObject> getLocalOutbox() {
        ParseUser user = ParseUser.getCurrentUser();
        if(user == null){
            Utility.LogoutUtility.logout();
            return new ArrayList<>();
        }

        String userId = user.getUsername();

        ParseQuery<ParseObject> pendingQuery = ParseQuery.getQuery(Constants.SENT_MESSAGES_TABLE);
        pendingQuery.fromLocalDatastore();
        pendingQuery.orderByDescending("creationTime");
        pendingQuery.whereEqualTo("userId", userId);
        pendingQuery.whereEqualTo("pending", true);
        pendingQuery.setLimit(Config.outboxMsgCount);

        List<ParseObject> pendingMessages = null;
        try {
            pendingMessages = pendingQuery.find();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        ParseQuery<ParseObject> sentQuery = ParseQuery.getQuery(Constants.SENT_MESSAGES_TABLE);
        sentQuery.fromLocalDatastore();
        sentQuery.orderByDescending("creationTime");
        sentQuery.whereEqualTo("userId", userId);
        sentQuery.whereNotEqualTo("pending", true); //not equal to
        sentQuery.setLimit(Config.outboxMsgCount);

        List<ParseObject> sentMessages = null;
        try {
            sentMessages = sentQuery.find();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        if(pendingMessages == null || sentMessages == null){//though this won't happen
            return new ArrayList<>();
        }

        //append sentMessages at end of pendingMessages
        pendingMessages.addAll(sentMessages);
        return pendingMessages;
    }

    public List<ParseObject> getExtraLocalOutbox(List<ParseObject> msgs) throws ParseException {

        if (msgs == null)
            return null;
        Date lastTimeStamp = null;

        if (msgs.size() > 0 && msgs.get(msgs.size() - 1) != null) {
            lastTimeStamp = msgs.get(msgs.size() - 1).getDate("creationTime");
        }

        ParseQuery<ParseObject> query = ParseQuery.getQuery(Constants.SENT_MESSAGES_TABLE);
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
