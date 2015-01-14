package BackGroundProcesses;

import java.util.Date;
import java.util.List;

import trumplabs.schoolapp.Application;
import trumplabs.schoolapp.Constants;
import trumplabs.schoolapp.Outbox;
import utility.Queries;
import utility.SessionManager;
import utility.Utility;

import android.graphics.Bitmap.Config;
import android.os.AsyncTask;
import android.util.Log;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;


public class Refresher {
    ParseUser freshUser;

    public Refresher(int appOpeningCount) {

        freshUser = ParseUser.getCurrentUser();

        if (freshUser != null) {


      /*
       * Storing current time stamp
       */
            freshUser.put("test", true);
            freshUser.saveInBackground(new SaveCallback() {

                @Override
                public void done(ParseException e) {
                    Date currentDate = freshUser.getUpdatedAt();

                    SessionManager sm = new SessionManager(Application.getAppContext());
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

            } else {

                SessionManager sm = new SessionManager(Application.getAppContext());
                sm.setAppOpeningCount();

        /*
         * Updating joined group list
         */
                JoinedClassRooms joinClass = new JoinedClassRooms(true);
                joinClass.execute();

                if(freshUser.getString("role").equalsIgnoreCase("teacher")) {
                    Log.d("DEBUG_REFRESHER", "fetching outbox messages for the first and last time");
                    OutboxMsgFetch outboxMsgFetch = new OutboxMsgFetch();
                    outboxMsgFetch.execute();
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
}
