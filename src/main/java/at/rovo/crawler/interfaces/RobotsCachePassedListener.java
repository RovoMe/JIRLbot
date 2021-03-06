package at.rovo.crawler.interfaces;

import at.rovo.crawler.bean.HostData;

/**
 * The listener interface for receiving notifications if either the URL is allowed to be crawled according to rules
 * defined in <em>robots.txt</em> or if the <em>URL</em> could not be checked as no <em>robots.txt</em> file is
 * available yet. The class that is interested in processing these notifications has to implement this interface, and
 * the object created with that class is registered with a component, using the component's
 * <code>addRobotsCachePassedListener</code> method. When a notification on the successful check against the
 * <em>robots.txt</em> occurs, that object's {@link #handleURLsPassed(String, HostData)} method is invoked. In cases no
 * <em>robots.txt</em> is available, that object's {@link #handleUnableToCheck(String)} method will be invoked. If a URL
 * violated against an available <em>robots.txt</em> file, it will be silently ignored.
 */
public interface RobotsCachePassedListener
{
    /**
     * Indicates that the given <em>URL</em> passed the check against the <em>robots.txt</em> file.
     *
     * @param url
     *         The <em>URL</em> that passed the check against the <em>robots.txt</em> file
     * @param hostData
     *         Some data attached to the URL
     */
    void handleURLsPassed(String url, HostData hostData);

    /**
     * Indicates that the given <em>URL</em> could not get checked against a <em>robots.txt</em> file as the file is not
     * yet available.
     *
     * @param url
     *         The <em>URL</em> that could not get check against the <em>robots.txt</em> file
     */
    void handleUnableToCheck(String url);
}
