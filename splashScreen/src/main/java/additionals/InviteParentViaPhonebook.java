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

import BackGroundProcesses.InviteTasks;
import library.UtilString;
import trumplab.textslate.R;
import trumplabs.schoolapp.Constants;
import utility.Utility;

/**
 * Created by dhanesh on 1/6/15.
 */
public class InviteParentViaPhonebook extends ActionBarActivity{
    static String LOGTAG = "DEBUG_INVITE_PHONE";

    private int inviteType = -1;
    private String classCode = "";

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

            if(!UtilString.isBlank(bundle.getString("classCode"))) {
                classCode = bundle.getString("classCode");
            }
        }

        fetchPhoneList();

        contactAdapter = new ContactAdapter();
        contactListview.setAdapter(contactAdapter);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if(inviteType > 0) {
            //track this event
            Map<String, String> dimensions = new HashMap<String, String>();
            dimensions.put("Invite Type", "type" + Integer.toString(inviteType));
            dimensions.put("Invite Mode", Constants.MODE_PHONE);
            ParseAnalytics.trackEventInBackground("inviteMode", dimensions);
            Log.d(LOGTAG, "tracking inviteMode type=" + inviteType + ", mode=" + Constants.MODE_PHONE);
        }
    }

    @Override
    public void onPause(){
        super.onPause();

        //create a thread to send the invitations of this type
        Runnable r = new Runnable() {
            @Override
            public void run(){
                if(inviteType == Constants.INVITATION_T2P){
                    InviteTasks.sendInvitePhonebook(inviteType, classCode);
                }
                else {
                    InviteTasks.sendInvitePhonebook(inviteType, "");
                }
            }
        };

        Thread t = new Thread(r);
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }

    private void fetchPhoneList()
    {
        if(initialContactList == null)
            initialContactList = new ArrayList<>();

        Cursor phones = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
        while (phones.moveToNext())
        {
            String name=phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
            String phoneNumber = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));

            if(name == null || phoneNumber == null) continue;
            //Toast.makeText(getApplicationContext(),name, Toast.LENGTH_LONG).show();
            //changing first letter to caps
            name = UtilString.changeFirstToCaps(name);

            Contact c = new Contact(name, phoneNumber, null);
            initialContactList.add(c);
        }
        phones.close();

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
        invitationQuery.whereEqualTo(Constants.MODE, Constants.MODE_PHONE);

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
            fetchPhoneList();

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

              //  Utility.toast(s + "-------------");
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {

                // Clear SearchView
               // searchView.clearFocus();
            //    Utility.toast(newText + "  : submitted");

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
            if(contact != null && contact.displayName != null && contact.phoneNumber != null)
            {
                name.setText(Html.fromHtml(contact.displayName), TextView.BufferType.SPANNABLE);
                phone.setText(contact.phoneNumber);
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
                        invitation.put(Constants.RECEIVER, contact.phoneNumber);
                        invitation.put(Constants.RECEIVER_NAME, contact.name);
                        invitation.put(Constants.TYPE, inviteType);
                        invitation.put(Constants.PENDING, true);
                        invitation.put(Constants.MODE, Constants.MODE_PHONE);
                        if(inviteType == Constants.INVITATION_T2P) {
                            invitation.put(Constants.CLASS_CODE, classCode);
                        }
                        contact.invitation = invitation;
                        try{
                            invitation.pin();
                            Log.d(LOGTAG, "new invitation created");

                            if(inviteType > 0) {
                                //track event
                                Map<String, String> dimensions = new HashMap<String, String>();
                                dimensions.put("Invite Type", "type" + Integer.toString(inviteType));
                                dimensions.put("Invite Mode", Constants.MODE_PHONE);
                                ParseAnalytics.trackEventInBackground("invitedUsersCount", dimensions);
                                Log.d(LOGTAG, "tracking invitedUsersCount type=" + inviteType + ", mode=" + Constants.MODE_PHONE);
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
                            Utility.toast("duplicate invitation");
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
        String phoneNumber;
        ParseObject invitation;
        Contact(String n, String num, ParseObject inv){
            name = n;
            displayName = name;
            phoneNumber = num;
            invitation = inv;
        }
    }
}
