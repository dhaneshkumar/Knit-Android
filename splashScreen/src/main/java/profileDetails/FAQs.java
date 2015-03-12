package profileDetails;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;

import baseclasses.MyActionBarActivity;
import trumplab.textslate.R;
import trumplabs.schoolapp.Constants;
import utility.Queries;
import utility.Utility;

public class FAQs extends MyActionBarActivity {

  private ListView faqListView;
  private BaseAdapter faqAdapter;
  private List<ParseObject> faqList;
  private LayoutInflater layoutinflater;
  private LinearLayout progressLayout;
  private LinearLayout editLayout;


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.profile_faq);
    layoutinflater = getLayoutInflater();

    faqListView = (ListView) findViewById(R.id.faqlistview);
    progressLayout = (LinearLayout) findViewById(R.id.progressBarLayout);
    editLayout = (LinearLayout) findViewById(R.id.editLayout);


    if (faqAdapter == null)
      faqAdapter = new myBaseAdapter();

    Queries query = new Queries();
    try {
      faqList = query.getLocalFAQs(ParseUser.getCurrentUser().getString("role"));


      if (faqList == null || faqList.size() == 0) {


          Utility utility = new Utility();
          if(utility.isInternetExist(this)){
          editLayout.setVisibility(View.GONE);
          progressLayout.setVisibility(View.VISIBLE);

          GetServerFaqs serverFaqs =
              new GetServerFaqs(ParseUser.getCurrentUser().getString("role"));
          serverFaqs.execute();
        }
      }

    } catch (ParseException e) {
    }

    if (faqList == null)
      faqList = new ArrayList<ParseObject>();

    faqListView.setAdapter(faqAdapter);

    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    getSupportActionBar().setTitle("FAQs");



    faqListView.setOnItemClickListener(new OnItemClickListener() {

      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
        View view1 = getLayoutInflater().inflate(R.layout.profile_faq_dialoge, null);
        builder.setView(view1);

        final TextView question = (TextView) view1.findViewById(R.id.question);
        final TextView answer = (TextView) view1.findViewById(R.id.answer);

        if (faqList.get(position) != null) {
          question.setText(faqList.get(position).getString("question"));
          answer.setText(faqList.get(position).getString("answer"));
        }

        final Dialog dialog = builder.create();
        dialog.show();
      }
    });
  }



  class myBaseAdapter extends BaseAdapter {

    @Override
    public int getCount() {
      if (faqList == null)
        faqList = new ArrayList<ParseObject>();

      return faqList.size();
    }

    @Override
    public Object getItem(int position) {

      return faqList.get(position);
    }

    @Override
    public long getItemId(int position) {
      return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
      View row = convertView;
      if (row == null) {
        row = layoutinflater.inflate(R.layout.profile_faq_item, parent, false);
      }
      TextView tView = (TextView) row.findViewById(R.id.faqItem);

      ParseObject faq = faqList.get(position);

      if (faq != null) {
        String question = faq.getString("question");
        String answer = faq.getString("answer");

        tView.setText(question);
      }

      return row;
    }



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

  class GetServerFaqs extends AsyncTask<Void, Void, Void> {

    private String role;


    public GetServerFaqs(String role) {
      this.role = role;
    }

    @Override
    protected Void doInBackground(Void... params) {
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

          faqList.add(faq);
          faq.put("userId", ParseUser.getCurrentUser().getUsername());

          try {
            faq.pin();
          } catch (ParseException e1) {
          }
        }
      }

      return null;
    }


    @Override
    protected void onPostExecute(Void result) {

      faqAdapter.notifyDataSetChanged();
      progressLayout.setVisibility(View.GONE);
      editLayout.setVisibility(View.VISIBLE);
    }

  }

}
