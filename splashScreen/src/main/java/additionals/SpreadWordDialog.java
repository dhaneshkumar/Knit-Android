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

import trumplabs.schoolapp.Constants;

/**
 * Created by dhanesh on 10/4/15.
 */
public class SpreadWordDialog extends DialogFragment {
    private Dialog dialog;


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Recommend Knit to Your Friends");
        builder.setMessage("If you like using Knit, recommend it to your friends and make their life much more easier.");

        //button responses
        builder.setPositiveButton("Recommend", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

                dialog.dismiss();

                Intent i = new Intent(android.content.Intent.ACTION_SEND);
                i.setType("text/plain");
                i.putExtra(android.content.Intent.EXTRA_SUBJECT, "Knit");
                i.putExtra(android.content.Intent.EXTRA_TEXT, Constants.spreadWordContent);
                startActivity(Intent.createChooser(i, "Share via"));
            }
        });
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
