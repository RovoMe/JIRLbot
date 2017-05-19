package at.rovo.crawler;

import at.rovo.drum.Dispatcher;
import at.rovo.drum.Drum;
import at.rovo.drum.DrumBuilder;
import at.rovo.drum.DrumException;
import at.rovo.drum.DrumListener;
import at.rovo.drum.berkeley.BerkeleyDBStoreMerger;
import at.rovo.drum.util.DrumUtils;

/**
 * URLSeen stores a set of URLs inside a DRUM cache. {@link #checkURL(String, String)} provides a mechanism to check and
 * update unique URLs and disband duplicate ones.
 * <p>
 * Note that unique URLs are handled by {@link UniqueUrlDispatcher} and are forwarded to the next queue
 *
 * @author Roman Vottner
 */
public final class URLseen
{
    /** The backing DRUM cache instance **/
    private Drum<String, String> drum = null;
    /** The number of buckets used by the backing DRUM instance **/
    private int numBuckets = 0;

    /**
     * Initializes a new instance. While initializing a new backing <em>DRUM</em> cache will be initialized which keeps
     * track of already crawled URLs.
     *
     * @param dispatcher
     *         The class which handles the check results returned by the backing DRUM instance
     * @param numBuckets
     *         The number of buckets used by the backing DRUM instance. Note that the number of buckets should always be
     *         of power 2
     * @param bucketByteSize
     *         The size in bytes the bucket files in the backing DRUM cache instance will be merged into a data store
     * @param listener
     *         A listener instance that wants to be informed on any events sent by the backing DRUM cache instance
     *
     * @throws DrumException
     *         If any exceptions are thrown by the backing DRUM cache instance
     */
    public URLseen(Dispatcher<String, String> dispatcher, int numBuckets, int bucketByteSize,
                   DrumListener listener) throws DrumException
    {
        this.numBuckets = numBuckets;
        try
        {
            this.drum =
                    new DrumBuilder<>("urlSeen", String.class, String.class)
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

    /**
     * Checks if the provided <em>url</em> was already crawled in a previous iteration. Additional <em>data</em> may be
     * added to the URL.
     * <p>
     * The provided URL and data will be sent to the backing <em>DRUM</em> instance. Results will be dispatched via the
     * {@link at.rovo.drum.Dispatcher} provided via initialization.
     *
     * @param data
     *         Any additional data which should be passed via the URL for later use
     * @param url
     *         The URL to check if it was already crawled or not
     */
    public void checkURL(String data, String url)
    {
        this.drum.checkUpdate(DrumUtils.hash(url), data, url);
    }

    /**
     * Invokes {@link at.rovo.drum.Drum#dispose()} on the backing <em>DRUM</em> cache
     *
     * @throws DrumException
     *         If during the disposal an exception was caught
     */
    public void dispose() throws DrumException
    {
        this.drum.dispose();
    }

    /**
     * Returns the number of buckets used by the backing <em>DRUM</em> cache.
     *
     * @return The number of buckets used by the backing <em>DRUM</em> cache
     */
    public int getNumberOfBuckets()
    {
        return this.numBuckets;
    }
}
