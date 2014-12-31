package joinclasses;


import library.UtilString;
import trumplab.textslate.R;
import utility.Popup;
import utility.Queries;
import utility.Tools;
import utility.Utility;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.parse.ParseInstallation;
import com.parse.ParseUser;

public class JoinClass extends Fragment {
  private EditText classCode;
  private String groupName = "";
  private LinearLayout joinLayout;
  private String code = "";
  private ParseInstallation pi;
  private Point p;
  private int height;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View layoutview = inflater.inflate(R.layout.joinclass_layout, container, false);
    return layoutview;
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    // retrieving userID
    ParseUser user = ParseUser.getCurrentUser();

    if (user == null)
      Utility.logout();

    user.getUsername();

    new Queries();
    Button join_btn = (Button) getActivity().findViewById(R.id.Join_btn);
    classCode = (EditText) getActivity().findViewById(R.id.classcodevalue);
    joinLayout = (LinearLayout) getActivity().findViewById(R.id.joinlayout);


    final ImageView help = (ImageView) getActivity().findViewById(R.id.help);

    // Get the x, y location and store it in the location[] array
    // location[0] = x, location[1] = y.


    ViewTreeObserver vto = help.getViewTreeObserver();
    vto.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
      @Override
      public void onGlobalLayout() {
        int[] location = new int[2];
        help.getLocationOnScreen(location);
        height = help.getHeight();

        // Initialize the Point with x, and y positions
        p = new Point();
        p.x = location[0];
        p.y = location[1];

      }
    });



    final String txt =
        "You need a class-code to join the class-room. If you don't have any, ask to teacher for it.";

    help.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {

        if (p != null) {
          Popup popup = new Popup();
          popup.showPopup(getActivity(), p, true, -300, txt, height, 15, 400);

          InputMethodManager inputMethodManager =
              (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
          // inputMethodManager.showSoftInput(viewToEdit, 0);
          if (getActivity().getCurrentFocus() != null) {
            inputMethodManager.hideSoftInputFromWindow(getActivity().getCurrentFocus()
                .getApplicationWindowToken(), 0);
          }
        }
      }
    });



    join_btn.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        if (!UtilString.isBlank(classCode.getText().toString())) {

          code = classCode.getText().toString().trim();

          if (code.length() != 5) {
            Utility.toast("Enter Correct Class Code");
            return;
          }


          // subscribe user to this group.
          Tools.hideKeyboard(getActivity());
          Utility.ls("ready to subscrible");
          if (Utility.isInternetOn(getActivity())) {

            Intent intent = new Intent(getActivity(), AddChildToClass.class);
            intent.putExtra("code", code);
            //startActivity(intent);
           
            getActivity().overridePendingTransition(R.anim.animation_leave, R.anim.animation_enter); 
            startActivityForResult(intent,0);
          } 
          else {
            Utility.toast("Check your Internet connection");
          }
        }
      }
    });
  }



  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.menu6, menu);
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);
  }
}
