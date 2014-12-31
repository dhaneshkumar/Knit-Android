package trumplabs.schoolapp;

import trumplab.textslate.R;
import android.os.Bundle;
import baseclasses.MyActionBarActivity;
import baseclasses.MyActivity;

public class Main extends MyActionBarActivity{

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		FontsOverride.setDefaultFont(this,"MONOSPACE","fonts/RobotoCondensed-Italic.ttf");
		setContentView(R.layout.professionchoice);		
	}
}
