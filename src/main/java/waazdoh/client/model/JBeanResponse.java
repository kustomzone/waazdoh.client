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

import java.util.LinkedList;
import java.util.List;

import org.xml.sax.SAXException;

import waazdoh.util.MLogger;
import waazdoh.util.MStringID;
import waazdoh.util.xml.XML;

public final class JBeanResponse {
	public static final String IDLIST_ITEM = "item";
	public static final String IDLIST = "idlist";

	private static final String AUTHENTICATION_FAILED_MESSAGE = "auth failed";
	private JBean bean = new JBean("response");
	private MLogger log = MLogger.getLogger(this);

	public JBeanResponse(final String o) throws SAXException {
		String string = (String) o;
		JBean nbean = new JBean(new XML(string));
		if (nbean.getName().equals("response")) {
			this.bean = nbean;
		} else {
			this.bean.add(nbean);
		}
	}

	public JBeanResponse() {
		// TODO Auto-generated constructor stub
	}

	public JBeanResponse(Error error) {
		bean.addValue("error", "" + error);
		log.error(error);
	}

	@Override
	public String toString() {
		return "" + toXML();
	}

	public XML toXML() {
		return bean.toXML();
	}

	public JBean getBean() {
		return bean;
	}

	public void setBean(JBean bean) {
		this.bean = bean;
	}

	public static JBeanResponse getTrue() {
		JBeanResponse ret = new JBeanResponse();
		ret.bean.addValue("success", "true");
		return ret;
	}

	public static JBeanResponse getError(final String s) {
		JBeanResponse ret = getFalse();
		ret.bean.addValue("error", s);
		return ret;
	}

	public List<MStringID> getIDList() {
		List<MStringID> ret = new LinkedList<MStringID>();
		JBean items = bean.get(IDLIST);
		if (items != null) {
			List<JBean> ids = items.getChildren();
			for (JBean cb : ids) {
				if (cb.getName().equals(IDLIST_ITEM)) {
					ret.add(new MStringID(cb.getText()));
				}
			}
			return ret;
		} else {
			return null;
		}
	}

	public static JBeanResponse getFalse() {
		JBeanResponse ret = new JBeanResponse();
		ret.bean.addValue("success", "false");
		return ret;
	}

	public static JBeanResponse getAuthenticationError() {
		JBeanResponse f = getError(AUTHENTICATION_FAILED_MESSAGE);
		return f;
	}

	public boolean isSuccess() {
		return bean.getBooleanValue("success");
	}

	public JBean find(String string) {
		return getBean().find(string);
	}
}
