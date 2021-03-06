package waazdoh.client;

import waazdoh.client.storage.local.FileBeanStorage;
import waazdoh.common.MStringID;
import waazdoh.common.WObject;
import waazdoh.common.testing.StaticTestPreferences;

public class TestFileBeanStorage extends WCTestCase {

	public void testIterator() {
		FileBeanStorage s = new FileBeanStorage(new StaticTestPreferences(
				"2015test" + getClass(), "2015test"
						+ System.currentTimeMillis()));
		MStringID madeupid = new MStringID("" + System.currentTimeMillis());

		assertNull(s.getBean(madeupid));

		String beanname = "test" + madeupid;
		s.addObject(madeupid, new WObject(beanname));
		Iterable<MStringID> i = s.getLocalSetIDs("2"); // TODO what about next
														// millenium?
		assertTrue(i.iterator().hasNext());
		MStringID id = i.iterator().next();
		assertNotNull(id);
		assertEquals(madeupid, id);
		//
		WObject b = s.getBean(id);
		assertEquals(beanname, b.getType());
	}

}
