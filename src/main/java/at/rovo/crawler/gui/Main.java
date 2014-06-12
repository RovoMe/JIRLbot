package at.rovo.crawler.gui;

import java.net.URISyntaxException;
import javax.swing.SwingUtilities;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main implements Runnable
{
	/** The logger of this class **/
	private static Logger logger; 
	
	public static void initLogger() throws URISyntaxException
	{
		String path = Main.class.getResource("/log/log4j2.xml").toURI().getPath();
		System.setProperty("log4j.configurationFile", path);
		logger = LogManager.getLogger(Main.class);
	}
	
	public static void main(String[] args) throws URISyntaxException
	{
		initLogger();
		SwingUtilities.invokeLater(new Main());
	}

	@Override
	public void run()
	{
		logger.info("Initialized JIRLbot application");
		new MainWindow();		
	}
}
