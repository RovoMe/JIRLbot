package at.rovo.WebCrawler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import at.rovo.caching.drum.DrumException;
import at.rovo.caching.drum.IDrumListener;
import at.rovo.caching.drum.event.DrumEvent;
import at.rovo.caching.drum.util.DrumUtil;
import at.rovo.caching.drum.util.NamedThreadFactory;

/**
 * <p>IRLbot is the web crawler published by Lee, Leonard, Wang and Loguinov in
 * their paper 'IRLbot: Scaling to 6 Billion Pages and Beyond'</p>
 * <p>A web crawler takes a set of seed pages which it first reads and then searches 
 * for further links of other pages. It collects those links in a queue and removes
 * those URLs from the queue it is currently reading and analyzing. It repeats those
 * steps until no further new sites can be found or the process is stopped via a  
 * {@link #stop()} method call.</p>
 * <p>This web crawler uses the disk repository with update management (DRUM) 
 * architecture to efficiently store the list of already seen URLs, the extracted 
 * robot.txt files as well as the comparison of URLs with the rules defined in the 
 * robot.txt file to provide politeness and a spam-filter and a budgeting algorithm
 * (STAR and BEAST).</p>
 * 
 * @author Roman Vottner
 */
public class IRLbot implements Runnable, UniqueUrlListener, CheckSpamUrlListener, 
	BEASTBudgetPassedListener, RobotsCachePassedListener, RobotsRequestedListener,
	IDrumListener
{
	// create a logger
	private final static Logger logger = LogManager.getLogger(IRLbot.class.getName()); 
		
	/** Contains the addresses of pages that need to be crawled **/
	private List<String> toCrawl = null;
	/** The mapping of URLs and the parsed links in that source **/
	private Map<String, Future<CrawledPage>> waitingList = null;
	/** Specifies if the crawling should stop **/
	private volatile boolean stopRequested = false; // Effective Java 2nd Edition, Item 66 - 
	                                                // synchronize solely for its communication effects, 
	                                                // not for mutual exclusion locking
	/** Specifies the number of worker threads used for crawling pages **/
	private int numCrawlThreads = 0;
	/** Specifies the number of robot.txt downloader threads **/
	private int numRobotsDownloadThreads = 0;
	/** A reader needed to fetch robots.txt files from the web **/
	private UrlReader reader = null;
	
	/** DRUM object representing the list of already crawled URLs **/
	private URLseen urlSeen = null; 
	/** DRUM object representing the STAR-algorithm to check for spam link-farms 
	 * and reputation**/
	private STAR pldIndegree = null;
	/** Budget Enforcement with Anti-Spam Tactics structure implementation is used
	 * to check the budget of any URL against their pay level domain**/
	private BEAST beast = null;
	/** DRUM object representing the robots cache which decides if a certain URL
	 * is eligible to be crawled according its hosts robots.txt file. If no 
	 * robots.txt file could be found for a host it requests a download for it **/
	private RobotsCache robotsCache = null;
	/** DRUM object that keeps track of the hosts that requested a robots.txt file **/
	private RobotsRequested robotsRequested = null;
	
	private BlockingQueue<String> robotsCheckQueue = null;
	private BlockingQueue<String> robotsRequestQueue = null;
	private BlockingQueue<String> robotsDownloadQueue = null;
	
	private Thread robotsCheckQueueEmptier = null;
	private Thread robotsRequestedQueueEmptier = null;
	
	private ExecutorService downloadExecutor = null;
	
	private Object lock = new Object();
	
	private Set<IRLbotListener> listeners = new CopyOnWriteArraySet<>(); 
	
	private AtomicLong numPagesCrawledTotal = new AtomicLong();
	private AtomicLong numPagesCrawledSuccess = new AtomicLong();
	
	
	/**
	 * <p>Instantiates a new object of a web crawler as presented by Lee, Leonard, 
	 * Wang and Loguinov in their paper 'IRLbot: Scaling to 6 Billion Pages and Beyond'.</p>
	 * <p>A web crawler </p>
	 * 
	 * @param numThreads The number of working crawlers used by this instance
	 */
	public IRLbot(int numThreads)
	{
		this.init(numThreads, 10, 128, 16, 16, 16, 1024, 64, 256, 32);
	}
	
	/**
	 * <p>Instantiates a new object of a web crawler as presented by Lee, Leonard, 
	 * Wang and Loguinov in their paper 'IRLbot: Scaling to 6 Billion Pages and Beyond'.</p>
	 * <p>A web crawler </p>
	 * 
	 * @param seedPages The initial seed pages to start the crawl from
	 */
	public IRLbot(String[] seedPages)
	{
		this.init(5, 10, 256, 16, 16, 16, 1024, 64, 256, 64);
		
		for (String seedPage : seedPages)
			this.toCrawl.add(seedPage);
	}
	
	/**
	 * <p>Instantiates a new object of a web crawler as presented by Lee, Leonard, 
	 * Wang and Loguinov in their paper 'IRLbot: Scaling to 6 Billion Pages and Beyond'.</p>
	 * <p>A web crawler </p>
	 * 
	 * @param seedPages The initial seed pages to start the crawl from
	 * @param numThreads The number of working crawlers used by this instance
	 */
	public IRLbot(String[] seedPages, int numThreads)
	{
		this.init(numThreads, 10, 256, 16, 16, 16, 1024, 64, 256, 64);
		
		for (String seedPage : seedPages)
			this.toCrawl.add(seedPage);
	}
	
	/**
	 * <p>Instantiates a new object of a web crawler as presented by Lee, Leonard, 
	 * Wang and Loguinov in their paper 'IRLbot: Scaling to 6 Billion Pages and Beyond'.</p>
	 * <p>A web crawler </p>
	 * 
	 * @param seedPages The initial seed pages to start the crawl from
	 * @param numCrawlThreads The number of working crawlers used by this instance
	 * @param numRobotsDownloadThreads The number of working robot.txt download threads
	 * @param numURLseenBuckets
	 * @param numSTARbuckets
	 * @param numRobotsCacheBuckets
	 * @param numRobotsRequestedBuckets
	 */
	public IRLbot(String[] seedPages, int numCrawlThreads, int numRobotsDownloadThreads,
			int numURLseenBuckets, int numSTARbuckets, int numRobotsCacheBuckets, int numRobotsRequestedBuckets)
	{
		this.init(numCrawlThreads, numRobotsDownloadThreads,
				numURLseenBuckets, numSTARbuckets, numRobotsCacheBuckets, numRobotsRequestedBuckets,
				1024, 64, 256, 64);
		
		for (String seedPage : seedPages)
			this.toCrawl.add(seedPage);
	}
	
	/**
	 * <p>Instantiates a new object of a web crawler as presented by Lee, Leonard, 
	 * Wang and Loguinov in their paper 'IRLbot: Scaling to 6 Billion Pages and Beyond'.</p>
	 * <p>A web crawler </p>
	 * 
	 * @param seedPages The initial seed pages to start the crawl from
	 * @param numThreads The number of working crawlers used by this instance
	 * @param numRobotsDownloadThreads The number of working robot.txt download threads
	 * @param numURLseenBuckets
	 * @param numSTARbuckets
	 * @param numRobotsCacheBuckets
	 * @param numRobotsRequestedBuckets
	 * @param URLseenBytes
	 * @param STARbytes
	 * @param RobotsCacheBytes
	 * @param RobotsRequestedBytes
	 */
	public IRLbot(String[] seedPages, int numCrawlThreads, int numRobotsDownloadThreads,
			int numURLseenBuckets, int numSTARbuckets, int numRobotsCacheBuckets, int numRobotsRequestedBuckets, 
			int URLseenBytes, int STARbytes, int RobotsCacheBytes, int RobotsRequestedBytes)
	{
		this.init(numCrawlThreads, numRobotsDownloadThreads,
				numURLseenBuckets, numSTARbuckets, numRobotsCacheBuckets, numRobotsRequestedBuckets,
				URLseenBytes, STARbytes, RobotsCacheBytes, RobotsRequestedBytes);
		
		for (String seedPage : seedPages)
			this.toCrawl.add(seedPage);
	}
	
	/**
	 * <p>Initializes required variables for the crawling process</p>
	 * 
	 * @param seedPages An array of absolute URLs as {@link String}s which
	 *                  correspond to the web pages to start crawling from
	 * @param numThreads The number of concurrent threads to use for the
	 *                   crawling process
	 */
	private void init(int numCrawlThreads, int numRobotsDownloadThreads,
			int numURLseenBuckets, int numSTARbuckets, int numRobotsCacheBuckets, int numRobotsRequestedBuckets, 
			int URLseenBytes, int STARbytes, int RobotsCacheBytes, int RobotsRequestedBytes)
	{
		this.toCrawl = new ArrayList<>();
		this.numCrawlThreads = numCrawlThreads;
		this.numRobotsDownloadThreads = numRobotsDownloadThreads;
		this.reader = new UrlReader();
		
		this.waitingList = new ConcurrentHashMap<>();
		
		// initialization of the urlSeen part
		UniqueUrlDispatcher uniqueURLDispatcher = new UniqueUrlDispatcher();
		uniqueURLDispatcher.addUniqueUrlListener(this);
		try
		{
			this.urlSeen = new URLseen(uniqueURLDispatcher, numURLseenBuckets, URLseenBytes, this);
		}
		catch (DrumException dEx)
		{
			if (logger.isErrorEnabled())
				logger.error(dEx.getMessage());
			dEx.printStackTrace();
			System.exit(1);
		}
		
		// initialization of the STAR part 
		try
		{
			this.pldIndegree = new STAR(numSTARbuckets, STARbytes, this);
		}
		catch (DrumException dEx)
		{
			if (logger.isErrorEnabled())
				logger.error(dEx.getMessage());
			dEx.printStackTrace();
			System.exit(1);
		}
		this.pldIndegree.addCheckSpamUrlListener(this);
		this.beast = new BEAST();
		this.beast.addBEASTBudgetPassedListener(this);
		
		// initialize the RobotsCache part
		RobotsCacheDispatcher robotsCacheDispatcher = new RobotsCacheDispatcher();
		robotsCacheDispatcher.addRobotsCachePassedListener(this);
		try
		{
			this.robotsCache = new RobotsCache(robotsCacheDispatcher, numRobotsCacheBuckets, RobotsCacheBytes, this);
		}
		catch (DrumException dEx)
		{
			if (logger.isErrorEnabled())
				logger.error(dEx.getMessage());
			dEx.printStackTrace();
			System.exit(1);
		}
		
		// initialize the RobotsRequested part
		RobotsRequestedDispatcher robotsRequestedDispatcher = new RobotsRequestedDispatcher();
		robotsRequestedDispatcher.addRobotsRequestedListener(this);
		try
		{
			this.robotsRequested = new RobotsRequested(robotsRequestedDispatcher, numRobotsRequestedBuckets, RobotsRequestedBytes, this);
		}
		catch (DrumException dEx)
		{
			if (logger.isErrorEnabled())
				logger.error(dEx.getMessage());
			dEx.printStackTrace();
			System.exit(1);
		}
		
		this.robotsCheckQueue = new LinkedBlockingQueue<>();
		this.robotsRequestQueue = new LinkedBlockingQueue<>();
		this.robotsDownloadQueue = new LinkedBlockingQueue<>();
		
		this.robotsCheckQueueEmptier = new Thread(new RobotsCheckQueueEmptier());
		this.robotsCheckQueueEmptier.setName("Robots Check Queue Emptier");
		this.robotsCheckQueueEmptier.start();
				
		this.robotsRequestedQueueEmptier = new Thread(new RobotsRequestedQueueEmptier());
		this.robotsRequestedQueueEmptier.setName("Robots Request Queue Emptier");
		this.robotsRequestedQueueEmptier.start();
				 
		NamedThreadFactory factory = new NamedThreadFactory();
		factory.setName("robotsTxtDownloader");
		this.downloadExecutor = Executors.newFixedThreadPool(this.numRobotsDownloadThreads, factory);
		for (int i=0; i<this.numRobotsDownloadThreads; i++)
		{
			this.downloadExecutor.submit(new DownloadRobotsTxtExecutor());
		}
	}
		
	/**
	 * <p>Adds a URL to the {@link List} of pages to crawl.</p>
	 * 
	 * @param url The URL to add to the list of pages to crawl
	 */
	public void addURL(String url)
	{
		if (!toCrawl.contains(url))
			this.toCrawl.add(url);
	}
	
	/**
	 * <p>Adds a {@link Collection} of URLs to the {@link List} of pages to crawl.</p>
	 * 
	 * @param urls The URLs to att th the list of pages to crawl.
	 */
	public void addURL(Collection<String> urls)
	{
		for (String url : urls)
		{
			if (!toCrawl.contains(url))
			this.toCrawl.add(url);
		}
	}
	
	/**
	 * <p>Adds a new IRLbotListener element to the instance.</p>
	 * 
	 * @param listener An object that needs to be informed of changes.
	 */
	public void addIRLbotListener(IRLbotListener listener)
	{
		this.listeners.add(listener);
	}
	
	/**
	 * <p>Removes an object from the set of elements to be notified on changes.</p>
	 * 
	 * @param listener The object to unregister
	 */
	public void removeIRLbotListener(IRLbotListener listener)
	{
		this.listeners.remove(listener);
	}
	
	private void informOnNumURLsCrawledTotalChanged(long newSize)
	{
		for (IRLbotListener listener : this.listeners)
			listener.numberOfURLsCrawledTotalChanged(newSize);
	}
	
	private void informOnNumURLsCrawledSuccessChanged(long newSize)
	{
		for (IRLbotListener listener : this.listeners)
			listener.numberOfURLsCrawledSuccessChanged(newSize);
	}
	
	private void informOnToCrawlChange(long newSize)
	{
		for (IRLbotListener listener : this.listeners)
			listener.numberOfURLsToCrawlChanged(newSize);
	}
	
	public int getNumberOfURLseenBuckets()
	{
		return this.urlSeen.getNumberOfBuckets();
	}
	
	public int getNumberOfSTARBuckets()
	{
		return this.pldIndegree.getNumberOfBuckets();
	}
	
	public int getNumberOfRobotsCacheBuckets()
	{
		return this.robotsCache.getNumberOfBuckets();
	}
	
	public int getNumberOfRobotsRequestedBuckets()
	{
		return this.robotsRequested.getNumberOfBuckets();
	}
	
	/**
	 * <p>Starts the actual crawl of all the URLs contained in the {@link List} 
	 * of pages to crawl.</p>
	 * <p>This method uses a fixed size thread pool to crawl multiple web pages
	 * at the same time.</p>
	 */
	public void crawl()
	{
		synchronized (this.lock)
		{
			this.lock.notify();
		}
	}
	
	@Override
	public void run()
	{
		synchronized (this.lock)
		{
			try
			{
				this.lock.wait();
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}
		
		NamedThreadFactory crawlerFactory = new NamedThreadFactory();
		crawlerFactory.setName("Crawler");
		ExecutorService executor = Executors.newFixedThreadPool(this.numCrawlThreads, crawlerFactory);
		
		if (logger.isInfoEnabled())
			logger.info("Starting to crawl");
		while(!this.stopRequested)
		{
			if (!toCrawl.isEmpty())
			{
				// fetch the first item from the list of URLs to crawl
				// and format it appropriately
				String pageToCrawl = this.formatURL(this.toCrawl.remove(0));
				this.informOnToCrawlChange(this.toCrawl.size());				
				
				if (pageToCrawl == null)
					continue;
				
				if (logger.isInfoEnabled())
					logger.info("crawling page: "+pageToCrawl);
				CrawlingThread crawler = new CrawlingThread(pageToCrawl, this.pldIndegree);
				this.numPagesCrawledTotal.incrementAndGet();
				this.waitingList.put(pageToCrawl, executor.submit(crawler));
			}
			
			this.checkResults();
			
			this.informOnNumURLsCrawledTotalChanged(this.numPagesCrawledTotal.get());
			this.informOnNumURLsCrawledSuccessChanged(this.numPagesCrawledSuccess.get());
		}
		if (logger.isInfoEnabled())
		{
			logger.info("crawler stopped: "+this.stopRequested);
			logger.info("size of the queue of URLs to read: "+this.toCrawl.size());
		}
		
		// work is done, ensure that all the data in the memory or 
		// in the disk files is synchronized with the backing database
		this.synchronize();
		
		// This will make the executor accept no new threads
		// and finish all existing threads in the queue
		executor.shutdown();
			
		// Wait until all threads are finish
		try
		{
			executor.awaitTermination(1, TimeUnit.DAYS);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		} 
	}
	
	/**
	 * <p>This method adds a '/' to the end of a URL if it either 
	 * has not yet one or it already points to a file (f.e. index.html)
	 * or some web applications or anchors on a site.</p>
	 * 
	 * @param url The URL to add a tailing '/' if it needs one
	 * @return The formated URL
	 */
	private String formatURL(String url)
	{
		if (url == null)
			return null;
		
		if (!url.endsWith("/") && !url.contains("?") 
				&& url.lastIndexOf("/") > url.lastIndexOf(".") 
				&& url.contains("#"))
			url += "/";
		return url;
	}
	
	/**
	 * <p>Runs through every future result and checks if its result is
	 * currently available. If so it fetches the available data (a 
	 * {@link List} of new URLs) and hands the data over to the 
	 * DRUM-structure which keeps track of new and already known URLs.</p>
	 * <p>The result is then removed from the {@link List} of not yet
	 * processed future results.</p>
	 * 
	 * @param results A {@link List} of all {@link Future} results
	 */
	private synchronized void checkResults()
	{
		List<String> removeList = new ArrayList<>();
		for (Future<CrawledPage> result : this.waitingList.values())
		{
			if (result.isDone())
			{
				// Results are ready - get them and send them to the
				// URLseen instance
				try 
				{
					CrawledPage page =  result.get();
					if (page == null)
						continue;
					this.numPagesCrawledSuccess.incrementAndGet();
					
					if (logger.isDebugEnabled())
						logger.info(Thread.currentThread().getName()+" - "+ page.getURL()+" - found: "+page.getContainedURLs().size()+" URLs");
					for (String url : page.getContainedURLs())
					{
						if (logger.isDebugEnabled())
							logger.debug(Thread.currentThread().getName()+" - "+ page.getURL()+" - found: "+url);
						this.urlSeen.checkUpdate(null, url);
					}
					// removing items from a list we are iterating through
					// is not possible so save it until we finished the iteration
					// and then remove them from the actual list
					removeList.add(page.getURL());
				} 
				catch (InterruptedException | ExecutionException e) 
				{
					if (logger.isErrorEnabled())
						logger.error("Error while retrieving callable result! Reason: "+e.getLocalizedMessage(), e);
					e.printStackTrace();
				}
			}
		}
		Future<CrawledPage> remItem = null;
		for (String url : removeList)
		{
			remItem = this.waitingList.remove(url);
			if (remItem == null)
			{
				if (logger.isErrorEnabled())
					logger.error("Invalid url should be removed: url="+url+" "+" keys: "+Arrays.toString(this.waitingList.keySet().toArray()));
			}
		}
	}
	
	public void synchronize()
	{
		try
		{
			this.urlSeen.synchronize();
			this.pldIndegree.synchronize();
			this.robotsCache.synchronize();
			this.robotsRequested.synchronize();
		}
		catch (DrumException dEx)
		{
			if (logger.isErrorEnabled())
				logger.error(dEx.getMessage());
		}
	}
	
	/**
	 * <p>Stops the current crawling process which leads to a synchronization
	 * of the data stored in memory or on in a cache file with the backing database.</p>
	 */
	public void stop()
	{
		this.stopRequested = true;
		
		this.robotsCheckQueueEmptier.interrupt();
		this.robotsRequestedQueueEmptier.interrupt();
		
		try
		{
			this.urlSeen.dispose();
			this.pldIndegree.dispose();
			this.beast.dispose();
			this.robotsCache.dispose();
			this.robotsRequested.dispose();
		}
		catch (DrumException dEx)
		{
			if (logger.isErrorEnabled())
				logger.error(dEx.getMessage());
		}
		
		// This will make the executor accept no new threads
		// and finish all existing threads in the queue
		this.downloadExecutor.shutdown();
					
		// Wait until all threads are finish
		try
		{
			this.downloadExecutor.awaitTermination(1, TimeUnit.MINUTES);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		} 

		
		if (logger.isInfoEnabled())
			logger.info("Finished crawling, all threads shutdown");
	}

	@Override
	public void handleUniqueURL(String url) 
	{
		// Unique URLs arriving from URLseen perform a batch check against PLDindegree
		if (logger.isDebugEnabled())
			logger.debug("sending to STAR budget check: "+url);
		this.pldIndegree.check(url);
	}
	
	@Override
	public void handleSpamCheck(String url, int budget) 
	{
		// Unique URLs and their budget arriving form STAR which need to be forwarded to BEAST
		if (logger.isDebugEnabled())
			logger.debug("sending to BEAST - url: "+url+" | budget: "+budget);
		this.beast.checkBudgetOfURL(url, budget);
	}
	
	@Override
	public void handleBudgetPassed(String url) 
	{
		if (logger.isDebugEnabled())
			logger.debug("sending URL to RobotsCache: "+url);
		this.robotsCheckQueue.add(url);
	}
	
	@Override
	public void handleUnableToCheck(String url) 
	{
		// no robots.txt available for this URL yet - request one by adding the PLD to 
		// RobotsRequest.checkUpdate(url). 
		String hostName = Util.getHostname(url);
		this.robotsRequestQueue.add(hostName);
		// Add the URL to the back of the robotsCheckQueue again
		this.robotsCheckQueue.add(url);
	}
	
	@Override
	public void handleNewRobotRequests(String hostName) 
	{
		if (logger.isDebugEnabled())
			logger.debug("requesting robots.txt download for host: "+hostName);
		this.robotsDownloadQueue.add(hostName);
		for (IRLbotListener listener : this.listeners)
			listener.sizeOfRobotTxtDownloadQueue(this.robotsDownloadQueue.size());
	}
	
	@Override
	public void handleURLsPassed(String url) 
	{
		if (logger.isDebugEnabled())
			logger.debug("adding "+url+" to the list of URLs to crawl!");
		this.toCrawl.add(url);		
		this.informOnToCrawlChange(this.toCrawl.size());
	}
		
	private class RobotsCheckQueueEmptier implements Runnable
	{
		@Override
		public void run() 
		{
			while (!stopRequested)
			{
				try
				{
					robotsCache.check(robotsCheckQueue.take());
				}
				catch (InterruptedException e)
				{

				}
			}
		}
	}
	
	private class RobotsRequestedQueueEmptier implements Runnable
	{
		@Override
		public void run() 
		{
			while (!stopRequested)
			{
				try
				{
					robotsRequested.request(null, robotsRequestQueue.take());
				}
				catch (InterruptedException e)
				{
					
				}
			}
		}
	}
	
	private class DownloadRobotsTxtExecutor implements Runnable
	{
		@Override
		public void run() 
		{
			while (!stopRequested)
			{
				String hostName = null;
				try
				{
					hostName = robotsDownloadQueue.take();
					for (IRLbotListener listener : listeners)
						listener.sizeOfRobotTxtDownloadQueue(robotsDownloadQueue.size());
					if (hostName != null)
					{
						String robotsFile = reader.readPage(hostName+"/robots.txt");
						if (robotsFile != null && !"".equals(robotsFile))
						{
							HostData hostData = new HostData(hostName, null, null);
							// avoid HTML pages which just return "no robots.txt"
							if (robotsFile.toLowerCase().contains("user-agent:"))
								hostData.setRobotsTxt(robotsFile);
							robotsCache.update(DrumUtil.hash(hostName), hostData);
							if (logger.isDebugEnabled())
								logger.debug("Received robots.txt for host: "+hostName+"; content: '"+robotsFile+"'");
						}
						else
						{
							if (logger.isErrorEnabled())
								logger.error("Could not download robots.txt file for host: "+hostName);
							robotsCache.update(DrumUtil.hash(hostName), new HostData(hostName, null, null));
						}
					}
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}	
		}
	}

	@Override
	public void update(DrumEvent<? extends DrumEvent<?>> event)
	{
		for (IRLbotListener listener : this.listeners)
			listener.drumUpdate(event);
	}
}
