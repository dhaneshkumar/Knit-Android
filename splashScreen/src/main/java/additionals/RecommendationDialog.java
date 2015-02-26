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
import com.parse.ParseUser;

import java.util.HashMap;

import library.UtilString;
import trumplab.textslate.R;
import utility.Utility;

/**
 * Show dialog to send instructions to user
 */
public class RecommendationDialog extends DialogFragment {
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
                    progressLayout.setVisibility(View.VISIBLE);
                    contentLayout.setVisibility(View.GONE);

                    SendInstructions sendInstructions = new SendInstructions();
                    sendInstructions.execute();

                }
            }
        });


        return dialog;
    }



    class SendInstructions extends AsyncTask<Void, Void, Boolean>
    {

        @Override
        protected Boolean doInBackground(Void... params) {


            Log.d("recom", "starting mailing......");
            Log.d("recom", classCode);
            Log.d("recom", className);
            Log.d("recom", email);

            if((!UtilString.isBlank(classCode)) && (!UtilString.isBlank(className)) )
            {
                HashMap<String, String> param = new HashMap<>();
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
