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

import java.util.Map;

import waazdoh.util.MStringID;
import waazdoh.util.MURL;

public interface CMService {
	boolean setSession(String session);

	JBean read(MStringID id);

	void addBean(MStringID id, JBean b);

	boolean publish(MStringID id);

	UserID getUserID();

	JBeanResponse search(final String filter, int index, int i);

	MURL getURL(final String service, String method, MID id);

	String getUsername();

	boolean isLoggedIn();

	JBean requestAppLogin();

	JBean checkAppLogin(MStringID id);

	JBean acceptAppLogin(MStringID id);

	String getSessionID();

	String getInfoText();

	JBeanResponse reportDownload(MStringID id, boolean success);

	JBeanResponse getBookmarkGroup(final String id);

	Map<String, String> getBookmarkGroups();

	JBeanResponse getUser(UserID userid);

	boolean publish(MID id);

	boolean isConnected();

}
