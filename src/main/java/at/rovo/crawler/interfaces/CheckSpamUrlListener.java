package at.rovo.crawler.interfaces;

/**
 * The listener interface for receiving a spam check request. The class that is interested in processing these spam
 * check requests needs to implement this interface, and the object created with that class is registered with a
 * component, using the component's <code>addCheckSpamListener</code> method. When the spam check request occurs, that
 * object's {@link #handleSpamCheck(String, int)} method is invoked.
 */
public interface CheckSpamUrlListener
{
    /**
     * Indicates that the given URL needs to be checked against originating from a spam source or for already having
     * crawled enough pages from the <em>URL</em>s host.
     *
     * @param url
     *         The URL to check if it is originating from a known spam source
     * @param budget
     *         The current budget available for the host of the URL. If to many pages have been crawled from a not so
     *         famous page, further crawling of pages on that site should be reduced or stopped in general
     */
    void handleSpamCheck(String url, int budget);
}
