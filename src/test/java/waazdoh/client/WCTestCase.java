package waazdoh.client;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import org.xml.sax.SAXException;

import waazdoh.client.storage.local.FileBeanStorage;
import waazdoh.common.ConditionWaiter;
import waazdoh.common.ThreadChecker;
import waazdoh.common.WLogger;
import waazdoh.common.WPreferences;
import waazdoh.cp2p.P2PBinarySource;
import waazdoh.cp2p.P2PServer;
import waazdoh.cp2p.P2PServerImpl;
import waazdoh.cp2p.common.MHost;
import waazdoh.testing.MockBeanStorage;
import waazdoh.testing.StaticService;
import waazdoh.testing.StaticTestPreferences;

public class WCTestCase extends TestCase {
	private static final String PREFERENCES_PREFIX = "wcclienttests";
	protected WLogger log = WLogger.getLogger(this);
	private Map<String, String> values = new HashMap<String, String>();
	private Set<P2PServer> servers = new HashSet<>();

	private int usernamecounter = 0;

	protected void setUp() throws Exception {
		log.info("************************ STARTING A TEST " + this + " "
				+ this.getName() + " ********");
		values.clear();
		servers.clear();
		StaticTestPreferences.clearPorts();
	};

	protected void tearDown() throws Exception {
		log.info("************************* CLOSING " + this + " "
				+ this.getName() + " ********");

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

		Set<P2PServer> ss = this.servers;
		for (P2PServer p2pServer : ss) {
			p2pServer.startClosing();
		}

		for (P2PServer p2pServer : ss) {
			p2pServer.close();
		}
	};

	WPreferences getPreferences(String username) {
		return new StaticTestPreferences(PREFERENCES_PREFIX, username);
	}

	protected void setValue(String name, String value) {
		values.put(name, value);
	}

	protected void waitForValue(final String name, int time) {
		new ConditionWaiter(new ConditionWaiter.Condition() {
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
		P2PServer nobindserver = new P2PServerImpl(new StaticTestPreferences(
				"otherserver", "otherserver"), false);

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

	protected WClient getClient(final String username, final boolean bind)
			throws MalformedURLException, SAXException {
		WPreferences p = new StaticTestPreferences("waazdohclienttests",
				username);
		P2PBinarySource source = new P2PBinarySource(p, new FileBeanStorage(p),
				bind);
		StaticService service = new StaticService(username);
		String session = service.createSession();

		MockBeanStorage bs = new MockBeanStorage();

		WClient c = new WClient(p, source, bs, service);
		c.setSession(session);
		assertNotNull(c.getUserID());

		return c;
	}

	protected String getRandomUserName() {
		return "username" + (usernamecounter++) + "_"
				+ System.currentTimeMillis();
	}
}
