package additionals;

import android.app.SearchManager;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SearchView;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.parse.ParseAnalytics;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import BackGroundProcesses.InviteTasks;
import library.UtilString;
import trumplab.textslate.R;
import trumplabs.schoolapp.Constants;
import utility.Utility;

/**
 * Created by dhanesh on 1/6/15.
 */
public class InviteVia extends ActionBarActivity{
    static String LOGTAG = "DEBUG_INVITE_PHONE";

    private int inviteType = -1;
    private String classCode = "";
    private String inviteMode = "";

    private List<Contact> contactList;
    private ListView contactListview;
    private BaseAdapter contactAdapter;
    private List<Contact> initialContactList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.contacts_list_view);

        contactListview = (ListView) findViewById(R.id.contact_list);

        if(getIntent()!= null && getIntent().getExtras() != null)
        {
            Bundle bundle = getIntent().getExtras();
            if(bundle.getInt("inviteType", -1000) != -1000){
                inviteType = bundle.getInt("inviteType");
            }

            if(!UtilString.isBlank(bundle.getString("inviteMode"))) {
                inviteMode = bundle.getString("inviteMode");
            }

            if(!UtilString.isBlank(bundle.getString("classCode"))) {
                classCode = bundle.getString("classCode");
            }
        }

        fetchContacts();

        contactAdapter = new ContactAdapter();
        contactListview.setAdapter(contactAdapter);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if(inviteType > 0) {
            //track this event
            Map<String, String> dimensions = new HashMap<String, String>();
            dimensions.put("Invite Type", "type" + Integer.toString(inviteType));
            dimensions.put("Invite Mode", inviteMode);
            ParseAnalytics.trackEventInBackground("inviteMode", dimensions);
            Log.d(LOGTAG, "tracking inviteMode type=" + inviteType + ", mode=" + inviteMode);
        }
    }

    @Override
    public void onPause(){
        super.onPause();

        //create a thread to send the invitations of this type
        Runnable r = new Runnable() {
            @Override
            public void run(){
                if(inviteType == Constants.INVITATION_T2P || inviteType == Constants.INVITATION_P2P){
                    InviteTasks.sendInvitePhonebook(inviteType, inviteMode, classCode);
                }
                else {
                    InviteTasks.sendInvitePhonebook(inviteType, inviteMode, "");
                }
            }
        };

        Thread t = new Thread(r);
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }

    private void fetchContacts(){
        //first fill initialContactList with the numbers/emails
        if(inviteMode.equals(Constants.MODE_PHONE)){
            fillPhoneNumbers();
        }
        else { //fill it with emails
            fillEmails();
        }

        //Now sorting contacts in alphabetical order
        Collections.sort(initialContactList, new Comparator() {
            @Override
            public int compare(Object o1, Object o2) {
                Contact c1 = (Contact) o1;
                Contact c2 = (Contact) o2;
                String s1 = (String) c1.name; //won't be null
                String s2 = (String) c2.name;
                return s1.compareTo(s2);
            }
        });

        //get the Invitation list from database
        List<ParseObject> invitationList = null;
        ParseQuery<ParseObject> invitationQuery = ParseQuery.getQuery(Constants.INVITATION);
        invitationQuery.fromLocalDatastore();
        invitationQuery.whereEqualTo(Constants.TYPE, inviteType);
        invitationQuery.whereEqualTo(Constants.MODE, inviteMode);

        if(inviteType == Constants.INVITATION_T2P){
            invitationQuery.whereEqualTo(Constants.CLASS_CODE, classCode);
        }

        invitationQuery.addAscendingOrder(Constants.NAME); //order by name

        try{
            invitationList = invitationQuery.find();
            Log.d(LOGTAG, "invitation list of size " + invitationList.size());
        }
        catch (ParseException e){
            e.printStackTrace();
        }

        //Now merge the two lists : initialContactList and invitationList. Set invitation field of Contact objects
        //both are sorted by the name fields

        merge(initialContactList, invitationList);

        contactList = initialContactList;
    }

    private void fillPhoneNumbers()
    {
        initialContactList = new ArrayList<>();

        Cursor cursor = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
        while (cursor.moveToNext())
        {
            String name=cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
            String phoneNumber = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));

            if(name == null || phoneNumber == null) continue;
            //changing first letter to caps
            name = UtilString.changeFirstToCaps(name);

            Contact c = new Contact(name, phoneNumber, null);
            initialContactList.add(c);
        }
        cursor.close();
    }

    private void fillEmails(){
        initialContactList = new ArrayList<>();

        ContentResolver cr = getContentResolver();
        String[] PROJECTION = new String[] { ContactsContract.RawContacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Email.DATA,
        };

        String filter = ContactsContract.CommonDataKinds.Email.DATA + " NOT LIKE ''"; //email data not empty
        Cursor cursor = cr.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, PROJECTION, filter, null, null);

        while (cursor.moveToNext()) {
            //to get the contact names
            String name=cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
            String email = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA));

            if(name == null || email == null) continue;

            name = UtilString.changeFirstToCaps(name);
            //Log.d(LOGTAG, name + "--" + email);

            //check name is not an email address
            Pattern p = Pattern.compile(".+@.+\\.[a-z]+");
            Matcher m = p.matcher(name);
            boolean matchFound = m.matches();
            if (!matchFound) {
                Contact c = new Contact(name, email, null);
                initialContactList.add(c);
            }
        }
        cursor.close();
    }

    void merge(List<Contact> contactList, List<ParseObject> invitationList){
        //both have been sorted on basis of phone number
        if(contactList == null || invitationList == null){
            return;
        }

        int M = contactList.size();
        int N = invitationList.size();

        int i = 0, j = 0;
        while(i < M && j < N){
            String n1 = contactList.get(i).name; //non-null
            String n2 = invitationList.get(j).getString(Constants.NAME);
            if(n2 == null) {
                j++;
                continue;
            }
            int result = n1.compareTo(n2);
            if(result == 0){
                contactList.get(i).invitation = invitationList.get(j);
                i++;
                j++;
            }
            else if(result < 0){
                i++; //lhs is smaller
            }
            else{
                j++; //rhs is smaller
            }
        }
    }

    private void searchResult(String query)
    {
        if(contactList == null)
            fetchContacts();

        if(UtilString.isBlank(query))
        {
            contactList = initialContactList;
            //reset the displayName field
            for(Contact contact : contactList){
                contact.displayName = contact.name;
            }
        }
        else {
            contactList = new ArrayList<>();

            for(int i=0; i<initialContactList.size(); i++)
            {
                int index = (initialContactList.get(i).name.toLowerCase()).indexOf(query.toLowerCase());

                if(index > -1)
                {
                    Contact contact = initialContactList.get(i);

                    String displayName = contact.name;
                    displayName = displayName.substring(0, index) +
                    "<font color='#29B6F6'>" + displayName.substring(index, index + query.length())  + "</font>"
                            + displayName.substring(index + query.length()) ;

                    contact.displayName = displayName;

                    contactList.add(contact);
                }
            }
        }

        contactAdapter.notifyDataSetChanged();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.phonebook_menu, menu);

        // Associate searchable configuration with the SearchView
        SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        final SearchView searchView =
                (SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setSearchableInfo(
                searchManager.getSearchableInfo(getComponentName()));

        searchView.setFocusable(true);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                searchResult(newText);
                return true;
            }
        });

        return true;
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


    class ContactAdapter extends BaseAdapter {

        @Override
        public int getCount() {

            if (contactList == null)
                contactList = new ArrayList<>();

            return contactList.size();
        }

        @Override
        public Object getItem(int position) {
            return contactList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View row = inflater.inflate(R.layout.contacts_list_item, parent, false);


            final TextView name = (TextView) row.findViewById(R.id.person_name);
            final TextView phone = (TextView) row.findViewById(R.id.person_number);
            final TextView invite = (TextView) row.findViewById(R.id.invite);
            final LinearLayout resend = (LinearLayout) row.findViewById(R.id.resend);

            final Contact contact = contactList.get(position);
            if(contact != null && contact.displayName != null && contact.number != null)
            {
                name.setText(Html.fromHtml(contact.displayName), TextView.BufferType.SPANNABLE);
                phone.setText(contact.number);
                if(contact.invitation == null){
                    invite.setVisibility(View.VISIBLE);
                    resend.setVisibility(View.GONE);
                }
                else{
                    invite.setVisibility(View.GONE);
                    resend.setVisibility(View.VISIBLE);
                }
            }

            invite.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View view) {
                    invite.setVisibility(View.GONE);
                    resend.setVisibility(View.VISIBLE);
                    if(contact.invitation == null){
                        ParseObject invitation = new ParseObject(Constants.INVITATION);
                        invitation.put(Constants.RECEIVER, contact.number);
                        invitation.put(Constants.RECEIVER_NAME, contact.name);
                        invitation.put(Constants.TYPE, inviteType);
                        invitation.put(Constants.PENDING, true);
                        invitation.put(Constants.MODE, inviteMode);
                        if(inviteType == Constants.INVITATION_T2P) {
                            invitation.put(Constants.CLASS_CODE, classCode);
                        }
                        //Log.d(LOGTAG, Utility.parseObjectToJson(invitation).toString());
                        contact.invitation = invitation;
                        try{
                            invitation.pin();
                            Log.d(LOGTAG, "new invitation created");

                            if(inviteType > 0) {
                                //track event
                                Map<String, String> dimensions = new HashMap<String, String>();
                                dimensions.put("Invite Type", "type" + Integer.toString(inviteType));
                                dimensions.put("Invite Mode", inviteMode);
                                ParseAnalytics.trackEventInBackground("invitedUsersCount", dimensions);
                                Log.d(LOGTAG, "tracking invitedUsersCount type=" + inviteType + ", mode=" + inviteMode);
                            }
                        }
                        catch (ParseException e){
                            e.printStackTrace();
                        }
                    }
                }
            });

            resend.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View view) {
                    if(contact.invitation != null){
                        contact.invitation.put(Constants.PENDING, true);
                        try{
                            contact.invitation.pin();
                            Log.d(LOGTAG, "resent invitation updated");
                            Utility.toast("invitation sent !");
                        }
                        catch (ParseException e){
                            e.printStackTrace();
                        }
                    }
                }
            });

            return row;
        }
    }

    class Contact{
        String name;
        String displayName; //how the name is displayed, for e.g when text search hightlight
        String number; //phone number or email
        ParseObject invitation;
        Contact(String n, String num, ParseObject inv){
            name = n;
            displayName = name;
            number = num;
            invitation = inv;
        }
    }
}
