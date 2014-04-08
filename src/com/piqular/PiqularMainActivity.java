package com.piqular;


import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.piqular.dropbox.DbManager;
import com.piqular.website.SiteManager;
import com.piqular.website.UrlShortener;


public class PiqularMainActivity extends ActionBarActivity {

    private DbManager dbManager;
    private String[] photoPaths;		// paths to the local photo files
    private String[] fullUrls;			// paths to the final public urls
    private int syncStatus;				// status of dropbox syncing

    private static final int SYNC_NOT_STARTED = 0;
    private static final int SYNC_STARTED = 1;
    private static final int SYNC_DONE = 2;		// TODO: set asynchronuously when sync done

    public static int LINK_DB_REQUEST = 100;
    private static int SELECT_PHOTO_REQUEST = 200;

    private static final String fontPath = "fonts/ABeeZee-Italic.ttf";

    protected void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	setContentView(R.layout.activity_main);

	Button selectPicsButton = (Button) findViewById(R.id.select_photos_button);
	Button createSiteButton = (Button) findViewById(R.id.create_website_button);

	//Button testButton = (Button) findViewById(R.id.test_button);

	syncStatus = SYNC_NOT_STARTED;

	TextView txtTransform = (TextView) findViewById(R.id.transform);
	Typeface tf = Typeface.createFromAsset(getAssets(), fontPath);
	txtTransform.setTypeface(tf);

	if (!UrlShortener.alreadyInit()) UrlShortener.init(this);
	if (!DbManager.alreadyInit()) DbManager.init(this, getApplicationContext());

	dbManager = DbManager.getInstance();

	if (!dbManager.isLinked())
	    dbManager.linkToDropbox();

	selectPicsButton.setOnClickListener(new OnClickListener() {
	    public void onClick(View v) {
		onClickStartPhotoSelect();
	    }
	});

//	testButton.setOnClickListener(new OnClickListener() {
//	    public void onClick(View v) {
//		testing();
//	    }        	
//	});

	createSiteButton.setOnClickListener(new OnClickListener() {
	    public void onClick(View v) {
		onClickCreateWebsite();
	    }
	});


    }

    // displays the result url
    public void displayResultUrl(final String url) {
	
	TextView motto_tv = (TextView) findViewById(R.id.transform);
	motto_tv.setVisibility(View.GONE);
	
	TextView tv = (TextView) findViewById(R.id.tinyurl_main);
	Log.w("urlpreset text", url);
	tv.setText(url);
	Log.w("urlpostset text", url);
	Button copyButton = (Button) findViewById(R.id.copyButton);
	Button shareButton = (Button) findViewById(R.id.shareButton);
	copyButton.setVisibility(View.VISIBLE);
	shareButton.setVisibility(View.VISIBLE);
	Log.w("urlpreset buttons", url);
	copyButton.setOnClickListener(new OnClickListener() {
	    public void onClick(View v) {
		ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
		ClipData data = ClipData.newPlainText("label", url);
		cm.setPrimaryClip(data);
		String msg = "website link copied to clipboard";
		Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();		    	
	    }
	});
	Log.w("urlpostset button clipboard", url);
	shareButton.setOnClickListener(new OnClickListener() {
	    public void onClick(View v) {
		Intent sendIntent = new Intent();
		sendIntent.setAction(Intent.ACTION_SEND);

		String title = SiteManager.getInstance().getTitle();
		String name = DbManager.getInstance().getUserName();				
		
		//mail to
		String subject = name + " has shared '" + title + "' with you!";
		String message = "Hey!\n\nCheck out "+ name + "'s moments at " + url + "\n\nbrought to you by piqular\nTransform your moments.";

		sendIntent.setType("text/plain");
		sendIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, subject);
		sendIntent.putExtra(android.content.Intent.EXTRA_TEXT, message);

		startActivity(Intent.createChooser(sendIntent, "Share with your friends!"));		
	    }
	});
	Log.w("urlpostset button share", url);
    }

    private void onClickStartPhotoSelect() {
	if (!dbManager.isLinked()) {
	    String msg = "please link with dropbox first.";
	    Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
	    return;    		
	}
	Intent intent = new Intent(this, PhotoSelectActivity.class);
	intent.putExtra("key","value");
	startActivityForResult(intent, SELECT_PHOTO_REQUEST);
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
	DbManager.getInstance().test();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	super.onActivityResult(requestCode, resultCode, data);

	if (requestCode == SELECT_PHOTO_REQUEST && resultCode == Activity.RESULT_OK) {
	    photoPaths = data.getStringArrayExtra("photo_paths");
	    for (String path : photoPaths) {
		Log.w("swifflet", path);
	    }
	    syncStatus = SYNC_STARTED;
	    dbManager.syncFiles(photoPaths);
	}

	if (requestCode == LINK_DB_REQUEST && resultCode != Activity.RESULT_OK) {
	    String msg = "Link to Dropbox failed or was cancelled";
	    Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
	}

	if (requestCode == LINK_DB_REQUEST && resultCode == Activity.RESULT_OK) {
	    Toast.makeText(getApplicationContext(), "linked with dropbox!", 
		    Toast.LENGTH_SHORT).show();
	    finish();
	    startActivity(getIntent());
	}
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
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
