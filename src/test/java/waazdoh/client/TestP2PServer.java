package waazdoh.client;

import java.net.MalformedURLException;

import org.xml.sax.SAXException;

import waazdoh.client.model.BinaryID;
import waazdoh.client.model.objects.Binary;
import waazdoh.client.utils.ConditionWaiter;
import waazdoh.client.utils.ConditionWaiter.Condition;
import waazdoh.common.MStringID;
import waazdoh.common.MTimedFlag;
import waazdoh.common.WLogger;
import waazdoh.common.WPreferences;
import waazdoh.common.client.ServiceClient;
import waazdoh.cp2p.P2PServer;
import waazdoh.cp2p.P2PServerImpl;
import waazdoh.cp2p.common.MHost;
import waazdoh.cp2p.messaging.MMessage;
import waazdoh.cp2p.messaging.SimpleMessageHandler;
import waazdoh.cp2p.network.ServerListener;
import waazdoh.cp2p.network.WNode;
import waazdoh.testing.StaticService;
import waazdoh.testing.StaticTestPreferences;

public class TestP2PServer extends WCTestCase {

	private P2PServer serverb;
	private P2PServer servera;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		servera = null;
		serverb = null;
		WLogger.resetStartTime();
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
		final WNode n = s.addNode(new MHost("localhost"), s.getPort());
		assertNotNull(n);
		assertFalse(n.isClosed());
		assertFalse(n.isConnected());
		assertNull(n.getID());

		// the node gets closed
		new ConditionWaiter(new Condition() {
			public boolean test() {
				return n.isClosed();
			}
		}, getWaitTime());

		assertTrue(n.isClosed());
		assertNull(n.getID());
		// node gets removed
		new ConditionWaiter(new Condition() {
			public boolean test() {
				return s.getNodeStatus(n) == null;
			}
		}, getWaitTime());

		assertNull(s.getNodeStatus(n));
	}

	public void testNodeListener() {
		P2PServer s = getServer();
		final MTimedFlag t = new MTimedFlag(10000);
		final MTimedFlag doneflag = new MTimedFlag(10000);
		s.addServerListener(new ServerListener() {

			@Override
			public void nodeAdded(WNode n) {
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
		s.clearMemory();
		assertTrue(doneflag.isTriggered());
	}

	public void testCanDownload() throws SAXException, MalformedURLException {
		P2PServer s = getServer();
		assertTrue(s.canDownload());
		ServiceClient service = new StaticService(getRandomUserName());

		BinaryID downloadid = null;
		for (int i = 0; i < WPreferences.NETWORK_MAX_DOWNLOADS_DEFAULT; i++) {
			assertTrue(s.canDownload());
			BinaryID id = new BinaryID();
			Binary b = new Binary(getClient(getRandomUserName(), false),
					getTempPath(), "", "");
			b.load(id);
			s.addDownload(b);
			downloadid = id;
			assertNotNull(s.getDownload(id));
		}
		assertFalse(s.canDownload());
		//
		s.removeDownload(downloadid);
		assertTrue(s.canDownload());
	}

	private String getTempPath() {
		String tempDir = System.getProperty("java.io.tmpdir");
		return tempDir;
	}

	public void testTwoNodes() {
		log.info("time servera " + System.currentTimeMillis());
		final P2PServer servera = getServer();
		log.info("time serverb " + System.currentTimeMillis());
		final P2PServer serverb = getOtherServerNoBind();
		log.info("time getting servers done");
		try {
			log.info("time addnode " + System.currentTimeMillis());
			// final Node n = serverb.addNode(new MHost("localhost"),
			// servera.getPort());
			log.info("time waiting");
			new ConditionWaiter(new Condition() {
				public boolean test() {
					// connected to some node
					return serverb.isConnected() && servera.isConnected();
				}
			}, getWaitTime());

			log.info("time check node " + System.currentTimeMillis());
			assertTrue(servera.isConnected());
			assertTrue(serverb.isConnected());

			assertNotNull(servera.getID());
			assertNotNull(serverb.getID());

			assertNotNull(serverb.getNode(servera.getID()));
			assertNotNull(servera.getNode(serverb.getID()));
		} finally {
			log.info("time closing " + System.currentTimeMillis());
			log.info("time closing");
		}
	}

	private int getWaitTime() {
		if (System.getProperty("extended.debug") != null) {
			return 360000;
		} else {
			return 20000;
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
		// final WNode n = serverb.addNode(new MHost("localhost"),
		// servera.getPort());
		log.info("waiting connection");

		new ConditionWaiter(new Condition() {

			@Override
			public boolean test() {
				return servera.isConnected() && serverb.isConnected();
			}
		}, getWaitTime());

		assertTrue(servera.isConnected());
		assertTrue(serverb.isConnected());

		log.info("servers connected");
	}

	public void testBroadcast() {
		createTwoServers();

		servera.getMessenger().addMessageHandler("testmessage",
				new SimpleMessageHandler() {

					@Override
					public MMessage handle(MMessage childb) {
						setValue("testbroadcast", "" + childb);
						return null;
					}
				});

		log.info("Broadcasting " + serverb + " testmessage");
		serverb.getMessenger().broadcastMessage(
				serverb.getMessenger().getMessage("testmessage"));
		//
		waitForValue("testbroadcast", getWaitTime());
		assertValue("testbroadcast");
	}

	public void testPingPong() {
		createTwoServers();
		class Count {
			int count = 0;
		}
		final Count c = new Count();
		final int maxcount = 20;

		servera.getMessenger().addMessageHandler("pingpong",
				new SimpleMessageHandler() {

					@Override
					public MMessage handle(MMessage childb) {
						if (c.count++ < maxcount) {
							return servera.getMessenger()
									.getMessage("pingpong");
						} else {
							return null;
						}
					}
				});

		serverb.getMessenger().addMessageHandler("pingpong",
				new SimpleMessageHandler() {

					@Override
					public MMessage handle(MMessage childb) {
						if (c.count++ < maxcount) {
							return serverb.getMessenger()
									.getMessage("pingpong");
						} else {
							return null;
						}
					}
				});

		servera.getMessenger().broadcastMessage(
				servera.getMessenger().getMessage("pingpong"));

		new ConditionWaiter(new Condition() {
			public boolean test() {
				return c.count >= maxcount;
			}
		}, getWaitTime());

		assertTrue("count " + c.count, c.count >= maxcount);
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
		P2PServer s = new P2PServerImpl(new StaticTestPreferences(
				"otherserver", "otherserver"), true);
		s.start();
		log.info("returning " + s);
		return s;
	}

}
