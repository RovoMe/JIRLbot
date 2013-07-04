package at.rovo.WebCrawler;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import at.rovo.caching.drum.NullDispatcher;
import at.rovo.caching.drum.data.StringSerializer;

public class UniqueUrlDispatcher extends NullDispatcher<StringSerializer,StringSerializer>
{
	// create a logger
	private final static Logger logger = LogManager.getLogger(UniqueUrlDispatcher.class.getName()); 
	private List<UniqueUrlListener> listeners = null;
	
	public UniqueUrlDispatcher()
	{
		this.listeners = new CopyOnWriteArrayList<UniqueUrlListener>();
	}
	
	public void addUniqueUrlListener(UniqueUrlListener listener)
	{
		if (!this.listeners.contains(listener))
			this.listeners.add(listener);
	}
	
	public void removeUniqueUrlListener(UniqueUrlListener listener)
	{
		if (this.listeners.contains(listener))
			this.listeners.remove(listener);
	}
	
	@Override
	public void uniqueKeyCheck(Long key, StringSerializer aux)
	{
		if (logger.isDebugEnabled())
			logger.debug("unique key check: "+aux);
		for (UniqueUrlListener listener : listeners)
			listener.handleUniqueURL(aux.getData());
	}
	
	@Override
	public void uniqueKeyUpdate(Long key, StringSerializer value, StringSerializer aux)
	{
		if (logger.isDebugEnabled())
			logger.debug("unique key update: "+aux);
		for (UniqueUrlListener listener : listeners)
			listener.handleUniqueURL(aux.getData());
	}
}
