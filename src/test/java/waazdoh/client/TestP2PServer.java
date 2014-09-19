package waazdoh.client;

import org.xml.sax.SAXException;

import waazdoh.client.binaries.ReportingService;
import waazdoh.client.model.Binary;
import waazdoh.client.model.MBinaryID;
import waazdoh.cp2p.P2PServer;
import waazdoh.cp2p.common.MHost;
import waazdoh.cp2p.messaging.MMessage;
import waazdoh.cp2p.messaging.SimpleMessageHandler;
import waazdoh.cp2p.network.Node;
import waazdoh.cp2p.network.SourceListener;
import waazdoh.testing.ServiceMock;
import waazdoh.testing.StaticTestPreferences;
import waazdoh.testing.TestPBinarySource;
import waazdoh.util.ConditionWaiter;
import waazdoh.util.ConditionWaiter.Condition;
import waazdoh.util.MPreferences;
import waazdoh.util.MStringID;
import waazdoh.util.MTimedFlag;

public class TestP2PServer extends WCTestCase {

	private P2PServer serverb;
	private P2PServer servera;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		servera = null;
		serverb = null;
	}

	public void testStartAndStop() {
		P2PServer s = getServer();
		assertTrue(s.isRunning());
		s.close();
		assertFalse(s.isRunning());
	}

	public void testAddSelfAsNode() {
		final P2PServer s = getServer();
		assertNotNull(s);
		final Node n = s.addNode(new MHost("localhost"), s.getPort());
		assertNotNull(n);
		assertFalse(n.isConnected());
		assertNull(n.getID());
		// the node gets and id, but it's the same id with server
		new ConditionWaiter(new Condition() {
			public boolean test() {
				return n.getID() != null;
			}
		}, 20000);

		assertNotNull(n.getID());
		// node gets disconnected
		new ConditionWaiter(new Condition() {
			public boolean test() {
				return !n.isConnected();
			}
		}, 10000);

		assertFalse(n.isConnected());
		// and finally node is removed
		new ConditionWaiter(new Condition() {
			public boolean test() {
				return s.getNode(n.getID()) == null;
			}
		}, 40000);
		assertNull(s.getNode(n.getID()));
	}

	public void testNodeListener() {
		P2PServer s = getServer();
		final MTimedFlag t = new MTimedFlag(10000);
		final MTimedFlag doneflag = new MTimedFlag(10000);
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
		s.addNode(new MHost("localhost"), 20000);
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

		MBinaryID downloadid = null;
		for (int i = 0; i < MPreferences.NETWORK_MAX_DOWNLOADS_DEFAULT; i++) {
			assertTrue(s.canDownload());
			MBinaryID id = new MBinaryID();
			s.addDownload(new Binary(id, service));
			downloadid = id;
			assertNotNull(s.getDownload(id));
		}
		assertFalse(s.canDownload());
		//
		s.removeDownload(downloadid);
		assertTrue(s.canDownload());
	}

	public void testTwoNodes() {
		P2PServer servera = getServer();
		P2PServer serverb = getOtherServer();
		log.info("getting servers done");
		try {
			final Node n = serverb.addNode(new MHost("localhost"),
					servera.getPort());
			log.info("waiting");
			new ConditionWaiter(new Condition() {
				public boolean test() {
					return n.isConnected();
				}
			}, 20000);

			assertNotNull(n.getID());
			assertTrue(n.getReceivedMessages() > 0);
		} finally {
			log.info("closing");
			servera.close();
			serverb.close();
		}
	}

	public void testABConnection() {
		try {
			createTwoServers();
		} finally {
			log.info("closing");
			servera.close();
			serverb.close();
		}
	}

	private void createTwoServers() {
		servera = getServer();
		serverb = getOtherServerNoBind();
		log.info("getting servers done");
		final Node n = serverb.addNode(new MHost("localhost"),
				servera.getPort());
		log.info("waiting");

		new ConditionWaiter(new Condition() {

			@Override
			public boolean test() {
				return n.isConnected();
			}
		}, 20000);

		assertNotNull(n.getID());
		assertTrue(n.getReceivedMessages() > 0);
	}

	public void testBroadcast() {
		createTwoServers();

		servera.addMessageHandler("test", new SimpleMessageHandler() {

			@Override
			public MMessage handle(MMessage childb) {
				setValue("testbroadcast", "" + childb);
				return null;
			}
		});
		serverb.broadcastMessage(serverb.getMessage("test"));
		//
		waitForValue("testbroadcast", 10000);
		assertValue("testbroadcast");
	}

	public void testReporting() {
		P2PServer a = getServer();
		final MTimedFlag f = new MTimedFlag(Integer.MAX_VALUE);
		a.setReportingService(new ReportingService() {
			@Override
			public void reportDownload(MStringID id, boolean success) {
				f.trigger();
			}
		});

		a.reportDownload(new MStringID(), false);
		new ConditionWaiter(new Condition() {

			@Override
			public boolean test() {
				return f.isTriggered();
			}
		}, 1000);

		assertTrue(f.isTriggered());
	}

	private P2PServer getOtherServer() {
		P2PServer s = new P2PServer(new StaticTestPreferences("otherserver",
				"otherserver"), true, null);
		s.start();
		log.info("returning " + s);
		return s;
	}

}
