package waazdoh.client;

import java.net.MalformedURLException;

import org.xml.sax.SAXException;

import waazdoh.client.model.User;
import waazdoh.common.UserID;

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

	public void testFailGetUser() throws MalformedURLException, SAXException {
		WClient c = getClient(getRandomUserName(), false);
		assertNull(c.getUser(new UserID("FAIL")));
	}
}
