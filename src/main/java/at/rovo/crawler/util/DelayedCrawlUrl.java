package at.rovo.crawler.util;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("NullableProblems")
public class DelayedCrawlUrl implements Delayed
{
    private String url;
    private long pldDelay;
    private long startTime;
    private TimeUnit unit;

    public DelayedCrawlUrl(String url, long pldDelay, TimeUnit unit)
    {
        this.url = url;
        this.pldDelay = TimeUnit.MILLISECONDS.convert(pldDelay, unit);
        this.unit = unit;
        this.startTime = System.currentTimeMillis() + this.pldDelay;
    }

    public String getUrl()
    {
        return this.url;
    }

    public long getPldDelay()
    {
        return unit.convert(this.pldDelay, TimeUnit.MILLISECONDS);
    }

    public TimeUnit getTimeUnit() {
        return unit;
    }

    public long getStartTime()
    {
        return startTime;
    }

    @Override
    public long getDelay(TimeUnit unit)
    {
        long diff = startTime - System.currentTimeMillis();
        return unit.convert(diff, TimeUnit.SECONDS);
    }

    @Override
    public int compareTo(Delayed o)
    {
        if (this.startTime < ((DelayedCrawlUrl) o).startTime)
        {
            return -1;
        }
        if (this.startTime > ((DelayedCrawlUrl) o).startTime)
        {
            return 1;
        }
        return 0;
    }

    @Override
    public String toString()
    {
        return url;
    }
}
