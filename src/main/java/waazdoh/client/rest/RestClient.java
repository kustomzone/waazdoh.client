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
package waazdoh.client.rest;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.xml.sax.SAXException;

import waazdoh.client.MBinarySource;
import waazdoh.client.URLCaller;
import waazdoh.cutils.JBeanResponse;
import waazdoh.cutils.MID;
import waazdoh.cutils.MLogger;
import waazdoh.cutils.MStringID;
import waazdoh.cutils.MURL;
import waazdoh.cutils.UserID;
import waazdoh.cutils.xml.JBean;
import waazdoh.cutils.xml.XML;
import waazdoh.service.CMService;

public final class RestClient implements CMService {
	private URL url;
	private MLogger log = MLogger.getLogger(this);
	private String sessionid;
	private UserID userid;
	private String username;
	private boolean loggedin;
	private MBinarySource source;

	public RestClient(final String localurl, MBinarySource source)
			throws MalformedURLException {
		this.url = new URL(localurl);
		this.source = source;
	}

	@Override
	public String getInfoText() {
		return "Session:" + sessionid + " User:" + userid;
	}

	@Override
	public String requestAppLogin(final String email, String appname,
			MStringID appid) {
		// @Path("/authenticateapp/{email}/{appid}/{appname}")
		//
		String method = "authenticateapp";
		Map<String, String> data = new HashMap<String, String>();
		data.put("email", email);
		data.put("appid", appid.toString());
		data.put("appname", appname);
		//
		JBeanResponse response = post("users", method,
				new LinkedList<String>(), data);
		log.info("response " + response);
		loggedin = response.isSuccess();
		if (loggedin) {
			JBean responsebean = response.getBean();
			sessionid = responsebean.getValue("sessionid");
			userid = new UserID(responsebean.getValue("userid"));
			this.username = email;
			//
			return sessionid;
		} else {
			sessionid = null;
			userid = null;
			return null;
		}
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
	public boolean setSession(final String username, String session) {
		if (session != null && session.length() > 0) {
			sessionid = session;
			List<String> params = new LinkedList<String>();
			params.add(username);
			JBeanResponse response = getResponses("users", "checksession",
					true, params);
			log.info("checksession response " + response);
			if (response.isSuccess()) {
				String suserid = response.getBean().find("uid").getText();
				if (suserid != null) {
					userid = new UserID(suserid);
					this.username = username;
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
	public MURL getURL(final String service, String type, MID id) {
		LinkedList<String> params = new LinkedList<String>();
		params.add("" + id);
		return getAnonymousURL(service, type, params);
	}

	private MURL getAnonymousURL(final String service, String string,
			LinkedList<String> params) {
		return getAuthURL(service, string, params, null);
	}

	@Override
	public JBeanResponse reportDownload(MStringID id, boolean success) {
		List<String> params = new LinkedList<String>();
		params.add(sessionid.toString());
		params.add(id.toString());
		params.add("" + success);
		JBeanResponse response = getResponses("objects", "reportdownload",
				false, params);
		return response;
	}

	@Override
	public JBeanResponse getUser(UserID userid) {
		JBean b = source.getBean(userid.toString());
		if (b == null) {
			List<String> params = new LinkedList<String>();
			params.add(userid.toString());
			JBeanResponse getresponse = getResponses("users", "get", true,
					params);
			if (getresponse.isSuccess()) {
				source.addBean(userid.toString(), b);
				JBeanResponse resp = JBeanResponse.getTrue();
				resp.setBean(b);
				return resp;
			} else {
				return JBeanResponse.getFalse();
			}
		} else {
			JBeanResponse resp = JBeanResponse.getTrue();
			resp.setBean(b);
			return resp;
		}
	}

	@Override
	public JBean read(MStringID id) {
		JBean bean = getBean(id);
		if (bean != null) {
			if (!bean.getName().equals("object") || bean.get("data") == null) {
				JBean b = new JBean("object");
				b.add("data").add(bean);
				return b;
			} else {
				return bean;
			}
		} else {
			List<String> params = new LinkedList<String>();
			params.add(id.toString());
			JBean response = getBean("objects", "read", false, params);
			if (response.get("object") != null) {
				source.addBean(id.toString(), response.get("object"));
			}
			return response;
		}
	}

	private JBean getBean(MStringID id) {
		return source.getBean(id.toString());
	}

	@Override
	public void addBean(MStringID id, JBean b) {
		source.addBean(id.toString(), b);
	}

	private JBeanResponse store(MStringID id, JBean bean) {
		List<String> params = new LinkedList<String>();
		params.add(id.toString());
		Map<String, String> data = new HashMap<String, String>();
		//
		data.put("data", bean.toXML().toString());
		return post("objects", "write", params, data);
	}

	private JBeanResponse getResponses(final String service, String method,
			boolean auth, List<String> params) {
		byte[] responseBody = callService(service, method, auth, params);
		return parseResponse(responseBody);
	}

	private byte[] callService(final String service, String method,
			boolean auth, List<String> params) {
		MURL murl = getURL(service, method, auth, params);
		//
		URLCaller urlcaller = new URLCaller(murl, new ClientProxySettings());
		byte[] responseBody = urlcaller.getResponseBody();
		return responseBody;
	}

	private JBean getBean(String service, String method, boolean auth,
			List<String> params) {
		byte[] bytes = callService(service, method, auth, params);
		try {
			String string = new String(bytes);
			// TODO parsing twice because xml data is escaped in the original.
			JBean b = new JBean(new XML(string));
			return new JBean(b.toXML());
		} catch (SAXException e) {
			log.error(e);
			return null;
		}
	}

	private JBeanResponse parseResponse(byte[] responseBody) {
		String sbody;
		if (responseBody != null) {
			sbody = new String(responseBody);
			try {
				return new JBeanResponse(sbody);
			} catch (SAXException e) {
				log.error("" + e);
				return null;
			}
		} else {
			return JBeanResponse.getError("Null response");
		}
	}

	public boolean isConnected() {
		MURL murl = getURL("users", "test", false, new LinkedList<String>());
		URLCaller urlcaller = new URLCaller(murl, new ClientProxySettings());
		urlcaller.setTimeout(200);

		byte[] responseBody = urlcaller.getResponseBody();
		JBeanResponse responseBean = parseResponse(responseBody);
		return responseBean != null && responseBean.isSuccess();
	}

	@Override
	public boolean publish(MID id) {
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
		MURL murl = new MURL(url.getHost(), url.getPort());
		murl.append(url.getPath());
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
	public JBeanResponse search(final String filter, int index, int i) {
		List<String> params = new LinkedList<String>();
		params.add(filter);
		params.add("" + index);
		params.add("" + i);
		//
		return getResponses("objects", "search", false, params);
	}

	private JBeanResponse post(final String service, String method,
			List<String> params, Map<String, String> data) {
		String sbody = null;
		try {
			MURL mnurl = getURL(service, method, true, params);
			URLCaller urlcaller = new URLCaller(mnurl,
					new ClientProxySettings());
			urlcaller.setPostData(data);
			log.info("posting " + data + " to " + mnurl);
			byte[] responseBody = urlcaller.getResponseBody();
			if (responseBody != null) {
				sbody = new String(responseBody);
			} else {
				sbody = "";
			}
			return new JBeanResponse(sbody);
		} catch (Exception e) {
			log.error(e);
			log.info("exception with response " + sbody);
			return JBeanResponse.getError("ERROR " + sbody);
		}
	}

	@Override
	public HashMap<String, String> getBookmarkGroups() {
		JBeanResponse ret = getResponses("bookmarks", "listgroups", true, null);
		if (ret.isSuccess()) {
			HashMap<String, String> list = new HashMap<String, String>();

			JBean bgroups = ret.getBean().get("bookmarkgroups");
			List<JBean> cs = bgroups.getChildren();
			for (JBean groupbean : cs) {
				/*
				 * <group> <name>users</name>
				 * <groupid>fac8093e-c9ed-43b6-99bd-7fc9207f3c7d</groupid>
				 * </group>
				 */
				log.info("bookmarkgroup " + groupbean);
				list.put(groupbean.getValue("groupid"),
						groupbean.getValue("name"));
			}

			return list;
		} else {
			return null;
		}
	}

	@Override
	public JBeanResponse getBookmarkGroup(final String id) {
		List<String> params = new LinkedList<String>();
		params.add(id.toString());
		return getResponses("bookmarks", "getgroup", true, params);
	}

	@Override
	public UserID getUserID() {
		return userid;
	}
}
