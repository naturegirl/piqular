package com.piqular.dropbox;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;

import android.app.Activity;
import android.content.Context;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import com.dropbox.sync.android.DbxAccountManager;
import com.dropbox.sync.android.DbxException;
import com.dropbox.sync.android.DbxFile;
import com.dropbox.sync.android.DbxFileSystem;
import com.dropbox.sync.android.DbxPath;
import com.piqular.PiqularMainActivity;
import com.piqular.R;


public class DbManager {
	
	private static DbManager instance = null;
	
    private final static String appKey = "g9u2511s94axpyh";
    private final static String appSecret = "rdqxnyirkc2rttp";
    
    public final static String AppDir = "Piqular/";
    private final static String PhotoDir = AppDir + "Photos/";
    
    private Context context;
    private Activity activity;
	
    private DbxAccountManager mDbxAcctMgr;
    private DbxFileSystem dbxFs;
    
	private DbManager(Activity act, Context ctx) {
		this.context = ctx;
		this.activity = act;
		mDbxAcctMgr = DbxAccountManager.getInstance(context, appKey, appSecret);
	}
	
	public static DbManager getInstance(Activity act, Context ctx) {
		if (instance == null) {
			instance = new DbManager(act, ctx);
		}
		return instance;
	}
	// call only when the other one with act/ctx has been called before
	public static DbManager getInstance() {
		if (instance == null) {
			Log.e("swifflet", "call getInstance(activity, context) first instead!");
			throw new RuntimeException("called wrong getInstance() function");
		}
		return instance;
	}
	
	public void linkToDropbox() {
		if (!mDbxAcctMgr.hasLinkedAccount()) {
			mDbxAcctMgr.startLink(activity, PiqularMainActivity.LINK_DB_REQUEST);
		}
	}
	
	public boolean isLinked() {
		return mDbxAcctMgr.hasLinkedAccount();
	}
	
	public String getUid() {
		if (isLinked())
			return mDbxAcctMgr.getLinkedAccount().getUserId();
		else
			return null;
	}
	
	/* sets up dbxFs, called from the activity after linking the account succeeded */
	public void setupFs() {
		if (dbxFs == null) {
			try {
				dbxFs = DbxFileSystem.forAccount(mDbxAcctMgr.getLinkedAccount());
				dbxFs.addSyncStatusListener(new DbxFileSystem.SyncStatusListener() {
				    @Override
				    public void onSyncStatusChange(DbxFileSystem fs) {
				    	Log.w("swifflet", "sync status change");
				    }
				});
			} catch (DbxException e) { e.printStackTrace(); }
		}
	}
	
	//CANNOT BE CALLED FROM MAIN UI THREAD UNDER ANY
	//CIRCUMSTANCES!!! THAT WOULD BE TERRIBLE!!!
    public String[] getPublicURLs(int length, boolean photos) {
        
        String[] publicURLs = new String[length];
		try{
			for (int i = 1; i <= length; i++) {
				DbxPath dbPath;
				if (photos) { dbPath = new DbxPath(DbxPath.ROOT, PhotoDir+getPhotoName(i));} 
				else { dbPath = new DbxPath(DbxPath.ROOT, AppDir+i+".html");}
				Log.w("dbmanager pre url", dbPath.getName());
				boolean precreated = findOrCreate(dbPath);
				Log.w("precreation", precreated + "");
				if (precreated) {		
					Log.w("precreation", findOrCreate(dbPath) + "");
					String url = dbxFs.fetchShareLink(dbPath, false).toString();
					publicURLs[i-1] = url.replace("www.dropbox.com", "dl.dropboxusercontent.com");
					publicURLs[i-1] = publicURLs[i-1].replaceFirst("https", "http");
					Log.w("dbmanager nice url", publicURLs[i-1]);
				}
			}
		} catch (DbxException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return publicURLs;
    }
    
    private boolean findOrCreate(DbxPath dbPath) {
    	try {
			if (dbxFs.exists(dbPath)) return true;
            DbxFile preFile = dbxFs.create(dbPath);
            preFile.close();
            return true;
		} catch (DbxException e) {
			e.printStackTrace();
		}
    	
    	return false;
    }
    
	
	private String getPhotoName(int cnt) {
		return "img"+cnt+".jpg";
	}
	
	/* make sure to call setupFs before */
	public void syncFiles(String photoPaths[]) {
		try {
			if (dbxFs == null) {
				dbxFs = DbxFileSystem.forAccount(mDbxAcctMgr.getLinkedAccount());
				dbxFs.addSyncStatusListener(new DbxFileSystem.SyncStatusListener() {
				    @Override
				    public void onSyncStatusChange(DbxFileSystem fs) {
				    	Log.w("swifflet", "sync status change");
				    }
				});				
			}
			
			int cnt = 1;		
			for (String imgPath : photoPaths) {
				String filename = getPhotoName(cnt);
				DbxPath dbPath = new DbxPath(DbxPath.ROOT, PhotoDir+filename);
				
				DbxFile dbFile;
				File imgFile = new File(imgPath);
				
				if (dbxFs.exists(dbPath))
					dbFile = dbxFs.open(dbPath);
				else
					dbFile = dbxFs.create(dbPath);
				dbFile.writeFromExistingFile(imgFile, false);
				dbFile.close();
				cnt++;
			}
		} catch (DbxException e) { e.printStackTrace();
		} catch (IOException e) { e.printStackTrace(); }
	}
	
	/* write a textfile to dropbox
	 * assumes that dbxFs has been called.
	 * @filecontent: the whole file
	 * @filename: the target filename
	 */
	public void writeFile(String filecontent, String filename) {
		if (dbxFs == null) {
			Log.e("swifflet", "dbxFs shouldn't be null");
			throw new RuntimeException("dbxFs is null in writeFile()");
		}
		DbxPath dbPath = new DbxPath(DbxPath.ROOT, AppDir+filename);
		try {
			DbxFile dbFile;
			if (dbxFs.exists(dbPath))
				dbFile = dbxFs.open(dbPath);
			else
				dbFile = dbxFs.create(dbPath);
			dbFile.writeString(filecontent);
			dbFile.close();
		} catch (DbxException e) { e.printStackTrace();
		} catch (IOException e) { e.printStackTrace(); }
	}
	
	/* returns the DbxFile to write outside DbManager.
	 * Use only when really necessary, and don't forget to close inside calling function!
	 * also dbxFs can't be null!
	 * @filename: of the file. Will overwrite any existing files
	 */
	public DbxFile getFileToWrite(String filename) {
		if (dbxFs == null) {
			Log.e("swifflet", "dbxFs shouldn't be null");
			throw new RuntimeException("dbxFs is null in writeFile()");
		}
		DbxPath dbPath = new DbxPath(DbxPath.ROOT, AppDir+filename);
		try {
			DbxFile dbFile;
			if (dbxFs.exists(dbPath))
				dbFile = dbxFs.open(dbPath);
			else
				dbFile = dbxFs.create(dbPath);
			return dbFile;
		} catch (DbxException e) {
			Log.e("swifflet", "error creating DbxFile");
			e.printStackTrace();
		}
		return null;
	}
	
	public void testing() {
        try {
            final String TEST_DATA = "Goodbye Dropbox";
            final String TEST_FILE_NAME = "blablabla.txt";
            
            DbxPath testPath = new DbxPath(DbxPath.ROOT, "Piqular/"+TEST_FILE_NAME);

            // Create DbxFileSystem for synchronized file access.
            DbxFileSystem dbxFs = DbxFileSystem.forAccount(mDbxAcctMgr.getLinkedAccount());

            // Create new file or overwrite existing file
            DbxFile testFile;
            if (dbxFs.exists(testPath)) {
                testFile = dbxFs.open(testPath);
            } else {
            	testFile = dbxFs.create(testPath);
            }
            testFile.writeString(TEST_DATA);
            testFile.close();
            Log.w("swifflet", "\nCreated new file '" + testPath + "'.\n");            	
        } catch (DbxException e) {
        	e.printStackTrace();
        } catch (IOException e) {
        	e.printStackTrace();
        }
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
