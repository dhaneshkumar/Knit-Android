
package additionals;

import trumplab.textslate.R;
import trumplabs.schoolapp.ClassMsg;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import baseclasses.MyActionBarActivity;

public class InviteParents extends MyActionBarActivity{
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.invite_parents);
    
    LinearLayout classLayout = (LinearLayout) findViewById(R.id.classInstructions);
    LinearLayout shareLayout = (LinearLayout) findViewById(R.id.shareInstructions);
    
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    /*
     * on clicking class instruction button
     */
    classLayout.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        
        Intent intent = new Intent(getBaseContext(), ClassInstructions.class);
        intent.putExtra("grpCode", ClassMsg.groupCode);
        intent.putExtra("grpName", ClassMsg.grpName);
        startActivity(intent);
      }
    });
    
    /*
     * on clicking share instruction button
     */
    shareLayout.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        
        Intent intent = new Intent(getBaseContext(), ShareInstructions.class);
        startActivity(intent);
        
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
