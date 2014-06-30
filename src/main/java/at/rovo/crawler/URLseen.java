package at.rovo.crawler;

import at.rovo.caching.drum.Drum;
import at.rovo.caching.drum.DrumException;
import at.rovo.caching.drum.IDispatcher;
import at.rovo.caching.drum.IDrum;
import at.rovo.caching.drum.IDrumListener;
import at.rovo.caching.drum.data.StringSerializer;
import at.rovo.caching.drum.util.DrumUtil;

/**
 * <p>
 * URLSeen stores a set of URLs inside a DRUM cache.
 * {@link #checkURL(String, String)} provides a mechanism to check and update
 * unique URLs and disband duplicate ones.
 * </p>
 * <p>
 * Note that unique URLs are handled by {@link UniqueUrlDispatcher} and are
 * forwarded to the next queue
 * </p>
 * 
 * @author Roman Vottner
 */
public final class URLseen
{
	/** The backing DRUM cache instance **/
	private IDrum<StringSerializer, StringSerializer> drum = null;
	/** The number of buckets used by the backing DRUM instance **/
	private int numBuckets = 0;

	/**
	 * <p>
	 * Initializes a new instance. While initializing a new backing
	 * <em>DRUM</em> cache will be initialized which keeps track of already
	 * crawled URLs.
	 * </p>
	 *
	 * @param dispatcher
	 *            The class which handles the check results returned by the
	 *            backing DRUM instance
	 * @param numBuckets
	 *            The number of buckets used by the backing DRUM instance. Note
	 *            that the number of buckets should always be of power 2
	 * @param bucketByteSize
	 *            The size in bytes the bucket files in the backing DRUM cache
	 *            instance will be merged into a data store
	 * @param listener
	 *            A listener instance that wants to be informed on any events
	 *            sent by the backing DRUM cache instance
	 * @throws DrumException If any exceptions are thrown by the backing DRUM
	 *         cache instance
	 */
	public URLseen(IDispatcher<StringSerializer, StringSerializer> dispatcher,
				   int numBuckets, int bucketByteSize, IDrumListener listener)
			throws DrumException
	{
		this.numBuckets = numBuckets;
		try
		{
			this.drum = new Drum.Builder<>(
					"urlSeen",
					StringSerializer.class,
					StringSerializer.class)
					.numBucket(numBuckets)
					.bufferSize(bucketByteSize)
					.dispatcher(dispatcher)
					.listener(listener)
					.build();
		}
		catch (Exception e)
		{
			throw new DrumException(e.getLocalizedMessage(), e);
		}
	}

	/**
	 * <p>
	 * Checks if the provided <em>url</em> was already crawled in a previous
	 * iteration. Additional <em>data</em> may be added to the URL.
	 * </p>
	 * <p>
	 * The provided URL and data will be sent to the backing <em>DRUM</em>
	 * instance. Results will be dispatched via the {@link IDispatcher}
	 * provided via initialization.
	 * </p>
	 * @param data
	 *            Any additional data which should be passed via the URL for
	 *            later use
	 * @param url
	 *            The URL to check if it was already crawled or not
	 */
	public void checkURL(String data, String url)
	{
		StringSerializer valString = null;
		if (data != null)
			valString = new StringSerializer(data);
		StringSerializer auxString = null;
		if (url != null)
			auxString = new StringSerializer(url);
		this.drum.checkUpdate(DrumUtil.hash(url), valString, auxString);
	}

	/**
	 * <p>
	 * Invokes {@link IDrum#dispose()} on the backing <em>DRUM</em> cache
	 * </p>
	 *
	 * @throws DrumException If during the disposal an exception was caught
	 */
	public void dispose() throws DrumException
	{
		this.drum.dispose();
	}

	/**
	 * <p>
	 * Returns the number of buckets used by the backing <em>DRUM</em> cache.
	 * </p>
	 *
	 * @return The number of buckets used by the backing <em>DRUM</em> cache
	 */
	public int getNumberOfBuckets()
	{
		return this.numBuckets;
	}
}
