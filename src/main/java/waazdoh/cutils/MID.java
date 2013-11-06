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

public final class MID {
	private final String id;
	private HashSource hashsource;

	public MID(String value, HashSource hsource) {
		int i = value.indexOf(".");
		id = value.substring(0, i);
		MID.check(id);
		hashsource = hsource;
	}

	public MID(HashSource hsource) {
		id = UUID.randomUUID().toString();
		hashsource = hsource;
	}

	public MID(MStringID oid, HashSource nhashsource) {
		this(oid.toString(), nhashsource);
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	private String getSourceHash() {
		return hashsource.getHash();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof MID) {
			MID bid = (MID) obj;
			return bid.id.equals(id)
					&& getSourceHash().equals(bid.hashsource.getHash());
		} else if (obj instanceof MStringID) {
			return getStringID().equals((MStringID) obj);
		} else {
			return false;
		}
	}

	@Override
	public String toString() {
		return id + "." + getSourceHash();
	}

	public static void check(String substring) {
		if (substring.length() != 36) {
			throw new IllegalArgumentException("id value length "
					+ substring.length());
		}
	}

	public MStringID getStringID() {
		return new MStringID(toString());
	}

}
