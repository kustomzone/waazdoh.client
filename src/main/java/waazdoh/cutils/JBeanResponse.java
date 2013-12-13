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
package waazdoh.cutils;

import java.util.LinkedList;
import java.util.List;

import waazdoh.cutils.xml.JBean;
import waazdoh.cutils.xml.XML;

public final class JBeanResponse {
	private static final String AUTHENTICATION_FAILED_MESSAGE = "auth failed";
	private JBean bean = new JBean("response");
	private MLogger log = MLogger.getLogger(this);

	public JBeanResponse(String o) {
		String string = (String) o;
		JBean bean = new JBean(new XML(string));
		if (bean.getName().equals("response")) {
			this.bean = bean;
		} else {
			bean.add(bean);
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

	public static JBeanResponse getError(String s) {
		JBeanResponse ret = getFalse();
		ret.bean.addValue("error", s);
		return ret;
	}

	public List<MStringID> getIDList() {
		List<MStringID> ret = new LinkedList<MStringID>();
		JBean items = bean.get("items");
		if (items != null) {
			List<JBean> ids = items.getChildren();
			for (JBean jBean : ids) {
				ret.add(new MStringID(jBean.getText()));
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
		Boolean asuccess = bean.getBooleanValue("success");
		if (asuccess != null) {
			return asuccess;
		} else {
			String serror = bean.getValue("error");
			return serror == null;
		}
	}
}
