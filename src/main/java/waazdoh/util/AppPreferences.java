/*******************************************************************************
 * Copyright (c) 2013 Juuso Vilmunen.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Juuso Vilmunen - initial API and implementation
 ******************************************************************************/
package waazdoh.util;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public final class AppPreferences implements MPreferences {
	private Preferences p;
	private MLogger log = MLogger.getLogger(this);

	public AppPreferences(String prefix) {
		init(prefix);
	}

	public AppPreferences() {
		init(null);
	}

	private void init(String prefix) {
		String propertyprefix = System.getProperty("waazdoh.prefix");
		if (prefix == null || propertyprefix != null) {
			prefix = propertyprefix;
		}

		if (prefix == null) {
			prefix = "default";
		}

		p = Preferences.userRoot().node("waazdoh/" + prefix);
	}

	@Override
	public Set<String> getNames() {
		Set<String> ret = new HashSet<String>();
		String[] keys;
		try {
			keys = p.keys();
			for (final String string : keys) {
				ret.add(string);
			}
			return ret;
		} catch (BackingStoreException e) {
			log.error(e);
			return null;
		}
	}

	@Override
	public String get(final String name, String defaultvalue) {
		if (p.get(name, null) == null && defaultvalue != null) {
			if (System.getProperty("waazdoh." + name) != null) {
				defaultvalue = System.getProperty("waazdoh." + name);
			}
			//
			set(name, defaultvalue);
		}
		String parsed = parse(name, p.get(name, defaultvalue));

		log.info("get " + name + " = " + parsed);
		return parsed;
	}

	@Override
	public int getInteger(final String string, int i) {
		String sint = get(string, "" + i);
		return Integer.parseInt(sint);
	}

	private String parse(final String name, String value) {
		if (name.indexOf(".home.") > 0) {
			if (value != null && value.indexOf("/") != 0) {
				value = System.getProperty("user.home") + File.separator
						+ value;
			}
		}
		return value;
	}

	@Override
	public void set(final String name, String value) {
		if (name != null && value != null) {
			p.put(name, value);
		}
	}

	@Override
	public void set(final String name, boolean b) {
		set(name, "" + b);
	}

	@Override
	public boolean getBoolean(final String valuename, boolean defaultvalue) {
		return "true".equals("" + get(valuename, "" + defaultvalue));
	}
}
