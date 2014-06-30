package at.rovo.crawler;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import at.rovo.caching.drum.NullDispatcher;
import at.rovo.caching.drum.data.StringSerializer;
import at.rovo.crawler.bean.HostData;
import at.rovo.crawler.interfaces.RobotsRequestedListener;

/**
 * <p>
 * An instance of this class is invoked by the backing <em>DRUM</em> instance
 * of {@link RobotsRequested} after a download for a <em>robots.txt</em> file
 * was requested for a given host.
 * </p>
 * <p>
 * This class does actually not execute the download of the <em>robots.txt</em>
 * file but informs any registered listening objects that this file should get
 * downloaded.
 * </p>
 */
@SuppressWarnings("unused")
public final class RobotsRequestedDispatcher extends NullDispatcher<HostData, StringSerializer>
{
	/** Contains all objects that registered interest if a <em>robots.txt</em>
	 * file is eligible for download **/
	private List<RobotsRequestedListener> listeners = null;

	/**
	 * <p>
	 * Creates a new instance which handles callbacks from
	 * {@link RobotsRequested} instance.
	 * </p>
	 */
	public RobotsRequestedDispatcher()
	{
		this.listeners = new CopyOnWriteArrayList<>();
	}

	/**
	 * <p>
	 * Adds a currently not yet included object to a set of objects which get
	 * informed if a <em>robots.txt</em> is eligible for download.
	 * file.
	 * </p>
	 *
	 * @param listener
	 *            The object to add to the set of objects to notify if a
	 *            <em>robots.txt</em> is eligible for download
	 */
	public void addRobotsRequestedListener(RobotsRequestedListener listener)
	{
		if (!this.listeners.contains(listener))
			this.listeners.add(listener);
	}

	/**
	 * <p>
	 * Removes an object from the set of objects which gets informed if a
	 * <em>robots.txt</em> file got eligible for download.
	 * </p>
	 *
	 * @param listener
	 *            The instance to remove from the set of notified objects
	 */
	public void removeRobotsRequestedListener(RobotsRequestedListener listener)
	{
		if (this.listeners.contains(listener))
			this.listeners.remove(listener);
	}
	
	/**
	 * <p>
	 * Informs any registered listening objects that the robots.txt for the
	 * given host is eligible for download.
	 * </p>
	 */
	@Override
	public void uniqueKeyUpdate(Long key, HostData hostData, StringSerializer hostName) 
	{
		for (RobotsRequestedListener listener : this.listeners)
			listener.handleRobotsTxtDownloadRequests(hostName.getData());
	}
}
