package additionals;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.text.Html;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.LinearLayout;
import android.widget.TextView;

import trumplab.textslate.R;

/**
 * Dialog class to "How to join" dialog box
 */
public class HowToJoinDialog extends DialogFragment {
    private Dialog dialog;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        //creating new alertdialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final View view =
                getActivity().getLayoutInflater().inflate(R.layout.how_to_join_popup, null);
        builder.setView(view);
        dialog = builder.create();
        dialog.show();




        TextView wayToJoin = (TextView) view.findViewById(R.id.wayToJoin);
        TextView content = (TextView) view.findViewById(R.id.content);
        String appHeading = "Join class via APP";
        String smsHeading = "Join class via SMS";
        LinearLayout rootLayout = (LinearLayout) view.findViewById(R.id.root);


        //flag to tell whether user has sms or app
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


            final WebView webView = (WebView) view.findViewById(R.id.webView);

            Display display = ((WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

            if(flag.equals("SMS"))
            {
                wayToJoin.setText(smsHeading);
                webView.loadUrl("file:///android_asset/sms_gif.gif");
                content.setText(Html.fromHtml(smsContent), TextView.BufferType.SPANNABLE);

                //setting custom size of dialog box after loading screen
                view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        int width = view.getWidth(); //height is ready

                        Double val = new Double(width) / new Double(724);
                        val = val * 100d;
                        webView.setInitialScale(val.intValue());
                    }
                });
            }
            else
            {
                content.setText(Html.fromHtml(androidContent), TextView.BufferType.SPANNABLE);
                wayToJoin.setText(appHeading);
                webView.loadUrl("file:///android_asset/android_gif.gif");

                //setting custom size of dialog box after loading screen
                view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        int width = view.getWidth(); //height is ready

                        Double val = new Double(width) / new Double(501);
                        val = val * 100d;
                        webView.setInitialScale(val.intValue());
                    }
                });
            }
        }

        return dialog;
    }
}