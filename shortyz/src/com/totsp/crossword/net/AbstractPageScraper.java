package com.totsp.crossword.net;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import com.totsp.crossword.io.IO;
import com.totsp.crossword.puz.Puzzle;

public class AbstractPageScraper {
	
	private static final String REGEX = "http://[^ ]*\\.puz";
	private static final String REL_REGEX="href=\"(.*\\.puz)\"";
	private static final Pattern PAT = Pattern.compile(REGEX);	
	private static final Pattern REL_PAT = Pattern.compile(REL_REGEX);
	
	
	private String url;
	private String sourceName;
	
	protected AbstractPageScraper(String url, String sourceName){
		this.url = url;
		this.sourceName = sourceName;
	}
	
	public String getContent() throws IOException  {
		URL u = new URL(url);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		AbstractDownloader.copyStream(u.openStream(), baos);
		return new String(baos.toByteArray());
	}
	
	public static List<String> puzzleURLs(String input){
		ArrayList<String> result = new ArrayList<String>();
		Matcher matcher = PAT.matcher(input);
		while(matcher.find()){
			result.add(matcher.group());
		}
		return result;
	}
	
	public static List<String> puzzleRelativeURLs(String baseUrl, String input) throws MalformedURLException{
		URL base = new URL(baseUrl);
		ArrayList<String> result = new ArrayList<String>();
		Matcher matcher = REL_PAT.matcher(input);
		while(matcher.find()){
			result.add(new URL(base, matcher.group(1)).toString()	);
		}
		return result;
	}
	
	public static Map<String, String> mapURLsToFileNames(List<String> urls){
		HashMap<String, String> result = new HashMap<String, String>(urls.size());
		
		for(String url : urls){
			String fileName = url.substring(url.lastIndexOf("/")+1);
			result.put(url, fileName);
		}
		
		return result;
	}
	
	public static File download(String url, String fileName) throws IOException {
		URL u = new URL(url);
		File output = new File(AbstractDownloader.DOWNLOAD_DIR, fileName);
		FileOutputStream fos = new FileOutputStream(output);
		AbstractDownloader.copyStream(u.openStream(), fos);
		fos.close();
		return output;
	}
	
	public boolean processFile(File file) {
		
		try {
			Puzzle puz = IO.load(file);
			puz.setSource(this.sourceName);
			puz.setDate(new Date());
			IO.save(puz, file);
			return true;
			
		} catch(Exception e){
			e.printStackTrace();
			file.delete();
			return false;
		}
	}
	
	public List<File> scrape(){
		ArrayList<File> scrapedFiles = new ArrayList<File>();
		
		try{
			String content = this.getContent();
			List<String> urls = puzzleURLs(content);
			try{
				urls.addAll( puzzleRelativeURLs(url, content));
			} catch(IOException e){
				e.printStackTrace();
			}
			
			System.out.println("Found puzzles: "+urls);
			Map<String,String> urlsToFilenames = mapURLsToFileNames(urls);
			System.out.println("Mapped: "+urlsToFilenames.size());
			for(String url : urls ){
				String filename = urlsToFilenames.get(url);
				if( !(new File(AbstractDownloader.DOWNLOAD_DIR, filename).exists()) && 
						!(new File(AbstractDownloader.DOWNLOAD_DIR, "archive/"+filename).exists()) && 
						scrapedFiles.size() < 3){
					
					System.out.println("Attempting "+url +"  scraped "+scrapedFiles.size());
					try{
						File file = download(url, filename);
						if(this.processFile(file)){
							scrapedFiles.add(file);
							System.out.println("SCRAPED.");
						}
					} catch(Exception e){
						System.err.println("Exception downloading "+url+" for "+this.sourceName);
						e.printStackTrace();
					}
				} else {
					System.out.println(filename +" exists.");
				}
			}
		} catch(IOException e){
			e.printStackTrace();
		}
		
		return scrapedFiles;
		
	}
	
	public String getSourceName(){
		return this.sourceName;
	}
	
	
}