package at.rovo.test;

import at.rovo.crawler.util.DelayedCrawlUrl;
import at.rovo.crawler.util.IRLbotUtils;
import de.jkeylockmanager.manager.KeyLockManager;
import de.jkeylockmanager.manager.KeyLockManagers;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.Assert.assertThat;

/**
 * This unit test tests the functionality of the delayed queue in combination with requeueing if the time did not yet
 * exceed
 */
@SuppressWarnings("NullableProblems")
public class DelayedUrlTest
{
    private final static Logger LOG = LogManager.getLogger(MethodHandles.lookup().lookupClass());

    private volatile boolean stopRequested = false;

    @Test
    public void testDelayAndRequeue() throws Exception
    {
        final BlockingQueue<DelayedCrawlUrl> toCrawl = new DelayQueue<>();
        final Map<String, AtomicLong> pldLastCrawled = new ConcurrentHashMap<>();
        final Map<String, Long> crawledPages = Collections.synchronizedMap(new LinkedHashMap<>());
        final long startTime = System.currentTimeMillis();

        DelayedCrawlUrl url1 = new DelayedCrawlUrl("http://www.test.org", 1, TimeUnit.SECONDS);
        DelayedCrawlUrl url2 = new DelayedCrawlUrl("http://www.some-other-test.org", 0, TimeUnit.SECONDS);
        DelayedCrawlUrl url3 = new DelayedCrawlUrl("http://www.test.org/some/other/page.html", 1, TimeUnit.SECONDS);
        DelayedCrawlUrl url4 = new DelayedCrawlUrl("http://www.delayed-test.com", 0, TimeUnit.SECONDS);
        DelayedCrawlUrl url5 = new DelayedCrawlUrl("http://www.test.org/some/other/page2.html", 1, TimeUnit.SECONDS);

        final KeyLockManager lockManager = KeyLockManagers.newLock();

        Runnable crawler = () -> {
            LOG.info("{} started", Thread.currentThread().getName());
            while (!stopRequested)
            {
                if (!toCrawl.isEmpty())
                {
                    DelayedCrawlUrl delayedUrl;
                    try
                    {
                        delayedUrl = toCrawl.remove();
                    }
                    catch (NoSuchElementException nseEx)
                    {
                        continue;
                    }
                    String url = delayedUrl.getUrl();
                    String pld = IRLbotUtils.getPLDofURL(url);

                    if (!pldLastCrawled.containsKey(pld))
                    {
                        pldLastCrawled.put(pld, new AtomicLong(System.currentTimeMillis()));
                    }
                    // check if we actually crawled the domain recently (within the delay time-frame)
                    long currentTime = System.currentTimeMillis();
                    if (delayedUrl.getPldDelay() > 0)
                    {

                        boolean doContinue = lockManager.executeLocked(pld, () -> checkAndRequeue(url, pld, delayedUrl,
                                                                                                  pldLastCrawled,
                                                                                                  toCrawl,
                                                                                                  currentTime));
                        if (doContinue)
                        {
                            LOG.info("Skipping crawl for {}", delayedUrl.getUrl());
                            continue;
                        }
                    }
                    else
                    {
                        // update the timestamp of the last crawl of the PLD
                       pldLastCrawled.get(pld).set(System.currentTimeMillis());
                    }

                    LOG.info("{} - crawled page: {} - Time: {}", Thread.currentThread().getName(), url,
                             new Date(currentTime));
                    crawledPages.put(url, currentTime);
                }
            }
            LOG.info("{} finished", Thread.currentThread().getName());
        };
        Thread t1 = new Thread(crawler);
        t1.setName("Thread1");
        Thread t2 = new Thread(crawler);
        t2.setName("Thread2");

        t1.start();
        t2.start();

        String pld = IRLbotUtils.getPLDofURL(url1.getUrl());
        pldLastCrawled.put(pld, new AtomicLong(System.currentTimeMillis() - 500));

        toCrawl.add(url1);
        toCrawl.add(url2);
        toCrawl.add(url3);
        toCrawl.add(url4);
        toCrawl.add(url5);

        Thread.sleep(4000);

        stopRequested = true;
        t1.join(500);
        t2.join(500);

        assertThat(crawledPages.size(), is(equalTo(5)));
        List<Long> crawlTimes = new ArrayList<>(crawledPages.values());

        assertThat(crawlTimes.get(2) - startTime, is(greaterThanOrEqualTo(1000L)));
        assertThat(crawlTimes.get(3) - crawlTimes.get(2), is(greaterThanOrEqualTo(1000L)));
        assertThat(crawlTimes.get(4) - crawlTimes.get(3), is(greaterThanOrEqualTo(1000L)));
    }

    private boolean checkAndRequeue(String url, String pld, DelayedCrawlUrl delayedUrl,
                                    Map<String, AtomicLong> pldLastCrawled, BlockingQueue<DelayedCrawlUrl> toCrawl,
                                    long currentTime)
    {
        // LOG.info("PLD {} for URL {} has a delay of {} seconds specified - last crawled: {} - current time: {} - diff: {}",
        //          pld, url, delayedUrl.getPldDelay(), pldLastCrawled.get(pld).get(), currentTime,
        //          currentTime-pldLastCrawled.get(pld).get());
        if (needsReQueueing(pld, delayedUrl, pldLastCrawled, currentTime))
        {
            LOG.info("{} - Re-Queue {} - lastCrawl: {} delay: {} seconds currentTime: {}",
                     Thread.currentThread().getName(), url, pldLastCrawled.get(pld).get(), delayedUrl.getPldDelay(),
                     currentTime);
            // since we last crawled the PLD less then time-frame seconds ago, add the URL back to the queue
            toCrawl.add(new DelayedCrawlUrl(url, delayedUrl.getPldDelay(), delayedUrl.getTimeUnit()));
            return true;
        }
        else
        {
            LOG.info("PLD {} for URL {} was last crawled {} seconds ago", pld, url,
                     currentTime - pldLastCrawled.get(pld).get());
            pldLastCrawled.get(pld).set(currentTime);
            return false;
        }
    }

    private boolean needsReQueueing(String pld, DelayedCrawlUrl delayedUrl, Map<String, AtomicLong> pldLastCrawled,
                                    long currentTime)
    {
        return pldLastCrawled.get(pld).get() +
               TimeUnit.MILLISECONDS.convert(delayedUrl.getPldDelay(), delayedUrl.getTimeUnit()) > currentTime;
    }
}
