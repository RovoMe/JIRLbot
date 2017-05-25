package at.rovo.crawler;

import at.rovo.drum.NullDispatcher;
import at.rovo.crawler.bean.HostData;
import at.rovo.crawler.bean.RobotsTxt;
import at.rovo.crawler.interfaces.RobotsCachePassedListener;
import java.lang.invoke.MethodHandles;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An instance of this class is invoked by the {@link RobotsCache} backing <em>DRUM</em> instance when it has finished
 * the check if a <em>robots.txt</em> for the requested domain is available or not.
 * <p>
 * In case of an available <em>robots.txt</em> the file is parsed and a decision is made based on the entries in the
 * <em>robots.txt</em> if the URL to check is allowed to proceed or not.
 */
public final class RobotsCacheDispatcher extends NullDispatcher<HostData, String>
{
    /** The logger of this class **/
    private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    /**
     * This list will hold all objects that requested to be informed if a certain robots.txt is available or not
     **/
    private Set<RobotsCachePassedListener> listeners = null;

    /**
     * Creates a new instance which handles callbacks from the {@link RobotsCache} instance.
     */
    public RobotsCacheDispatcher()
    {
        // all write operations make a fresh copy of the entire underlying array
        // iteration require no locking and is very fast
        this.listeners = new CopyOnWriteArraySet<>();
    }

    /**
     * Adds a currently not yet included object to a set of objects which get informed on the availability or the
     * absence of a <em>robots.txt</em> file.
     *
     * @param listener
     *         The object to add to the set of objects to notify if a <em>robots.txt</em> file is available or not
     */
    public void addRobotsCachePassedListener(RobotsCachePassedListener listener)
    {
        this.listeners.add(listener);
    }

    /**
     * Removes an object from the set of objects which gets informed on a succeeding or failing lookup of available
     * <em>robots.txt</em> files.
     *
     * @param listener
     *         The instance to remove from the set of notified objects
     */
    public void removeRobotsCachePassedListener(RobotsCachePassedListener listener)
    {
        this.listeners.remove(listener);
    }

    /**
     * URLs which have already a robots.txt stored for their pay level domain are returning a duplicate key. Check the
     * returned host data if the URL is allowed to be crawled. If the URL passed the check against the
     * <em>robots.txt</em> file it will inform any registered listening objects. If the URL failed these tests, it will
     * drop out and therefore not get crawled.
     * <p>
     * <b>Note:</b> This method is invoked as a result of a previous check operation in {@link
     * RobotsCache#check(String)} and should therefore not be called by users.
     *
     * @param key
     *         The hash value of the URL which was checked for an available robots.txt file
     * @param hostData
     *         Some related host information like the hostname, the IP address or the available <em>robots.txt</em>
     *         file
     * @param url
     *         The URL which passed the check
     */
    @Override
    public void duplicateKeyCheck(Long key, HostData hostData, String url)
    {
        LOG.debug("Checking compliance with robots.txt rules: {}", url);
        if (this.isAllowedToPass(url, hostData))
        {
            LOG.debug("URL passed tests!");
            this.listeners.forEach(listener -> listener.handleURLsPassed(url, hostData));
        }
    }

    /**
     * No robots.txt file could be found for this URL yet. This will inform any registered listening objects that no
     * <em>robots.txt</em> file could be found which might further result in a later download of it.
     * <p>
     * <b>Note:</b> This method is invoked as a result of a previous check operation in {@link
     * RobotsCache#check(String)} and should therefore not be called by users.
     *
     * @param key
     *         The hash value of the URL which was checked for an available robots.txt file
     * @param url
     *         The URL which failed the check for an available <em>robots.txt</em> file
     */
    @Override
    public void uniqueKeyCheck(Long key, String url)
    {
        LOG.debug("No robots.txt found for {} inside DRUM!", url);
        this.listeners.forEach(listener -> listener.handleUnableToCheck(url));
    }

    /**
     * Checks if a URL is allowed to be crawled based on the hosts robots.txt defined rules.
     *
     * @param url
     *         The URL to be checked against the rules of the hosts robots.txt file
     * @param hostData
     *         The host data object which contains the robots.txt rules as String
     *
     * @return Returns true if the URL has passed the robots.txt rules test, false if a rule in the robots.txt file
     * prevents crawling
     */
    private boolean isAllowedToPass(String url, HostData hostData)
    {
        if (hostData == null || hostData.getRobotsTxt() == null || hostData.getRobotsTxt().equals(""))
        {
            return true;
        }

        RobotsTxt robotsTxt = new RobotsTxt(hostData.getRobotsTxt());
        long crawlDelay = 0;
        // check if a crawl delay is specified
        if (robotsTxt.getRecords().containsKey(IRLbot.CRAWLER_NAME)) {
            crawlDelay = robotsTxt.getRecords().get(IRLbot.CRAWLER_NAME).getCrawlDelay();
        } else if (robotsTxt.getRecords().containsKey("*")) {
            crawlDelay = robotsTxt.getRecords().get("*").getCrawlDelay();
        }
        hostData.setCrawlDelay(crawlDelay);
        return robotsTxt.checkRobotRules(IRLbot.CRAWLER_NAME, url);
    }
}
