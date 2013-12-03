package at.rovo.crawler;

import at.rovo.caching.drum.Drum;
import at.rovo.caching.drum.DrumException;
import at.rovo.caching.drum.IDispatcher;
import at.rovo.caching.drum.IDrum;
import at.rovo.caching.drum.IDrumListener;
import at.rovo.caching.drum.data.StringSerializer;
import at.rovo.caching.drum.util.DrumUtil;
import at.rovo.crawler.bean.HostData;

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
		try
		{
			this.drum = new Drum.Builder<>("robotsRequested", HostData.class, StringSerializer.class)
					.numBucket(numBuckets)
					.bufferSize(byteSize)
					.dispatcher(dispatcher)
					.listener(listener)
					.build();
		}
		catch (Exception e)
		{
			throw new DrumException(e.getLocalizedMessage(), e);
		}
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
