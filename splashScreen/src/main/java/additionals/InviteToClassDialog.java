package additionals;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

import com.github.amlcurran.showcaseview.ShowcaseView;

import trumplabs.schoolapp.Application;
import trumplabs.schoolapp.Constants;
import trumplabs.schoolapp.Messages;

/**
 * Created by dhanesh on 10/4/15.
 */
public class InviteToClassDialog extends DialogFragment {
    private Dialog dialog;

    public InviteToClassDialog(){
        ShowcaseView.isVisible = true;  //so that show case view is not shown now
        //fragment.setArguments
    }

    @Override
    public void onDismiss(final DialogInterface dialog) {
        super.onDismiss(dialog);
        ShowcaseView.isVisible = false;
        if(Messages.myadapter != null)
            Messages.myadapter.notifyDataSetChanged(); //so that response tutorial is shown now
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {

        final Bundle extras = getArguments();

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Invite others");
        builder.setMessage("Do you know anyone else from class '" +
                extras.getString("className")  + "'? Help them join this class by inviting them!");

        //button responses
        builder.setPositiveButton("Invite", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();

                //show the common Invite screen
                Intent intent = new Intent(Application.getAppContext(), Invite.class);
                intent.putExtra("classCode", extras.getString("classCode"));
                intent.putExtra("className", extras.getString("className"));
                intent.putExtra("inviteType", Constants.INVITATION_P2P);
                intent.putExtra("teacherName", extras.getString("teacherName"));
                startActivity(intent);
            }
        });
        builder.setNegativeButton("No, Thanks", new DialogInterface.OnClickListener() {
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
