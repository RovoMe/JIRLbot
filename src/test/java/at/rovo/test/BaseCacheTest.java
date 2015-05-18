package at.rovo.test;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;

/**
 * Base class for all cache based unit tests. This abstract class does take responsibility of the creation and deletion
 * of the actual cache files and the initialization of the logger instance.
 */
public abstract class BaseCacheTest
{
    /** The logger of this class **/
    private final static Logger LOG = LogManager.getLogger(MethodHandles.lookup().lookupClass());

    protected File cacheDir = null;

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
        {
            this.cacheDir = new File(appDir.getAbsoluteFile() + "/cache");
        }
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
