package loginpages;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import trumplab.textslate.R;

public class signUp0Class extends DialogFragment{
	
	TextView teacher;
	TextView parent;
	TextView ok_btn;
	String choice="";
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		View view = getActivity().getLayoutInflater().inflate(
				R.layout.signup0_layout, null);
		builder.setView(view);
		Dialog dialog = builder.create();
		dialog.show();
		
		parent = (TextView) view.findViewById(R.id.parent);
		teacher = (TextView) view.findViewById(R.id.teacher);
		
		parent.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				choice="parent";
				parent.setTextColor(getResources().getColor(R.color.secondarycolor));
				teacher.setTextColor(getResources().getColor(R.color.buttoncolor));
				
				
				Intent intent=new Intent(getActivity().getBaseContext(),Signup1Class.class);
                intent.putExtra("role", choice);
                startActivity(intent);
			}
		});
			
		teacher.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					choice="teacher";
					parent.setTextColor(getResources().getColor(R.color.buttoncolor));
					teacher.setTextColor(getResources().getColor(R.color.secondarycolor));
					
					Intent intent=new Intent(getActivity().getBaseContext(),Signup1Class.class);
                    intent.putExtra("role", choice);
                    startActivity(intent);
				}
		});
		
		return dialog;
	}
	
/*
	protected void onCreate(android.os.Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.signup0_layout);
		
		parent = (Button) findViewById(R.id.parent);
		teacher = (Button) findViewById(R.id.teacher);
		
			parent.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				Intent intent=new Intent(getBaseContext(),Signup1Class.class);
				intent.putExtra("role", "parent");
				startActivity(intent);
			}
			});
			
			teacher.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					Intent intent=new Intent(getBaseContext(),Signup1Class.class);
					intent.putExtra("role", "teacher");
					startActivity(intent);
				}
				});
	}
	*/
}
