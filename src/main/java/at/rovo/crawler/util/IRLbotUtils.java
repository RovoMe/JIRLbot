package at.rovo.crawler.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class IRLbotUtils
{
    /** The logger of this class **/
    private final static Logger LOG = LogManager.getLogger(IRLbotUtils.class);

    /**
     * Extracts the pay level domain from a URL
     *
     * @param url
     *         The absolute URL of any web page
     *
     * @return The pay level domain of the provided URL or null if the pay level domain could not be extracted
     */
    public static String getPLDofURL(String url)
    {
        String origin = url;
        try
        {
            LOG.debug("Extracting PLD of {}", url);
            // remove leading 'http://' or 'https://'
            if (url.contains("://"))
            {
                url = url.substring(url.indexOf("://") + 3);
            }
            // remove everything after the first '/' which indicates the first
            // sub-directory
            if (url.contains("/"))
            {
                url = url.substring(0, url.indexOf("/"));
            }
            // remove the port from the PLD
            if (url.contains(":"))
            {
                url = url.substring(0, url.indexOf(":"));
            }
            if (url.contains("?"))
            {
                url = url.substring(0, url.indexOf("?"));
            }
            if (url.startsWith("localhost") || url.startsWith("192.168."))
            {
                return url;
            }
            // fetch the top level domain
            if (!url.contains(".")) {
                LOG.warn("Could not determine PLD for {}", url);
                return url;
            }
            String TLD = url.substring(url.lastIndexOf("."));
            // and split the rest of the URL apart from the TLD
            String rest = url.substring(0, url.lastIndexOf(TLD));

            // check if we have found an extended TLD
            // if so, add this part to the TLD
            if (rest.endsWith(".co") || rest.endsWith("wa.edu") || rest.endsWith(".edu") || rest.endsWith(".ac") ||
                rest.endsWith(".go"))
            {
                TLD = rest.substring(rest.lastIndexOf(".")) + TLD;
                rest = url.substring(0, url.lastIndexOf(TLD));
            }

            // we now only have to look up the part after the last
            // remained '.' and add the TLD to its end
            String PLD = rest.substring(rest.lastIndexOf(".") + 1) + TLD;
            LOG.debug("PLD: {}", PLD);
            return PLD;
        }
        catch (IndexOutOfBoundsException e)
        {
            LOG.error("Error extracting PLD of url: {}", origin);
            LOG.catching(e);
            throw e;
        }
    }

    /**
     * Returns the host name of a URL
     *
     * @param url
     *         The URL to extract the host name from
     *
     * @return The host name of a URL
     */
    public static String getHostname(String url)
    {
        return "http://" + getPLDofURL(url);
    }

    /**
     * Returns the directory path of a URL without its domain-part.
     * <p>
     * F.e the directory path of <em> http://http://irl.cs.tamu.edu/people/hsin-tsang/papers/www2008.pdf</em> is
     * <em>hsing-tsang/papers/www2008.pdf</em>.
     *
     * @param url
     *         The URL to extract the
     *
     * @return The directory path of the given URL
     */
    public static String getDirectoryPathOfUrl(String url)
    {
        // get the pay level domain of the URL
        String PLD = IRLbotUtils.getPLDofURL(url);
        // find the pay level domain in the URL and set the cursor
        return url.substring(url.indexOf(PLD) + PLD.length());
    }

    /**
     * Checks if a URL is in an absolute format and if not it tries to transform it into an absolute URL using the
     * parent URL.
     * <p>
     * Note that URLs that start with an anchor, javascript or mailto tag are discarded and null will be returned
     * therefore.
     *
     * @param url
     *         The URL that should be checked for its absolute format
     * @param parentURL
     *         The parent URL of <em>url</em> which will be used to transform the relative URL into an absolute one
     *
     * @return The absolute URL or null if <em>url</em> was either an anchor, javascript or mailto tag
     */
    public static String checkAndTransformURL(String url, String parentURL)
    {
        url = url.trim();
        if (!url.endsWith("/") && !url.contains("?") && url.lastIndexOf("/") > url.lastIndexOf(".") &&
            url.contains("#"))
        {
            url += "/";
        }
        // remove anchor-links
        if ("".equals(url) || "/".equals(url) || url.startsWith("#") || url.startsWith("/#") ||
            url.startsWith("javascript") || url.startsWith("mailto"))
        {
            return null;
        }
        // change relative links to absolute links
        else if (url.startsWith("/"))
        {

            String parent = parentURL.substring(0, parentURL.lastIndexOf("/") + 1);
            // no sub-directories found
            if ("http://".equals(parent) || "https://".equals(parent))
            {
                parent = parentURL;
            }
            if (parent.endsWith("/"))
            {
                LOG.debug("url starts with '/': {} | {} | {}", parentURL, parent, url.substring(1));
                return parent + url.substring(1);
            }
            else
            {
                LOG.debug("url starts with '/': {} | {} | {}", parentURL, parent, url);
                return parent + url;
            }
        }
        else if (url.startsWith(".."))
        {
            String tmp = url;
            int numOfDirsUp = 1;
            if (parentURL.endsWith("/") && ("http:/".equals(parentURL.subSequence(0, parentURL.lastIndexOf("/"))) ||
                                            "https:/".equals(parentURL.subSequence(0, parentURL.lastIndexOf("/")))))
            {
                numOfDirsUp = 0;
            }
            while (tmp.startsWith(".."))
            {
                tmp = tmp.substring(3);
                numOfDirsUp++;
            }
            String parent = parentURL;
            for (int i = 0; i < numOfDirsUp; i++)
            {
                parent = parent.substring(0, parent.lastIndexOf("/"));
            }

            if (parent.equals("http:/") || parent.equals("https:/") || parent.equals("http:") ||
                parent.equals("https:"))
            // && numOfDirsUp == 1)
            {
                parent = parentURL;
            }

            String ret;
            if (!parent.endsWith("/") && !tmp.startsWith("/"))
            {
                ret = parent + "/" + tmp;
            }
            else if (parent.endsWith("/") && tmp.startsWith("/"))
            {
                ret = parent.substring(0, parent.length() - 1) + tmp;
            }
            else
            {
                ret = parent + tmp;
            }

            LOG.debug("url starts with '..': {} | {} | {} | {} | {}", parentURL, parent, url, tmp, ret);

            return ret;
        }
        else if (!url.startsWith("http://") && !url.startsWith("https://"))
        {
            String parent = parentURL.substring(0, parentURL.lastIndexOf("/") + 1);
            // no sub-directories found
            if ("http://".equals(parent) || "https://".equals(parent))
            {
                parent = parentURL;
            }

            if (parent.endsWith("/") && url.startsWith("/"))
            {
                LOG.debug("url starts not with 'http://': {} | {} | {}", parentURL, parent, url);
                return parent + url.substring(1);
            }
            else if (!parentURL.endsWith("/") && !url.startsWith("/"))
            {
                LOG.debug("url starts not with 'http://': {} | {} | {}", parentURL, parent, url);
                return parent + "/" + url;
            }
            else
            {
                LOG.debug("url starts not with 'http://': {} | {} | {}", parentURL, parent, url);
                return parent + url;
            }
        }
        // url is in absolute format
        else
        // if (url.startsWith("http://") || url.startsWith("https://"))
        {
            LOG.debug("found url: {}", url);
            return url;
        }
    }
}
