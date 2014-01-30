package waazdoh.client;

import junit.framework.TestCase;

import org.junit.Test;
import org.xml.sax.SAXException;

import waazdoh.cp2p.impl.P2PBinarySource;
import waazdoh.cutils.MPreferences;
import waazdoh.cutils.MStringID;
import waazdoh.testing.ServiceMock;

public final class TestiBinaryTransfer extends TestCase {

	public void testTransfer10k() throws SAXException {
		testTransfer(10000);
	}

	public void testTransfer30k() throws SAXException {
		testTransfer(30000);
	}

	public void testTransfer75k() throws SAXException {
		testTransfer(75000);
	}

	public void testTransfer100k() throws SAXException {
		testTransfer(100000);
	}

	public void testTransfer300k() throws SAXException {
		testTransfer(300000);
	}

	public void testTransfer1M() throws SAXException {
		testTransfer(1000000);
	}

	/*
	 * public void testTransfer4M() { testTransfer(4000000); }
	 */

	@Test
	public void testTransfer(int time) throws SAXException {

		String username1 = "test1" + Math.random();

		P2PBinarySource source1 = getServiceSource(username1, true);
		Binary b1 = source1.newBinary("test", "bin");
		assertNotNull(b1);
		//
		for (int i = 0; i < time; i++) {
			b1.add((byte) (i & 0xff));
		}
		//
		b1.setReady();
		b1.publish();
		String b1hasht = b1.getHash();
		//
		source1.clearMemory(0);
		Binary b1reload = source1.getOrDownload(b1.getID());
		assertNotNull(b1reload);
		assertEquals(b1hasht, b1reload.getHash());

		//
		String username2 = "test2" + Math.random();
		P2PBinarySource source2 = getServiceSource(username2, false);
		//
		Binary b2 = source2.getOrDownload(b1.getID());
		assertNotNull(b2);
		//
		long st = System.currentTimeMillis();
		while (!b2.isReady() && (System.currentTimeMillis() - st) < 300000) {
			doWait(100);
		}

		assertTrue(b2.isReady());
		assertTrue(b2.isOK());
		//
		assertEquals(b1, b2);
		//
		source1.close();
		source2.close();
	}

	private P2PBinarySource getServiceSource(String username1, boolean bind)
			throws SAXException {
		MPreferences p1 = new StaticTestPreferences(username1);
		P2PBinarySource source1 = new P2PBinarySource(p1, bind);
		ServiceMock service1 = new ServiceMock(source1);
		source1.setService(service1);
		service1.setSession(username1, "" + new MStringID());
		return source1;
	}

	private synchronized void doWait(int i) {
		try {
			this.wait(i);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
