package waazdoh.client;

import junit.framework.TestCase;
import waazdoh.testing.StaticTestPreferences;
import waazdoh.util.MPreferences;

public class WCTestCase extends TestCase {
	private static final String PREFERENCES_PREFIX = "wcclienttests";

	MPreferences getPreferences(String username) {
		return new StaticTestPreferences(PREFERENCES_PREFIX, username);
	}

	public void testTrue() {
		assertTrue(true);
	}
}
