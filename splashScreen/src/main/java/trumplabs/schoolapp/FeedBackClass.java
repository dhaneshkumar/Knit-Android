package trumplabs.schoolapp;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import trumplab.textslate.R;
import utility.Utility;

public class FeedBackClass extends DialogFragment {

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    View view = getActivity().getLayoutInflater().inflate(R.layout.feedback_layout, null);
    builder.setView(view);
    final Dialog dialog = builder.create();
    dialog.show();
    final EditText feedbackcontent = (EditText) view.findViewById(R.id.feedbackcontent);
    feedbackcontent.setOnFocusChangeListener(new View.OnFocusChangeListener() {
      @Override
      public void onFocusChange(View v, boolean hasFocus) {
        if (hasFocus) {
          dialog.getWindow().setSoftInputMode(
              WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
      }
    });

    TextView sendfb = (TextView) view.findViewById(R.id.sendfeedback);
    sendfb.setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View v) {
        String content = feedbackcontent.getText().toString();
        if (content.equals(""))
          Utility.toast("Please type some feedback");
        else {
          final ParseObject feedbacks = new ParseObject("Feedbacks");
          feedbacks.put("content", content);
          feedbacks.put("emailId", ParseUser.getCurrentUser().getEmail());
          feedbacks.saveInBackground(new SaveCallback() {

            @Override
            public void done(ParseException e) {
              if (e != null)
                feedbacks.saveEventually();
              Utility.toast("Thanks for the feedback! :)");
            }
          });
          getDialog().dismiss();
        }
      }
    });
    return dialog;
  }
}
