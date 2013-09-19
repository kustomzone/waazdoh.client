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
package waazdoh.client;

import java.util.LinkedList;
import java.util.List;

import waazdoh.cutils.JBeanResponse;
import waazdoh.cutils.MLogger;
import waazdoh.cutils.xml.JBean;
import waazdoh.service.CMService;

public final class WBookmarkGroup {
	private CMService service;
	private String id;
	private String name;

	private List<WBookmark> bookmarks = new LinkedList<WBookmark>();

	private List<WBookmarkGroupListener> listeners = new LinkedList<WBookmarkGroupListener>();
	private String created;

	public WBookmarkGroup(String mid, String name, CMService service) {
		this.service = service;
		this.id = mid;
		this.name = name;
		//
		update();
	}

	private void update() {
		/*
		 * <response> <bookmarkgroup>
		 * <owner>1b32558c-827d-4f4c-83bf-b9ea4a313db6</owner>
		 * <name>users</name>
		 * <groupid>fac8093e-c9ed-43b6-99bd-7fc9207f3c7d</groupid>
		 * <created>2012-09-14T03:27:05.200Z</created> <bookmarks> <bookmark>
		 * <objectid>1b32558c-827d-4f4c-83bf-b9ea4a313db6</objectid>
		 * <created>Tue Sep 18 10:35:50 UTC 2012</created>
		 * <bookmarkid>8fbdff42-16f
		 * 1-4619-9083-1624b8ed4ef4.141a553a-7664-4752-a176
		 * -3d19b8faf34e</bookmarkid> </bookmark> <bookmark>
		 * <objectid>1b32558c-827d-4f4c-83bf-b9ea4a313db6</objectid>
		 * <created>Thu Sep 20 07:25:19 UTC 2012</created>
		 * <bookmarkid>6b8f96a2-db16
		 * -452f-8038-df8d4c681d2d.15d64b78-4fb0-4948-8543
		 * -73cf49cdf627</bookmarkid> </bookmark> </bookmarks> </bookmarkgroup>
		 * </response>
		 */
		JBeanResponse br = service.getBookmarkGroup(id);
		MLogger.getLogger(this).info("BookmarkGroup " + br);
		//
		JBean b = br.getBean().get("bookmarkgroup");
		this.name = b.getValue("name");
		this.created = b.getValue("created");

		List<JBean> cbookmarks = b.get("bookmarks").getChildren();
		for (JBean bbookmark : cbookmarks) {
			bookmarks.add(new WBookmark(bbookmark));
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
