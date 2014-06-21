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

import waazdoh.client.model.JBean;
import waazdoh.client.model.JBeanResponse;
import waazdoh.client.model.MID;
import waazdoh.client.model.UserID;
import waazdoh.service.CMService;
import waazdoh.util.MStringID;
import waazdoh.util.MURL;

public final class StaticService implements CMService {
	private UserID userid;
	private static Map<MStringID, JBean> data = new HashMap<MStringID, JBean>();

	@Override
	public boolean setSession(String session) {
		return true;
	}

	@Override
	public String getInfoText() {
		return "staticservice:" + data;
	}

	@Override
	public boolean isConnected() {
		return true;
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
	public JBean requestAppLogin() {
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
	public JBean read(MStringID id) {
		JBean bean = data.get(id);
		return bean;
	}

	@Override
	public String getUsername() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MURL getURL(final String service, String method, MID id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isLoggedIn() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void addBean(MStringID id, JBean b) {
		data.put(id, b);
	}

	@Override
	public UserID getUserID() {
		if (userid == null) {
			userid = new UserID((new MStringID()).toString());
		}
		return userid;
	}

	@Override
	public JBeanResponse search(final String filter, int index, int i) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JBeanResponse getBookmarkGroup(final String id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, String> getBookmarkGroups() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JBean acceptAppLogin(MStringID id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JBean checkAppLogin(MStringID id) {
		// TODO Auto-generated method stub
		return null;
	}
}
