package com.piqular.website;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Scanner;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.util.Log;

import com.dropbox.sync.android.DbxFile;
import com.piqular.dropbox.DbManager;

public class SiteManager {

	private static SiteManager instance = null;
	
	private static int NUM_IMG_PER_PAGE = 7;
	private static int QUOTE_IMG_RATIO = 3;
	
	private static String CssFilename = "mystyle.css";
	private static String HeaderFilename = "header.txt";
	private static String QuotesFilename = "quote.txt";
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
	
	// generates and writes the website
	public void generate() {
		writeCss();
		int p = 0;
		try {
			int max = (int) Math.ceil((double)images.length/NUM_IMG_PER_PAGE);
			Log.w("max", max+"");
			for (int i = 1; i <= max; i++) {
				String filename = i + ".html";
				DbxFile dbFile = DbManager.getInstance().getFileToWrite(filename);
				StringBuilder sb = new StringBuilder();
				writeHeaders(sb, title, desc);
				
				for (int j = 0; j < NUM_IMG_PER_PAGE && p < images.length; j++) {
					Log.w("photo", images[p]);
					printPhoto(sb, images[p], "");
					p++;
					if (category != 0) {
						if (p % QUOTE_IMG_RATIO == 0) {
							String[] quote = chooseQuote(category-1);
							printQuote(sb, quote);
						}
					}
				}
				printFooters(sb, i, max);
				dbFile.writeString(sb.toString());
				dbFile.close();
			}
		} catch (Exception e) { e.printStackTrace(); }
	}

	private void writeCss () {
		try {
			InputStream is = manager.open(CssFilename);
		    Scanner s = new Scanner(is);			// convert inputstream to string
		    s.useDelimiter("\\A");
		    String content = s.hasNext() ? s.next() : "";
		    s.close();
		    DbManager.getInstance().writeFile(content, CssFilename);
		} catch (IOException e) { e.printStackTrace(); }
	}
	
	private void writeHeaders(StringBuilder sb, String title, String desc) {
		try {
			InputStream is = manager.open(HeaderFilename);
			BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
			String line;
			while ((line = br.readLine()) != null) {
				line = line.replace("#INSERT TITLE#", title);
				if (desc == null) {				
					line = line.replace("<div class=\"description\"></div>", "<div class=\"description\">"+desc+"</div>");
				} else {
					line = line.replace("<div class=\"description\"></div>", "");
				}
				sb.append(line+"\n");
				//Log.w("swifflet", line);
			}
			br.close();
		} catch (Exception e) {System.out.println(e);}
	}
	
	private void printFooters(StringBuilder sb, int i , int max) {
		//no pagination needed
		try {
			if (max == 1) {sb.append("\t</body>\n</html>"); return;}
			InputStream is = manager.open(FooterFilename);
			BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
			String line;
			while ((line = br.readLine()) != null) {
				if (line.contains("#PREV#")) {
					if (i == 1) line = "";
					sb.append(line.replace("#PREV#", (i-1) + "")+"\n");
				}
				else if (line.contains("#NEXT#")) {
					if (i >= max) line = "";
					sb.append(line.replace("#NEXT#", (i+1) + "")+"\n");
				}
				else if (line.contains("#PRECURR#")) {
					if ((i-2) >= 1)
						sb.append(line.replace("#PRECURR#", (i-2) + "")+"\n");
					if ((i-1) >= 1)
						sb.append(line.replace("#PRECURR#", (i-1) + "")+"\n");
				}
				else if (line.contains("#POSTCURR#")) {
					if ((i+1) <= max)
						sb.append(line.replace("#POSTCURR#", (i+1) + "")+"\n");
					if ((i+2) <= max) 
						sb.append(line.replace("#POSTCURR#", (i+2) + "")+"\n");
				}
				else if (line.contains("#CURRENT#")) {
					sb.append(line.replace("#CURRENT#", (i) + "")+"\n");
				}
				else
					sb.append(line+"\n");
			}
			br.close();
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
