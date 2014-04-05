package com.piqular.website;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Scanner;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.EditText;

import com.dropbox.sync.android.DbxFile;
import com.piqular.R;
import com.piqular.UrlShortener;
import com.piqular.dropbox.DbManager;

public class SiteManager {

	private static SiteManager instance = null;
	
	private static int NUM_IMG_PER_PAGE = 7;
	private static int QUOTE_IMG_RATIO = 3;
	
	private static String HeaderFilename = "header.txt";
	private static String QuotesFilename = "quote.txt";
	private static String PaginateFilename = "paginate.txt";
	private static String FooterFilename = "footer.txt";
	private static String PhotoFilename = "photo.txt";
	private static String[] quoteMap = {"food.txt", "friendsnfamily.txt", "productivity.txt", "vacation.txt"};
	private ArrayList<String[]> quotes;
	
	private Activity activity;
	private Context context;
	
	private AssetManager manager;
	
	private String title;
	private String desc;
	private String[] images;
	int category;

	private SiteManager (Activity act, Context ctx, String title, String desc, int category, String[] images) {

		this.activity = act;
		this.context = ctx;
		manager = context.getAssets();
		this.images = images.clone();
		this.title = title;
		this.desc = desc;
		this.category = category;
		
	}
	
	public static SiteManager getInstance(Activity act, Context ctx, 
			String title, String desc, 
			int category, String[] images) {
		if (instance == null) {
			instance = new SiteManager(act, ctx, title, desc, category, images);
		}
		return instance;
		
	}
	
    private class GenerateTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void ... nothing) {
            int length = images.length;
            int max = (int) Math.ceil((double)length/NUM_IMG_PER_PAGE);
            String[] photos = DbManager.getInstance().getPublicURLs(length, true);
            Log.w("swifflet", "trying to get photo URLS");
            for (int i = 0; i < photos.length; i++) {
            	Log.w("photo URLs", photos[i]);
            }
            String[] htmlDocs = DbManager.getInstance().getPublicURLs(max, false);
            for (int i = 0; i < htmlDocs.length; i++) {
            	Log.w("htmlDocs URLS", htmlDocs[i]);
            }
            //NEED TO GET SITECREATEACTIVITY
            UrlShortener.getInstance(activity).shorten(htmlDocs[0]);
            int p = 0;
    		try {
				for (int i = 1; i <= max; i++) {
					String filename = i + ".html";
					StringBuilder sb = new StringBuilder();
					writeHeaders(sb, title, desc, htmlDocs[0]);
					
					for (int j = 0; j < NUM_IMG_PER_PAGE && p < length; j++) {
						Log.w("photo", photos[p]);
						printPhoto(sb, photos[p], "");
						p++;
						if (category != 0) {
							if (p % QUOTE_IMG_RATIO == 0) {
								String[] quote = chooseQuote(category-1);
								printQuote(sb, quote);
							}
						}
					}
					printFooters(sb, i, max, htmlDocs);
					DbManager.getInstance().writeFile(sb.toString(), filename);
				}
    		} catch (Exception e) { e.printStackTrace(); }
            
            
            return null;
        }

    }
	
	
	// generates and writes the website
	public void generate() {
		new GenerateTask().execute();
	}
	
	private void writeHeaders(StringBuilder sb, String title, String desc, String link) {
		try {
			InputStream is = manager.open(HeaderFilename);
			BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
			String line;
			while ((line = br.readLine()) != null) {
				line = line.replace("#INSERT TITLE#", title);
				line = line.replace("#HOME#", link);
				if (line.contains("#DESCRIPTION#")) {
					if (desc == null || desc == "") line = "";
					else line = line.replace("#DESCRIPTION#", desc);	
				}
				sb.append(line+"\n");
				//Log.w("swifflet", line);
			}
			br.close();
		} catch (Exception e) {System.out.println(e);}
	}
	
	private void printFooters(StringBuilder sb, int i , int max, String[] links) {
		try {
			//paginate
			if (max > 1) {
				InputStream is = manager.open(PaginateFilename);
				BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
				String line;
				while ((line = br.readLine()) != null) {
					if (line.contains("#PREVLINK#")) {
						if (i != 1) sb.append(line.replace("#PREVLINK#", links[i-2] + "")+"\n");
					}
					else if (line.contains("#NEXTLINK#")) {
						if (i < max) sb.append(line.replace("#NEXTLINK#", links[i] + "")+"\n");
					}
					else if (line.contains("#PRECURRLINK#")) {
						if ((i-2) >= 1) {
							line = line.replace("#PRECURRLINK#", links[i-3] + "")+"\n";
							sb.append(line.replace("#PRECURR#", (i-2) + "")+"\n");
						}
						if ((i-1) >= 1) {
							line = line.replace("#PRECURRLINK#", links[i-2] + "")+"\n";
							sb.append(line.replace("#PRECURR#", (i-1) + "")+"\n");
						}
					}
					else if (line.contains("#POSTCURRLINK#")) {
						if ((i+1) <= max) {
							line = line.replace("#POSTCURRLINK#", links[i] + "")+"\n";
							sb.append(line.replace("#POSTCURR#", (i+1) + "")+"\n");
						}
						if ((i+2) <= max) {
							line = line.replace("#POSTCURRLINK#", links[i+1] + "")+"\n";
							sb.append(line.replace("#POSTCURR#", (i+2) + "")+"\n");
						}
					}
					else if (line.contains("#CURRENT#")) {
						sb.append(line.replace("#CURRENT#", (i) + "")+"\n");
					}
					else
						sb.append(line+"\n");
				}
				br.close();
			}
			//print footer
			InputStream is = manager.open(FooterFilename);
			BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
			String line;
			while ((line = br.readLine()) != null) {
				sb.append(line+"\n");
			}
		} catch (Exception e) { e.printStackTrace(); }
	}


	private void printPhoto(StringBuilder sb, String img, String details) {
		try {
			InputStream is = manager.open(PhotoFilename);
			BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
			String line;
			while ((line = br.readLine()) != null) {
				Log.w("inPhoto", line);
				if (line.contains("#IMG#")) {
					sb.append(line.replace("#IMG#", img)+"\n");
				}
				else if (line.contains("#DETAILS#")) {
					if (details == null || details == "");
					sb.append(line.replace("#DETAILS#", details)+"\n");
				}
				else 
					sb.append(line+"\n");
			}
			br.close();
		} catch (Exception e) {e.printStackTrace();}
	}

	
	private String[] chooseQuote(int cat) {
		if (quotes == null) {
			quotes = new ArrayList<String[]>();
			try {
				InputStream is = manager.open(quoteMap[cat]);
				BufferedReader bf = new BufferedReader(new InputStreamReader(is, "UTF-8"));
				String line;
				while ((line = bf.readLine()) != null) {
					line = line.replace("\"", "");
					Log.w("quote", line);
					quotes.add(line.split("\t"));
				} 
				bf.close();
			} catch (Exception e) {System.out.println(e);}
		}
		
		int rand = (int) (Math.random()*quotes.size());
		String[] quote = quotes.get(rand);
		quotes.remove(rand);
		return quote;
		
	}
	

	private void printQuote(StringBuilder sb, String[] quote) {
		try {
			InputStream is = manager.open(QuotesFilename);
			BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
			String line;
			while ((line = br.readLine()) != null) {
				if (line.contains("#QUOTE#")) {
					sb.append(line.replace("#QUOTE#", quote[0])+"\n");
				}
				else if (line.contains("#CITE#")) {
					if (quote[1] == "");
					else sb.append(line.replace("#CITE#", quote[1])+"\n");
				}
				else
					sb.append(line+"\n");
			}
			br.close();
		} catch (Exception e) { e.printStackTrace(); }
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
