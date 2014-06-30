package at.rovo.crawler.util;

import java.util.Comparator;
import at.rovo.crawler.bean.PLDData;

/**
 * <p>
 * Compares two {@link PLDData} instances by their budget and ranks the object
 * with the greater budget before the object with less budget. This will sort
 * the {@link PLDData} objects in reverse order by their budget listing the
 * object with the largest budget before objects with less budget.
 * </p>
 *
 * @param <T> The type of the data to compare
 */
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
