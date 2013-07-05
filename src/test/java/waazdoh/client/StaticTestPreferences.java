package waazdoh.client;

import java.util.HashSet;
import java.util.Set;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import waazdoh.cutils.MPreferences;

public class StaticTestPreferences implements MPreferences {
	private String username;

	public StaticTestPreferences(String username) {
		this.username = username;
		Preferences prefs = getPrefs();
		if (prefs.get(MPreferences.SERVICE_URL, "").equals("")) {
			prefs.put(MPreferences.SERVICE_URL, "http://localhost:8080/cmusic");
		}
		if (prefs.get(MPreferences.LOCAL_PATH, "").equals("")) {
			prefs.put(MPreferences.LOCAL_PATH, System.getProperty("user.home")
					+ "/waazdihclienttest/" + username + "/");
		}

		prefs.putInt(MPreferences.NETWORK_MAX_DOWNLOADS, 8);
		prefs.put(MPreferences.SERVERLIST, "localhost");
	}

	@Override
	public Set<String> getNames() {
		Set<String> ret = new HashSet<String>();
		String[] keys;
		try {
			keys = getPrefs().keys();
			for (String string : keys) {
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
				"test/" + username);
		return prefs;
	}

	private int randomPort() {
		int port = 9000 + (int) (Math.random() * 10000);
		return port;
	}

	@Override
	public void set(String name, String value) {
		getPrefs().put(name, value);
	}

	@Override
	public void set(String name, boolean b) {
		set(name, "" + b);
	}

	@Override
	public int getInteger(String string, int i) {
		String sint = get(string, "" + i);
		return Integer.parseInt(sint);
	}

	@Override
	public boolean getBoolean(String name, boolean defbool) {
		return "true".equals(get(name));
	}

	@Override
	public String get(String name, String defaultvalue) {
		String get = get(name);
		if (get == null || get.equals("")) {
			getPrefs().put(name, defaultvalue);
			return defaultvalue;
		} else {
			return get;
		}
	}

	public String get(String string) {
		return getPrefs().get(string, "");
	}
}