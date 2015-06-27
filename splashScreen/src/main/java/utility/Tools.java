package utility;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Point;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.view.Display;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ProgressBar;

import java.lang.reflect.InvocationTargetException;

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
    h.sendMessageDelayed(new Message(), seconds * 1000);

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

  //call only when api >= 14
  public static Point getNavigationBarSize(Context context) {
    if (Build.VERSION.SDK_INT < 14){
      return new Point(0, 0);
    }

    Point appUsableSize = getAppUsableScreenSize(context);
    Point realScreenSize = getRealScreenSize(context);

    // navigation bar on the right
    if (appUsableSize.x < realScreenSize.x) {
      return new Point(realScreenSize.x - appUsableSize.x, appUsableSize.y);
    }

    // navigation bar at the bottom
    if (appUsableSize.y < realScreenSize.y) {
      return new Point(appUsableSize.x, realScreenSize.y - appUsableSize.y);
    }

    // navigation bar is not present
    return new Point();
  }

  @TargetApi(14)
  public static Point getAppUsableScreenSize(Context context) {
    WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    Display display = windowManager.getDefaultDisplay();
    Point size = new Point();
    display.getSize(size);
    return size;
  }

  @TargetApi(14)
  public static Point getRealScreenSize(Context context) {
    WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    Display display = windowManager.getDefaultDisplay();
    Point size = new Point();

    if (Build.VERSION.SDK_INT >= 17) {
      display.getRealSize(size);
    } else if (Build.VERSION.SDK_INT >= 14) {
      try {
        size.x = (Integer) Display.class.getMethod("getRawWidth").invoke(display);
        size.y = (Integer) Display.class.getMethod("getRawHeight").invoke(display);
      } catch (IllegalAccessException e) {}
      catch (InvocationTargetException e) {} catch (NoSuchMethodException e) {}
    }

    return size;
  }

}
