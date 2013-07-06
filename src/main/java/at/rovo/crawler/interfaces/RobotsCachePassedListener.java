package at.rovo.crawler.interfaces;

public interface RobotsCachePassedListener 
{
	public void handleURLsPassed(String url);
	public void handleUnableToCheck(String url);
}
