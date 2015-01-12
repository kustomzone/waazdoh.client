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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import waazdoh.client.service.WService;

public final class Bookmarks {
	private List<BookmarksListener> listeners = new LinkedList<BookmarksListener>();
	private Map<String, BookmarkGroup> groups = new HashMap<String, BookmarkGroup>();

	private WService service;

	public Bookmarks(WService service) {
		this.service = service;
		update();
	}

	private synchronized void update() {
		Set<String> groups = service.listStorageArea("bookmarks");
		if (groups != null) {
			for (final String name : groups) {
				if (this.groups.get(name) == null) {
					this.groups.put(name, new BookmarkGroup(name, service));
					fireGroupAdded(get(name));
				}
			}
		}
	}

	private BookmarkGroup get(final String mid) {
		return groups.get(mid);
	}

	private void fireGroupAdded(BookmarkGroup group) {
		for (BookmarksListener l : listeners) {
			l.groupAdded(group);
		}
	}

	public void addListener(BookmarksListener bookmarksListener) {
		this.listeners.add(bookmarksListener);
	}

	public List<BookmarkGroup> getBookmarkGroups() {
		return new LinkedList<BookmarkGroup>(groups.values());
	}

	public synchronized void addGroup(String string) {
		BookmarkGroup g = new BookmarkGroup(string, service);
		this.groups.put(string, g);
		fireGroupAdded(g);
	}

}
