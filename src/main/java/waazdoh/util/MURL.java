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

import java.net.MalformedURLException;
import java.net.URL;

public final class MURL {
	private String append = "";
	private int port;
	private String host;

	public MURL(String protocol, final String host, final int port) {
		this.host = protocol + "://" + host;
		this.port = port;
	}

	public MURL(final String host, int port) {
		this.host = host;
		this.port = port;
		checkURL();
	}

	public MURL(final String host) {
		this.host = host;
		checkURL();
	}

	@Override
	public String toString() {
		String url = "";
		if (host.indexOf("http") < 0) {
			url = "https://";
		}

		url += host;

		if (port > 0) {
			url += ":" + port;
		}
		url += append;
		return url;
	}

	public MURL append(final String string) {
		append = append + string;
		checkURL();
		return this;
	}

	private void checkURL() {
		append = append.replace("//", "/");
	}

	public int getPort() {
		return port;
	}

	public String getHost() {
		return host;
	}

	public URL getURL() throws MalformedURLException {
		return new URL(toString());
	}
}
