package com.piqular;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.piqular.dropbox.DbManager;
import com.piqular.website.SiteManager;


public class PiqularMainActivity extends ActionBarActivity {

	private DbManager dbManager;
	private String photoPaths[];		// paths to the local photo files
	private String[] fullUrls;			// paths to the final public urls
	private int syncStatus;				// status of dropbox syncing
	
	private static final int SYNC_NOT_STARTED = 0;
	private static final int SYNC_STARTED = 1;
	private static final int SYNC_DONE = 2;		// TODO: set asynchronuously when sync done
	
	public static int LINK_DB_REQUEST = 100;
	private static int SELECT_PHOTO_REQUEST = 200;
	
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		View connectButtonView = findViewById(R.id.connect_db_button);
		Button connectButton = (Button) connectButtonView;
        Button selectPicsButton = (Button) findViewById(R.id.select_photos_button);
        Button syncButton = (Button) findViewById(R.id.sync_db_button);
        Button createSiteButton = (Button) findViewById(R.id.create_website_button);
        
        Button testButton = (Button) findViewById(R.id.test_button);
        
        syncStatus = SYNC_NOT_STARTED;
		dbManager = DbManager.getInstance(this, getApplicationContext());
        
        if (dbManager.isLinked()) {
        	connectButtonView.setVisibility(View.GONE);
        }
        else {
	        connectButton.setOnClickListener(new OnClickListener() {
	            public void onClick(View v) {
	            	onClickLinkToDropbox();
	            }
        });
        }
        
        selectPicsButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				onClickStartPhotoSelect();
			}
        });
        
        syncButton.setOnClickListener(new OnClickListener() {
        	public void onClick(View v) {
        		onClickSyncDropbox();
        	}
        });
        
        testButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	testing();
            }        	
        });
        
        createSiteButton.setOnClickListener(new OnClickListener() {
        	public void onClick(View v) {
        		onClickCreateWebsite();
        	}
        });
        

	}
	
    private void onClickLinkToDropbox() {
    	if (!dbManager.isLinked())
    		dbManager.linkToDropbox();
    	else {
	    	Toast.makeText(getApplicationContext(), "linked with dropbox!", 
	                Toast.LENGTH_SHORT).show();
    	}
    }
    
    private void onClickStartPhotoSelect() {
    	Intent intent = new Intent(this, PhotoSelectActivity.class);
    	intent.putExtra("key","value");
    	startActivityForResult(intent, SELECT_PHOTO_REQUEST);
    }
    
    private void onClickSyncDropbox() {
    	if (!dbManager.isLinked()) {
    		String msg = "please link with dropbox first.";
        	Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
        	return;
    	}
    	else if (photoPaths == null || photoPaths.length == 0) {
    		String msg = "please select photos first.";
        	Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
        	return;
    	}
    	dbManager.syncFiles(photoPaths);
    	syncStatus = SYNC_STARTED;
    }
    
    private void onClickCreateWebsite() {
    	if (!dbManager.isLinked()) {
    		String msg = "please link with dropbox first.";
        	Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
        	return;
    	}
    	else if (photoPaths == null || photoPaths.length == 0) {
    		String msg = "please select photos first.";
        	Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
        	return;
    	}
    	else if (syncStatus == SYNC_NOT_STARTED) {
    		String msg = "please sync with dropbox first.";
        	Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
        	return;    		
    	}
    	
    	fullUrls = new String[photoPaths.length];		// full public URLs of images
    	String uid = dbManager.getUid();
    	String prefix = "http://dl.dropboxusercontent.com/u/" + uid + "/" + DbManager.AppDir;
    	for (int i = 0; i < photoPaths.length; ++i) {
    		fullUrls[i] = prefix + "img" + Integer.toString(i+1) + ".jpg";
    		Log.w("swifflet", fullUrls[i]);
    	}
    	
    	Intent intent = new Intent(this, SiteCreateActivity.class);
    	intent.putExtra("full_urls", fullUrls);
    	startActivity(intent);
    }
    
    private void testing() {
    	//String link = "http://tinyurl.com/api-create.php?url=http://scripting.com/";
    	//UrlShortener.getInstance(this).shorten(link);
    }
    
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if (requestCode == SELECT_PHOTO_REQUEST && resultCode == Activity.RESULT_OK) {
			photoPaths = data.getStringArrayExtra("photo_paths");
			for (String path : photoPaths) {
				Log.w("swifflet", path);
			}
		}
		
		if (requestCode == LINK_DB_REQUEST && resultCode != Activity.RESULT_OK) {
			String msg = "Link to Dropbox failed or was cancelled";
			Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
		}
		
		if (requestCode == LINK_DB_REQUEST && resultCode == Activity.RESULT_OK) {
	    	Toast.makeText(getApplicationContext(), "linked with dropbox!", 
	                Toast.LENGTH_SHORT).show();
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
