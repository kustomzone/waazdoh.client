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

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.xml.sax.SAXException;

import waazdoh.util.MLogger;

public final class WBookmarkGroup {
	private CMService service;
	private String name;

	private List<WBookmark> bookmarks = new LinkedList<WBookmark>();
	private List<WBookmarkGroupListener> listeners = new LinkedList<WBookmarkGroupListener>();

	private MLogger log = MLogger.getLogger(this);

	public WBookmarkGroup(final String name, CMService service) {
		this.service = service;
		this.name = name;
		//
		update();
	}

	private void update() {
		Set<String> br = service.listStorageArea("/bookmarks/" + name);
		MLogger.getLogger(this).info("BookmarkGroup " + br);
		//
		if (br != null) {
			for (String bookmarkname : br) {
				try {
					bookmarks.add(new WBookmark(name, bookmarkname, service));
				} catch (SAXException e) {
					log.error(e);
				}
			}
		}
	}

	public String getName() {
		return name;
	}

	public List<WBookmark> getBookmarks() {
		return new LinkedList<WBookmark>(bookmarks);
	}

	public void addListener(WBookmarkGroupListener wBookmarkGroupListener) {
		listeners.add(wBookmarkGroupListener);
	}
}
