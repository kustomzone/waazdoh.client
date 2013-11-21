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
import java.util.Map;

import waazdoh.cutils.JBeanResponse;
import waazdoh.cutils.MID;
import waazdoh.cutils.MStringID;
import waazdoh.cutils.MURL;
import waazdoh.cutils.UserID;
import waazdoh.cutils.xml.JBean;
import waazdoh.service.CMService;

public final class StaticService implements CMService {
	private UserID userid;
	private static Map<MStringID, JBean> data = new HashMap<MStringID, JBean>();

	@Override
	public boolean setSession(String username, String session) {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public String getInfoText() {
		return "staticservice:" + data;
	}

	@Override
	public JBeanResponse reportDownload(MStringID id, boolean success) {
		// TODO Auto-generated method stub
		return null;
	}

	public JBeanResponse getUser(UserID userid) {
		return null;
	};

	@Override
	public String getSessionID() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String requestAppLogin(String username, String appname, MStringID id) {
		return null;
	}

	public StaticService() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean publish(MID id) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean publish(MStringID id) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public JBeanResponse read(MStringID id) {
		JBean bean = data.get(id);
		if (bean != null) {
			JBeanResponse resp = JBeanResponse.getTrue();
			resp.setBean(bean);
			return resp;
		} else {
			return JBeanResponse.getFalse();
		}
	}

	@Override
	public String getUsername() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MURL getURL(String service, String method, MID id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isLoggedIn() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public JBeanResponse write(MStringID id, JBean b) {
		data.put(id, b);
		return JBeanResponse.getTrue();
	}

	@Override
	public UserID getUserID() {
		if (userid == null) {
			userid = new UserID((new MStringID()).toString());
		}
		return userid;
	}

	@Override
	public JBeanResponse search(String filter, int index, int i) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JBeanResponse getBookmarkGroup(String id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, String> getBookmarkGroups() {
		// TODO Auto-generated method stub
		return null;
	}

}