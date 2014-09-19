package waazdoh.client;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;
import waazdoh.cp2p.P2PServer;
import waazdoh.testing.StaticTestPreferences;
import waazdoh.util.ConditionWaiter;
import waazdoh.util.MLogger;
import waazdoh.util.MPreferences;

public class WCTestCase extends TestCase {
	private static final String PREFERENCES_PREFIX = "wcclienttests";
	protected MLogger log = MLogger.getLogger(this);
	private Map<String, String> values = new HashMap<String, String>();
	private Set<P2PServer> servers = new HashSet<>();

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

	private P2PServer getServerNoBind() {
		P2PServer s = new P2PServer(getPreferences("p2pservertests"), false,
				null);
		s.start();
		servers.add(s);
		log.info("returning " + s);
		return s;
	}
}
