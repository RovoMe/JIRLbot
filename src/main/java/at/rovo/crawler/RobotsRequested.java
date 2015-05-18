package at.rovo.crawler;

import at.rovo.caching.drum.Dispatcher;
import at.rovo.caching.drum.Drum;
import at.rovo.caching.drum.DrumBuilder;
import at.rovo.caching.drum.DrumException;
import at.rovo.caching.drum.DrumListener;
import at.rovo.caching.drum.data.StringSerializer;
import at.rovo.caching.drum.util.DrumUtils;
import at.rovo.crawler.bean.HostData;

/**
 * RobotsRequested is used for storing the hashes of sites for which a <em>robots.txt</em> has been requested. A
 * <em>robots.txt</em> file is either requested if no <em>robots.txt</em> is available yet or the old definition became
 * old and therefore got invalidated.
 *
 * @author Roman Vottner
 */
public final class RobotsRequested
{
    /** The backing cache to be used **/
    private Drum<HostData, StringSerializer> drum = null;
    /** The number of bucket files the DRUM instance should handle **/
    private int numBuckets = 0;

    /**
     * Creates a new instance and initializes a new backing <em>DRUM</em> cache which is used to store each robots.txt
     * download request and to decide if already a download was requested in a previous call.
     *
     * @param dispatcher
     *         The object that will handle results returned by the backing <em>DRUM</em> instance
     * @param numBuckets
     *         The number of disk bucket files to be used by the backing <em>DRUM</em> instance
     * @param byteSize
     *         The size of byte data stored into a disk bucket file before the content is synchronized with a backing
     *         database. Note however that large values might delay the dispatch of check results
     * @param listener
     *         An object that needs to be informed on any backing <em>DRUM</em> events
     *
     * @throws DrumException
     *         Will be thrown if the backing <em>DRUM</em> instance could not get initialized
     */
    public RobotsRequested(Dispatcher<HostData, StringSerializer> dispatcher, int numBuckets, int byteSize,
                           DrumListener listener) throws DrumException
    {
        this.numBuckets = numBuckets;
        try
        {
            this.drum =
                    new DrumBuilder<>("robotsRequested", HostData.class, StringSerializer.class).numBucket(numBuckets)
                            .bufferSize(byteSize).dispatcher(dispatcher).listener(listener).build();
        }
        catch (Exception e)
        {
            throw new DrumException(e.getLocalizedMessage(), e);
        }
    }

    /**
     * Executes an asynchronous check for the existence of any open download requests for the given host.
     * <p>
     * The results of this request will be handled by {@link RobotsRequestedDispatcher#uniqueKeyUpdate(Long, HostData,
     * StringSerializer)} in case no previous URL requested a download of a <em>robots.txt</em> file for this domain.
     *
     * @param hostData
     *         The data object containing further host information like any existing <em>robots.txt</em> files, the IP
     *         of the host and its host name
     * @param hostName
     *         The name of the host the <em>robots.txt</em> should be downloaded for
     */
    public void request(HostData hostData, String hostName)
    {
        // TODO: make clear why checkUpdate and not check - might be due to old robots.txt updates though
        this.drum.checkUpdate(DrumUtils.hash(hostName), hostData, new StringSerializer(hostName));
    }

    /**
     * Shutdown the currently running instance and its backing <em>DRUM</em> cache.
     *
     * @throws DrumException
     *         If during the shutdown of the backing <em>DRUM</em> cache an error occurred
     */
    public void dispose() throws DrumException
    {
        this.drum.dispose();
    }

    /**
     * Returns the number of configured in memory buffers and disk bucket files used by the backing <em>DRUM</em>
     * instance.
     *
     * @return The number of disk bucket files used by the backing cache
     */
    public int getNumberOfBuckets()
    {
        return this.numBuckets;
    }
}
