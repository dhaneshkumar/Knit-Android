package utility;

import trumplab.textslate.R;
import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;


public class Popup {
  int textHeight;
  int popupWidth=300;
  
  public  void showPopup(final Activity context, final Point p, boolean flag, final int offset, String txt, int height, final int iconPadding, int width) {
    
    popupWidth = width;
    // Inflate the popup_layout.xml
    RelativeLayout viewGroup = (RelativeLayout) context.findViewById(R.id.popup_element);
    LayoutInflater layoutInflater = (LayoutInflater) context
      .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    final View layout = layoutInflater.inflate(R.layout.suggestion_popup, viewGroup);
    
  
    // Creating the PopupWindow
    final PopupWindow popup = new PopupWindow(context);
    popup.setContentView(layout);
    popup.setWidth(popupWidth);
    popup.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
    popup.setFocusable(true);
    popup.setAnimationStyle(android.R.anim.fade_in);
  
    // Clear the default translucent background
    popup.setBackgroundDrawable(new BitmapDrawable());
    
   
  
  
    
    
    // Getting a reference to Close button, and close the popup when clicked.
    ImageView upIcon = (ImageView) layout.findViewById(R.id.popup_up);
    final ImageView downIcon = (ImageView) layout.findViewById(R.id.popup_down);
    final TextView popupText = (TextView) layout.findViewById(R.id.popup_text);
    final RelativeLayout suggestion_layout = (RelativeLayout) layout.findViewById(R.id.popup_element);
    
    popupText.setText(txt);
    
    
    if(flag)
    {
        upIcon.setVisibility(View.VISIBLE);
         downIcon.setVisibility(View.GONE);
     // Displaying the popup at the specified location, + offsets.
        popup.showAtLocation(layout, Gravity.NO_GRAVITY, p.x+offset , p.y +height-18 );
        upIcon.setPadding(0, 0, 20+iconPadding,0);
    }
    else
    {
        upIcon.setVisibility(View.GONE);
         downIcon.setVisibility(View.VISIBLE);
     // Displaying the popup at the specified location, + offsets.
      
         popup.showAtLocation(layout, Gravity.NO_GRAVITY, p.x , p.y);
        
       ViewTreeObserver vto = suggestion_layout.getViewTreeObserver();
      vto.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
          public boolean onPreDraw() {
             suggestion_layout.getViewTreeObserver().removeOnPreDrawListener(this);
              textHeight = suggestion_layout.getMeasuredHeight();
              
           //  Toast.makeText(context, textHeight + " : height", Toast.LENGTH_SHORT).show();
           
            
            
          // popupText.setVisibility(View.GONE);
             popup.update(p.x+offset, p.y-textHeight+18,popupWidth, textHeight,true);
             
            downIcon.setPadding(0, 0, 20+iconPadding,0);
              return true;
          }
      });
      
        
       
     
    }
    
    
   
    
 }

 

}
