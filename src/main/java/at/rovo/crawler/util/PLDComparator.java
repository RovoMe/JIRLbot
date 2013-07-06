package at.rovo.crawler.util;

import java.util.Comparator;
import at.rovo.crawler.bean.PLDData;

public class PLDComparator<T extends PLDData> implements Comparator<T>
{
	@Override
	public int compare(T data1, T data2) 
	{
		// reverse order sorting - bigger in-degree values should be
		// listed first
		if (data1.getIndegree() < data2.getIndegree())
			return 1;
		else if (data1.getIndegree() > data2.getIndegree())
			return -1;
		// if the in-degree values are equal sort by the hash value
		if (data1.getHash() < data2.getHash())
			return -1;
		else if (data1.getHash() > data2.getHash())
			return 1;
		// data-items are equal
		return 0;
	}
}
