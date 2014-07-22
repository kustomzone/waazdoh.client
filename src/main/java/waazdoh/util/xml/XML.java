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
package waazdoh.util.xml;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

public final class XML {
	private String string;

	public XML(final String string) {
		this.string = string;
	}

	public String getString() {
		return string;
	}

	public void setString(final String string) {
		this.string = string;
	}

	public XML(Reader stringReader) throws IOException {
		StringBuilder sb = new StringBuilder();
		BufferedReader br = new BufferedReader(stringReader);
		while (true) {
			String line = br.readLine();
			if (line == null) {
				break;
			}
			sb.append(line);
			sb.append("\n");
		}
		string = sb.toString();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof XML) {
			XML b = (XML) obj;
			return b.string.equals(string);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return string.hashCode();
	}

	@Override
	public String toString() {
		return "" + string;
	}
}
