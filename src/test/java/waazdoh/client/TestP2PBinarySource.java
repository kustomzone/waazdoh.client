package waazdoh.client;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;

import org.xml.sax.SAXException;

import waazdoh.client.model.objects.Binary;
import waazdoh.client.utils.ConditionWaiter;
import waazdoh.common.WLogger;

public class TestP2PBinarySource extends WCTestCase {

	private BinarySource serverb;
	private BinarySource servera;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		servera = null;
		serverb = null;
		WLogger.resetStartTime();
	}

	public void testSourcesConnected() throws MalformedURLException,
			SAXException {
		servera = getSource();
		serverb = getSource();

		servera.waitUntilReady();
		serverb.waitUntilReady();

		assertTrue(servera.isReady());
		assertTrue(servera.isRunning());
		assertTrue(serverb.isReady());
		assertTrue(serverb.isRunning());
	}

	private BinarySource getSource() throws MalformedURLException, SAXException {
		WClient c = getClient(getRandomUserName(), servera == null);
		return c.getBinarySource();
	}

	public void testGetOrDownload() throws SAXException, FileNotFoundException,
			IOException {
		servera = getSource();

		
		Binary binarya = servera.newBinary("comment", "bin");
		binarya.add(new byte[1000]);
		binarya.setReady();
		binarya.publish();

		serverb = getSource();
		final Binary binaryb = serverb.getOrDownload(binarya.getID());
		assertNotNull(binaryb);
		assertFalse(binaryb.isReady());

		serverb.waitUntilReady();

		new ConditionWaiter(new ConditionWaiter.Condition() {

			@Override
			public boolean test() {
				return binaryb.isReady();
			}
		}, getWaitTime());

		assertTrue(binaryb.isReady());
		assertEquals(1000, binaryb.length());
	}
}
