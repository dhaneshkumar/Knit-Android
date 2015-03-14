package additionals;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.View;

import trumplab.textslate.R;
import utility.Utility;

/**
 * Created by dhanesh on 12/3/15.
 */
public class RateAppDialog extends DialogFragment {
    private Dialog dialog;


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Rate Our App");
        builder.setMessage("If you enjoy using Knit, Please take a moment to rate the app.\n Thank you for support!");

        //button responses
        builder.setPositiveButton("Rate", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        dialog.dismiss();

                        Uri uri = Uri.parse("market://details?id=" + getActivity().getPackageName());
                        Intent myAppLinkToMarket = new Intent(Intent.ACTION_VIEW, uri);

                        try {
                            startActivity(myAppLinkToMarket);
                        } catch (ActivityNotFoundException e) {
                        }
                    }});
        builder.setNegativeButton("No, Thanks", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
            }
        });



        dialog = builder.create();
        dialog.show();

        return dialog;
    }
}
