package at.rovo.WebCrawler;

import at.rovo.caching.drum.event.DrumEvent;

public interface IRLbotListener
{
	public void numberOfURLsCrawledTotalChanged(long newSize);
	public void numberOfURLsCrawledSuccessChanged(long newSize);
	public void numberOfURLsToCrawlChanged(long newSize);
	public void sizeOfRobotTxtDownloadQueue(long newSize);
	public void drumUpdate(DrumEvent<? extends DrumEvent<?>> event);
}
