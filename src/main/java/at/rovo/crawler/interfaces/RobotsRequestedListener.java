package at.rovo.crawler.interfaces;

/**
 * The listener interface for receiving <em>robot.txt</em> download requests. The class that is interested in processing
 * a <em>robot.txt</em> file download requests implements this interface. The object created with that class is
 * registered with a component using the component's <code>addRobotsRequestedListener</code> method. When a download
 * request occurs, that object's {@link #handleRobotsTxtDownloadRequests(String)} method is invoked.
 */
public interface RobotsRequestedListener
{
    /**
     * Indicates that a <em>robots.txt</em> file for the given hostname needs to be downloaded.
     *
     * @param hostName
     *         The name of the host to download the <em>robots.txt</em> file for
     */
    void handleRobotsTxtDownloadRequests(String hostName);
}
