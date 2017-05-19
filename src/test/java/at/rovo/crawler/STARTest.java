package at.rovo.crawler;

import at.rovo.drum.DrumException;
import at.rovo.drum.DrumListener;
import at.rovo.drum.event.DrumEvent;
import at.rovo.drum.utils.BaseDataStoreTest;
import at.rovo.crawler.bean.PLDData;
import at.rovo.crawler.interfaces.CheckSpamUrlListener;
import at.rovo.crawler.util.IRLbotUtils;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class STARTest extends BaseDataStoreTest implements DrumListener, CheckSpamUrlListener
{
    private final static Logger LOG = LogManager.getLogger(MethodHandles.lookup().lookupClass());

    private STAR star = null;
    private String checkReturnURL = null;
    private int checkReturnBudget = 0;
    private boolean closed = false;

    @Test
    public void testSTAR_Top3()
    {
        LOG.info("Testing STAR for top3 set");
        this.testTopN(3);
    }

    @Test
    public void testSTAR_Top6()
    {
        LOG.info("Testing STAR for top6 set");
        this.testTopN(6);
    }

    @Test
    public void testSTAR_Top10()
    {
        LOG.info("Testing STAR for top10 set");
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
            String url1 = "http://www.tuwien.ac.at";
            String url2 = "http://www.univie.ac.at";
            String url3 = "http://www.winf.at";
            String url4 = "http://www.jku.at";
            String url5 = "http://www.technikum-wien.at";
            String url6 = "http://www.wu.ac.at";
            String url7 = "http://www.fh-kufstein.ac.at";
            String url8 = "http://www.it-campus.at";
            String pld1 = IRLbotUtils.getPLDofURL(url1); //  2100990832276391538
            String pld2 = IRLbotUtils.getPLDofURL(url2); //  2120302946100091416
            String pld3 = IRLbotUtils.getPLDofURL(url3); // -5513532477925238510
            String pld4 = IRLbotUtils.getPLDofURL(url4); // -1367968407639347918
            String pld5 = IRLbotUtils.getPLDofURL(url5); //  -432095547552965690
            String pld6 = IRLbotUtils.getPLDofURL(url6); // -4898810143481738924
            String pld7 = IRLbotUtils.getPLDofURL(url7); //  -756481501653347580
            String pld8 = IRLbotUtils.getPLDofURL(url8); //  1818716199713354213

            Set<String> plds = new TreeSet<>();
            plds.add(pld1); // http://www.tuwien.ac.at<--http://www.winf.at (2100990832276391538<---5513532477925238510)
            plds.add(pld2); // http://www.univie.ac.at<--http://www.winf.at (2120302946100091416<---5513532477925238510)
            plds.add(pld3); // http://www.winf.at<--http://www.winf.at (-5513532477925238510<---5513532477925238510)
            this.star.update(pld3, plds);

            try
            {
                Thread.sleep(300);
            }
            catch (InterruptedException e)
            {
                LOG.catching(e);
            }

            plds = new TreeSet<>();
            plds.add(pld1); // http://www.tuwien.ac.at<--http://www.it-campus.at (2100990832276391538<--1818716199713354213)
            plds.add(pld2); // http://www.univie.ac.at<--http://www.it-campus.at (2120302946100091416<--1818716199713354213)
            plds.add(pld3); // http://www.winf.at<--http://www.it-campus.at (-5513532477925238510<--1818716199713354213)
            plds.add(pld4); // http://www.jku.at<--http://www.it-campus.at (-1367968407639347918<--1818716199713354213)
            plds.add(pld5); // http://www.technikum-wien.at<--http://www.it-campus.at (-432095547552965690<--1818716199713354213)
            plds.add(pld6); // http://www.wu.ac.at<--http://www.it-campus.at (-4898810143481738924<--1818716199713354213)
            plds.add(pld7); // http://www.fh-kufstein.ac.at<--http://www.it-campus.at (-756481501653347580<--1818716199713354213)
            plds.add(pld8); // http://www.it-campus.at<--http://www.it-campus.at (1818716199713354213<--1818716199713354213)
            this.star.update(pld8, plds);

            try
            {
                Thread.sleep(300);
            }
            catch (InterruptedException e)
            {
                LOG.catching(e);
            }

            plds = new TreeSet<>();
            plds.add(pld1); // http://www.tuwien.ac.at<--http://www.tuwien.ac.at (2100990832276391538<--2100990832276391538)
            plds.add(pld3); // http://www.winf.at<--http://www.tuwien.ac.at (-5513532477925238510<--2100990832276391538)
            plds.add(pld8); // http://www.it-campus.at<--http://www.tuwien.ac.at (1818716199713354213<--2100990832276391538)
            this.star.update(pld1, plds);

            try
            {
                Thread.sleep(300);
            }
            catch (InterruptedException e)
            {
                LOG.catching(e);
            }

            plds = new TreeSet<>();
            plds.add(pld1); // http://www.tuwien.ac.at<--http://www.winf.at (2100990832276391538<---5513532477925238510)
            plds.add(pld2); // http://www.univie.ac.at<--http://www.winf.at (2120302946100091416<---5513532477925238510)
            plds.add(pld3); // http://www.winf.at<--http://www.winf.at (-5513532477925238510<---5513532477925238510)
            plds.add(pld6); // http://www.wu.ac.at<--http://www.winf.at (-4898810143481738924<---5513532477925238510)
            this.star.update(pld3, plds);

            try
            {
                Thread.sleep(300);
            }
            catch (InterruptedException e)
            {
                LOG.catching(e);
            }

            plds = new TreeSet<>();
            plds.add(pld5); // http://www.technikum-wien.at<--http://www.technikum-wien.at (-432095547552965690<---432095547552965690)
            plds.add(pld8); // http://www.it-campus.at<--http://www.technikum-wien.at (1818716199713354213<---432095547552965690)
            this.star.update(pld5, plds);

            try
            {
                Thread.sleep(300);
            }
            catch (InterruptedException e)
            {
                LOG.catching(e);
            }

            // in-degree value:
            // tuwien: 3
            // univie: 2
            // winf: 3
            // jku: 1
            // technikum: 2
            // wu: 2
            // kufstein: 1
            // campus: 3

            this.star.check(url1);

            try
            {
                Thread.sleep(500);
            }
            catch (InterruptedException e)
            {
                LOG.catching(e);
            }

            Set<PLDData> pldData = this.star.getTopNSet();
            List<PLDData> pldList = new ArrayList<>();
            pldList.addAll(pldData);
            // we only have 8 entries so in case more entries should be selected
            assertEquals(Math.min(8, topN), pldList.size());
            // test in-degree levels
            if (topN > 0)
            {
                assertEquals(3, pldList.get(0).getIndegree());
            }
            if (topN > 1)
            {
                assertEquals(3, pldList.get(1).getIndegree());
            }
            if (topN > 2)
            {
                assertEquals(3, pldList.get(2).getIndegree());
            }
            if (topN > 3)
            {
                assertEquals(2, pldList.get(3).getIndegree());
            }
            if (topN > 4)
            {
                assertEquals(2, pldList.get(4).getIndegree());
            }
            if (topN > 5)
            {
                assertEquals(2, pldList.get(5).getIndegree());
            }
            if (topN > 6)
            {
                assertEquals(1, pldList.get(6).getIndegree());
            }
            if (topN > 7)
            {
                assertEquals(1, pldList.get(7).getIndegree());
            }

            // test budgets
            if (topN > 0)
            {
                assertEquals(50, pldList.get(0).getBudget());
            }
            if (topN > 1)
            {
                assertEquals(50, pldList.get(1).getBudget());
            }
            if (topN > 2)
            {
                assertEquals(50, pldList.get(2).getBudget());
            }
            if (topN > 3)
            {
                assertEquals(47, pldList.get(3).getBudget());
            }
            if (topN > 4)
            {
                assertEquals(47, pldList.get(4).getBudget());
            }
            if (topN > 5)
            {
                assertEquals(47, pldList.get(5).getBudget());
            }
            if (topN > 6)
            {
                assertEquals(44, pldList.get(6).getBudget());
            }
            if (topN > 7)
            {
                assertEquals(44, pldList.get(7).getBudget());
            }

            // test PLD names
            PLDData test;
            if (topN > 0)
            {
                test = pldList.get(0);
                Assert.assertTrue(
                        pld1.equals(test.getPLD()) || pld3.equals(test.getPLD()) || pld8.equals(test.getPLD()));
            }
            if (topN > 1)
            {
                test = pldList.get(1);
                Assert.assertTrue(
                        pld1.equals(test.getPLD()) || pld3.equals(test.getPLD()) || pld8.equals(test.getPLD()));
            }
            if (topN > 2)
            {
                test = pldList.get(2);
                Assert.assertTrue(
                        pld1.equals(test.getPLD()) || pld3.equals(test.getPLD()) || pld8.equals(test.getPLD()));
            }

            if (topN > 3)
            {
                test = pldList.get(3);
                Assert.assertTrue(
                        pld2.equals(test.getPLD()) || pld5.equals(test.getPLD()) || pld6.equals(test.getPLD()));
            }
            if (topN > 4)
            {
                test = pldList.get(4);
                Assert.assertTrue(
                        pld2.equals(test.getPLD()) || pld5.equals(test.getPLD()) || pld6.equals(test.getPLD()));
            }
            if (topN > 5)
            {
                test = pldList.get(5);
                Assert.assertTrue(
                        pld2.equals(test.getPLD()) || pld5.equals(test.getPLD()) || pld6.equals(test.getPLD()));
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

            Assert.assertEquals(url1, this.checkReturnURL); // tuwien.ac.at - 3
            Assert.assertEquals(50, this.checkReturnBudget);

            LOG.info("URL: {}; Budget: {}", this.checkReturnURL, this.checkReturnBudget);

            // entry is cached for top 10 and top 6 sets but not for top 3!
            this.star.check(url5); // technikum-wien.at - 2

            // values not cached in STAR's topSet have to be checked in DRUM
            // which requires the query to be merged with the backing data store
            // disposing star lead to unmerged data to get merged with the backing
            // data store, however - any further queries will not be processed
            // any further
            if (topN > 6)
            {
                LOG.info("URL: {}; Budget: {}", this.checkReturnURL, this.checkReturnBudget);

                Assert.assertEquals(url5, this.checkReturnURL);
                Assert.assertEquals(47, this.checkReturnBudget);

                // Entries is cached, so results should be available instantly
                this.star.check(url4);

                LOG.info("URL: {}; Budget: {}", this.checkReturnURL, this.checkReturnBudget);
                Assert.assertEquals(url4, this.checkReturnURL);
                Assert.assertEquals(44, this.checkReturnBudget);
            }
            else if (topN > 3)
            {
                LOG.info("URL: {}; Budget: {}", this.checkReturnURL, this.checkReturnBudget);

                Assert.assertEquals(url5, this.checkReturnURL);
                Assert.assertEquals(47, this.checkReturnBudget);

                // Entries will have to wait for the merge phase before a budget is returned
                this.star.check(url4);
                this.star.dispose();

                LOG.info("URL: {}; Budget: {}", this.checkReturnURL, this.checkReturnBudget);
                Assert.assertNotSame(url4, this.checkReturnURL);
                Assert.assertEquals(10, this.checkReturnBudget);
            }
            else
            {
                this.star.dispose();
                Assert.assertNotSame(url5, this.checkReturnURL);
                Assert.assertEquals(10, this.checkReturnBudget);
            }
        }
        catch (DrumException e)
        {
            LOG.catching(e);
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
                    if (LOG.isErrorEnabled())
                    {
                        LOG.error(e.getMessage());
                    }
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

    @Override
    public void cleanDataDir()
    {
        if (this.star != null && !this.closed)
        {
            try
            {
                this.star.dispose();
            }
            catch (DrumException e)
            {
                LOG.catching(e);
            }
        }
    }
}
