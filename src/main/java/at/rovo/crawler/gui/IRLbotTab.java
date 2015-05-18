package at.rovo.crawler.gui;

import at.rovo.caching.drum.event.DrumEvent;
import at.rovo.caching.drum.event.StorageEvent;
import at.rovo.crawler.gui.events.IRLbotEventListener;
import java.awt.BorderLayout;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class IRLbotTab extends JPanel implements IRLbotEventListener
{
    /**
     *
     */
    private static final long serialVersionUID = -4337156840802441598L;

    private JLabel numURLsCrawledTotal = new JLabel("0");
    private JLabel numURLsCrawledSuccess = new JLabel("0");
    private JLabel numURLsToCrawl = new JLabel("0");
    private JLabel numUniqueURLs = new JLabel("0");
    private JLabel numUniquePLDs = new JLabel("0");
    private JLabel numRobotTxt = new JLabel("0");
    private JLabel numRobotsRequested = new JLabel("0");
    private JLabel numRobotsTxtDownloadQueue = new JLabel("0");

    public IRLbotTab()
    {
        this.setLayout(new BorderLayout());
        this.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        Manager.getInstance().registerForIRLbotEvents(this);

        // JPanel scrollArea = new JPanel();
        // this.add(new JScrollPane(scrollArea), BorderLayout.CENTER);
        //
        // scrollArea.add(this.initIRLbotStatistics(), BorderLayout.NORTH);
        this.add(this.initIRLbotStatistics(), BorderLayout.NORTH);
    }

    public JPanel initIRLbotStatistics()
    {
        // create a title border
        JPanel infoSection = new JPanel();
        infoSection.setLayout(new BorderLayout());
        infoSection.setBorder(BorderFactory.createTitledBorder(""));

        // create a inner border which has a 10 pixel boundary on every end
        JPanel innerBorder = new JPanel();
        innerBorder.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        innerBorder.setLayout(new BorderLayout());

        Box line = Box.createVerticalBox();

        Box box = Box.createHorizontalBox();
        box.add(new JLabel("Number of URLs crawled total:"));
        box.add(Box.createGlue());
        box.add(this.numURLsCrawledTotal);
        line.add(box);

        box = Box.createHorizontalBox();
        box.add(new JLabel("Number of URLs crawled successfuly:"));
        box.add(Box.createGlue());
        box.add(this.numURLsCrawledSuccess);
        line.add(box);

        box = Box.createHorizontalBox();
        box.add(new JLabel(" "));
        box.add(Box.createGlue());
        line.add(box);

        box = Box.createHorizontalBox();
        box.add(new JLabel("Number of URLs to crawl:"));
        box.add(Box.createGlue());
        box.add(this.numURLsToCrawl);
        line.add(box);

        box = Box.createHorizontalBox();
        box.add(new JLabel("Number of robot.txt file to download:"));
        box.add(Box.createGlue());
        box.add(this.numRobotsTxtDownloadQueue);
        line.add(box);

        box = Box.createHorizontalBox();
        box.add(new JLabel(" "));
        box.add(Box.createGlue());
        line.add(box);

        box = Box.createHorizontalBox();
        box.add(new JLabel("Number of unique URLs stored:"));
        box.add(Box.createGlue());
        box.add(this.numUniqueURLs);
        line.add(box);

        box = Box.createHorizontalBox();
        box.add(new JLabel("Number of unique PLDs stored:"));
        box.add(Box.createGlue());
        box.add(this.numUniquePLDs);
        line.add(box);

        box = Box.createHorizontalBox();
        box.add(new JLabel("Number of robot.txt files stored:"));
        box.add(Box.createGlue());
        box.add(this.numRobotTxt);
        line.add(box);

        box = Box.createHorizontalBox();
        box.add(new JLabel("Number of robot.txt files requested:"));
        box.add(Box.createGlue());
        box.add(this.numRobotsRequested);
        line.add(box);

        innerBorder.add(line);

        infoSection.add(innerBorder, BorderLayout.NORTH);

        return infoSection;
    }

    @Override
    public void numberOfURLsCrawledTotalChanged(final long newSize)
    {
        this.numURLsCrawledTotal.setText("" + newSize);
        this.numURLsCrawledTotal.repaint();
    }

    @Override
    public void numberOfURLsCrawledSuccessChanged(final long newSize)
    {
        this.numURLsCrawledSuccess.setText("" + newSize);
        this.numURLsCrawledSuccess.repaint();
    }

    @Override
    public void numberOfURLsToCrawlChanged(final long newSize)
    {
        this.numURLsToCrawl.setText("" + newSize);
        this.numURLsToCrawl.repaint();
    }

    @Override
    public void numberOfRobotsTxtToDownload(final long newSize)
    {
        this.numRobotsTxtDownloadQueue.setText("" + newSize);
        this.numRobotsTxtDownloadQueue.repaint();
    }

    @Override
    public void numberOfUniqueURLs(final DrumEvent<? extends DrumEvent<?>> event)
    {
        this.numUniqueURLs.setText("" + ((StorageEvent) event).getNumberOfEntries());
        this.numUniqueURLs.repaint();
    }

    @Override
    public void numberOfUniquePLDs(final DrumEvent<? extends DrumEvent<?>> event)
    {
        this.numUniquePLDs.setText("" + ((StorageEvent) event).getNumberOfEntries());
        this.numUniquePLDs.repaint();
    }

    @Override
    public void numberOfUniqueRobotEntries(final DrumEvent<? extends DrumEvent<?>> event)
    {
        this.numRobotTxt.setText("" + ((StorageEvent) event).getNumberOfEntries());
        this.numRobotTxt.repaint();
    }

    @Override
    public void numberOfRobotEntriesRequested(final DrumEvent<? extends DrumEvent<?>> event)
    {
        this.numRobotsRequested.setText("" + ((StorageEvent) event).getNumberOfEntries());
        this.numRobotsRequested.repaint();
    }
}
