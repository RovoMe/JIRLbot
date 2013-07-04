package at.rovo.WebCrawler;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import at.rovo.caching.drum.NullDispatcher;
import at.rovo.caching.drum.data.StringSerializer;

public class RobotsCacheDispatcher extends NullDispatcher<HostData, StringSerializer>
{
	// create a logger
	private final static Logger logger = LogManager.getLogger(RobotsCacheDispatcher.class.getName());
	
	private List<RobotsCachePassedListener> listeners = null;
	
	public RobotsCacheDispatcher()
	{
		// all write operations make a fresh copy of the entire underlying array
		// iteration require no locking and is very fast
		this.listeners = new CopyOnWriteArrayList<RobotsCachePassedListener>();
	}
	
	public void addRobotsCachePassedListener(RobotsCachePassedListener listener)
	{
		if (!this.listeners.contains(listener))
			this.listeners.add(listener);
	}
	
	public void removeRobotsCachePassedListener(RobotsCachePassedListener listener)
	{
		if (this.listeners.contains(listener))
			this.listeners.remove(listener);
	}
	
	/**
	 * <p>URLs which have already a robots.txt stored for their pay level
	 * domain are returning a duplicate key. Check the returned host data
	 * if the URL is allowed to be crawled.</p>
	 */
	@Override
	public void duplicateKeyCheck(Long key, HostData hostData, StringSerializer url) 
	{
		logger.debug("Checking compliance with robots.txt rules: "+url);
		if (this.isAllowedToPass(url.getData(), hostData))
		{
			logger.debug("URL passed tests!");
			for (RobotsCachePassedListener listener : this.listeners)
				listener.handleURLsPassed(url.getData());
		}
	}

	/**
	 * <p>No robots.txt file could be found for this URL yet</p>
	 */
	@Override
	public void uniqueKeyCheck(Long key, StringSerializer url) 
	{
		logger.debug("No robots.txt found for "+url+" inside DRUM!");
		for (RobotsCachePassedListener listener : this.listeners)
			listener.handleUnableToCheck(url.getData());
	}
	
	/**
	 * <p>Checks if a URL is allowed to be crawled based on the hosts
	 * robots.txt defined rules.</p>
	 * 
	 * @param url The URL to be checked against the rules of the hosts robots.txt file
	 * @param hostData The host data object which contains the robots.txt rules as String
	 * @return Returns true if the URL has passed the robots.txt rules test, false if
	 *         a rule in the robots.txt file prevents crawling
	 */
	private boolean isAllowedToPass(String url, HostData hostData)
	{
		if (hostData == null || hostData.getRobotsTxt() == null || hostData.getRobotsTxt().equals(""))
			return true;
		
		url = Util.getDirectoryPathOfUrl(url);
		
		List<String> allowedSites = new ArrayList<String>();
		List<String> disallowedSites = new ArrayList<String>();
		String crawlDelay = "";
		
		this.parseRobotsTxt(hostData.getRobotsTxt(), disallowedSites, allowedSites, crawlDelay);
		return this.checkRobotRules(url, disallowedSites, allowedSites);
	}		
	
	/**
	 * <p>Parses a robots.txt {@link String} representation and fills the provided 
	 * arguments with rules collected from the robots.txt file.</p>
	 * 
	 * @param robotsTxt The {@link String} representation of the robots.txt file
	 * @param disallowedSites A {@link List} that will hold all preventing-rules 
	 *                        which matches the IRLbot or the generalized '*' 
	 *                        user-agent
	 * @param allowedSites A {@link List} that will hold all allowing-rules which 
	 *                     matches the IRLbot or the generalized '*' user-agent
	 * @param crawlDelay A {@link String} that will hold the delay between to page
	 *                   crawls of the same host                   
	 */
	private void parseRobotsTxt(String robotsTxt, List<String> disallowedSites, List<String> allowedSites, String crawlDelay)
	{	
		Scanner scanner = new Scanner(robotsTxt);		
		while (scanner.hasNextLine())
		{
			String line = scanner.nextLine();
			
			if (line.startsWith("#"))
				continue;
			
			String userAgent = "";
			
			// look for a 'User-agent: ' part
			if (line.toLowerCase().startsWith("user-agent: "))
			{
				userAgent = line.toLowerCase().substring("user-agent: ".length());
				
				// if it either equals IRLbot or the generality '*' extract their
				// defined values
				if ("*".equals(userAgent.trim()) || "irlbot".equals(userAgent.trim()))
				{
					while (scanner.hasNextLine())
					{
						line = scanner.nextLine();
						
						if (line.toLowerCase().startsWith("allow: "))
						{
							String allow = line.substring("allow:".length()).trim();
							// as comments are possible at the end of the line,
							// filter them out
							if (allow.contains("#"))
								allow = allow.substring(0,allow.indexOf("#")).trim();
							allowedSites.add(allow);
						}
						else if (line.toLowerCase().startsWith("disallow: "))
						{
							String disallow = line.substring("disallow:".length()).trim();
							if (disallow.contains("#"))
								disallow = disallow.substring(0, disallow.indexOf("#")).trim();
							disallowedSites.add(disallow);
						}
						else if (line.toLowerCase().startsWith("drawl-delay: "))
						{
							crawlDelay = line.substring("Crawl-delay:".length()).trim();
						}
						else if ("".equals(line.trim()) || "\n".equals(line.trim()))
							break;
					}
				}	
			}
		}
		scanner.close();
	}
	
	/**
	 * <p>Checks a URL against rules extracted from a robots.txt file</p>
	 * 
	 * @param url The URL to test against the rules
	 * @param disallowedSites A {@link List} of preventing rules
	 * @param allowedSites A {@link List} of allowing rules
	 * @return Returns true if a URL passed the tests against the rules, 
	 *         false otherwise
	 */
	private boolean checkRobotRules(String url, List<String> disallowedSites, List<String> allowedSites)
	{
		boolean isAllowed = true;
		// evaluate settings and compare them with the URL
		for (String disallow : disallowedSites)
		{
			// '' allows the whole host to be crawled
			if (disallow.trim().equals(""))
				isAllowed = true;
			// '/' prevents the whole host from being crawled
			else if (disallow.trim().equals("/"))
				isAllowed = false;
			// endline-anchor indicates that the part before the "$" sign needs to be
			// at the end of the URL
			else if (disallow.endsWith("$"))
			{
				String pathRestriction = disallow.substring(0, disallow.length()-1);
				// rule is valid for any URL
				if (pathRestriction.startsWith("/*"))
				{
					pathRestriction = pathRestriction.substring(2);
					if (url.endsWith(pathRestriction))
						isAllowed = false;
				}
				// there are some restrictions for the rule
				else if (pathRestriction.startsWith("/"))
				{
					// there is a wildcard in the middle of the URL, so we have to
					// check the first and the end part of the URL
					if (pathRestriction.contains("*"))
					{
						String startPart = pathRestriction.substring(0,pathRestriction.indexOf("*"));
						String endPart = pathRestriction.substring(pathRestriction.indexOf("*")+1);
						if (url.startsWith(startPart) && url.endsWith(endPart))
							isAllowed = false;
					}
					// a path without asterix was found - we can use the whole thing
					// from start on
					else
					{
						if (url.startsWith(pathRestriction))
							isAllowed = false;
					}
				}
			}
			// ignore URLs that contain a '?'
			else if (disallow.endsWith("?"))
			{
				String pathRestriction = disallow.substring(0, disallow.length()-1);
				if (pathRestriction.startsWith("/*"))
				{
					String part = pathRestriction.substring(2);
					if (part != null && part.trim().length() > 0)
					{
						// should match a rule like /*mnp_utility.mspx?
						if (url.contains(part) && url.contains("?"))
							isAllowed = false;
					}
				}
				else if (pathRestriction.startsWith("/"))
				{
					// there is a wildcard in the middle of the URL, so we have to
					// check the first and the end part of the URL
					// should match f.e. a rule like /hpc/*/supported-applications.aspx?
					if (pathRestriction.contains("*"))
					{
						String startPart = pathRestriction.substring(0,pathRestriction.indexOf("*"));
						String endPart = pathRestriction.substring(pathRestriction.indexOf("*")+1);
						if (url.startsWith(startPart) && url.contains(endPart) && url.contains("?"))
							isAllowed = false;
					}
				}
			}
			// 'ordinary' path provided
			else
			{
				if (disallow.contains("*"))
				{
					String firstPart = disallow.substring(0, disallow.indexOf("*"));
					String secondPart = disallow.substring(disallow.indexOf("*")+1);
					if (url.startsWith(firstPart) && url.contains(secondPart))
						isAllowed = false;
				}
				else
				{
					if (url.startsWith(disallow))
						isAllowed = false;
				}
			}
		}
		
		for (String allow : allowedSites)
		{
			if (allow.endsWith("$"))
			{
				String path = allow.substring(0, allow.length()-1);
				// fetches rules like '/*.html$'
				if (path.startsWith("/*"))
				{
					path = path.substring(2);
					if (path != null && path.length() > 0)
					{
						if (url.endsWith(path))
							isAllowed = true;
					}
				}
				// fetches rules like '/articles/*/index.html$
				else if (path.startsWith("/") && path.contains("*"))
				{
					String startPart = path.substring(0, path.indexOf("*"));
					String endPart = path.substring(path.indexOf("*")+1);
					if (url.startsWith(startPart) && url.contains(endPart))
						isAllowed = true;
				}
			}
			else if (allow.endsWith("?"))
			{
				String path = allow.substring(0, allow.length()-1);
				// fetches rules like '/*search.php?'
				if (path.startsWith("/*"))
				{
					path = path.substring(2);
					if (path != null && path.length() > 0)
					{
						if (url.contains(path) && url.contains("?"))
							isAllowed = true;
					}
				}
				// fetches rules like '/public/*/search.php?
				else if (path.startsWith("/"))
				{
					String startPart = path.substring(0, path.indexOf("*"));
					String endPart = path.substring(path.indexOf("*")+1);
					if (url.startsWith(startPart) && url.contains(endPart) && url.contains("?"))
						isAllowed = true;
				}
			}
			// fetches rules like '/public/*/index.html'
			else if (allow.startsWith("/") && allow.contains("*"))
			{
				String startPart = allow.substring(0, allow.indexOf("*"));
				String endPart = allow.substring(allow.indexOf("*")+1);
				if (url.startsWith(startPart) && url.contains(endPart))
					isAllowed = true;
			}
			// fetches rules like '/default'
			else if (url.startsWith(allow))
				isAllowed = true;
		}
		
		return isAllowed;
	}
}
