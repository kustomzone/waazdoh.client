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

import java.util.LinkedList;
import java.util.List;

import org.xml.sax.SAXException;

import waazdoh.common.WLogger;
import waazdoh.common.client.ServiceClient;

public final class BookmarkGroup {
	private ServiceClient service;
	private String name;

	private List<Bookmark> bookmarks = new LinkedList<Bookmark>();
	private List<BookmarkGroupListener> listeners = new LinkedList<BookmarkGroupListener>();

	private WLogger log = WLogger.getLogger(this);

	public BookmarkGroup(final String name, ServiceClient service) {
		this.service = service;
		this.name = name;
		//
		update();
	}

	private void update() {
		List<String> br = service.getStorageArea().list("/bookmarks/" + name);
		WLogger.getLogger(this).info("BookmarkGroup " + br);
		//
		if (br != null) {
			for (String bookmarkname : br) {
				try {
					bookmarks.add(new Bookmark(name, bookmarkname, service));
				} catch (SAXException e) {
					log.error(e);
				}
			}
		}
	}

	public void add(String name, String value) {
		service.getStorageArea().write("/bookmarks/" + name, value);
		update();
	}

	public String getName() {
		return name;
	}

	public Bookmark get(String name) {
		for (Bookmark bookmark : bookmarks) {
			if (bookmark.getName().equals(name)) {
				return bookmark;
			}
		}
		return null;
	}

	public List<Bookmark> getBookmarks() {
		return new LinkedList<Bookmark>(bookmarks);
	}

	public void addListener(BookmarkGroupListener wBookmarkGroupListener) {
		listeners.add(wBookmarkGroupListener);
	}
}
