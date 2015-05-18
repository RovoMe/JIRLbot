package at.rovo.test;

import at.rovo.caching.drum.DrumException;
import at.rovo.caching.drum.util.DrumUtils;
import at.rovo.crawler.RobotsCache;
import at.rovo.crawler.RobotsCacheDispatcher;
import at.rovo.crawler.bean.HostData;
import at.rovo.crawler.interfaces.RobotsCachePassedListener;
import at.rovo.crawler.util.IRLbotUtils;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

/**
 * <p>
 * This test case initializes a DRUM instance to manage robot.txt files and PLDs
 * queried for those robot.txt files.
 * </p>
 * <p>
 * Example of RobotsCache usage:
 * </p>
 *
 * <code>
 * Initializing Drum ... <br/>
 * done!<br/>
 * <br/>
 * unable to check URL: http://winf.at/rss-feed.php<br/>
 * unable to check URL: http://www.informatik.tuwien.ac.at/aktuelles/672<br/>
 * unable to check URL: http://www.tuwien.ac.at/metanavigation/faqs/<br/>
 * Mocking robot.txt update of 'http://www.tuwien.ac.at' and 'http://winf.at'<br/>
 * check passed for URL: http://winf.at/rss-feed.php<br/>
 * check passed for URL: http://www.tuwien.ac.at/metanavigation/faqs/<br/>
 * unable to check URL: http://www.facebook.com/events/350068195090400<br/>
 * check passed for URL: http://www.tuwien.ac.at/metanavigation/links/<br/>
 * check passed for URL: http://www.informatik.tuwien.ac.at/aktuelles/672<br/>
 * Disposing robotsCache ... <br/>
 * check passed for URL: http://www.winf.at<br/>
 * check passed for URL: http://www.winf.at<br/>
 * check passed for URL: http://www.informatik.tuwien.ac.at/aktuelles/672<br/>
 * done!<br/>
 * <br/>
 * Data contained in cache.db:<br/>
 * Key: -8811650085514601110, Value: http://winf.at<br/>
 * Key: -7476758895180383974, Value: http://tuwien.ac.at<br/>
 * </code>
 */
public class RobotsCacheTest extends BaseCacheTest implements RobotsCachePassedListener
{
    private final static Logger LOG = LogManager.getLogger(MethodHandles.lookup().lookupClass());

    private RobotsCache robotsCache = null;
    private Set<String> urlsPassed = null;

    @Test
    public void RobotsCacheDrumTest()
    {
        String robotsCacheName = "RobotsCacheTest";
        this.urlsPassed = new LinkedHashSet<>();

        try
        {
            LOG.info("Example of RobotsCache usage:");
            LOG.info("----------------------");
            LOG.info("");

            LOG.info("Initializing Drum ... ");

            RobotsCacheDispatcher dispatcher = new RobotsCacheDispatcher();
            dispatcher.addRobotsCachePassedListener(this);
            this.robotsCache = new RobotsCache(robotsCacheName, dispatcher, 16, 64);
            LOG.info("done!");

            // key: -8811650085514601110
            String url1 = "http://winf.at/rss-feed.php";
            // key: -7476758895180383974
            String url2 = "http://www.informatik.tuwien.ac.at/aktuelles/672";
            // key: -7476758895180383974
            String url3 = "http://www.tuwien.ac.at/metanavigation/faqs/";

            this.robotsCache.check(url1);   // new host: http://winf.at - no
                                            // robots.txt available by now
            this.robotsCache.check(url2);   // new hosts: http://tuwien.ac.at - no
                                            // robots.txt available by now
            this.robotsCache.check(url3);   // host: http://tuwien.ac.at -
                                            // robots.txt value was not yet
                                            // updated!

            try
            {
                Thread.sleep(2000);
                LOG.info("Moking robot.txt update of 'http://www.tuwien.ac.at' and 'http://winf.at'");
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }

            // the merge will cause all data which was written to local disk
            // files before to be merged into the backing data store
            this.robotsCache.check(url3);

            // key: -7849402421258767002
            String url4 = "http://www.facebook.com/events/350068195090400";
            // key: -7476758895180383974
            String url5 = "http://www.tuwien.ac.at/metanavigation/links/";

            this.robotsCache.check(url4);   // new host: http://facebook.com - no
                                            // robots.txt available
            this.robotsCache.check(url5);   // should pass as inside robotsCache
                                            // http://tuwien.ac.at got updated
                                            // with the robots.txt value!
            this.robotsCache.check(url2);   // re-check url2! should pass now
            this.robotsCache.check(url1);   // re-check url1 (winf.at) will pass
                                            // as the first check lead to a write
                                            // to disk bucket and through multiple
                                            // checking for url2 or url3 a merge
                                            // was forced which stored all values
                                            // contained in any of the disk bucket
                                            // file into the data store and
                                            // returned a UNIQUE_KEY result which
                                            // furthermore lead to a
                                            // handleUnableToCheck() method
                                            // invocation which furthermore mocked
                                            // a robot.txt for winf.at
            try
            {
                Thread.sleep(1000);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
            this.robotsCache.check(url2);   // re-check url2! should pass too, but
                                            // as it is already contained only
                                            // one entry should be in the list!

            // key: -8811650085514601110
            String url6 = "http://www.winf.at";

            this.robotsCache.check(url6);

            String url7 = "http://www.winf.at/forum/shouldNotReturnAValue";
            this.robotsCache.check(url7);   // URL 7 violates a rule defined in
                                            // the robots.txt file so it should
                                            // neither pass nor return an unable
                                            // to check event
        }
        catch (DrumException dEx)
        {
            LOG.catching(dEx);
        }
        finally
        {
            // disposing the robots.txt cache forces a synchronization which results
            // in the robots.txt of winf.at to be written to the cache
            LOG.info("Disposing robotsCache ... ");
            if (this.robotsCache != null)
            {
                try
                {
                    this.robotsCache.dispose();
                }
                catch (DrumException e)
                {
                    LOG.catching(e);
                }
            }
            LOG.info("done!");

            LOG.info("URLs passed: {}", this.urlsPassed);
        }

        Assert.assertTrue("'http://www.tuwien.ac.at/metanavigation/links/' not contained in passed URLs!",
                          this.urlsPassed.contains("http://www.tuwien.ac.at/metanavigation/links/"));
        Assert.assertTrue("'http://www.informatik.tuwien.ac.at/aktuelles/672' not contained in passed URLs!",
                          this.urlsPassed.contains("http://www.informatik.tuwien.ac.at/aktuelles/672"));
        Assert.assertTrue("'http://www.tuwien.ac.at/metanavigation/faqs/' not contained in passed URLs!",
                          this.urlsPassed.contains("http://www.tuwien.ac.at/metanavigation/faqs/"));
        Assert.assertTrue("'http://www.winf.at' not contained in passed URLs!",
                          this.urlsPassed.contains("http://www.winf.at"));
        Assert.assertTrue("'http://winf.at/rss-feed.php' not contained in passed URL!",
                          this.urlsPassed.contains("http://winf.at/rss-feed.php"));
        Assert.assertEquals("Size of passed URLs differs!", 5, this.urlsPassed.size(), 0);
        Assert.assertFalse("'http://www.facebook.com/events/350068195090400' contained in passed URLs!",
                           this.urlsPassed.contains("http://www.facebook.com/events/350068195090400"));

        try
        {
            try
            {
                Thread.sleep(1000);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }

            List<Long> keys = new ArrayList<>();
            DrumUtils.printCacheContent(robotsCacheName, keys, HostData.class);
            Assert.assertNotNull("No keys found in the robots cache!", keys);
            Assert.assertEquals("Expected number of keys inside robots cache do not match!", 2, keys.size(), 0);
            // winf.at
            Assert.assertEquals("Key for 'winf.at' does not match!", new Long(-8811650085514601110L), keys.get(0), 0);
            // tuwien
            Assert.assertEquals("Key for 'tuwien.ac.at' does not macht!", new Long(-7476758895180383974L), keys.get(1),
                                0);
        }
        catch (IOException | DrumException e)
        {
            LOG.catching(e);
        }
    }

    @Override
    public void handleURLsPassed(String url, HostData hostData)
    {
        LOG.info("check passed for URL: {}", url);
        this.urlsPassed.add(url);
    }

    @Override
    public void handleUnableToCheck(String url)
    {
        LOG.info("unable to check URL: {}", url);
        // URL would be forwarded to robotsRequest which manages
        // the download of a robots.txt file
        // here we will just send an imaginary robots.txt for certain hosts
        if ("http://tuwien.ac.at".equals(IRLbotUtils.getHostname(url)))
        {
            String robotsTxt = "# Zugriff fuer alle bots erlauben\n" +
                               "User-agent: *\n" +
                               "Disallow:\n" +
                               "#Disallow: /\n" +
                               "#User-agent: Googlebot\n" +
                               "#Allow: /\n";
            try
            {
                Thread.sleep(500);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
            this.robotsCache.update(DrumUtils.hash(IRLbotUtils.getHostname(url)),
                                    new HostData(IRLbotUtils.getHostname(url), null, robotsTxt));

        }
        else if ("http://winf.at".equals(IRLbotUtils.getHostname(url)))
        {
            String robotsTxt = "# robots.txt fuer winf.at (siehe www.robotstxt.org)\n" +
                               "User-agent: *\n" +
                               "Disallow: /forum/\n" +
                               "Disallow: /angaben/\n" +
                               "Disallow: /~leo/\n" +
                               "\n" +
                               "User-agent: ia_archiver\n" +
                               "Disallow: /~klaus/\n";
            try
            {
                Thread.sleep(500);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
            this.robotsCache.update(DrumUtils.hash(IRLbotUtils.getHostname(url)),
                                    new HostData(IRLbotUtils.getHostname(url), null, robotsTxt));
        }
    }
}
