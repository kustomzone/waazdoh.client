package waazdoh.client;

import junit.framework.TestCase;
import waazdoh.client.impl.FileBeanStorage;
import waazdoh.client.model.WData;
import waazdoh.testing.StaticTestPreferences;
import waazdoh.util.MStringID;

public class TestFileBeanStorage extends TestCase {

	public void testIterator() {
		FileBeanStorage s = new FileBeanStorage(new StaticTestPreferences(
				"test" + getClass(), "test" + System.currentTimeMillis()));
		MStringID madeupid = new MStringID("" + System.currentTimeMillis());

		String beanname = "test" + madeupid;
		s.addBean(madeupid, new WData(beanname));
		Iterable<MStringID> i = s.getLocalSetIDs("2"); // TODO what about next
														// millenium?
		assertTrue(i.iterator().hasNext());
		MStringID id = i.iterator().next();
		assertNotNull(id);
		assertEquals(madeupid, id);
		//
		WData b = s.getBean(id);
		assertEquals(beanname, b.getName());
	}
}
