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
		return getSource(servera == null);
	}

	private BinarySource getSource(boolean bind) throws MalformedURLException,
			SAXException {
		WClient c = getClient(getRandomUserName(), bind);
		return c.getBinarySource();
	}

	public void testGetOrDownload() throws SAXException, FileNotFoundException,
			IOException {
		servera = getSource();
		getSource(true);

		Binary binarya = servera.newBinary("comment", "bin");
		addBinaryData(binarya);

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

	private void addBinaryData(Binary binarya) throws IOException {
		binarya.add(new byte[1000]);
		binarya.setReady();
		binarya.publish();
	}
}
