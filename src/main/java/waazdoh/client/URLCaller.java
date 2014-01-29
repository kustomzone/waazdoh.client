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

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpClientParams;

import waazdoh.cutils.MLogger;
import waazdoh.cutils.MURL;

public final class URLCaller {
	private MURL url;
	private Integer code;
	private byte[] o;
	private String post;
	private MLogger log;
	private ProxySettings proxysettings;
	private String password;
	private String username;
	private File postfile;
	private String postfilename;
	private int timeout;
	private static int clientscreated = 0;
	private static List<HttpClient> httpclients = new LinkedList<HttpClient>();

	private static HttpClient getHttpClient() {
		if (httpclients == null) {
			httpclients = new LinkedList<HttpClient>();
		}
		synchronized (httpclients) {
			if (httpclients.size() == 0) {
				clientscreated++;
				getLogger().info("clients created " + clientscreated);
				HttpClient c;
				c = new HttpClient();
				httpclients.add(c);
			}
			return httpclients.remove(0);
		}
	}

	private static MLogger getLogger() {
		return MLogger.getLogger("static urlcaller");
	}

	private static void reset() {
		synchronized (httpclients) {
			try {
				httpclients.wait(2000);
			} catch (InterruptedException e) {
				getLogger().error(e);
			}
		}
		httpclients = null;
	}

	public URLCaller(MURL url, ProxySettings proxy) {
		this.url = url;
		this.proxysettings = proxy;
		this.log = MLogger.getLogger(this);
	}

	public void setCredentials(String un, String pw) {
		this.username = un;
		this.password = pw;
	}

	private byte[] doCall() {
		try {
			HttpMethodBase method;
			if (post == null && postfile == null) {
				method = new GetMethod(url.toString());
			} else {
				method = getPostMethod();
			}
			MLogger.getLogger(this).info("calling " + method + " url:" + url);
			HttpClient httpClient = getHttpClient();
			httpClient.getParams().setSoTimeout(timeout);
			//
			if (proxysettings != null) {
				proxysettings.handle(httpClient);
			}
			if (username != null) {
				httpClient.getParams().setAuthenticationPreemptive(true);
				Credentials defaultcreds = new UsernamePasswordCredentials(
						username, password);
				httpClient.getState().setCredentials(
						new AuthScope(url.getHost(), url.getPort(),
								AuthScope.ANY_REALM), defaultcreds);
			}
			//
			code = httpClient.executeMethod(method);
			byte bs[] = method.getResponseBody();
			if (bs != null && isOKHttpReponse()) {
				o = bs;
			} else {
				log.error("Response:" + new String(bs));
			}
			synchronized (httpclients) {
				httpclients.add(httpClient);
			}
		} catch (HttpException e) {
			log.info("" + e);
			reset();
		} catch (IOException e) {
			log.info("" + e);
			URLCaller.reset();
		} catch (IllegalStateException e) {
			log.info("" + e);
			URLCaller.reset();
		} catch (IllegalArgumentException e) {
			log.info("" + e);
			URLCaller.reset();
		}
		return o;
	}

	private HttpMethodBase getPostMethod() {
		HttpMethodBase method;
		NameValuePair[] parts;
		PostMethod nmethod = new PostMethod(url.toString());
		String spost = post.replace("+", "%2B");
		parts = new NameValuePair[] { new NameValuePair("data", spost) };
		// StringPart part = new StringPart("data", spost);
		// part.setCharSet("UTF-8");
		// parts = new Part[] { part };
		nmethod.setRequestBody(parts);
		method = nmethod;
		return method;
	}

	public byte[] getResponseBody() {
		return doCall();
	}

	private boolean isOKHttpReponse() {
		if (code == 0 || code == 200) {
			return true;
		} else {
			log.info("response not ok " + code + " " + o);
			return false;
		}
	}

	public boolean isOK() {
		if (isOKHttpReponse() && getResponseBody() != null) {
			return true;
		} else {
			return false;
		}
	}

	public void setPost(String data) {
		this.post = data;
	}

	public void setPost(String filename, File f) {
		this.postfile = f;
		this.postfilename = filename;
	}

	public static void closeConnections() {
		List<HttpClient> cs = URLCaller.httpclients;
		for (HttpClient httpClient : cs) {
			httpClient.getHttpConnectionManager().closeIdleConnections(0);
		}
	}

	public void setTimeout(int i) {
		this.timeout = i;
	}
}
