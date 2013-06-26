package at.rovo.WebCrawler;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import at.rovo.caching.drum.NullDispatcher;
import at.rovo.caching.drum.StringSerializer;

public class RobotsRequestedDispatcher extends NullDispatcher<HostData, StringSerializer> 
{
	private List<RobotsRequestedListener> listeners = null;
	
	public RobotsRequestedDispatcher()
	{
		this.listeners = new CopyOnWriteArrayList<RobotsRequestedListener>();
	}
	
	public void addRobotsRequestedListener(RobotsRequestedListener listener)
	{
		if (!this.listeners.contains(listener))
			this.listeners.add(listener);
	}
	
	public void removeRobotsRequestedListener(RobotsRequestedListener listener)
	{
		if (this.listeners.contains(listener))
			this.listeners.remove(listener);
	}
	
	/**
	 * <p></p>
	 */
	@Override
	public void uniqueKeyUpdate(Long key, HostData hostData, StringSerializer hostName) 
	{
		for (RobotsRequestedListener listener : this.listeners)
			listener.handleNewRobotRequests(hostName.getData());
	}
}
