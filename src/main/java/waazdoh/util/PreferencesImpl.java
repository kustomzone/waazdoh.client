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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class PreferencesImpl implements MPreferences {
	private Map<String, String> values = new HashMap<String, String>();

	@Override
	public Set<String> getNames() {
		return values.keySet();
	}

	@Override
	public String get(final String name, String defaultvalue) {
		String v = values.get(name);
		if (v == null) {
			set(name, defaultvalue);
		}
		return defaultvalue;
	}

	@Override
	public int getInteger(final String string, int i) {
		String sint = get(string, "" + i);
		return Integer.parseInt(sint);
	}

	@Override
	public boolean getBoolean(final String name, boolean def) {
		return Boolean.parseBoolean(get(name, "" + def));
	}

	@Override
	public void set(final String name, String value) {
		values.put(name, value);
	}

	@Override
	public void set(final String name, boolean b) {
		set(name, "" + b);
	}
}
