package library;

import android.os.Bundle;
import android.widget.ImageView;

import baseclasses.MyActionBarActivity;
import trumplab.textslate.R;

public class ImageViewer extends MyActionBarActivity {
  
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    setContentView(R.layout.image_viewer);
    
    ImageView imageView = (ImageView) findViewById(R.id.imageViewer);
    
    final String imageName = getIntent().getExtras().getString("image");
  }

}
