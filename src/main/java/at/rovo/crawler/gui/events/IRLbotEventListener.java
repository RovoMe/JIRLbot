package at.rovo.crawler.gui.events;

import at.rovo.caching.drum.event.DrumEvent;

public interface IRLbotEventListener
{
	public void numberOfURLsCrawledTotalChanged(long size);
	public void numberOfURLsCrawledSuccessChanged(long size);
	public void numberOfURLsToCrawlChanged(long size);
	public void numberOfRobotsTxtToDownload(long size);
	public void numberOfUniqueURLs(DrumEvent<? extends DrumEvent<?>> event);
	public void numberOfUniquePLDs(DrumEvent<? extends DrumEvent<?>> event);
	public void numberOfUniqueRobotEntries(DrumEvent<? extends DrumEvent<?>> event);
	public void numberOfRobotEntriesRequested(DrumEvent<? extends DrumEvent<?>> event);
}
