package at.rovo.WebCrawler;

import at.rovo.caching.drum.data.ByteSerializer;
import at.rovo.caching.drum.util.DrumUtil;

public class HostData implements ByteSerializer<HostData>
{
	private String robotsTxt = null;
	private String ipAddress = null;
	private String hostName = null;
	
	public HostData()
	{
		
	}
	
	public HostData(String hostName, String ipAddress, String robotsTxt)
	{
		this.robotsTxt = robotsTxt;
		this.ipAddress = ipAddress;
		this.hostName = hostName;
	}
	
	public void setHostName(String hostName)
	{
		this.hostName = hostName;
	}
	
	public void setRobotsTxt(String robotsTxt)
	{
		this.robotsTxt = robotsTxt;
	}
	
	public void setIPAddress(String ipAddress)
	{
		this.ipAddress = ipAddress;
	}
	
	public void setIPAddressAsString(String ipAddress)
	{
		this.ipAddress = ipAddress;
	}
	
	public String getHostName()
	{
		return this.hostName;
	}
	
	public String getRobotsTxt()
	{
		return this.robotsTxt;
	}
	
	public String getIPAddress()
	{
		return this.ipAddress;
	}
	
	@Override
	public String toString()
	{
		return this.hostName;
	}

	@Override
	public byte[] toBytes()
	{
		// 4 bytes int - size of hostName
		// n bytes char[] - hostName
		// 4 bytes int - size of robotsTxt
		// (n bytes char[] - robotsTxt)
		int size = this.hostName.length()+8;
		if (this.robotsTxt != null)
			size += this.robotsTxt.length();
		byte[] totalBytes = new byte[size];
		
		// copy size of hostName into byte array
		System.arraycopy(DrumUtil.int2bytes(this.hostName.length()), 0, totalBytes, 0, 4);
		// copy hostName characters into byte array
		System.arraycopy(this.hostName.getBytes(), 0, totalBytes, 4, this.hostName.length());
		if (this.robotsTxt != null)
		{
			// copy size of robots.txt into byte array
			System.arraycopy(DrumUtil.int2bytes(this.robotsTxt.length()), 0, totalBytes, this.hostName.length()+4, 4);
			// copy robots.txt into the byte array
			System.arraycopy(this.robotsTxt.getBytes(), 0, totalBytes, this.hostName.length()+8, this.robotsTxt.length());
		}
		else
		{
			// copy size of robots.txt into byte array = 0
			System.arraycopy(DrumUtil.int2bytes(0), 0, totalBytes, this.hostName.length()+4, 4);
		}
		
		return totalBytes;
	}

	@Override
	public HostData readBytes(byte[] data)
	{
		// read hostName length
		byte[] hostSizeBytes = new byte[4];
		System.arraycopy(data, 0, hostSizeBytes, 0, 4);
		int hostSize = DrumUtil.bytes2int(hostSizeBytes);
		// read hostName
		byte[] hostNameBytes = new byte[hostSize];
		System.arraycopy(data, 4, hostNameBytes, 0, hostSize);
		String hostName = new String(hostNameBytes);
		// read robots.txt length
		byte[] robotsSizeBytes = new byte[4];
		System.arraycopy(data, 4+hostSize, robotsSizeBytes, 0, 4);
		int robotsSize = DrumUtil.bytes2int(robotsSizeBytes);
		// read robots.txt
		String robotsTxt = null;
		if (robotsSize > 0)
		{
			byte[] robotsBytes = new byte[robotsSize];
			System.arraycopy(data, 4+hostSize+4, robotsBytes, 0, robotsSize);
			robotsTxt = new String(robotsBytes);
		}
		// create a new object with the deserialized data
		HostData host = new HostData();
		host.setHostName(hostName);
		host.setRobotsTxt(robotsTxt);
		
		return host;
	}
}
