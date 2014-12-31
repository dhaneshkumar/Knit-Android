package additionals;

import trumplab.textslate.R;
import android.os.Bundle;
import android.view.MenuItem;
import baseclasses.MyActionBarActivity;

public class Instructions extends MyActionBarActivity 
{

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.instructions);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    
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
