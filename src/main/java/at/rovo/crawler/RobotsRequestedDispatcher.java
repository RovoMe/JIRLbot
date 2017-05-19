package at.rovo.crawler;

import at.rovo.drum.NullDispatcher;
import at.rovo.crawler.bean.HostData;
import at.rovo.crawler.interfaces.RobotsRequestedListener;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * An instance of this class is invoked by the backing <em>DRUM</em> instance of {@link RobotsRequested} after a
 * download for a <em>robots.txt</em> file was requested for a given host.
 * <p>
 * This class does actually not execute the download of the <em>robots.txt</em> file but informs any registered
 * listening objects that this file should get downloaded.
 */
public final class RobotsRequestedDispatcher extends NullDispatcher<HostData, String>
{
    /**
     * Contains all objects that registered interest if a <em>robots.txt</em> file is eligible for download
     **/
    private Set<RobotsRequestedListener> listeners = null;

    /**
     * Creates a new instance which handles callbacks from {@link RobotsRequested} instance.
     */
    public RobotsRequestedDispatcher()
    {
        this.listeners = new CopyOnWriteArraySet<>();
    }

    /**
     * Adds a currently not yet included object to a set of objects which get informed if a <em>robots.txt</em> is
     * eligible for download. file.
     *
     * @param listener
     *         The object to add to the set of objects to notify if a <em>robots.txt</em> is eligible for download
     */
    public void addRobotsRequestedListener(RobotsRequestedListener listener)
    {
        this.listeners.add(listener);
    }

    /**
     * Removes an object from the set of objects which gets informed if a <em>robots.txt</em> file got eligible for
     * download.
     *
     * @param listener
     *         The instance to remove from the set of notified objects
     */
    public void removeRobotsRequestedListener(RobotsRequestedListener listener)
    {
        this.listeners.remove(listener);
    }

    /**
     * Informs any registered listening objects that the robots.txt for the given host is eligible for download.
     */
    @Override
    public void uniqueKeyUpdate(Long key, HostData hostData, String hostName)
    {
        this.listeners.forEach(listener -> listener.handleRobotsTxtDownloadRequests(hostName));
    }
}
