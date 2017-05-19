package at.rovo.crawler.bean;

import at.rovo.crawler.util.IRLbotUtils;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

/**
 * Contains the data of a downloaded RobotsTxt file
 */
public class RobotsTxt
{
    /**
     * A map holding the found user agents within the robots.txt file as key and a {@link RobotsTxtRecord} entry as
     * value which will contain the settings for the respective user agent
     */
    private Map<String, RobotsTxtRecord> records = new LinkedHashMap<>();

    /**
     * Initializes a new instance which parses the provided <em>robotsTxt</em> representation which can be retrieved
     * after finalization via {@link #getRecords()}.
     *
     * @param robotsTxt The robots.txt String representation to parse
     */
    public RobotsTxt(String robotsTxt)
    {
        parseRobotsTxt(robotsTxt);
    }

    /**
     * This class holds the parsed record information for a specific user agent.
     */
    public class RobotsTxtRecord
    {
        /** The default crawl delay does not specify any wait time and thus allows for immediate re-crawls **/
        final static int DEFAULT_CRAWL_DELAY = 0;

        /** Specifies the targeted user-agent for this section **/
        private final String userAgent;
        /** Contains the allowed path-segments for the user-agent of this section **/
        private final Set<String> allowed = new LinkedHashSet<>();
        /** Contains the permitted path-segments for ths specified user-agent **/
        private final Set<String> disallowed = new LinkedHashSet<>();
        /** Specifies a delay between crawls of two pages from the same domain **/
        private int crawlDelay = DEFAULT_CRAWL_DELAY;
        /** Optional sitemap url **/
        private Set<String> sitemap = new LinkedHashSet<>();


        private RobotsTxtRecord(String userAgent)
        {
            this.userAgent = userAgent;
        }

        public String getUserAgent()
        {
            return this.userAgent;
        }

        public Set<String> getAllowedPathSegments()
        {
            return allowed;
        }

        public void addAllowedPathSegment(String allowed)
        {
            this.allowed.add(allowed);
        }

        public Set<String> getDisallowedPathSegments()
        {
            return this.disallowed;
        }

        public void addDisallowedPathSegment(String disallowed)
        {
            this.disallowed.add(disallowed);
        }

        public void setCrawlDelay(int delay)
        {
            this.crawlDelay = delay;
        }

        public int getCrawlDelay()
        {
            return this.crawlDelay;
        }

        public void addSitemap(String sitemapUrl)
        {
            this.sitemap.add(sitemapUrl);
        }

        public Set<String> getSitemap()
        {
            return this.sitemap;
        }
    }

    /**
     * Returns all parsed robots.txt records within a map with the user-agent as key and the respective record as
     * value.
     *
     * @return A map holding each parsed robots.txt record for a parsed user-agent
     */
    public Map<String, RobotsTxtRecord> getRecords()
    {
        return records;
    }

    /**
     * Parses a robots.txt {@link String} representation and fills the provided arguments with rules collected from the
     * robots.txt file.
     *
     * @param robotsTxt
     *         The {@link String} representation of the robots.txt file
     */
    private void parseRobotsTxt(String robotsTxt)
    {
        try (Scanner scanner = new Scanner(robotsTxt))
        {
            while (scanner.hasNextLine())
            {
                String line = scanner.nextLine();

                if (line.trim().startsWith("#"))
                {
                    continue;
                }

                // look for a 'User-agent: ' part
                if (line.toLowerCase().startsWith("user-agent: "))
                {
                    String userAgent = line.substring("user-agent: ".length());

                    RobotsTxtRecord record = new RobotsTxtRecord(userAgent);
                    records.put(userAgent, record);

                    while (scanner.hasNextLine())
                    {
                        line = scanner.nextLine();
                        String lowerLine = line.toLowerCase();
                        // as comments are possible at the end of the line, filter them out
                        if (line.contains("#"))
                        {
                            line = line.substring(0, line.indexOf("#")).trim();
                        }

                        if (lowerLine.startsWith("allow: "))
                        {
                            String allow = line.substring("allow:".length()).trim();
                            record.addAllowedPathSegment(allow);
                        }
                        else if (lowerLine.startsWith("disallow: "))
                        {
                            String disallow = line.substring("disallow:".length()).trim();
                            if ("".equals(disallow))
                            {
                                record.getDisallowedPathSegments().clear();
                                continue;
                            }
                            record.addDisallowedPathSegment(disallow);
                        }
                        else if (lowerLine.startsWith("crawl-delay: "))
                        {
                            String crawlDelay = line.substring("crawl-delay:".length()).trim();
                            try
                            {
                                record.setCrawlDelay(Integer.parseInt(crawlDelay));
                            }
                            catch (NumberFormatException nfEx)
                            {
                                // as a crawl delay was specified but we could not read it, default to 1 second
                                record.setCrawlDelay(1);
                            }
                        }
                        else if (lowerLine.startsWith("sitemap"))
                        {
                            String sitemap = line.substring("sitemap: ".length()).trim();
                            record.addSitemap(sitemap);
                        }
                        else if ("".equals(line.trim()) || "\n".equals(line.trim()))
                        {
                            break;
                        }
                    }
                }
            }
        }
    }

    private void copyDisallowedSites(String userAgent, Set<String> target)
    {
        RobotsTxtRecord record = records.get(userAgent);
        if (record != null)
        {
            target.addAll(record.getDisallowedPathSegments());
        }
    }

    private void copyAllowedSites(String userAgent, Set<String> target)
    {
        RobotsTxtRecord record = records.get(userAgent);
        if (record != null)
        {
            target.addAll(record.getAllowedPathSegments());
        }
    }

    /**
     * Checks a URL against rules extracted from a robots.txt file
     *
     * @param userAgent
     *         The name of the user-agent to check if it passes the check
     * @param url
     *         The URL to test against the rules
     *
     * @return Returns true if a URL passed the tests against the rules, false otherwise
     */
    public boolean checkRobotRules(String userAgent, String url)
    {
        // evaluate settings and compare them with the URL
        Set<String> disallowedSites = new LinkedHashSet<>();
        copyDisallowedSites(userAgent, disallowedSites);
        copyDisallowedSites("*", disallowedSites);

        Set<String> allowedSites = new LinkedHashSet<>();
        copyAllowedSites(userAgent, allowedSites);
        copyAllowedSites("*", allowedSites);

        url = IRLbotUtils.getDirectoryPathOfUrl(url);

        // default behavior auto-allow
        // if we find a disallow rule we prevent further crawling
        boolean isDisallowed = isDisallowed(url, disallowedSites);
        // though, check if a further allow rule overrides the disallow rule
        if (isDisallowed)
        {
            isDisallowed = !isAllowed(url, allowedSites);
        }

        return !isDisallowed;
    }

    private boolean isDisallowed(String url, Set<String> disallowedSites)
    {
        for (String disallow : disallowedSites)
        {
            // '' allows the whole host to be crawled
            if (disallow.trim().equals(""))
            {
                return true;
            }
            // '/' prevents the whole host from being crawled
            else if (disallow.trim().equals("/"))
            {
                return true;
            }
            // endline-anchor indicates that the part before the "$" sign needs to be
            // at the end of the URL
            else if (disallow.endsWith("$"))
            {
                String pathRestriction = disallow.substring(0, disallow.length() - 1);
                // rule is valid for any URL
                if (pathRestriction.startsWith("/*"))
                {
                    pathRestriction = pathRestriction.substring(2);
                    if (url.endsWith(pathRestriction))
                    {
                        return true;
                    }
                }
                // there are some restrictions for the rule
                else if (pathRestriction.startsWith("/"))
                {
                    // there is a wildcard in the middle of the URL, so we have to
                    // check the first and the end part of the URL
                    if (pathRestriction.contains("*"))
                    {
                        String startPart = pathRestriction.substring(0, pathRestriction.indexOf("*"));
                        String endPart = pathRestriction.substring(pathRestriction.indexOf("*") + 1);
                        if (url.startsWith(startPart) && url.endsWith(endPart))
                        {
                            return true;
                        }
                    }
                    // a path without asterix was found - we can use the whole thing
                    // from start on
                    else
                    {
                        if (url.startsWith(pathRestriction))
                        {
                            return true;
                        }
                    }
                }
            }
            // ignore URLs that contain a '?'
            else if (disallow.endsWith("?"))
            {
                String pathRestriction = disallow.substring(0, disallow.length() - 1);
                if (pathRestriction.startsWith("/*"))
                {
                    String part = pathRestriction.substring(2);
                    if (part.trim().length() > 0)
                    {
                        // should match a rule like /*mnp_utility.mspx?
                        if (url.contains(part) && url.contains("?"))
                        {
                            return true;
                        }
                    }
                }
                else if (pathRestriction.startsWith("/"))
                {
                    // there is a wildcard in the middle of the URL, so we have to
                    // check the first and the end part of the URL
                    // should match f.e. a rule like /hpc/*/supported-applications.aspx?
                    if (pathRestriction.contains("*"))
                    {
                        String startPart = pathRestriction.substring(0, pathRestriction.indexOf("*"));
                        String endPart = pathRestriction.substring(pathRestriction.indexOf("*") + 1);
                        if (url.startsWith(startPart) && url.contains(endPart) && url.contains("?"))
                        {
                            return true;
                        }
                    }
                }
            }
            // 'ordinary' path provided
            else
            {
                if (disallow.contains("*"))
                {
                    String firstPart = disallow.substring(0, disallow.indexOf("*"));
                    String secondPart = disallow.substring(disallow.indexOf("*") + 1);
                    if (url.startsWith(firstPart) && url.contains(secondPart))
                    {
                        return true;
                    }
                }
                else
                {
                    if (url.startsWith(disallow))
                    {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isAllowed(String url, Set<String> allowedSites)
    {
        for (String allow : allowedSites)
        {
            if (allow.endsWith("$"))
            {
                String path = allow.substring(0, allow.length() - 1);
                // fetches rules like '/*.html$'
                if (path.startsWith("/*"))
                {
                    path = path.substring(2);
                    if (path.length() > 0)
                    {
                        if (url.endsWith(path))
                        {
                            return true;
                        }
                    }
                }
                // fetches rules like '/articles/*/index.html$
                else if (path.startsWith("/") && path.contains("*"))
                {
                    String startPart = path.substring(0, path.indexOf("*"));
                    String endPart = path.substring(path.indexOf("*") + 1);
                    if (url.startsWith(startPart) && url.contains(endPart))
                    {
                        return true;
                    }
                }
            }
            else if (allow.endsWith("?"))
            {
                String path = allow.substring(0, allow.length() - 1);
                // fetches rules like '/*search.php?'
                if (path.startsWith("/*"))
                {
                    path = path.substring(2);
                    if (path.length() > 0)
                    {
                        if (url.contains(path) && url.contains("?"))
                        {
                            return true;
                        }
                    }
                }
                // fetches rules like '/public/*/search.php?
                else if (path.startsWith("/"))
                {
                    String startPart = path.substring(0, path.indexOf("*"));
                    String endPart = path.substring(path.indexOf("*") + 1);
                    if (url.startsWith(startPart) && url.contains(endPart) && url.contains("?"))
                    {
                        return true;
                    }
                }
            }
            // fetches rules like '/public/*/index.html'
            else if (allow.startsWith("/") && allow.contains("*"))
            {
                String startPart = allow.substring(0, allow.indexOf("*"));
                String endPart = allow.substring(allow.indexOf("*") + 1);
                if (url.startsWith(startPart) && url.contains(endPart))
                {
                    return true;
                }
            }
            // fetches rules like '/default'
            else if (url.startsWith(allow))
            {
                return true;
            }
        }
        return false;
    }
}
