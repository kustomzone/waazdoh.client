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
package waazdoh.client.service;

import java.util.Set;

import waazdoh.client.model.WResponse;
import waazdoh.common.MStringID;
import waazdoh.common.MURL;
import waazdoh.common.ObjectID;
import waazdoh.common.UserID;
import waazdoh.common.WData;

public interface WService {
	boolean setSession(String session);

	WData read(MStringID id);

	void addBean(MStringID id, WData b);

	boolean publish(MStringID id);

	UserID getUserID();

	WResponse search(final String filter, int index, int i);

	MURL getURL(final String service, String method, ObjectID id);

	String getUsername();

	boolean isLoggedIn();

	WData requestAppLogin();

	WData checkAppLogin(MStringID id);

	WData acceptAppLogin(MStringID id);

	String getSessionID();

	String getInfoText();

	WResponse reportDownload(MStringID id, boolean success);

	WResponse getUser(UserID userid);

	boolean publish(ObjectID id);

	boolean isConnected();

	String readStorageArea(String string);

	Set<String> listStorageArea(String string);

	void writeStorageArea(String string, String data);

}
