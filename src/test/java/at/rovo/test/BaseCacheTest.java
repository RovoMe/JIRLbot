package at.rovo.test;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;

/**
 * <p>
 * Base class for all cache based unit tests. This abstract class does take
 * responsibility of the creation and deletion of the actual cache files and
 * the initialization of the logger instance.
 * </p>
 */
public abstract class BaseCacheTest
{
	/** The logger of this class **/
	protected static Logger LOG;

	protected File cacheDir = null;

	@BeforeClass
	public static final void initLogger() throws URISyntaxException
	{
		String path = RobotsCacheTest.class.getResource("/log/log4j2-test.xml").toURI().getPath();
		System.setProperty("log4j.configurationFile", path);
		LOG = LogManager.getLogger(RobotsCacheTest.class);
	}

	@AfterClass
	public static final void cleanLogger()
	{
		System.clearProperty("log4j.configurationFile");
	}

	@Before
	public final void init() throws Exception
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
					this.cacheDir = new File(item);
					if (this.cacheDir.isDirectory() && "cache".equals(this.cacheDir.getName()))
					{
						try
						{
							Files.walkFileTree(this.cacheDir.toPath(), new CacheFileDeleter());
						}
						catch (IOException e)
						{
							e.printStackTrace();
						}
					}
				}
			}
		}
		if (this.cacheDir == null)
			this.cacheDir = new File (appDir.getAbsoluteFile()+"/cache");
		if (!this.cacheDir.exists())
		{
			if (!this.cacheDir.mkdir())
			{
				throw new Exception("Cache did not exist yet and could not get created!");
			}
		}

		this.initDataDir();
	}

	public void initDataDir() throws Exception
	{

	}

	@After
	public final void clean() throws Exception
	{
		this.cleanDataDir();

		File cache = this.cacheDir;
		if (cache.isDirectory() && "cache".equals(cache.getName()))
		{
			try
			{
				Files.walkFileTree(cache.toPath(), new CacheFileDeleter());
			}
			catch (IOException e)
			{
				LOG.catching(e);
			}
		}
	}

	public void cleanDataDir() throws Exception
	{

	}
}
