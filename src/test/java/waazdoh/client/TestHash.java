package waazdoh.client;

import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;

import org.xml.sax.SAXException;

import waazdoh.common.ObjectID;
import waazdoh.common.WData;
import waazdoh.common.WObject;
import waazdoh.common.WaazdohInfo;
import waazdoh.common.XML;

public class TestHash extends WCTestCase {

	ServiceObject o;

	public void testBeanHash() throws SAXException, IOException {
		WData abean = new WData("test");
		abean.addValue("testvalue", false);
		String ahash = abean.getContentHash();

		WData bbean = new WData(new XML(new StringReader(abean.toXML()
				.toString())));

		String bhash = bbean.getContentHash();

		assertEquals(ahash, bhash);
	}

	public void testServiceObjectHash() throws SAXException,
			MalformedURLException {
		ServiceObjectData so = new ServiceObjectData() {

			@Override
			public boolean parse(WObject bean) {
				// FAILS
				return false;
			}

			@Override
			public WObject getObject() {
				WObject b = o.getBean();
				return b;
			}
		};

		WClient c = getClient(getRandomUserName(), false);
		o = new ServiceObject("test", c, so, WaazdohInfo.VERSION, "WAAZDOHTEST");

		ObjectID id = o.getID();
		assertNotNull(id);

		String ahash = o.getHash();
		assertTrue(ahash.length() > 30);

		o.modified();

		String bhash = o.getHash();
		assertNotSame(ahash, bhash);
	}
}
