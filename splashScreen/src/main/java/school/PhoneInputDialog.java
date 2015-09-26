package school;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;

import trumplabs.schoolapp.FeedBackClass;

/**
 * Created by dhanesh on 12/3/15.
 */
public class PhoneInputDialog extends DialogFragment {
    private Dialog dialog;


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Phone Number");
        builder.setMessage("We need your phone number to verify your profile. Don't worry it will not be shared with anyone");

        //button responses
        builder.setPositiveButton("Sure", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        dialog.dismiss();
                        Intent phoneIntent = new Intent(getActivity(), PhoneInputActivity.class);
                        startActivity(phoneIntent);
                    }});

        builder.setNegativeButton("Later", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
            }
        });

        dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();

        return dialog;
    }
}
