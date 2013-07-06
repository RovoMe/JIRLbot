package at.rovo.crawler;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import at.rovo.caching.drum.util.DrumUtil;
import at.rovo.common.UrlReader;
import at.rovo.crawler.bean.CrawledPage;
import at.rovo.crawler.util.IRLbotUtil;

/**
 * <p>
 * This class is a worker object to read and parse a web page for further URLs
 * in a concurrent way. It implements {@link Callable} to return a {@link List}
 * of URLs in {@link String}-form after the web resource has been parsed
 * completely.
 * </p>
 * <p>
 * It further provides a mechanism to ensure that only absolute URLs are
 * returned and it therefore discards links to the same page (f.e. as anchor) or
 * to javascript or mailto tags.
 * </p>
 * 
 * @author Roman Vottner
 */
public class CrawlingThread implements Callable<CrawledPage>
{
	/** The logger of this class **/
	private final static Logger logger = LogManager.getLogger(CrawlingThread.class); 
	
	/** The absolute URL of a web page **/
	private String url = null;
	/** A reference to the STAR structure to update the PLD-PLD link graph **/
	private STAR pldIndegree = null;
	
	/**
	 * <p>
	 * Instantiates a new {@link Callable} object which later uses the provided
	 * URL to read a web page and extracts all its links
	 * </p>
	 * 
	 * @param url
	 *            The absolute URL of a web resource
	 * @param pldIndegree
	 *            The spam tracking and avoidance through reputation algorithm,
	 *            which needs to be batch updated with all found URLs
	 */
	public CrawlingThread(String url, STAR pldIndegree)
	{
		this.url = url;
		this.pldIndegree = pldIndegree;
	}

	/**
	 * <p>
	 * Reads the web page the URL provided in the Constructor points to and
	 * extracts all links in the read web page. Those links are then collected
	 * and returned.
	 * </p>
	 * 
	 * @return All contained absolute links in the web page the URL is pointing
	 *         to
	 */
	@Override
	public CrawledPage call() throws Exception 
	{
		Set<String> foundURLs = new LinkedHashSet<>();
		Set<String> uniquePLDs = new LinkedHashSet<String>();
		
		// read the web page
		UrlReader reader = new UrlReader();
		String webPage = reader.readPage(this.url);
		String baseURL = this.url;
		// if the web page could not be read, return null
		if (webPage == null)
			return null;
		// due to redirects the real URL may be hidden behind an origin URL
		// the real URL may only be learned after following the redirect directives
		this.url = reader.getRealURL();
		
		 String originPLD = IRLbotUtil.getPLDofURL(this.url);
	     if (originPLD == null)
	      	throw new Exception("could not extract PLD of URL: "+this.url+"; baseURL: "+baseURL);
	     
	    logger.debug("{} {}: PLD: {} ({})", Thread.currentThread().getName(), this.url, originPLD, DrumUtil.hash(originPLD));
		
		// find all links inside the page
		Pattern pattern = Pattern.compile("<[aA] [hH][rR][eE][fF]=\"(.*?)\"");
		// remove scripts
		webPage = webPage.replaceAll("<[sS][cC][rR][iI][pP][tT](.*?)</[sS][cC][rR][iI][pP][tT]>", "<script></script>");
		// remove comments
		webPage = webPage.replaceAll("<!--(.*?)-->", "");
		
		Matcher matcher = pattern.matcher(webPage);
        while (matcher.find()) 
        {
        	// extract URLs
        	String _url = matcher.group(1);
        	String validURL = "";
        	try
        	{
            	validURL = IRLbotUtil.checkAndTransformURL(_url, this.url);
	        	if (validURL != null)
	        	{
	        		String PLD = IRLbotUtil.getPLDofURL(validURL);
	        		if (PLD != null)
	        		{
	        			foundURLs.add(validURL);
	        			// aggregate PLD-PLD link information and send it to a DRUM structure
	        			uniquePLDs.add(PLD);
	        			logger.debug("{} "+"\tURL found: {} PLD: {}", Thread.currentThread().getName(), validURL, PLD);
	        		}
	        	}
        	}
        	catch (Exception e)
        	{
        		logger.error("Error extracting URLs from: {} - matcher: {}, validated to: {}! Reason: {}", this.url, _url, validURL, e.getLocalizedMessage());
        		logger.catching(e);
        		continue;
        	}
        }
       
        this.pldIndegree.update(originPLD, uniquePLDs);

        CrawledPage page = new CrawledPage(baseURL, foundURLs);
		return page;
	}
}
