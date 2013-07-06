package at.rovo.test;

import static org.junit.Assert.assertEquals;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.junit.Assert;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import at.rovo.caching.drum.DrumException;
import at.rovo.caching.drum.IDrumListener;
import at.rovo.caching.drum.event.DrumEvent;
import at.rovo.crawler.STAR;
import at.rovo.crawler.bean.PLDData;
import at.rovo.crawler.interfaces.CheckSpamUrlListener;

public class STARTest implements IDrumListener, CheckSpamUrlListener
{
	/** The logger of this class **/
	private static Logger logger;
	private STAR star = null;
	private File cache = null;
	private String checkReturnURL = null;
	private int checkReturnBudget = 0;
	private boolean closed = false;
	
	@BeforeClass
	public static void initLogger() throws URISyntaxException
	{
		String path = DrumTest.class.getResource("/log/log4j2-test.xml").toURI().getPath();
		System.setProperty("log4j.configurationFile", path);
		logger = LogManager.getLogger(STARTest.class);
	}
	
	@AfterClass
	public static void cleanLogger()
	{
		System.clearProperty("log4j.configurationFile");
	}
	
	@Before
	public void init()
	{
		String appDirPath = System.getProperty("user.dir");
		File appDir = new File(appDirPath);
		if (appDir.isDirectory())
		{
			String[] items = appDir.list();
			for (String item : items)
			{
				if (item.endsWith("cache"))
				{
					this.cache = new File(item);
					if (this.cache.isDirectory() && "cache".equals(this.cache.getName()))
					{
						try
						{
							Files.walkFileTree(this.cache.toPath(), new CacheFileDeleter());
						}
						catch (IOException e)
						{
							e.printStackTrace();
						}
					}
				}
			}
		}
		if (this.cache == null)
			this.cache = new File (appDir.getAbsoluteFile()+"/cache");
		if (!this.cache.exists())
			this.cache.mkdir();
	}
	
	@Test
	public void testSTAR_Top3()
	{
		this.testTopN(3);
	}
	
	@Test
	public void testSTAR_Top6()
	{		
		this.testTopN(6);
	}
	
	@Test
	public void testSTAR_Top10()
	{
		this.testTopN(10);
	}
	
	private void testTopN(int topN)
	{
		try
		{
			this.star = new STAR(2, 32, this);
			this.star.addCheckSpamUrlListener(this);
			this.star.setMaxBudget(50);
			this.star.setTopN(topN);
			
			// Austrian institutes dealing with business informatics
			String pld1 = "http://www.tuwien.ac.at";
			String pld2 = "http://www.univie.ac.at";
			String pld3 = "http://www.winf.at";
			String pld4 = "http://www.jku.at";
			String pld5 = "http://www.technikum-wien.at";
			String pld6 = "http://www.wu.ac.at";
			String pld7 = "http://www.fh-kufstein.ac.at";
			String pld8 = "http://www.it-campus.at";
			
			Set<String> plds = new TreeSet<String>();
			plds.add(pld1); // http://www.tuwien.ac.at<--http://www.winf.at (-427820934381562479<--356380646382129811)
			plds.add(pld2); // http://www.univie.ac.at<--http://www.winf.at (-408508820557862601<--356380646382129811)
			plds.add(pld3); // http://www.winf.at<--http://www.winf.at (356380646382129811<--356380646382129811)
			this.star.update(pld3, plds);
			
			plds = new TreeSet<String>();
			plds.add(pld1); // http://www.tuwien.ac.at<--http://www.it-campus.at (-427820934381562479<---710095566944599804)
			plds.add(pld2); // http://www.univie.ac.at<--http://www.it-campus.at (-408508820557862601<---710095566944599804)
			plds.add(pld3); // http://www.winf.at<--http://www.it-campus.at (356380646382129811<---710095566944599804)
			plds.add(pld4); // http://www.jku.at<--http://www.it-campus.at (7747227535262285713<---710095566944599804)
			plds.add(pld5); // http://www.technikum-wien.at<--http://www.it-campus.at (-798419812034467833<---710095566944599804)
			plds.add(pld6); // http://www.wu.ac.at<--http://www.it-campus.at (-7398944027048837133<---710095566944599804)
			plds.add(pld7); // http://www.fh-kufstein.ac.at<--http://www.it-campus.at (-1122805766134849723<---710095566944599804)
			plds.add(pld8); // http://www.it-campus.at<--http://www.it-campus.at (-710095566944599804<---710095566944599804)
			this.star.update(pld8, plds);
			
			plds = new TreeSet<String>();
			plds.add(pld1); // http://www.tuwien.ac.at<--http://www.tuwien.ac.at (-427820934381562479<---427820934381562479)
			plds.add(pld3); // http://www.winf.at<--http://www.tuwien.ac.at (356380646382129811<---427820934381562479)
			plds.add(pld8); // http://www.it-campus.at<--http://www.tuwien.ac.at (-710095566944599804<---427820934381562479)
			this.star.update(pld1, plds);
			
			plds = new TreeSet<String>();
			plds.add(pld1); // http://www.tuwien.ac.at<--http://www.winf.at (-427820934381562479<--356380646382129811)
			plds.add(pld2); // http://www.univie.ac.at<--http://www.winf.at (-408508820557862601<--356380646382129811)
			plds.add(pld3); // http://www.winf.at<--http://www.winf.at (356380646382129811<--356380646382129811)
			plds.add(pld6); // http://www.wu.ac.at<--http://www.winf.at (-7398944027048837133<--356380646382129811)
			this.star.update(pld3, plds);
			
			plds = new TreeSet<String>();
			plds.add(pld5); // http://www.technikum-wien.at<--http://www.technikum-wien.at (-798419812034467833<---798419812034467833)
			plds.add(pld8); // http://www.it-campus.at<--http://www.technikum-wien.at (-710095566944599804<---798419812034467833)
			this.star.update(pld5, plds);
					
			// in-degree value:
			// tuwien: 3
			// univie: 2
			// winf: 3
			// jku: 1
			// technikum: 2
			// wu: 2
			// kufstein: 1
			// campus: 3
			
			this.star.check(pld1);
			
			// we need to synchronize here as not enough data is stored into the
			// buffers/buckets to produce automatic merges
			this.star.synchronize();
			
			Set<PLDData> pldData = this.star.getTopNSet();
			List<PLDData> pldList = new ArrayList<>();
			pldList.addAll(pldData);
			// we only have 8 entries so in case more entries should be selected
			assertEquals(Math.min(8, topN), pldList.size());
			// test in-degree levels
			if (topN > 0)
				assertEquals(3, pldList.get(0).getIndegree());
			if (topN > 1)
				assertEquals(3, pldList.get(1).getIndegree());
			if (topN > 2)
				assertEquals(3, pldList.get(2).getIndegree());
			if (topN > 3)
				assertEquals(2, pldList.get(3).getIndegree());
			if (topN > 4)
				assertEquals(2, pldList.get(4).getIndegree());
			if (topN > 5)
				assertEquals(2, pldList.get(5).getIndegree());
			if (topN > 6)
				assertEquals(1, pldList.get(6).getIndegree());
			if (topN > 7)
				assertEquals(1, pldList.get(7).getIndegree());
			
			// test budgets
			if (topN > 0)
				assertEquals(50, pldList.get(0).getBudget());
			if (topN > 1)
				assertEquals(50, pldList.get(1).getBudget());
			if (topN > 2)
				assertEquals(50, pldList.get(2).getBudget());
			if (topN > 3)
				assertEquals(47, pldList.get(3).getBudget());
			if (topN > 4)
				assertEquals(47, pldList.get(4).getBudget());
			if (topN > 5)
				assertEquals(47, pldList.get(5).getBudget());
			if (topN > 6)
				assertEquals(44, pldList.get(6).getBudget());
			if (topN > 7)
				assertEquals(44, pldList.get(7).getBudget());
			
			// test PLD names
			PLDData test = null;
			if (topN > 0)
			{
				test = pldList.get(0);
				Assert.assertTrue(pld1.equals(test.getPLD()) || pld3.equals(test.getPLD()) || pld8.equals(test.getPLD()));
			}
			if (topN > 1)
			{
				test = pldList.get(1);
				Assert.assertTrue(pld1.equals(test.getPLD()) || pld3.equals(test.getPLD()) || pld8.equals(test.getPLD()));
			}
			if (topN > 2)
			{
				test = pldList.get(2);
				Assert.assertTrue(pld1.equals(test.getPLD()) || pld3.equals(test.getPLD()) || pld8.equals(test.getPLD()));
			}
			
			if (topN > 3)
			{
				test = pldList.get(3);
				Assert.assertTrue(pld2.equals(test.getPLD()) || pld5.equals(test.getPLD()) || pld6.equals(test.getPLD()));
			}
			if (topN > 4)
			{
				test = pldList.get(4);
				Assert.assertTrue(pld2.equals(test.getPLD()) || pld5.equals(test.getPLD()) || pld6.equals(test.getPLD()));
			}
			if (topN > 5)
			{
				test = pldList.get(5);
				Assert.assertTrue(pld2.equals(test.getPLD()) || pld5.equals(test.getPLD()) || pld6.equals(test.getPLD()));
			}
			
			if (topN > 6)
			{
				test = pldList.get(6);
				Assert.assertTrue(pld4.equals(test.getPLD()) || pld7.equals(test.getPLD()));
			}
			if (topN > 7)
			{
				test = pldList.get(7);
				Assert.assertTrue(pld4.equals(test.getPLD()) || pld7.equals(test.getPLD()));
			}
			
			Assert.assertEquals(pld1, this.checkReturnURL);
			Assert.assertEquals(50, this.checkReturnBudget);
			
			logger.info("URL: {}; Budget: {}", this.checkReturnURL, this.checkReturnBudget);
			
			this.star.check(pld5);
			this.star.synchronize();
			
			logger.info("URL: {}; Budget: {}", this.checkReturnURL, this.checkReturnBudget);
			
			Assert.assertEquals(pld5, this.checkReturnURL);
			if (topN > 3)
				Assert.assertEquals(47, this.checkReturnBudget);
			else
				Assert.assertEquals(10, this.checkReturnBudget);
			
			// Entries will have to wait for the merge phase before a budget is returned
			this.star.check(pld4);
			
			Assert.assertNotSame(pld4, this.checkReturnURL);
			if (topN < 6)
				Assert.assertNotSame(44, this.checkReturnBudget);
			else if (topN < 3)
				Assert.assertNotSame(10, this.checkReturnBudget);
			
			// synchronize star so the data stored in the buffer or in the bucket file
			// is stored in the datastore
			this.star.synchronize();
			
			logger.info("URL: {}; Budget: {}", this.checkReturnURL, this.checkReturnBudget);
			
			Assert.assertEquals(pld4, this.checkReturnURL);
			if (topN > 6)
				Assert.assertEquals(44, this.checkReturnBudget);
			else
				Assert.assertEquals(10, this.checkReturnBudget);
		}
		catch (DrumException e)
		{
			logger.catching(e);
		}
		finally
		{
			if (this.star != null)
			{
				try
				{
					this.star.dispose();
				}
				catch (DrumException e)
				{
					if (logger.isErrorEnabled())
						logger.error(e.getMessage());
				}
			}
			this.closed = true;
		}
	}	

	@Override
	public void update(DrumEvent<? extends DrumEvent<?>> event)
	{
		
	}
	
	@Override
	public void handleSpamCheck(String aux, int budget)
	{
		this.checkReturnURL = aux;
		this.checkReturnBudget = budget;
	}
	
	@After
	public void clean()
	{
		if (this.star != null && !this.closed)
		{
			try
			{
				this.star.dispose();
			}
			catch (DrumException e)
			{
				logger.catching(e);
			}
		}
		
		File cache = this.cache;
		if (cache.isDirectory() && "cache".equals(cache.getName()))
		{
			try
			{
				Files.walkFileTree(cache.toPath(), new CacheFileDeleter());
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}
}
