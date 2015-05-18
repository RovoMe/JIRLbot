package at.rovo.crawler.gui;

import at.rovo.caching.drum.event.DrumEvent;
import at.rovo.crawler.IRLbot;
import at.rovo.crawler.gui.events.BEASTEventListener;
import at.rovo.crawler.gui.events.IRLbotCreationListener;
import at.rovo.crawler.gui.events.IRLbotEventListener;
import at.rovo.crawler.gui.events.RobotsCacheEventListener;
import at.rovo.crawler.gui.events.RobotsRequestedEventListener;
import at.rovo.crawler.gui.events.STAREventListener;
import at.rovo.crawler.gui.events.URLseenEventListener;
import at.rovo.crawler.interfaces.IRLbotListener;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@SuppressWarnings("unused")
public class Manager implements IRLbotListener
{
    private static Manager INSTANCE = null;
    private IRLbot crawler = null;
    private Thread crawlerThread = null;
    private Set<IRLbotEventListener> irlbotEventListeners = new CopyOnWriteArraySet<>();
    private Set<URLseenEventListener> urlseenEventListeners = new CopyOnWriteArraySet<>();
    private Set<STAREventListener> starEventListeners = new CopyOnWriteArraySet<>();
    private Set<BEASTEventListener> beastEventListeners = new CopyOnWriteArraySet<>();
    private Set<RobotsCacheEventListener> robotsCacheEventListeners = new CopyOnWriteArraySet<>();
    private Set<RobotsRequestedEventListener> robotsRequestedEventLisetenres = new CopyOnWriteArraySet<>();
    private Set<IRLbotCreationListener> irlbotCreationListener = new CopyOnWriteArraySet<>();

    private Manager()
    {

    }

    public static Manager getInstance()
    {
        if (INSTANCE == null)
        {
            INSTANCE = new Manager();
        }
        return INSTANCE;
    }

    public IRLbot getIRLbot()
    {
        return this.crawler;
    }

    public Thread createCrawler(String[] urls, int numCrawler, int numRobotDownloader, int urlSeenBuckets,
                                int urlSeenByteSize, int pldBuckets, int pldByteSize, int robotsCacheBuckets,
                                int robotsCacheByteSize, int robotsRequestedBuckets, int robotsRequestedByteSize)
    {
        this.crawler = new IRLbot(urls, numCrawler, numRobotDownloader, urlSeenBuckets, pldBuckets, robotsCacheBuckets,
                                  robotsRequestedBuckets, urlSeenByteSize, pldByteSize, robotsCacheByteSize,
                                  robotsRequestedByteSize);

        this.crawler.addIRLbotListener(this);
        if (crawlerThread == null)
        {
            this.crawlerThread = new Thread(this.crawler);
            this.crawlerThread.setName("IRLbot");
            this.crawlerThread.start();
        }

        this.irlbotCreationListener.forEach(IRLbotCreationListener::irlbotCreated);

        return this.crawlerThread;
    }

    // Listener Section

    public void registerForIRLbotCreationEvents(IRLbotCreationListener listener)
    {
        this.irlbotCreationListener.add(listener);
    }

    public void unregisterForIRLbotCreatetionEvents(IRLbotCreationListener listener)
    {
        this.irlbotCreationListener.remove(listener);
    }

    public void registerForIRLbotEvents(IRLbotEventListener listener)
    {
        this.irlbotEventListeners.add(listener);
    }

    public void unregisterFromIRLbotEvents(IRLbotEventListener listener)
    {
        this.irlbotEventListeners.remove(listener);
    }

    public void registerForUrlSeenEvents(URLseenEventListener listener)
    {
        this.urlseenEventListeners.add(listener);
    }

    public void unregisterFromUrlSeenEvents(URLseenEventListener listener)
    {
        this.urlseenEventListeners.remove(listener);
    }

    public void registerForStarEvents(STAREventListener listener)
    {
        this.starEventListeners.add(listener);
    }

    public void unregisterFromStarEvents(STAREventListener listener)
    {
        this.starEventListeners.remove(listener);
    }

    public void registerForBeastEvents(BEASTEventListener listener)
    {
        this.beastEventListeners.add(listener);
    }

    public void unregisterFromBeastEvents(BEASTEventListener listener)
    {
        this.beastEventListeners.remove(listener);
    }

    public void registerForRobotsCacheEvents(RobotsCacheEventListener listener)
    {
        this.robotsCacheEventListeners.add(listener);
    }

    public void unregisterFromRobotsCacheEvents(RobotsCacheEventListener listener)
    {
        this.robotsCacheEventListeners.remove(listener);
    }

    public void registerForRobotsRequestedEvents(RobotsRequestedEventListener listener)
    {
        this.robotsRequestedEventLisetenres.add(listener);
    }

    public void unregisterFromRobotsRequestedEvents(RobotsRequestedEventListener listener)
    {
        this.robotsRequestedEventLisetenres.remove(listener);
    }

    @Override
    public void numberOfURLsCrawledTotalChanged(long newSize)
    {
        this.irlbotEventListeners.forEach(listener -> listener.numberOfURLsCrawledTotalChanged(newSize));
    }

    @Override
    public void numberOfURLsCrawledSuccessChanged(long newSize)
    {
        this.irlbotEventListeners.forEach(listener -> listener.numberOfURLsCrawledSuccessChanged(newSize));
    }

    @Override
    public void numberOfURLsToCrawlChanged(long newSize)
    {
        this.irlbotEventListeners.forEach(listener -> listener.numberOfURLsToCrawlChanged(newSize));
    }

    @Override
    public void sizeOfRobotTxtDownloadQueue(long newSize)
    {
        this.irlbotEventListeners.forEach(listener -> listener.numberOfRobotsTxtToDownload(newSize));
    }

    @Override
    public void drumUpdate(DrumEvent<? extends DrumEvent<?>> event)
    {
        switch (event.getDrumName())
        {
            case "urlSeen":
            {
                for (URLseenEventListener listener : this.urlseenEventListeners)
                {
                    if (event.getRealClass().getName().endsWith("StateUpdate"))
                    {
                        listener.stateChanged(event);
                    }
                    else
                    {
                        listener.actionPerformed(event);
                    }
                }
                this.irlbotEventListeners.forEach(listener -> {
                    if (event.getRealClass().getName().endsWith("StorageEvent"))
                    {
                        listener.numberOfUniqueURLs(event);
                    }
                });
                break;
            }
            case "pldIndegree":
            {
                for (STAREventListener listener : this.starEventListeners)
                {
                    if (event.getRealClass().getName().endsWith("StateUpdate"))
                    {
                        listener.stateChanged(event);
                    }
                    else
                    {
                        listener.actionPerformed(event);
                    }
                }
                this.irlbotEventListeners.forEach(listener -> {
                    if (event.getRealClass().getName().endsWith("StorageEvent"))
                    {
                        listener.numberOfUniquePLDs(event);
                    }
                });
                break;
            }
            case "robotsCache":
            {
                for (RobotsCacheEventListener listener : this.robotsCacheEventListeners)
                {
                    if (event.getRealClass().getName().endsWith("StateUpdate"))
                    {
                        listener.stateChanged(event);
                    }
                    else
                    {
                        listener.actionPerformed(event);
                    }
                }
                this.irlbotEventListeners.forEach(listener -> {
                    if (event.getRealClass().getName().endsWith("StorageEvent"))
                    {
                        listener.numberOfUniqueRobotEntries(event);
                    }
                });
                break;
            }
            case "robotsRequested":
            {
                for (RobotsRequestedEventListener listener : this.robotsRequestedEventLisetenres)
                {
                    if (event.getRealClass().getName().endsWith("StateUpdate"))
                    {
                        listener.stateChanged(event);
                    }
                    else
                    {
                        listener.actionPerformed(event);
                    }
                }
                this.irlbotEventListeners.forEach(listener -> {
                    if (event.getRealClass().getName().endsWith("StorageEvent"))
                    {
                        listener.numberOfRobotEntriesRequested(event);
                    }
                });
                break;
            }
        }
    }
}
