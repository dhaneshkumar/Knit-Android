package school;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;

import trumplabs.schoolapp.FeedBackClass;

/**
 * Created by dhanesh on 12/3/15.
 */
public class SchoolInputDialog extends DialogFragment {
    private Dialog dialog;


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("School Info");
        builder.setMessage("Please take a moment to tell in which school do you teach. Not only will it make your profile complete but also help parents search for your class");

        //button responses
        builder.setPositiveButton("Sure", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        dialog.dismiss();
                        Intent schoolIntent = new Intent(getActivity(), SchoolActivity.class);
                        startActivity(schoolIntent);
                    }});
        builder.setNegativeButton("Later", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();

                FeedBackClass feedBack = new FeedBackClass();
                FragmentManager fmr = getActivity().getSupportFragmentManager();
                feedBack.show(fmr, "FeedBackClass");
            }
        });

        dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();

        return dialog;
    }
}
