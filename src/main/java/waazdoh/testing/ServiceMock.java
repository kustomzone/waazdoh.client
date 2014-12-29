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

import waazdoh.client.binaries.BinarySource;
import waazdoh.client.model.WService;
import waazdoh.client.model.WData;
import waazdoh.client.model.WResponse;
import waazdoh.client.model.ObjectID;
import waazdoh.client.model.UserID;
import waazdoh.util.MLogger;
import waazdoh.util.MStringID;
import waazdoh.util.MURL;
import waazdoh.util.xml.XML;

public final class ServiceMock implements WService {
	final private String username;
	private String session;
	private UserID userid;
	private Map<String, WData> groups = new HashMap<String, WData>();
	final private BinarySource source;

	private static Map<String, WData> objects = new HashMap<String, WData>();
	private static Map<String, String> storagearea = new HashMap<>();

	private MLogger log = MLogger.getLogger(this);

	public ServiceMock(String username, BinarySource nsource)
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
				String name = storedpath.substring(string.length() + 1);
				ret.add(name);
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

		WData b = new WData(new XML(sxml));
		groups.put(gid, b);
	}

	@Override
	public WResponse getUser(UserID userid) {
		String sxml = "<response> <user> <uid>"
				+ userid
				+ "</uid><profile><pictureURL>https://twimg0-a.akamaihd.net/profile_images/2297908262/rhp37rm35mul5uf0zom6_reasonably_small.jpeg</pictureURL>	  <name>Juuso</name> <info>me!!!</info> </profile> <name>test"
				+ userid
				+ "</name>		  </user> <success>true</success> </response>";
		WResponse r = WResponse.getTrue();
		try {
			r.setBean(new WData(new XML(sxml)));
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
	public WData read(MStringID id) {
		if (id == null) {
			return null;
		} else {
			WData b = source.getBean(id.toString());
			if (b == null) {
				b = ServiceMock.objects.get(id.toString());
			}

			if (b != null) {
				WData ret = new WData("object");
				ret.add("data").add(b);
				ret.addValue("success", true);
				return ret;
			} else {
				return null;
			}
		}
	}

	@Override
	public void addBean(MStringID id, WData b) {
		source.addBean(id.toString(), b);
		ServiceMock.objects.put(id.toString(), b);
	}

	@Override
	public boolean publish(ObjectID id) {
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
	public WResponse search(final String filter, int index, int count) {
		WResponse ret = WResponse.getTrue();
		HashSet<String> list = new HashSet<String>();
		for (int i = index; i < count; i++) {
			list.add("" + new MStringID());
		}
		ret.getBean().addList("idlist", list);
		return ret;
	}

	@Override
	public MURL getURL(final String service, String method, ObjectID id) {
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
	public WData requestAppLogin() {
		WData b = new WData("applogin");
		b.addValue("id", new MStringID().toString());
		b.addValue("url", "mockurl");
		return b;
	}

	public void createSession() {
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
	public WResponse reportDownload(MStringID id, boolean success) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isConnected() {
		return true;
	}

	@Override
	public WData acceptAppLogin(MStringID id) {
		return WResponse.getTrue().getBean();
	}

	@Override
	public WData checkAppLogin(MStringID id) {
		createSession();
		//
		WData b = new WData("mockapplogin");
		b.addValue("id", id.toString());
		b.addValue("sessionid", session);
		return b;
	}

}
