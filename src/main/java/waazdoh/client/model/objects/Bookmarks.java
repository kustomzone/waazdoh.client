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

import waazdoh.common.client.ServiceClient;

public final class Bookmarks {
	private List<BookmarksListener> listeners = new LinkedList<BookmarksListener>();
	private Map<String, BookmarkGroup> groups = new HashMap<String, BookmarkGroup>();

	private ServiceClient service;

	public Bookmarks(ServiceClient service) {
		this.service = service;
		update();
	}

	private synchronized void update() {
		List<String> servicegroups = service.getStorageArea().list("bookmarks");
		if (servicegroups != null) {
			for (final String name : servicegroups) {
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
