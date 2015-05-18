package at.rovo.test;

import at.rovo.caching.drum.DrumException;
import at.rovo.caching.drum.util.DrumUtils;
import at.rovo.crawler.bean.PLDData;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Lists the content of a specified DRUM cache.db
 */
public class CacheDBContentLister
{

    public static void main(String ... args) throws DrumException, IOException
    {
        List<Long> keys = new ArrayList<>();
        DrumUtils.printCacheContent("pldIndegree", keys, PLDData.class);
//        DrumUtil.printCacheContent("robotsCache", keys, HostData.class);
//        DrumUtil.printCacheContent("robotsRequested", keys, HostData.class);
//        DrumUtil.printCacheContent("urlSeen", keys, StringSerializer.class);
    }
}
