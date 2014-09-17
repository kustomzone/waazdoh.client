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
import java.util.Set;

import org.xml.sax.SAXException;

import waazdoh.client.binaries.MBinarySource;
import waazdoh.client.model.CMService;
import waazdoh.client.model.JBean;
import waazdoh.client.model.JBeanResponse;
import waazdoh.client.model.MID;
import waazdoh.client.model.UserID;
import waazdoh.util.MLogger;
import waazdoh.util.MStringID;
import waazdoh.util.MURL;
import waazdoh.util.xml.XML;

public final class ServiceMock implements CMService {
	final private String username;
	private String session;
	private UserID userid;
	private Map<String, JBean> groups = new HashMap<String, JBean>();
	final private MBinarySource source;

	private static Map<String, JBean> objects = new HashMap<String, JBean>();
	private static Map<String, String> storagearea = new HashMap<>();

	private MLogger log = MLogger.getLogger(this);

	public ServiceMock(String username, MBinarySource nsource)
			throws SAXException {
		this.username = username;
		MStringID gusersid = new MStringID();
		this.source = nsource;
		nsource.setService(this);
		//
		String gname = "users";
		addBGroup(gusersid.toString(), gname);
		addBGroup(new MStringID().toString(), "test");

	}

	@Override
	public String readStorageArea(String string) {
		log.info("read storagearea " + string);
		return storagearea.get(string);
	}

	@Override
	public Set<String> listStorageArea(String string) {
		Set<String> ret = new HashSet<>();

		Set<String> s = storagearea.keySet();
		for (String storedpath : s) {
			if (storedpath.indexOf(string) == 0) {
				ret.add(storedpath);
			}
		}

		return ret;
	}

	public void writeStorageArea(String string, String string2) {
		storagearea.put(string, string2);
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
			MLogger.getLogger(this).error(e);
		}
		return r;
	}

	@Override
	public boolean setSession(String nsession) {
		if (nsession == null || nsession.length() == 0) {
			return false;
		} else {
			this.session = nsession;
			createUserID();
			return true;
		}
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

			if (b != null) {
				JBean ret = new JBean("object");
				ret.add("data").add(b);
				ret.addValue("success", true);
				return ret;
			} else {
				return null;
			}
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
	public JBean requestAppLogin() {
		JBean b = new JBean("applogin");
		b.addValue("id", new MStringID().toString());
		b.addValue("url", "mockurl");
		return b;
	}

	private void createSession() {
		session = new MStringID().toString();
		createUserID();
	}

	private void createUserID() {
		userid = new UserID(new MStringID().toString());
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
	public boolean isConnected() {
		return true;
	}

	@Override
	public JBean acceptAppLogin(MStringID id) {
		return JBeanResponse.getTrue().getBean();
	}

	@Override
	public JBean checkAppLogin(MStringID id) {
		createSession();
		//
		JBean b = new JBean("mockapplogin");
		b.addValue("id", id.toString());
		b.addValue("sessionid", session);
		return b;
	}

}
