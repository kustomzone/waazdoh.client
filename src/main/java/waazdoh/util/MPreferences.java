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

import java.util.Set;

public interface MPreferences {
	static final String SERVICE_URL = "service.url";
	static final String LOCAL_PATH = "local.path";
	static final String LOCAL_PATH_DEFAULT = "waazdoh";
	static final String SERVERLIST = "network.hosts.list";
	static final String NETWORK_SERVER_PORT = "network.server.port";
	static final String NETWORK_MAX_DOWNLOADS = "network.downloads.max";
	static final String MEMORY_MAX_USAGE = "memory.max";
	static final String PREFERENCES_SESSION = "user.session";

	//

	String get(final String name, String defaultvalue);

	boolean getBoolean(final String valuename, boolean defaultvalue);

	void set(final String name, String value);

	void set(final String name, boolean b);

	int getInteger(final String string, int i);

	Set<String> getNames();

}
