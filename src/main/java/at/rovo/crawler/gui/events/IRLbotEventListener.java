package at.rovo.crawler.gui.events;

import at.rovo.drum.event.DrumEvent;

public interface IRLbotEventListener
{
    void numberOfURLsCrawledTotalChanged(long size);

    void numberOfURLsCrawledSuccessChanged(long size);

    void numberOfURLsToCrawlChanged(long size);

    void numberOfRobotsTxtToDownload(long size);

    void numberOfUniqueURLs(DrumEvent<? extends DrumEvent<?>> event);

    void numberOfUniquePLDs(DrumEvent<? extends DrumEvent<?>> event);

    void numberOfUniqueRobotEntries(DrumEvent<? extends DrumEvent<?>> event);

    void numberOfRobotEntriesRequested(DrumEvent<? extends DrumEvent<?>> event);
}
