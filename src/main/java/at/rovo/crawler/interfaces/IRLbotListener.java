package at.rovo.crawler.interfaces;

import at.rovo.caching.drum.event.DrumEvent;

/**
 * <p>
 * The listener interface for notifications on the change of some crawling
 * statistics. The class that is interested in processing these statistic
 * notifications implement this interface, and the object created with that
 * class is registered with a component, using the component's
 * <code>addIRLbotListener</code> method.
 * </p>
 */
public interface IRLbotListener
{
	/**
	 * <p>
	 * Indicates that the number of total crawled URLs has changed.
	 * </p>
	 *
	 * @param newSize
	 *            The new number of total URLs crawled
	 */
	public void numberOfURLsCrawledTotalChanged(long newSize);

	/**
	 * <p>
	 * Indicates that the number of successfully crawled URLs has changed.
	 * </p>
	 *
	 * @param newSize
	 *            The new number of successfully crawled URLs
	 */
	public void numberOfURLsCrawledSuccessChanged(long newSize);

	/**
	 * <p>
	 * Indicates that the number of URLs to crawl has changed.
	 *
	 * </p>
	 * @param newSize
	 *            The new number of URLs to crawl
	 */
	public void numberOfURLsToCrawlChanged(long newSize);

	/**
	 * <p>
	 * Indicates that the number of <em>robots.txt</em> files
	 * requested to download has changed.
	 * </p>
	 *
	 * @param newSize
	 *            The new number of robot.txt files requested to download
	 */
	public void sizeOfRobotTxtDownloadQueue(long newSize);

	/**
	 * <p>
	 * Indicate that a new {@link DrumEvent} occurred in the backing cache.
	 * </p>
	 *
	 * @param event
	 *            The event issued by the backing <em>DRUM</em> instance
	 */
	public void drumUpdate(DrumEvent<? extends DrumEvent<?>> event);
}
