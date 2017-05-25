package at.rovo.crawler;

import at.rovo.drum.Dispatcher;
import at.rovo.drum.Drum;
import at.rovo.drum.DrumBuilder;
import at.rovo.drum.DrumException;
import at.rovo.drum.DrumListener;
import at.rovo.drum.berkeley.BerkeleyDBStoreMerger;
import at.rovo.drum.util.DrumUtils;
import at.rovo.crawler.bean.HostData;
import at.rovo.crawler.util.IRLbotUtils;
import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * For caching robots.txt, we have another <em>DRUM</em> structure called RobotsCache, which supports asynchronous check
 * and update operations
 *
 * @author Roman Vottner
 */
public final class RobotsCache
{
    /** The logger of this class **/
    private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private int numBuckets = 0;
    private Drum<HostData, String> drum = null;

    public RobotsCache(String name, Dispatcher<HostData, String> dispatcher, int numBuckets,
                       int bucketByteSize) throws DrumException
    {
        this.numBuckets = numBuckets;
        try
        {
            this.drum = new DrumBuilder<>(name, HostData.class, String.class)
                    .numBucket(numBuckets)
                    .bufferSize(bucketByteSize)
                    .dispatcher(dispatcher)
                    .datastore(BerkeleyDBStoreMerger.class)
                    .build();
        }
        catch (Exception e)
        {
            throw new DrumException(e.getLocalizedMessage(), e);
        }
    }

    public RobotsCache(Dispatcher<HostData, String> dispatcher, int numBuckets, int bucketByteSize,
                       DrumListener listener) throws DrumException
    {
        this.numBuckets = numBuckets;
        try
        {
            this.drum = new DrumBuilder<>("robotsCache", HostData.class, String.class)
                    .numBucket(numBuckets)
                    .bufferSize(bucketByteSize)
                    .dispatcher(dispatcher)
                    .listener(listener)
                    .datastore(BerkeleyDBStoreMerger.class)
                    .build();
        }
        catch (Exception e)
        {
            throw new DrumException(e.getLocalizedMessage(), e);
        }
    }

    public void check(String url)
    {
        LOG.debug("Checking URL {} for robots.txt compliance on host: {}", url, IRLbotUtils.getHostname(url));
        this.drum.check(DrumUtils.hash(IRLbotUtils.getHostname(url)), url);
    }

    public void update(Long key, HostData hostData)
    {
        LOG.debug("Receiving update on requested robots.txt for host {}", hostData.getHostName());
        this.drum.update(key, hostData);
    }

    public void dispose() throws DrumException
    {
        this.drum.dispose();
    }

    public int getNumberOfBuckets()
    {
        return this.numBuckets;
    }
}
