package additionals;

import android.app.SearchManager;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SearchView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import library.UtilString;
import trumplab.textslate.R;

/**
 * Created by dhanesh on 1/6/15.
 */
public class InviteParentViaPhonebook extends ActionBarActivity{
    private List<List<String>> contactList;
    private ListView contactListview;
    private BaseAdapter contactAdapter;
    private SearchView searchView;
    private List<List<String>> initialContactList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.contacts_list_view);

        contactListview = (ListView) findViewById(R.id.contact_list);


        fetchPhoneList();

        contactAdapter = new ContactAdapter();
        contactListview.setAdapter(contactAdapter);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onPause(){
        super.onPause();

        //TaskInviteParentViaPhonebook taskInviteParentViaPhonebook = new TaskInviteParentViaPhonebook();
        //taskInviteParentViaPhonebook.execute();
    }

    private void fetchPhoneList()
    {
        if(initialContactList == null)
            initialContactList = new ArrayList<List<String>>();

        Cursor phones = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,null,null, null);
        while (phones.moveToNext())
        {
            String name=phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
            String phoneNumber = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
            //Toast.makeText(getApplicationContext(),name, Toast.LENGTH_LONG).show();


            //changing first letter to caps
            name = UtilString.changeFirstToCaps(name);

            List<String> phoneList = new ArrayList<>();
            phoneList.add(name);
            phoneList.add(phoneNumber);

            initialContactList.add(phoneList);
        }
        phones.close();

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
            fetchPhoneList();

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


            final TextView name = (TextView) row.findViewById(R.id.person_name);
            final TextView phone = (TextView) row.findViewById(R.id.person_number);
            final TextView invite = (TextView) row.findViewById(R.id.invite);

            List<String> contact = contactList.get(position);
            if(contact != null && contact.size()==2)
            {
                //name.setText(contact.get(0));
                name.setText(Html.fromHtml(contact.get(0)), TextView.BufferType.SPANNABLE);
                phone.setText(contact.get(1));
            }

            invite.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View view) {
                    invite.setText("invited");
                    String contactName = name.getText().toString();
                    String contactPhone = phone.getText().toString();
                }
            });

            return row;
        }
    }
}
