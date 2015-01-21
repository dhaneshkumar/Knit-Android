package BackGroundProcesses;

import android.os.AsyncTask;
import android.util.Log;

import com.parse.ParseException;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import trumplabs.schoolapp.Application;
import trumplabs.schoolapp.Constants;
import trumplabs.schoolapp.Outbox;
import utility.SessionManager;


public class Refresher {
    ParseUser freshUser;

    public Refresher(int appOpeningCount) {

        freshUser = ParseUser.getCurrentUser();

        if (freshUser != null) {


            final SessionManager sm = new SessionManager(Application.getAppContext());
      /*
       * Storing current time stamp
       */
            boolean test = freshUser.getBoolean("test");
            test = !test;
            freshUser.put("test", true);
            freshUser.saveInBackground(new SaveCallback() {

                @Override
                public void done(ParseException e) {
                    Date currentDate = freshUser.getUpdatedAt();
                    sm.setCurrentTime(currentDate);
                }
            });


            Log.d("splashScreen", "ccc");
            freshUser.fetchInBackground();

            if (appOpeningCount > 0) {

        /*
         * Updating joined group list
         */
                JoinedClassRooms joinClass = new JoinedClassRooms();
                joinClass.execute();

        /*
         * Updating inbox msgs
         */

                Inbox newInboxMsg = new Inbox(null);
                newInboxMsg.execute();

       /*
        *   Updating counts for outbox messages
        *
        */
                Outbox.refreshCountInBackground();

        /*
            Update total count of outbox messages
         */
                Outbox.updateOutboxTotalMessages();

        /*
         * Updating created class rooms list
         */

                CreatedClassRooms createdClassList = new CreatedClassRooms();
                createdClassList.execute();


                /*
                If its new user then refresh on evry app openingtime,
                After 50 opening count, refresh this suggestion list on interval of 10 opening counts
                 */
                if (appOpeningCount <= 50) {
                    UpdateSuggestions updateSuggestions = new UpdateSuggestions();
                    updateSuggestions.execute();
                } else {
                    if (appOpeningCount % 10 == 0) {
                        UpdateSuggestions updateSuggestions = new UpdateSuggestions();
                        updateSuggestions.execute();
                    }
                }


                //Checking for correct channels (It should match to joined groups entries)
                ParseInstallation installation = ParseInstallation.getCurrentInstallation();
                List<String> channels = installation.getList("channels");
                if (channels != null) {
                    List<String> channelList = new ArrayList<String>();
                    List<List<String>> joinedGroups;
                    joinedGroups = freshUser.getList("joined_groups");

                    if (joinedGroups != null) {
                        for (int i = 0; i < joinedGroups.size(); i++) {
                            channelList.add(joinedGroups.get(i).get(0));
                        }

                        if (channelList.size() != channels.size()) {
                            installation.put("channels", channelList);

                            installation.saveEventually();
                        }

                    }
                }
                else {
                        updateChannels();
                    }


                //Checking for username in parseinstallation entry

                if(installation.getString("username") == null) {
                    installation.put("username", freshUser.getUsername());
                    installation.saveEventually();
                }


            } else {

                sm.setAppOpeningCount();
        /*
         * Updating joined group list
         */
                JoinedClassRooms joinClass = new JoinedClassRooms(true);
                joinClass.execute();
            }

            //Refresh local outbox data, if not in valid state clear and fetch new.
            //If already present then no need to fetch outbox messages
            if(freshUser.getString("role").equalsIgnoreCase("teacher")) {
                if(sm.getOutboxLocalState(freshUser.getUsername())==0) {
                    Log.d("DEBUG_REFRESHER", "fetching outbox messages for the first and last time in a thread");
                    Runnable r = new Runnable() {
                        @Override
                        public void run() {
                            Log.d("DEBUG_REFRESHER", "running fetchOutboxMessages");
                            OutboxMsgFetch.fetchOutboxMessages();
                        }
                    };

                    Thread t = new Thread(r);
                    t.setPriority(Thread.MIN_PRIORITY);
                    t.start();
                }
                else{
                    Log.d("DEBUG_REFRESHER", "local outbox data intact. No need to fetch anything");
                }
            }


            if (appOpeningCount % utility.Config.faqRefreshingcount == 0) {
                getServerFAQs faqs = new getServerFAQs(freshUser.getString("role"));
                faqs.execute();


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

                ParseInstallation installation = ParseInstallation.getCurrentInstallation();

                if (installation.getString("username") == null) {
                    installation.put("username", freshUser.getUsername());
                    try {
                        installation.save();
                    } catch (ParseException e1) {
                    }
                }
            }

        } else {
            SessionManager session = new SessionManager(Application.getAppContext());
            session.reSetAppOpeningCount();
        }

    }



    /*
     * getting faq from server
     */
    class getServerFAQs extends AsyncTask<Void, Void, String[]> {

        private String role;


        public getServerFAQs(String role) {
            this.role = role;
        }

        @Override
        protected String[] doInBackground(Void... params) {
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

            return null;
        }


    }



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
