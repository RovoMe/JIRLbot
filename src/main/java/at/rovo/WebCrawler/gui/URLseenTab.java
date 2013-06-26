package at.rovo.WebCrawler.gui;

import at.rovo.WebCrawler.gui.events.URLseenEventListener;

public class URLseenTab extends DrumEventTab implements URLseenEventListener
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -6069995609576161045L;

	public URLseenTab(int numBuckets)
	{
		super(numBuckets);
		
		Manager.getInstance().registerForUrlSeenEvents(this);
	}
}
