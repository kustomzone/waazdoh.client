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

import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import waazdoh.cutils.xml.JBean;
import waazdoh.cutils.xml.XML;

public class JBeanResponse {
	private static final String AUTHENTICATION_FAILED_MESSAGE = "auth failed";
	private JBean bean = new JBean("response");
	private MLogger log = MLogger.getLogger(this);

	public JBeanResponse(Object o) {
		if (o instanceof String) {
			String string = (String) o;
			JBean bean = new JBean(new XML(string));
			if (bean.getName().equals("response")) {
				this.bean = bean;
			} else {
				bean.add(bean);
			}
		} else {
			try {
				JAXBContext c = JAXBContext.newInstance(o.getClass());
				Marshaller m;
				m = c.createMarshaller();
				m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
				StringWriter writer = new StringWriter();
				m.marshal(o, writer);
				//
				StringBuffer buffer = writer.getBuffer();
				log.info("marshalled " + buffer);
				bean.add(new JBean(new XML(buffer.toString())));
			} catch (JAXBException e) {
				e.printStackTrace();
				log.error(e);
				getBean().add("error " + e);
			}
		}
	}

	public JBeanResponse() {
		// TODO Auto-generated constructor stub
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
		ret.bean.addAttribute("success", "true");
		return ret;
	}

	public static JBeanResponse getError(String s) {
		JBeanResponse ret = getFalse();
		ret.bean.addAttribute("error", s);
		return ret;
	}

	public List<MID> getIDList() {
		List<MID> ret = new LinkedList<MID>();
		JBean items = bean.get("items");
		if (items != null) {
			List<JBean> ids = items.getChildren();
			for (JBean jBean : ids) {
				ret.add(new MID(jBean.getValue()));
			}
			return ret;
		} else {
			return null;
		}
	}

	public static JBeanResponse getFalse() {
		JBeanResponse ret = new JBeanResponse();
		ret.bean.addAttribute("success", "false");
		return ret;
	}

	public static JBeanResponse getAuthenticationError() {
		JBeanResponse f = getError(AUTHENTICATION_FAILED_MESSAGE);
		return f;
	}

	public boolean isSuccess() {
		Boolean asuccess = bean.getAttributeBoolean("success");
		if (asuccess != null) {
			return asuccess;
		} else {
			String serror = bean.getAttribute("error");
			return serror == null;
		}
	}
}
