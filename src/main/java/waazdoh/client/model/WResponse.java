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
import waazdoh.util.XML;

public final class WResponse {
	public static final String IDLIST_ITEM = "item";
	public static final String IDLIST = "idlist";

	private static final String AUTHENTICATION_FAILED_MESSAGE = "auth failed";
	private WData bean = new WData("response");
	private MLogger log = MLogger.getLogger(this);

	public WResponse(final String o) throws SAXException {
		String string = (String) o;
		WData nbean = new WData(new XML(string));
		if (nbean.getName().equals("response")) {
			this.bean = nbean;
		} else {
			this.bean.add(nbean);
		}
	}

	public WResponse() {
		// TODO Auto-generated constructor stub
	}

	public WResponse(Error error) {
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

	public WData getBean() {
		return bean;
	}

	public void setBean(WData bean) {
		this.bean = bean;
	}

	public static WResponse getTrue() {
		WResponse ret = new WResponse();
		ret.bean.addValue("success", "true");
		return ret;
	}

	public static WResponse getError(final String s) {
		WResponse ret = getFalse();
		ret.bean.addValue("error", s);
		return ret;
	}

	public List<MStringID> getIDList() {
		List<MStringID> ret = new LinkedList<MStringID>();
		WData items = bean.get(IDLIST);
		if (items != null) {
			List<WData> ids = items.getChildren();
			for (WData cb : ids) {
				if (cb.getName().equals(IDLIST_ITEM)) {
					ret.add(new MStringID(cb.getText()));
				}
			}
			return ret;
		} else {
			return null;
		}
	}

	public static WResponse getFalse() {
		WResponse ret = new WResponse();
		ret.bean.addValue("success", "false");
		return ret;
	}

	public static WResponse getAuthenticationError() {
		WResponse f = getError(AUTHENTICATION_FAILED_MESSAGE);
		return f;
	}

	public boolean isSuccess() {
		return bean.getBooleanValue("success");
	}

	public WData find(String string) {
		return getBean().find(string);
	}
}
