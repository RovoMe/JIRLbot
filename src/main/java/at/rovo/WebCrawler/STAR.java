package at.rovo.WebCrawler;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import at.rovo.caching.drum.Drum;
import at.rovo.caching.drum.DrumException;
import at.rovo.caching.drum.IDrum;
import at.rovo.caching.drum.IDrumListener;
import at.rovo.caching.drum.NullDispatcher;
import at.rovo.caching.drum.data.StringSerializer;
import at.rovo.caching.drum.util.DrumUtil;

/**
 * <p>
 * Spam Tracking and Avoidance through Reputation (STAR) uses DRUM to calculate
 * a budget for pay level domains (PLDs). It therefore builds a PLD-PLD graph
 * which keeps track of the in-degree value, which defines the number of links
 * from one PLD to an other PLD. If a PLD has multiple links to an other PLD
 * only one link is counted.
 * </p>
 * <p>
 * The PLD-PLD graph is used to calculate the top N PLDs according their
 * in-degree value, where N can be specified via {@link #setTopN(int)}. This set
 * is further used to calculate a budget for a PLD based on the in-degree number
 * of the PLD within the top N set.
 * </p>
 * <p>
 * The budget itself lies between a minimum and maximum value which may be
 * specified via {@link #setMinBudget(int)} and {@link #setMaxBudget(int)} and
 * is calculated as presented in the paper 'IRLbot: Scaling to 6 Billion Pages
 * and Beyond' on interpolating between minimum and maximum budget.
 * </p>
 * 
 * @author Roman Vottner
 */
public class STAR extends NullDispatcher<PLDData, StringSerializer>
{
	// create a logger
	private final static Logger logger = LogManager.getLogger(STAR.class
			.getName());

	/** The DRUM object managing the update and unique/duplicate checking **/
	private IDrum<PLDData, StringSerializer> drum = null;
	/** The registered listeners **/
	private List<CheckSpamUrlListener> listeners = null;
	/** The number of buckets used by the STAR structure **/
	private int numBuckets = 0;
	/** The comparator used to sort PLDs based on their in-degree value **/
	private PLDComparator<PLDData> comparator = new PLDComparator<>();
	/** Contains the top N PLDs according their in-degree value **/
	private ConcurrentSkipListSet<PLDData> topSet = new ConcurrentSkipListSet<>(
			this.comparator);
	/**
	 * Used to compare the topSet with the value of the last iteration to
	 * determine if the order has changed and to update the elements that have
	 * changed
	 **/
	private ConcurrentSkipListSet<PLDData> compareSet = new ConcurrentSkipListSet<>(
			this.comparator);
	/** The maximum budget assigned to a PLD **/
	private int maxBudget = 10000;
	/** The minimum budget assigned to a PLD **/
	private int minBudget = 10;
	/** The number of items kept in the pldCache and in the topSet **/
	private int topN = 10000;

	/**
	 * <p>
	 * Initializes a new instance of STAR structure with basic values. This
	 * constructor sets the number of buckets to 1024 and the size of the bucket
	 * before the data is written to disk file or merged with the backing data
	 * store to 64k bytes.
	 * </p>
	 * 
	 * @throws DrumException
	 *             If any exception during the initialization of the backing
	 *             DRUM cache occurs
	 */
	public STAR() throws DrumException
	{
		this.numBuckets = 1024;
		this.drum = new Drum<>("pldIndegree", 1024, 65536, this, PLDData.class,
				StringSerializer.class);
		this.listeners = new CopyOnWriteArrayList<>();
	}

	/**
	 * <p>
	 * Initializes a new instance of STAR structure with required values.
	 * </p>
	 * 
	 * @param numBuckets
	 *            The number of in-memory buffers and disk-buckets used by DRUM
	 *            for storing and managing the data
	 * @param bucketByteSize
	 *            The size of a in-memory buffer and disk-bucket in bytes before
	 *            the data gets written or checked
	 * @param listener
	 *            An object to forward internal DRUM events to
	 * @throws DrumException
	 *             If any exception during the initialization of the backing
	 *             DRUM cache occurs
	 */
	public STAR(int numBuckets, int bucketByteSize, IDrumListener listener)
			throws DrumException
	{
		this.numBuckets = numBuckets;
		this.drum = new Drum<>("pldIndegree", numBuckets, bucketByteSize, this,
				PLDData.class, StringSerializer.class, listener);
		this.listeners = new CopyOnWriteArrayList<>();
	}

	/**
	 * <p>
	 * Adds a {@link CheckSpamUrlListener} implementing object to the list of
	 * objects to be notified if a PLD has passed the budget check.
	 * </p>
	 * 
	 * @param listener
	 *            The object to be notified if a PLD passes the budget check
	 */
	public void addCheckSpamUrlListener(CheckSpamUrlListener listener)
	{
		if (!this.listeners.contains(listener))
			this.listeners.add(listener);
	}

	/**
	 * <p>
	 * Removes an item from being notified on PLDs passing the budget check.
	 * </p>
	 * 
	 * @param listener
	 *            The object to unregister from notification on PLD budget
	 *            passes
	 */
	public void removeCheckSpamUrlListener(CheckSpamUrlListener listener)
	{
		if (this.listeners.contains(listener))
			this.listeners.remove(listener);
	}

	/**
	 * <p>
	 * Sets the maximum budget assigned to a PLD if it has the highest number of
	 * incoming PLD links.
	 * </p>
	 * 
	 * @param maxBudget
	 *            The maximum assigned budget to a PLD
	 */
	public void setMaxBudget(int maxBudget)
	{
		this.maxBudget = maxBudget;
	}

	/**
	 * <p>
	 * Returns the specified maximum budget to be set if a PLD has the highest
	 * number of incoming PLD links.
	 * </p>
	 * <p>
	 * By default <code>getMaxBudget()</code> will return 10000.
	 * </p>
	 * 
	 * @return The currently set maximum budget
	 */
	public int getMaxBudget()
	{
		return this.maxBudget;
	}

	/**
	 * <p>
	 * Sets the minimum budget assigned to any PLD that has not enough incoming
	 * PLD links to be within the topN set of PLDs.
	 * </p>
	 * 
	 * @param minBudget
	 *            The minimum budget for any PLD if it is not within the topN
	 *            PLDs
	 * @see #setTopN(int)
	 * @see #getTopN()
	 */
	public void setMinBudget(int minBudget)
	{
		this.minBudget = minBudget;
	}

	/**
	 * <p>
	 * Returns the specified minimum budget for a PLD not within the topN PLDs
	 * according their in-degree number.
	 * </p>
	 * <p>
	 * By default <code>getMinBudget()</code> will return 10.
	 * </p>
	 * 
	 * @return The minimum budget for any PLD not within the topN PLDs
	 * @see #setTopN(int)
	 * @see #getTopN()
	 */
	public int getMinBudget()
	{
		return this.minBudget;
	}

	/**
	 * <p>
	 * Sets the number of PLD items kept in memory. The PLDs are stored within a
	 * {@link Set} sorted by their in-degree value with the head entry being the
	 * one with the highest in-degree value while the number decreases towards
	 * the tail of the list.
	 * </p>
	 * 
	 * @param topN
	 *            The number of entries kept in the pldCache and in the topN set
	 */
	public void setTopN(int topN)
	{
		this.topN = topN;
	}

	/**
	 * <p>
	 * Returns the specified number of entries kept in memory sorted according
	 * their in-degree value of other PLDs.
	 * </p>
	 * <p>
	 * By default <code>getTopN()</code> will return 10000.
	 * </p>
	 * 
	 * @return The specified number of entries kept in memory
	 */
	public int getTopN()
	{
		return this.topN;
	}

	/**
	 * <p>
	 * Returns the number of buckets used by the backing DRUM structure.
	 * </p>
	 * 
	 * @return The number of buckets used by the backing DRUM structure
	 */
	public int getNumberOfBuckets()
	{
		return this.numBuckets;
	}

	/**
	 * <p>
	 * Returns a copy of the current top N PLD entries according to their
	 * in-degree value.
	 * </p>
	 * 
	 * @return The top N PLD entries
	 */
	public Set<PLDData> getTopNSet()
	{
		return new ConcurrentSkipListSet<>(this.topSet);
	}

	/**
	 * <p>
	 * Checks if a certain URL passes the budget check.
	 * </p>
	 * 
	 * @param aux
	 */
	public void check(String aux)
	{
		long key = DrumUtil.hash(aux);
		PLDData data = new PLDData();
		data.setHash(key);
		// check if the URL is present in the topN set
		if (this.topSet.contains(data))
		{
			data = this.topSet.ceiling(data);
			for (CheckSpamUrlListener listener : this.listeners)
				listener.handleSpamCheck(aux, data.getBudget());
		}
		else
			this.drum.check(key, new StringSerializer(aux));
	}

	/**
	 * <p>
	 * Updates the PLD-PLD link graph letting the <code>origin</code> PLD point
	 * to every PLD contained in <code>plds</code>.
	 * </p>
	 * <p>
	 * This method append-updates neighbor information using DRUM and builds the
	 * topN set of PLDs which have the highest number of in-degree links.
	 * Furthermore this method keeps track of PLDs dropping from the topN set
	 * which get check+updated with the new minimal budget.
	 * </p>
	 * 
	 * @param origin
	 *            The origin PLD
	 * @param plds
	 *            The PLDs contained in a page of PLD
	 */
	public void update(String origin, Set<String> plds)
	{
		// crawling threads aggregate PLD-PLD link information and send it to a
		// DRUM structure
		// PLDindegree, which uses a batch update to store for each PLD x its
		// hash hx, in-degree
		// dx, current budget Bx, and hashes of all in-degree neighbors in the
		// PLD graph.

		for (String pld : plds)
		{
			if (logger.isDebugEnabled())
				logger.debug("Updating PLD link information: " + pld + "<--"
						+ origin + " (" + DrumUtil.hash(pld) + "<--"
						+ DrumUtil.hash(origin) + ")");
			PLDData data = new PLDData();
			data.setPLD(pld);
			data.setHash(DrumUtil.hash(pld));
			Set<Long> neighbor = new ConcurrentSkipListSet<>();
			neighbor.add(DrumUtil.hash(origin));
			data.setIndegreeNeighbors(neighbor);
			data.setBudget(10);
			this.drum.appendUpdate(DrumUtil.hash(pld), data,
					new StringSerializer(pld));
		}
	}

	/**
	 * <p>
	 * Compares the current version of the topSet with the version of the last
	 * iteration and updates entries if either the hash of the old entry differs
	 * from the current entry or if the budget of the new entry changed and
	 * needs an update.
	 * </p>
	 * <p>
	 * This method will check+update PLD entries with DRUM if the value has
	 * changed since the last iteration.
	 * </p>
	 */
	private void checkOrderHasChanged()
	{
		Iterator<PLDData> topIter = this.topSet.iterator();
		Iterator<PLDData> compIter = this.compareSet.iterator();

		// run through the top- and its comparison-set and filter items that
		// changed between the last and the current iteration and calculate the
		// new budget for those PLDs that changed and update their budget in
		// DRUM too.
		int curBudget = 10000;
		while (topIter.hasNext() && compIter.hasNext())
		{
			PLDData top = topIter.next();
			PLDData comp = compIter.next();

			if (top.getHash() != comp.getHash() || top.getBudget() > curBudget)
			{
				top.setBudget(this.calculateBudget(top.getIndegree()));
				curBudget = top.getBudget();
				this.drum.checkUpdate(top.getHash(), top);
			}
		}
		// check if the topSet has expanded in comparison to the last call
		while (topIter.hasNext())
		{
			PLDData top = topIter.next();
			top.setBudget(this.calculateBudget(top.getIndegree()));
			this.drum.checkUpdate(top.getHash(), top);
		}
		// should not happen as the list should not get less than 25 - just in
		// case is might happen for some reason set the budget of those PLDs
		// not in the current top list to 10
		while (compIter.hasNext())
		{
			PLDData comp = compIter.next();
			comp.setBudget(10);
			this.drum.checkUpdate(comp.getHash(), comp);
		}

		if (logger.isDebugEnabled())
		{
			logger.debug("New Budgets assigned: ");
			this.printTop(this.topN);
		}
	}

	/**
	 * <p>
	 * Callback method for the {@link DrumDispatcher}. This method should not be
	 * called by the user.
	 * </p>
	 */
	@Override
	public void update(Long key, PLDData data, StringSerializer aux)
	{
		data.setPLD(aux.getData());

		// as adding an already stored element in the top set leaves the set
		// unchanged and ConcurrentSkipListSet lacks a method to replace an
		// entry
		// we need to retrieve the element first and append the data to the
		// entry
		if (this.topSet.contains(data))
			this.topSet.ceiling(data).append(data);
		else
			this.topSet.add(data);

		// Only the top N PLD entries are kept in memory as they get assigned a
		// budget between max- and minBudget uniformly. If a PLD drops from the
		// topSet it automatically gets assigned a budget of minBudget
		if (this.topSet.size() > this.topN)
		{
			for (int i = this.topSet.size(); i > this.topN; i--)
			{
				PLDData remData = this.topSet.last();
				this.topSet.remove(remData);

				if (remData.getBudget() > this.minBudget)
				{
					remData.setBudget(this.minBudget);
					this.drum.checkUpdate(remData.getHash(), remData);
				}
			}
		}

		// check if the order of the topSet has changed
		this.checkOrderHasChanged();

		// create a copy for the next iteration
		this.compareSet = new ConcurrentSkipListSet<>(this.topSet);
	}

	/**
	 * <p>
	 * Callback method for the {@link DrumDispatcher}. This method should not be
	 * called by the user.
	 * </p>
	 */
	@Override
	public void duplicateKeyCheck(Long key, PLDData value, StringSerializer aux)
	{
		for (CheckSpamUrlListener listener : this.listeners)
			listener.handleSpamCheck(aux.getData(), value.getBudget());

		this.printTop(this.topN);
	}

	/**
	 * <p>
	 * Calculates the budget based on the in-degree number of a PLD. It
	 * therefore iterates through the topN set and returns a budget calculated
	 * on the position of the entry in the topN set. If the last entry in the
	 * topN set has a higher in-degree value than the provided argument the
	 * minimum budget will be returned.
	 * 
	 * @param indegree
	 *            The in-degree number of a PLD
	 * @return The calculated budget which is a value between the maximum and
	 *         the minimum budget
	 */
	private int calculateBudget(int indegree)
	{
		// each PLD x starts with a default budget B0, which is dynamically
		// adjusted
		// using some function F(dx) as x's in-degree dx changes
		// Budget Bx represents the number of pages that are allowed to pass
		// from x
		// (including all hosts and subdomains in x) to crawling threads every T
		// time units.

		// as in the paper the top 10k entries are linearly assigned
		// a value between 10k and 10 - PLDs below 10k are assigned 10
		int budget = this.maxBudget;
		int i = 0;
		Iterator<PLDData> iter = this.topSet.iterator();
		while (iter.hasNext() && budget >= this.minBudget)
		{
			budget = this.maxBudget - i++;
			PLDData data = iter.next();
			if (data.getIndegree() <= indegree)
				return budget;
		}
		return this.minBudget;
	}

	/**
	 * <p>
	 * Forces a synchronization which results in all the data currently hold in
	 * memory buffers and disk buckets to be synchronized with the backing data
	 * store.
	 * </p>
	 * 
	 * @throws DrumException
	 *             If any exception during the synchronization of the backing
	 *             DRUM cache occurs
	 */
	public void synchronize() throws DrumException
	{
		this.drum.synchronize();
	}

	/**
	 * <p>
	 * Disposes the backing DRUM structure and frees resources hold by DRUM.
	 * </p>
	 * 
	 * @throws DrumException
	 *             If any exception during disposing of the backing DRUM
	 *             framework occurs
	 */
	public void dispose() throws DrumException
	{
		if (logger.isDebugEnabled())
			this.printTop(100);

		this.drum.dispose();
	}

	/**
	 * <p>
	 * Debug method to print the topN entries according their in-degree value.
	 * </p>
	 * <p>
	 * If the number is larger than elements within the topN set, the result
	 * will only contain the output of the topN entries.
	 * </p>
	 * 
	 * @param numEntries
	 *            The number of Entries to print
	 */
	private void printTop(int numEntries)
	{
		Iterator<PLDData> iter = this.topSet.iterator();
		int i = 0;
		if (logger.isDebugEnabled())
			logger.debug("Top " + numEntries + " list");
		while (iter.hasNext() && i < numEntries)
		{
			PLDData data = iter.next();
			if (logger.isDebugEnabled())
				logger.debug((i + 1) + " - PLD: " + data.getPLD() + " hash: "
						+ data.getHash() + " in-degree: " + data.getIndegree()
						+ " budget: " + data.getBudget());
			i++;
		}
	}
}
