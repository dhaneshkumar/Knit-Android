package utility;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ProgressBar;

import fr.castorflex.android.smoothprogressbar.SmoothProgressBar;

public class Tools {
  /*
   * hide keyboard
   */

  public static void hideKeyboard(Activity currentActiviry) {
    InputMethodManager inputManager =
        (InputMethodManager) currentActiviry.getSystemService(Context.INPUT_METHOD_SERVICE);

    inputManager.hideSoftInputFromWindow(currentActiviry.getCurrentFocus().getWindowToken(),
        InputMethodManager.HIDE_NOT_ALWAYS);

    currentActiviry.getWindow()
        .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
  }

  /*
   * make visible smooth progress bar for certain interval
   */
  public static void runSmoothProgressBar(final SmoothProgressBar progressBar, final int seconds) {

    if(progressBar == null)
      return ;

    progressBar.setVisibility(View.VISIBLE);
    progressBar.setIndeterminate(true);
    
    /*
     * stop refreshing progress bar
     */
    final Handler h = new Handler() {
      @Override
      public void handleMessage(Message message) {
        progressBar.setVisibility(View.GONE);
      }
    };
    h.sendMessageDelayed(new Message(), seconds*1000);

  }

    /*
   * make visible  progress bar for certain interval
   */
  public static void runProgressBar(final ProgressBar progressBar, final int seconds) {

    if(progressBar == null)
      return ;

    progressBar.setVisibility(View.VISIBLE);
    progressBar.setIndeterminate(true);
    
    /*
     * stop refreshing progress bar after some interval
     */
    final Handler h = new Handler() {
      @Override
      public void handleMessage(Message message) {
        progressBar.setVisibility(View.GONE);
      }
    };
    h.sendMessageDelayed(new Message(), seconds * 1000);

  }

}
