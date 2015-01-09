package waazdoh.client;

import java.net.MalformedURLException;

import org.xml.sax.SAXException;

import waazdoh.client.model.User;

public class TestUser extends WCTestCase {
	public void testGetUser() throws MalformedURLException, SAXException {
		WClient c = getClient(getRandomUserName(), false);
		User user = c.getUser(c.getUserID());
		assertNotNull(user);
		assertNotNull(user.getName());
		//
		String name2 = getRandomUserName();
		WClient c1 = getClient(name2, false);
		assertEquals(user.getName(), c1.getUser(c.getUserID()).getName());
	}
}
