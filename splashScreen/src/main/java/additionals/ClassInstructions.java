package additionals;

import trumplabs.schoolapp.ClassMsg;
import trumplab.textslate.R;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.TextView.BufferType;

import com.parse.ParseUser;

import baseclasses.MyActionBarActivity;
import trumplabs.schoolapp.Constants;

/**
 *  Contain instructions for teacher to invite parents
 */
public class ClassInstructions extends MyActionBarActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.class_instructions);

    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    final TextView androidCode = (TextView) findViewById(R.id.androidcode);
    final TextView smsCode = (TextView) findViewById(R.id.smsCode);
    
    final String grpCode = getIntent().getExtras().getString("grpCode");
    final String grpName = getIntent().getExtras().getString("grpName");
    
    

    String subString1 = "Download our free Knit app and enter ";
    String subString2 = " to join ";
   
    String andr = subString1 +
    		 " <font color='#0099cc'><b >"+grpCode+ "</b></font>" +
    		subString2 +
    		" <font color='#0099cc'><b >"+grpName+ "</b></font>" +".";
    
    androidCode.setText(Html.fromHtml(andr), TextView.BufferType.SPANNABLE);
    

    String role = ParseUser.getCurrentUser().getString(Constants.ROLE);
      String text = "";

      if(role.equals(Constants.STUDENT))
    {
        text = "Send" +
                " <font color='#0099cc'><b >"+grpCode+ " &lt;SPACE&gt; Your-Name </b></font>" +
                "&nbsp&nbsp to&nbsp&nbsp" +
                " <font color='#0099cc'><b >+91 9243000080</b> </font>" +
                " and receive reminders on your phone via SMS";

    }
     else
    {
        text = "Send" +
                " <font color='#0099cc'><b >"+grpCode+ " &lt;SPACE&gt; Child-Name </b></font>" +
                "&nbsp&nbsp to&nbsp&nbsp" +
                " <font color='#0099cc'><b >+91 9243000080</b> </font>" +
                " and receive reminders on your phone via SMS";
    }


    
        smsCode.setText(Html.fromHtml(text), TextView.BufferType.SPANNABLE);
   
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
