package at.rovo.WebCrawler;

import at.rovo.caching.drum.Drum;
import at.rovo.caching.drum.DrumUtil;
import at.rovo.caching.drum.IDispatcher;
import at.rovo.caching.drum.IDrum;
import at.rovo.caching.drum.IDrumListener;
import at.rovo.caching.drum.StringSerializer;

/**
 * <p>URLSeen stores a set of URLs inside a DRUM cache. {@link #checkUpdate(Integer, String, String)} 
 * provides a mechanism to check and update unique URLs and disband duplicate ones.</p>
 * <p>Note that unique URLs are handled by {@link UniqueUrlDispatcher} and are forwarded 
 * to the next queue</p>
 * 
 * @author Roman Vottner
 */
public class URLseen
{
	private IDrum<StringSerializer, StringSerializer> drum = null;
	private int numBuckets = 0;
		
	public URLseen(IDispatcher<StringSerializer, StringSerializer> dispatcher, int numBuckets, int bucketByteSize, IDrumListener listener)
	{
		this.numBuckets = numBuckets;
		this.drum = new Drum<>("urlSeen", numBuckets, bucketByteSize, dispatcher, StringSerializer.class, StringSerializer.class, listener);
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
	
	public void synchronize()
	{
		this.drum.synchronize();
	}
	
	public void dispose()
	{
		this.drum.dispose();
	}
	
	public int getNumberOfBuckets()
	{
		return this.numBuckets;
	}
}
