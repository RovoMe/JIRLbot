package at.rovo.crawler;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import at.rovo.caching.drum.NullDispatcher;
import at.rovo.caching.drum.data.StringSerializer;
import at.rovo.crawler.interfaces.UniqueUrlListener;

/**
 * <p>
 * Methods of this class are invoked by the backing DRUM instance
 * </p>
 */
@SuppressWarnings("unused")
public final class UniqueUrlDispatcher extends NullDispatcher<StringSerializer,StringSerializer>
{
	/** The logger of this class **/
	private final static Logger LOG = LogManager.getLogger(UniqueUrlDispatcher.class);
	/** The set of listeners that need to be informed of certain check-outcomes **/
	private List<UniqueUrlListener> listeners = null;
	
	public UniqueUrlDispatcher()
	{
		this.listeners = new CopyOnWriteArrayList<>();
	}

	/**
	 * <p>
	 * Adds a new {@link UniqueUrlListener} instance to the objects which
	 * requested to be notified on the outcome of certain check-outcomes.
	 * </p>
	 *
	 * @param listener The object to add to the list of objects which need to
	 *                 be informed on certain check-outcomes
	 */
	public void addUniqueUrlListener(UniqueUrlListener listener)
	{
		if (!this.listeners.contains(listener))
			this.listeners.add(listener);
	}

	/**
	 * <p>
	 * Removes a {@link UniqueUrlListener} instance from the set of objects
	 * that are notified on certain check-outcomes.
	 * </p>
	 *
	 * @param listener The object to remove from the list of objects to inform
	 *                 on check-outcomes
	 */
	public void removeUniqueUrlListener(UniqueUrlListener listener)
	{
		if (this.listeners.contains(listener))
			this.listeners.remove(listener);
	}
	
	@Override
	public void uniqueKeyCheck(Long key, StringSerializer url)
	{
		LOG.debug("unique key check: {}", url);
		for (UniqueUrlListener listener : listeners)
			listener.handleUniqueURL(url.getData());
	}
	
	@Override
	public void uniqueKeyUpdate(Long key, StringSerializer value, StringSerializer url)
	{
		LOG.debug("unique key update: {}", url);
		for (UniqueUrlListener listener : listeners)
			listener.handleUniqueURL(url.getData());
	}
}
