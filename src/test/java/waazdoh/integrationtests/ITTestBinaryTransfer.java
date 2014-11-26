package waazdoh.integrationtests;

import java.io.IOException;

import org.junit.Test;
import org.xml.sax.SAXException;

import waazdoh.client.WCTestCase;
import waazdoh.client.model.Binary;
import waazdoh.cp2p.P2PBinarySource;
import waazdoh.testing.StaticTestPreferences;
import waazdoh.util.MLogger;

public final class ITTestBinaryTransfer extends WCTestCase {
	private MLogger log = MLogger.getLogger(this);

	public void testTransfer10kAB() throws SAXException, InterruptedException,
			IOException {
		testTransfer(10000, true, false);
	}

	public void testTransfer10kBA() throws SAXException, InterruptedException,
			IOException {
		testTransfer(10000, false, true);
	}

	public void testTransfer10k() throws SAXException, InterruptedException,
			IOException {
		testTransfer(10000);
	}

	public void testTransfer30k() throws SAXException, InterruptedException,
			IOException {
		testTransfer(30000);
	}

	public void testTransfer75k() throws SAXException, InterruptedException,
			IOException {
		testTransfer(75000);
	}

	public void testTransfer100k() throws SAXException, InterruptedException,
			IOException {
		testTransfer(100000);
	}

	public void testTransfer300k() throws SAXException, InterruptedException,
			IOException {
		testTransfer(300000);
	}

	public void testTransfer1M() throws SAXException, InterruptedException,
			IOException {
		testTransfer(1000000);
	}

	public void testTransfer5M() throws SAXException, InterruptedException,
			IOException {
		testTransfer(5000000);
	}

	private void testTransfer(int i) throws SAXException, InterruptedException,
			IOException {
		testTransfer(i, true, false);
	}

	@Test
	public void testTransfer(int binarysize, boolean bind1, boolean bind2)
			throws SAXException, InterruptedException, IOException {
		log.info("test transferm " + binarysize);
		String username1 = "test1" + Math.random();
		log.info("service1 with " + username1);
		P2PBinarySource source1 = getServiceSource(username1, bind1);

		log.info("creating binary");
		Binary b1 = source1.newBinary("test", "bin");
		assertNotNull(b1);
		log.info("adding bytes");
		for (int i = 0; i < binarysize; i++) {
			b1.add((byte) (i & 0xff));
		}
		log.info("publishing " + b1);
		b1.setReady();
		b1.publish();
		assertEquals(binarysize, b1.length());
		String b1hasht = b1.getHash();

		log.info("getOrDownload source1");
		Binary b1reload = source1.getOrDownload(b1.getID());
		assertNotNull(b1reload);
		assertEquals(b1.getBean().toText(), b1reload.getBean().toText());
		assertEquals(b1hasht, b1reload.getHash());

		// 2

		log.info("getOrDownload source2");
		String username2 = "test2" + Math.random();
		log.info("service2 with " + username2);
		P2PBinarySource source2 = getServiceSource(username2, bind2);
		log.info("wait service2 " + source2);
		source2.waitUntilReady();

		Binary b2 = source2.getOrDownload(b1.getID());
		assertNotNull(b2);
		log.info("wait until ready");

		long st = System.currentTimeMillis();
		while (!b2.isReady() && System.currentTimeMillis() - st < 60000) {
			doWait(100);
		}

		log.info("checks " + b2);
		assertTrue(b2.isReady());
		assertTrue(b2.isOK());
		assertEquals(b1, b2);
		log.info("closing");
		source1.startClosing();
		source2.startClosing();

		source1.close();
		source2.close();

	}

	private synchronized void doWait(int i) {
		try {
			this.wait(i);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void tearDown() throws Exception {
		StaticTestPreferences.clearPorts();
		super.tearDown();
	}
}
