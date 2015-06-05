package additionals;

import android.app.SearchManager;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.provider.ContactsContract;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import library.UtilString;
import trumplab.textslate.R;
import trumplabs.schoolapp.Constants;

/**
 * Created by dhanesh on 1/6/15.
 */
public class InviteParentViaEmail extends ActionBarActivity{
    static String LOGTAG = "DEBUG_INVITE_EMAIL";

    private int inviteType = -1;
    private String classCode = "";

    private List<List<String>> contactList;
    private ListView contactListview;
    private BaseAdapter contactAdapter;
    private SearchView searchView;
    private List<List<String>> initialContactList;
    private LinearLayout progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.contacts_list_view);

        contactListview = (ListView) findViewById(R.id.contact_list);
        progressBar = (LinearLayout) findViewById(R.id.contact_progress);

        progressBar.setVisibility(View.VISIBLE);

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

        loadEmails();

        contactAdapter = new ContactAdapter();
        contactListview.setAdapter(contactAdapter);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if(inviteType > 0) {
            //track this event
            Map<String, String> dimensions = new HashMap<>();
            dimensions.put("Invite Type", "type" + Integer.toString(inviteType));
            dimensions.put("Invite Mode", Constants.MODE_EMAIL);
            ParseAnalytics.trackEventInBackground("inviteMode", dimensions);
            Log.d(LOGTAG, "tracking inviteMode type=" + inviteType + ", mode=" + Constants.MODE_EMAIL);
        }
    }

    private void fetch(){
        if(initialContactList == null)
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
            Log.d(LOGTAG, name +   "--"+email);

            //check name is not an email address
            Pattern p = Pattern.compile(".+@.+\\.[a-z]+");
            Matcher m = p.matcher(name);
            boolean matchFound = m.matches();
            if (!matchFound) {
                List<String> phoneList = new ArrayList<>();
                phoneList.add(name);
                phoneList.add(email);
                initialContactList.add(phoneList);
            }
        }
        cursor.close();

        //Sorting contacts in alphabetical order
        Collections.sort(initialContactList, new Comparator() {
            @Override
            public int compare(Object o1, Object o2) {
                List<String> list1 = (ArrayList<String>) o1;
                List<String> list2 = (ArrayList<String>) o2;
                String s1 = (String) list1.get(0);
                String s2 = (String) list2.get(0);
                return s1.compareTo(s2);
            }
        });

        contactList = initialContactList;
    }

    private void fetchEmailList()
    {

        if(initialContactList == null)
            initialContactList = new ArrayList<List<String>>();


        ContentResolver cr = getContentResolver();
        Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI,null, null, null, null);
        if (cur.getCount() > 0) {
            while (cur.moveToNext()) {
                String id = cur.getString(cur.getColumnIndex(ContactsContract.Contacts._ID));
                Cursor cur1 = cr.query(
                        ContactsContract.CommonDataKinds.Email.CONTENT_URI, null,
                        ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
                        new String[]{id}, null);
                while (cur1.moveToNext()) {
                    //to get the contact names
                    String name=cur1.getString(cur1.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                    String number=cur1.getString(cur1.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    String email = cur1.getString(cur1.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA));
                    Log.d("Email", name + " -- "+ number+"  -- "+email);


                    //check name is not an email address
                    Pattern p = Pattern.compile(".+@.+\\.[a-z]+");
                    Matcher m = p.matcher(name);
                    boolean matchFound = m.matches();
                    if (!matchFound) {
                        List<String> phoneList = new ArrayList<>();
                        phoneList.add(name);
                        phoneList.add(email);
                        initialContactList.add(phoneList);
                    }
                }
                cur1.close();
            }
        }


        //Sorting contacts in alphabetical order
        Collections.sort(initialContactList, new Comparator() {
            @Override
            public int compare(Object o1, Object o2) {
                List<String> list1 = (ArrayList<String>) o1;
                List<String> list2 = (ArrayList<String>) o2;
                String s1 = (String) list1.get(0);
                String s2 = (String) list2.get(0);
                return s1.compareTo(s2);
            }
        });

        contactList = initialContactList;


    }



    private void searchResult(String query)
    {
        if(contactList == null)
        {
            progressBar.setVisibility(View.VISIBLE);

            loadEmails();
        }

        if(UtilString.isBlank(query))
        {
            contactList = initialContactList;

        }
        else {
            contactList = new ArrayList<>();

            for(int i=0; i<initialContactList.size(); i++)
            {
                //  if(initialContactList.get(i).get(0).toLowerCase().contains(query.toLowerCase())) {

                int index = (initialContactList.get(i).get(0).toLowerCase()).indexOf( query.toLowerCase());

                if(index > -1)
                {

                    String oldname = initialContactList.get(i).get(0);

                    oldname = oldname.substring(0, index) +
                            "<font color='#29B6F6'>" + oldname.substring(index, index + query.length())  + "</font>"
                            + oldname.substring(index + query.length()) ;

                    List<String> list = new ArrayList<>();
                    list.add(oldname);
                    list.add(initialContactList.get(i).get(1));

                    contactList.add(list);
                }


                // }
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
                contactList = new ArrayList<List<String>>();

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


            TextView name = (TextView) row.findViewById(R.id.person_name);
            TextView phone = (TextView) row.findViewById(R.id.person_number);
            TextView invite = (TextView) row.findViewById(R.id.invite);

            List<String> contact = contactList.get(position);
            if(contact != null && contact.size()==2)
            {
                //name.setText(contact.get(0));
                name.setText(Html.fromHtml(contact.get(0)), TextView.BufferType.SPANNABLE);
                phone.setText(contact.get(1));
            }

            return row;
        }
    }

    private void loadEmails()
    {
        final Handler mHandler = new Handler();
        new Thread(new Runnable(){
            @Override
            public void run () {
                fetch();
                mHandler.post(new Runnable() {
                    @Override
                    public void run () {
                        progressBar.setVisibility(View.GONE);
                    }
                });
            }
        }).start();
    }
}
