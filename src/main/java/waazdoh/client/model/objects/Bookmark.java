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
package waazdoh.client.model.objects;

import org.xml.sax.SAXException;

import waazdoh.common.WData;
import waazdoh.common.XML;
import waazdoh.common.client.ServiceClient;

public final class Bookmark {
	private String oid;
	private String created;
	private String name;

	public Bookmark(String group, String bookmarkname, ServiceClient service)
			throws SAXException {
		this.name = bookmarkname;

		String r = service.getStorageArea().read(
				service.getUser().getUsername(),
				"/bookmarks/" + group + "/" + bookmarkname);
		WData bbookmark = new WData(new XML(r));

		oid = bbookmark.getValue("objectid");
		created = bbookmark.getValue("created");
	}

	public String getObjectID() {
		return oid;
	}

	public String getName() {
		return name;
	}
}
