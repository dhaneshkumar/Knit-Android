package additionals;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.text.Html;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.TextView;

import trumplab.textslate.R;

public class HowToJoinDialog extends DialogFragment {
    private Dialog dialog;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        //creating new alertdialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View view =
                getActivity().getLayoutInflater().inflate(R.layout.how_to_join_popup, null);
        builder.setView(view);
        dialog = builder.create();
        dialog.show();




        TextView wayToJoin = (TextView) view.findViewById(R.id.wayToJoin);
        TextView content = (TextView) view.findViewById(R.id.content);
        String appHeading = "Join class via APP";
        String smsHeading = "Join class via SMS";


        //flag to tell whether user has signup or not
        if(getArguments() != null) {
            String flag = getArguments().getString("flag");
            String classCode = getArguments().getString("classCode");

            final String smsContent = "To subscribe via SMS, send <br>" +
                    " <font color='#4E4E4E'> "+classCode+" &lt;SPACE&gt; NAME</font>" +" <br> to "+
                    " <font color='#4E4E4E'>9243000080 </font>";

            final String androidContent = "Install "+
                    " <font color='#4E4E4E'>Knit messaging </font>" +
                    " from playstore and enter the class-code : <font color='#4E4E4E'>"+ classCode+"</font> & " +
                    "<font color='#4E4E4E'>" +   "student's name" +"</font> to join.";


            if(flag.equals("SMS"))
            {
                wayToJoin.setText(smsHeading);
                WebView webView = (WebView) view.findViewById(R.id.webView);
                webView.getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
                webView.loadUrl("file:///android_asset/sms_gif.gif");

                content.setText(Html.fromHtml(smsContent), TextView.BufferType.SPANNABLE);
            }
            else
            {
                content.setText(Html.fromHtml(androidContent), TextView.BufferType.SPANNABLE);

                wayToJoin.setText(appHeading);
                WebView webView = (WebView) view.findViewById(R.id.webView);
                webView.getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
                webView.loadUrl("file:///android_asset/android_gif.gif");
            }
        }

        return dialog;
    }
}