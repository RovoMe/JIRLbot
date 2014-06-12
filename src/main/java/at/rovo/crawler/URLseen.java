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
 * {@link #checkUpdate(Integer, String, String)} provides a mechanism to check
 * and update unique URLs and disband duplicate ones.
 * </p>
 * <p>
 * Note that unique URLs are handled by {@link UniqueUrlDispatcher} and are
 * forwarded to the next queue
 * </p>
 * 
 * @author Roman Vottner
 */
public class URLseen
{
	private IDrum<StringSerializer, StringSerializer> drum = null;
	private int numBuckets = 0;
		
	public URLseen(IDispatcher<StringSerializer, StringSerializer> dispatcher, int numBuckets, int bucketByteSize, IDrumListener listener) throws DrumException
	{
		this.numBuckets = numBuckets;
		try
		{
			this.drum = new Drum.Builder<>("urlSeen", StringSerializer.class, StringSerializer.class)
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
	
	public void checkUpdate(String value, String aux)
	{
		StringSerializer valString = null;
		if (value != null)
			valString = new StringSerializer(value);
		StringSerializer auxString = null;
		if (aux != null)
			auxString = new StringSerializer(aux);
		this.drum.checkUpdate(DrumUtil.hash(aux), valString, auxString);
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
