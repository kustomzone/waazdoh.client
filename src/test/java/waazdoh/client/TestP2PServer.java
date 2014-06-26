package waazdoh.client;

import org.xml.sax.SAXException;

import waazdoh.client.model.Binary;
import waazdoh.client.model.MBinaryID;
import waazdoh.cp2p.P2PServer;
import waazdoh.cp2p.common.MHost;
import waazdoh.cp2p.network.Node;
import waazdoh.cp2p.network.SourceListener;
import waazdoh.testing.ServiceMock;
import waazdoh.testing.StaticTestPreferences;
import waazdoh.testing.TestPBinarySource;
import waazdoh.util.ConditionWaiter;
import waazdoh.util.MPreferences;
import waazdoh.util.MTimedFlag;

public class TestP2PServer extends WCTestCase {

	public void testStartAndStop() {
		P2PServer s = getServer();
		s.start();
		assertTrue(s.isRunning());
		//
		s.close();
		assertFalse(s.isRunning());
	}

	public void testAddSelfAsNode() {
		P2PServer s = getServer();
		assertNotNull(s);
		s.start();
		//
		Node n = s.addNode(new MHost("localhost"), s.getPort());
		assertNotNull(n);
		assertFalse(n.isConnected());
		assertNull(n.getID());
		// the node gets and id, but it's the same id with server
		new ConditionWaiter(() -> n.getID() != null, 20000);
		assertNotNull(n.getID());
		// node gets disconnected
		new ConditionWaiter(() -> !n.isConnected(), 1000);
		assertFalse(n.isConnected());
		// and finally node is removed
		new ConditionWaiter(() -> s.getNode(n.getID()) == null, 1000);
		assertNull(s.getNode(n.getID()));
	}

	public void testNodeListener() {
		P2PServer s = getServer();
		MTimedFlag t = new MTimedFlag(10000);
		MTimedFlag doneflag = new MTimedFlag(10000);
		//
		s.addSourceListener(new SourceListener() {

			@Override
			public void nodeAdded(Node n) {
				t.trigger();
			}

			@Override
			public boolean isDone() {
				doneflag.trigger();
				return true;
			}
		});
		s.addNode(new MHost("localhost"), 1000000000);
		assertTrue(t.isTriggered());
		// sourcelisteners are removed if they are "done" when clearing memory
		s.clearMemory(0);
		assertTrue(doneflag.isTriggered());
	}

	public void testCanDownload() throws SAXException {
		P2PServer s = getServer();
		assertTrue(s.canDownload());
		ServiceMock service = new ServiceMock("test", new TestPBinarySource(
				s.getPreferences()));
		for (int i = 0; i < MPreferences.NETWORK_MAX_DOWNLOADS_DEFAULT; i++) {
			assertTrue(s.canDownload());
			MBinaryID id = new MBinaryID();
			s.addDownload(new Binary(id, service));
			assertNotNull(s.getDownload(id));
		}
		//
		assertFalse(s.canDownload());
	}

	public void testTwoNodes() {
		P2PServer servera = getServer();
		servera.start();
		P2PServer serverb = new P2PServer(new StaticTestPreferences(
				"otherserver", "otherserver"), true, null);
		serverb.start();
		try {
			Node n = serverb.addNode(new MHost("localhost"), servera.getPort());
			new ConditionWaiter(() -> n.isConnected(), 20000);
			assertNotNull(n.getID());
			assertTrue(n.getReceivedMessages() > 0);
			//
		} finally {
			servera.close();
			serverb.close();
		}
	}

	private P2PServer getServer() {
		P2PServer s = new P2PServer(getPreferences("p2pservertests"), true,
				null);
		return s;
	}
}
