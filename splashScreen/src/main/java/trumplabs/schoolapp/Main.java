package trumplabs.schoolapp;

import android.os.Bundle;

import baseclasses.MyActionBarActivity;
import trumplab.textslate.R;

public class Main extends MyActionBarActivity{

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		FontsOverride.setDefaultFont(this,"MONOSPACE","fonts/RobotoCondensed-Italic.ttf");
		setContentView(R.layout.professionchoice);		
	}
}
