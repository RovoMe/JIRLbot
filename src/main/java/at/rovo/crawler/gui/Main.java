package at.rovo.crawler.gui;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import javax.swing.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main implements Runnable
{
	/** The logger of this class **/
	private static Logger LOG;

	private List<String> lookAndFeels = Arrays.asList(
			//"com.seaglasslookandfeel.SeaGlassLookAndFeel",        // SeaGlass has issues on Mac OS X
			//"apple.laf.AquaLookAndFeel",                          // Apples default look and feel does not show button
			                                                        // colors correctly and is only available on Mac
			//"com.sun.java.swing.plaf.windows.WindowsLookAndFeel", // Windows L&F is only available on Windows machines
			//"com.sun.java.swing.plaf.gtk.GTKLookAndFeel",         // GTK L&F is only available on Linux machines with
			                                                        // GTK installed
			"javax.swing.plaf.nimbus.NimbusLookAndFeel",
			"javax.swing.plaf.metal.MetalLookAndFeel",
			"javax.swing.plaf.basic.BasicLookAndFeel",
			"com.sun.java.swing.plaf.motif.MotifLookAndFeel"
	);
	
	public static void initLogger() throws URISyntaxException
	{
		String path = Main.class.getResource("/log/log4j2.xml").toURI().getPath();
		System.setProperty("log4j.configurationFile", path);
		LOG = LogManager.getLogger(Main.class);
	}
	
	public static void main(String[] args) throws URISyntaxException
	{
		String lookAndFeel = null;
		for (String arg : args)
		{
			if (arg.startsWith("-laf") || arg.startsWith("--lookandfeel"))
			{
				if (arg.contains("="))
					lookAndFeel = arg.split("=")[1];
				else
					throw new IllegalArgumentException("Invalid look and feel format recognized. Use -laf=YourPreferredLookAndFeel or --lookandfeel=YourPreferredLookAndFeel" );
			}
			else if ("-h".equals(arg) || "--help".equals(arg))
			{
				System.out.println("Use: java -cp=JIRLbot.jar at.rovo.crawler.gui.Main [-h|--help|-laf|--lookandfeel]");
				System.out.println("");
				System.out.println("Supported arguments:");
				System.out.println("\t-h or --help\t\tPrints this help");
				System.out.println("\t-laf or --lookandfeel\tSpecifies the preferred look and feel to use");
			}
		}
		initLogger();
		SwingUtilities.invokeLater(new Main(lookAndFeel));
	}

	public Main(String lookAndFeel)
	{
		if (lookAndFeel != null)
		{
			lookAndFeels.add(0, lookAndFeel);
		}
	}

	@Override
	public void run()
	{
		LOG.info("Initialized JIRLbot application");
		// list of look and feels to try. The first one available will be used

		boolean success = false;
		for (String lookAndFeel : lookAndFeels)
		{
			try
			{
				UIManager.setLookAndFeel(lookAndFeel);
				String used;
				if (lookAndFeel.contains("."))
					used = lookAndFeel.substring(lookAndFeel.lastIndexOf(".“")+1);
				else
					used = lookAndFeel;
				LOG.info("UIManager is using {}", used);
				success = true;
				break;
			}
			catch (Exception e)
			{
				LOG.warn("Could not load {}. Reason: {}", e.getLocalizedMessage());
			}
		}
		if (!success)
		{
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			}
			catch (Exception e)
			{
				LOG.error("Could not load systems default look and feel after all previous attempts failed!", e);
				System.exit(1);
			}
		}
		new MainWindow();
	}
}
