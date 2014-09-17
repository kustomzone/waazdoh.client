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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class WBookmarks {
	private List<BookmarksListener> listeners = new LinkedList<BookmarksListener>();
	private Map<String, WBookmarkGroup> groups = new HashMap<String, WBookmarkGroup>();

	private CMService service;

	public WBookmarks(CMService service) {
		this.service = service;
		update();
	}

	private void update() {
		Set<String> groups = service.listStorageArea("bookmarks");
		if (groups != null) {
			for (final String name : groups) {
				if (this.groups.get(name) == null) {
					this.groups.put(name,
							new WBookmarkGroup(name, service));
					fireGroupAdded(get(name));
				}
			}
		}
	}

	private WBookmarkGroup get(final String mid) {
		return groups.get(mid);
	}

	private void fireGroupAdded(WBookmarkGroup group) {
		for (BookmarksListener l : listeners) {
			l.groupAdded(group);
		}
	}

	public void addListener(BookmarksListener bookmarksListener) {
		this.listeners.add(bookmarksListener);
	}

	public List<WBookmarkGroup> getBookmarkGroups() {
		return new LinkedList<WBookmarkGroup>(groups.values());
	}

}
