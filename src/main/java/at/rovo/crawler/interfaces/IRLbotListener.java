package at.rovo.crawler.interfaces;

import at.rovo.drum.event.DrumEvent;

/**
 * The listener interface for notifications on the change of some crawling statistics. The class that is interested in
 * processing these statistic notifications implement this interface, and the object created with that class is
 * registered with a component, using the component's <code>addIRLbotListener</code> method.
 */
public interface IRLbotListener
{
    /**
     * Indicates that the number of total crawled URLs has changed.
     *
     * @param newSize
     *         The new number of total URLs crawled
     */
    void numberOfURLsCrawledTotalChanged(long newSize);

    /**
     * Indicates that the number of successfully crawled URLs has changed.
     *
     * @param newSize
     *         The new number of successfully crawled URLs
     */
    void numberOfURLsCrawledSuccessChanged(long newSize);

    /**
     * Indicates that the number of URLs to crawl has changed.
     *
     * @param newSize
     *         The new number of URLs to crawl
     */
    void numberOfURLsToCrawlChanged(long newSize);

    /**
     * Indicates that the number of <em>robots.txt</em> files requested to download has changed.
     *
     * @param newSize
     *         The new number of robot.txt files requested to download
     */
    void sizeOfRobotTxtDownloadQueue(long newSize);

    /**
     * Indicate that a new {@link DrumEvent} occurred in the backing cache.
     *
     * @param event
     *         The event issued by the backing <em>DRUM</em> instance
     */
    void drumUpdate(DrumEvent<? extends DrumEvent<?>> event);
}
