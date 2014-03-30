package com.piqular;

import com.piqular.website.SiteManager;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

public class SiteCreateActivity extends Activity {
		
		private EditText title_et;
		private EditText desc_et;
		private Spinner quote_s;
		private RadioGroup layout_rb;

		private String[] fullUrls;
		
		@Override
		protected void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			setContentView(R.layout.site_main);
			
			quote_s = (Spinner) findViewById(R.id.quote_cat);
			ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
			        R.array.quote_options, android.R.layout.simple_spinner_item);
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			quote_s.setAdapter(adapter);
			
			title_et = (EditText) findViewById(R.id.title);
			desc_et = (EditText) findViewById(R.id.desc);
			layout_rb = (RadioGroup) findViewById(R.id.layout);
			
			Context context = getApplicationContext();
			String title = "Vacation Photos";
			String desc = "Tigers! Lots and Lots of Tigers.";
			int cat = 2;
			
			Bundle extras = getIntent().getExtras();
			if (extras != null) {
			    fullUrls = extras.getStringArray("full_urls");
			}
			if (fullUrls == null) {
				Log.e("swifflet", "error, couldn't retrieve full urls!");
			}
		}
		
		public void onClick(View view) {
	        InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
	        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
	    }

		public void genWebsite(View view) {
			
			//EditText title_et = (EditText) findViewById(R.id.title);
			String title = title_et.getText().toString();
			String desc = desc_et.getText().toString();
			int layout = layout_rb.getCheckedRadioButtonId();
			int quoteType = quote_s.getSelectedItemPosition();
			
			if (title.length() == 0 || layout == -1) {
				Context context = getApplicationContext();
				CharSequence text = "";
				if (title.length() == 0 && layout != -1)
					text = "Please enter a title for your site.";
				if (title.length() != 0 && layout == -1)
					text = "Please select what layout you'd like on your site.";
				if (title.length() == 0 && layout == -1)
					text = "Please enter a title and select a layout type.";
				int duration = Toast.LENGTH_SHORT;

				Toast toast = Toast.makeText(context, text, duration);
				toast.show();
			} else {
				//site manager
		    	SiteManager sm = SiteManager.getInstance(this, getApplicationContext(), title, desc, quoteType, fullUrls);
		    	sm.generate();
				
				Log.w("attributes", title);
				Log.w("attributes", desc);
			}
		}
		
		@Override
		public boolean onCreateOptionsMenu(Menu menu) {

			// Inflate the menu; this adds items to the action bar if it is present.
			getMenuInflater().inflate(R.menu.main, menu);
			return true;
		}

		@Override
		public boolean onOptionsItemSelected(MenuItem item) {
			// Handle action bar item clicks here. The action bar will
			// automatically handle clicks on the Home/Up button, so long
			// as you specify a parent activity in AndroidManifest.xml.
			int id = item.getItemId();
			if (id == R.id.action_settings) {
				return true;
			}
			return super.onOptionsItemSelected(item);
		}


	}
