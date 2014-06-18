package waazdoh.client;

import junit.framework.TestCase;

import org.junit.Test;
import org.xml.sax.SAXException;

import waazdoh.cp2p.impl.P2PBinarySource;
import waazdoh.cutils.MLogger;
import waazdoh.cutils.MPreferences;
import waazdoh.cutils.MStringID;
import waazdoh.testing.ServiceMock;

public final class TestBinaryTransfer extends TestCase {
	private MLogger log = MLogger.getLogger(this);

	public void testTransfer10k() throws SAXException, InterruptedException {
		testTransfer(10000);
	}

	public void testTransfer30k() throws SAXException, InterruptedException {
		testTransfer(30000);
	}

	public void testTransfer75k() throws SAXException, InterruptedException {
		testTransfer(75000);
	}

	public void testTransfer100k() throws SAXException, InterruptedException {
		testTransfer(100000);
	}

	public void testTransfer300k() throws SAXException, InterruptedException {
		testTransfer(300000);
	}

	public void testTransfer1M() throws SAXException, InterruptedException {
		testTransfer(1000000);
	}

	/*
	 * public void testTransfer4M() { testTransfer(4000000); }
	 */

	@Test
	public void testTransfer(int binarysize) throws SAXException,
			InterruptedException {
		log.info("test transferm " + binarysize);
		//
		String username1 = "test1" + Math.random();
		log.info("service1 with " + username1);
		P2PBinarySource source1 = getServiceSource(username1, true);
		String username2 = "test2" + Math.random();
		log.info("service2 with " + username2);
		P2PBinarySource source2 = getServiceSource(username2, false);
		log.info("wait service2 " + source2);
		source2.waitUntilReady();

		log.info("creating binary");
		Binary b1 = source1.newBinary("test", "bin");
		assertNotNull(b1);
		//
		log.info("adding bytes");
		for (int i = 0; i < binarysize; i++) {
			b1.add((byte) (i & 0xff));
		}
		//
		log.info("publishing " + b1);
		b1.setReady();
		b1.publish();
		String b1hasht = b1.getHash();
		//
		source1.clearMemory(0);
		log.info("getOrDownload source1");
		Binary b1reload = source1.getOrDownload(b1.getID());
		assertNotNull(b1reload);
		assertEquals(b1hasht, b1reload.getHash());

		//
		log.info("getOrDownload source2");
		Binary b2 = source2.getOrDownload(b1.getID());
		assertNotNull(b2);
		//
		log.info("wait until ready");

		long st = System.currentTimeMillis();
		while (!b2.isReady() && (System.currentTimeMillis() - st) < 60000) {
			doWait(100);
		}

		log.info("checks " + b2);
		assertTrue(b2.isReady());
		assertTrue(b2.isOK());
		//
		assertEquals(b1, b2);
		//
		log.info("closing");
		source1.close();
		source2.close();
		//
		
	}

	private P2PBinarySource getServiceSource(final String username1,
			boolean bind) throws SAXException {
		MPreferences p1 = new StaticTestPreferences(username1);
		P2PBinarySource source1 = new P2PBinarySource(p1, bind);
		ServiceMock service1 = new ServiceMock(username1, source1);

		service1.setSession("" + new MStringID());
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
