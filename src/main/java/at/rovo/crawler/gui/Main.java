package at.rovo.crawler.gui;

import javax.swing.SwingUtilities;

public class Main implements Runnable
{
	public static void main(String[] args)
	{
		SwingUtilities.invokeLater(new Main());
	}

	@Override
	public void run()
	{
		new MainWindow();		
	}
}
