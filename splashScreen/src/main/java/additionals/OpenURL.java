package additionals;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.MenuItem;
import android.webkit.WebView;

import library.UtilString;
import trumplab.textslate.R;
import trumplabs.schoolapp.MainActivity;

/**
 * Created by dhanesh on 19/1/15.
 */
public class OpenURL extends ActionBarActivity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.open_url);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        WebView webView = (WebView) findViewById(R.id.webView);

        /*Uri uri = Uri.parse("market://details?id=" + getPackageName());
        Intent myAppLinkToMarket = new Intent(Intent.ACTION_VIEW, uri);

        if (Utility.isInternetOn(this)) {

            try {
                startActivity(myAppLinkToMarket);
            } catch (ActivityNotFoundException e) {
            }
        } else {
            Utility.toast("Check your Internet Connection.");

            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        }*/

        if(getIntent().getExtras() != null) {
            String url = getIntent().getExtras().getString("URL");
            Log.d("OPEN_URL", "url not null...");

            if(!UtilString.isBlank(url))
            {
                Log.d("OPEN_URL", "url not null..." + url);

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
        startActivity(intent);
    }

}
