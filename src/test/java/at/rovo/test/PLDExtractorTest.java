package at.rovo.test;

import org.junit.Assert;
import org.junit.Test;
import at.rovo.crawler.util.IRLbotUtil;

public class PLDExtractorTest
{	
	@Test
	public void testPLDExtraction()
	{
		Assert.assertEquals("tuwien.ac.at", IRLbotUtil.getPLDofURL("https://tiss.tuwien.ac.at/?locale=en"));
		Assert.assertEquals("tuwien.ac.at", IRLbotUtil.getPLDofURL("https://tiss.tuwien.ac.at/main?locale=en"));
		Assert.assertEquals("tuwien.ac.at", IRLbotUtil.getPLDofURL("https://tiss.tuwien.ac.at/adressbuch/adressbuch?locale=en"));
		Assert.assertEquals("tuwien.ac.at", IRLbotUtil.getPLDofURL("https://tiss.tuwien.ac.at/hilfe"));
		Assert.assertEquals("tuwien.ac.at", IRLbotUtil.getPLDofURL("https://tiss.tuwien.ac.at/hilfe/zu/tiss/support"));
		Assert.assertEquals("tuwien.ac.at", IRLbotUtil.getPLDofURL("https://tiss.tuwien.ac.at/admin/authentifizierung"));
		Assert.assertEquals("tuwien.ac.at", IRLbotUtil.getPLDofURL("https://tiss.tuwien.ac.at/adressbuch/adressbuch"));
		Assert.assertEquals("tuwien.ac.at", IRLbotUtil.getPLDofURL("https://tiss.tuwien.ac.at/mbl/main?fuer=mb"));
		Assert.assertEquals("tuwien.ac.at", IRLbotUtil.getPLDofURL("https://tiss.tuwien.ac.at/student/self_service"));
		Assert.assertEquals("tuwien.ac.at", IRLbotUtil.getPLDofURL("https://tiss.tuwien.ac.at/hilfe/zu/tiss/support"));
		Assert.assertEquals("tuwien.ac.at", IRLbotUtil.getPLDofURL("https://tiss.tuwien.ac.at/ueber_tiss/impressum"));
		Assert.assertEquals("tuwien.ac.at", IRLbotUtil.getPLDofURL("http://www.zid.tuwien.ac.at/kom/services/mail/konzept/mail_adressierung/#c2077/"));
		Assert.assertEquals("tuwien.ac.at", IRLbotUtil.getPLDofURL("http://igw.tuwien.ac.at/"));
		Assert.assertEquals("tuwien.ac.at", IRLbotUtil.getPLDofURL("http://www.zid.tuwien.ac.at/zidnews/news_detail/archive/2013/january/article/campussw-mathtype-67e-fuer-mac-en/?tx_ttnews%5Bday%5D=10&amp;cHash=df7baf8166edd284afc87f35753d9673"));
		Assert.assertEquals("univie.ac.at", IRLbotUtil.getPLDofURL("http://www.univie.ac.at/universitaet/universitaet/zahlen-und-fakten/"));
		Assert.assertEquals("winf.at", IRLbotUtil.getPLDofURL("http://winf.at/files/Anrechnungen_Detail.pdf"));
	}
		
	@Test
	public void testCheckAndTransformURL()
	{
		Assert.assertEquals("https://tiss.tuwien.ac.at/?locale=en", IRLbotUtil.checkAndTransformURL("/?locale=en", "https://tiss.tuwien.ac.at"));
		Assert.assertEquals("http://www.oeh.univie.ac.at/?id=93", IRLbotUtil.checkAndTransformURL("../?id=93", "http://www.oeh.univie.ac.at"));
		Assert.assertEquals("https://tiss.tuwien.ac.at/mbl/main?fuer=mb", IRLbotUtil.checkAndTransformURL("mbl/main?fuer=mb", "https://tiss.tuwien.ac.at"));
		Assert.assertEquals("http://www.test.at/test/dir2", IRLbotUtil.checkAndTransformURL("../../dir2", "http://www.test.at/test/dir1/subdir/someFile"));
		Assert.assertEquals("http://www.test.at/test/dir2", IRLbotUtil.checkAndTransformURL("../../dir2", "http://www.test.at/test/dir1/subdir/"));
	}
}
