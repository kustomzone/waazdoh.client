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


public final class WBookmark {
	private String id;
	private String oid;
	private String created;

	public WBookmark(JBean bbookmark) {
		id = bbookmark.getValue("bookmarkid");
		oid = bbookmark.getValue("objectid");
		created = bbookmark.getValue("created");
	}

	public String getObjectID() {
		return oid;
	}
}