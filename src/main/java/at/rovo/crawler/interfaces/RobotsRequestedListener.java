package at.rovo.crawler.interfaces;

/**
 * <p>
 * The listener interface for receiving <em>robot.txt</em> download requests.
 * The class that is interested in processing a <em>robot.txt</em>
 * file download requests implements this interface, and the object created
 * with that class is registered with a component, using the component's
 * <code>addRobotsRequestedListener</code> method. When the download request
 * occurs, that object's {@link #handleRobotsTxtDownloadRequests(String)}
 * method is invoked.
 * </p>
 */
public interface RobotsRequestedListener 
{
	/**
	 * <p>
	 * Indicates that a <em>robots.txt</em> file for the given hostname needs
	 * to be downloaded.
	 * </p>
	 *
	 * @param hostName
	 *            The name of the host to download the <em>robots.txt</em> file
	 *            for
	 */
	public void handleRobotsTxtDownloadRequests(String hostName);
}
