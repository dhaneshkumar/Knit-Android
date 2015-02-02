package library;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.widget.ImageView;

import trumplab.textslate.R;

public class ImageViewer extends ActionBarActivity{
  
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    setContentView(R.layout.image_viewer);
    
    ImageView imageView = (ImageView) findViewById(R.id.imageViewer);
    
    final String imageName = getIntent().getExtras().getString("image");
  }

}
