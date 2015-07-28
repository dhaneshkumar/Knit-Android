package additionals;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.webkit.WebView;

import baseclasses.MyActionBarActivity;
import library.UtilString;
import trumplab.textslate.R;
import trumplabs.schoolapp.MainActivity;
import utility.Config;

/**
 * Created by dhanesh on 19/1/15.
 */
public class OpenURL extends MyActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.open_url);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        WebView webView = (WebView) findViewById(R.id.webView);

        if(getIntent().getExtras() != null) {
            String url = getIntent().getExtras().getString("URL");
            if(Config.SHOWLOG) Log.d("OPEN_URL", "url not null...");

            if(!UtilString.isBlank(url))
            {
                if(Config.SHOWLOG) Log.d("OPEN_URL", "url not null..." + url);

                webView.getSettings().setJavaScriptEnabled(true);
                webView.loadUrl(url);
            }
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

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

}
