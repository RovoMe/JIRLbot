package at.rovo.WebCrawler;

/**
 * <p>Defines a simple pair class that stores two elements 
 * and provides access to both of them via according getter- 
 * and setter-methods.</p>
 *
 * @param <A> The type of the first element
 * @param <B> The type of the second element
 * @author Roman Vottner
 */
public class Pair<A,B> 
{
	private A a = null;
	private B b = null;
	
	/**
	 * Default Constructor
	 */
	public Pair()
	{
		
	}
	
	/**
	 * <p>Creates a new instance of this class and sets the first 
	 * element as a, and the second element as b</p>
	 * 
	 * @param a
	 * @param b
	 */
	public Pair(A a, B b)
	{
		this.a = a;
		this.b = b;
	}
	
	/**
	 * <p>Returns the first element of this pair</p>
	 * @return The first element of this pair
	 */
	public A getFirst() { return a; }
	
	/**
	 * <p>Returns the last element of this pair</p>
	 * @return The last element of this pair
	 */
	public B getLast() { return b; }
	
	/**
	 * <p>Sets the first element of this pair</p>
	 * @param a The first element of this pair
	 */
	public void setFirst(A a) { this.a = a; }
	
	/**
	 * <p>Sets the last element of this pair</p>
	 * @param b The last element of this pair
	 */
	public void setLast(B b) { this.b = b; }
}
