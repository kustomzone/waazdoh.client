package waazdoh.client;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import org.xml.sax.SAXException;

import waazdoh.client.storage.local.FileBeanStorage;
import waazdoh.cp2p.P2PBinarySource;
import waazdoh.cp2p.P2PServer;
import waazdoh.testing.MockBeanStorage;
import waazdoh.testing.ServiceMock;
import waazdoh.testing.StaticTestPreferences;
import waazdoh.util.ConditionWaiter;
import waazdoh.util.MLogger;
import waazdoh.util.MPreferences;
import waazdoh.util.MStringID;

public class WCTestCase extends TestCase {
	private static final String PREFERENCES_PREFIX = "wcclienttests";
	protected MLogger log = MLogger.getLogger(this);
	private Map<String, String> values = new HashMap<String, String>();
	private Set<P2PServer> servers = new HashSet<>();

	private int usernamecounter = 0;

	protected void setUp() throws Exception {
		log.info("************************ STARTING A TEST " + this + " "
				+ this.getName() + " ********");
		values.clear();
		servers.clear();
	};

	protected void tearDown() throws Exception {
		Set<P2PServer> ss = this.servers;
		for (P2PServer p2pServer : ss) {
			p2pServer.close();
		}
	};

	MPreferences getPreferences(String username) {
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
		P2PServer s = new P2PServer(getPreferences("p2pservertests"), true,
				null);
		s.start();
		servers.add(s);
		log.info("returning " + s);
		return s;
	}

	protected P2PServer getOtherServerNoBind() {
		P2PServer s = new P2PServer(new StaticTestPreferences("otherserver",
				"otherserver"), false, null);
		s.start();
		servers.add(s);
		log.info("returning " + s);
		return s;
	}

	protected WClient getClient(final String username, final boolean bind)
			throws MalformedURLException, SAXException {
		MPreferences p = new StaticTestPreferences("waazdohclienttests",
				username);
		P2PBinarySource source = new P2PBinarySource(p, new FileBeanStorage(p),
				bind);
		ServiceMock service = new ServiceMock(username, source);
		service.createSession();

		MockBeanStorage bs = new MockBeanStorage();

		WClient c = new WClient(p, source, bs, service);
		c.setSession(service.getSessionID());
		return c;
	}

	protected P2PBinarySource getServiceSource(final String username1,
			boolean bind) throws SAXException {
		MPreferences p1 = new StaticTestPreferences("waazdohclienttests",
				username1);
		P2PBinarySource source1 = new P2PBinarySource(p1, new FileBeanStorage(
				p1), bind);
		ServiceMock service1 = new ServiceMock(username1, source1);

		service1.setSession("" + new MStringID());
		return source1;
	}

	protected String getRandomUserName() {
		return "username" + (usernamecounter++) + "_"
				+ System.currentTimeMillis();
	}
}
