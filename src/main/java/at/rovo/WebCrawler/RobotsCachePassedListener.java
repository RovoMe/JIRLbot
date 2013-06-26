package at.rovo.WebCrawler;

public interface RobotsCachePassedListener 
{
	public void handleURLsPassed(String url);
	public void handleUnableToCheck(String url);
}
