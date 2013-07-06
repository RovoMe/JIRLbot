package at.rovo.crawler.gui.events;

import at.rovo.caching.drum.event.DrumEvent;

public interface RobotsRequestedEventListener
{
	public void stateChanged(DrumEvent<? extends DrumEvent<?>> event);
	public void actionPerformed(DrumEvent<? extends DrumEvent<?>> event);
}
