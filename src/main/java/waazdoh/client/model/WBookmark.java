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

import org.xml.sax.SAXException;

import waazdoh.util.xml.XML;

public final class WBookmark {
	private String oid;
	private String created;
	private String name;

	public WBookmark(String group, String bookmarkname, CMService service)
			throws SAXException {
		this.name = bookmarkname;

		String r = service.readStorageArea("/bookmarks/" + group + "/"
				+ bookmarkname);
		JBean bbookmark = new JBean(new XML(r));

		oid = bbookmark.getValue("objectid");
		created = bbookmark.getValue("created");
	}

	public String getObjectID() {
		return oid;
	}
}
