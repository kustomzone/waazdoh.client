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
package waazdoh.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import waazdoh.client.model.WaazdohInfo;

public final class URLCaller {
	private MURL url;
	private String o;
	private Map<String, String> postdata;
	private Map<String, String> requestproperties = new HashMap<String, String>();
	private MLogger log;

	public URLCaller(MURL url) {
		this.url = url;
		this.log = MLogger.getLogger(this);
		log.info("URLCaller with " + url);
	}

	private MLogger getLogger() {
		return log;
	}

	private String doCall() {
		try {
			HttpRequestBase method;
			if (postdata == null) {
				method = new HttpGet(url.toString());
			} else {
				method = getPostMethod();
			}
			getLogger().info("calling " + method + " url:" + url);

			CloseableHttpClient client = HttpClients.createDefault();

			method.addHeader("waazdoh.version", WaazdohInfo.version);

			CloseableHttpResponse response = client.execute(method);
			try {
				HttpEntity e = response.getEntity();
				o = EntityUtils.toString(e);
				EntityUtils.consume(e);
			} finally {
				response.close();
			}

		} catch (IOException e) {
			getLogger().info("" + e);
			return null;
		} catch (IllegalStateException e) {
			getLogger().info("" + e);
		} catch (IllegalArgumentException e) {
			getLogger().error(e);
		}
		return o;
	}

	private HttpRequestBase getPostMethod() {
		HttpPost nmethod = new HttpPost(url.toString());
		nmethod.setHeader("Content-Type", "application/x-www-form-urlencoded");

		List<NameValuePair> formData = new ArrayList<NameValuePair>();

		Set<String> postdatakeys = postdata.keySet();
		for (String postkey : postdatakeys) {
			formData.add(new BasicNameValuePair(postkey, postdata.get(postkey)));
		}

		UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formData,
				Consts.UTF_8);

		nmethod.setEntity(entity);
		return nmethod;
	}

	public String getResponseBody() {
		return doCall();
	}

	public boolean isOK() {
		return o != null;
	}

	public void setPostData(final Map<String, String> data) {
		this.postdata = data;
	}

	public void setTimeout(int i) {
		RequestConfig requestConfig = RequestConfig.custom()
				.setConnectTimeout(30 * 1000).build();
		HttpClientBuilder.create().setDefaultRequestConfig(requestConfig);
	}

	public void setRequestProperty(String name, String value) {
		requestproperties.put(name, value);
	}
}
