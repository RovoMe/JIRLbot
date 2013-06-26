package at.rovo.WebCrawler;

import java.util.Set;

public class CrawledPage
{
	private String url = null;
	private Set<String> containedURLs = null;
	
	public CrawledPage(String url, Set<String> containedURLs)
	{
		this.url = url;
		this.containedURLs = containedURLs;
	}
	
	public void setURL(String url)
	{
		this.url = url;
	}
	
	public String getURL()
	{
		return this.url;
	}
	
	public void setContainedURLs(Set<String> containedURLs)
	{
		this.containedURLs = containedURLs;
	}
	
	public Set<String> getContainedURLs()
	{
		return this.containedURLs;
	}
}
