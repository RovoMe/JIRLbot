package at.rovo.crawler.gui;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

public class ConfigurationTab extends JPanel implements ActionListener, KeyListener
{
	private static final long serialVersionUID = -2155169146694876983L;
	private final JTextField txtAddSeedPage = new JTextField("");
		
	private final JButton btnAddSeedPage = new JButton("add");
	private final JButton btnClearList = new JButton("clear");
	private final JButton btnSynchronize = new JButton("synchronize");
	private final JButton btnStartStopCrawl = new JButton("start");
	private final JButton btnPauseResumeCrawl = new JButton("pause");
	
	private final JList<String> lstSeedPages = new JList<>();
	private final DefaultListModel<String> listModel = new DefaultListModel<String>();
	
	private final JTextField numCrawlingThreads = new JTextField("100", 8);
	private final JTextField numRobotsDownloaderThreads = new JTextField("10", 8);
	private final JTextField numUrlSeenBuckets = new JTextField("512", 8);
	private final JTextField numUrlSeenByteSize = new JTextField("1024", 8);
	private final JTextField numPldIndegreeBuckets = new JTextField("16", 8);
	private final JTextField numPldIndegreeByteSize = new JTextField("64", 8);
	private final JTextField numRobotsCacheBuckets = new JTextField("16", 8);
	private final JTextField numRobotsCacheByteSize = new JTextField("256", 8);
	private final JTextField numRobotsRequestedBuckets = new JTextField("16", 8);
	private final JTextField numRobotsRequestedByteSize = new JTextField("64", 8);	
		
	public ConfigurationTab()
	{
		this.setLayout(new BorderLayout());
		this.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		
		this.add(this.initAddSeedSection(), BorderLayout.NORTH);
		this.add(this.initSeedListSection(), BorderLayout.CENTER);	
		this.add(this.initButtonsSection(), BorderLayout.SOUTH);
		
		this.addURL("http://www.winf.at");
		this.addURL("http://www.tuwien.ac.at");
		this.addURL("http://www.univie.ac.at");
		this.addURL("http://www.orf.at");
		this.addURL("http://www.apa.at");
		this.addURL("http://www.krone.at");
		// top 15 news web sites
		this.addURL("http://www.yahoo.com");
		this.addURL("http://www.cnn.com");
		this.addURL("http://www.msnbc.com");
		this.addURL("http://news.google.com");
		this.addURL("http://www.nytimes.com");
		this.addURL("http://www.huffingtonpost.com");
		this.addURL("http://www.foxnews.com");
		this.addURL("http://www.washingtonpost.com");
		this.addURL("http://www.latimes.com");
		this.addURL("http://www.dailymail.co.uk");
		this.addURL("http://www.reuters.com");
		this.addURL("http://www.abcnews.go.com");
		this.addURL("http://www.usatoday.com");
		this.addURL("http://www.bbc.com");
		this.addURL("http://www.drudgereport.com");
	}	
	
	private JPanel initAddSeedSection()
	{
		// create a title border
		JPanel addSeedPagePanel = new JPanel();
		addSeedPagePanel.setLayout(new BorderLayout());
		addSeedPagePanel.setBorder(BorderFactory.createTitledBorder("Add seed page"));
		
		// create a inner border which has a 10 pixel boundary on every end
		JPanel innerBorder = new JPanel();
		innerBorder.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		innerBorder.setLayout(new BorderLayout());
		
		// create the input area
		JPanel inputArea = new JPanel();
		inputArea.setLayout(new BorderLayout());
		this.txtAddSeedPage.addKeyListener(this);
		this.btnAddSeedPage.addActionListener(this);
		
		inputArea.add(this.txtAddSeedPage, BorderLayout.CENTER);
		inputArea.add(this.btnAddSeedPage, BorderLayout.EAST);
		
		innerBorder.add(inputArea, BorderLayout.SOUTH);
		
		// add the inner border to the title border
		addSeedPagePanel.add(innerBorder, BorderLayout.CENTER);
		
		return addSeedPagePanel;
	}
	
	private JPanel initSeedListSection()
	{
		// create a title border
		JPanel listArea = new JPanel();
		listArea.setLayout(new BorderLayout());
		listArea.setBorder(BorderFactory.createTitledBorder("Current seed pages"));
		
		// create a inner border which has a 10 pixel boundary on every end
		JPanel innerBorder = new JPanel();
		innerBorder.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		innerBorder.setLayout(new BorderLayout());
		
		// create the list in the center
//		this.lstSeedPages = new JList<>();
		this.lstSeedPages.setModel(this.listModel);
		innerBorder.add(new JScrollPane(this.lstSeedPages), BorderLayout.CENTER);
		// add a button at the bottom of the group which does not occupy the whole
		// space
		Box clearListBox = Box.createHorizontalBox();
//		this.btnClearList = new JButton("clear");
		this.btnClearList.addActionListener(this);
		this.btnClearList.setEnabled(false);
		clearListBox.add(this.btnClearList);
		innerBorder.add(clearListBox, BorderLayout.SOUTH);
		
		// add the inner border to the title border
		listArea.add(innerBorder, BorderLayout.CENTER);
		
		return listArea;
	}
	
	private JPanel initButtonsSection()
	{		
		this.btnSynchronize.addActionListener(this);
		this.btnSynchronize.setEnabled(false);
		// create a start/stop button
		this.btnStartStopCrawl.addActionListener(this);
		this.btnStartStopCrawl.setEnabled(false);
		// create a pause/resume button
		this.btnPauseResumeCrawl.addActionListener(this);
		this.btnPauseResumeCrawl.setEnabled(false);
		
		// create the panel that contains the buttons
		JPanel buttonsContainer = new JPanel();
		buttonsContainer.setBorder(BorderFactory.createTitledBorder("Crawler"));
		buttonsContainer.setLayout(new BorderLayout());
		
		JPanel innerBorder1 = new JPanel();
		innerBorder1.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		innerBorder1.setLayout(new BorderLayout());
		// create the extended options collapse panel
		CollapsePanel collapsePanel = new CollapsePanel("Extended Options", this.initExtendedOptions());
		innerBorder1.add(collapsePanel, BorderLayout.CENTER);
		buttonsContainer.add(innerBorder1, BorderLayout.NORTH);
		
		JPanel innerBorder2 = new JPanel();
		innerBorder2.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		innerBorder2.setLayout(new BorderLayout());

		// create the button-area with a space between the two buttons
		Box btnBox = Box.createHorizontalBox();
		btnBox.add(this.btnSynchronize);
		btnBox.add (Box.createHorizontalStrut (10));
		btnBox.add(this.btnStartStopCrawl);
		btnBox.add (Box.createHorizontalStrut (10));
		btnBox.add(this.btnPauseResumeCrawl);
		
		innerBorder2.add(btnBox, BorderLayout.EAST);
		
		// add the inner border to the title border
		buttonsContainer.add(innerBorder2, BorderLayout.SOUTH);
		
		return buttonsContainer;
	}
	
	private JPanel initExtendedOptions()
	{
		JPanel options = new JPanel();
		options.setLayout(new GridBagLayout());
		options.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(1, 3, 0, 3);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		
		this.addLine(options, new JLabel("Crawling threads: "), this.numCrawlingThreads, 
				new JLabel("Robot.txt crawlers: "), this.numRobotsDownloaderThreads, gbc);
		this.addLine(options, new JLabel("URLseen buckets: "), this.numUrlSeenBuckets, 
				new JLabel("URLseen bytes per bucket: "), this.numUrlSeenByteSize, gbc);
		this.addLine(options, new JLabel("pldIndegree buckets: "), this.numPldIndegreeBuckets, 
				new JLabel("pldIndegree bytes per bucket: "), this.numPldIndegreeByteSize, gbc);
		this.addLine(options, new JLabel("robotsCache buckets: "), this.numRobotsCacheBuckets, 
				new JLabel("robotsCache bytes per bucket: "), this.numRobotsCacheByteSize, gbc);
		this.addLine(options, new JLabel("robotsRequested buckets: "), this.numRobotsRequestedBuckets, 
				new JLabel("robotsRequested byptes per bucket: "), this.numRobotsRequestedByteSize, gbc);
		
		return options;
	}
	
	private void addLine(JPanel parent, JLabel label, JTextField text, JLabel label2, JTextField text2, GridBagConstraints gbc)
	{
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.gridwidth = 1; // Default value
		gbc.weightx = 0.0;
		parent.add(label, gbc);
		parent.add(text, gbc);
		text.setHorizontalAlignment(JTextField.RIGHT);
		
		gbc.weightx = 0.5;
		parent.add(new JLabel(), gbc);
		
		gbc.weightx = 0.0;
		parent.add(label2, gbc);
		parent.add(text2, gbc);
		text2.setHorizontalAlignment(JTextField.RIGHT);
		
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 0.5;
		parent.add(new JLabel(), gbc);
	}

	private void addURL(String seedPage)
	{
		if (seedPage != null || "".equals(seedPage))
		{
			this.listModel.addElement(seedPage);
		}
		// activate the clear button as well as the start-button for the crawling
		if (!this.btnClearList.isEnabled() && this.listModel.getSize() > 0)
		{
			this.btnClearList.setEnabled(true);
			this.btnStartStopCrawl.setEnabled(true);
		}
		// clear the input text
		this.txtAddSeedPage.setText("");
	}
	
	@Override
	public void actionPerformed(ActionEvent e)
	{
		String command = e.getActionCommand();
		if ("add".equals(command))
		{
			this.addURL(this.txtAddSeedPage.getText());
		}
		else if ("clear".equals(command))
		{
			this.btnClearList.setEnabled(false);
			this.btnSynchronize.setEnabled(false);
			this.btnStartStopCrawl.setEnabled(false);
			this.btnPauseResumeCrawl.setEnabled(false);
			this.listModel.clear();
		}
		else if ("start".equals(command))
		{
			this.btnAddSeedPage.setEnabled(false);
			this.txtAddSeedPage.setEnabled(false);
			this.lstSeedPages.setEnabled(false);
			this.btnClearList.setEnabled(false);
			this.btnSynchronize.setEnabled(true);
			this.btnPauseResumeCrawl.setEnabled(true);
			this.btnStartStopCrawl.setText("stop");
			
			this.numCrawlingThreads.setEnabled(false);
			this.numRobotsDownloaderThreads.setEnabled(false);
			this.numUrlSeenBuckets.setEnabled(false);
			this.numUrlSeenByteSize.setEnabled(false);
			this.numPldIndegreeBuckets.setEnabled(false);
			this.numPldIndegreeByteSize.setEnabled(false);
			this.numRobotsCacheBuckets.setEnabled(false);
			this.numRobotsCacheByteSize.setEnabled(false);
			this.numRobotsRequestedBuckets.setEnabled(false);
			this.numRobotsRequestedByteSize.setEnabled(false);
			
			String[] urls = new String[this.listModel.size()];
			for (int i=0; i<this.listModel.getSize(); i++)
			{
				urls[i] = this.listModel.get(i);
			}
				
			try
			{
				int crawlingThreads = Integer.parseInt(this.numCrawlingThreads.getText());
				int robotsDownloader = Integer.parseInt(this.numRobotsDownloaderThreads.getText());
				int urlSeenBuckets = Integer.parseInt(this.numUrlSeenBuckets.getText());
				int urlSeenByteSize = Integer.parseInt(this.numUrlSeenByteSize.getText());
				int pldBuckets = Integer.parseInt(this.numPldIndegreeBuckets.getText());
				int pldByteSize = Integer.parseInt(this.numPldIndegreeByteSize.getText());
				int robotsCacheBuckets = Integer.parseInt(this.numRobotsCacheBuckets.getText());
				int robotsCacheByteSize = Integer.parseInt(this.numRobotsCacheByteSize.getText());
				int robotsRequestedBuckets = Integer.parseInt(this.numRobotsRequestedBuckets.getText());
				int robotsRequestedByteSize = Integer.parseInt(this.numRobotsRequestedByteSize.getText());
				
				Manager.getInstance().createCrawler(urls, crawlingThreads, robotsDownloader, 
						urlSeenBuckets, urlSeenByteSize, pldBuckets, pldByteSize, 
						robotsCacheBuckets, robotsCacheByteSize, robotsRequestedBuckets, robotsRequestedByteSize);
				
				Manager.getInstance().getIRLbot().crawl();
			}
			catch (NumberFormatException nfE)
			{
				System.err.println("Error parsing String into an integer!");
				nfE.printStackTrace();
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}
		else if ("stop".equals(command))
		{
			this.btnAddSeedPage.setEnabled(true);
			this.txtAddSeedPage.setEnabled(true);
			this.lstSeedPages.setEnabled(true);
			if (this.listModel.getSize() > 0)
				this.btnClearList.setEnabled(true);
			else
				this.btnClearList.setEnabled(false);
			this.btnSynchronize.setEnabled(false);
			this.btnPauseResumeCrawl.setEnabled(false);
			this.btnStartStopCrawl.setText("start");
			
			this.numCrawlingThreads.setEnabled(true);
			this.numRobotsDownloaderThreads.setEnabled(true);
			this.numUrlSeenBuckets.setEnabled(true);
			this.numUrlSeenByteSize.setEnabled(true);
			this.numPldIndegreeBuckets.setEnabled(true);
			this.numPldIndegreeByteSize.setEnabled(true);
			this.numRobotsCacheBuckets.setEnabled(true);
			this.numRobotsCacheByteSize.setEnabled(true);
			this.numRobotsRequestedBuckets.setEnabled(true);
			this.numRobotsRequestedByteSize.setEnabled(true);
			
			Manager.getInstance().getIRLbot().stop();
		}
		else if ("pause".equals(command))
		{
			this.btnPauseResumeCrawl.setText("resume");
		}
		else if ("resume".equals(command))
		{
			this.btnPauseResumeCrawl.setText("pause");
		}
		else if ("synchronize".equals(command))
		{
			this.setCursor(new Cursor(Cursor.WAIT_CURSOR));
			Manager.getInstance().getIRLbot().synchronize();
			this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		}
	}

	@Override
	public void keyTyped(KeyEvent e)
	{

	}

	@Override
	public void keyPressed(KeyEvent e)
	{		
		if (KeyEvent.VK_ENTER == e.getExtendedKeyCode())
		{
			this.addURL(this.txtAddSeedPage.getText());
		}
	}

	@Override
	public void keyReleased(KeyEvent e)
	{

	}
}
