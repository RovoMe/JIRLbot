package at.rovo.crawler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import at.rovo.caching.drum.Drum;
import at.rovo.caching.drum.DrumException;
import at.rovo.caching.drum.IDispatcher;
import at.rovo.caching.drum.IDrum;
import at.rovo.caching.drum.IDrumListener;
import at.rovo.caching.drum.data.StringSerializer;
import at.rovo.caching.drum.util.DrumUtil;
import at.rovo.crawler.bean.HostData;
import at.rovo.crawler.util.IRLbotUtil;

/**
 * <p>
 * For caching robots.txt, we have another DRUM structure called RobotsCache,
 * which supports asynchronous check and update operations
 * </p>
 * 
 * @author Roman Vottner
 */
public class RobotsCache
{
	/** The logger of this class **/
	private final static Logger logger = LogManager.getLogger(IRLbot.class); 
	
	private int numBuckets = 0;
	private IDrum<HostData,StringSerializer> drum = null;
	
	public RobotsCache(String name, IDispatcher<HostData,StringSerializer> dispatcher, int numBuckets, int bucketByteSize) throws DrumException
	{
		this.numBuckets = numBuckets;
		this.drum = new Drum<>(name, numBuckets, bucketByteSize, dispatcher, HostData.class, StringSerializer.class);
	}
	
	public RobotsCache(IDispatcher<HostData,StringSerializer> dispatcher, int numBuckets, int bucketByteSize) throws DrumException
	{
		this.numBuckets = numBuckets;
		this.drum = new Drum<>("robotsCache", numBuckets, bucketByteSize, dispatcher, HostData.class, StringSerializer.class);
	}
	
	public RobotsCache(String name, IDispatcher<HostData,StringSerializer> dispatcher, int numBuckets, int bucketByteSize, IDrumListener listener) throws DrumException
	{
		this.numBuckets = numBuckets;
		this.drum = new Drum<>(name, numBuckets, bucketByteSize, dispatcher, HostData.class, StringSerializer.class, listener);
	}
	
	public RobotsCache(IDispatcher<HostData,StringSerializer> dispatcher, int numBuckets, int bucketByteSize, IDrumListener listener) throws DrumException
	{
		this.numBuckets = numBuckets;
		this.drum = new Drum<>("robotsCache", numBuckets, bucketByteSize, dispatcher, HostData.class, StringSerializer.class, listener);
	}
		
	public void check(String url)
	{
		logger.debug("Checking URL {} for robots.txt compliance on host: {}", url, IRLbotUtil.getHostname(url));
		this.drum.check(DrumUtil.hash(IRLbotUtil.getHostname(url)), new StringSerializer(url));
	}
	
	public void update(Long key, HostData hostData)
	{
		logger.debug("Receiving update on requested robots.txt for host {}", hostData.getHostName());
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
