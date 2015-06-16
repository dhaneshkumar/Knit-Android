package additionals;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.parse.ParseCloud;
import com.parse.ParseException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.HashMap;

import library.UtilString;
import trumplab.textslate.R;
import utility.Utility;

/**
 * Show dialog to send instructions to user
 */
public class RecommendationDialog extends DialogFragment {
    static final String LOGTAG = "DEBUG_RECO_DIALOG";
    private Dialog dialog;
    private LinearLayout progressLayout;
    private LinearLayout contentLayout;
    private String classCode;
    private String className;
    private String email;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View view =
                getActivity().getLayoutInflater().inflate(R.layout.recommendation_popup, null);
        builder.setView(view);
        dialog = builder.create();
        dialog.show();

        final TextView send = (TextView) view.findViewById(R.id.send);
        final EditText emailId = (EditText) view.findViewById(R.id.emailId);
        progressLayout = (LinearLayout) view.findViewById(R.id.progresslayout);
        contentLayout = (LinearLayout) view.findViewById(R.id.recommendedlayout);

        if(getArguments() != null) {
            classCode = getArguments().getString("classCode");
            className = getArguments().getString("className");
        }

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                email = emailId.getText().toString();

                if(UtilString.isBlank(email))
                    Utility.toast("Enter your Email-ID");
                else
                {
                    if(Utility.isInternetExist(getActivity())) {
                        progressLayout.setVisibility(View.VISIBLE);
                        contentLayout.setVisibility(View.GONE);

                        SendInstructions sendInstructions = new SendInstructions();
                        sendInstructions.execute();
                    }
                    else
                    {
                        Utility.toast("No Internet Connection");
                    }

                }
            }
        });


        return dialog;
    }



    class SendInstructions extends AsyncTask<Void, Void, Boolean>
    {

        @Override
        protected Boolean doInBackground(Void... params) {
            if((!UtilString.isBlank(classCode)) && (!UtilString.isBlank(className)) ) {
                Log.d(LOGTAG, "starting mailing......classCode=" + classCode + ", className="+className + ", email=" + email);

                try {
                    String urlString = "http://ec2-52-26-56-243.us-west-2.compute.amazonaws.com/createPdf.php?username=" + email + "&code=" + classCode;
                    Log.d(LOGTAG, "url is " + urlString);
                    URL url = new URL(urlString);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    InputStream in = new BufferedInputStream(conn.getInputStream());
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    StringBuilder total = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        total.append(line);
                    }
                    String response = total.toString();
                    Log.d(LOGTAG, "response is " + response);
                    return true;
                } catch (MalformedURLException e) {
                    Log.d(LOGTAG, "MalformedURLException");
                    e.printStackTrace();
                } catch (IOException e) {
                    Log.d(LOGTAG, "IOException");
                    e.printStackTrace();
                }

                /*HashMap<String, String> param = new HashMap<>();
                param.put("classCode", classCode);
                param.put("className", className);
                param.put("emailId", email);


                boolean result = false;
                try {
                    result = ParseCloud.callFunction("mailInstructions", param);

                    if(result)
                        return true;

                } catch (ParseException e) {
                    e.printStackTrace();
                    return false;
                }
                return false;
                */
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {

            if(aBoolean)
            {
                Utility.toast("Instructions sent on your email-id");
                dialog.dismiss();
            }
            else
            {
                Utility.toast("Sorry, instructions not sent on your email-id. Try Again");
                progressLayout.setVisibility(View.GONE);
                contentLayout.setVisibility(View.VISIBLE);
            }

            super.onPostExecute(aBoolean);
        }
    }


}
