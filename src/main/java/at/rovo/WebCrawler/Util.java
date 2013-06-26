package at.rovo.WebCrawler;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import at.rovo.caching.drum.DrumException;
import at.rovo.caching.drum.DrumUtil;

public class Util 
{
	private final static Logger logger = LogManager.getLogger(Util.class.getName()); 
	
	/**
	 * <p>Extracts the pay level domain from a URL</p>
	 * 
	 * @param url The absolute URL of any web page
	 * @return The pay level domain of the provided URL or null if the
	 *         pay level domain could not be extracted
	 */
	public static String getPLDofURL(String url)
	{
		String origin = url;
		try
		{
			if (logger.isDebugEnabled())
				logger.debug("Extracting PLD of "+url);
			// remove leading 'http://' or 'https://'
			if (url.contains("://"))
				url = url.substring(url.indexOf("://")+3);
			// remove everything after the first '/' which indicates the first sub-directory
			if (url.indexOf("/") != -1)
				url = url.substring(0, url.indexOf("/"));
			if (url.contains("?"))
				url = url.substring(0, url.indexOf("?"));
			if (url.startsWith("localhost") || url.startsWith("192.168."))
				return null;
			// fetch the top level domain
			String TLD = url.substring(url.lastIndexOf("."));
			// and split the rest of the URL apart from the TLD
			String rest = url.substring(0, url.lastIndexOf(TLD));
			
			// check if we have found an extended TLD
			// if so, add this part to the TLD
			if (rest.endsWith(".co") || rest.endsWith("wa.edu") || rest.endsWith(".edu") || rest.endsWith(".ac") || rest.endsWith(".go"))
			{
				TLD = rest.substring(rest.lastIndexOf("."))+TLD;
				rest = url.substring(0, url.lastIndexOf(TLD));
			}
			
			// we now only have to look up the part after the last 
			// remained '.' and add the TLD to its end
			String PLD = rest.substring(rest.lastIndexOf(".")+1) + TLD;
			if (logger.isDebugEnabled())
				logger.debug("PLD: "+PLD);
			return PLD;
		}
		catch (IndexOutOfBoundsException e)
		{
			if (logger.isErrorEnabled())
				logger.error("Error extracting PLD of url: "+origin, e);
			throw e;
		}
	}
	
	public static String getHostname(String url)
	{
		return "http://"+getPLDofURL(url);
	}
	
	public static String getDirectoryPathOfUrl(String url)
	{
		// get the pay level domain of the URL
		String PLD = getPLDofURL(url);
		// find the pay level domain in the URL and set the cursor
		// behind the end of the URL and return the rest of the URL
		String path = url.substring(url.indexOf(PLD)+PLD.length());
		return path;
	}
	
	public static <V> void printCacheContent(String name, List<Long> keys, Class<V> valueClass) throws IOException 
	{
		if (logger.isInfoEnabled())
			logger.info("Data contained in cache.db:");
		
		String userDir = System.getProperty("user.dir");
		String cacheName = userDir+"/cache/"+name+"/cache.db";
		RandomAccessFile cacheFile = new RandomAccessFile(cacheName, "r");
		cacheFile.seek(0);
		
		Pair<Long, V> data = null;
		do
		{
			data = getNextEntry(cacheFile, valueClass);
			if (data != null)
			{
				keys.add(data.getFirst());
				V hostData = data.getLast();
				if (hostData != null)
					if (logger.isInfoEnabled())
						logger.info("Key: "+data.getFirst()+", Value: "+hostData);
				else
					if (logger.isInfoEnabled())
						logger.info("Key: "+data.getFirst()+", Value: "+null);
			}
		}
		while (data != null);
		
		cacheFile.close();
	}
	
	public static <V> Pair<Long, V> getNextEntry(RandomAccessFile cacheFile, Class<V> valueClass) 
	{
		// Retrieve the key from the file
		try
		{	if (cacheFile.getFilePointer() == cacheFile.length())
				return null;
		
			Long key = cacheFile.readLong();
			
			// Retrieve the value from the file
			int valueSize = cacheFile.readInt();
			if (valueSize > 0)
			{
				byte[] byteValue = new byte[valueSize];
				cacheFile.read(byteValue);
				V value = DrumUtil.deserialize(byteValue, valueClass);
				return new Pair<Long, V>(key, value);
			}
			return new Pair<Long, V>(key, null);
		}
		catch (IOException | ClassNotFoundException e)
		{
			throw new DrumException("Error fetching next entry from cache", e);
		}
	}
	
	/**
	 * <p>Checks if a URL is in an absolute format and if not it tries to
	 * transform it into an absolute URL using the parent URL.</p>
	 * <p>Note that URLs that start with an anchor, javascript or mailto tag
	 * are discarded and null will be returned therefore.</p>
	 * 
	 * @param url The URL that should be checked for its absolute format
	 * @param parentURL The parent URL of <em>url</em> which will be used to
	 *                  transform the relative URL into an absolute one
	 * @return The absolute URL or null if <em>url</em> was either an anchor,
	 *         javascript or mailto tag
	 */
	public static String checkAndTransformURL(String url, String parentURL)
	{
		url = url.trim();
    	if (!url.endsWith("/") && !url.contains("?") && url.lastIndexOf("/") > url.lastIndexOf(".") && url.contains("#"))
    		url += "/";
    	// remove anchor-links
    	if ("".equals(url) || "/".equals(url) || url.startsWith("#") || 
    			url.startsWith("/#") || url.startsWith("javascript") || url.startsWith("mailto"))
    		return null;
    	// change relative links to absolute links
    	else if (url.startsWith("/"))
    	{
    		
    		String parent = parentURL.substring(0, parentURL.lastIndexOf("/")+1);
    		// no sub-directories found
    		if ("http://".equals(parent)  || "https://".equals(parent))
    			parent = parentURL;
    		if (parent.endsWith("/"))
    		{
    			logger.debug("url starts with '/': "+parentURL+" | "+parent+" | "+url.substring(1));
    			return parent+url.substring(1);
    		}
    		else
    		{
    			logger.debug("url starts with '/': "+parentURL+" | "+parent+" | "+url);
        		return parent+url;
    		}
    	}
    	else if (url.startsWith(".."))
    	{
    		String tmp = url;
    		int numOfDirsUp = 1;
    		if (parentURL.endsWith("/") && 
    				("http:/".equals(parentURL.subSequence(0, parentURL.lastIndexOf("/"))) || 
    				"https:/".equals(parentURL.subSequence(0, parentURL.lastIndexOf("/")))))
    			numOfDirsUp = 0;
    		while (tmp.startsWith(".."))
    		{
    			tmp = tmp.substring(3);
    			numOfDirsUp++;
    		}
    		String parent = parentURL;
    		for (int i=0; i<numOfDirsUp; i++)
    			parent = parent.substring(0, parent.lastIndexOf("/"));
    		
    		if (parent.equals("http:/") || parent.equals("https:/") || 
    				parent.equals("http:") || parent.equals("https:")) // && numOfDirsUp == 1)
    			parent = parentURL;
    		
    		String ret = "";
    		if (!parent.endsWith("/") && !tmp.startsWith("/"))
    			ret = parent+"/"+tmp;
    		else if (parent.endsWith("/") && tmp.startsWith("/"))
    			ret = parent.substring(0, parent.length()-1)+tmp;
    		else
    			ret = parent+tmp;
    		
    		logger.debug("url starts with '..': "+parentURL+" | "+parent+" | "+url + " | "+tmp+" | "+ret);
    		
    		return ret;
    	}
    	else if (!url.startsWith("http://") && !url.startsWith("https://"))
    	{
    		String parent = parentURL.substring(0, parentURL.lastIndexOf("/")+1);
    		// no sub-directories found
    		if ("http://".equals(parent) || "https://".equals(parent))
    			parent = parentURL;
    		
    		if (parent.endsWith("/") && url.startsWith("/"))
    		{
    			if (logger.isDebugEnabled())
    				logger.debug("url starts not with 'http://': "+parentURL+" | "+parent+" | "+url);
        		return parent+url.substring(1);
    		}
    		else if (!parentURL.endsWith("/") && !url.startsWith("/"))
    		{
    			if (logger.isDebugEnabled())
    				logger.debug("url starts not with 'http://': "+parentURL+" | "+parent+" | "+url);
        		return parent+"/"+url;
    		}
    		else
    		{
    			if (logger.isDebugEnabled())
    				logger.debug("url starts not with 'http://': "+parentURL+" | "+parent+" | "+url);
        		return parent+url;
    		}
    	}
    	// url is in absolute format
    	else // if (url.startsWith("http://") || url.startsWith("https://"))
    	{
    		if (logger.isDebugEnabled())
    			logger.debug("found url: "+url);
            return url;
    	}
	}
}
