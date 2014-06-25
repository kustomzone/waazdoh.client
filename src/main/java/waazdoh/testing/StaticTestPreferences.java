package waazdoh.testing;

import java.io.File;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import waazdoh.util.MPreferences;

public final class StaticTestPreferences implements MPreferences {
	private String username;
	private String prefix;
	private static List<Integer> ports = new LinkedList<Integer>();

	public StaticTestPreferences(final String prefix, final String username) {
		this.username = username;
		this.prefix = prefix;
		//
		Preferences prefs = getPrefs();
		if (prefs.get(MPreferences.SERVICE_URL, "").equals("")) {
			prefs.put(MPreferences.SERVICE_URL,
					"http://localhost:18099/waazdoh");
		}
		if (prefs.get(MPreferences.LOCAL_PATH, "").equals("")) {
			prefs.put(MPreferences.LOCAL_PATH, System.getProperty("user.home")
					+ File.separator + "waazdohclienttest" + File.separator
					+ username + File.separator);
		}

		// creating a list of servers used in other clients in this test.
		prefs.putInt(MPreferences.NETWORK_MAX_DOWNLOADS, 8);
		String serverlist = "";
		for (Integer serviceport : ports) {
			serverlist += "localhost:" + serviceport + ",";
		}
		prefs.put(MPreferences.SERVERLIST, serverlist);
		// creating a random port
		int port = 8000 + (int) (Math.random() * 10000);
		prefs.putInt(MPreferences.NETWORK_SERVER_PORT, port);
		ports.add(port);
	}

	public static void clearPorts() {
		ports = new LinkedList<Integer>();
	}

	@Override
	public Set<String> getNames() {
		Set<String> ret = new HashSet<String>();
		String[] keys;
		try {
			keys = getPrefs().keys();
			for (final String string : keys) {
				ret.add(string);
			}

			return ret;
		} catch (BackingStoreException e) {
			e.printStackTrace();
			return null;
		}
	}

	public Preferences getPrefs() {
		Preferences prefs = Preferences.userNodeForPackage(getClass()).node(
				prefix + "/" + username);
		return prefs;
	}

	private int randomPort() {
		int port = 9000 + (int) (Math.random() * 10000);
		return port;
	}

	@Override
	public void set(final String name, String value) {
		getPrefs().put(name, value);
	}

	@Override
	public void set(final String name, boolean b) {
		set(name, "" + b);
	}

	@Override
	public int getInteger(final String string, int i) {
		String sint = get(string, "" + i);
		return Integer.parseInt(sint);
	}

	@Override
	public boolean getBoolean(final String name, boolean defbool) {
		return "true".equals(get(name));
	}

	@Override
	public String get(final String name, String defaultvalue) {
		String get = get(name);
		if (get == null || get.equals("")) {
			getPrefs().put(name, defaultvalue);
			return defaultvalue;
		} else {
			return get;
		}
	}

	public String get(final String string) {
		return getPrefs().get(string, "");
	}

	public static void resetPorts() {
		ports = new LinkedList<Integer>();
	}
}
