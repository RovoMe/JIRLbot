package at.rovo.test;

import static org.junit.Assert.assertNotNull;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import junit.framework.Assert;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import at.rovo.WebCrawler.HostData;
import at.rovo.WebCrawler.RobotsCache;
import at.rovo.WebCrawler.RobotsCacheDispatcher;
import at.rovo.WebCrawler.RobotsCachePassedListener;
import at.rovo.WebCrawler.Util;
import at.rovo.caching.drum.DrumException;
import at.rovo.caching.drum.DrumUtil;

public class RobotsCacheTest implements RobotsCachePassedListener
{
	// create a logger
	private final static Logger logger = LogManager.getLogger(RobotsCacheTest.class.getName()); 
	private RobotsCache robotsCache = null;
	private List<String> urlsPassed = null;
	private final String robotsCacheName = "RobotsCacheTest";
	
	private File cache = null;
	
	@Before
	public void init()
	{
		String appDirPath = System.getProperty("user.dir");
		File appDir = new File(appDirPath);
		if (appDir.isDirectory())
		{
			String[] items = appDir.list();
			for (String item : items)
			{
				if (item.endsWith("cache"))
				{
					this.cache = new File(item);
					if (this.cache.isDirectory() && "cache".equals(this.cache.getName()))
					{
						try
						{
							Files.walkFileTree(this.cache.toPath(), new CacheFileDeleter());
						}
						catch (IOException e)
						{
							e.printStackTrace();
						}
					}
				}
			}
		}
		if (this.cache == null)
			this.cache = new File (appDir.getAbsoluteFile()+"/cache");
		if (!this.cache.exists())
			this.cache.mkdir();
	}
	
	/**
	 * Example of RobotsCache usage:
	 *
	 * Initializing Drum ... 
	 * done!
	 * 
	 * unable to check URL: http://www.informatik.tuwien.ac.at/aktuelles/672
	 * unable to check URL: http://www.tuwien.ac.at/metanavigation/faqs/
	 * 
	 * Moking robot.txt update of 'http://www.tuwien.ac.at'
	 * check passed for URL: http://www.tuwien.ac.at/metanavigation/faqs/
	 * 
	 * unable to check URL: http://winf.at/rss-feed.php
	 * unable to check URL: http://winf.at/rss-feed.php
	 * unable to check URL: http://www.winf.at
	 * unable to check URL: http://www.facebook.com/events/350068195090400
	 * check passed for URL: http://www.tuwien.ac.at/metanavigation/links/
	 * check passed for URL: http://www.informatik.tuwien.ac.at/aktuelles/672
	 * check passed for URL: http://www.informatik.tuwien.ac.at/aktuelles/672
	 * 
	 * Moking robot.txt update of 'http://www.winf.at'
	 * 
	 * Disposing robotsCache ... 
	 * check passed for URL: http://www.winf.at
	 * done!
	 * 
	 * Data contained in cache.db:
	 * Key: -8811650085514601110, Value: http://winf.at
	 * Key: -7476758895180383974, Value: http://tuwien.ac.at
	 */
	
	@Test
	public void RobotsCacheDrumTest()
	{
		this.urlsPassed = new ArrayList<String>();
		
		try
		{
			logger.info("Example of RobotsCache usage:");
			logger.info("----------------------");
			logger.info("");
			
			logger.info("Initializing Drum ... ");
			RobotsCacheDispatcher dispatcher = new RobotsCacheDispatcher();
			dispatcher.addRobotsCachePassedListener(this);
			this.robotsCache = new RobotsCache(this.robotsCacheName, dispatcher, 16, 64);
			logger.info("done!");
			
			String url1 = "http://winf.at/rss-feed.php"; // key: -8811650085514601110
			String url2 = "http://www.informatik.tuwien.ac.at/aktuelles/672"; // key: -7476758895180383974
			String url3 = "http://www.tuwien.ac.at/metanavigation/faqs/"; // key: -7476758895180383974
			
			this.robotsCache.check(url1); // new host: http://winf.at - no robots.txt available by now
			this.robotsCache.check(url2); // new hosts: http://tuwien.ac.at - no robots.txt available by now
			this.robotsCache.check(url3); // host: http://tuwien.ac.at - robots.txt value was not yet updated!
			
			try
			{
				Thread.sleep(2000);
				logger.info("Moking robot.txt update of 'http://www.tuwien.ac.at'");
			}
			catch (InterruptedException e) 
			{
				e.printStackTrace();
			}
						
			this.robotsCache.check(url3); // re-check host: http://tuwien.ac.at - should now be available
			String url4 = "http://www.facebook.com/events/350068195090400"; // key: -7849402421258767002
			String url5 = "http://www.tuwien.ac.at/metanavigation/links/"; // key: -7476758895180383974
			
			this.robotsCache.check(url4); // new host: http://facebook.com - no robots.txt available
			this.robotsCache.check(url5); // should pass as inside robotsCache http://tuwien.ac.at 
			                              // got updated with the robots.txt value!
			this.robotsCache.check(url2); // re-check url2! should pass now
			this.robotsCache.check(url1); // re-check url1 (winf.at) will still not pass as the query still
			                              // resides in the bucket file and was not merged into the data 
			                              // storage
			try
			{
				Thread.sleep(1000);
			}
			catch (InterruptedException e) 
			{
				e.printStackTrace();
			}
			this.robotsCache.check(url2); // re-check url2! should pass too, but as it is already contained
			                              // only one entry should be in the list!	
			
			String url6 = "http://www.winf.at"; // key: -8811650085514601110
			this.robotsCache.check(url6); // this query for the PLD winf.at will result in a the 3 queries for
			                              // winf.at to be written to the bucket files and then be merged into
			                              // the data store. But as no robots.txt entries for the PLD are
			                              // available it will return an unable to check event
			try
			{
				Thread.sleep(2000);
				logger.info("Moking robot.txt update of 'http://www.winf.at'");
			}
			catch (InterruptedException e) 
			{
				e.printStackTrace();
			}
			
			this.robotsCache.check(url6); // this query will pass as in the meantime the backing drum structure
			                              // received an update for the robots.txt file of the PLD. The query
			                              // therefore is processed after the update and should succeed.
			
			String url7 = "http://www.winf.at/forum/shouldNotReturnAValue";
			this.robotsCache.check(url7); // URL 7 violates a rule defined in the robots.txt file
			                              // so it should neither pass nor return an unable to check event
		}
		catch(DrumException e)
		{
			System.err.println(e.getLocalizedMessage());
			e.printStackTrace();
		} 
		finally
		{
			// disposing the robots.txt cache forces a synchronization which results
			// in the robots.txt of winf.at to be written to the cache
			logger.info("Disposing robotsCache ... ");			
			if (this.robotsCache != null)
				this.robotsCache.dispose();
			logger.info("done!");
		}
		
		Assert.assertEquals(true, this.urlsPassed.contains("http://www.tuwien.ac.at/metanavigation/links/"));
		Assert.assertEquals(true, this.urlsPassed.contains("http://www.informatik.tuwien.ac.at/aktuelles/672"));
		Assert.assertEquals(true, this.urlsPassed.contains("http://www.tuwien.ac.at/metanavigation/faqs/"));
		Assert.assertEquals(true, this.urlsPassed.contains("http://www.winf.at"));
		Assert.assertEquals(4, this.urlsPassed.size());
		Assert.assertEquals(false, this.urlsPassed.contains("http://www.facebook.com/events/350068195090400"));
		Assert.assertEquals(false, this.urlsPassed.contains("http://winf.at/rss-feed.php"));
		
		try
		{
			try
			{
				Thread.sleep(1000);
			}
			catch (InterruptedException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			List<Long> keys = new ArrayList<>();
			Util.printCacheContent(this.robotsCacheName, keys, HostData.class);
			assertNotNull(keys);
			Assert.assertEquals(2, keys.size());
			// winf.at
			Assert.assertEquals(new Long(-8811650085514601110L), keys.get(0));
			// tuwien
			Assert.assertEquals(new Long(-7476758895180383974L), keys.get(1));
		}
		catch (IOException e)
		{
			System.err.println(e.getLocalizedMessage());
			e.printStackTrace();
		}
	}
	
	@Override
	public void handleURLsPassed(String url) 
	{
		logger.info("check passed for URL: "+url);
		if (!this.urlsPassed.contains(url))
			this.urlsPassed.add(url);
	}

	@Override
	public void handleUnableToCheck(String url) 
	{
		logger.info("unable to check URL: "+url);
		// URL would be forwarded to robotsRequest which manages
		// the download of a robots.txt file
		// here we will just send an imaginary robots.txt for certain hosts
		if ("http://tuwien.ac.at".equals(Util.getHostname(url)))
		{
			String robotsTxt = "# Zugriff fuer alle bots erlauben\n"+
					"User-agent: *\n"+
					"Disallow:\n"+
					"#Disallow: /\n"+
					"#User-agent: Googlebot\n"+
					"#Allow: /\n";
			try
			{
				Thread.sleep(500);
			}
			catch (InterruptedException e) 
			{
				e.printStackTrace();
			}
			this.robotsCache.update(DrumUtil.hash(Util.getHostname(url)), new HostData(Util.getHostname(url), null, robotsTxt));

		}
		else if ("http://winf.at".equals(Util.getHostname(url)))
		{
			String robotsTxt = "# robots.txt fuer winf.at (siehe www.robotstxt.org)\n"+
					"User-agent: *\n"+
					"Disallow: /forum/\n"+
					"Disallow: /angaben/\n"+
					"Disallow: /~leo/\n"+
					"\n"+
					"User-agent: ia_archiver\n"+
					"Disallow: /~klaus/\n";
			try
			{
				Thread.sleep(500);
			}
			catch (InterruptedException e) 
			{
				e.printStackTrace();
			}
			this.robotsCache.update(DrumUtil.hash(Util.getHostname(url)), new HostData(Util.getHostname(url), null, robotsTxt));
		}
	}
	
	@After
	public void clean()
	{
		File cache = this.cache;
		if (cache.isDirectory() && "cache".equals(cache.getName()))
		{
			try
			{
				Files.walkFileTree(cache.toPath(), new CacheFileDeleter());
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}
}
