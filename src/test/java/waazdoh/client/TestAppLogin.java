package waazdoh.client;

import java.net.MalformedURLException;

import org.xml.sax.SAXException;

import waazdoh.common.vo.AppLoginVO;

public class TestAppLogin extends WCTestCase {
	public void testGetUser() throws MalformedURLException, SAXException {
		WClient c = getClient(getRandomUserName(), false);
		AppLoginVO applogin = c.requestAppLogin();
		assertNotNull(applogin);

		AppLoginVO c2 = c.checkAppLogin(applogin.getId());
		assertNotNull(c2.getSessionid());

		assertTrue(c.isLoggedIn());
		assertTrue(c.isRunning());
	}
}
