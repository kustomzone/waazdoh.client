package waazdoh.client;

import java.net.MalformedURLException;

import org.xml.sax.SAXException;

import waazdoh.common.ObjectID;
import waazdoh.common.WData;
import waazdoh.common.WaazdohInfo;

public class TestServiceObject extends WCTestCase {

	private final class ServiceObjectDataImplementation implements
			ServiceObjectData {
		double d = Math.random();
		long time = System.currentTimeMillis();

		@Override
		public boolean parseBean(WData bean) {
			d = bean.getDoubleValue("d");
			time = bean.getLongValue("time");
			return true;
		}

		@Override
		public WData getBean() {
			WData b = o.getBean();
			b.addValue("d", d);
			b.addValue("time", time);
			return b;
		}
	}

	ServiceObject o;

	public void testServiceObject() throws SAXException, MalformedURLException {
		ServiceObjectData data1 = new ServiceObjectDataImplementation();

		WClient c1 = getClient(getRandomUserName(), false);
		o = new ServiceObject("test", c1, data1, WaazdohInfo.version,
				"WAAZDOHTEST");
		o.save();
		o.publish();

		ObjectID id = o.getID();
		assertNotNull(id);

		ServiceObjectDataImplementation data2 = new ServiceObjectDataImplementation();

		WClient c2 = getClient(getRandomUserName(), false);
		ServiceObject o1 = new ServiceObject("test", c2, data2,
				WaazdohInfo.version, "WAAZDOHTEST");
		o1.load(id.getStringID());

		WData o2data = data2.getBean();

		WData o1data = data1.getBean();
		assertNull(o1data.getAttribute("id"));
		assertNull(o2data.getAttribute("id"));

		assertEquals(o1data, o2data);
		assertEquals(o1data.getContentHash(), o2data.getContentHash());
	}
}
