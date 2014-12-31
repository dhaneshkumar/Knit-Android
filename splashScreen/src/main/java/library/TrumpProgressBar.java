package library;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;


/*
 * Dynamic progressbar
 * 
 * @author : dhanesh
 */
public class TrumpProgressBar {
  private ProgressBar progressBar;
  RelativeLayout layout;
  RelativeLayout layout1;
  Context context;
  TextView tv;
  LayoutInflater inflater;
  View view;
  
  public TrumpProgressBar(Context context) {
    setup(context);
  }
  
  public TrumpProgressBar(Context context, String text) {
    setup(context);
    tv.setText(text);
    
  }
  
  public TrumpProgressBar(Context context, int resource)
  {
    setup(context);
    view = inflater.inflate(resource, null);
  }
  
  private void setup(Context context) {
    this.context = context;
    inflater = (LayoutInflater)context.getSystemService (Context.LAYOUT_INFLATER_SERVICE);
    layout = new RelativeLayout(context);
    layout1 = new RelativeLayout(context);
    progressBar = new ProgressBar(context,null,android.R.attr.progressBarStyleLargeInverse);
    progressBar.setIndeterminate(true);
    progressBar.setVisibility(View.VISIBLE);
    RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(80,80);
    RelativeLayout.LayoutParams tvparams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,RelativeLayout.LayoutParams.WRAP_CONTENT );
    //tvparams.addRule(RelativeLayout.BELOW, progressBar.getId());
    RelativeLayout.LayoutParams params1 = new RelativeLayout.LayoutParams(500,170);
    params.addRule(RelativeLayout.CENTER_IN_PARENT);
    params1.addRule(RelativeLayout.CENTER_IN_PARENT);
    tvparams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM , layout.getId());
    tvparams.addRule(RelativeLayout.CENTER_HORIZONTAL, layout.getId());
    layout.addView(progressBar,params);
    layout.setBackgroundColor(Color.WHITE);
    
    tv=new TextView(context);
    tv.setText("Loading...");
    tv.setTextSize(19);
    layout.addView(tv, tvparams);
    
    int rgb = 205;
    layout1.setBackgroundColor(Color.rgb(rgb, rgb, rgb));
    layout1.addView(layout, params1);
    
    
  }


  public TrumpProgressBar(Context context, int resource, String text) {
    setup(context);
    view = inflater.inflate(resource, null);
    tv.setText(text);
  
  }
  
  public void show()
  {
    Activity a = (Activity) context;
    a.setContentView(layout1);
    
  }

  public void hide()
  {
    Activity a = (Activity) context;
    a.setContentView(view);
  }

}
