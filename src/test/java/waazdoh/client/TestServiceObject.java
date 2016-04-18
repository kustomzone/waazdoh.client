package waazdoh.client;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import org.xml.sax.SAXException;

import waazdoh.client.WClient.Filter;
import waazdoh.common.MStringID;
import waazdoh.common.MTimedFlag;
import waazdoh.common.ObjectID;
import waazdoh.common.WObject;
import waazdoh.common.WaazdohInfo;

public class TestServiceObject extends WCTestCase {

	private final class ServiceObjectDataImplementation implements ServiceObjectData {
		double d = Math.random();
		long time = System.currentTimeMillis();

		@Override
		public boolean parse(WObject bean) {
			d = bean.getDoubleValue("d");
			time = bean.getLongValue("time");
			return true;
		}

		@Override
		public WObject getObject() {
			WObject b = o.getBean();
			b.addValue("d", d);
			b.addValue("time", time);
			return b;
		}

		public void update() {
			d = Math.random();
		}
	}

	ServiceObject o;

	public void testModifiedListener() throws MalformedURLException, SAXException {
		ServiceObjectData data1 = new ServiceObjectDataImplementation();

		WClient c1 = getClient(getRandomUserName(), false);
		o = new ServiceObject("test", c1, data1, WaazdohInfo.VERSION, "WAAZDOHTEST");

		final Map<String, String> map = new HashMap<String, String>();

		o.addListener(new ServiceObjectListener() {

			@Override
			public void modified() {
				map.put("done", "true");
			}
		});

		o.save();
		o.publish();

		assertEquals(map.get("done"), "true");
	}

	public void testServiceObject() throws SAXException, MalformedURLException {
		ServiceObjectData data1 = new ServiceObjectDataImplementation();

		ObjectID id = createAndPublish(data1);
		assertNotNull(id);

		ServiceObjectDataImplementation data2 = new ServiceObjectDataImplementation();

		WClient c2 = getClient(getRandomUserName(), false);
		ServiceObject o1 = new ServiceObject("test", c2, data2, WaazdohInfo.VERSION, "WAAZDOHTEST");

		o1.load(id.getStringID());

		WObject o2data = data2.getObject();

		WObject o1data = data1.getObject();
		assertNull(o1data.getAttribute("id"));
		assertNull(o2data.getAttribute("id"));

		assertEquals(o1data, o2data);
		assertEquals(o1data.getContentHash(), o2data.getContentHash());

		String o1hash = o1.getHash();
		data2.update();

		o1.save();
		assertNotNull(o1.getCopyOf());
		assertFalse(o1.getHash().equals(o1hash));
	}

	public void testFilter() throws SAXException, MalformedURLException {
		ServiceObjectData data1 = new ServiceObjectDataImplementation();

		ObjectID id = createAndPublish(data1);
		assertNotNull(id);

		ServiceObjectDataImplementation data2 = new ServiceObjectDataImplementation();

		WClient c2 = getClient(getRandomUserName(), false);
		ServiceObject o1 = new ServiceObject("test", c2, data2, WaazdohInfo.VERSION, "WAAZDOHTEST");

		final MTimedFlag f = new MTimedFlag(getWaitTime());

		c2.addObjectFilter(new Filter() {
			int counter = 0;

			@Override
			public boolean check(WObject o) {
				f.trigger();
				return counter++ > 0;
			}
		});

		o1.load(id.getStringID());
		assertTrue(f.isTriggered());
		assertTrue(!data1.getObject().equals(data2.getObject()));

		o1.load(id.getStringID());
		assertTrue(f.isTriggered());

		WObject o2data = data2.getObject();

		WObject o1data = data1.getObject();
		assertNull(o1data.getAttribute("id"));
		assertNull(o2data.getAttribute("id"));

		assertTrue(data1.getObject().equals(data2.getObject()));
		assertEquals(o1data.getContentHash(), o2data.getContentHash());
	}

	private ObjectID createAndPublish(ServiceObjectData data1) throws MalformedURLException, SAXException {
		WClient c1 = getClient(getRandomUserName(), false);
		o = new ServiceObject("test", c1, data1, WaazdohInfo.VERSION, "WAAZDOHTEST");

		o.save();
		o.publish();

		ObjectID id = o.getID();
		return id;
	}

}
