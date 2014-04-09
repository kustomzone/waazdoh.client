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
package waazdoh.testing;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.xml.sax.SAXException;

import waazdoh.client.MBinarySource;
import waazdoh.cutils.JBeanResponse;
import waazdoh.cutils.MID;
import waazdoh.cutils.MStringID;
import waazdoh.cutils.MURL;
import waazdoh.cutils.UserID;
import waazdoh.cutils.xml.JBean;
import waazdoh.cutils.xml.XML;
import waazdoh.service.CMService;

public final class ServiceMock implements CMService {
	private String username;
	private String session;
	private UserID userid;
	private Map<String, JBean> groups = new HashMap<String, JBean>();
	private MBinarySource source;

	private static Map<String, JBean> objects = new HashMap<String, JBean>();

	public ServiceMock(MBinarySource source) throws SAXException {
		MStringID gusersid = new MStringID();
		this.source = source;
		//
		String gname = "users";
		addBGroup(gusersid.toString(), gname);
		addBGroup(new MStringID().toString(), "test");

	}

	private void addBGroup(final String gid, String gname) throws SAXException {
		String sxml = "<response> <bookmarkgroup>		  <owner>1b32558c-827d-4f4c-83bf-b9ea4a313db6</owner>		  <name>users</name>"
				+ "  <groupid>"
				+ gid
				+ "</groupid>		  <created>2012-09-14T03:27:05.200Z</created> <bookmarks> <bookmark>"
				+ "		  <objectid>1b32558c-827d-4f4c-83bf-b9ea4a313db6</objectid>		  <created>Tue Sep 18 10:35:50 UTC 2012</created>		  <bookmarkid>8fbdff42-16f1-4619-9083-1624b8ed4ef4.141a553a-7664-4752-a176-3d19b8faf34e</bookmarkid> </bookmark> <bookmark>"
				+ "		  <objectid>1b32558c-827d-4f4c-83bf-b9ea4a313db6</objectid>"
				+ "		  <created>Thu Sep 20 07:25:19 UTC 2012</created>		  <bookmarkid>6b8f96a2-db16-452f-8038-df8d4c681d2d.15d64b78-4fb0-4948-8543-73cf49cdf627</bookmarkid> </bookmark> </bookmarks> </bookmarkgroup>"
				+ "		  </response>";

		JBean b = new JBean(new XML(sxml));
		groups.put(gid, b);
	}

	@Override
	public JBeanResponse getUser(UserID userid) {
		String sxml = "<response> <user> <uid>"
				+ userid
				+ "</uid><profile><pictureURL>https://twimg0-a.akamaihd.net/profile_images/2297908262/rhp37rm35mul5uf0zom6_reasonably_small.jpeg</pictureURL>	  <name>Juuso</name> <info>me!!!</info> </profile> <name>test"
				+ userid
				+ "</name>		  </user> <success>true</success> </response>";
		JBeanResponse r = JBeanResponse.getTrue();
		try {
			r.setBean(new JBean(new XML(sxml)));
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return r;
	}

	@Override
	public boolean setSession(final String username, String session) {
		createSession(username);
		return true;
	}

	@Override
	public JBean read(MStringID id) {
		if (id == null) {
			return null;
		} else {
			JBean b = source.getBean(id.toString());
			if (b == null) {
				b = ServiceMock.objects.get(id.toString());
			}
			JBean ret = new JBean("object");
			ret.add("data").add(b);
			ret.addValue("success", true);
			return ret;
		}
	}

	@Override
	public void addBean(MStringID id, JBean b) {
		source.addBean(id.toString(), b);
		ServiceMock.objects.put(id.toString(), b);
	}

	@Override
	public boolean publish(MID id) {
		return publish(id.getStringID());
	}

	@Override
	public boolean publish(MStringID id) {
		return true;
	}

	@Override
	public UserID getUserID() {
		return userid;
	}

	@Override
	public JBeanResponse search(final String filter, int index, int count) {
		JBeanResponse ret = JBeanResponse.getTrue();
		HashSet<String> list = new HashSet<String>();
		for (int i = index; i < count; i++) {
			list.add("" + new MStringID());
		}
		ret.getBean().addList("items", list);
		return ret;
	}

	@Override
	public MURL getURL(final String service, String method, MID id) {
		return new MURL("localhost");
	}

	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public boolean isLoggedIn() {
		return username != null && session != null && userid != null;
	}

	@Override
	public String requestAppLogin(final String username, String appname,
			MStringID appid) {
		createSession(username);
		return session;
	}

	private void createSession(final String username) {
		session = new MStringID().toString();
		userid = new UserID(new MStringID().toString());
		this.username = username;
	}

	@Override
	public String getSessionID() {
		return session;
	}

	@Override
	public String getInfoText() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JBeanResponse reportDownload(MStringID id, boolean success) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JBeanResponse getBookmarkGroup(final String id) {
		JBeanResponse ret = JBeanResponse.getTrue();
		ret.setBean(groups.get(id));
		return ret;
	}

	@Override
	public Map<String, String> getBookmarkGroups() {
		Map<String, String> ret = new HashMap<String, String>();
		for (final String id : groups.keySet()) {
			JBean b = groups.get(id);
			ret.put(id, b.getValue("name"));
		}
		return ret;
	}

	@Override
	public boolean isConnected() {
		return true;
	}
}
