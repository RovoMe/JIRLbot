package at.rovo.crawler;

import at.rovo.drum.NullDispatcher;
import at.rovo.crawler.interfaces.UniqueUrlListener;
import java.lang.invoke.MethodHandles;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Methods of this class are invoked by the backing DRUM instance
 */
public final class UniqueUrlDispatcher extends NullDispatcher<String, String>
{
    /** The logger of this class **/
    private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    /** The set of listeners that need to be informed of certain check-outcomes **/
    private Set<UniqueUrlListener> listeners = null;

    public UniqueUrlDispatcher()
    {
        this.listeners = new CopyOnWriteArraySet<>();
    }

    /**
     * Adds a new {@link UniqueUrlListener} instance to the objects which requested to be notified on the outcome of
     * certain check-outcomes.
     *
     * @param listener
     *         The object to add to the list of objects which need to be informed on certain check-outcomes
     */
    public void addUniqueUrlListener(UniqueUrlListener listener)
    {
        this.listeners.add(listener);
    }

    /**
     * Removes a {@link UniqueUrlListener} instance from the set of objects that are notified on certain
     * check-outcomes.
     *
     * @param listener
     *         The object to remove from the list of objects to inform on check-outcomes
     */
    public void removeUniqueUrlListener(UniqueUrlListener listener)
    {
        this.listeners.remove(listener);
    }

    @Override
    public void uniqueKeyCheck(Long key, String url)
    {
        LOG.debug("unique key check: {}", url);
        this.listeners.forEach(listener -> listener.handleUniqueURL(url));
    }

    @Override
    public void uniqueKeyUpdate(Long key, String data, String url)
    {
        LOG.debug("unique key update: {}", url);
        this.listeners.forEach(listener -> listener.handleUniqueURL(url));
    }
}
