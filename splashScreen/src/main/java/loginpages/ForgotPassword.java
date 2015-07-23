package loginpages;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;

import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.RequestPasswordResetCallback;

import library.UtilString;
import trumplab.textslate.R;
import utility.Utility;

public class ForgotPassword extends DialogFragment {
  Dialog dialog;
  String email;

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    View view =
        getActivity().getLayoutInflater().inflate(R.layout.custom_forgotpassdialog_layout, null);
    builder.setView(view);
    dialog = builder.create();
    dialog.setCanceledOnTouchOutside(true);
    dialog.show();

    final EditText emailBox = (EditText) view.findViewById(R.id.emailid_forgot);
    TextView sendButton = (TextView) view.findViewById(R.id.button_forgotpass);

    sendButton.setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View v) {

        email = emailBox.getText().toString().trim();

        if (!UtilString.isBlank(email)) {
          ParseUser.requestPasswordResetInBackground(email, new UserForgotPasswordCallback());
        }
        dialog.dismiss();
      }
    });


    return dialog;
  }


  private class UserForgotPasswordCallback implements RequestPasswordResetCallback {
    public UserForgotPasswordCallback() {
      super();
    }

    @Override
    public void done(ParseException e) {
      if (e == null) {
        Utility.toast("Password Reset Link is sent to " + email);
      } else {
        Utility.toast("Failed to send link to " + email + ". Try Again..");
      }
    }

  }
}

