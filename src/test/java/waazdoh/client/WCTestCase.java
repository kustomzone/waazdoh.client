package waazdoh.client;

import java.io.File;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.xml.sax.SAXException;

import junit.framework.TestCase;
import waazdoh.client.storage.local.FileBeanStorage;
import waazdoh.client.utils.ConditionWaiter;
import waazdoh.client.utils.ThreadChecker;
import waazdoh.common.WLogger;
import waazdoh.common.WPreferences;
import waazdoh.common.client.MemoryBeanStorage;
import waazdoh.common.testing.StaticService;
import waazdoh.common.testing.StaticTestPreferences;
import waazdoh.cp2p.P2PBinarySource;
import waazdoh.cp2p.P2PServer;
import waazdoh.cp2p.P2PServerImpl;
import waazdoh.cp2p.common.MHost;

public class WCTestCase extends TestCase {
	private static final String PREFERENCES_PREFIX = "wcclienttests";
	protected WLogger log = WLogger.getLogger(this);
	private Map<String, String> values = new HashMap<String, String>();
	private Set<P2PServer> servers = new HashSet<>();

	private int usernamecounter = 0;

	protected void setUp() throws Exception {
		log.info("************************ STARTING A TEST " + this + " " + this.getName() + " ********");
		values.clear();
		servers.clear();
		StaticTestPreferences.clearPorts();
	};

	protected void tearDown() throws Exception {
		log.info("************************* CLOSING " + this + " " + this.getName() + " ********");

		// startThreadChecker();

		Set<P2PServer> ss = this.servers;
		for (P2PServer p2pServer : ss) {
			p2pServer.startClosing();
		}

		for (P2PServer p2pServer : ss) {
			p2pServer.close();
		}

		boolean nettyfound;
		do {
			nettyfound = false;
			Map<Thread, StackTraceElement[]> sts = Thread.getAllStackTraces();
			for (Thread t : sts.keySet()) {
				StackTraceElement[] st = sts.get(t);
				for (StackTraceElement stackTraceElement : st) {
					// log.info("Thread " + t + " ST:" + stackTraceElement);

					if (("" + st).indexOf("netty") > 0) {
						nettyfound = true;
						break;
					}
				}
			}

			if (nettyfound) {
				log.info("Netty found. Waiting");
				synchronized (this) {
					this.wait(1000);
				}
			}
		} while (nettyfound);

		log.info("************************* TEARDOWN DONE *****");
	}

	private void startThreadChecker() {
		new ThreadChecker(new ThreadChecker.IChecker() {

			@Override
			public boolean check() {

				for (P2PServer s : servers) {
					if (s.isRunning()) {
						return true;
					}
				}
				return false;
			}
		});
	};

	WPreferences getPreferences(String username) {
		return new StaticTestPreferences(PREFERENCES_PREFIX, username);
	}

	protected void setValue(String name, String value) {
		values.put(name, value);
	}

	protected void waitForValue(final String name, int time) {
		ConditionWaiter.wait(new ConditionWaiter.Condition() {
			@Override
			public boolean test() {
				return values.get(name) != null;
			}
		}, time);
	}

	protected void assertValue(String name) {
		assertNotNull(values.get(name));
	}

	public void testTrue() {
		assertTrue(true);
	}

	protected P2PServer getServer() {
		P2PServer s = new P2PServerImpl(getPreferences("p2pservertests"), true);
		s.start();

		servers.add(s);
		log.info("returning " + s);
		return s;
	}

	protected P2PServer getOtherServerNoBind() {
		P2PServer nobindserver = new P2PServerImpl(new StaticTestPreferences("otherserver", "otherserver"), false);

		nobindserver.start();

		addServerNodes(nobindserver);

		servers.add(nobindserver);
		log.info("returning " + nobindserver);
		return nobindserver;
	}

	private void addServerNodes(P2PServer currentserver) {
		for (P2PServer server : servers) {
			if (server.getPort() != currentserver.getPort()) {
				currentserver.addNode(new MHost("localhost"), server.getPort());
			}

		}
	}

	protected WClient getClient(final String username, final boolean bind) throws MalformedURLException, SAXException {
		WPreferences p = new StaticTestPreferences("waazdohclienttests", username);
		P2PBinarySource source = new P2PBinarySource(p, new FileBeanStorage(p), bind);
		StaticService service = new StaticService(username);
		String session = service.createSession();

		MemoryBeanStorage bs = new MemoryBeanStorage();

		WClient c = new WClient(p, source, bs, service);
		c.setSession(session);
		assertNotNull(c.getUserID());

		return c;
	}

	protected String getRandomUserName() {
		return "username" + (usernamecounter++) + "_" + System.currentTimeMillis();
	}

	protected int getWaitTime() {
		if (System.getProperty("extended.debug") != null) {
			return 360000;
		} else {
			return 60000;
		}
	}

	protected String getTempPath() {
		String tempDir = System.getProperty("java.io.tmpdir") + File.separator + "user_"
				+ System.getProperty("user.name");
		return tempDir;
	}

}
