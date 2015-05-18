package at.rovo.crawler;

import at.rovo.caching.drum.DrumException;
import at.rovo.caching.drum.DrumListener;
import at.rovo.caching.drum.event.DrumEvent;
import at.rovo.caching.drum.util.DrumUtils;
import at.rovo.caching.drum.util.NamedThreadFactory;
import at.rovo.common.UrlReader;
import at.rovo.crawler.bean.CrawledPage;
import at.rovo.crawler.bean.HostData;
import at.rovo.crawler.interfaces.BEASTBudgetPassedListener;
import at.rovo.crawler.interfaces.CheckSpamUrlListener;
import at.rovo.crawler.interfaces.IRLbotListener;
import at.rovo.crawler.interfaces.RobotsCachePassedListener;
import at.rovo.crawler.interfaces.RobotsRequestedListener;
import at.rovo.crawler.interfaces.UniqueUrlListener;
import at.rovo.crawler.util.DelayedCrawlUrl;
import at.rovo.crawler.util.IRLbotUtils;
import de.jkeylockmanager.manager.KeyLockManager;
import de.jkeylockmanager.manager.KeyLockManagers;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * IRLbot is the web crawler published by Lee, Leonard, Wang and Loguinov in their paper 'IRLbot: Scaling to 6 Billion
 * Pages and Beyond'
 * <p>
 * A web crawler takes a set of seed pages which it first reads and then searches for further links of other pages. It
 * collects those links in a queue and removes those URLs from the queue it is currently reading and analyzing. It
 * repeats those steps until no further new sites can be found or the process is stopped via a {@link #stop()} method
 * call.
 * <p>
 * This web crawler uses the disk repository with update management (DRUM) architecture to efficiently store the list of
 * already seen URLs, the extracted robot.txt files as well as the comparison of URLs with the rules defined in the
 * robot.txt file to provide politeness and a spam-filter and a budgeting algorithm (STAR and BEAST).
 *
 * @author Roman Vottner
 */
@SuppressWarnings("unused")
public class IRLbot implements Runnable, UniqueUrlListener, CheckSpamUrlListener, BEASTBudgetPassedListener,
        RobotsCachePassedListener, RobotsRequestedListener, DrumListener
{
    /** The logger of this class **/
    private final static Logger LOG = LogManager.getLogger(IRLbot.class);
    public final static String CRAWLER_NAME = "JIRLbot/1.0";

    /** Contains the addresses of pages that need to be crawled **/
    private BlockingQueue<DelayedCrawlUrl> toCrawl = null;
    /** Contains the timestamps of the last crawl of a PLD **/
    private Map<String, AtomicLong> pldLastCrawled = new ConcurrentHashMap<>();
    /** The mapping of URLs and the parsed links in that source **/
    private Map<String, Future<CrawledPage>> waitingList = null;
    /** Specifies if the crawling should stop **/
    private volatile boolean stopRequested = false; // Effective Java 2nd Edition, Item 66 -
    // synchronize solely for its communication effects,
    // not for mutual exclusion locking
    /** Specifies the number of worker threads used for crawling pages **/
    private int numCrawlThreads = 0;
    /** A reader needed to fetch robots.txt files from the web **/
    private UrlReader reader = null;

    /** DRUM object representing the list of already crawled URLs **/
    private URLseen urlSeen = null;
    /**
     * DRUM object representing the STAR-algorithm to check for spam link-farms and reputation
     **/
    private STAR pldIndegree = null;
    /**
     * Budget Enforcement with Anti-Spam Tactics structure implementation is used to check the budget of any URL against
     * their pay level domain
     **/
    private BEAST beast = null;
    /**
     * DRUM object representing the robots cache which decides if a certain URL is eligible to be crawled according its
     * hosts robots.txt file. If no robots.txt file could be found for a host it requests a download for it
     **/
    private RobotsCache robotsCache = null;
    /**
     * DRUM object that keeps track of the hosts that requested a robots.txt file
     **/
    private RobotsRequested robotsRequested = null;

    private BlockingQueue<String> robotsCheckQueue = null;
    private BlockingQueue<String> robotsRequestQueue = null;
    private BlockingQueue<String> robotsDownloadQueue = null;

    private Thread robotsCheckQueueEmptier = null;
    private Thread robotsRequestedQueueEmptier = null;

    private ExecutorService downloadExecutor = null;

    private final Object lock = new Object();

    private Set<IRLbotListener> listeners = new CopyOnWriteArraySet<>();

    private AtomicLong numPagesCrawledTotal = new AtomicLong();
    private AtomicLong numPagesCrawledSuccess = new AtomicLong();
    /**
     * As re-queuing URLs is PLD specific a custom locking is needed so that the check can be entered by other PLDs but
     * still avoid running into race-conditions.
     **/
    final KeyLockManager lockManager = KeyLockManagers.newLock();


    /**
     * Instantiates a new object of a web crawler as presented by Lee, Leonard, Wang and Loguinov in their paper
     * 'IRLbot: Scaling to 6 Billion Pages and Beyond'.
     * <p>
     * A web crawler
     *
     * @param numThreads
     *         The number of working crawlers used by this instance
     */
    public IRLbot(int numThreads)
    {
        this.init(numThreads, 10, 128, 16, 16, 16, 1024, 64, 256, 32);
    }

    /**
     * Instantiates a new object of a web crawler as presented by Lee, Leonard, Wang and Loguinov in their paper
     * 'IRLbot: Scaling to 6 Billion Pages and Beyond'.
     * <p>
     * A web crawler
     *
     * @param seedPages
     *         The initial seed pages to start the crawl from
     */
    public IRLbot(String[] seedPages)
    {
        this.init(5, 10, 256, 16, 16, 16, 1024, 64, 256, 64);

        this.addURL(Arrays.asList(seedPages));
    }

    /**
     * Instantiates a new object of a web crawler as presented by Lee, Leonard, Wang and Loguinov in their paper
     * 'IRLbot: Scaling to 6 Billion Pages and Beyond'.
     * <p>
     * A web crawler
     *
     * @param seedPages
     *         The initial seed pages to start the crawl from
     * @param numThreads
     *         The number of working crawlers used by this instance
     */
    public IRLbot(String[] seedPages, int numThreads)
    {
        this.init(numThreads, 10, 256, 16, 16, 16, 1024, 64, 256, 64);

        this.addURL(Arrays.asList(seedPages));
    }

    /**
     * Instantiates a new object of a web crawler as presented by Lee, Leonard, Wang and Loguinov in their paper
     * 'IRLbot: Scaling to 6 Billion Pages and Beyond'.
     * <p>
     * A web crawler
     *
     * @param seedPages
     *         The initial seed pages to start the crawl from
     * @param numCrawlThreads
     *         The number of concurrent threads to use for the crawling process
     * @param numRobotsDownloadThreads
     *         The number of robot.txt download threads
     * @param numURLseenBuckets
     *         The number of buckets used for the already seen URLs
     * @param numSTARbuckets
     *         The number of threads used for STAR
     * @param numRobotsCacheBuckets
     *         The number of buckets used for the robots.txt cache
     * @param numRobotsRequestedBuckets
     *         The number of buckets used for the robots.txt requests
     */
    public IRLbot(String[] seedPages, int numCrawlThreads, int numRobotsDownloadThreads, int numURLseenBuckets,
                  int numSTARbuckets, int numRobotsCacheBuckets, int numRobotsRequestedBuckets)
    {
        this.init(numCrawlThreads, numRobotsDownloadThreads, numURLseenBuckets, numSTARbuckets, numRobotsCacheBuckets,
                  numRobotsRequestedBuckets, 1024, 64, 256, 64);

        this.addURL(Arrays.asList(seedPages));
    }

    /**
     * Instantiates a new object of a web crawler as presented by Lee, Leonard, Wang and Loguinov in their paper
     * 'IRLbot: Scaling to 6 Billion Pages and Beyond'.
     * <p>
     * A web crawler
     *
     * @param seedPages
     *         The initial seed pages to start the crawl from
     * @param numCrawlThreads
     *         The number of concurrent threads to use for the crawling process
     * @param numRobotsDownloadThreads
     *         The number of robot.txt download threads
     * @param numURLseenBuckets
     *         The number of buckets used for the already seen URLs
     * @param numSTARbuckets
     *         The number of threads used for STAR
     * @param numRobotsCacheBuckets
     *         The number of buckets used for the robots.txt cache
     * @param numRobotsRequestedBuckets
     *         The number of buckets used for the robots.txt requests
     * @param URLseenBytes
     *         The bytesize on which a merge of the seen URLs with the backing data store is invoked
     * @param STARbytes
     *         The bytesize on which a merge of the STAR data with the backing data store is invoked
     * @param RobotsCacheBytes
     *         The bytesize on which a merge of the RobotsCache with the backing data store is invoked
     * @param RobotsRequestedBytes
     *         The bytesize on which a merge of the RobotsRequested with the backing data store is invoked
     */
    public IRLbot(String[] seedPages, int numCrawlThreads, int numRobotsDownloadThreads, int numURLseenBuckets,
                  int numSTARbuckets, int numRobotsCacheBuckets, int numRobotsRequestedBuckets, int URLseenBytes,
                  int STARbytes, int RobotsCacheBytes, int RobotsRequestedBytes)
    {
        this.init(numCrawlThreads, numRobotsDownloadThreads, numURLseenBuckets, numSTARbuckets, numRobotsCacheBuckets,
                  numRobotsRequestedBuckets, URLseenBytes, STARbytes, RobotsCacheBytes, RobotsRequestedBytes);

        this.addURL(Arrays.asList(seedPages));
    }

    /**
     * Initializes required variables for the crawling process
     *
     * @param numCrawlThreads
     *         The number of concurrent threads to use for the crawling process
     * @param numRobotsDownloadThreads
     *         The number of robot.txt download threads
     * @param numURLseenBuckets
     *         The number of buckets used for the already seen URLs
     * @param numSTARbuckets
     *         The number of threads used for STAR
     * @param numRobotsCacheBuckets
     *         The number of buckets used for the robots.txt cache
     * @param numRobotsRequestedBuckets
     *         The number of buckets used for the robots.txt requests
     * @param URLseenBytes
     *         The bytesize on which a merge of the seen URLs with the backing data store is invoked
     * @param STARbytes
     *         The bytesize on which a merge of the STAR data with the backing data store is invoked
     * @param RobotsCacheBytes
     *         The bytesize on which a merge of the RobotsCache with the backing data store is invoked
     * @param RobotsRequestedBytes
     *         The bytesize on which a merge of the RobotsRequested with the backing data store is invoked
     */
    private void init(int numCrawlThreads, int numRobotsDownloadThreads, int numURLseenBuckets, int numSTARbuckets,
                      int numRobotsCacheBuckets, int numRobotsRequestedBuckets, int URLseenBytes, int STARbytes,
                      int RobotsCacheBytes, int RobotsRequestedBytes)
    {
        this.toCrawl = new DelayQueue<>();
        this.numCrawlThreads = numCrawlThreads;
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
            LOG.fatal("Could not initialize cache for url-seen instance", dEx);
            System.exit(1);
        }

        // initialization of the STAR part
        try
        {
            this.pldIndegree = new STAR(numSTARbuckets, STARbytes, this);
        }
        catch (DrumException dEx)
        {
            LOG.fatal("Could not initialize cache for STAR instance", dEx);
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
            LOG.fatal("Could not initialize cache for checks against robots.txt files", dEx);
            System.exit(1);
        }

        // initialize the RobotsRequested part
        RobotsRequestedDispatcher robotsRequestedDispatcher = new RobotsRequestedDispatcher();
        robotsRequestedDispatcher.addRobotsRequestedListener(this);
        try
        {
            this.robotsRequested =
                    new RobotsRequested(robotsRequestedDispatcher, numRobotsRequestedBuckets, RobotsRequestedBytes,
                                        this);
        }
        catch (DrumException dEx)
        {
            LOG.fatal("Could not initialize cache for requested downloads of robots.txt files", dEx);
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
        this.downloadExecutor = Executors.newFixedThreadPool(numRobotsDownloadThreads, factory);
        for (int i = 0; i < numRobotsDownloadThreads; i++)
        {
            this.downloadExecutor.submit(new DownloadRobotsTxtExecutor());
        }
    }

    /**
     * Adds a URL to the {@link List} of pages to crawl.
     *
     * @param url
     *         The URL to add to the list of pages to crawl
     */
    public void addURL(String url)
    {
        this.toCrawl.add(new DelayedCrawlUrl(url, 0, TimeUnit.SECONDS));
    }

    /**
     * Adds a {@link Collection} of URLs to the {@link List} of pages to crawl.
     *
     * @param urls
     *         The URLs to att th the list of pages to crawl.
     */
    public void addURL(Collection<String> urls)
    {
        urls.forEach(url -> this.toCrawl.add(new DelayedCrawlUrl(url, 0, TimeUnit.SECONDS)));
    }

    /**
     * Adds a new IRLbotListener element to the instance.
     *
     * @param listener
     *         An object that needs to be informed of changes.
     */
    public void addIRLbotListener(IRLbotListener listener)
    {
        this.listeners.add(listener);
    }

    /**
     * Removes an object from the set of elements to be notified on changes.
     *
     * @param listener
     *         The object to unregister
     */
    public void removeIRLbotListener(IRLbotListener listener)
    {
        this.listeners.remove(listener);
    }

    private void informOnNumURLsCrawledTotalChanged(long newSize)
    {
        this.listeners.forEach(listener -> listener.numberOfURLsCrawledTotalChanged(newSize));
    }

    private void informOnNumURLsCrawledSuccessChanged(long newSize)
    {
        this.listeners.forEach(listener -> listener.numberOfURLsCrawledSuccessChanged(newSize));
    }

    private void informOnToCrawlChange(long newSize)
    {
        this.listeners.forEach(listener -> listener.numberOfURLsToCrawlChanged(newSize));
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
     * Starts the actual crawl of all the URLs contained in the {@link List} of pages to crawl.
     * <p>
     * This method uses a fixed size thread pool to crawl multiple web pages at the same time.
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

        LOG.info("Starting to crawl");
        while (!this.stopRequested)
        {
            if (!toCrawl.isEmpty())
            {
                // fetch the first item from the list of URLs to crawl
                DelayedCrawlUrl delayedUrl;
                try
                {
                    delayedUrl = this.toCrawl.remove();
                }
                catch (NoSuchElementException nseEx)
                {
                    continue;
                }
                String url = delayedUrl.getUrl();
                String pld = IRLbotUtils.getPLDofURL(url);

                if (!this.pldLastCrawled.containsKey(pld))
                {
                    this.pldLastCrawled.put(pld, new AtomicLong(System.currentTimeMillis()));
                }
                // check if we actually crawled the domain recently (within the delay time-frame)
                if (delayedUrl.getPldDelay() > 0)
                {
                    // In case the URL needed to be re-queued skip the rest of the crawl
                    if (lockManager.executeLocked(pld, () -> checkAndRequeue(pld, delayedUrl)))
                    {
                        continue;
                    }
                }
                else
                {
                    // update the timestamp of the last crawl of the PLD
                    this.pldLastCrawled.get(pld).set(System.currentTimeMillis());
                }

                String pageToCrawl = this.formatURL(url);
                this.informOnToCrawlChange(this.toCrawl.size());

                if (pageToCrawl == null)
                {
                    continue;
                }

                LOG.info("crawling page: {}", pageToCrawl);
                CrawlingThread crawler = new CrawlingThread(pageToCrawl, this.pldIndegree);
                this.numPagesCrawledTotal.incrementAndGet();
                this.waitingList.put(pageToCrawl, executor.submit(crawler));
            }
            this.checkResults();

            this.informOnNumURLsCrawledTotalChanged(this.numPagesCrawledTotal.get());
            this.informOnNumURLsCrawledSuccessChanged(this.numPagesCrawledSuccess.get());

        }
        LOG.info("crawler stopped: {}", this.stopRequested);
        LOG.info("size of the queue of URLs to read: {}", this.toCrawl.size());

        // This will make the executor accept no new threads
        // and finish all existing threads in the queue
        executor.shutdown();

        // Wait until all threads are finish
        try
        {
            executor.awaitTermination(1, TimeUnit.MINUTES);
        }
        catch (InterruptedException e)
        {
            LOG.error("Executor was interrupted while terminating all running threads.", e);
        }
    }

    /**
     * Checks if enough time has passed between two URL invocations and re-queues the URL if not enough time has passed
     * according the delay specified in the <em>delayedUrl</em> parameter.
     *
     * @param pld
     *         The pay level domain of the URL to check
     * @param delayedUrl
     *         The {@link DelayedCrawlUrl} object containing the URL to crawl as well as the time delay between two
     *         invocations for the same domain
     *
     * @return true if the URL needed to be re-queued; false otherwise
     */
    private boolean checkAndRequeue(String pld, DelayedCrawlUrl delayedUrl)
    {
        long currentTime = System.currentTimeMillis();
        //        if (LOG.isTraceEnabled())
        {
            LOG.debug(
                    "PLD {} for URL {} has a delay of {} seconds specified - last crawled: {} - current time: {} - diff: {}",
                    pld, delayedUrl.getUrl(), delayedUrl.getPldDelay(), this.pldLastCrawled.get(pld).get(), currentTime,
                    currentTime - this.pldLastCrawled.get(pld).get());
        }
        if (needsReQueueing(pld, delayedUrl, currentTime))
        {
            //            if (LOG.isTraceEnabled())
            {
                LOG.debug("{} - Re-Queue {} - lastCrawl: {} delay: {} seconds currentTime: {}",
                          Thread.currentThread().getName(), delayedUrl.getUrl(), this.pldLastCrawled.get(pld).get(),
                          delayedUrl.getPldDelay(), currentTime);
            }
            // since we last crawled the PLD less then time-frame seconds ago, add the URL back to the queue
            this.toCrawl
                    .add(new DelayedCrawlUrl(delayedUrl.getUrl(), delayedUrl.getPldDelay(), delayedUrl.getTimeUnit()));
            return true;
        }
        else
        {
            //            if (LOG.isTraceEnabled())
            {
                LOG.debug("PLD {} for URL {} was last crawled {} seconds ago", pld, delayedUrl.getUrl(),
                          currentTime - this.pldLastCrawled.get(pld).get());
            }
            // update the timestamp of the last crawl of the PLD
            this.pldLastCrawled.get(pld).set(System.currentTimeMillis());
            return false;
        }
    }

    /**
     * Checks if a URL needs to be re-queued. A URL needs to be re-queued if not enough time between two invocations of
     * pages from the same domain has passed. The time span between two valid invocations is taken from the provided
     * {@link DelayedCrawlUrl} object.
     *
     * @param pld
     *         The pay level domain of the URL to check
     * @param delayedUrl
     *         The {@link DelayedCrawlUrl} object check if it needs to be re-queued
     * @param currentTime
     *         The current timestamp
     *
     * @return true if provided URL needs re-queueing; false otherwise
     */
    private boolean needsReQueueing(String pld, DelayedCrawlUrl delayedUrl, long currentTime)
    {
        return this.pldLastCrawled.get(pld).get() +
               TimeUnit.MILLISECONDS.convert(delayedUrl.getPldDelay(), delayedUrl.getTimeUnit()) > currentTime;
    }

    /**
     * This method adds a '/' to the end of a URL if it either has not yet one or it already points to a file (f.e.
     * index.html) or some web applications or anchors on a site.
     *
     * @param url
     *         The URL to add a tailing '/' if it needs one
     *
     * @return The formated URL
     */
    private String formatURL(String url)
    {
        if (url == null)
        {
            return null;
        }

        if (!url.endsWith("/") && !url.contains("?") && url.lastIndexOf("/") > url.lastIndexOf(".") &&
            url.contains("#"))
        {
            url += "/";
        }
        return url;
    }

    /**
     * Runs through every future result and checks if its result is currently available. If so it fetches the available
     * data (a {@link List} of new URLs) and hands the data over to the DRUM-structure which keeps track of new and
     * already known URLs.
     * <p>
     * The result is then removed from the {@link List} of not yet processed future results.
     */
    private synchronized void checkResults()
    {
        LOG.debug("Checking crawl results");
        List<String> removeList = new ArrayList<>();
        for (Future<CrawledPage> result : this.waitingList.values())
        {
            if (result.isDone())
            {
                // Results are ready - get them and send them to the
                // URLseen instance
                try
                {
                    CrawledPage page = result.get();
                    if (page == null)
                    {
                        continue;
                    }
                    this.numPagesCrawledSuccess.incrementAndGet();

                    LOG.info("{} - {} - found: {} URLs", Thread.currentThread().getName(), page.getURL(),
                             page.getContainedURLs().size());
                    for (String url : page.getContainedURLs())
                    {
                        LOG.debug("{} - {} - found: {}", Thread.currentThread().getName(), page.getURL(), url);
                        this.urlSeen.checkURL(null, url);
                    }
                    // removing items from a list we are iterating through
                    // is not possible so save it until we finished the iteration
                    // and then remove them from the actual list
                    removeList.add(page.getURL());
                }
                catch (InterruptedException | ExecutionException e)
                {
                    LOG.error("Error while retrieving callable result! Reason: " + e.getLocalizedMessage(), e);
                }
            }
        }
        Future<CrawledPage> remItem;
        for (String url : removeList)
        {
            remItem = this.waitingList.remove(url);
            if (remItem == null)
            {
                LOG.error("Invalid url should be removed: url={} keys: {}", url,
                          Arrays.toString(this.waitingList.keySet().toArray()));
            }
        }
    }

    /**
     * Stops the current crawling process which leads to a synchronization of the data stored in memory or on in a cache
     * file with the backing database.
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
            LOG.error("Error while disposing IRLbot components", dEx);
        }

        // This will make the executor accept no new threads and finish all existing threads in the queue
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

        LOG.info("Finished crawling, all threads shutdown");
    }

    @Override
    public void handleUniqueURL(String url)
    {
        // Unique URLs arriving from URLseen perform a check against PLDindegree
        LOG.debug("sending to STAR budget check: {}", url);
        this.pldIndegree.check(url);
    }

    @Override
    public void handleSpamCheck(String url, int budget)
    {
        // Unique URLs and their budget arriving form STAR which need to be forwarded to BEAST
        LOG.debug("sending to BEAST - url: {} | budget: {}", url, budget);
        this.beast.checkBudgetOfURL(url, budget);
    }

    @Override
    public void handleBudgetPassed(String url)
    {
        LOG.debug("sending URL to RobotsCache: {}", url);
        this.robotsCheckQueue.add(url);
    }

    @Override
    public void handleUnableToCheck(String url)
    {
        LOG.debug("no robots.txt available yet for {}", url);
        // no robots.txt available for this URL yet - request one by adding the PLD to RobotsRequest.checkUpdate(url).
        String hostName = IRLbotUtils.getHostname(url);
        this.robotsRequestQueue.add(hostName);
        // Add the URL to the back of the robotsCheckQueue again
        this.robotsCheckQueue.add(url);
    }

    @Override
    public void handleRobotsTxtDownloadRequests(String hostName)
    {
        LOG.debug("requesting robots.txt download for host: {}", hostName);
        this.robotsDownloadQueue.add(hostName);
        for (IRLbotListener listener : this.listeners)
        {
            listener.sizeOfRobotTxtDownloadQueue(this.robotsDownloadQueue.size());
        }
    }

    @Override
    public void handleURLsPassed(String url, HostData hostData)
    {
        LOG.debug("adding {} to the list of URLs to crawl! Delay for PLD {} is {}", url, hostData.getHostName(),
                  hostData.getCrawlDelay());
        this.toCrawl.add(new DelayedCrawlUrl(url, hostData.getCrawlDelay(), TimeUnit.SECONDS));
        this.informOnToCrawlChange(this.toCrawl.size());
    }

    /**
     * This runnable class takes the first available URL from the <em>robotsCheckQueue</em> and issues a new check
     * request to the <em>robotsCache</em>. This check will determine if there is already a robots.txt for the given
     * hostname available. If not, the request will later be issued to the <em>robotsRequestQueue</em> which will try to
     * download the robots.txt from the given URL.
     */
    private class RobotsCheckQueueEmptier implements Runnable
    {
        @Override
        public void run()
        {
            while (!stopRequested)
            {
                try
                {
                    String url = robotsCheckQueue.take();
                    LOG.debug("Taking {} from the queue to check in robotsCache", url);
                    robotsCache.check(url);
                }
                catch (InterruptedException e)
                {
                    LOG.warn("robotsCacheQueue was interrupted while waiting for the next available URL to check");
                }
            }
        }
    }

    /**
     * This class takes the first available hostname from the robots request queue and invokes the RobotsRequest segment
     * with a null HostData element and the actual retrieved host name. The taken hostname will be removed in this step
     * from the <em>robotsRequestQueue</em>.
     */
    private class RobotsRequestedQueueEmptier implements Runnable
    {
        @Override
        public void run()
        {
            while (!stopRequested)
            {
                try
                {
                    // takes the first hostname available from the
                    // robotsRequestQueue if available and issues a new request
                    // to the robotsRequested segment. The take operation
                    // blocks until a hostname is available
                    String hostname = robotsRequestQueue.take();
                    LOG.debug("Taking {} from RobotsRequestQueue to check in " +
                              "RobotsRequested if a request needs to be issued", hostname);
                    robotsRequested.request(null, hostname);
                }
                catch (InterruptedException e)
                {
                    LOG.warn("robotsRequestQueue was interrupted while waiting for " +
                             "the next available host name to request a new robots.txt from");
                }
            }
        }
    }

    /**
     * This runnable class executes the actual downloading of the robots.txt for a URL which was taken from the
     * <em>robotsDownloadQueue</em>. The <em>robotsCache</em> will be updated with the downloaded robots.txt file so
     * later look-ups wont need to request or even download the robots.txt file again.
     */
    private class DownloadRobotsTxtExecutor implements Runnable
    {
        @Override
        public void run()
        {
            while (!stopRequested)
            {
                String hostName;
                try
                {
                    // take the first available host name from the queue and
                    // start the download process of the robots.txt file.
                    hostName = robotsDownloadQueue.take();
                    for (IRLbotListener listener : listeners)
                    {
                        listener.sizeOfRobotTxtDownloadQueue(robotsDownloadQueue.size());
                    }
                    if (hostName != null)
                    {
                        String robotsFile = reader.readPage(hostName + "/robots.txt");
                        if (robotsFile != null && !"".equals(robotsFile))
                        {
                            HostData hostData = new HostData(hostName, null, null);
                            // avoid HTML pages which just return "no robots.txt"
                            if (robotsFile.toLowerCase().contains("user-agent:"))
                            {
                                hostData.setRobotsTxt(robotsFile);
                            }
                            robotsCache.update(DrumUtils.hash(hostName), hostData);
                            LOG.debug("Received robots.txt for host: {}; content: '{}'", hostName, robotsFile);
                        }
                        else
                        {
                            LOG.warn("Could not download robots.txt file for host: {}", hostName);
                            robotsCache.update(DrumUtils.hash(hostName), new HostData(hostName, null, null));
                        }
                    }
                }
                catch (Exception e)
                {
                    LOG.error("Error while downloading robots.txt", e);
                }
            }
        }
    }

    @Override
    public void update(DrumEvent<? extends DrumEvent<?>> event)
    {
        for (IRLbotListener listener : this.listeners)
        {
            listener.drumUpdate(event);
        }
    }
}
