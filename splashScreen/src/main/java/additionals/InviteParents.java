
package additionals;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.text.Html;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import baseclasses.MyActionBarActivity;
import joinclasses.JoinClassDialog;
import trumplab.textslate.R;
import trumplabs.schoolapp.ClassMsg;
import utility.Utility;

public class InviteParents extends MyActionBarActivity{
    private TextView textTV;
    private ImageView imageView;
    private String classCode;
    private String className;

  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.invite_parent_newgui);

      final TextView smsTV = (TextView) findViewById(R.id.smsTV);
      final TextView androidTV = (TextView) findViewById(R.id.androidTV);
      TextView recommendedTv = (TextView) findViewById(R.id.recommended);
      LinearLayout whatsapp = (LinearLayout) findViewById(R.id.whatsApp);
      LinearLayout sms = (LinearLayout) findViewById(R.id.sms);
      LinearLayout gmail = (LinearLayout) findViewById(R.id.gmail);
      LinearLayout copy = (LinearLayout) findViewById(R.id.copy);
      textTV = (TextView) findViewById(R.id.content);
      imageView = (ImageView) findViewById(R.id.imageContent);

      if(getIntent().getExtras()!= null) {
          classCode = getIntent().getExtras().getString("classCode");
          className = getIntent().getExtras().getString("className");
      }

      getSupportActionBar().setDisplayHomeAsUpEnabled(true);
      getSupportActionBar().setTitle("Invite Parents");

      final String smsContent = "To subscribe via SMS, send " +
              " <font color='#000000'> "+classCode+" &lt;SPACE&gt; name</font>" +" to "+
              " <font color='#000000'>9243000080 </font>";

      final String androidContent = "Install "+
              " <font color='#000000'>Knit messaging </font>" +
              " form playstore and enter the "+ classCode+" to join.";

      textTV.setText(Html.fromHtml(smsContent), TextView.BufferType.SPANNABLE);



      //click on sms textview icon
      smsTV.setOnClickListener(new OnClickListener() {
          @Override
          public void onClick(View v) {

              //Highlighting sms textview
              smsTV.setTextColor(Color.parseColor("#505050"));
              int sdk = android.os.Build.VERSION.SDK_INT;
              if(sdk < android.os.Build.VERSION_CODES.JELLY_BEAN) {
                  smsTV.setBackgroundDrawable( getResources().getDrawable(R.drawable.grey_light_box) );
                  androidTV.setBackgroundDrawable( getResources().getDrawable(R.drawable.grey_light_boundry) );
                  imageView.setBackgroundDrawable( getResources().getDrawable(R.drawable.sms_mobile) );
              } else {
                  smsTV.setBackground( getResources().getDrawable(R.drawable.grey_light_box));
                  androidTV.setBackground( getResources().getDrawable(R.drawable.grey_light_boundry));
                  imageView.setBackground( getResources().getDrawable(R.drawable.sms_mobile));
              }
              smsTV.setTypeface(Typeface.DEFAULT_BOLD);

              //remove highlight from android tv
              androidTV.setTextColor(Color.parseColor("#808284"));
              androidTV.setTypeface(Typeface.DEFAULT);

              //setting header content
              textTV.setText(Html.fromHtml(smsContent), TextView.BufferType.SPANNABLE);
          }
      });


      //click on android textview icon
      androidTV.setOnClickListener(new OnClickListener() {
          @Override
          public void onClick(View v) {

              //Highlighting sms textview
              androidTV.setTextColor(Color.parseColor("#505050"));
              int sdk = android.os.Build.VERSION.SDK_INT;
              if(sdk < android.os.Build.VERSION_CODES.JELLY_BEAN) {
                  androidTV.setBackgroundDrawable( getResources().getDrawable(R.drawable.grey_light_box) );
                  smsTV.setBackgroundDrawable( getResources().getDrawable(R.drawable.grey_light_boundry) );
                  imageView.setBackgroundDrawable( getResources().getDrawable(R.drawable.android_mobile) );
              } else {
                  androidTV.setBackground( getResources().getDrawable(R.drawable.grey_light_box));
                  smsTV.setBackground( getResources().getDrawable(R.drawable.grey_light_boundry));
                  imageView.setBackground( getResources().getDrawable(R.drawable.android_mobile));
              }
              androidTV.setTypeface(Typeface.DEFAULT_BOLD);

              //remove highlight from android tv

              smsTV.setTextColor(Color.parseColor("#808284"));
              smsTV.setTypeface(Typeface.DEFAULT);

              //setting header content
              textTV.setText(Html.fromHtml(androidContent), TextView.BufferType.SPANNABLE);

          }
      });

      final String sharingContent = "Hey, Use Knit";


      //share via whatsapp
      whatsapp.setOnClickListener(new OnClickListener() {
          @Override
          public void onClick(View v) {
              PackageManager pm = getPackageManager();
              try {
                  pm.getPackageInfo("com.whatsapp", PackageManager.GET_ACTIVITIES);
                  Intent sendIntent = new Intent(Intent.ACTION_SEND);
                  sendIntent.setPackage("com.whatsapp");
                  sendIntent.setType("text/plain");


                  sendIntent.putExtra(Intent.EXTRA_TEXT, sharingContent);
                  startActivity(sendIntent);

              } catch (PackageManager.NameNotFoundException e) {
                  e.printStackTrace();
                  Utility.toast("WhatsApp not installed !");
              }
          }
      });


      //share via sms
      sms.setOnClickListener(new OnClickListener() {
          @Override
          public void onClick(View v) {

              Intent sendIntent = new Intent(Intent.ACTION_VIEW);
              sendIntent.addCategory(Intent.CATEGORY_DEFAULT);
              sendIntent.setType("vnd.android-dir/mms-sms");
              sendIntent.setData(Uri.parse("sms:"));
              sendIntent.putExtra("sms_body", sharingContent);
              startActivity(sendIntent);
          }
      });


      //share via gmail
      gmail.setOnClickListener(new OnClickListener() {
          @Override
          public void onClick(View v) {
              Intent sendIntent = new Intent(Intent.ACTION_VIEW);
              sendIntent.setType("plain/text");
             // sendIntent.setData(Uri.parse("test@gmail.com"));
              sendIntent.setClassName("com.google.android.gm", "com.google.android.gm.ComposeActivityGmail");
            //  sendIntent.putExtra(Intent.EXTRA_EMAIL, new String[] { "test@gmail.com" });
              sendIntent.putExtra(Intent.EXTRA_SUBJECT, "Invitation to use Knit");
              sendIntent.putExtra(Intent.EXTRA_TEXT, sharingContent);
              startActivity(sendIntent);
          }
      });

    //copy content
    copy.setOnClickListener(new OnClickListener() {
        @Override
        public void onClick(View v) {
            Utility.copyToClipBoard(InviteParents.this, "Instructions", sharingContent);
        }
    });

      recommendedTv.setOnClickListener(new OnClickListener() {
          @Override
          public void onClick(View v) {
              FragmentManager fm = getSupportFragmentManager();
              RecommendationDialog recommendationDialog = new RecommendationDialog();
              Bundle args = new Bundle();
              args.putString("classCode", classCode);
              args.putString("className", className);

              recommendationDialog.setArguments(args);
              recommendationDialog.show(fm, "Join Class");
          }
      });

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


}
