package BackGroundProcesses;

import android.os.AsyncTask;
import android.util.Log;

import com.parse.ParseException;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import trumplabs.schoolapp.Application;
import trumplabs.schoolapp.Constants;
import trumplabs.schoolapp.Outbox;
import utility.Queries2;
import utility.SessionManager;
import utility.Utility;


public class Refresher {
    ParseUser freshUser;

    public Refresher(int appOpeningCount) {
        Log.d("DEBUG_REFRESHER", "Entering Refresher Thread");
        /* just trying to see if sleeping blocks ui
        try{
            Thread.sleep(1*Constants.MINUTE_MILLISEC);
        }
        catch (InterruptedException e){
            e.printStackTrace();
        }*/

        freshUser = ParseUser.getCurrentUser();

        if (freshUser != null) {

            final SessionManager sm = new SessionManager(Application.getAppContext());
            Utility.checkParseInstallation(); //important for upgrade issues. This will be called first time app is launched after update
      /*
       * Storing current time stamp
       */
            Utility.updateCurrentTimeInBackground(freshUser);


            freshUser.fetchInBackground();

                Log.d("DEBUG_REFRESHER",  "calling background tasks");



        /*
         * Updating inbox msgs
         */
            Log.d("DEBUG_REFRESHER", "calling Inbox execute()");

            Inbox newInboxMsg = new Inbox(null);
            newInboxMsg.doInBackgroundCore();
            newInboxMsg.onPostExecuteHelper(); //done
            newInboxMsg.syncOtherInboxDetails();

           /*
            *   Updating counts for outbox messages
            *
            */
            Outbox.refreshCountCore(); //simple function

            /*
                Update total count of outbox messages
             */
            Outbox.updateOutboxTotalMessages(); //simple function

            /*
             * Updating created class rooms list
             */

            CreatedClassRooms createdClassList = new CreatedClassRooms();
            createdClassList.doInBackgroundCore();
            createdClassList.onPostExecuteCoreHelper(); //done

             /*
             * Updating joined classes teacher details(name, profile pic)
             */
            final JoinedClassRooms joinClass = new JoinedClassRooms();
            joinClass.doInBackgroundCore();
            joinClass.onPostExecuteHelper();

            /*
            //Checking for correct channels (It should match to joined groups entries)
            ParseInstallation installation = ParseInstallation.getCurrentInstallation();

            //update channels NOT REQUIRED Now ParseInstallation updates handled in server
            updateChannels();

            //Checking for username in parseinstallation entry

            if(installation.getString("username") == null) {
                installation.put("username", freshUser.getUsername());
                installation.saveEventually();
            }*/

            //Refresh local outbox data, if not in valid state clear and fetch new.
            //If already present then no need to fetch outbox messages
            if(freshUser.getString("role").equalsIgnoreCase("teacher")) {
                if(sm.getOutboxLocalState(freshUser.getUsername())==0) {
                    Log.d("DEBUG_REFRESHER", "fetching outbox messages for the first and last time");
                    //no need to do in seperate thread. Already this is running in a background thread
                    OutboxMsgFetch.fetchOutboxMessages();
                }
                else{
                    Log.d("DEBUG_REFRESHER", "local outbox data intact. No need to fetch anything");
                }
            }

            //Fetch codegroup details if not yet fetched after reinstallation
            if(sm.getCodegroupLocalState(freshUser.getUsername()) == 0){
                Log.d("DEBUG_REFRESHER", "fetching Codegroup info for the first and last time");
                Queries2.fetchAllClassDetails();
            }
            else{
                Log.d("DEBUG_REFRESHER", "local Codegroup data intact. No need to fetch anything");
            }


            if (appOpeningCount % utility.Config.faqRefreshingcount == 0) {
                getServerFAQs faqs = new getServerFAQs(freshUser.getString("role"));
                faqs.doInBackgroundCore(); //no on-post-execute in this

                /*
                 * saving user's mobile model no.
                 */

                if (freshUser.getString("MODEL") == null && android.os.Build.MODEL != null) {
                    freshUser.put("MODEL", android.os.Build.MODEL);
                    try {
                        freshUser.save();
                    } catch (ParseException e1) {
                    }
                }


                /*if (installation.getString("username") == null) {
                    installation.put("username", freshUser.getUsername());
                    try {
                        installation.save();
                    } catch (ParseException e1) {
                    }
                }*/
            }

        } else {
            Log.d("DEBUG_REFRESHER", "User NULL");
            SessionManager session = new SessionManager(Application.getAppContext());
            session.reSetAppOpeningCount();
        }

        Log.d("DEBUG_REFRESHER", "Leaving Refresher Thread");
    }



    /*
     * getting faq from server
     */
    class getServerFAQs extends AsyncTask<Void, Void, String[]> {

        private String role;


        public getServerFAQs(String role) {
            this.role = role;
        }

        public void doInBackgroundCore(){
            ParseQuery<ParseObject> query = ParseQuery.getQuery("FAQs");
            query.orderByAscending(Constants.TIMESTAMP);

            if (role.equals("Parent"))
                query.whereEqualTo("role", role);

            List<ParseObject> faqs = null;
            try {
                faqs = query.find();
            } catch (ParseException e) {
            }

            if (faqs != null) {
                for (int i = 0; i < faqs.size(); i++) {
                    ParseObject faq = faqs.get(i);

                    if (ParseUser.getCurrentUser().getUsername() != null)
                        faq.put("userId", ParseUser.getCurrentUser().getUsername());

                    try {
                        faq.unpin();
                        faq.pin();
                    } catch (ParseException e1) {
                    }
                }
            }

            return;
        }

        @Override
        protected String[] doInBackground(Void... params) {
            doInBackgroundCore();
            return  null;
        }
    }



    //not used. All ParseInstallation updates handled in cloud code during signup/signin and logout
    private void updateChannels() {
        List<String> channelList = new ArrayList<String>();
        List<List<String>> joinedGroups;
        joinedGroups = freshUser.getList("joined_groups");

        if (joinedGroups != null) {
            for (int i = 0; i < joinedGroups.size(); i++) {
                channelList.add(joinedGroups.get(i).get(0));
            }

            ParseInstallation pi = ParseInstallation.getCurrentInstallation();

            if (pi != null && channelList.size() > 0) {
                pi.put("channels", channelList);
                if(pi.getString("username") == null)
                    pi.put("username", freshUser.getUsername());
                pi.saveEventually();
            }
        }
    }
}
