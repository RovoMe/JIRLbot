package at.rovo.WebCrawler;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;
import at.rovo.caching.drum.data.AppendableData;
import at.rovo.caching.drum.util.DrumUtil;

public class PLDData implements AppendableData<PLDData>, Comparable<PLDData>
{
	private long hash = 0;
	private int budget = 0;
	private Set<Long> indegreeNeighbors = null;
	private transient String pld = null;
	
	public PLDData()
	{
		this.indegreeNeighbors = new LinkedHashSet<>();
	}
	
	public PLDData(long hash, int budget, Set<Long> indegreeNeighbors)
	{
		this.hash = hash;
		this.budget = budget;
		this.indegreeNeighbors = indegreeNeighbors;
	}
	
	public long getHash() { return this.hash; }
	public int getIndegree() { return this.indegreeNeighbors.size(); }
	public int getBudget() { return this.budget; }
	public Set<Long> getIndegreeNeighbors() { return this.indegreeNeighbors; }
	public String getPLD() { return this.pld; }
	
	public void setHash(long hash) { this.hash = hash; }
	public void setBudget(int budget) { this.budget = budget; }
	public void setIndegreeNeighbors(Set<Long> indegreeNeighbors) { this.indegreeNeighbors = indegreeNeighbors; }
	public void setPLD(String pld) { this.pld = pld; }
	
	public void addIndegreeNeighbors(Set<Long> indegreeNeighbors)
	{
		for (Long neighbor : indegreeNeighbors)
			this.indegreeNeighbors.add(neighbor);
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof PLDData)
		{
			PLDData data = (PLDData)obj;
			if (data.getHash() == this.hash)
				return true;
		}
		return false;
	}
	
	@Override
	public int hashCode()
	{
		int result = 17;
		result = 31*result + new Long(this.hash).hashCode();
		return result;
	}
	
	@Override
	public String toString()
	{
		StringBuffer buffer = new StringBuffer();
		buffer.append("Hash: ");
		buffer.append(this.hash);
		buffer.append("; neighbors: {");
		int size = this.indegreeNeighbors.size();
		int i = 0;
		for (Long neighbor : this.indegreeNeighbors)
		{
			i++;
			buffer.append(neighbor);
			if (i != size)
				buffer.append(", ");
		}
		buffer.append("}");
		
		return buffer.toString();
	}
	
	@Override
	public void append(PLDData data)
	{
		if (this.indegreeNeighbors == null)
			this.indegreeNeighbors = new TreeSet<>();
		if (data != null)
			this.indegreeNeighbors.addAll(data.getIndegreeNeighbors());
	}
	
	@Override
	public synchronized byte[] toBytes()
	{
		int size = 12+8*this.indegreeNeighbors.size()+4;
		byte[] totalBytes = new byte[size];
		byte[] keyBytes = DrumUtil.long2bytes(this.hash);
		System.arraycopy(keyBytes, 0, totalBytes, 0, 8);
		byte[] neighborSize = DrumUtil.int2bytes(this.indegreeNeighbors.size());
		System.arraycopy(neighborSize, 0, totalBytes, 8, 4);
		if (this.indegreeNeighbors.size() > 0);
		int pos = 12;
		for (Long neighbor : this.indegreeNeighbors)
		{
			byte[] neighborBytes = DrumUtil.long2bytes(neighbor);
			System.arraycopy(neighborBytes, 0, totalBytes, pos, 8);
			pos += 8;
		}
		byte[] budget = DrumUtil.int2bytes(this.budget);
		System.arraycopy(budget, 0, totalBytes, pos, 4);
		
		return totalBytes;
	}
	
	@Override
	public synchronized PLDData readBytes(byte[] bytes)
	{
		byte[] keyBytes = new byte[8];
		System.arraycopy(bytes, 0, keyBytes, 0, 8);
		long hash = DrumUtil.byte2long(keyBytes);
		
		byte[] valueSizeBytes = new byte[4];	
		System.arraycopy(bytes, 8, valueSizeBytes, 0, 4);
		int valueSize = DrumUtil.bytes2int(valueSizeBytes);
		
		TreeSet<Long> indegreeNeighbors = new TreeSet<>();
		
		int pos = 12;
		for (int i=0; i<valueSize; i++)
		{
			byte[] valueBytes = new byte[8];
			System.arraycopy(bytes, pos, valueBytes, 0, 8);
			indegreeNeighbors.add(DrumUtil.byte2long(valueBytes));
			pos += 8;
		}
		
		byte[] budgetBytes = new byte[4];
		System.arraycopy(bytes, pos, budgetBytes, 0, 4);
		int budget = DrumUtil.bytes2int(budgetBytes);
		
		PLDData data = new PLDData();
		data.setHash(hash);
		data.setIndegreeNeighbors(indegreeNeighbors);
		data.setBudget(budget);
		
		return data;
	}

	@Override
	public int compareTo(PLDData o)
	{		
		if (this.getHash() < o.getHash())
			return -1;
		else if (this.getHash() > o.getHash())
			return 1;
		
		return 0;
	}
}
