package waazdoh.client;

import junit.framework.TestCase;
import waazdoh.testing.StaticTestPreferences;
import waazdoh.util.MLogger;
import waazdoh.util.MPreferences;

public class WCTestCase extends TestCase {
	private static final String PREFERENCES_PREFIX = "wcclienttests";
	protected MLogger log = MLogger.getLogger(this);

	protected void setUp() throws Exception {
		log.info("************************ STARTING A TEST " + this + " "
				+ this.getName() + " ********");
	};

	MPreferences getPreferences(String username) {
		return new StaticTestPreferences(PREFERENCES_PREFIX, username);
	}

	public void testTrue() {
		assertTrue(true);
	}
}
