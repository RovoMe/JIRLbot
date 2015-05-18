package at.rovo.crawler.interfaces;

/**
 * The listener interface for receiving unique URL notifications. The class that is interested in processing a unique
 * URL implements this interface, and the object created with that class is registered with a component, using the
 * component's <code>addUniqueUrlListener</code> method. When the unique URL is found, that object's {@link
 * #handleUniqueURL(String)} method is invoked.
 */
public interface UniqueUrlListener
{
    /**
     * Indicates that the given <em>URL</em> was not crawled before and therefore is eligible for further crawling.
     *
     * @param url
     *         The <em>URL</em> which passed all checks and is eligible for further crawling
     */
    void handleUniqueURL(String url);
}
