package at.rovo.crawler;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;

import at.rovo.caching.drum.Drum;
import at.rovo.caching.drum.DrumBuilder;
import at.rovo.caching.drum.DrumListener;
import at.rovo.crawler.util.IRLbotUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import at.rovo.caching.drum.DrumException;
import at.rovo.caching.drum.NullDispatcher;
import at.rovo.caching.drum.data.StringSerializer;
import at.rovo.caching.drum.util.DrumUtil;
import at.rovo.crawler.bean.PLDData;
import at.rovo.crawler.interfaces.CheckSpamUrlListener;
import at.rovo.crawler.util.PLDComparator;

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
@SuppressWarnings("unused")
public final class STAR extends NullDispatcher<PLDData, StringSerializer>
{
	/** The logger of this class **/
	private final static Logger LOG = LogManager.getLogger(STAR.class);

	/** The DRUM object managing the update and unique/duplicate checking **/
	private Drum<PLDData, StringSerializer> drum = null;
	/** The registered listeners **/
	private List<CheckSpamUrlListener> listeners = null;
	/** The number of buckets used by the STAR structure **/
	private int numBuckets = 0;
	/** The comparator used to sort PLDs based on their in-degree value **/
	private PLDComparator<PLDData> comparator = new PLDComparator<>();
	/** Contains the top N PLDs according their in-degree value in a sorted order
	 * depending on the ordering returned by the {@link PLDComparator} **/
	private ConcurrentSkipListSet<PLDData> topSet = new ConcurrentSkipListSet<>(
			this.comparator);
	/**
	 * Used to compare the topSet with the value of the last iteration to
	 * determine if the order has changed and to update the elements that have
	 * changed
	 **/
	private ConcurrentSkipListSet<PLDData> compareSet =
			new ConcurrentSkipListSet<>(this.comparator);
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
		try
		{
		this.drum = new DrumBuilder<>("pldIndegree", PLDData.class, StringSerializer.class)
				.numBucket(1024)
				.bufferSize(65536)
				.dispatcher(this)
				.build();
		}
		catch (Exception e)
		{
		throw new DrumException(e.getLocalizedMessage(), e);
		}
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
	public STAR(int numBuckets, int bucketByteSize, DrumListener listener)
			throws DrumException
	{
		this.numBuckets = numBuckets;
		try
		{
			this.drum = new DrumBuilder<>("pldIndegree", PLDData.class, StringSerializer.class)
					.numBucket(numBuckets)
					.bufferSize(bucketByteSize)
					.dispatcher(this)
					.listener(listener)
					.build();
		}
		catch (Exception e)
		{
			throw new DrumException(e.getLocalizedMessage(), e);
		}
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
	 * <p>
	 * This method will check if an entry of the URLs pay level domain exists
	 * already in the local in memory cache and redirects the request to the
	 * backing DRUM cache if not. In case the entry was found within DRUM
	 * {@link #duplicateKeyCheck(Long, PLDData, StringSerializer)} will be
	 * invoked by the backing DRUM cache.
	 * </p>
	 * 
	 * @param url
	 *            The auxiliary data object representing the URL to check
	 *
	 * @see {@link #duplicateKeyCheck(Long, PLDData, StringSerializer)}
	 */
	public void check(String url)
	{
		String pld = IRLbotUtil.getPLDofURL(url);
		long key = DrumUtil.hash(pld);
		PLDData data = new PLDData();
		data.setHash(key);
		data.setPLD(pld);
		// check if the URL is present in the topN set. If it is not, we first
		// redirect the request to DRUM. It will then respond with either a
		// unique or duplicate check key response. But as we are only
		// interested in entries contained within DRUM
		PLDData pldData = this.getData(data);
		if (null != pldData)
		{
			// as the data is already locally available we don't need to send
			// a check request to the backing drum instance
			LOG.debug("In memory cached data found for {}/{}/{} - budget is {}",
					key, pld, url, pldData.getBudget());
			for (CheckSpamUrlListener listener : this.listeners)
				listener.handleSpamCheck(url, pldData.getBudget());
		}
		else
		{
			// after invoking check() on the backing DRUM instance, if there
			// was already a PLD entry for this URL the duplicateKeyCheck
			// method will be invoked by the dispatcher
			LOG.debug("Checking {}/{}/{} against backing DRUM cache",
					key, pld, url);
			this.drum.check(key, new StringSerializer(url));
		}
	}

	/**
	 * <p>
	 * Returns the PLDData element from the topSet if it includes an element
	 * with the same hash number. Otherwise null is returned.
	 * </p>
	 * <p>
	 * Note that this method is necessary as topSet.ceiling(data) object returns
	 * the first element instead of the object at the position that equals (=
	 * same hash) the provided one.
	 * </p>
	 *
	 * @param data
	 *        A PLDData object whose correspondent object should be looked up in
	 *        the topSet.
	 * @return The PLDData object in the topSet which shares the same hash key
	 *         as the PLDData object provided
	 */
	private PLDData getData(PLDData data)
	{
		LOG.debug("Size of topSet: {}", topSet.size());
		for (PLDData obj : this.topSet)
		{
			if (obj.getHash() == data.getHash())
				return obj;
		}
		return null;
	}
	
	/**
	 * <p>
	 * Returns <code>true</code> if the topSet contains the specified element.
	 * </p>
	 * 
	 * @param data
	 *        The PLDData object which should be checked for containment in the
	 *        topSet
	 * @return <code>true</code> if this element is contained in the topSet,
	 *         false otherwise
	 */
	private boolean containsData(PLDData data)
	{
		return getData(data) != null;
	}

	/**
	 * <p>
	 * Callback method which is invoked by the backing DRUM instance. This
	 * method should not be called by the user.
	 * </p>
	 * <p>
	 * If the backing <em>DRUM</em> cache already contained PLD data for
	 * the given pay level domain, this method will be invoked.
	 * </p>
	 *
	 * @param key
	 *            The hash value of the pay level domain available within the
	 *            DRUM cache
	 * @param data
	 *            The encapsulated data for the given pay level domain. This
	 *            includes f.e. the current budget as well as all the neighbors
	 *            pointing to this domain
	 * @param url
	 *            The actual URL the check was executed for
	 */
	@Override
	public void duplicateKeyCheck(Long key, PLDData data, StringSerializer url)
	{
		LOG.debug("Backing DRUM already contained data for PLD {}/{} - budget is {}",
				key, url.getData(), data.getBudget());
		for (CheckSpamUrlListener listener : this.listeners)
		{
			listener.handleSpamCheck(url.getData(), data.getBudget());
		}

		if (LOG.isDebugEnabled())
			this.printTop(this.topN);
	}

	@Override
	public void uniqueKeyCheck(Long key, StringSerializer pld)
	{
		LOG.warn("PLD {}/{} not found within backing DRUM!", key, pld.getData());
	}

	/**
	 * <p>
	 * Updates the PLD-PLD link graph letting the <code>origin</code> PLD point
	 * to every PLD contained in <code>plds</code>.
	 * </p>
	 * <p>
	 * This method append-updates neighbor information using DRUM and builds
	 * the topN set of PLDs which have the highest number of in-degree links.
	 * Furthermore this method keeps track of PLDs dropping from the topN set
	 * which get check+updated with the new minimal budget.
	 * </p>
	 * <p>
	 * <b>Note</b> that this method is usually called by the crawling threads
	 * to update the PLD-PLD graph.
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
		// DRUM structure. PLDindegree uses a batch update to store for each
		// PLD x its hash hx, in-degree dx, current budget Bx, and hashes of
		// all in-degree neighbors in the PLD graph. This data is encapsulated
		// within a PLDData object.
		for (String pld : plds)
		{
			LOG.debug("Updating PLD link information: {}<--{} ({}<--{})",
					pld, origin, DrumUtil.hash(pld), DrumUtil.hash(origin));
			PLDData data = new PLDData();
			data.setPLD(pld);
			data.setHash(DrumUtil.hash(pld));
			// Neighbors are kept in a sortable list where neighbors with a
			// lower hash value of their PLD are ranked first.
			Set<Long> neighbor = new ConcurrentSkipListSet<>();
			neighbor.add(DrumUtil.hash(origin));
			data.setIndegreeNeighbors(neighbor);
			// by default every PLD has a budget of 10. If the domain is linked
			// more often the budget will increase
			data.setBudget(this.minBudget);
			this.drum.appendUpdate(DrumUtil.hash(pld), data, new StringSerializer(pld));
		}
	}

	/**
	 * <p>
	 * Callback method which is invoked by the backing DRUM instance if in a
	 * previous step a change of the PLD data was noticed. DRUM will return
	 * therefore the most recent state of the attached data for the given PLD.
	 * This method should not be called by the user directly.
	 * </p>
	 *
	 * @param key
	 *            The hash value of the updated pay level domain
	 * @param data
	 *            The encapsulated data for the given pay level domain. This
	 *            includes f.e. the current budget as well as all the neighbors
	 *            pointing to this domain
	 * @param pld
	 *            The actual pay level domain the update was executed for
	 */
	@Override
	public void update(Long key, PLDData data, StringSerializer pld)
	{
		if (null == pld)
		{
			throw new IllegalArgumentException("PLD was null! It seems no auxiliary PLD data was provided upon calling DRUMs update operation");
		}
		LOG.debug("Receiving update for PLD {}/{}", key, pld.getData());
		data.setPLD(pld.getData());

		// as adding an already stored element in the top set leaves the set
		// unchanged and ConcurrentSkipListSet lacks a method to replace an
		// entry we need to retrieve the element first and append the data to
		// the entry
		PLDData tmp = this.getData(data);
		if (null != tmp)
		{
			// as updating a PLDData object does not change the position of the
			// object in the data structure, which is internally sorted, we 
			// remove the object and add it again
			LOG.debug("PLD before the appending: {}", tmp);
			this.topSet.remove(tmp);
			tmp.append(data);
			LOG.debug("PLD after the merge: {}", tmp);
			this.topSet.add(tmp);
		}
		else
		{
			LOG.debug("Adding {} to the in memory cache", data);
			this.topSet.add(data);
		}

		// Only the top N PLD entries are kept in memory as they get assigned a
		// budget between max- and minBudget uniformly. If a PLD drops from the
		// topSet it automatically gets assigned a budget of minBudget
		if (this.topSet.size() > this.topN)
		{
			for (int i = this.topSet.size(); i > this.topN; i--)
			{
				PLDData remData = this.topSet.last();
				LOG.debug("Removing {} from the top-set - was the {}. element",
						remData, i);
				this.topSet.remove(remData);
				// update the budget of the PLD which drops from the top set if
				// it had a budget from more than the minimum budget. If a PLD
				// is not within the top set, it automatically has minimum
				// budget available
				if (remData.getBudget() > this.minBudget)
				{
					remData.setBudget(this.minBudget);
					this.drum.update(remData.getHash(), remData, new StringSerializer(remData.getPLD()));
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
		int curBudget = this.maxBudget;
		while (topIter.hasNext() && compIter.hasNext())
		{
			PLDData top = topIter.next();
			PLDData comp = compIter.next();
			// compare the n-th item of the top-set with the n-th item of the
			// comparison set. If the items are not identical or if the budget
			// of the PLD in the top-set is greater than the current budget, we
			// need to recalculate the budget
			if (top.getHash() != comp.getHash() || top.getBudget() > curBudget)
			{
				top.setBudget(this.calculateBudget(top.getIndegree()));
				curBudget = top.getBudget();
				LOG.debug("Recalculated Budget for PLD {}/{} with a budget of {} - previous set contained {}/{} at this position - current budget {}",
						top.getHash(), top.getPLD(), top.getBudget(),
						comp.getHash(), comp.getPLD(), curBudget);
				this.drum.update(top.getHash(), top, new StringSerializer(top.getPLD()));
			}
		}
		// check if the topSet has expanded in comparison to the last call
		while (topIter.hasNext())
		{
			PLDData top = topIter.next();
			top.setBudget(this.calculateBudget(top.getIndegree()));
			LOG.debug("Recalculated Budget for PLD {}/{} with a budget of {} - current budget {}",
					top.getHash(), top.getPLD(), top.getBudget(), curBudget);
			this.drum.update(top.getHash(), top, new StringSerializer(top.getPLD()));
		}
		// should not happen as the list should not get less than 25 - just in
		// case is might happen for some reason set the budget of those PLDs
		// not in the current top list to 10
		while (compIter.hasNext())
		{
			PLDData comp = compIter.next();
			comp.setBudget(this.minBudget);
			LOG.warn("Setting Budget for PLD {}/{} to the minimum budget of {}",
					comp.getHash(), comp.getPLD(), this.minBudget);
			this.drum.update(comp.getHash(), comp, new StringSerializer(comp.getPLD()));
		}

		if (LOG.isTraceEnabled())
		{
			LOG.trace("New Budgets assigned: ");
			this.printTop(this.topN);
		}
		else
		{
			LOG.debug("CheckOrderHasChanged processed");
		}
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
		// adjusted using some function F(dx) as x's in-degree dx changes Budget
		// Bx represents the number of pages that are allowed to pass from x
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
			{
				return budget;
			}
		}
		return this.minBudget;
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
		this.checkOrderHasChanged();
		if (LOG.isDebugEnabled())
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
		LOG.debug("Top {} list", numEntries);
		while (iter.hasNext() && i < numEntries)
		{
			PLDData data = iter.next();
			LOG.debug("{} - PLD: {} hash: {} in-degree: {} budget: {}",
					(i + 1), data.getPLD(), data.getHash(), data.getIndegree(), 
					data.getBudget());
			i++;
		}
	}
}
