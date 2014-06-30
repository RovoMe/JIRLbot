package at.rovo.crawler.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JTabbedPane;
import at.rovo.crawler.gui.events.IRLbotCreationListener;

public class MainWindow extends JFrame implements ActionListener, WindowListener, 
	IRLbotCreationListener
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -5030363401745630286L;
	
	// content area
	private JTabbedPane tabbedPane = new JTabbedPane();

	public MainWindow()
	{	
		Thread.currentThread().setName("main");
		
		Manager.getInstance().registerForIRLbotCreationEvents(this);

		JMenuBar menuBar = new JMenuBar();

		JMenu menuFile = new JMenu("File");
		JMenuItem menuFileStart = new JMenuItem("Start");
		JMenuItem menuFileClose = new JMenuItem("Close");
		JMenu menuHelp = new JMenu("Help");
		JMenuItem menuHelpAbout = new JMenuItem("About ...");

		// set Mnemonics
		menuFile.setMnemonic('F');
		menuHelp.setMnemonic('H');
		menuFileStart.setMnemonic('S');
		menuFileClose.setMnemonic('C');
		menuHelpAbout.setMnemonic('A');
		
		// add action listeners to the menu items
		menuFileStart.addActionListener(this);
		menuFileClose.addActionListener(this);
		menuHelpAbout.addActionListener(this);
		
		// set the menu bar and its entries
		menuFile.add(menuFileStart);
		menuFile.addSeparator();
		menuFile.add(menuFileClose);
		menuHelp.add(menuHelpAbout);
		menuBar.add(menuFile);
		menuBar.add(menuHelp);
		this.setJMenuBar(menuBar);
		
		// add tabs to the main pane
		this.tabbedPane.addTab("Configuration", new ConfigurationTab());
		
		this.add(this.tabbedPane);
		
		// show the frame
		this.setTitle("IRLbot WebCrawler");
		this.setSize(1024, 768);
		this.setVisible(true);
		this.addWindowListener(this);
//		this.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
	}
	
	@Override
	public void irlbotCreated()
	{
		this.tabbedPane.addTab("IRLbot", new IRLbotTab());
		this.tabbedPane.addTab("URLseen", new URLseenTab(Manager.getInstance().getIRLbot().getNumberOfURLseenBuckets()));
		this.tabbedPane.addTab("STAR", new STARTab(Manager.getInstance().getIRLbot().getNumberOfSTARBuckets()));
		this.tabbedPane.addTab("BEAST", new BEASTTab());
		this.tabbedPane.addTab("RobotsCache" ,new RobotsCacheTab(Manager.getInstance().getIRLbot().getNumberOfRobotsCacheBuckets()));
		this.tabbedPane.addTab("RobotsRequested" ,new RobotsRequestedTab(Manager.getInstance().getIRLbot().getNumberOfRobotsRequestedBuckets()));
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		String command = e.getActionCommand();
		
		if ("Close".equals(command))
		{
			this.setVisible(false);
			System.exit(0);
		}
	}

	@Override
	public void windowOpened(WindowEvent e)
	{
		
	}


	@Override
	public void windowClosing(WindowEvent e)
	{
		System.exit(0);
	}


	@Override
	public void windowClosed(WindowEvent e)
	{
		
	}


	@Override
	public void windowIconified(WindowEvent e)
	{
		
	}


	@Override
	public void windowDeiconified(WindowEvent e)
	{
		
	}


	@Override
	public void windowActivated(WindowEvent e)
	{
		
	}


	@Override
	public void windowDeactivated(WindowEvent e)
	{
		
	}
}
