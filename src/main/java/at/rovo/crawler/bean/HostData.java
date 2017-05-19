package at.rovo.crawler.bean;


import at.rovo.drum.data.ByteSerializable;
import at.rovo.drum.util.DrumUtils;

public class HostData implements ByteSerializable<HostData>
{
    private String robotsTxt = null;
    private String ipAddress = null;
    private String hostName = null;
    private transient long crawlDelay = 0;

    public HostData()
    {

    }

    public HostData(String hostName, String ipAddress, String robotsTxt)
    {
        this.robotsTxt = robotsTxt;
        this.ipAddress = ipAddress;
        this.hostName = hostName;
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

    public long getCrawlDelay() {
        return this.crawlDelay;
    }

    public void setCrawlDelay(long delay) {
        this.crawlDelay = delay;
    }

    private void writeObject(java.io.ObjectOutputStream stream) throws java.io.IOException
    {
        // 4 bytes int - size of hostName
        stream.writeInt(this.hostName.length());
        // n bytes char[] - hostName
        stream.write(this.hostName.getBytes());
        // 4 bytes int - size of ipAddress
        stream.writeInt(null != this.ipAddress ? this.ipAddress.length() : 0);
        if (null != this.ipAddress)
        {
            // (n bytes char[] - ipAddress; IPv4 or IPv6)
            stream.write(this.ipAddress.getBytes());
        }
        // 4 bytes int - size of robotsTxt
        stream.writeInt(null != this.robotsTxt ? this.robotsTxt.length() : 0);
        if (null != this.robotsTxt)
        {
            // (n bytes char[] - robotsTxt)
            stream.write(this.robotsTxt.getBytes());
        }
    }

    private void readObject(java.io.ObjectInputStream stream) throws java.io.IOException, ClassNotFoundException
    {
        // read hostName length
        int hostSize = stream.readInt();
        // read hostName
        byte[] hostNameBytes = new byte[hostSize];
        stream.readFully(hostNameBytes);
        String hostName = new String(hostNameBytes);

        // read ipAddress length
        int ipSize = stream.readInt();
        // read ipAddress
        byte[] ipAddressBytes = new byte[ipSize];
        stream.readFully(ipAddressBytes);
        String ipAddress = new String(ipAddressBytes);

        // read robots.txt length
        int robotsSize = stream.readInt();
        // read robots.txt
        String robotsTxt = null;
        if (robotsSize > 0)
        {
            byte[] robotsBytes = new byte[robotsSize];
            stream.readFully(robotsBytes);
            robotsTxt = new String(robotsBytes);
        }

        this.hostName = hostName;
        this.ipAddress = ipAddress;
        this.robotsTxt = robotsTxt;
    }

    @Override
    public byte[] toBytes()
    {
        // 4 bytes int - size of hostName
        // n bytes char[] - hostName
        // 4 bytes int - size of robotsTxt
        // (n bytes char[] - robotsTxt)
        int size = this.hostName.length() + 8;
        if (this.robotsTxt != null)
        {
            size += this.robotsTxt.length();
        }
        byte[] totalBytes = new byte[size];

        // copy size of hostName into byte array
        System.arraycopy(DrumUtils.int2bytes(this.hostName.length()), 0, totalBytes, 0, 4);
        // copy hostName characters into byte array
        System.arraycopy(this.hostName.getBytes(), 0, totalBytes, 4, this.hostName.length());
        if (this.robotsTxt != null)
        {
            // copy size of robots.txt into byte array
            System.arraycopy(DrumUtils.int2bytes(this.robotsTxt.length()), 0, totalBytes, this.hostName.length() + 4, 4);
            // copy robots.txt into the byte array
            System.arraycopy(this.robotsTxt.getBytes(), 0, totalBytes, this.hostName.length() + 8,
                             this.robotsTxt.length());
        }
        else
        {
            // copy size of robots.txt into byte array = 0
            System.arraycopy(DrumUtils.int2bytes(0), 0, totalBytes, this.hostName.length() + 4, 4);
        }

        return totalBytes;
    }

    @Override
    public HostData readBytes(byte[] data)
    {
        // read hostName length
        byte[] hostSizeBytes = new byte[4];
        System.arraycopy(data, 0, hostSizeBytes, 0, 4);
        int hostSize = DrumUtils.bytes2int(hostSizeBytes);
        // read hostName
        byte[] hostNameBytes = new byte[hostSize];
        System.arraycopy(data, 4, hostNameBytes, 0, hostSize);
        String hostName = new String(hostNameBytes);
        // read robots.txt length
        byte[] robotsSizeBytes = new byte[4];
        System.arraycopy(data, 4 + hostSize, robotsSizeBytes, 0, 4);
        int robotsSize = DrumUtils.bytes2int(robotsSizeBytes);
        // read robots.txt
        String robotsTxt = null;
        if (robotsSize > 0)
        {
            byte[] robotsBytes = new byte[robotsSize];
            System.arraycopy(data, 4 + hostSize + 4, robotsBytes, 0, robotsSize);
            robotsTxt = new String(robotsBytes);
        }
        // create a new object with the deserialized data
        return new HostData(hostName, ipAddress, robotsTxt);
    }

    @Override
    public String toString()
    {
        return this.hostName;
    }
}
