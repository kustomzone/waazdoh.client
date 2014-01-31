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
package waazdoh.cutils;

import java.util.UUID;

public final class Version {
	private String id;

	public Version() {
		this.id = UUID.randomUUID().toString();
	}

	public Version(final String substring) {
		MID.check(substring);
		this.id = substring;
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Version) {
			Version b = (Version) obj;
			return id.equals(b.id);
		} else {
			return true;
		}
	}

	@Override
	public String toString() {
		return "" + id;
	}
}
