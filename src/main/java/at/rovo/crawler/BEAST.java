package at.rovo.crawler;

import at.rovo.common.Pair;
import at.rovo.crawler.interfaces.BEASTBudgetPassedListener;
import at.rovo.crawler.util.IRLbotUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This is an implementation of the <em>Budget Enforcement with Anti-Spam tactics (BEAST)</em> structure presented by
 * Lee, Leonard, Wang and Loguinov in their paper <em>IRLbot: Scaling to 6 Billion Pages and Beyond</em>.
 * <p>
 * BEAST administers a {@link List} of {@link Queue}s which contain the URLs and their actual budget, which was
 * calculated by {@link STAR} beforehand. New URLs, which are provided via {@link #checkBudgetOfURL(String, int)}, get
 * arranged into the first queue which has still room according to the number of URLs of the same pay level domain
 * within the queue and the provided budget-value that indicates the capacity of pay level dependent URLs in a queue.
 * <p>
 * If all queues have exhausted their capacity for a certain URL of a pay level domain, the URL and its budget will get
 * stored in a left-over queue which will be split up into n new queues, depending on the current number of queues,
 * which are added to the end of the list of queues, after all queues in the list of queues have been read. The data
 * contained within the left-over queue is spread across the newly added queues.
 *
 * @author Roman Vottner
 */
@SuppressWarnings("unused")
public final class BEAST
{
    /** The logger of this class **/
    private final static Logger LOG = LogManager.getLogger(BEAST.class);

    /** The {@link List} of expandable queues **/
    private List<Queue<Pair<String, Integer>>> queues = null;
    /** The left-over budgets for each pay level domain per queue **/
    private Map<Queue<Pair<String, Integer>>, Map<String, Integer>> pldBudgets = null;
    /** The queue currently read from. Note that new entries are stored within the other n-1 queues **/
    private Queue<Pair<String, Integer>> currentQueue = null;
    /** This queue will take all URLs that did not fit into any other queue **/
    private Queue<Pair<String, Integer>> leftOverQueue = null;
    /** Number of the current queue in the list of queues **/
    private int currentQueueNumber = 0;
    /** Indicates if the work should be stooped **/
    private volatile boolean stopRequested = false;
    /** Indicates when to start off work **/
    private boolean startOff = false;
    /** Classes that need to be informed of URLs passing the budget check **/
    private List<BEASTBudgetPassedListener> listeners = null;
    /** The object to use for the synchronization lock **/
    private final Object syncObj = new Object();

    /**
     * Instantiates a new BEAST object
     */
    public BEAST()
    {
        this.queues = new ArrayList<>();
        this.currentQueue = new LinkedList<>();
        this.leftOverQueue = new LinkedList<>();
        this.pldBudgets = new HashMap<>();

        // Adding starting queues to the list of queues
        this.queues.add(this.currentQueue);
        Queue<Pair<String, Integer>> queue = new LinkedList<>();
        this.queues.add(queue);
        // Adding queues and the mapping of PLD with their left-over budget for each queue to the budget map
        this.pldBudgets.put(this.currentQueue, new HashMap<>());
        this.pldBudgets.put(queue, new HashMap<>());

        this.listeners = new CopyOnWriteArrayList<>();

        Thread worker = new Thread(new BEASTQueueReader());
        worker.setName("BEAST Queue Reader");
        worker.start();
    }

    /**
     * Adds a new listener to BEAST, which implementing class needs to be informed if a URL passed the budget test. If
     * the listener is already registered with BEAST, the invocation will be ignored.
     *
     * @param listener
     *         The listener which need to be informed of passing URLs
     */
    public void addBEASTBudgetPassedListener(BEASTBudgetPassedListener listener)
    {
        if (!this.listeners.contains(listener))
        {
            this.listeners.add(listener);
        }
    }

    /**
     * Removes a currently registered instance that implements the {@link BEASTBudgetPassedListener}. If the listener
     * was not registered before the invocation will be ignored.
     *
     * @param listener
     *         The listener-instance which needs to be removed of the set of registered listeners.
     */
    public void removeBEASTBudgetPassedListener(BEASTBudgetPassedListener listener)
    {
        if (this.listeners.contains(listener))
        {
            this.listeners.remove(listener);
        }
    }

    /**
     * Arranges URLs into specific queues based on the provided budget and the number of URLs from the same pay level
     * domain within the queues.
     * <p>
     * This implementation will take the first available queue whose budget for the provided pay level domain is not
     * depleted.
     * <p>
     * If all of the queues are already depleted with URLs of the same pay level domain, the URL itself is added to a
     * further queue, the fail-over queue, at the end of the list.
     *
     * @param url
     *         The URL to check its budget for
     * @param budget
     *         The current budget for this URL
     */
    public void checkBudgetOfURL(String url, int budget)
    {
        String PLD = IRLbotUtils.getPLDofURL(url);
        // for a given domain x with budget Bx, the first Bx URLs are sent into Q2, the next into Q3 and so on
        // this means if there are 4 queues and the budget of a URL is f.e. 10 - every queue has a limit of 10 URLs
        boolean found = false;

        synchronized (this.syncObj)
        {
            for (int i = 0; i < this.queues.size(); i++)
            {
                // this will add the recently emptied queue to the back of the list while adding values
                int j = i + this.currentQueueNumber + 1;
                if (j >= this.queues.size())
                {
                    j = j - this.queues.size();
                }
                // check if the selected queue is not the current one
                if (j != this.currentQueueNumber)
                {
                    Integer queueBudget = 0;
                    Map<String, Integer> pldDataInQueue = this.pldBudgets.get(this.queues.get(j));
                    if (pldDataInQueue != null)
                    {
                        queueBudget = pldDataInQueue.get(PLD);
                    }
                    if (queueBudget == null)
                    {
                        queueBudget = 0;
                    }
                    LOG.debug("current queue id: {} | j: {} | budget: {} | queueBudget: {}",
                              this.currentQueueNumber, j, budget, queueBudget);
                    if (queueBudget < budget)
                    {
                        this.queues.get(j).add(new Pair<>(url, budget));
                        if (this.pldBudgets.get(this.queues.get(j)) == null)
                        {
                            Map<String, Integer> data = new HashMap<>();
                            data.put(PLD, queueBudget++);
                            this.pldBudgets.put(this.queues.get(j), data);
                        }
                        else
                        {
                            this.pldBudgets.get(this.queues.get(j)).put(PLD, queueBudget++);
                        }
                        found = true;
                        LOG.debug("Adding {} to queue {} which had available {} slot(s)",
                                  url, j, (budget - queueBudget));
                        break;
                    }
                }
            }
            // ... if all 40 places are used the remaining URLs of this PLD are sent to the leftOverQueue
            if (!found)
            {
                this.leftOverQueue.add(new Pair<>(url, budget));
                LOG.debug("No queue found for url {} - using fail-over queue", url);
            }
        }

        this.startOff = true;
    }

    /**
     * Reads the current queue and informs listeners of URLs that passed the budget check.
     * <p>
     * This method is invoked repeatedly by a worker thread.
     */
    private void readCurrentQueue()
    {
        synchronized (this.syncObj)
        {
            for (Pair<String, Integer> data : this.currentQueue)
            {
                for (BEASTBudgetPassedListener listener : this.listeners)
                {
                    listener.handleBudgetPassed(data.getFirst());
                }
            }
            this.currentQueue.clear();
            Map<String, Integer> dataMap = this.pldBudgets.get(this.currentQueue);
            if (dataMap != null)
            {
                dataMap.clear();
            }
            else
            {
                this.pldBudgets.put(this.currentQueue, new HashMap<>());
            }

            readNextQueue();
        }
    }

    /**
     * Sets the next queue in line to read from. If it reaches the end of the list it sets the pointer to the start of
     * the list. Moreover if the last element of the list was reached, it invokes splitting the left-over queue.
     */
    private void readNextQueue()
    {
        int currentSize = this.queues.size();
        int activeQueueNumber = this.currentQueueNumber;

        // check if we already hit the last queue
        if (activeQueueNumber == currentSize)
        {
            // double the size of queues
            for (int i = 0; i < currentSize; i++)
            {
                Queue<Pair<String, Integer>> queue = new LinkedList<>();
                this.queues.add(queue);
                this.pldBudgets.put(queue, new HashMap<>());
            }
            //
            this.splitLeftOverQueue();
        }

        // set the next queue in the line to read from
        if (activeQueueNumber < currentSize - 1)
        {
            this.currentQueueNumber++;
        }
        else
        {
            this.currentQueueNumber = 0;
        }
        this.currentQueue = this.queues.get(this.currentQueueNumber);
    }

    /**
     * Splits the left-over queue into n separate new queues which are added to the list of queues. n represents the
     * current size of queues in the list of queues excluding the left-over queue.
     * <p>
     * Data contained inside the left-over queue is distributed according the budget values of the URLs among the newly
     * created queues.
     */
    private void splitLeftOverQueue()
    {
        LOG.debug("Splitting left-over queue");
        Queue<Pair<String, Integer>> tmpLeftOverQueue = new LinkedList<>();
        for (Pair<String, Integer> data : this.leftOverQueue)
        {
            String PLD = IRLbotUtils.getPLDofURL(data.getFirst());
            int budget = data.getLast();
            boolean found = false;
            // only split the left-over queue between the newly created queues
            for (int i = this.queues.size() / 2; i < this.queues.size(); i++)
            {
                Integer queueBudget = 0;
                Map<String, Integer> pldDataInQueue = this.pldBudgets.get(this.queues.get(i));
                if (pldDataInQueue != null)
                {
                    queueBudget = pldDataInQueue.get(PLD);
                }
                if (queueBudget == null)
                {
                    queueBudget = 0;
                }
                if (queueBudget < budget)
                {
                    this.queues.get(i).add(data);
                    if (this.pldBudgets.get(this.queues.get(i)) == null)
                    {
                        Map<String, Integer> dataMap = new HashMap<>();
                        dataMap.put(PLD, queueBudget++);
                        this.pldBudgets.put(this.queues.get(i), dataMap);
                    }
                    else
                    {
                        this.pldBudgets.get(this.queues.get(i)).put(PLD, queueBudget++);
                    }
                    found = true;
                    LOG.debug("Adding {} to queue {} which had available {} slots", data.getFirst(), i, queueBudget);
                    break;
                }
            }

            if (!found)
            {
                tmpLeftOverQueue.add(data);
                LOG.debug("No queue found for url {} - using fail-over queue", data.getFirst());
            }
        }
        this.leftOverQueue = tmpLeftOverQueue;
    }

    /**
     * Indicates the BEAST algorithm to terminate execution
     */
    public void dispose()
    {
        this.stopRequested = true;
    }

    /**
     * Helper class which constantly reads the current queue
     */
    private class BEASTQueueReader implements Runnable
    {
        @Override
        public void run()
        {
            while (!stopRequested)
            {
                if (startOff)
                {
                    readCurrentQueue();
                }
            }
        }
    }
}
