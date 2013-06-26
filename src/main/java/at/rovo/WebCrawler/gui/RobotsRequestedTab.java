package at.rovo.WebCrawler.gui;

import at.rovo.WebCrawler.gui.events.RobotsRequestedEventListener;


public class RobotsRequestedTab extends DrumEventTab implements RobotsRequestedEventListener
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -5488962987696960698L;

	public RobotsRequestedTab(int numBuckets)
	{
		super(numBuckets);
		
		Manager.getInstance().registerForRobotsRequestedEvents(this);
	}
}
