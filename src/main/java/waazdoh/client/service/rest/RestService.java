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
package waazdoh.client.service.rest;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.xml.sax.SAXException;

import waazdoh.client.model.ObjectID;
import waazdoh.client.model.UserID;
import waazdoh.client.model.WData;
import waazdoh.client.model.WResponse;
import waazdoh.client.service.WService;
import waazdoh.client.storage.BeanStorage;
import waazdoh.util.MLogger;
import waazdoh.util.MStringID;
import waazdoh.util.MURL;
import waazdoh.util.URLCaller;
import waazdoh.util.XML;

public final class RestService implements WService {
	private MURL url;
	private MLogger log = MLogger.getLogger(this);
	private String sessionid;
	private UserID userid;
	private String username;
	private boolean loggedin;
	private BeanStorage beanstorage;

	public RestService(final String localurl,
			final BeanStorage beanstorage) throws MalformedURLException {
		this.url = new MURL(localurl);
		this.beanstorage = beanstorage;
	}

	@Override
	public String getInfoText() {
		return "Session:" + sessionid + " User:" + userid;
	}

	@Override
	public WData requestAppLogin() {
		String method = "requestapplogin";
		//
		WData response = getBean("users", method, false,
				new LinkedList<String>());
		return response;
	}

	@Override
	public WData acceptAppLogin(MStringID id) {
		List<String> params = new LinkedList<String>();
		params.add(id.toString());
		return getBean("users", "acceptapp", true, params);
	}

	@Override
	public WData checkAppLogin(MStringID id) {
		List<String> params = new LinkedList<String>();
		params.add(id.toString());
		return getBean("users", "checkapplogin", false, params);
	}

	@Override
	public String readStorageArea(String string) {
		List<String> params = new LinkedList<>();
		params.add(string);
		return getBean("storage", "read", true, params).getValue("data");
	}

	@Override
	public Set<String> listStorageArea(String string) {
		List<String> params = new LinkedList<>();
		params.add(string);
		WData b = getBean("storage", "list", true, params).get("items");
		Set<String> ret = new HashSet<>();
		List<WData> cs = b.getChildren();
		for (WData childbean : cs) {
			ret.add(childbean.getValue("name"));
		}
		//
		return ret;
	}

	@Override
	public void writeStorageArea(String string, String sdata) {
		List<String> params = new LinkedList<String>();
		StringTokenizer st = new StringTokenizer(string, "/");
		while (st.hasMoreTokens()) {
			String t = st.nextToken();
			params.add(t);
		}

		Map<String, String> data = new HashMap<String, String>();
		data.put("data", sdata);
		post("storage", "write", params, data);
	}

	@Override
	public String getSessionID() {
		return sessionid;
	}

	@Override
	public boolean isLoggedIn() {
		return loggedin;
	}

	@Override
	public boolean setSession(String session) {
		if (session != null && session.length() > 0) {
			sessionid = session;
			List<String> params = new LinkedList<String>();
			WResponse response = getResponses("users", "checksession", true,
					params);
			log.info("checksession response " + response);
			WData buid = response.find("userid");
			if (response.isSuccess() && buid != null) {
				String suserid = buid.getText();
				if (suserid != null) {
					userid = new UserID(suserid);
					this.username = response.find("username").getText();
					this.loggedin = true;
					return true;
				} else {
					loggedin = false;
					sessionid = null;
					return false;
				}
			} else {
				sessionid = null;
				return false;
			}
		} else {
			return false;
		}
	}

	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public MURL getURL(final String service, String type, ObjectID id) {
		List<String> params = new LinkedList<String>();
		params.add("" + id);
		return getAnonymousURL(service, type, params);
	}

	private MURL getAnonymousURL(final String service, String string,
			List<String> params) {
		return getAuthURL(service, string, params, null);
	}

	@Override
	public WResponse reportDownload(MStringID id, boolean success) {
		List<String> params = new LinkedList<String>();
		params.add(sessionid.toString());
		params.add(id.toString());
		params.add("" + success);
		WResponse response = getResponses("objects", "reportdownload", false,
				params);
		return response;
	}

	@Override
	public WResponse getUser(UserID userid) {
		WData b = beanstorage.getBean(userid);
		if (b == null) {
			List<String> params = new LinkedList<String>();
			params.add(userid.toString());
			WResponse getresponse = getResponses("users", "get", true, params);
			if (getresponse.isSuccess()) {
				beanstorage.addBean(userid, getresponse.getBean());
				WResponse resp = WResponse.getTrue();
				resp.setBean(b);
				return resp;
			} else {
				return WResponse.getFalse();
			}
		} else {
			WResponse resp = WResponse.getTrue();
			resp.setBean(b);
			return resp;
		}
	}

	@Override
	public WData read(MStringID id) {
		WData bean = getBean(id);
		if (bean != null) {
			if (!bean.getName().equals("object") || bean.get("data") == null) {
				WData b = new WData("object");
				b.addValue("success", true);
				b.add("data").add(bean);
				return b;
			} else {
				return bean;
			}
		} else {
			List<String> params = new LinkedList<String>();
			params.add(id.toString());
			WData response = getBean("objects", "read", false, params);
			if (response.get("object") != null) {
				beanstorage.addBean(id, response.get("object"));
			}
			return response;
		}
	}

	private WData getBean(MStringID id) {
		return beanstorage.getBean(id);
	}

	@Override
	public void addBean(MStringID id, WData b) {
		beanstorage.addBean(id, b);
	}

	private WResponse store(MStringID id, WData bean) {
		List<String> params = new LinkedList<String>();
		params.add(id.toString());
		Map<String, String> data = new HashMap<String, String>();
		//
		data.put("data", bean.toXML().toString());
		return post("objects", "write", params, data);
	}

	private WResponse getResponses(final String service, String method,
			boolean auth, List<String> params) {
		String responseBody = callService(service, method, auth, params);
		return parseResponse(responseBody);
	}

	private String callService(final String service, String method,
			boolean auth, List<String> params) {
		MURL murl = getURL(service, method, auth, params);
		//
		URLCaller urlcaller = new URLCaller(murl);
		return urlcaller.getResponseBody();
	}

	private WData getBean(String service, String method, boolean auth,
			List<String> params) {
		String string = callService(service, method, auth, params);
		try {
			// TODO parsing twice because xml data is escaped in the original.
			WData b = new WData(new XML(string));
			return new WData(b.toXML());
		} catch (SAXException e) {
			log.error(e);
			return null;
		}
	}

	private WResponse parseResponse(String responseBody) {
		if (responseBody != null) {
			try {
				return new WResponse(responseBody);
			} catch (SAXException e) {
				log.error("" + e);
				return null;
			}
		} else {
			return WResponse.getError("Null response");
		}
	}

	public boolean isConnected() {
		MURL murl = getURL("users", "test", false, new LinkedList<String>());
		URLCaller urlcaller = new URLCaller(murl);
		urlcaller.setTimeout(200);

		String responseBody = urlcaller.getResponseBody();
		WResponse responseBean = parseResponse(responseBody);
		return responseBean != null && responseBean.isSuccess();
	}

	@Override
	public boolean publish(ObjectID id) {
		return publish(id.getStringID());
	}

	@Override
	public boolean publish(MStringID id) {
		List<String> params = new LinkedList<String>();
		params.add(id.toString());
		store(id, getBean(id));
		return getResponses("objects", "publish", true, params).isSuccess();
	}

	private MURL getURL(final String service, String method, boolean doauth,
			List<String> params) {
		String auth = null;
		if (doauth) {
			auth = sessionid;
		}
		return getAuthURL(service, method, params, auth);
	}

	public MURL getAuthURL(final String service, String method,
			List<String> params, String auth) {
		MURL murl = new MURL(url.toString());
		murl.append("/" + service);
		murl.append("/" + method);
		if (auth != null) {
			murl.append("/" + auth);
		}
		if (params != null) {
			for (final String string : params) {
				murl.append("/" + string);
			}
		}
		return murl;
	}

	@Override
	public WResponse search(final String filter, int index, int i) {
		List<String> params = new LinkedList<String>();
		params.add(filter);
		params.add("" + index);
		params.add("" + i);
		//
		return getResponses("objects", "search", false, params);
	}

	private WResponse post(final String service, String method,
			List<String> params, Map<String, String> data) {
		String sbody = null;
		try {
			MURL mnurl = getURL(service, method, true, params);
			URLCaller urlcaller = new URLCaller(mnurl);
			urlcaller.setPostData(data);
			log.info("posting " + data + " to " + mnurl);
			String responseBody = urlcaller.getResponseBody();
			if (responseBody != null) {
				sbody = new String(responseBody);
			} else {
				sbody = "";
			}
			return new WResponse(sbody);
		} catch (Exception e) {
			log.error(e);
			log.info("exception with response " + sbody);
			return WResponse.getError("ERROR " + sbody);
		}
	}

	@Override
	public UserID getUserID() {
		return userid;
	}
}
