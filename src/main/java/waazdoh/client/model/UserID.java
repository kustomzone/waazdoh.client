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
package waazdoh.client.model;

public final class UserID {
	private String id;

	public UserID(final String id) {
		this.id = id.toString();
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof UserID && obj != null) {
			UserID nid = (UserID) obj;
			return id.equals(nid.id);
		} else {
			return false;
		}
	}

	@Override
	public String toString() {
		return id;
	}
}
