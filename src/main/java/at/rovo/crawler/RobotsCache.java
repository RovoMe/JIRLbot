package at.rovo.crawler;

import at.rovo.caching.drum.Dispatcher;
import at.rovo.caching.drum.DrumBuilder;
import at.rovo.caching.drum.DrumListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import at.rovo.caching.drum.DrumException;
import at.rovo.caching.drum.Drum;
import at.rovo.caching.drum.data.StringSerializer;
import at.rovo.caching.drum.util.DrumUtil;
import at.rovo.crawler.bean.HostData;
import at.rovo.crawler.util.IRLbotUtil;

/**
 * <p>
 * For caching robots.txt, we have another <em>DRUM</em> structure called
 * RobotsCache, which supports asynchronous check and update operations
 * </p>
 * 
 * @author Roman Vottner
 */
public final class RobotsCache
{
	/** The logger of this class **/
	private final static Logger LOG = LogManager.getLogger(IRLbot.class);
	
	private int numBuckets = 0;
	private Drum<HostData,StringSerializer> drum = null;
	
	public RobotsCache(String name, Dispatcher<HostData,StringSerializer> dispatcher, int numBuckets, int bucketByteSize) throws DrumException
	{
		this.numBuckets = numBuckets;
		try
		{
			this.drum = new DrumBuilder<>(name, HostData.class, StringSerializer.class)
					.numBucket(numBuckets)
					.bufferSize(bucketByteSize)
					.dispatcher(dispatcher)
					.build();
		}
		catch (Exception e)
		{
			throw new DrumException(e.getLocalizedMessage(), e);
		}
	}
	
	public RobotsCache(Dispatcher<HostData,StringSerializer> dispatcher, int numBuckets, int bucketByteSize, DrumListener listener) throws DrumException
	{
		this.numBuckets = numBuckets;
		try
		{
			this.drum = new DrumBuilder<>("robotsCache", HostData.class, StringSerializer.class)
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
		
	public void check(String url)
	{
		LOG.debug("Checking URL {} for robots.txt compliance on host: {}", url, IRLbotUtil.getHostname(url));
		this.drum.check(DrumUtil.hash(IRLbotUtil.getHostname(url)), new StringSerializer(url));
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
