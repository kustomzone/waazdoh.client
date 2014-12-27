package waazdoh.client;

import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;

import org.xml.sax.SAXException;

import waazdoh.client.model.JBean;
import waazdoh.client.model.MID;
import waazdoh.client.model.WaazdohInfo;
import waazdoh.util.xml.XML;

public class TestHash extends WCTestCase {

	ServiceObject o;

	public void testBeanHash() throws SAXException, IOException {
		JBean abean = new JBean("test");
		abean.addValue("testvalue", false);
		String ahash = abean.getContentHash();
		
		JBean bbean = new JBean(new XML(new StringReader(abean.toXML()
				.toString())));
		
		String bhash = bbean.getContentHash();

		assertEquals(ahash, bhash);
	}

	public void testServiceObjectHash() throws SAXException,
			MalformedURLException {
		ServiceObjectData so = new ServiceObjectData() {
			@Override
			public boolean parseBean(JBean bean) {
				// FAILS
				return false;
			}

			@Override
			public JBean getBean() {
				JBean b = o.getBean();
				return b;
			}
		};

		WClient c = getClient(getRandomUserName(), false);
		o = new ServiceObject("test", c, so, WaazdohInfo.version, "WAAZDOHTEST");

		MID id = o.getID();
		assertNotNull(id);

		String ahash = o.getHash();
		assertTrue(ahash.length() > 30);

		o.modified();

		String bhash = o.getHash();
		assertNotSame(ahash, bhash);
	}
}
