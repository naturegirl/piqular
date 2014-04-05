package com.piqular.website;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;

import com.piqular.R;
import com.piqular.R.id;

import android.app.Activity;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

public class UrlShortener {
	
	private static UrlShortener instance = null;
	private static final String prefix = "http://tinyurl.com/api-create.php?url=";
	
	private Activity activity;		// must be SiteCreateActivity
	
	private UrlShortener(Activity act) {
		activity = act;
	}
	
	public static UrlShortener getInstance(Activity act) {
		if (instance == null) {
			instance = new UrlShortener(act);
		}
		return instance;
	}
	
	
	public void shorten(String longUrl) {
		String requestUrl = prefix + longUrl;
		new NetworkTask().execute(requestUrl);
	}

    private class NetworkTask extends AsyncTask<String, Void, HttpResponse> {
        @Override
        protected HttpResponse doInBackground(String... params) {
            String link = params[0];
            HttpGet request = new HttpGet(link);
            AndroidHttpClient client = AndroidHttpClient.newInstance("Android");
            try {
                return client.execute(request);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            } finally {
            client.close();
        }
        }

        @Override
        protected void onPostExecute(HttpResponse result) {
            //Do something with result
            if (result != null) {
            	HttpEntity entity = result.getEntity();
            	try {
					String shortUrl = EntityUtils.toString(entity, "UTF-8");
				} catch (ParseException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
            }
        }
    }
    
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
