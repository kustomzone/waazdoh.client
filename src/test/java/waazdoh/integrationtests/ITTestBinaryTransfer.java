package waazdoh.integrationtests;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.Test;
import org.xml.sax.SAXException;

import waazdoh.client.BinarySource;
import waazdoh.client.WCTestCase;
import waazdoh.client.WClient;
import waazdoh.client.model.objects.Binary;
import waazdoh.common.WLogger;
import waazdoh.testing.StaticTestPreferences;

public final class ITTestBinaryTransfer extends WCTestCase {
	private WLogger log = WLogger.getLogger(this);

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
		String username1 = "test1" + Math.random();
		String username2 = "test2" + Math.random();
		log.info("test transfer " + binarysize + " user1 + " + username1
				+ " user2 " + username2);

		WClient c1 = getClient(username1, bind1);
		WClient c2 = getClient(username2, bind2);

		c2.getBinarySource().waitUntilReady();

		testTransfer(binarysize, c1.getBinarySource(),
				c2.getBinarySource());

	}

	private void testTransfer(int binarysize, BinarySource source1,
			BinarySource source2) throws FileNotFoundException, IOException {
		log.info("creating binary");
		Binary b1 = source1.newBinary("test", "bin");
		assertNotNull(b1);
		log.info("adding bytes");
		byte bs[] = new byte[binarysize];
		for (int i = 0; i < binarysize; i++) {
			bs[i] = (byte) (i & 0xff);
		}
		b1.add(bs);

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

		Binary b2 = source2.getOrDownload(b1.getID());
		assertNotNull(b2);
		log.info("wait until ready");

		long st = System.currentTimeMillis();
		while (!b2.isReady() && System.currentTimeMillis() - st < 120000) {
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
			log.error(e);
		}
	}

	@Override
	protected void tearDown() throws Exception {
		StaticTestPreferences.clearPorts();
		super.tearDown();
	}
}
