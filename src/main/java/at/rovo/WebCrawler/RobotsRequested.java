package at.rovo.WebCrawler;

import at.rovo.caching.drum.Drum;
import at.rovo.caching.drum.DrumException;
import at.rovo.caching.drum.IDispatcher;
import at.rovo.caching.drum.IDrum;
import at.rovo.caching.drum.IDrumListener;
import at.rovo.caching.drum.data.StringSerializer;
import at.rovo.caching.drum.util.DrumUtil;

/**
 * <p>RobotsRequested is used for storing the hashes of sites for which a robots.txt has been requested.</p>
 * 
 * @author Roman Vottner
 */
public class RobotsRequested
{
	private IDrum<HostData, StringSerializer> drum = null;
	private int numBuckets = 0;
	
	public RobotsRequested(IDispatcher<HostData, StringSerializer> dispatcher, int numBuckets, int byteSize, IDrumListener listener) throws DrumException
	{
		this.numBuckets = numBuckets;
		this.drum = new Drum<>("robotsRequested", numBuckets, byteSize, dispatcher, HostData.class, StringSerializer.class, listener);
	}
	
	public void request(HostData hostData, String hostName)
	{
		// TODO: make clear why checkUpdate and not check - might be due to old robots.txt updates though
		this.drum.checkUpdate(DrumUtil.hash(hostName), hostData, new StringSerializer(hostName));
	}
	
	public void synchronize() throws DrumException
	{
		this.drum.synchronize();
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
