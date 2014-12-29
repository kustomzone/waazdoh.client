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
import java.util.Set;

import waazdoh.client.model.WService;
import waazdoh.client.model.WData;
import waazdoh.client.model.WResponse;
import waazdoh.client.model.ObjectID;
import waazdoh.client.model.UserID;
import waazdoh.util.MStringID;
import waazdoh.util.MURL;

public final class StaticService implements WService {
	private UserID userid;
	private static Map<MStringID, WData> data = new HashMap<MStringID, WData>();
	private static Map<String, String> storage = new HashMap<>();

	@Override
	public boolean setSession(String session) {
		return true;
	}

	@Override
	public String getInfoText() {
		return "staticservice:" + data;
	}

	@Override
	public String readStorageArea(String string) {
		return storage.get(string);
	}

	@Override
	public Set<String> listStorageArea(String string) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void writeStorageArea(String string, String data) {
		// TODO Auto-generated method stub	
	}
	
	@Override
	public boolean isConnected() {
		return true;
	}

	@Override
	public WResponse reportDownload(MStringID id, boolean success) {
		// TODO Auto-generated method stub
		return null;
	}

	public WResponse getUser(UserID userid) {
		return null;
	};

	@Override
	public String getSessionID() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public WData requestAppLogin() {
		return null;
	}

	public StaticService() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean publish(ObjectID id) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean publish(MStringID id) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public WData read(MStringID id) {
		WData bean = data.get(id);
		return bean;
	}

	@Override
	public String getUsername() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MURL getURL(final String service, String method, ObjectID id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isLoggedIn() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void addBean(MStringID id, WData b) {
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
	public WResponse search(final String filter, int index, int i) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public WData acceptAppLogin(MStringID id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public WData checkAppLogin(MStringID id) {
		// TODO Auto-generated method stub
		return null;
	}
}
