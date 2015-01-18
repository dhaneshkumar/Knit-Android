package additionals;

import java.util.HashMap;
import java.util.Map;

import trumplab.textslate.R;
import trumplabs.schoolapp.ClassMsg;
import utility.Utility;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import baseclasses.MyActionBarActivity;

import com.parse.ParseAnalytics;

public class ShareInstructions extends MyActionBarActivity{

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.share_instructions);
    
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    final String grpCode = ClassMsg.groupCode;
    final String link = "www.knitapp.co.in/user.html?" + grpCode;
    TextView shareView = (TextView) findViewById(R.id.share);
    TextView copyView = (TextView) findViewById(R.id.copy);
    TextView classLink = (TextView) findViewById(R.id.classLink);
    
    Map<String, String> dimensions = new HashMap<String, String>();
    dimensions.put("page", "sharing");
    dimensions.put("dayType", "weekday");
    // Send the dimensions to Parse along with the 'read' event
  
    ParseAnalytics.trackEvent("page", dimensions);
    
    classLink.setText(link);
    
    
    shareView.setOnClickListener(new View.OnClickListener() {
      
      @Override
      public void onClick(View v) {
        Intent i=new Intent(android.content.Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(android.content.Intent.EXTRA_SUBJECT,"Knit");
        i.putExtra(android.content.Intent.EXTRA_TEXT, link);
        startActivity(Intent.createChooser(i,"Share via"));
      }
    });
    
    
    copyView.setOnClickListener(new View.OnClickListener() {
      
      @Override
      public void onClick(View v) {
        Utility.copyToClipBoard(ShareInstructions.this, "URL",
            link);
        
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
