package at.rovo.crawler.interfaces;

/**
 * <p>
 * The listener interface for receiving notifications on the budget passing of
 * URLs. The class that is interested in processing these notifications hast to
 * implement this interface, and the object created with that class is
 * registered with a component, using the component's
 * <code>addBEASTBudgetPassedListener</code> method. When the notification
 * occurs, that object's {@link #handleBudgetPassed(String)} method is invoked.
 * </p>
 */
public interface BEASTBudgetPassedListener 
{
	/**
	 * <p>
	 * Indicates that the given <em>url</em> has passed the budget check.
	 * </p>
	 *
	 * @param url
	 *            The URL that passed the budget check
	 */
	public void handleBudgetPassed(String url);
}
